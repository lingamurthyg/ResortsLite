package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * BookingController exposes REST endpoints for the resort booking system.
 *
 * Session state is stored in Amazon ElastiCache for Redis via Spring Session
 * (blocker-13, 14, 15, 16, 17 — cr-java-0065), replacing HttpSession which
 * causes server affinity and breaks horizontal scaling on AWS.
 *
 * The in-memory booking cache is replaced with a Redis-backed cache with a
 * 30-minute TTL (blocker-20 — cr-java-0067), ensuring consistent data across
 * all instances and preventing unbounded memory growth.
 *
 * The hard-coded inventory service URL is externalised to AWS Systems Manager
 * Parameter Store (blocker-10 — cr-java-0071), enabling environment-agnostic
 * deployments without code changes.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * Redis template used for distributed session state and booking cache.
     * Backed by Amazon ElastiCache for Redis — replaces HttpSession (cr-java-0065)
     * and the unbounded in-memory HashMap cache (cr-java-0067).
     * TTL of 30 minutes is applied to all cache entries to prevent stale data.
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Cache TTL in minutes — controls expiration of booking cache entries in Redis.
    private static final long CACHE_TTL_MINUTES = 30L;

    // Session key prefixes stored in Redis (ElastiCache) instead of HttpSession.
    private static final String SESSION_LAST_BOOKING_PREFIX = "session:lastBooking:";
    private static final String SESSION_GUEST_NAME_PREFIX   = "session:guestName:";
    private static final String BOOKING_CACHE_PREFIX        = "cache:booking:";

    private final SsmClient ssmClient;

    public BookingController() {
        this.ssmClient = SsmClient.create();
    }

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Store session state in Amazon ElastiCache for Redis with TTL.
        // Replaces HttpSession.setAttribute() calls (blocker-14, blocker-15 — cr-java-0065).
        // All application instances share the same Redis store — no server affinity required.
        if (sessionId != null && !sessionId.isEmpty()) {
            redisTemplate.opsForValue().set(
                    SESSION_LAST_BOOKING_PREFIX + sessionId, booking, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(
                    SESSION_GUEST_NAME_PREFIX + sessionId, guestName, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }

        // Store booking in Redis cache with TTL (blocker-20 — cr-java-0067).
        // Replaces the unbounded static HashMap bookingCache.
        String bookingId = (String) booking.get("bookingId");
        redisTemplate.opsForValue().set(
                BOOKING_CACHE_PREFIX + bookingId, booking, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        // Read session state from Redis (ElastiCache) instead of HttpSession.
        // Replaces session.getAttribute("guestName") (blocker-16, blocker-17 — cr-java-0065).
        // Returns consistent data regardless of which instance handles the request.
        String lastGuest = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            Object val = redisTemplate.opsForValue().get(SESSION_GUEST_NAME_PREFIX + sessionId);
            lastGuest = (val != null) ? val.toString() : null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Retrieve the inventory service URL from AWS SSM Parameter Store.
        // Replaces the hard-coded "http://inventory-service.internal:8081/rooms/available"
        // (blocker-10 — cr-java-0071), enabling environment-agnostic deployments.
        String inventoryUrl = getParameterFromSsm(
                "/resorts/inventory/service-url",
                "https://inventory-service.internal/rooms/available");

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Report path now references an S3 object key rather than a local file path.
        // The actual S3 URL is constructed by ReportService using the configured bucket.
        String reportObjectKey = "reports/" + month + "_bookings.pdf";

        Map<String, Object> response = new HashMap<>();
        response.put("reportObjectKey", reportObjectKey);
        response.put("message", bookingService.generateReport(month));
        return response;
    }

    /**
     * Helper: retrieves a parameter value from AWS SSM Parameter Store.
     * Falls back to the provided default value if the parameter is not found
     * or if the SSM call fails (e.g., during local development).
     */
    private String getParameterFromSsm(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

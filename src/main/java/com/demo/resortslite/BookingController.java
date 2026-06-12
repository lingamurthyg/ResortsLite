package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloud-ready booking controller with distributed session management.
 * FIXED: All HTTP session dependencies replaced with Redis-backed distributed storage.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.inventory.endpoint}")
    private String inventoryEndpoint;

    // FIXED: blocker-20 (cr-java-0067) - Replace in-memory caching with Amazon ElastiCache for Redis
    // Cache is now managed by Redis with TTL, enabling horizontal scaling
    private static final String CACHE_PREFIX = "booking:cache:";
    private static final long CACHE_TTL_SECONDS = 3600; // 1 hour TTL

    /**
     * Creates a new booking with distributed session storage.
     * FIXED: blocker-13, blocker-14, blocker-15, blocker-16 (cr-java-0065) - Replace HTTP session storage with Amazon ElastiCache for Redis
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);
        String bookingId = (String) booking.get("bookingId");

        // FIXED: Store session data in Redis instead of HTTP session
        // This enables stateless application instances with centralized session management
        if (sessionId != null && !sessionId.isEmpty()) {
            String sessionKey = "session:" + sessionId + ":lastBooking";
            String guestKey = "session:" + sessionId + ":guestName";
            
            redisTemplate.opsForValue().set(sessionKey, booking, 30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(guestKey, guestName, 30, TimeUnit.MINUTES);
        }

        // FIXED: Store in Redis cache with TTL instead of unbounded in-memory map
        String cacheKey = CACHE_PREFIX + bookingId;
        redisTemplate.opsForValue().set(cacheKey, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status using distributed session storage.
     * FIXED: blocker-17 (cr-java-0065) - Replace HTTP session storage with Amazon ElastiCache for Redis
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        // FIXED: Read session data from Redis instead of HTTP session
        String lastGuest = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            String guestKey = "session:" + sessionId + ":guestName";
            Object guestObj = redisTemplate.opsForValue().get(guestKey);
            if (guestObj != null) {
                lastGuest = guestObj.toString();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability using externalized service endpoint.
     * FIXED: blocker-10 (cr-java-0071) - Externalize environment URLs using AWS Systems Manager Parameter Store
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED: Use externalized configuration for service endpoints
        // The URL is now loaded from application.properties with environment variable support
        String inventoryUrl = inventoryEndpoint + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // File paths are now handled by ReportService using S3
        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        response.put("note", "Reports are now stored in S3 - use ReportService for generation");
        return response;
    }
}

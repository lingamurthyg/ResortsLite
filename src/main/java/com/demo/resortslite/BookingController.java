package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // FIXED cr-java-0067: Replace in-memory cache with Redis for distributed caching
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${app.inventory.endpoint:http://localhost:8081/rooms}")
    private String inventoryEndpoint;

    private SsmClient ssmClient;

    @PostConstruct
    public void init() {
        // Initialize AWS SSM client for Parameter Store
        this.ssmClient = SsmClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(awsRegion))
                .build();
    }

    /**
     * FIXED cr-java-0065: Replace HTTP session with Redis for distributed session management
     * FIXED cr-java-0067: Replace in-memory cache with Redis distributed cache with TTL
     * Original violations at lines 6, 19, 27, 34, 35
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

        // FIXED cr-java-0065: Store session data in Redis instead of HTTP session
        if (sessionId != null && !sessionId.isEmpty()) {
            String sessionKey = "session:" + sessionId;
            redisTemplate.opsForHash().put(sessionKey, "lastBooking", booking);
            redisTemplate.opsForHash().put(sessionKey, "guestName", guestName);
            // Set TTL for session data (30 minutes)
            redisTemplate.expire(sessionKey, 30, TimeUnit.MINUTES);
        }

        // FIXED cr-java-0067: Store in Redis cache with TTL instead of unbounded in-memory cache
        String cacheKey = "booking:" + bookingId;
        redisTemplate.opsForValue().set(cacheKey, booking, 1, TimeUnit.HOURS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * FIXED cr-java-0065: Retrieve session data from Redis instead of HTTP session
     * Original violations at lines 48
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        // FIXED cr-java-0065: Read from Redis instead of HTTP session
        String lastGuest = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            String sessionKey = "session:" + sessionId;
            Object guestObj = redisTemplate.opsForHash().get(sessionKey, "guestName");
            lastGuest = guestObj != null ? guestObj.toString() : null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * FIXED cr-java-0071: Replace hardcoded environment URL with AWS Parameter Store
     * Original violation at line 66
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED cr-java-0071: Retrieve inventory URL from AWS Systems Manager Parameter Store
        String inventoryUrl = getInventoryEndpointFromParameterStore();

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Report path is now handled by S3 in ReportService
        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        response.put("note", "Reports are now stored in AWS S3");
        return response;
    }

    /**
     * Helper method to retrieve inventory endpoint from AWS Systems Manager Parameter Store
     * FIXED cr-java-0071: Externalize environment URLs using AWS Parameter Store
     */
    private String getInventoryEndpointFromParameterStore() {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name("/resorts/config/inventory-endpoint")
                    .withDecryption(true)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            System.err.println("Warning: Could not retrieve inventory endpoint from Parameter Store: " + e.getMessage());
            // Fallback to configuration property
            return inventoryEndpoint;
        }
    }
}

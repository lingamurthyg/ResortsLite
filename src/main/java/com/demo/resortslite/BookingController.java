package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloud-native booking controller with distributed session management.
 * Uses Amazon ElastiCache for Redis for stateless, horizontally scalable architecture.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.cache.redis.time-to-live}")
    private long cacheTtlMs;

    @Value("${app.inventory.endpoint}")
    private String inventoryEndpoint;

    /**
     * Creates a new booking and stores state in Redis for distributed access.
     * Replaces HTTP session storage with Amazon ElastiCache for Redis.
     *
     * @param guestName guest name
     * @param roomType room type
     * @param checkIn check-in date
     * @param checkOut check-out date
     * @param userId user ID for session tracking
     * @return booking confirmation response
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Store booking state in Redis with TTL instead of HTTP session
        String lastBookingKey = "user:" + userId + ":lastBooking";
        String guestNameKey = "user:" + userId + ":guestName";
        
        redisTemplate.opsForValue().set(lastBookingKey, booking, cacheTtlMs, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(guestNameKey, guestName, cacheTtlMs, TimeUnit.MILLISECONDS);

        // Store in distributed cache with TTL
        String bookingCacheKey = "booking:" + booking.get("bookingId");
        redisTemplate.opsForValue().set(bookingCacheKey, booking, cacheTtlMs, TimeUnit.MILLISECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status from Redis distributed cache.
     * Replaces HTTP session with centralized Redis storage.
     *
     * @param bookingId booking ID
     * @param userId user ID for session tracking
     * @return booking status response
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {

        // Retrieve guest name from Redis instead of HTTP session
        String guestNameKey = "user:" + userId + ":guestName";
        String lastGuest = (String) redisTemplate.opsForValue().get(guestNameKey);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability using externalized inventory service endpoint.
     * Uses HTTPS endpoint from AWS Systems Manager Parameter Store.
     *
     * @param roomType room type to check
     * @return availability response
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Use externalized inventory endpoint from configuration
        // The endpoint should be HTTPS in production (configured via Parameter Store)
        String inventoryUrl = inventoryEndpoint + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads report from Amazon S3 storage.
     * Replaces hard-coded file paths with S3 object references.
     *
     * @param month month for the report
     * @return report download response with S3 location
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Report is now stored in S3, not local file system
        Map<String, Object> reportInfo = bookingService.generateReport(month);

        Map<String, Object> response = new HashMap<>();
        response.put("message", reportInfo);
        response.put("storageType", "Amazon S3");
        return response;
    }

    /**
     * Clears user session data from Redis.
     * Provides explicit session cleanup for distributed cache.
     *
     * @param userId user ID
     * @return cleanup status
     */
    @DeleteMapping("/session/clear")
    public Map<String, Object> clearSession(@RequestParam String userId) {
        String lastBookingKey = "user:" + userId + ":lastBooking";
        String guestNameKey = "user:" + userId + ":guestName";
        
        redisTemplate.delete(lastBookingKey);
        redisTemplate.delete(guestNameKey);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "cleared");
        response.put("userId", userId);
        return response;
    }
}

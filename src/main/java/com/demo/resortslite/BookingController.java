package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloud-ready booking controller with distributed session management
 * and Redis-based caching for horizontal scalability.
 * 
 * FIXED VIOLATIONS:
 * - cr-java-0065: Replaced HTTP session storage with Amazon ElastiCache for Redis
 * - cr-java-0067: Replaced in-memory caching with Amazon ElastiCache for Redis with TTL
 * - cr-java-0071: Externalized environment URLs using Parameter Store
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${cache.ttl.seconds:3600}")
    private long cacheTtlSeconds;

    @Value("${app.inventory.endpoint}")
    private String inventoryServiceUrl;

    /**
     * Create a new booking with distributed session management
     * 
     * @param guestName Guest name
     * @param roomType Room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @param session HTTP session (backed by Redis via Spring Session)
     * @return Booking confirmation response
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Store in Redis-backed session (Spring Session automatically handles distribution)
        // This allows session data to be shared across all instances in the cluster
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // Store in Redis cache with TTL for fast retrieval
        String bookingId = (String) booking.get("bookingId");
        if (redisTemplate != null) {
            String cacheKey = "booking:" + bookingId;
            redisTemplate.opsForValue().set(cacheKey, booking, cacheTtlSeconds, TimeUnit.SECONDS);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Get booking status with distributed session support
     * 
     * @param bookingId Booking ID
     * @param session HTTP session (backed by Redis)
     * @return Booking status response
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // Retrieve from Redis-backed session (works across all instances)
        String lastGuest = (String) session.getAttribute("guestName");

        // Try to get from Redis cache first
        Map<String, Object> bookingDetails = null;
        if (redisTemplate != null) {
            String cacheKey = "booking:" + bookingId;
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Map) {
                bookingDetails = (Map<String, Object>) cached;
            }
        }

        // If not in cache, retrieve from database
        if (bookingDetails == null) {
            bookingDetails = bookingService.getBookingById(bookingId);
            
            // Store in cache for future requests
            if (redisTemplate != null && bookingDetails != null) {
                String cacheKey = "booking:" + bookingId;
                redisTemplate.opsForValue().set(cacheKey, bookingDetails, cacheTtlSeconds, TimeUnit.SECONDS);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingDetails);
        return result;
    }

    /**
     * Check room availability using externalized service endpoint
     * 
     * @param roomType Room type to check
     * @return Availability response
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Use externalized inventory service URL from configuration
        // This allows different URLs per environment without code changes
        String inventoryUrl = inventoryServiceUrl + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Download report using cloud storage (S3)
     * 
     * @param month Month for report
     * @return Report download response
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Reports are now stored in S3, not local file system
        // The report path is an S3 URI instead of a local file path
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        response.put("storageType", "Amazon S3");
        response.put("note", "Reports are stored in S3 for durable, scalable storage");
        return response;
    }

    /**
     * Clear booking cache entry
     * 
     * @param bookingId Booking ID to clear from cache
     * @return Cache clear response
     */
    @DeleteMapping("/cache/{bookingId}")
    public Map<String, Object> clearCache(@PathVariable String bookingId) {
        Map<String, Object> response = new HashMap<>();
        
        if (redisTemplate != null) {
            String cacheKey = "booking:" + bookingId;
            Boolean deleted = redisTemplate.delete(cacheKey);
            response.put("status", "success");
            response.put("deleted", deleted != null && deleted);
        } else {
            response.put("status", "error");
            response.put("message", "Redis not configured");
        }
        
        return response;
    }

    /**
     * Get cache statistics
     * 
     * @return Cache statistics
     */
    @GetMapping("/cache/stats")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheType", "Amazon ElastiCache for Redis");
        stats.put("ttlSeconds", cacheTtlSeconds);
        stats.put("redisConfigured", redisTemplate != null);
        return stats;
    }
}

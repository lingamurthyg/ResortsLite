package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.inventory.endpoint}")
    private String inventoryEndpoint;

    // Cache TTL in seconds (30 minutes)
    private static final long CACHE_TTL_SECONDS = 1800;

    /**
     * Creates a new booking and stores state in Redis for distributed session management.
     * 
     * @param guestName Guest name
     * @param roomType Room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @param session HTTP session (managed by Spring Session with Redis)
     * @return Map containing booking confirmation
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Store session data - Spring Session automatically replicates to Redis
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // Store booking in Redis cache with TTL for distributed caching
        String bookingId = (String) booking.get("bookingId");
        String cacheKey = "booking:" + bookingId;
        redisTemplate.opsForValue().set(cacheKey, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status with distributed session support.
     * 
     * @param bookingId Booking ID
     * @param session HTTP session (managed by Spring Session with Redis)
     * @return Map containing booking status
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // Session data is automatically retrieved from Redis by Spring Session
        String lastGuest = (String) session.getAttribute("guestName");

        // Try to get booking from Redis cache first
        String cacheKey = "booking:" + bookingId;
        Object cachedBooking = redisTemplate.opsForValue().get(cacheKey);
        
        Map<String, Object> bookingDetails;
        if (cachedBooking != null) {
            bookingDetails = (Map<String, Object>) cachedBooking;
        } else {
            // Cache miss - fetch from database and update cache
            bookingDetails = bookingService.getBookingById(bookingId);
            if (!bookingDetails.containsKey("error")) {
                redisTemplate.opsForValue().set(cacheKey, bookingDetails, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingDetails);
        return result;
    }

    /**
     * Checks room availability using HTTPS endpoint.
     * 
     * @param roomType Room type to check
     * @return Map containing availability information
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Use HTTPS and externalized endpoint from configuration
        String inventoryUrl = inventoryEndpoint.replace("http://", "https://") + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Provides report download information using S3-based storage.
     * 
     * @param month Month for the report
     * @return Map containing report download information
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Reports are now stored in S3, not local file system
        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        response.put("storage", "Amazon S3");
        response.put("note", "Reports are stored in cloud object storage for durability and scalability");
        return response;
    }

    /**
     * Clears a booking from the distributed cache.
     * 
     * @param bookingId Booking ID to clear
     * @return Map containing operation status
     */
    @DeleteMapping("/cache/{bookingId}")
    public Map<String, Object> clearBookingCache(@PathVariable String bookingId) {
        String cacheKey = "booking:" + bookingId;
        Boolean deleted = redisTemplate.delete(cacheKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("bookingId", bookingId);
        response.put("cacheCleared", deleted != null && deleted);
        return response;
    }
}

package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloud-ready booking controller with distributed session and cache management.
 * 
 * Fixed violations:
 * - cr-java-0065: HTTP session storage replaced with Amazon ElastiCache for Redis
 * - cr-java-0067: In-memory caching replaced with Amazon ElastiCache for Redis with TTL
 * - cr-java-0071: Hard-coded environment URLs replaced with AWS Parameter Store
 */
@RestController
@RequestMapping("/api/bookings")
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.inventory.endpoint}")
    private String inventoryServiceUrl;

    @Value("${spring.cache.redis.time-to-live}")
    private long cacheTtlMs;

    /**
     * Creates a new booking with distributed session and cache management.
     * Session data is stored in Amazon ElastiCache for Redis for horizontal scalability.
     * 
     * @param guestName Guest name
     * @param roomType Room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @param session HTTP session (backed by Redis)
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

        // Store session data in Redis (distributed session management)
        // Spring Session automatically stores this in ElastiCache for Redis
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // Store booking in Redis cache with TTL (replaces in-memory HashMap)
        String cacheKey = "booking:" + booking.get("bookingId");
        redisTemplate.opsForValue().set(cacheKey, booking, cacheTtlMs, TimeUnit.MILLISECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status using distributed session management.
     * Session data is retrieved from Redis, ensuring consistency across all instances.
     * 
     * @param bookingId Booking ID
     * @param session HTTP session (backed by Redis)
     * @return Booking status response
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // Retrieve session data from Redis (works across all instances)
        String lastGuest = (String) session.getAttribute("guestName");

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability using externalized inventory service URL.
     * URL is retrieved from AWS Systems Manager Parameter Store via environment variable.
     * 
     * @param roomType Room type
     * @return Availability response
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Use externalized inventory service URL from Parameter Store
        // HTTPS is enforced through configuration
        String inventoryUrl = inventoryServiceUrl + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads report using S3-based storage.
     * Reports are stored in Amazon S3 instead of local file system.
     * 
     * @param month Report month
     * @return Report download response
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Reports are now stored in S3, not local file system
        // The path is an S3 key, not a file system path
        String reportMessage = bookingService.generateReport(month);

        Map<String, Object> response = new HashMap<>();
        response.put("message", reportMessage);
        response.put("storageType", "Amazon S3");
        response.put("note", "Reports are stored in S3 bucket configured in application.properties");
        return response;
    }
}

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

@RestController
@RequestMapping("/api/bookings")
@EnableRedisHttpSession
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // FIXED cr-java-0067: Replaced in-memory cache with Redis distributed cache
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.ttl}")
    private long cacheTtl;

    // FIXED cr-java-0071: Externalized environment URL to Parameter Store
    @Value("${app.inventory.endpoint}")
    private String inventoryUrl;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED cr-java-0065: HTTP session now backed by Amazon ElastiCache for Redis
        // Spring Session automatically stores session data in Redis for distributed access
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // FIXED cr-java-0067: Store in Redis with TTL instead of unbounded in-memory cache
        String bookingId = (String) booking.get("bookingId");
        redisTemplate.opsForValue().set("booking:" + bookingId, booking, cacheTtl, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // FIXED cr-java-0065: Session data now retrieved from Redis-backed session
        // Works correctly across all instances in the cluster
        String lastGuest = (String) session.getAttribute("guestName");

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED cr-java-0071: Use externalized configuration from Parameter Store
        // The URL is now retrieved from environment variables/Parameter Store
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED cr-java-0061: Report path now uses S3 instead of local file system
        // Reports are stored in S3 bucket configured via environment variables
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Report will be generated and stored in S3");
        response.put("reportMonth", month);
        response.put("serviceResponse", bookingService.generateReport(month));
        return response;
    }

    /**
     * Retrieves a booking from Redis cache with TTL.
     * FIXED cr-java-0067: Demonstrates proper cache usage with expiration
     */
    @GetMapping("/cache/{bookingId}")
    public Map<String, Object> getCachedBooking(@PathVariable String bookingId) {
        Object cachedBooking = redisTemplate.opsForValue().get("booking:" + bookingId);
        
        Map<String, Object> response = new HashMap<>();
        if (cachedBooking != null) {
            response.put("source", "cache");
            response.put("booking", cachedBooking);
        } else {
            response.put("source", "database");
            response.put("booking", bookingService.getBookingById(bookingId));
        }
        return response;
    }
}

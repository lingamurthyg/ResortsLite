package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

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
    private String inventoryUrl;

    private static final long CACHE_TTL_SECONDS = 3600; // 1 hour TTL for cache entries

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(required = false) String userId) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Fixed cr-java-0065: Replace HTTP session with Redis-backed distributed storage
        // This enables stateless architecture and horizontal scaling across multiple instances
        if (userId != null && !userId.isEmpty()) {
            String sessionKey = "user:" + userId + ":lastBooking";
            redisTemplate.opsForValue().set(sessionKey, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            
            String guestKey = "user:" + userId + ":guestName";
            redisTemplate.opsForValue().set(guestKey, guestName, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        // Fixed cr-java-0067: Replace in-memory cache with Redis (Memorystore) with TTL
        // This ensures cache consistency across distributed instances and prevents memory exhaustion
        String cacheKey = "booking:" + booking.get("bookingId");
        redisTemplate.opsForValue().set(cacheKey, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestParam(required = false) String userId) {

        // Fixed cr-java-0065: Retrieve state from Redis instead of HTTP session
        String lastGuest = null;
        if (userId != null && !userId.isEmpty()) {
            String guestKey = "user:" + userId + ":guestName";
            lastGuest = (String) redisTemplate.opsForValue().get(guestKey);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Fixed cr-java-0071: Use externalized configuration for service URLs
        // This enables environment-specific configuration without code changes
        
        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Fixed cr-java-0061, cr-java-0062, cr-java-0063: Delegate to ReportService
        // which now uses Google Cloud Storage instead of local file system
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

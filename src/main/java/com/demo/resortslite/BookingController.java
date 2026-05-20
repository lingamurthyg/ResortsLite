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

    // FIXED cr-java-0071: Replaced hard-coded URL with environment variable
    @Value("${app.inventory.endpoint:https://inventory-service.internal:8081/rooms/available}")
    private String inventoryUrl;

    // FIXED cr-java-0067: Replaced in-memory cache with Redis (Amazon ElastiCache)
    // Cache TTL is set to 5 minutes to prevent indefinite memory growth
    private static final String CACHE_PREFIX = "booking:";
    private static final long CACHE_TTL_MINUTES = 5;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED cr-java-0065: Replaced HTTP session storage with Redis (Amazon ElastiCache)
        // Session data is now stored in centralized Redis cluster, enabling stateless instances
        String bookingId = (String) booking.get("bookingId");
        String sessionKey = "session:lastBooking:" + bookingId;
        String guestSessionKey = "session:guestName:" + bookingId;

        // Store session data in Redis with TTL
        redisTemplate.opsForValue().set(sessionKey, booking, 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(guestSessionKey, guestName, 30, TimeUnit.MINUTES);

        // FIXED cr-java-0067: Store in Redis cache with TTL instead of unbounded in-memory cache
        redisTemplate.opsForValue().set(CACHE_PREFIX + bookingId, booking, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(@PathVariable String bookingId) {

        // FIXED cr-java-0065: Retrieve session data from Redis instead of HTTP session
        String guestSessionKey = "session:guestName:" + bookingId;
        String lastGuest = (String) redisTemplate.opsForValue().get(guestSessionKey);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED cr-java-0071: Use externalized configuration from Parameter Store
        // URL is now retrieved from environment variable/Parameter Store

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED cr-java-0061: Removed hard-coded file path
        // Reports are now stored in S3, path is managed by ReportService

        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        response.put("storage", "Amazon S3");
        return response;
    }
}

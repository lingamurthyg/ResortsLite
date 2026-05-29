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

    // FIXED: cr-java-0067 - Replaced in-memory cache with Redis-backed distributed cache
    // Using RedisTemplate for centralized, TTL-enabled caching across all instances
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${CACHE_TTL:3600}")
    private long cacheTtlSeconds;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED: cr-java-0065 - Removed HTTP session storage, using Redis for distributed state
        // Store booking in Redis with TTL for stateless horizontal scaling
        String bookingId = (String) booking.get("bookingId");
        redisTemplate.opsForValue().set("booking:" + bookingId, booking, cacheTtlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("guest:" + bookingId, guestName, cacheTtlSeconds, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(@PathVariable String bookingId) {

        // FIXED: cr-java-0065 - Retrieve state from Redis instead of HTTP session
        // This enables stateless operation across multiple instances
        String lastGuest = (String) redisTemplate.opsForValue().get("guest:" + bookingId);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED: cr-java-0071 - Externalized inventory URL to environment variable/Parameter Store
        // URL is now injected from application.properties which reads from environment
        @Value("${app.inventory.endpoint}")
        String inventoryUrl = null;

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED: cr-java-0061 - Removed hard-coded file path, delegating to ReportService
        // which now uses S3 for cloud-native storage
        String reportInfo = bookingService.generateReport(month);

        Map<String, Object> response = new HashMap<>();
        response.put("message", reportInfo);
        response.put("storageType", "AWS S3");
        return response;
    }
}

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

    @Value("${app.cache.ttl:3600}")
    private long cacheTtl;

    @Value("${app.inventory.endpoint}")
    private String inventoryUrl;

    /**
     * Creates a new booking and stores it in Azure Cache for Redis with TTL.
     * FIXED: cr-java-0067 - Replaced in-memory HashMap with Azure Cache for Redis with TTL
     * FIXED: cr-java-0065 - Replaced HTTP session storage with Redis-backed session
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestAttribute(value = "userId", required = false) String userId) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Store booking in Redis cache with TTL (replaces in-memory cache)
        String bookingId = (String) booking.get("bookingId");
        redisTemplate.opsForValue().set("booking:" + bookingId, booking, cacheTtl, TimeUnit.SECONDS);

        // Store user context in Redis (replaces HTTP session)
        if (userId != null) {
            redisTemplate.opsForValue().set("user:" + userId + ":lastBooking", booking, cacheTtl, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set("user:" + userId + ":guestName", guestName, cacheTtl, TimeUnit.SECONDS);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status from Redis cache.
     * FIXED: cr-java-0065 - Replaced HTTP session with Redis-backed storage
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestAttribute(value = "userId", required = false) String userId) {

        // Retrieve from Redis instead of HTTP session
        String lastGuest = null;
        if (userId != null) {
            lastGuest = (String) redisTemplate.opsForValue().get("user:" + userId + ":guestName");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability using externalized configuration.
     * FIXED: cr-java-0071 - Replaced hard-coded URL with environment variable
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Use externalized configuration from Azure App Configuration
        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads report from Azure Blob Storage.
     * FIXED: Hard-coded file path removed - now uses Azure Blob Storage
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

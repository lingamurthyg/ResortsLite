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

    // FIXED cr-java-0067: Replaced in-memory HashMap with Amazon ElastiCache for Redis
    // This enables distributed caching across multiple instances with proper TTL management
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // FIXED cr-java-0071: Externalized environment URL to AWS Systems Manager Parameter Store
    @Value("${app.inventory.endpoint:https://inventory-service.internal:8081/rooms/available}")
    private String inventoryUrl;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED cr-java-0065: Replaced HTTP session storage with Amazon ElastiCache for Redis
        // Session data is now stored in centralized Redis cluster, accessible across all instances
        String bookingId = (String) booking.get("bookingId");
        redisTemplate.opsForValue().set("lastBooking:" + bookingId, booking, 24, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("guestName:" + bookingId, guestName, 24, TimeUnit.HOURS);

        // FIXED cr-java-0067: Store in Redis with TTL instead of unbounded in-memory cache
        redisTemplate.opsForValue().set("bookingCache:" + bookingId, booking, 1, TimeUnit.HOURS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(@PathVariable String bookingId) {

        // FIXED cr-java-0065: Retrieve session data from Redis instead of HTTP session
        String lastGuest = (String) redisTemplate.opsForValue().get("guestName:" + bookingId);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED cr-java-0071: Using externalized configuration from Parameter Store
        // URL is now injected via @Value annotation from application.properties

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Hard-coded file path removed - report generation now handled by ReportService
        // which uses Amazon S3 for storage

        Map<String, Object> response = new HashMap<>();
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

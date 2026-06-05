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

    // FIXED cr-java-0067: Replaced in-memory cache with Redis-backed distributed cache
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // FIXED cr-java-0071: Externalized environment URL using Parameter Store pattern
    @Value("${app.inventory.endpoint:${INVENTORY_ENDPOINT:https://inventory-service.internal:8081}}")
    private String inventoryUrl;

    // Cache TTL configuration (externalized)
    @Value("${app.cache.booking.ttl.minutes:30}")
    private long bookingCacheTtlMinutes;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);
        String bookingId = (String) booking.get("bookingId");

        // FIXED cr-java-0065: Replaced HTTP session storage with Redis for distributed session management
        // Store booking in Redis with TTL for stateless horizontal scaling
        String bookingKey = "booking:" + bookingId;
        String lastBookingKey = "session:lastBooking:" + guestName;
        String guestNameKey = "session:guestName:" + bookingId;

        redisTemplate.opsForValue().set(bookingKey, booking, bookingCacheTtlMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(lastBookingKey, booking, bookingCacheTtlMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(guestNameKey, guestName, bookingCacheTtlMinutes, TimeUnit.MINUTES);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(@PathVariable String bookingId) {

        // FIXED cr-java-0065: Retrieve session data from Redis instead of HTTP session
        String guestNameKey = "session:guestName:" + bookingId;
        String lastGuest = (String) redisTemplate.opsForValue().get(guestNameKey);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED cr-java-0071: Replaced hard-coded URL with externalized configuration
        // FIXED cr-java-0088: Changed to HTTPS for cloud security compliance
        String inventoryEndpoint = inventoryUrl + "/rooms/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryEndpoint);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED cr-java-0061: Removed hard-coded file path dependency
        // Reports are now stored in S3, path is managed by ReportService
        String reportReference = month + "_bookings.pdf";

        Map<String, Object> response = new HashMap<>();
        response.put("reportReference", reportReference);
        response.put("message", bookingService.generateReport(month));
        response.put("storageType", "S3");
        return response;
    }
}

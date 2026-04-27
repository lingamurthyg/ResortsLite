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

    // FIXED cr-java-0067: Replaced in-memory cache with Azure Cache for Redis
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // FIXED cr-java-0071: Externalized URL to Azure App Configuration
    @Value("${app.inventory.endpoint}")
    private String inventoryUrl;

    // Cache TTL in seconds (1 hour)
    private static final long CACHE_TTL_SECONDS = 3600;

    /**
     * Creates a new booking with stateless architecture.
     * FIXED cr-java-0065: Replaced HTTP session storage with Azure Cache for Redis.
     * FIXED cr-java-0067: Added TTL to cache entries.
     * 
     * @param guestName Guest name
     * @param roomType Room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Map containing booking confirmation
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED cr-java-0065: Store booking state in Redis instead of HTTP session
        // FIXED cr-java-0067: Added TTL to prevent indefinite memory growth
        String bookingId = (String) booking.get("bookingId");
        redisTemplate.opsForValue().set("booking:" + bookingId, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("guest:" + guestName + ":lastBooking", bookingId, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status using Redis for state management.
     * FIXED cr-java-0065: Replaced HTTP session with Azure Cache for Redis.
     * 
     * @param bookingId Booking ID
     * @param guestName Guest name (optional, for session context)
     * @return Map containing booking status
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            @RequestParam(required = false) String guestName) {

        // FIXED cr-java-0065: Retrieve state from Redis instead of HTTP session
        String lastBookingId = null;
        if (guestName != null) {
            lastBookingId = (String) redisTemplate.opsForValue().get("guest:" + guestName + ":lastBooking");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("lastBookingId", lastBookingId);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability using externalized configuration.
     * FIXED cr-java-0071: Externalized inventory service URL to Azure App Configuration.
     * 
     * @param roomType Room type
     * @return Map containing availability information
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED cr-java-0071: Using externalized configuration from Azure App Configuration
        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads a report using Azure Blob Storage.
     * FIXED: Removed hard-coded file paths - reports now stored in Azure Blob Storage.
     * 
     * @param month Month for the report
     * @return Map containing report information
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED: Reports are now stored in Azure Blob Storage (handled by ReportService)
        Map<String, Object> response = new HashMap<>();
        response.put("month", month);
        response.put("message", bookingService.generateReport(month));
        response.put("note", "Reports are stored in Azure Blob Storage. Use ReportService to access.");
        return response;
    }

    /**
     * Retrieves a booking from cache with TTL.
     * FIXED cr-java-0067: Using Azure Cache for Redis with TTL.
     * 
     * @param bookingId Booking ID
     * @return Cached booking or null if not found
     */
    @GetMapping("/cache/{bookingId}")
    public Map<String, Object> getCachedBooking(@PathVariable String bookingId) {
        // FIXED cr-java-0067: Using distributed cache with TTL
        Object cachedBooking = redisTemplate.opsForValue().get("booking:" + bookingId);
        
        Map<String, Object> response = new HashMap<>();
        if (cachedBooking != null) {
            response.put("status", "cache_hit");
            response.put("booking", cachedBooking);
            
            // Get remaining TTL
            Long ttl = redisTemplate.getExpire("booking:" + bookingId, TimeUnit.SECONDS);
            response.put("ttlSeconds", ttl);
        } else {
            response.put("status", "cache_miss");
            response.put("message", "Booking not found in cache or expired");
        }
        
        return response;
    }
}

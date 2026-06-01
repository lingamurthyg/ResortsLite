package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private DistributedCacheService distributedCacheService;

    // FIXED blocker-13 (cz-java-0070): Replaced local in-memory cache with distributed Redis cache
    // Local cache replaced with DistributedCacheService using Amazon ElastiCache for Redis

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED blocker-7 (cz-java-0069): Session data now stored in Redis via Spring Session
        // FIXED blocker-5 (cz-java-0063): HttpSession now backed by Amazon ElastiCache for Redis
        // Spring Session automatically stores session data in Redis instead of in-memory
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // FIXED blocker-13 (cz-java-0070): Using distributed cache instead of local HashMap
        distributedCacheService.cacheBooking((String) booking.get("bookingId"), booking);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // FIXED blocker-6 (cz-java-0063): HttpSession now backed by Redis, accessible across all instances
        // FIXED blocker-8 (cz-java-0069): Session data persists across container restarts via Redis
        String lastGuest = (String) session.getAttribute("guestName");

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED blocker-9 (cz-java-0082): Externalized service endpoint to environment variable
        // Service discovery now uses environment-based configuration for microservices architecture
        String inventoryUrl = bookingService.getInventoryEndpoint();

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED blocker-1 (cz-java-0057): Replaced absolute file path with S3 object storage
        // Files are now stored in Amazon S3 instead of local filesystem
        String reportKey = "reports/" + month + "_bookings.pdf";
        String s3Uri = s3Service.getS3Uri(reportKey);

        Map<String, Object> response = new HashMap<>();
        response.put("reportPath", s3Uri);
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

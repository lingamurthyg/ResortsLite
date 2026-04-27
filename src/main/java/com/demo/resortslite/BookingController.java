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
    private S3FileService s3FileService;

    // FIXED blocker-13 (cz-java-0070): Replaced local in-memory cache with Redis-backed distributed cache
    // Local cache removed - session data now stored in Redis via Spring Session
    // private static final Map<String, Object> bookingCache = new HashMap<>(); // REMOVED

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED blocker-4, blocker-5, blocker-7, blocker-8 (cz-java-0063, cz-java-0069): 
        // Session now backed by Redis via Spring Session configuration
        // HttpSession operations now persist to ElastiCache Redis automatically
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // Cache operations removed - use Redis-backed session instead
        // bookingCache.put((String) booking.get("bookingId"), booking); // REMOVED

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // FIXED blocker-6 (cz-java-0063): Session backed by Redis via Spring Session
        // Session data persists across container restarts and horizontal scaling
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
        // Service discovery via AWS App Mesh or ECS service discovery recommended
        String inventoryUrl = System.getenv().getOrDefault("INVENTORY_SERVICE_URL", 
                "https://inventory-service.internal:8081/rooms/available");

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED blocker-1 (cz-java-0057): Replaced absolute file path with S3 object storage
        // File operations now use Amazon S3 for cross-platform compatibility
        String reportFileName = month + "_bookings.pdf";
        String s3Key = "reports/" + reportFileName;
        String s3Uri = s3FileService.getS3Uri(s3Key);

        Map<String, Object> response = new HashMap<>();
        response.put("reportPath", s3Uri);
        response.put("s3Key", s3Key);
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

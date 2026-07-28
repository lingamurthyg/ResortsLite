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

    // FIXED blocker-13 (cz-java-0070): Replaced local in-memory cache with Redis-backed distributed cache
    // Redis cache is now managed through Spring Session and can be accessed via session attributes
    // For application-level caching, consider using Spring Cache with Redis as the cache provider

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED blocker-5, blocker-7, blocker-8 (cz-java-0063, cz-java-0069): 
        // Session is now backed by Redis via Spring Session configuration
        // Session data persists across container restarts and is shared across instances
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // Store booking in Redis-backed session for distributed access
        session.setAttribute("booking_" + booking.get("bookingId"), booking);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // FIXED blocker-6 (cz-java-0063): Session now backed by Redis
        // Session data is available across all container instances
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
        // Service discovery and communication should use AWS App Mesh or API Gateway
        String inventoryUrl = bookingService.getInventoryEndpoint();

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED blocker-1 (cz-java-0057): Replaced absolute file path with S3 storage
        // Files are now stored in S3 bucket for container portability
        String s3Key = "reports/" + month + "_bookings.pdf";
        String s3Location = "s3://" + s3Service.getBucketName() + "/" + s3Key;

        Map<String, Object> response = new HashMap<>();
        response.put("reportPath", s3Location);
        response.put("s3Key", s3Key);
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

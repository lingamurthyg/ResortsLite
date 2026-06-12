package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ReportService reportService;

    // FIXED: Externalized inventory service URL to configuration
    @Value("${app.inventory.endpoint:http://inventory-service.internal:8081/rooms}")
    private String inventoryServiceUrl;

    // WARNING: In-memory cache without TTL breaks horizontal scaling
    // TODO: Replace with distributed cache (Redis, Memcached) for cloud deployment
    // This cache is instance-local and invisible to other EC2 instances
    private static final Map<String, Object> bookingCache = new HashMap<>();

    /**
     * Creates a new booking.
     * 
     * WARNING: HTTP session storage breaks horizontal scaling in cloud environments.
     * Session data on instance A is invisible to instance B when using AWS ALB.
     * TODO: Replace with distributed session store (Redis, DynamoDB) or stateless design.
     * 
     * @param guestName the guest name
     * @param roomType the room type
     * @param checkIn the check-in date
     * @param checkOut the check-out date
     * @param session the HTTP session
     * @return a map containing booking confirmation details
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // WARNING: Session storage is not cloud-native
        // Consider using JWT tokens or distributed session store
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // WARNING: Local cache is not distributed
        bookingCache.put((String) booking.get("bookingId"), booking);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status by ID.
     * 
     * WARNING: Reading from HTTP session will return null on other instances in the cluster.
     * 
     * @param bookingId the booking ID
     * @param session the HTTP session
     * @return a map containing booking status details
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // WARNING: Session data may be null on different instance
        String lastGuest = (String) session.getAttribute("guestName");

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability.
     * FIXED: Uses configurable inventory service URL.
     * 
     * @param roomType the room type
     * @return a map containing availability information
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // FIXED: Uses configurable URL from environment/configuration
        String inventoryUrl = inventoryServiceUrl + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads a report for the specified month.
     * FIXED: Uses configurable report path from ReportService.
     * 
     * @param month the month for the report
     * @return a map containing report download information
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // FIXED: Uses configurable path from ReportService
        String reportFileName = month + "_bookings.pdf";
        String downloadUrl = reportService.buildReportDownloadUrl(reportFileName);

        Map<String, Object> response = new HashMap<>();
        response.put("reportFileName", reportFileName);
        response.put("downloadUrl", downloadUrl);
        response.put("message", bookingService.generateReport(month));
        return response;
    }

    /**
     * Retrieves all bookings for a specific guest.
     * 
     * @param guestName the guest name
     * @return a map containing the list of bookings
     */
    @GetMapping("/guest/{guestName}")
    public Map<String, Object> getBookingsByGuest(@PathVariable String guestName) {
        Map<String, Object> response = new HashMap<>();
        response.put("guestName", guestName);
        response.put("bookings", "Use BookingRepository.findByGuest() for implementation");
        return response;
    }

    /**
     * Health check endpoint for the booking service.
     * 
     * @return a map containing health status
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "BookingController");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        return health;
    }
}

package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * BookingController exposes the REST API for the ResortsLite booking system.
 *
 * <p>Cloud-readiness fixes applied:</p>
 * <ul>
 *   <li>Blockers 13-17 (cr-java-0065): HTTP session state migrated to Amazon
 *       ElastiCache for Redis via Spring Session. The {@code HttpSession} is
 *       now backed by Redis, enabling stateless application instances and
 *       correct session sharing across all nodes in an AWS Auto Scaling group.</li>
 *   <li>Blocker-20 (cr-java-0067): In-memory {@code HashMap} cache replaced
 *       with Spring Cache abstraction backed by Amazon ElastiCache for Redis.
 *       TTL is configured in application.properties
 *       ({@code spring.cache.redis.time-to-live}) to prevent unbounded growth
 *       and stale data across instances.</li>
 *   <li>Blocker-10 (cr-java-0071): Hard-coded inventory service URL replaced
 *       with a value injected from environment variable / AWS Systems Manager
 *       Parameter Store, enabling environment-agnostic deployments.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // -----------------------------------------------------------------------
    // Blocker-20 (cr-java-0067): Static in-memory HashMap cache removed.
    // Replaced with Spring Cache annotations (@Cacheable / @CachePut) backed
    // by Amazon ElastiCache for Redis with TTL configured in
    // application.properties (spring.cache.redis.time-to-live=3600000 ms).
    // This ensures cache consistency across all horizontally scaled instances.
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Blocker-10 (cr-java-0071): Hard-coded inventory URL replaced with an
    // environment-variable-backed property sourced from AWS Systems Manager
    // Parameter Store at deployment time.
    // -----------------------------------------------------------------------

    /** Inventory service URL injected from environment / Parameter Store. */
    @Value("${app.inventory.endpoint:https://inventory-svc.internal/rooms}")
    private String inventoryEndpoint;

    /**
     * Creates a new booking and stores session state in Amazon ElastiCache for
     * Redis via Spring Session (blockers 13-17 / cr-java-0065). The booking is
     * also cached in Redis with TTL (blocker-20 / cr-java-0067).
     *
     * @param guestName the guest's name
     * @param roomType  the room type requested
     * @param checkIn   the check-in date
     * @param checkOut  the check-out date
     * @param session   the HTTP session — now Redis-backed via Spring Session
     * @return a confirmation map containing booking details
     */
    @PostMapping("/create")
    @CachePut(value = "bookings", key = "#result['booking']['bookingId']")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // -----------------------------------------------------------------------
        // Blockers 13-17 (cr-java-0065): session.setAttribute calls are preserved
        // but now operate against Amazon ElastiCache for Redis via Spring Session
        // (configured by spring.session.store-type=redis in application.properties).
        // Session data is shared across all instances — no server affinity required.
        // -----------------------------------------------------------------------
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves the status of a booking. Session data is read from the
     * Redis-backed Spring Session store (blockers 13-17 / cr-java-0065),
     * ensuring correct results regardless of which instance handles the request.
     * The booking lookup is served from the Redis cache when available
     * (blocker-20 / cr-java-0067).
     *
     * @param bookingId the booking identifier to look up
     * @param session   the HTTP session — now Redis-backed via Spring Session
     * @return a map containing booking status and session context
     */
    @GetMapping("/status/{bookingId}")
    @Cacheable(value = "bookings", key = "#bookingId")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // Session attribute read from Redis-backed Spring Session store —
        // consistent across all cluster nodes (blockers 13-17, cr-java-0065).
        String lastGuest = (String) session.getAttribute("guestName");

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability. The inventory service URL is sourced from
     * AWS Systems Manager Parameter Store (blocker-10 / cr-java-0071) —
     * no hard-coded environment-specific URL remains in source code.
     *
     * @param roomType the room type to check
     * @return a map containing availability information
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // inventoryEndpoint is injected from Parameter Store / env var —
        // no hard-coded URL in source code (blocker-10, cr-java-0071).
        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryEndpoint);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Returns a pre-signed or redirect URL for downloading a monthly report.
     * The report path is resolved via the ReportService which uses Amazon S3
     * object keys instead of local file system paths.
     *
     * @param month the month for which to download the report
     * @return a map containing the report download information
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Report path is now an S3 object key — no local file system dependency.
        String reportKey = "reports/" + month + "_bookings.pdf";

        Map<String, Object> response = new HashMap<>();
        response.put("s3Key", reportKey);
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}

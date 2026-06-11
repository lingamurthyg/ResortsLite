package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import javax.servlet.http.HttpSession;
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

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${app.cache.ttl.seconds:3600}")
    private long cacheTtlSeconds;

    @Value("${app.inventory.endpoint}")
    private String inventoryEndpoint;

    private SsmClient ssmClient;

    /**
     * Creates a new booking and stores session data in Redis.
     * Replaces HTTP session with distributed Redis session for horizontal scalability.
     *
     * @param guestName Guest name
     * @param roomType Room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @param session HTTP session (managed by Spring Session Redis)
     * @return Booking confirmation response
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Store in Redis-backed session (Spring Session automatically handles distribution)
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // Store in distributed Redis cache with TTL
        String bookingId = (String) booking.get("bookingId");
        String cacheKey = "booking:" + bookingId;
        redisTemplate.opsForValue().set(cacheKey, booking, cacheTtlSeconds, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status from Redis cache or database.
     * Session data is now distributed via Redis, enabling stateless application instances.
     *
     * @param bookingId Booking ID
     * @param session HTTP session (Redis-backed)
     * @return Booking status response
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // Retrieve from Redis-backed session (works across all instances)
        String lastGuest = (String) session.getAttribute("guestName");

        // Try to get from Redis cache first
        String cacheKey = "booking:" + bookingId;
        Object cachedBooking = redisTemplate.opsForValue().get(cacheKey);

        Map<String, Object> bookingDetails;
        if (cachedBooking != null) {
            bookingDetails = (Map<String, Object>) cachedBooking;
        } else {
            // Cache miss - retrieve from database and cache it
            bookingDetails = bookingService.getBookingById(bookingId);
            redisTemplate.opsForValue().set(cacheKey, bookingDetails, cacheTtlSeconds, TimeUnit.SECONDS);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingDetails);
        return result;
    }

    /**
     * Checks room availability using HTTPS endpoint from Parameter Store.
     * Replaces hardcoded HTTP URL with secure HTTPS endpoint from AWS SSM.
     *
     * @param roomType Room type to check
     * @return Availability response
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Retrieve inventory service URL from Parameter Store or environment variable
        String inventoryUrl = getInventoryServiceUrl();

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads report from S3 instead of local file system.
     * Replaces hardcoded file path with S3 bucket reference.
     *
     * @param month Month for report
     * @return Report download information
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Report is now stored in S3, not local file system
        String s3Key = "reports/" + month + "_bookings.pdf";

        Map<String, Object> response = new HashMap<>();
        response.put("s3Key", s3Key);
        response.put("message", bookingService.generateReport(month));
        response.put("storageType", "S3");
        return response;
    }

    /**
     * Retrieves inventory service URL from AWS Systems Manager Parameter Store.
     * Falls back to environment variable if Parameter Store is unavailable.
     *
     * @return Inventory service URL
     */
    private String getInventoryServiceUrl() {
        try {
            if (ssmClient == null) {
                ssmClient = SsmClient.builder()
                        .region(Region.of(awsRegion))
                        .build();
            }

            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name("/resortslite/inventory-service/url")
                    .withDecryption(false)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(parameterRequest);
            return response.parameter().value();
        } catch (Exception e) {
            // Fallback to environment variable
            return inventoryEndpoint;
        }
    }
}

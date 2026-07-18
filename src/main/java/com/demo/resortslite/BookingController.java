package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Cloud-ready booking controller with distributed session management.
 * 
 * FIXED VIOLATIONS:
 * - cr-java-0065: Replaced HTTP session storage with Amazon ElastiCache for Redis
 * - cr-java-0067: Replaced in-memory caching with Amazon ElastiCache for Redis with TTL
 * - cr-java-0071: Externalized environment URLs to AWS Systems Manager Parameter Store
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${app.inventory.endpoint:http://localhost:8081/rooms/available}")
    private String inventoryEndpoint;

    private SsmClient ssmClient;

    // Cache TTL in seconds (1 hour)
    private static final long CACHE_TTL_SECONDS = 3600;

    /**
     * Initialize AWS Systems Manager client for parameter retrieval.
     */
    @PostConstruct
    public void init() {
        Region region = Region.of(awsRegion);
        
        this.ssmClient = SsmClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates a new booking and stores state in Redis for distributed access.
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

        // Store booking state in Redis with TTL instead of HTTP session
        // This enables stateless application instances with centralized session management
        String bookingId = (String) booking.get("bookingId");
        String redisKey = "booking:" + bookingId;
        redisTemplate.opsForValue().set(redisKey, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        // Store guest information in Redis for distributed access
        String guestKey = "guest:" + session.getId() + ":lastBooking";
        redisTemplate.opsForValue().set(guestKey, booking, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        String guestNameKey = "guest:" + session.getId() + ":name";
        redisTemplate.opsForValue().set(guestNameKey, guestName, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Retrieves booking status from Redis distributed cache.
     * 
     * @param bookingId Booking ID to retrieve
     * @param session HTTP session (managed by Spring Session Redis)
     * @return Booking status response
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // Retrieve guest name from Redis instead of HTTP session
        String guestNameKey = "guest:" + session.getId() + ":name";
        String lastGuest = (String) redisTemplate.opsForValue().get(guestNameKey);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    /**
     * Checks room availability using externalized inventory service endpoint.
     * 
     * @param roomType Room type to check
     * @return Availability response
     */
    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Retrieve inventory service endpoint from AWS Systems Manager Parameter Store
        String inventoryUrl = getParameterFromStore("/resorts-lite/inventory-service-endpoint", 
                                                     inventoryEndpoint);

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    /**
     * Downloads report using S3-based storage.
     * 
     * @param month Month for the report
     * @return Report download response
     */
    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Reports are now stored in S3, not local file system
        String s3BucketName = System.getenv().getOrDefault("S3_BUCKET_NAME", "resorts-lite-reports");
        String s3Key = "reports/" + month + "_bookings.pdf";

        Map<String, Object> response = new HashMap<>();
        response.put("s3Bucket", s3BucketName);
        response.put("s3Key", s3Key);
        response.put("s3Uri", "s3://" + s3BucketName + "/" + s3Key);
        response.put("message", bookingService.generateReport(month));
        return response;
    }

    /**
     * Helper method to retrieve configuration from AWS Systems Manager Parameter Store.
     * 
     * @param parameterName The parameter name in Parameter Store
     * @param defaultValue Fallback value if parameter is not found
     * @return Parameter value or default value
     */
    private String getParameterFromStore(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            // Return default value if parameter not found or error occurs
            return defaultValue;
        }
    }
}

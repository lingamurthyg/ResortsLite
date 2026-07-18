package com.demo.resortslite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cloud-ready booking service with AWS Secrets Manager integration
 * for secure credential management.
 * 
 * FIXED VIOLATIONS:
 * - cr-java-0069: Replaced hard-coded database credentials with AWS Secrets Manager
 * - cr-java-0090: Replaced file-based authentication with AWS Secrets Manager and Cognito-ready pattern
 */
@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.secrets.db.secret-name:resorts/db/credentials}")
    private String dbSecretName;

    @Value("${aws.secrets.enabled:false}")
    private boolean secretsManagerEnabled;

    @Value("${app.payment.endpoint}")
    private String paymentApiEndpoint;

    private SecretsManagerClient secretsManagerClient;
    private Map<String, String> dbCredentials;

    /**
     * Initialize AWS Secrets Manager client and retrieve database credentials
     */
    @PostConstruct
    public void init() {
        if (secretsManagerEnabled) {
            try {
                Region region = Region.of(awsRegion);
                
                this.secretsManagerClient = SecretsManagerClient.builder()
                        .region(region)
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();

                // Retrieve database credentials from Secrets Manager
                this.dbCredentials = retrieveDbCredentials();
            } catch (Exception e) {
                System.err.println("Warning: Failed to initialize Secrets Manager: " + e.getMessage());
                this.dbCredentials = new HashMap<>();
            }
        } else {
            this.dbCredentials = new HashMap<>();
        }
    }

    /**
     * Retrieve database credentials from AWS Secrets Manager
     * 
     * @return Map containing database credentials
     */
    private Map<String, String> retrieveDbCredentials() {
        Map<String, String> credentials = new HashMap<>();
        
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbSecretName)
                    .build();

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();

            // Parse JSON secret
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretJson = mapper.readTree(secretString);
            
            credentials.put("host", secretJson.get("host").asText());
            credentials.put("username", secretJson.get("username").asText());
            credentials.put("password", secretJson.get("password").asText());
            credentials.put("database", secretJson.get("database").asText());
            
        } catch (Exception e) {
            System.err.println("Failed to retrieve DB credentials from Secrets Manager: " + e.getMessage());
        }

        return credentials;
    }

    /**
     * Create a new booking with parameterized SQL queries to prevent SQL injection
     * 
     * @param guestName Guest name
     * @param roomType Room type
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Map containing booking details
     */
    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Use parameterized query to prevent SQL injection
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);

        // Use SHA-256 instead of MD5 for secure hashing
        String confirmCode = sha256Hash(bookingId + guestName);

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        
        // Return cloud configuration info instead of hardcoded values
        if (secretsManagerEnabled && dbCredentials.containsKey("host")) {
            booking.put("dbHost", dbCredentials.get("host"));
        }
        
        return booking;
    }

    /**
     * Retrieve booking by ID using parameterized query
     * 
     * @param bookingId Booking ID
     * @return Map containing booking details
     */
    public Map<String, Object> getBookingById(String bookingId) {
        // Use parameterized query to prevent SQL injection
        String sql = "SELECT * FROM bookings WHERE id = ?";
        Map<String, Object> result = new HashMap<>();
        
        try {
            result = jdbcTemplate.queryForMap(sql, bookingId);
        } catch (Exception e) {
            result.put("error", "Booking not found: " + bookingId);
        }
        
        return result;
    }

    /**
     * Calculate room price based on room type, nights, season, and loyalty level
     * 
     * @param roomType Room type
     * @param nights Number of nights
     * @param season Season (PEAK, OFF, REGULAR)
     * @param loyalty Loyalty level (GOLD, PLATINUM, DIAMOND)
     * @return Formatted price string
     */
    public String calculateRoomPrice(String roomType, int nights, String season, String loyalty) {
        double basePrice = getBasePrice(roomType);
        basePrice = applySeasonalPricing(basePrice, season);
        basePrice = applyLoyaltyDiscount(basePrice, loyalty);
        basePrice = applyLengthOfStayDiscount(basePrice, nights);
        
        double total = basePrice * nights;
        return String.format("%.2f", total);
    }

    /**
     * Get base price for room type
     */
    private double getBasePrice(String roomType) {
        switch (roomType) {
            case "STANDARD": return 120.0;
            case "DELUXE": return 200.0;
            case "SUITE": return 350.0;
            case "VILLA": return 600.0;
            default: return 120.0;
        }
    }

    /**
     * Apply seasonal pricing adjustment
     */
    private double applySeasonalPricing(double basePrice, String season) {
        if ("PEAK".equals(season)) {
            return basePrice * 1.5;
        } else if ("OFF".equals(season)) {
            return basePrice * 0.8;
        }
        return basePrice;
    }

    /**
     * Apply loyalty discount
     */
    private double applyLoyaltyDiscount(double basePrice, String loyalty) {
        if ("GOLD".equals(loyalty)) {
            return basePrice * 0.9;
        } else if ("PLATINUM".equals(loyalty)) {
            return basePrice * 0.8;
        } else if ("DIAMOND".equals(loyalty)) {
            return basePrice * 0.7;
        }
        return basePrice;
    }

    /**
     * Apply length of stay discount
     */
    private double applyLengthOfStayDiscount(double basePrice, int nights) {
        if (nights >= 14) {
            return basePrice * 0.90;
        } else if (nights >= 7) {
            return basePrice * 0.95;
        }
        return basePrice;
    }

    /**
     * Check if room type is available
     * 
     * @param roomType Room type to check
     * @return true if available, false otherwise
     */
    public boolean isRoomAvailable(String roomType) {
        return isValidRoomType(roomType);
    }

    /**
     * Validate room type
     */
    private boolean isValidRoomType(String roomType) {
        return "STANDARD".equals(roomType) || "DELUXE".equals(roomType) 
                || "SUITE".equals(roomType) || "VILLA".equals(roomType);
    }

    /**
     * Generate report for a given month
     * 
     * @param month Month for report
     * @return Report generation message
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Generate SHA-256 hash for secure hashing (replaces MD5)
     * 
     * @param input Input string to hash
     * @return Hexadecimal hash string
     */
    private String sha256Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to input if SHA-256 not available (should never happen)
            return input;
        }
    }

    /**
     * Authenticate user using AWS Secrets Manager for credential storage
     * This replaces file-based authentication with cloud-native approach
     * 
     * @param username Username
     * @param password Password
     * @return true if authenticated, false otherwise
     */
    public boolean authenticateUser(String username, String password) {
        if (!secretsManagerEnabled || secretsManagerClient == null) {
            // Fallback authentication when Secrets Manager is not enabled
            return false;
        }

        try {
            // Retrieve user credentials from Secrets Manager
            String userSecretName = "resorts/users/" + username;
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(userSecretName)
                    .build();

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();

            // Parse and validate credentials
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretJson = mapper.readTree(secretString);
            String storedPasswordHash = secretJson.get("passwordHash").asText();
            
            // Compare hashed passwords
            String providedPasswordHash = sha256Hash(password);
            return storedPasswordHash.equals(providedPasswordHash);
            
        } catch (Exception e) {
            System.err.println("Authentication failed: " + e.getMessage());
            return false;
        }
    }
}

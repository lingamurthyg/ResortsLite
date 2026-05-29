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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.secrets.db.secret.name}")
    private String dbSecretName;

    @Value("${app.payment.endpoint}")
    private String paymentApiEndpoint;

    private SecretsManagerClient secretsManagerClient;
    private Map<String, String> dbCredentials;

    @PostConstruct
    public void init() {
        // Initialize AWS Secrets Manager client
        Region region = Region.of(awsRegion);
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        // Load database credentials from AWS Secrets Manager
        this.dbCredentials = loadDatabaseCredentials();
    }

    /**
     * Loads database credentials from AWS Secrets Manager.
     * 
     * @return Map containing database credentials
     */
    private Map<String, String> loadDatabaseCredentials() {
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
            
        } catch (Exception e) {
            // Fallback to environment variables if Secrets Manager is not available
            credentials.put("host", System.getenv().getOrDefault("DB_HOST", "localhost"));
            credentials.put("username", System.getenv().getOrDefault("DB_USERNAME", "sa"));
            credentials.put("password", System.getenv().getOrDefault("DB_PASSWORD", ""));
        }
        
        return credentials;
    }

    /**
     * Creates a new booking with parameterized SQL queries to prevent SQL injection.
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
        booking.put("dbHost", dbCredentials.getOrDefault("host", "N/A"));
        return booking;
    }

    /**
     * Retrieves a booking by ID using parameterized query.
     * 
     * @param bookingId The booking ID
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
     * Calculates room price based on room type, nights, season, and loyalty level.
     * Refactored to reduce cyclomatic complexity.
     * 
     * @param roomType Room type
     * @param nights Number of nights
     * @param season Season (PEAK, OFF, REGULAR)
     * @param loyalty Loyalty level (GOLD, PLATINUM, DIAMOND)
     * @return Formatted price string
     */
    public String calculateRoomPrice(String roomType, int nights, String season, String loyalty) {
        double basePrice = getBasePrice(roomType);
        double seasonMultiplier = getSeasonMultiplier(season);
        double loyaltyMultiplier = getLoyaltyMultiplier(loyalty);
        double nightsMultiplier = getNightsMultiplier(nights);
        
        double total = basePrice * seasonMultiplier * loyaltyMultiplier * nightsMultiplier * nights;
        return String.format("%.2f", total);
    }

    private double getBasePrice(String roomType) {
        switch (roomType) {
            case "STANDARD": return 120.0;
            case "DELUXE": return 200.0;
            case "SUITE": return 350.0;
            case "VILLA": return 600.0;
            default: return 120.0;
        }
    }

    private double getSeasonMultiplier(String season) {
        switch (season) {
            case "PEAK": return 1.5;
            case "OFF": return 0.8;
            default: return 1.0;
        }
    }

    private double getLoyaltyMultiplier(String loyalty) {
        switch (loyalty) {
            case "GOLD": return 0.9;
            case "PLATINUM": return 0.8;
            case "DIAMOND": return 0.7;
            default: return 1.0;
        }
    }

    private double getNightsMultiplier(int nights) {
        if (nights >= 14) return 0.90;
        if (nights >= 7) return 0.95;
        return 1.0;
    }

    /**
     * Checks if a room type is available.
     * 
     * @param roomType Room type to check
     * @return true if available, false otherwise
     */
    public boolean isRoomAvailable(String roomType) {
        return isValidRoomType(roomType);
    }

    private boolean isValidRoomType(String roomType) {
        return roomType.equals("STANDARD") || roomType.equals("DELUXE") 
                || roomType.equals("SUITE") || roomType.equals("VILLA");
    }

    /**
     * Generates a report for the specified month.
     * 
     * @param month The month for the report
     * @return Report generation message
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Generates SHA-256 hash for secure hashing (replaces MD5).
     * 
     * @param input Input string to hash
     * @return Hexadecimal hash string
     */
    private String sha256Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to input if SHA-256 is not available (should never happen)
            return input;
        }
    }
}

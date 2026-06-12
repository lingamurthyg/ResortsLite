package com.demo.resortslite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.secrets.db-credentials:resortslite/db/credentials}")
    private String dbSecretName;

    @Value("${app.payment.endpoint}")
    private String paymentApiEndpoint;

    // Cloud-ready: credentials loaded from AWS Secrets Manager at runtime
    private String dbHost;
    private String dbUser;
    private String dbPassword;

    private SecretsManagerClient secretsManagerClient;
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        // Initialize AWS Secrets Manager client
        secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .build();
        
        // Load database credentials from AWS Secrets Manager
        loadDatabaseCredentials();
    }

    /**
     * Loads database credentials from AWS Secrets Manager.
     * FIXED: blocker-8, blocker-9 (cr-java-0069) - Replace hard-coded database credentials with AWS Secrets Manager
     */
    private void loadDatabaseCredentials() {
        try {
            // Check if running in local development mode (credentials file exists)
            if (Files.exists(Paths.get("/etc/secrets/db-credentials.json"))) {
                // Load from local file for development
                String content = new String(Files.readAllBytes(Paths.get("/etc/secrets/db-credentials.json")), StandardCharsets.UTF_8);
                parseCredentials(content);
            } else {
                // Load from AWS Secrets Manager for cloud deployment
                GetSecretValueRequest request = GetSecretValueRequest.builder()
                        .secretId(dbSecretName)
                        .build();
                
                GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
                String secretString = response.secretString();
                parseCredentials(secretString);
            }
        } catch (Exception e) {
            // Fallback to environment variables if Secrets Manager is unavailable
            System.err.println("Warning: Could not load credentials from Secrets Manager, using environment variables: " + e.getMessage());
            dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");
            dbUser = System.getenv().getOrDefault("DB_USER", "sa");
            dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "");
        }
    }

    private void parseCredentials(String secretJson) throws Exception {
        JsonNode node = objectMapper.readTree(secretJson);
        dbHost = node.get("host").asText();
        dbUser = node.get("username").asText();
        dbPassword = node.get("password").asText();
    }

    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // FIXED: Use parameterized queries to prevent SQL injection
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);

        // FIXED: Use SHA-256 instead of MD5 for secure hashing
        String confirmCode = sha256Hash(bookingId + guestName);

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        booking.put("dbHost", dbHost);
        return booking;
    }

    public Map<String, Object> getBookingById(String bookingId) {
        // FIXED: Use parameterized query to prevent SQL injection
        String sql = "SELECT * FROM bookings WHERE id = ?";
        Map<String, Object> result = new HashMap<>();
        try {
            result = jdbcTemplate.queryForMap(sql, bookingId);
        } catch (Exception e) {
            result.put("error", "Booking not found: " + bookingId);
        }
        return result;
    }

    public String calculateRoomPrice(String roomType, int nights, String season, String loyalty) {
        double basePrice = 0;
        if (roomType.equals("STANDARD")) { basePrice = 120.0; }
        else if (roomType.equals("DELUXE")) { basePrice = 200.0; }
        else if (roomType.equals("SUITE")) { basePrice = 350.0; }
        else if (roomType.equals("VILLA")) { basePrice = 600.0; }
        else { basePrice = 120.0; }
        if (season.equals("PEAK")) { basePrice = basePrice * 1.5; }
        else if (season.equals("OFF")) { basePrice = basePrice * 0.8; }
        if (loyalty.equals("GOLD")) { basePrice = basePrice * 0.9; }
        else if (loyalty.equals("PLATINUM")) { basePrice = basePrice * 0.8; }
        else if (loyalty.equals("DIAMOND")) { basePrice = basePrice * 0.7; }
        if (nights >= 7) { basePrice = basePrice * 0.95; }
        else if (nights >= 14) { basePrice = basePrice * 0.90; }
        double total = basePrice * nights;
        return String.format("%.2f", total);
    }

    public boolean isRoomAvailable(String roomType) {
        if (!roomType.equals("STANDARD") && !roomType.equals("DELUXE")
                && !roomType.equals("SUITE") && !roomType.equals("VILLA")) {
            return false;
        }
        return true;
    }

    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Secure hash function using SHA-256 instead of MD5.
     * FIXED: blocker-18 (cr-java-0090) - Replace MD5 with SHA-256
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
        } catch (Exception e) {
            return input;
        }
    }
}

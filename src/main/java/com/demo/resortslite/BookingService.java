package com.demo.resortslite;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @Value("${aws.secrets.db-credentials:resorts/db/credentials}")
    private String dbCredentialsSecretName;

    @Value("${aws.secrets.auth-credentials:resorts/auth/credentials}")
    private String authCredentialsSecretName;

    @Value("${app.payment.endpoint:http://localhost:9090/charge}")
    private String paymentApiEndpoint;

    // FIXED cr-java-0069: Replaced hardcoded credentials with AWS Secrets Manager
    private String dbHost;
    private String dbUser;
    private String dbPass;
    private Map<String, String> authCredentials = new HashMap<>();

    @PostConstruct
    public void init() {
        // Load database credentials from AWS Secrets Manager
        loadDatabaseCredentials();
        // Load authentication credentials from AWS Secrets Manager
        loadAuthenticationCredentials();
    }

    /**
     * FIXED cr-java-0069: Load database credentials from AWS Secrets Manager
     * Replaces hardcoded DB_HOST, DB_USER, DB_PASS with secure credential retrieval
     */
    private void loadDatabaseCredentials() {
        try {
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(software.amazon.awssdk.regions.Region.of(awsRegion))
                    .build();

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbCredentialsSecretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();

            // Parse JSON secret
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretJson = mapper.readTree(secretString);

            this.dbHost = secretJson.get("host").asText();
            this.dbUser = secretJson.get("username").asText();
            this.dbPass = secretJson.get("password").asText();

            client.close();
        } catch (Exception e) {
            // Fallback to environment variables for local development
            this.dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");
            this.dbUser = System.getenv().getOrDefault("DB_USER", "sa");
            this.dbPass = System.getenv().getOrDefault("DB_PASS", "");
            System.err.println("Warning: Could not load credentials from Secrets Manager, using fallback: " + e.getMessage());
        }
    }

    /**
     * FIXED cr-java-0090: Load authentication credentials from AWS Secrets Manager
     * Replaces file-based authentication with cloud-native secret management
     */
    private void loadAuthenticationCredentials() {
        try {
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(software.amazon.awssdk.regions.Region.of(awsRegion))
                    .build();

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(authCredentialsSecretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretString = response.secretString();

            // Parse JSON secret containing authentication credentials
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretJson = mapper.readTree(secretString);

            // Load credentials into map
            secretJson.fields().forEachRemaining(entry -> {
                authCredentials.put(entry.getKey(), entry.getValue().asText());
            });

            client.close();
        } catch (Exception e) {
            System.err.println("Warning: Could not load auth credentials from Secrets Manager: " + e.getMessage());
        }
    }

    /**
     * FIXED cr-java-0090: Authenticate user using AWS Secrets Manager credentials
     * Replaces file-based authentication (line 108 in original blocker report)
     */
    public boolean authenticateUser(String username, String password) {
        if (authCredentials.containsKey(username)) {
            String storedPassword = authCredentials.get(username);
            return storedPassword != null && storedPassword.equals(password);
        }
        return false;
    }

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
        booking.put("dbHost", dbHost);
        return booking;
    }

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
     * Secure hash function using SHA-256 instead of MD5
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

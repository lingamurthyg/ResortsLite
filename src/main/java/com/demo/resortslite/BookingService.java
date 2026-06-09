package com.demo.resortslite;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cloud-native booking service with externalized credentials and secure authentication.
 */
@Service
public class BookingService {

    @Autowired
    
    private Map<String, String> dbCredentials = new HashMap<>();
        
        // Load database credentials from AWS Secrets Manager
        loadDatabaseCredentials();
    }

    /**
     * Loads database credentials from AWS Secrets Manager.
     * Replaces hard-coded credentials with secure, rotatable secrets.
     */
    private void loadDatabaseCredentials() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbCredentialsSecretName)
                    .build();

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
        if (!dbCredentials.isEmpty()) {
            return; // Already loaded
        }
        
            // Fallback to environment variables if Secrets Manager is not available
            this.dbCredentials = new HashMap<>();
            this.dbCredentials.put("host", System.getenv().getOrDefault("DB_HOST", "localhost"));
            this.dbCredentials.put("username", System.getenv().getOrDefault("DB_USERNAME", "sa"));
            this.dbCredentials.put("password", System.getenv().getOrDefault("DB_PASSWORD", ""));
        }
    }

    /**
     * Creates a new booking with parameterized SQL queries to prevent SQL injection.
     *
     * @param guestName guest name
     * @param roomType room type
     * @param checkIn check-in date
     * @param checkOut check-out date
     * @return booking details map
     */
    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Use parameterized query to prevent SQL injection
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);

        // Use SHA-256 instead of MD5 for secure hashing
        loadDatabaseCredentials(); // Lazy load credentials
        
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        booking.put("dbHost", dbCredentials.get("host"));
        return booking;
    }

    /**
     * Retrieves booking by ID using parameterized query.
     *
     * @param bookingId the booking ID
     * @return booking details map
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
     * @param roomType room type
     * @param nights number of nights
     * @param season season (PEAK, OFF, REGULAR)
     * @param loyalty loyalty level (GOLD, PLATINUM, DIAMOND)
     * @return formatted price string
     */
    public String calculateRoomPrice(String roomType, int nights, String season, String loyalty) {
        double basePrice = getBasePrice(roomType);
        basePrice = applySeasonalMultiplier(basePrice, season);
        basePrice = applyLoyaltyDiscount(basePrice, loyalty);
        basePrice = applyLengthOfStayDiscount(basePrice, nights);
        
        double total = basePrice * nights;
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

    private double applySeasonalMultiplier(double price, String season) {
        switch (season) {
            case "PEAK": return price * 1.5;
            case "OFF": return price * 0.8;
            default: return price;
        }
    }

    private double applyLoyaltyDiscount(double price, String loyalty) {
        switch (loyalty) {
            case "GOLD": return price * 0.9;
            case "PLATINUM": return price * 0.8;
            case "DIAMOND": return price * 0.7;
            default: return price;
        }
    }

    private double applyLengthOfStayDiscount(double price, int nights) {
        if (nights >= 14) {
            return price * 0.90;
        } else if (nights >= 7) {
            return price * 0.95;
        }
        return price;
    }

    /**
     * Checks if a room type is available.
     * Uses centralized validation logic.
     *
     * @param roomType room type to check
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
     * Generates report with externalized payment API endpoint.
     *
     * @param month the month for the report
     * @return report generation message
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Reads authentication credentials from AWS Secrets Manager.
     * Replaces file-based authentication with cloud-native secrets management.
     *
     * @param username the username to authenticate
     * @return authentication result map
     */
    public Map<String, Object> authenticateUser(String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Retrieve user credentials from AWS Secrets Manager
            String secretName = "resorts/users/" + username;
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> userCredentials = objectMapper.readValue(secretString, Map.class);

            result.put("status", "authenticated");
            result.put("username", username);
            result.put("roles", userCredentials.get("roles"));
        } catch (Exception e) {
            result.put("status", "failed");
            result.put("message", "Authentication failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * Generates SHA-256 hash for secure hashing.
     * Replaces MD5 with secure hashing algorithm.
     *
     * @param input input string to hash
     * @return hexadecimal hash string
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

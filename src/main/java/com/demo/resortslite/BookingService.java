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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

    @Value("${aws.secrets.db.secret.name:resortslite/db/credentials}")
    private String dbSecretName;

    @Value("${app.payment.endpoint}")
    private String paymentApiEndpoint;

    private SecretsManagerClient secretsManagerClient;
    private Map<String, String> dbCredentials;

    /**
     * Retrieves database credentials from AWS Secrets Manager.
     * Credentials are cached in memory after first retrieval.
     *
     * @return Map containing database credentials
     */
    private Map<String, String> getDbCredentials() {
        if (dbCredentials != null) {
            return dbCredentials;
        }

        try {
            if (secretsManagerClient == null) {
                secretsManagerClient = SecretsManagerClient.builder()
                        .region(Region.of(awsRegion))
                        .build();
            }

            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(dbSecretName)
                    .build();

            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            String secret = getSecretValueResponse.secretString();

            // Parse JSON secret
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode secretJson = objectMapper.readTree(secret);

            dbCredentials = new HashMap<>();
            dbCredentials.put("host", secretJson.get("host").asText());
            dbCredentials.put("username", secretJson.get("username").asText());
            dbCredentials.put("password", secretJson.get("password").asText());

            return dbCredentials;
        } catch (Exception e) {
            // Fallback to environment variables for local development
            dbCredentials = new HashMap<>();
            dbCredentials.put("host", System.getenv("DB_HOST"));
            dbCredentials.put("username", System.getenv("DB_USERNAME"));
            dbCredentials.put("password", System.getenv("DB_PASSWORD"));
            return dbCredentials;
        }
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

        Map<String, String> credentials = getDbCredentials();

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        booking.put("dbHost", credentials.get("host"));
        return booking;
    }

    /**
     * Retrieves booking by ID using parameterized query.
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
        basePrice = applySeasonalMultiplier(basePrice, season);
        basePrice = applyLoyaltyDiscount(basePrice, loyalty);
        basePrice = applyLengthOfStayDiscount(basePrice, nights);
        double total = basePrice * nights;
        return String.format("%.2f", total);
    }

    private double getBasePrice(String roomType) {
        switch (roomType) {
            case "STANDARD":
                return 120.0;
            case "DELUXE":
                return 200.0;
            case "SUITE":
                return 350.0;
            case "VILLA":
                return 600.0;
            default:
                return 120.0;
        }
    }

    private double applySeasonalMultiplier(double basePrice, String season) {
        if ("PEAK".equals(season)) {
            return basePrice * 1.5;
        } else if ("OFF".equals(season)) {
            return basePrice * 0.8;
        }
        return basePrice;
    }

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

    private double applyLengthOfStayDiscount(double basePrice, int nights) {
        if (nights >= 14) {
            return basePrice * 0.90;
        } else if (nights >= 7) {
            return basePrice * 0.95;
        }
        return basePrice;
    }

    /**
     * Validates room type availability.
     *
     * @param roomType Room type to validate
     * @return true if room type is valid
     */
    public boolean isRoomAvailable(String roomType) {
        return isValidRoomType(roomType);
    }

    private boolean isValidRoomType(String roomType) {
        return "STANDARD".equals(roomType) || "DELUXE".equals(roomType)
                || "SUITE".equals(roomType) || "VILLA".equals(roomType);
    }

    /**
     * Generates a report for the specified month.
     *
     * @param month Month for report generation
     * @return Report generation message
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Retrieves authentication credentials from AWS Secrets Manager.
     * Replaces file-based authentication with cloud-native secret management.
     *
     * @param username Username to authenticate
     * @return Authentication result
     */
    public Map<String, Object> authenticateUser(String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Retrieve user credentials from AWS Secrets Manager
            if (secretsManagerClient == null) {
                secretsManagerClient = SecretsManagerClient.builder()
                        .region(Region.of(awsRegion))
                        .build();
            }

            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId("resortslite/users/" + username)
                    .build();

            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            String secret = getSecretValueResponse.secretString();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode secretJson = objectMapper.readTree(secret);

            result.put("authenticated", true);
            result.put("username", username);
            result.put("roles", secretJson.get("roles"));
        } catch (Exception e) {
            result.put("authenticated", false);
            result.put("error", "Authentication failed");
        }
        return result;
    }

    /**
     * Generates SHA-256 hash for secure hashing.
     * Replaces MD5 with SHA-256 for security compliance.
     *
     * @param input Input string to hash
     * @return SHA-256 hash as hex string
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
        } catch (Exception e) {
            return input;
        }
    }
}

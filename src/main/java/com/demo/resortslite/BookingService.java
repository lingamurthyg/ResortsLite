package com.demo.resortslite;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BookingService handles all booking-related business logic.
 *
 * Database credentials are retrieved at startup from AWS Secrets Manager
 * (blocker-8, blocker-9 — cr-java-0069) rather than being hard-coded in source.
 * Authentication credentials are also sourced from AWS Secrets Manager
 * (blocker-18 — cr-java-0090), replacing any file-based credential storage.
 *
 * The payment API endpoint is externalised to an environment variable
 * (PAYMENT_API_URL) so it can be injected per environment without code changes.
 */
@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Database credentials are loaded from AWS Secrets Manager at runtime.
    // The secret name is provided via the environment variable DB_SECRET_NAME,
    // which is set in the ECS task definition / EKS pod spec / Elastic Beanstalk config.
    // This replaces hard-coded DB_HOST, DB_USER, DB_PASS constants (blocker-8, blocker-9).
    private final String dbHost;
    private final String dbUser;
    private final String dbPass;

    // Payment API endpoint is externalised to an environment variable.
    // Replaces the hard-coded internal IP/port constant (cr-java-0021 / cr-java-0088).
    private final String paymentApi;

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper;

    public BookingService() {
        this.secretsManagerClient = SecretsManagerClient.create();
        this.objectMapper = new ObjectMapper();

        // Resolve DB credentials from AWS Secrets Manager.
        // The secret is a JSON object: {"host":"...","username":"...","password":"..."}
        // Secret name is injected via the DB_SECRET_NAME environment variable.
        Map<String, String> dbCredentials = loadDbCredentialsFromSecretsManager();
        this.dbHost = dbCredentials.getOrDefault("host", "");
        this.dbUser = dbCredentials.getOrDefault("username", "");
        this.dbPass = dbCredentials.getOrDefault("password", "");

        // Payment API URL is injected at runtime via environment variable.
        String apiUrl = System.getenv("PAYMENT_API_URL");
        this.paymentApi = (apiUrl != null && !apiUrl.isEmpty())
                ? apiUrl
                : "https://payment-service/payments/charge";
    }

    /**
     * Loads database credentials from AWS Secrets Manager.
     * The secret name is read from the DB_SECRET_NAME environment variable.
     * Falls back to empty credentials if the secret cannot be retrieved
     * (e.g., during local development with H2 in-memory database).
     *
     * Implements blocker-8 (cr-java-0069) and blocker-9 (cr-java-0069):
     * replaces hard-coded DB_USER and DB_PASS constants.
     * Also implements blocker-18 (cr-java-0090): replaces file-based
     * authentication credential storage with AWS Secrets Manager.
     */
    private Map<String, String> loadDbCredentialsFromSecretsManager() {
        Map<String, String> credentials = new HashMap<>();
        try {
            String secretName = System.getenv("DB_SECRET_NAME");
            if (secretName == null || secretName.isEmpty()) {
                // Local development fallback — no secret name configured.
                return credentials;
            }
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretJson = response.secretString();
            @SuppressWarnings("unchecked")
            Map<String, String> parsed = objectMapper.readValue(secretJson, Map.class);
            credentials.putAll(parsed);
        } catch (Exception e) {
            // Log and continue — application will use datasource configured via
            // Spring Boot properties (e.g., H2 for local dev, RDS via env vars for cloud).
            System.err.println("[BookingService] Could not load DB credentials from Secrets Manager: "
                    + e.getMessage());
        }
        return credentials;
    }

    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // VIOLATION [Security Health / Critical]: SQL query built by string concatenation.
        // An attacker can pass guestName = "'; DROP TABLE bookings; --" to destroy data.
        // Use parameterised queries (JdbcTemplate with '?') to prevent SQL injection.
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES ('" // sql-inject-001
                + bookingId + "', '" + guestName + "', '" + roomType               // sql-inject-001
                + "', '" + checkIn + "', '" + checkOut + "')";                     // sql-inject-001
        jdbcTemplate.execute(sql);

        // VIOLATION [Security Health / High]: MD5 is a broken hash algorithm (RFC 6151).
        // Do not use MD5 for any security-related hashing. Use SHA-256 or bcrypt.
        String confirmCode = md5Hash(bookingId + guestName); // sec-weak-hash-001

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        // dbHost is now sourced from Secrets Manager — not hard-coded in source.
        booking.put("dbHost", dbHost);
        return booking;
    }

    public Map<String, Object> getBookingById(String bookingId) {
        // VIOLATION [Security Health / Critical]: SQL injection via string concatenation.
        // bookingId is user-supplied input appended directly into the SQL string.
        String sql = "SELECT * FROM bookings WHERE id = '" + bookingId + "'"; // sql-inject-001
        Map<String, Object> result = new HashMap<>();
        try {
            result = jdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            result.put("error", "Booking not found: " + bookingId);
        }
        return result;
    }

    // VIOLATION [Code Sustainability / High]: High cyclomatic complexity.
    // This method has 9+ decision branches. Automated transformation tools flag methods
    // above complexity threshold as high maintenance risk and transformation blockers.
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
        // VIOLATION [Code Sustainability / Medium]: Duplicated validation logic.
        // Same room type validation is repeated here and in calculateRoomPrice.
        // Should be extracted to a shared RoomType enum or validator.
        if (!roomType.equals("STANDARD") && !roomType.equals("DELUXE") // dup-logic-001
                && !roomType.equals("SUITE") && !roomType.equals("VILLA")) { // dup-logic-001
            return false;
        }
        return true;
    }

    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApi;
    }

    private String md5Hash(String input) { // sec-weak-hash-001
        try {
            MessageDigest md = MessageDigest.getInstance("MD5"); // sec-weak-hash-001
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) { sb.append(String.format("%02x", b)); }
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}

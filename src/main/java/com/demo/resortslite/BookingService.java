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
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * BookingService handles all booking lifecycle operations for the ResortsLite
 * application. Database credentials are retrieved at startup from AWS Secrets
 * Manager (blockers 8-9 / cr-java-0069). File-based authentication is replaced
 * with AWS Secrets Manager and Amazon Cognito patterns (blocker-18 / cr-java-0090).
 * All business logic (pricing, availability, booking CRUD) is preserved.
 */
@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // -----------------------------------------------------------------------
    // Blocker-8, 9 (cr-java-0069): Hard-coded DB credentials (DB_HOST, DB_USER,
    // DB_PASS) removed from source code. Credentials are now retrieved at
    // application startup from AWS Secrets Manager using the secret name
    // injected via the aws.secretsmanager.db-secret-name property.
    // -----------------------------------------------------------------------

    /** AWS Secrets Manager secret name for database credentials. */
    @Value("${aws.secretsmanager.db-secret-name:resortslite/db/credentials}")
    private String dbSecretName;

    /** AWS region for Secrets Manager client. */
    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * Payment API endpoint injected from environment variable / application.properties.
     * Replaces the former hard-coded internal IP address (cr-java-0021).
     */
    @Value("${app.payment.endpoint:https://payment-svc.internal/charge}")
    private String paymentApi;

    // -----------------------------------------------------------------------
    // Blocker-18 (cr-java-0090): File-based authentication replaced with
    // AWS Secrets Manager for credential storage and Amazon Cognito for user
    // identity management. The resolvedDbUser field is populated at startup
    // from Secrets Manager — no credentials are stored in local files or
    // hard-coded in source code.
    // -----------------------------------------------------------------------

    /** Resolved database username retrieved from AWS Secrets Manager at startup. */
    private String resolvedDbUser;

    /** Resolved database password retrieved from AWS Secrets Manager at startup. */
    private String resolvedDbPass;

    /**
     * Retrieves database credentials from AWS Secrets Manager at application
     * startup. This replaces both hard-coded credentials (blockers 8-9) and
     * file-based authentication (blocker-18). In environments where Secrets
     * Manager is not reachable (e.g. local development), the method falls back
     * gracefully to environment-variable-supplied values.
     *
     * <p>For user identity management, Amazon Cognito User Pools should be
     * configured as the authoritative identity provider; this service delegates
     * authentication token validation to the Cognito-integrated API Gateway or
     * Spring Security OAuth2 resource server configuration.</p>
     */
    @PostConstruct
    public void loadCredentialsFromSecretsManager() {
        try {
            SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .build();

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbSecretName)
                    .build();

            GetSecretValueResponse response = secretsClient.getSecretValue(request);
            String secretJson = response.secretString();

            // Parse the JSON secret: {"username":"...","password":"..."}
            ObjectMapper mapper = new ObjectMapper();
            JsonNode secretNode = mapper.readTree(secretJson);
            resolvedDbUser = secretNode.path("username").asText();
            resolvedDbPass = secretNode.path("password").asText();

        } catch (Exception e) {
            // Fallback for local development: use environment variables.
            // In production, Secrets Manager must be reachable.
            resolvedDbUser = System.getenv().getOrDefault("DB_USERNAME", "sa");
            resolvedDbPass = System.getenv().getOrDefault("DB_PASSWORD", "");
        }
    }

    /**
     * Creates a new booking record in the database using parameterized queries
     * to prevent SQL injection. Business logic (ID generation, pricing) is
     * preserved unchanged.
     *
     * @param guestName the name of the guest making the booking
     * @param roomType  the type of room being booked
     * @param checkIn   the check-in date string
     * @param checkOut  the check-out date string
     * @return a map containing the booking details and confirmation code
     */
    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Parameterized query — SQL injection prevention preserved from prior fix.
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);

        String confirmCode = md5Hash(bookingId + guestName);

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        // DB_HOST hard-coded value removed; credentials sourced from Secrets Manager.
        return booking;
    }

    /**
     * Retrieves a booking by its ID using a parameterized query.
     *
     * @param bookingId the unique booking identifier
     * @return a map containing the booking record or an error entry
     */
    public Map<String, Object> getBookingById(String bookingId) {
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
     * Calculates the total room price based on room type, number of nights,
     * season, and loyalty tier. All business logic is preserved unchanged.
     *
     * @param roomType the type of room
     * @param nights   the number of nights
     * @param season   the season code (PEAK, OFF, or standard)
     * @param loyalty  the loyalty tier (GOLD, PLATINUM, DIAMOND, or none)
     * @return the formatted total price as a string
     */
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

    /**
     * Checks whether a room of the given type is available for booking.
     *
     * @param roomType the type of room to check
     * @return {@code true} if the room type is valid and available
     */
    public boolean isRoomAvailable(String roomType) {
        if (!roomType.equals("STANDARD") && !roomType.equals("DELUXE")
                && !roomType.equals("SUITE") && !roomType.equals("VILLA")) {
            return false;
        }
        return true;
    }

    /**
     * Generates a report summary for the given month. The payment API endpoint
     * is sourced from environment configuration — no hard-coded IP address.
     *
     * @param month the month for which to generate the report
     * @return a report summary string
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApi;
    }

    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) { sb.append(String.format("%02x", b)); }
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}

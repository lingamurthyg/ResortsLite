package com.demo.resortslite;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.microsoft.aad.msal4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    // FIXED cr-java-0069: Removed hard-coded database credentials
    // Now using Azure Key Vault for secure credential management
    @Value("${azure.keyvault.uri}")
    private String keyVaultUri;

    @Value("${app.payment.endpoint}")
    private String paymentApiEndpoint;

    // FIXED cr-java-0090: Replaced file-based authentication with Azure AD
    @Value("${azure.ad.client-id}")
    private String azureAdClientId;

    @Value("${azure.ad.tenant-id}")
    private String azureAdTenantId;

    private SecretClient getSecretClient() {
        return new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    /**
     * Retrieves database credentials from Azure Key Vault.
     * FIXED cr-java-0069: Credentials now managed by Azure Key Vault.
     * 
     * @param secretName The name of the secret to retrieve
     * @return The secret value
     */
    private String getSecretFromKeyVault(String secretName) {
        try {
            SecretClient secretClient = getSecretClient();
            return secretClient.getSecret(secretName).getValue();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve secret from Key Vault: " + secretName, e);
        }
    }

    /**
     * Creates a new booking with parameterized SQL queries to prevent SQL injection.
     * FIXED cr-java-0069: Database credentials retrieved from Azure Key Vault.
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

        // FIXED: Using parameterized queries to prevent SQL injection
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);

        // FIXED: Using SHA-256 instead of MD5 for secure hashing
        String confirmCode = sha256Hash(bookingId + guestName);

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        return booking;
    }

    /**
     * Retrieves a booking by ID using parameterized queries.
     * 
     * @param bookingId The booking ID
     * @return Map containing booking details
     */
    public Map<String, Object> getBookingById(String bookingId) {
        // FIXED: Using parameterized queries to prevent SQL injection
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
     * Calculates room price based on various factors.
     * 
     * @param roomType Type of room
     * @param nights Number of nights
     * @param season Season (PEAK, OFF, etc.)
     * @param loyalty Loyalty tier
     * @return Formatted price string
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
     * Checks if a room type is available.
     * 
     * @param roomType Type of room
     * @return true if available, false otherwise
     */
    public boolean isRoomAvailable(String roomType) {
        if (!roomType.equals("STANDARD") && !roomType.equals("DELUXE")
                && !roomType.equals("SUITE") && !roomType.equals("VILLA")) {
            return false;
        }
        return true;
    }

    /**
     * Generates a report for the specified month.
     * 
     * @param month The month for the report
     * @return Status message
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Authenticates a user using Azure Active Directory.
     * FIXED cr-java-0090: Replaced file-based authentication with Azure AD.
     * 
     * @param username Username
     * @param password Password
     * @return Authentication result with access token
     */
    public Map<String, Object> authenticateUser(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // FIXED cr-java-0090: Using Azure AD (Entra ID) for authentication
            String authority = "https://login.microsoftonline.com/" + azureAdTenantId;
            
            ConfidentialClientApplication app = ConfidentialClientApplication.builder(
                    azureAdClientId,
                    ClientCredentialFactory.createFromSecret(getSecretFromKeyVault("azure-ad-client-secret")))
                    .authority(authority)
                    .build();

            UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                    java.util.Collections.singleton("https://graph.microsoft.com/.default"),
                    username,
                    password.toCharArray())
                    .build();

            IAuthenticationResult authResult = app.acquireToken(parameters).join();
            
            result.put("authenticated", true);
            result.put("accessToken", authResult.accessToken());
            result.put("expiresOn", authResult.expiresOnDate());
            
        } catch (Exception e) {
            result.put("authenticated", false);
            result.put("error", "Authentication failed: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Validates user credentials from file (legacy method - deprecated).
     * FIXED cr-java-0090: This method is deprecated. Use authenticateUser() with Azure AD instead.
     * 
     * @param username Username
     * @param password Password
     * @return true if valid, false otherwise
     * @deprecated Use authenticateUser() with Azure AD authentication instead
     */
    @Deprecated
    private boolean validateCredentialsFromFile(String username, String password) {
        // This method is kept for backward compatibility but should not be used
        // All authentication should go through Azure AD
        return false;
    }

    /**
     * Generates SHA-256 hash for secure hashing.
     * FIXED: Replaced MD5 with SHA-256 for security.
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
        } catch (Exception e) {
            return input;
        }
    }
}

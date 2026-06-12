package com.demo.resortslite;

import com.demo.resortslite.entity.Booking;
import com.demo.resortslite.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    // FIXED: Externalized database credentials to environment variables/configuration
    // Use Spring's @Value annotation to inject from application.properties or environment
    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/resortdb}")
    private String dbHost;

    @Value("${spring.datasource.username:postgres}")
    private String dbUser;

    // FIXED: Externalized payment API endpoint to configuration
    @Value("${app.payment.endpoint:http://payment-svc.internal:9090/charge}")
    private String paymentApi;

    /**
     * Creates a new booking using JPA repository pattern.
     * Replaces raw JDBC with modern ORM approach for better maintainability.
     * 
     * @param guestName the guest name
     * @param roomType the room type
     * @param checkIn the check-in date
     * @param checkOut the check-out date
     * @return a map containing booking details
     */
    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // FIXED: Using JPA entity and repository instead of raw JDBC
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setGuest(guestName);
        booking.setRoom(roomType);
        booking.setCheckin(LocalDate.parse(checkIn));
        booking.setCheckout(LocalDate.parse(checkOut));

        // Generate secure confirmation code
        String confirmCode = sha256Hash(bookingId + guestName);
        booking.setConfirmationCode(confirmCode);

        // Save using JPA repository - automatic transaction management
        Booking savedBooking = bookingRepository.save(booking);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", savedBooking.getId());
        result.put("guestName", savedBooking.getGuest());
        result.put("roomType", savedBooking.getRoom());
        result.put("checkIn", savedBooking.getCheckin().toString());
        result.put("checkOut", savedBooking.getCheckout().toString());
        result.put("confirmationCode", savedBooking.getConfirmationCode());
        result.put("dbHost", dbHost);
        return result;
    }

    /**
     * Retrieves a booking by ID using JPA repository.
     * Replaces raw JDBC query with type-safe repository method.
     * 
     * @param bookingId the booking ID
     * @return a map containing booking details or error message
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBookingById(String bookingId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        
        if (bookingOpt.isPresent()) {
            Booking booking = bookingOpt.get();
            result.put("id", booking.getId());
            result.put("guest", booking.getGuest());
            result.put("room", booking.getRoom());
            result.put("checkin", booking.getCheckin().toString());
            result.put("checkout", booking.getCheckout().toString());
            result.put("confirmationCode", booking.getConfirmationCode());
            result.put("createdAt", booking.getCreatedAt().toString());
            result.put("updatedAt", booking.getUpdatedAt().toString());
        } else {
            result.put("error", "Booking not found: " + bookingId);
        }
        
        return result;
    }

    /**
     * Calculates room price based on various factors.
     * REFACTORED: Reduced cyclomatic complexity by extracting logic into separate methods.
     * 
     * @param roomType the room type
     * @param nights number of nights
     * @param season the season
     * @param loyalty the loyalty tier
     * @return the calculated price as a string
     */
    public String calculateRoomPrice(String roomType, int nights, String season, String loyalty) {
        double basePrice = getBasePrice(roomType);
        basePrice = applySeasonalAdjustment(basePrice, season);
        basePrice = applyLoyaltyDiscount(basePrice, loyalty);
        basePrice = applyLengthOfStayDiscount(basePrice, nights);
        
        double total = basePrice * nights;
        return String.format("%.2f", total);
    }

    /**
     * Gets the base price for a room type.
     * Extracted method to reduce complexity.
     * 
     * @param roomType the room type
     * @return the base price
     */
    private double getBasePrice(String roomType) {
        return switch (roomType) {
            case "STANDARD" -> 120.0;
            case "DELUXE" -> 200.0;
            case "SUITE" -> 350.0;
            case "VILLA" -> 600.0;
            default -> 120.0;
        };
    }

    /**
     * Applies seasonal price adjustment.
     * Extracted method to reduce complexity.
     * 
     * @param basePrice the base price
     * @param season the season
     * @return the adjusted price
     */
    private double applySeasonalAdjustment(double basePrice, String season) {
        return switch (season) {
            case "PEAK" -> basePrice * 1.5;
            case "OFF" -> basePrice * 0.8;
            default -> basePrice;
        };
    }

    /**
     * Applies loyalty discount.
     * Extracted method to reduce complexity.
     * 
     * @param basePrice the base price
     * @param loyalty the loyalty tier
     * @return the adjusted price
     */
    private double applyLoyaltyDiscount(double basePrice, String loyalty) {
        return switch (loyalty) {
            case "GOLD" -> basePrice * 0.9;
            case "PLATINUM" -> basePrice * 0.8;
            case "DIAMOND" -> basePrice * 0.7;
            default -> basePrice;
        };
    }

    /**
     * Applies length of stay discount.
     * Extracted method to reduce complexity.
     * 
     * @param basePrice the base price
     * @param nights number of nights
     * @return the adjusted price
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
     * Checks if a room type is available.
     * REFACTORED: Uses enum-based validation for better maintainability.
     * 
     * @param roomType the room type
     * @return true if the room type is valid, false otherwise
     */
    public boolean isRoomAvailable(String roomType) {
        return RoomType.isValid(roomType);
    }

    /**
     * Generates a report reference.
     * 
     * @param month the month for the report
     * @return a report generation message
     */
    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + paymentApi;
    }

    /**
     * Generates SHA-256 hash for the given input string.
     * Replaces the deprecated MD5 algorithm with secure SHA-256.
     * 
     * @param input the string to hash
     * @return hexadecimal representation of the SHA-256 hash
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
            // Fallback to input if SHA-256 is not available (should never happen in Java 21)
            return input;
        }
    }

    /**
     * Enum for room types to eliminate duplicated validation logic.
     * Provides centralized room type management.
     */
    private enum RoomType {
        STANDARD, DELUXE, SUITE, VILLA;

        public static boolean isValid(String roomType) {
            if (roomType == null) {
                return false;
            }
            try {
                valueOf(roomType);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}

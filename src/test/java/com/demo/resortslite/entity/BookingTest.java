package com.demo.resortslite.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Booking entity.
 * Tests entity validation, getters/setters, and JPA annotations.
 */
class BookingTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void defaultConstructor_initializesTimestamps() {
        // Act
        Booking booking = new Booking();

        // Assert
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getUpdatedAt());
    }

    @Test
    void parameterizedConstructor_setsAllFields() {
        // Arrange
        String id = "BK-12345678";
        String guest = "John Doe";
        String room = "DELUXE";
        LocalDate checkin = LocalDate.of(2024, 3, 1);
        LocalDate checkout = LocalDate.of(2024, 3, 5);

        // Act
        Booking booking = new Booking(id, guest, room, checkin, checkout);

        // Assert
        assertEquals(id, booking.getId());
        assertEquals(guest, booking.getGuest());
        assertEquals(room, booking.getRoom());
        assertEquals(checkin, booking.getCheckin());
        assertEquals(checkout, booking.getCheckout());
        assertNotNull(booking.getCreatedAt());
        assertNotNull(booking.getUpdatedAt());
    }

    @Test
    void setId_updatesIdField() {
        // Arrange
        Booking booking = new Booking();
        String id = "BK-TEST123";

        // Act
        booking.setId(id);

        // Assert
        assertEquals(id, booking.getId());
    }

    @Test
    void setGuest_updatesGuestField() {
        // Arrange
        Booking booking = new Booking();
        String guest = "Jane Smith";

        // Act
        booking.setGuest(guest);

        // Assert
        assertEquals(guest, booking.getGuest());
    }

    @Test
    void setRoom_updatesRoomField() {
        // Arrange
        Booking booking = new Booking();
        String room = "SUITE";

        // Act
        booking.setRoom(room);

        // Assert
        assertEquals(room, booking.getRoom());
    }

    @Test
    void setCheckin_updatesCheckinField() {
        // Arrange
        Booking booking = new Booking();
        LocalDate checkin = LocalDate.of(2024, 4, 1);

        // Act
        booking.setCheckin(checkin);

        // Assert
        assertEquals(checkin, booking.getCheckin());
    }

    @Test
    void setCheckout_updatesCheckoutField() {
        // Arrange
        Booking booking = new Booking();
        LocalDate checkout = LocalDate.of(2024, 4, 10);

        // Act
        booking.setCheckout(checkout);

        // Assert
        assertEquals(checkout, booking.getCheckout());
    }

    @Test
    void setConfirmationCode_updatesConfirmationCodeField() {
        // Arrange
        Booking booking = new Booking();
        String code = "CONF123456";

        // Act
        booking.setConfirmationCode(code);

        // Assert
        assertEquals(code, booking.getConfirmationCode());
    }

    @Test
    void setCreatedAt_updatesCreatedAtField() {
        // Arrange
        Booking booking = new Booking();
        LocalDateTime timestamp = LocalDateTime.of(2024, 3, 1, 10, 30);

        // Act
        booking.setCreatedAt(timestamp);

        // Assert
        assertEquals(timestamp, booking.getCreatedAt());
    }

    @Test
    void setUpdatedAt_updatesUpdatedAtField() {
        // Arrange
        Booking booking = new Booking();
        LocalDateTime timestamp = LocalDateTime.of(2024, 3, 2, 15, 45);

        // Act
        booking.setUpdatedAt(timestamp);

        // Assert
        assertEquals(timestamp, booking.getUpdatedAt());
    }

    @Test
    void validation_withNullId_failsValidation() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(null);
        booking.setGuest("John Doe");
        booking.setRoom("DELUXE");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(LocalDate.now().plusDays(1));

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void validation_withBlankGuest_failsValidation() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-123");
        booking.setGuest("");
        booking.setRoom("DELUXE");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(LocalDate.now().plusDays(1));

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be blank")));
    }

    @Test
    void validation_withBlankRoom_failsValidation() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-123");
        booking.setGuest("John Doe");
        booking.setRoom("");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(LocalDate.now().plusDays(1));

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be blank")));
    }

    @Test
    void validation_withNullCheckin_failsValidation() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-123");
        booking.setGuest("John Doe");
        booking.setRoom("DELUXE");
        booking.setCheckin(null);
        booking.setCheckout(LocalDate.now().plusDays(1));

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void validation_withNullCheckout_failsValidation() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-123");
        booking.setGuest("John Doe");
        booking.setRoom("DELUXE");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(null);

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void validation_withValidData_passesValidation() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-VALID123");
        booking.setGuest("John Doe");
        booking.setRoom("DELUXE");
        booking.setCheckin(LocalDate.of(2024, 3, 1));
        booking.setCheckout(LocalDate.of(2024, 3, 5));

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void preUpdate_updatesTimestamp() {
        // Arrange
        Booking booking = new Booking();
        LocalDateTime originalUpdatedAt = booking.getUpdatedAt();
        
        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        booking.preUpdate();

        // Assert
        assertNotEquals(originalUpdatedAt, booking.getUpdatedAt());
        assertTrue(booking.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void bookingEntity_hasEntityAnnotation() {
        // Assert
        assertTrue(Booking.class.isAnnotationPresent(jakarta.persistence.Entity.class));
    }

    @Test
    void bookingEntity_hasTableAnnotation() {
        // Assert
        assertTrue(Booking.class.isAnnotationPresent(jakarta.persistence.Table.class));
    }

    @Test
    void getId_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-GET123");

        // Act
        String id = booking.getId();

        // Assert
        assertEquals("BK-GET123", id);
    }

    @Test
    void getGuest_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        booking.setGuest("Test Guest");

        // Act
        String guest = booking.getGuest();

        // Assert
        assertEquals("Test Guest", guest);
    }

    @Test
    void getRoom_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        booking.setRoom("VILLA");

        // Act
        String room = booking.getRoom();

        // Assert
        assertEquals("VILLA", room);
    }

    @Test
    void getCheckin_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        LocalDate checkin = LocalDate.of(2024, 5, 15);
        booking.setCheckin(checkin);

        // Act
        LocalDate result = booking.getCheckin();

        // Assert
        assertEquals(checkin, result);
    }

    @Test
    void getCheckout_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        LocalDate checkout = LocalDate.of(2024, 5, 20);
        booking.setCheckout(checkout);

        // Act
        LocalDate result = booking.getCheckout();

        // Assert
        assertEquals(checkout, result);
    }

    @Test
    void getConfirmationCode_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        booking.setConfirmationCode("ABC123XYZ");

        // Act
        String code = booking.getConfirmationCode();

        // Assert
        assertEquals("ABC123XYZ", code);
    }

    @Test
    void getCreatedAt_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        LocalDateTime timestamp = LocalDateTime.of(2024, 3, 1, 12, 0);
        booking.setCreatedAt(timestamp);

        // Act
        LocalDateTime result = booking.getCreatedAt();

        // Assert
        assertEquals(timestamp, result);
    }

    @Test
    void getUpdatedAt_returnsCorrectValue() {
        // Arrange
        Booking booking = new Booking();
        LocalDateTime timestamp = LocalDateTime.of(2024, 3, 2, 14, 30);
        booking.setUpdatedAt(timestamp);

        // Act
        LocalDateTime result = booking.getUpdatedAt();

        // Assert
        assertEquals(timestamp, result);
    }

    @Test
    void booking_withLongGuestName_handlesCorrectly() {
        // Arrange
        Booking booking = new Booking();
        String longName = "A".repeat(255);
        booking.setId("BK-LONG");
        booking.setGuest(longName);
        booking.setRoom("SUITE");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(LocalDate.now().plusDays(1));

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertTrue(violations.isEmpty());
        assertEquals(longName, booking.getGuest());
    }

    @Test
    void booking_withSpecialCharactersInGuest_handlesCorrectly() {
        // Arrange
        Booking booking = new Booking();
        String specialName = "O'Brien-Smith";
        booking.setGuest(specialName);

        // Act & Assert
        assertEquals(specialName, booking.getGuest());
    }

    @Test
    void booking_withFutureDates_handlesCorrectly() {
        // Arrange
        Booking booking = new Booking();
        LocalDate futureCheckin = LocalDate.now().plusMonths(6);
        LocalDate futureCheckout = futureCheckin.plusDays(7);
        
        booking.setId("BK-FUTURE");
        booking.setGuest("Future Guest");
        booking.setRoom("DELUXE");
        booking.setCheckin(futureCheckin);
        booking.setCheckout(futureCheckout);

        // Act
        Set<ConstraintViolation<Booking>> violations = validator.validate(booking);

        // Assert
        assertTrue(violations.isEmpty());
    }
}

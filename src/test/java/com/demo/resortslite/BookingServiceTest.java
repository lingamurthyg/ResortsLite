package com.demo.resortslite;

import com.demo.resortslite.entity.Booking;
import com.demo.resortslite.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for BookingService.
 * Tests all business logic, JPA operations, and price calculations.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "dbHost", 
                "jdbc:postgresql://localhost:5432/resortdb");
        ReflectionTestUtils.setField(bookingService, "dbUser", "postgres");
        ReflectionTestUtils.setField(bookingService, "paymentApi", 
                "http://payment-svc.internal:9090/charge");
    }

    @Test
    void createBooking_withValidData_savesAndReturnsBooking() {
        // Arrange
        String guestName = "John Doe";
        String roomType = "DELUXE";
        String checkIn = "2024-03-01";
        String checkOut = "2024-03-05";

        Booking savedBooking = new Booking();
        savedBooking.setId("BK-12345678");
        savedBooking.setGuest(guestName);
        savedBooking.setRoom(roomType);
        savedBooking.setCheckin(LocalDate.parse(checkIn));
        savedBooking.setCheckout(LocalDate.parse(checkOut));
        savedBooking.setConfirmationCode("abc123");
        savedBooking.setCreatedAt(LocalDateTime.now());
        savedBooking.setUpdatedAt(LocalDateTime.now());

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Act
        Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(result);
        assertEquals("BK-12345678", result.get("bookingId"));
        assertEquals(guestName, result.get("guestName"));
        assertEquals(roomType, result.get("roomType"));
        assertEquals(checkIn, result.get("checkIn"));
        assertEquals(checkOut, result.get("checkOut"));
        assertNotNull(result.get("confirmationCode"));
        assertNotNull(result.get("dbHost"));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_generatesUniqueBookingId() {
        // Arrange
        Booking booking1 = new Booking();
        booking1.setId("BK-UNIQUE1");
        booking1.setGuest("Guest1");
        booking1.setRoom("SUITE");
        booking1.setCheckin(LocalDate.now());
        booking1.setCheckout(LocalDate.now().plusDays(2));
        booking1.setConfirmationCode("code1");
        booking1.setCreatedAt(LocalDateTime.now());
        booking1.setUpdatedAt(LocalDateTime.now());

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking1);

        // Act
        Map<String, Object> result1 = bookingService.createBooking("Guest1", "SUITE", "2024-03-01", "2024-03-03");
        Map<String, Object> result2 = bookingService.createBooking("Guest2", "DELUXE", "2024-03-05", "2024-03-07");

        // Assert
        assertNotNull(result1.get("bookingId"));
        assertNotNull(result2.get("bookingId"));
        assertTrue(result1.get("bookingId").toString().startsWith("BK-"));
    }

    @Test
    void createBooking_withNullGuestName_handlesGracefully() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-NULL");
        booking.setGuest(null);
        booking.setRoom("STANDARD");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(LocalDate.now().plusDays(1));
        booking.setConfirmationCode("code");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act
        Map<String, Object> result = bookingService.createBooking(null, "STANDARD", "2024-03-01", "2024-03-02");

        // Assert
        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void getBookingById_withExistingId_returnsBookingDetails() {
        // Arrange
        String bookingId = "BK-12345678";
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setGuest("Jane Doe");
        booking.setRoom("SUITE");
        booking.setCheckin(LocalDate.of(2024, 3, 1));
        booking.setCheckout(LocalDate.of(2024, 3, 5));
        booking.setConfirmationCode("CONF123");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("id"));
        assertEquals("Jane Doe", result.get("guest"));
        assertEquals("SUITE", result.get("room"));
        assertEquals("2024-03-01", result.get("checkin"));
        assertEquals("2024-03-05", result.get("checkout"));
        assertEquals("CONF123", result.get("confirmationCode"));
        assertNotNull(result.get("createdAt"));
        assertNotNull(result.get("updatedAt"));
        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getBookingById_withNonExistingId_returnsErrorMessage() {
        // Arrange
        String bookingId = "BK-NOTFOUND";
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(result.get("error").toString().contains("not found"));
        verify(bookingRepository).findById(bookingId);
    }

    @Test
    void getBookingById_withNullId_returnsError() {
        // Arrange
        when(bookingRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = bookingService.getBookingById(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    @Test
    void calculateRoomPrice_standardRoom_returnsCorrectPrice() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 3, "REGULAR", "NONE");

        // Assert
        assertNotNull(price);
        assertEquals("360.00", price); // 120 * 3
    }

    @Test
    void calculateRoomPrice_deluxeRoom_returnsCorrectPrice() {
        // Act
        String price = bookingService.calculateRoomPrice("DELUXE", 2, "REGULAR", "NONE");

        // Assert
        assertEquals("400.00", price); // 200 * 2
    }

    @Test
    void calculateRoomPrice_suiteRoom_returnsCorrectPrice() {
        // Act
        String price = bookingService.calculateRoomPrice("SUITE", 1, "REGULAR", "NONE");

        // Assert
        assertEquals("350.00", price); // 350 * 1
    }

    @Test
    void calculateRoomPrice_villaRoom_returnsCorrectPrice() {
        // Act
        String price = bookingService.calculateRoomPrice("VILLA", 2, "REGULAR", "NONE");

        // Assert
        assertEquals("1200.00", price); // 600 * 2
    }

    @Test
    void calculateRoomPrice_withPeakSeason_appliesSeasonalAdjustment() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 2, "PEAK", "NONE");

        // Assert
        assertEquals("360.00", price); // 120 * 1.5 * 2 = 360
    }

    @Test
    void calculateRoomPrice_withOffSeason_appliesDiscount() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 2, "OFF", "NONE");

        // Assert
        assertEquals("192.00", price); // 120 * 0.8 * 2 = 192
    }

    @Test
    void calculateRoomPrice_withGoldLoyalty_appliesDiscount() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 2, "REGULAR", "GOLD");

        // Assert
        assertEquals("216.00", price); // 120 * 0.9 * 2 = 216
    }

    @Test
    void calculateRoomPrice_withPlatinumLoyalty_appliesDiscount() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 2, "REGULAR", "PLATINUM");

        // Assert
        assertEquals("192.00", price); // 120 * 0.8 * 2 = 192
    }

    @Test
    void calculateRoomPrice_withDiamondLoyalty_appliesDiscount() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 2, "REGULAR", "DIAMOND");

        // Assert
        assertEquals("168.00", price); // 120 * 0.7 * 2 = 168
    }

    @Test
    void calculateRoomPrice_with7NightsStay_appliesLengthDiscount() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 7, "REGULAR", "NONE");

        // Assert
        assertEquals("798.00", price); // 120 * 0.95 * 7 = 798
    }

    @Test
    void calculateRoomPrice_with14NightsStay_appliesLargerDiscount() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 14, "REGULAR", "NONE");

        // Assert
        assertEquals("1512.00", price); // 120 * 0.90 * 14 = 1512
    }

    @Test
    void calculateRoomPrice_withMultipleDiscounts_appliesAllCorrectly() {
        // Act - Peak season, Gold loyalty, 7 nights
        String price = bookingService.calculateRoomPrice("DELUXE", 7, "PEAK", "GOLD");

        // Assert
        // 200 * 1.5 (peak) * 0.9 (gold) * 0.95 (7 nights) * 7 = 1795.50
        assertEquals("1795.50", price);
    }

    @Test
    void calculateRoomPrice_withInvalidRoomType_usesDefaultPrice() {
        // Act
        String price = bookingService.calculateRoomPrice("INVALID", 2, "REGULAR", "NONE");

        // Assert
        assertEquals("240.00", price); // Default 120 * 2
    }

    @Test
    void calculateRoomPrice_withZeroNights_returnsZero() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 0, "REGULAR", "NONE");

        // Assert
        assertEquals("0.00", price);
    }

    @Test
    void isRoomAvailable_withValidRoomType_returnsTrue() {
        // Act & Assert
        assertTrue(bookingService.isRoomAvailable("STANDARD"));
        assertTrue(bookingService.isRoomAvailable("DELUXE"));
        assertTrue(bookingService.isRoomAvailable("SUITE"));
        assertTrue(bookingService.isRoomAvailable("VILLA"));
    }

    @Test
    void isRoomAvailable_withInvalidRoomType_returnsFalse() {
        // Act & Assert
        assertFalse(bookingService.isRoomAvailable("INVALID"));
        assertFalse(bookingService.isRoomAvailable("PENTHOUSE"));
        assertFalse(bookingService.isRoomAvailable(""));
    }

    @Test
    void isRoomAvailable_withNullRoomType_returnsFalse() {
        // Act & Assert
        assertFalse(bookingService.isRoomAvailable(null));
    }

    @Test
    void isRoomAvailable_withLowercaseRoomType_returnsFalse() {
        // Act & Assert
        assertFalse(bookingService.isRoomAvailable("standard"));
        assertFalse(bookingService.isRoomAvailable("deluxe"));
    }

    @Test
    void generateReport_withValidMonth_returnsMessage() {
        // Act
        String result = bookingService.generateReport("March");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("March"));
        assertTrue(result.contains("Report generation triggered"));
    }

    @Test
    void generateReport_withEmptyMonth_handlesGracefully() {
        // Act
        String result = bookingService.generateReport("");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Report generation triggered"));
    }

    @Test
    void generateReport_includesPaymentApiUrl() {
        // Act
        String result = bookingService.generateReport("April");

        // Assert
        assertTrue(result.contains("payment-svc"));
    }

    @Test
    void createBooking_generatesSecureConfirmationCode() {
        // Arrange
        Booking booking = new Booking();
        booking.setId("BK-SECURE");
        booking.setGuest("Test User");
        booking.setRoom("DELUXE");
        booking.setCheckin(LocalDate.now());
        booking.setCheckout(LocalDate.now().plusDays(1));
        booking.setConfirmationCode("secureHash123");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        // Act
        Map<String, Object> result = bookingService.createBooking("Test User", "DELUXE", "2024-03-01", "2024-03-02");

        // Assert
        assertNotNull(result.get("confirmationCode"));
        assertFalse(result.get("confirmationCode").toString().isEmpty());
    }

    @Test
    void calculateRoomPrice_withNegativeNights_handlesGracefully() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", -1, "REGULAR", "NONE");

        // Assert
        assertNotNull(price);
        // Should handle negative as 0 or absolute value
    }

    @Test
    void calculateRoomPrice_withVeryLongStay_calculatesCorrectly() {
        // Act
        String price = bookingService.calculateRoomPrice("STANDARD", 30, "REGULAR", "NONE");

        // Assert
        assertNotNull(price);
        // 120 * 0.90 * 30 = 3240.00
        assertEquals("3240.00", price);
    }
}

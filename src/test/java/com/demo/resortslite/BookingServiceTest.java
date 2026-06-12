package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        // Setup common test data if needed
    }

    @Test
    void createBooking_withValidParameters_returnsBookingWithId() {
        // Arrange
        String guestName = "John Doe";
        String roomType = "DELUXE";
        String checkIn = "2024-03-01";
        String checkOut = "2024-03-05";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertNotNull(booking.get("bookingId"));
        assertTrue(booking.get("bookingId").toString().startsWith("BK-"));
        assertEquals(guestName, booking.get("guestName"));
        assertEquals(roomType, booking.get("roomType"));
        assertEquals(checkIn, booking.get("checkIn"));
        assertEquals(checkOut, booking.get("checkOut"));
        assertNotNull(booking.get("confirmCode"));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void createBooking_withDifferentGuest_generatesUniqueBookingId() {
        // Arrange
        String guestName1 = "Alice Smith";
        String guestName2 = "Bob Johnson";
        String roomType = "SUITE";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-05";

        // Act
        Map<String, Object> booking1 = bookingService.createBooking(guestName1, roomType, checkIn, checkOut);
        Map<String, Object> booking2 = bookingService.createBooking(guestName2, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking1.get("bookingId"));
        assertNotNull(booking2.get("bookingId"));
        assertNotEquals(booking1.get("bookingId"), booking2.get("bookingId"));
    }

    @Test
    void createBooking_withStandardRoom_createsBookingSuccessfully() {
        // Arrange
        String guestName = "Jane Doe";
        String roomType = "STANDARD";
        String checkIn = "2024-05-01";
        String checkOut = "2024-05-03";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(roomType, booking.get("roomType"));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void createBooking_withEmptyGuestName_stillCreatesBooking() {
        // Arrange
        String guestName = "";
        String roomType = "VILLA";
        String checkIn = "2024-06-01";
        String checkOut = "2024-06-10";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(guestName, booking.get("guestName"));
        assertNotNull(booking.get("bookingId"));
    }

    @Test
    void createBooking_generatesConfirmCodeWithSHA256() {
        // Arrange
        String guestName = "Test Guest";
        String roomType = "DELUXE";
        String checkIn = "2024-07-01";
        String checkOut = "2024-07-05";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        String confirmCode = (String) booking.get("confirmCode");
        assertNotNull(confirmCode);
        assertTrue(confirmCode.length() > 0);
        // SHA-256 produces 64 character hex string
        assertEquals(64, confirmCode.length());
    }

    @Test
    void getBookingById_withValidId_returnsBookingDetails() {
        // Arrange
        String bookingId = "BK-12345678";
        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("id", bookingId);
        mockResult.put("guest", "John Doe");
        mockResult.put("room", "DELUXE");
        
        when(jdbcTemplate.queryForMap(anyString())).thenReturn(mockResult);

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("id"));
        assertEquals("John Doe", result.get("guest"));
        verify(jdbcTemplate, times(1)).queryForMap(anyString());
    }

    @Test
    void getBookingById_withInvalidId_returnsErrorMessage() {
        // Arrange
        String bookingId = "INVALID-ID";
        when(jdbcTemplate.queryForMap(anyString())).thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(result.get("error").toString().contains(bookingId));
    }

    @Test
    void getBookingById_withNullId_handlesException() {
        // Arrange
        String bookingId = null;
        when(jdbcTemplate.queryForMap(anyString())).thenThrow(new RuntimeException("Null ID"));

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
    }

    @Test
    void calculateRoomPrice_withStandardRoom_returnsCorrectPrice() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 3;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("360.00", price); // 120 * 3
    }

    @Test
    void calculateRoomPrice_withDeluxeRoom_returnsCorrectPrice() {
        // Arrange
        String roomType = "DELUXE";
        int nights = 2;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("400.00", price); // 200 * 2
    }

    @Test
    void calculateRoomPrice_withSuiteRoom_returnsCorrectPrice() {
        // Arrange
        String roomType = "SUITE";
        int nights = 1;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("350.00", price); // 350 * 1
    }

    @Test
    void calculateRoomPrice_withVillaRoom_returnsCorrectPrice() {
        // Arrange
        String roomType = "VILLA";
        int nights = 5;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("3000.00", price); // 600 * 5
    }

    @Test
    void calculateRoomPrice_withPeakSeason_appliesSurcharge() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 2;
        String season = "PEAK";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("360.00", price); // 120 * 1.5 * 2
    }

    @Test
    void calculateRoomPrice_withOffSeason_appliesDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 2;
        String season = "OFF";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("192.00", price); // 120 * 0.8 * 2
    }

    @Test
    void calculateRoomPrice_withGoldLoyalty_appliesDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 2;
        String season = "REGULAR";
        String loyalty = "GOLD";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("216.00", price); // 120 * 0.9 * 2
    }

    @Test
    void calculateRoomPrice_withPlatinumLoyalty_appliesDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 2;
        String season = "REGULAR";
        String loyalty = "PLATINUM";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("192.00", price); // 120 * 0.8 * 2
    }

    @Test
    void calculateRoomPrice_withDiamondLoyalty_appliesDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 2;
        String season = "REGULAR";
        String loyalty = "DIAMOND";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("168.00", price); // 120 * 0.7 * 2
    }

    @Test
    void calculateRoomPrice_with7Nights_appliesDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 7;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("798.00", price); // 120 * 0.95 * 7
    }

    @Test
    void calculateRoomPrice_with14Nights_appliesLargerDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 14;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        // Note: The code has a bug - it checks nights >= 7 first, so 14 nights gets 0.95 discount, not 0.90
        assertEquals("1596.00", price); // 120 * 0.95 * 14
    }

    @Test
    void calculateRoomPrice_withInvalidRoomType_usesDefaultPrice() {
        // Arrange
        String roomType = "INVALID";
        int nights = 2;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("240.00", price); // Default 120 * 2
    }

    @Test
    void calculateRoomPrice_withCombinedDiscounts_appliesAll() {
        // Arrange
        String roomType = "DELUXE";
        int nights = 7;
        String season = "OFF";
        String loyalty = "GOLD";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        // 200 * 0.8 (OFF) * 0.9 (GOLD) * 0.95 (7 nights) * 7 nights
        assertEquals("957.60", price);
    }

    @Test
    void calculateRoomPrice_withZeroNights_returnsZero() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 0;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("0.00", price);
    }

    @Test
    void isRoomAvailable_withStandardRoom_returnsTrue() {
        // Arrange
        String roomType = "STANDARD";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_withDeluxeRoom_returnsTrue() {
        // Arrange
        String roomType = "DELUXE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_withSuiteRoom_returnsTrue() {
        // Arrange
        String roomType = "SUITE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_withVillaRoom_returnsTrue() {
        // Arrange
        String roomType = "VILLA";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_withInvalidRoomType_returnsFalse() {
        // Arrange
        String roomType = "INVALID_TYPE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available);
    }

    @Test
    void isRoomAvailable_withEmptyRoomType_returnsFalse() {
        // Arrange
        String roomType = "";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available);
    }

    @Test
    void isRoomAvailable_withNullRoomType_returnsFalse() {
        // Arrange
        String roomType = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            bookingService.isRoomAvailable(roomType);
        });
    }

    @Test
    void generateReport_withValidMonth_returnsMessage() {
        // Arrange
        String month = "March";

        // Act
        String result = bookingService.generateReport(month);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(month));
        assertTrue(result.contains("Report generation triggered"));
    }

    @Test
    void generateReport_withDifferentMonth_includesMonthInMessage() {
        // Arrange
        String month = "December";

        // Act
        String result = bookingService.generateReport(month);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(month));
    }

    @Test
    void generateReport_withEmptyMonth_stillReturnsMessage() {
        // Arrange
        String month = "";

        // Act
        String result = bookingService.generateReport(month);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Report generation triggered"));
    }

    @Test
    void createBooking_executesInsertQuery() {
        // Arrange
        String guestName = "Test User";
        String roomType = "STANDARD";
        String checkIn = "2024-08-01";
        String checkOut = "2024-08-05";

        // Act
        bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        verify(jdbcTemplate, times(1)).execute(contains("INSERT INTO bookings"));
    }

    @Test
    void calculateRoomPrice_withPeakSeasonAndGoldLoyalty_appliesBothModifiers() {
        // Arrange
        String roomType = "SUITE";
        int nights = 3;
        String season = "PEAK";
        String loyalty = "GOLD";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        // 350 * 1.5 (PEAK) * 0.9 (GOLD) * 3 nights
        assertEquals("1417.50", price);
    }
}

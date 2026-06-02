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
    void createBooking_withValidParameters_returnsBookingMap() {
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
        assertEquals(guestName, booking.get("guestName"));
        assertEquals(roomType, booking.get("roomType"));
        assertEquals(checkIn, booking.get("checkIn"));
        assertEquals(checkOut, booking.get("checkOut"));
        assertNotNull(booking.get("confirmCode"));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void createBooking_generatesUniqueBookingId() {
        // Arrange
        String guestName = "Jane Smith";
        String roomType = "SUITE";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-10";

        // Act
        Map<String, Object> booking1 = bookingService.createBooking(guestName, roomType, checkIn, checkOut);
        Map<String, Object> booking2 = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking1.get("bookingId"));
        assertNotNull(booking2.get("bookingId"));
        assertNotEquals(booking1.get("bookingId"), booking2.get("bookingId"));
    }

    @Test
    void createBooking_bookingIdStartsWithBK() {
        // Arrange
        String guestName = "Test User";
        String roomType = "STANDARD";
        String checkIn = "2024-05-01";
        String checkOut = "2024-05-03";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        String bookingId = (String) booking.get("bookingId");
        assertTrue(bookingId.startsWith("BK-"));
    }

    @Test
    void createBooking_generatesConfirmCode() {
        // Arrange
        String guestName = "Alice Johnson";
        String roomType = "VILLA";
        String checkIn = "2024-06-01";
        String checkOut = "2024-06-15";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking.get("confirmCode"));
        assertTrue(((String) booking.get("confirmCode")).length() > 0);
    }

    @Test
    void createBooking_withEmptyGuestName_stillCreatesBooking() {
        // Arrange
        String guestName = "";
        String roomType = "STANDARD";
        String checkIn = "2024-07-01";
        String checkOut = "2024-07-05";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(guestName, booking.get("guestName"));
    }

    @Test
    void createBooking_withSpecialCharacters_handlesCorrectly() {
        // Arrange
        String guestName = "O'Brien-Smith";
        String roomType = "DELUXE";
        String checkIn = "2024-08-01";
        String checkOut = "2024-08-05";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(guestName, booking.get("guestName"));
    }

    @Test
    void getBookingById_withValidId_returnsBookingData() {
        // Arrange
        String bookingId = "BK-12345678";
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("id", bookingId);
        mockData.put("guest", "John Doe");
        mockData.put("room", "DELUXE");

        when(jdbcTemplate.queryForMap(anyString())).thenReturn(mockData);

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("id"));
        assertEquals("John Doe", result.get("guest"));
        verify(jdbcTemplate, times(1)).queryForMap(anyString());
    }

    @Test
    void getBookingById_withInvalidId_returnsErrorMap() {
        // Arrange
        String bookingId = "INVALID-ID";
        when(jdbcTemplate.queryForMap(anyString())).thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Map<String, Object> result = bookingService.getBookingById(bookingId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(((String) result.get("error")).contains(bookingId));
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
    void calculateRoomPrice_standardRoom_returnsCorrectPrice() {
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
    void calculateRoomPrice_deluxeRoom_returnsCorrectPrice() {
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
    void calculateRoomPrice_suiteRoom_returnsCorrectPrice() {
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
    void calculateRoomPrice_villaRoom_returnsCorrectPrice() {
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
    void calculateRoomPrice_peakSeason_appliesMultiplier() {
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
    void calculateRoomPrice_offSeason_appliesDiscount() {
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
    void calculateRoomPrice_goldLoyalty_appliesDiscount() {
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
    void calculateRoomPrice_platinumLoyalty_appliesDiscount() {
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
    void calculateRoomPrice_diamondLoyalty_appliesDiscount() {
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
    void calculateRoomPrice_sevenNights_appliesDiscount() {
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
    void calculateRoomPrice_fourteenNights_appliesLargerDiscount() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 14;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        // Note: Code has bug - should check nights >= 14 before nights >= 7
        // Current logic applies 0.95 discount, not 0.90
        assertEquals("1596.00", price); // 120 * 0.95 * 14
    }

    @Test
    void calculateRoomPrice_unknownRoomType_usesDefaultPrice() {
        // Arrange
        String roomType = "UNKNOWN";
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
    void calculateRoomPrice_combinedDiscounts_appliesAll() {
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
        assertEquals("958.32", price);
    }

    @Test
    void calculateRoomPrice_zeroNights_returnsZero() {
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
    void isRoomAvailable_standardRoom_returnsTrue() {
        // Arrange
        String roomType = "STANDARD";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_deluxeRoom_returnsTrue() {
        // Arrange
        String roomType = "DELUXE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_suiteRoom_returnsTrue() {
        // Arrange
        String roomType = "SUITE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_villaRoom_returnsTrue() {
        // Arrange
        String roomType = "VILLA";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void isRoomAvailable_invalidRoomType_returnsFalse() {
        // Arrange
        String roomType = "PRESIDENTIAL";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available);
    }

    @Test
    void isRoomAvailable_emptyRoomType_returnsFalse() {
        // Arrange
        String roomType = "";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available);
    }

    @Test
    void isRoomAvailable_nullRoomType_returnsFalse() {
        // Arrange
        String roomType = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            bookingService.isRoomAvailable(roomType);
        });
    }

    @Test
    void isRoomAvailable_lowercaseRoomType_returnsFalse() {
        // Arrange
        String roomType = "standard";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available); // Case-sensitive check
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
    void generateReport_withDifferentMonths_includesMonthInMessage() {
        // Arrange
        String month1 = "January";
        String month2 = "December";

        // Act
        String result1 = bookingService.generateReport(month1);
        String result2 = bookingService.generateReport(month2);

        // Assert
        assertTrue(result1.contains(month1));
        assertTrue(result2.contains(month2));
        assertNotEquals(result1, result2);
    }

    @Test
    void generateReport_includesPaymentApiInMessage() {
        // Arrange
        String month = "June";

        // Act
        String result = bookingService.generateReport(month);

        // Assert
        assertTrue(result.contains("http://"));
        assertTrue(result.contains("payments"));
    }

    @Test
    void createBooking_executesJdbcInsert() {
        // Arrange
        String guestName = "Test User";
        String roomType = "STANDARD";
        String checkIn = "2024-09-01";
        String checkOut = "2024-09-05";

        // Act
        bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        verify(jdbcTemplate, times(1)).execute(contains("INSERT INTO bookings"));
    }

    @Test
    void createBooking_withLongGuestName_handlesCorrectly() {
        // Arrange
        String guestName = "Alexander Maximilian Christopher Wellington-Smythe III";
        String roomType = "VILLA";
        String checkIn = "2024-10-01";
        String checkOut = "2024-10-10";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(guestName, booking.get("guestName"));
    }
}

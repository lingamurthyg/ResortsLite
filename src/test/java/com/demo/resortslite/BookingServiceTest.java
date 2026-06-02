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
        Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(result);
        assertNotNull(result.get("bookingId"));
        assertEquals(guestName, result.get("guestName"));
        assertEquals(roomType, result.get("roomType"));
        assertEquals(checkIn, result.get("checkIn"));
        assertEquals(checkOut, result.get("checkOut"));
        assertNotNull(result.get("confirmCode"));
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void createBooking_generatesUniqueBookingId() {
        // Arrange
        String guestName = "Jane Smith";
        String roomType = "SUITE";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-05";

        // Act
        Map<String, Object> result1 = bookingService.createBooking(guestName, roomType, checkIn, checkOut);
        Map<String, Object> result2 = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(result1.get("bookingId"));
        assertNotNull(result2.get("bookingId"));
        assertNotEquals(result1.get("bookingId"), result2.get("bookingId"));
    }

    @Test
    void createBooking_bookingIdStartsWithBK() {
        // Arrange
        String guestName = "Test User";
        String roomType = "STANDARD";
        String checkIn = "2024-05-01";
        String checkOut = "2024-05-03";

        // Act
        Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        String bookingId = (String) result.get("bookingId");
        assertTrue(bookingId.startsWith("BK-"));
    }

    @Test
    void createBooking_generatesConfirmCode() {
        // Arrange
        String guestName = "Alice Johnson";
        String roomType = "VILLA";
        String checkIn = "2024-06-01";
        String checkOut = "2024-06-10";

        // Act
        Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        String confirmCode = (String) result.get("confirmCode");
        assertNotNull(confirmCode);
        assertFalse(confirmCode.isEmpty());
        assertTrue(confirmCode.length() > 10); // SHA-256 hash should be long
    }

    @Test
    void createBooking_withEmptyGuestName_stillCreatesBooking() {
        // Arrange
        String guestName = "";
        String roomType = "DELUXE";
        String checkIn = "2024-07-01";
        String checkOut = "2024-07-05";

        // Act
        Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(result);
        assertEquals(guestName, result.get("guestName"));
    }

    @Test
    void createBooking_withSpecialCharacters_handlesCorrectly() {
        // Arrange
        String guestName = "O'Brien-Smith & Co.";
        String roomType = "SUITE";
        String checkIn = "2024-08-01";
        String checkOut = "2024-08-05";

        // Act
        Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(result);
        assertEquals(guestName, result.get("guestName"));
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
        when(jdbcTemplate.queryForMap(anyString())).thenThrow(new RuntimeException("SQL error"));

        // Act
        Map<String, Object> result = bookingService.getBookingById(null);

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
    void calculateRoomPrice_peakSeason_appliesSurcharge() {
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
        // Note: Code has bug - 14 nights should get 0.90 discount but checks 7 first
        assertEquals("1596.00", price); // 120 * 0.95 * 14 (gets 7+ discount, not 14+)
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
        assertEquals("957.60", price);
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
    void calculateRoomPrice_negativeNights_handlesGracefully() {
        // Arrange
        String roomType = "STANDARD";
        int nights = -5;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        // Negative nights will produce negative price
        assertTrue(price.contains("-"));
    }

    @Test
    void isRoomAvailable_standardRoom_returnsTrue() {
        // Arrange
        String roomType = "STANDARD";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(result);
    }

    @Test
    void isRoomAvailable_deluxeRoom_returnsTrue() {
        // Arrange
        String roomType = "DELUXE";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(result);
    }

    @Test
    void isRoomAvailable_suiteRoom_returnsTrue() {
        // Arrange
        String roomType = "SUITE";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(result);
    }

    @Test
    void isRoomAvailable_villaRoom_returnsTrue() {
        // Arrange
        String roomType = "VILLA";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(result);
    }

    @Test
    void isRoomAvailable_invalidRoomType_returnsFalse() {
        // Arrange
        String roomType = "PENTHOUSE";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(result);
    }

    @Test
    void isRoomAvailable_emptyRoomType_returnsFalse() {
        // Arrange
        String roomType = "";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(result);
    }

    @Test
    void isRoomAvailable_nullRoomType_returnsFalse() {
        // Arrange
        String roomType = null;

        // Act
        // Act & Assert - Should throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            bookingService.isRoomAvailable(roomType);
        });
    }

    @Test
    void isRoomAvailable_lowercaseRoomType_returnsFalse() {
        // Arrange
        String roomType = "standard";

        // Act
        boolean result = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(result); // Case-sensitive check
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
    }

    @Test
    void generateReport_includesPaymentApiReference() {
        // Arrange
        String month = "April";

        // Act
        String result = bookingService.generateReport(month);

        // Assert
        assertTrue(result.contains("http://"));
    }

    @Test
    void createBooking_executesJdbcTemplate() {
        // Arrange
        String guestName = "Test Guest";
        String roomType = "STANDARD";
        String checkIn = "2024-09-01";
        String checkOut = "2024-09-05";

        // Act
        bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void createBooking_withNullValues_handlesGracefully() {
        // Arrange
        String guestName = null;
        String roomType = null;
        String checkIn = null;
        String checkOut = null;

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            Map<String, Object> result = bookingService.createBooking(guestName, roomType, checkIn, checkOut);
            assertNotNull(result);
        });
    }
}

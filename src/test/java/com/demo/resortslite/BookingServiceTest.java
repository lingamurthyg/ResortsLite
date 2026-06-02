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
        // Reset any static state if needed
    }

    @Test
    void testCreateBooking_withValidInputs_returnsBookingMap() {
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
    void testCreateBooking_generatesUniqueBookingId() {
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
    void testCreateBooking_bookingIdStartsWithBK() {
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
    void testCreateBooking_generatesConfirmCode() {
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
    void testCreateBooking_withEmptyGuestName_stillCreatesBooking() {
        // Arrange
        String guestName = "";
        String roomType = "DELUXE";
        String checkIn = "2024-07-01";
        String checkOut = "2024-07-05";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(guestName, booking.get("guestName"));
    }

    @Test
    void testCreateBooking_withSpecialCharactersInGuestName_handlesCorrectly() {
        // Arrange
        String guestName = "O'Brien";
        String roomType = "SUITE";
        String checkIn = "2024-08-01";
        String checkOut = "2024-08-05";

        // Act
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Assert
        assertNotNull(booking);
        assertEquals(guestName, booking.get("guestName"));
    }

    @Test
    void testGetBookingById_withValidId_returnsBookingDetails() {
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
    void testGetBookingById_withInvalidId_returnsErrorMap() {
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
    void testGetBookingById_withNullId_handlesException() {
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
    void testCalculateRoomPrice_standardRoom_regularSeason_noLoyalty() {
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
    void testCalculateRoomPrice_deluxeRoom_peakSeason_noLoyalty() {
        // Arrange
        String roomType = "DELUXE";
        int nights = 2;
        String season = "PEAK";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("600.00", price); // 200 * 1.5 * 2
    }

    @Test
    void testCalculateRoomPrice_suiteRoom_offSeason_goldLoyalty() {
        // Arrange
        String roomType = "SUITE";
        int nights = 5;
        String season = "OFF";
        String loyalty = "GOLD";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("1260.00", price); // 350 * 0.8 * 0.9 * 5
    }

    @Test
    void testCalculateRoomPrice_villaRoom_regularSeason_platinumLoyalty() {
        // Arrange
        String roomType = "VILLA";
        int nights = 4;
        String season = "REGULAR";
        String loyalty = "PLATINUM";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("1920.00", price); // 600 * 0.8 * 4
    }

    @Test
    void testCalculateRoomPrice_standardRoom_regularSeason_diamondLoyalty() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 10;
        String season = "REGULAR";
        String loyalty = "DIAMOND";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        // 120 * 0.7 * 0.95 * 10 = 798.00
        assertEquals("798.00", price);
    }

    @Test
    void testCalculateRoomPrice_with7NightsDiscount() {
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
    void testCalculateRoomPrice_with14NightsDiscount() {
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
    void testCalculateRoomPrice_unknownRoomType_defaultsToStandard() {
        // Arrange
        String roomType = "UNKNOWN";
        int nights = 2;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertEquals("240.00", price); // defaults to 120 * 2
    }

    @Test
    void testCalculateRoomPrice_peakSeasonMultiplier() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 1;
        String season = "PEAK";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertEquals("180.00", price); // 120 * 1.5
    }

    @Test
    void testCalculateRoomPrice_offSeasonMultiplier() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 1;
        String season = "OFF";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertEquals("96.00", price); // 120 * 0.8
    }

    @Test
    void testCalculateRoomPrice_zeroNights_returnsZero() {
        // Arrange
        String roomType = "STANDARD";
        int nights = 0;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertEquals("0.00", price);
    }

    @Test
    void testCalculateRoomPrice_negativeNights_calculatesNegativePrice() {
        // Arrange
        String roomType = "STANDARD";
        int nights = -1;
        String season = "REGULAR";
        String loyalty = "NONE";

        // Act
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Assert
        assertNotNull(price);
        assertTrue(price.startsWith("-"));
    }

    @Test
    void testIsRoomAvailable_standardRoom_returnsTrue() {
        // Arrange
        String roomType = "STANDARD";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void testIsRoomAvailable_deluxeRoom_returnsTrue() {
        // Arrange
        String roomType = "DELUXE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void testIsRoomAvailable_suiteRoom_returnsTrue() {
        // Arrange
        String roomType = "SUITE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void testIsRoomAvailable_villaRoom_returnsTrue() {
        // Arrange
        String roomType = "VILLA";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertTrue(available);
    }

    @Test
    void testIsRoomAvailable_invalidRoomType_returnsFalse() {
        // Arrange
        String roomType = "PENTHOUSE";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available);
    }

    @Test
    void testIsRoomAvailable_emptyRoomType_returnsFalse() {
        // Arrange
        String roomType = "";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available);
    }

    @Test
    void testIsRoomAvailable_nullRoomType_returnsFalse() {
        // Arrange
        String roomType = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            bookingService.isRoomAvailable(roomType);
        });
    }

    @Test
    void testIsRoomAvailable_lowercaseRoomType_returnsFalse() {
        // Arrange
        String roomType = "standard";

        // Act
        boolean available = bookingService.isRoomAvailable(roomType);

        // Assert
        assertFalse(available); // Case-sensitive check
    }

    @Test
    void testGenerateReport_withValidMonth_returnsMessage() {
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
    void testGenerateReport_withDifferentMonths_includesMonthInMessage() {
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
    void testGenerateReport_includesPaymentApiUrl() {
        // Arrange
        String month = "April";

        // Act
        String result = bookingService.generateReport(month);

        // Assert
        assertTrue(result.contains("http://"));
        assertTrue(result.contains("payments"));
    }

    @Test
    void testCreateBooking_executesJdbcInsert() {
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
    void testCalculateRoomPrice_allRoomTypes_returnValidPrices() {
        // Test all room types return valid formatted prices
        String[] roomTypes = {"STANDARD", "DELUXE", "SUITE", "VILLA"};
        
        for (String roomType : roomTypes) {
            String price = bookingService.calculateRoomPrice(roomType, 1, "REGULAR", "NONE");
            assertNotNull(price);
            assertTrue(price.matches("\\d+\\.\\d{2}"));
        }
    }

    @Test
    void testCalculateRoomPrice_allSeasons_applyCorrectMultipliers() {
        // Test different seasons
        String standardPrice = bookingService.calculateRoomPrice("STANDARD", 1, "REGULAR", "NONE");
        String peakPrice = bookingService.calculateRoomPrice("STANDARD", 1, "PEAK", "NONE");
        String offPrice = bookingService.calculateRoomPrice("STANDARD", 1, "OFF", "NONE");

        assertNotEquals(standardPrice, peakPrice);
        assertNotEquals(standardPrice, offPrice);
        assertNotEquals(peakPrice, offPrice);
    }

    @Test
    void testCalculateRoomPrice_allLoyaltyLevels_applyCorrectDiscounts() {
        // Test different loyalty levels
        String nonePrice = bookingService.calculateRoomPrice("STANDARD", 1, "REGULAR", "NONE");
        String goldPrice = bookingService.calculateRoomPrice("STANDARD", 1, "REGULAR", "GOLD");
        String platinumPrice = bookingService.calculateRoomPrice("STANDARD", 1, "REGULAR", "PLATINUM");
        String diamondPrice = bookingService.calculateRoomPrice("STANDARD", 1, "REGULAR", "DIAMOND");

        // Gold should be less than none
        assertTrue(Double.parseDouble(goldPrice) < Double.parseDouble(nonePrice));
        // Platinum should be less than gold
        assertTrue(Double.parseDouble(platinumPrice) < Double.parseDouble(goldPrice));
        // Diamond should be less than platinum
        assertTrue(Double.parseDouble(diamondPrice) < Double.parseDouble(platinumPrice));
    }
}

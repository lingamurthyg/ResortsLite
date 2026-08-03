package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BookingService class.
 */
@SpringBootTest
class BookingServiceTests {

    @Autowired
    private BookingService bookingService;

    @Test
    void testCreateBooking() {
        // Given
        String guestName = "Test Guest";
        String roomType = "DELUXE";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-05";

        // When
        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Then
        assertNotNull(booking);
        assertTrue(booking.containsKey("bookingId"));
        assertEquals(guestName, booking.get("guestName"));
        assertEquals(roomType, booking.get("roomType"));
        assertEquals(checkIn, booking.get("checkIn"));
        assertEquals(checkOut, booking.get("checkOut"));
        assertNotNull(booking.get("confirmationCode"));
    }

    @Test
    void testCalculateRoomPriceStandard() {
        // Given
        String roomType = "STANDARD";
        int nights = 3;
        String season = "REGULAR";
        String loyalty = "NONE";

        // When
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Then
        assertNotNull(price);
        assertEquals("360.00", price); // 120 * 3 nights
    }

    @Test
    void testCalculateRoomPriceWithDiscount() {
        // Given
        String roomType = "DELUXE";
        int nights = 7;
        String season = "REGULAR";
        String loyalty = "GOLD";

        // When
        String price = bookingService.calculateRoomPrice(roomType, nights, season, loyalty);

        // Then
        assertNotNull(price);
        // 200 * 0.9 (GOLD) * 0.95 (7 nights) * 7 = 1197.00
        assertEquals("1197.00", price);
    }

    @Test
    void testIsRoomAvailableValid() {
        // Test valid room types
        assertTrue(bookingService.isRoomAvailable("STANDARD"));
        assertTrue(bookingService.isRoomAvailable("DELUXE"));
        assertTrue(bookingService.isRoomAvailable("SUITE"));
        assertTrue(bookingService.isRoomAvailable("VILLA"));
    }

    @Test
    void testIsRoomAvailableInvalid() {
        // Test invalid room type
        assertFalse(bookingService.isRoomAvailable("INVALID"));
        assertFalse(bookingService.isRoomAvailable(""));
    }

    @Test
    void testGenerateReport() {
        // Given
        String month = "March";

        // When
        String result = bookingService.generateReport(month);

        // Then
        assertNotNull(result);
        assertTrue(result.contains(month));
    }
}

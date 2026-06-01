package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Test
    void createBooking_withValidParameters_returnsConfirmedBooking() {
        // Arrange
        String guestName = "John Doe";
        String roomType = "DELUXE";
        String checkIn = "2024-03-01";
        String checkOut = "2024-03-05";
        
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-12345678");
        mockBooking.put("guestName", guestName);
        mockBooking.put("roomType", roomType);
        mockBooking.put("checkIn", checkIn);
        mockBooking.put("checkOut", checkOut);
        
        when(bookingService.createBooking(guestName, roomType, checkIn, checkOut))
            .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
            guestName, roomType, checkIn, checkOut, session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
        assertNotNull(response.get("booking"));
        assertEquals(mockBooking, response.get("booking"));
        verify(bookingService, times(1)).createBooking(guestName, roomType, checkIn, checkOut);
    }

    @Test
    void createBooking_withValidParameters_storesBookingInSession() {
        // Arrange
        String guestName = "Jane Smith";
        String roomType = "SUITE";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-10";
        
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-87654321");
        mockBooking.put("guestName", guestName);
        
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(mockBooking);

        // Act
        bookingController.createBooking(guestName, roomType, checkIn, checkOut, session);

        // Assert
        assertEquals(mockBooking, session.getAttribute("lastBooking"));
        assertEquals(guestName, session.getAttribute("guestName"));
    }

    @Test
    void createBooking_withEmptyGuestName_stillProcessesBooking() {
        // Arrange
        String guestName = "";
        String roomType = "STANDARD";
        String checkIn = "2024-05-01";
        String checkOut = "2024-05-03";
        
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-11111111");
        
        when(bookingService.createBooking(guestName, roomType, checkIn, checkOut))
            .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
            guestName, roomType, checkIn, checkOut, session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
    }

    @Test
    void getBookingStatus_withValidBookingId_returnsBookingDetails() {
        // Arrange
        String bookingId = "BK-12345678";
        String guestName = "Test Guest";
        session.setAttribute("guestName", guestName);
        
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("bookingId", bookingId);
        mockDetails.put("status", "confirmed");
        
        when(bookingService.getBookingById(bookingId)).thenReturn(mockDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("bookingId"));
        assertEquals(guestName, result.get("sessionGuest"));
        assertEquals(mockDetails, result.get("details"));
        verify(bookingService, times(1)).getBookingById(bookingId);
    }

    @Test
    void getBookingStatus_withNoSessionData_returnsNullSessionGuest() {
        // Arrange
        String bookingId = "BK-99999999";
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("bookingId", bookingId);
        
        when(bookingService.getBookingById(bookingId)).thenReturn(mockDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertNull(result.get("sessionGuest"));
        assertEquals(mockDetails, result.get("details"));
    }

    @Test
    void getBookingStatus_withInvalidBookingId_returnsErrorDetails() {
        // Arrange
        String bookingId = "INVALID-ID";
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Booking not found: " + bookingId);
        
        when(bookingService.getBookingById(bookingId)).thenReturn(errorDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(errorDetails, result.get("details"));
    }

    @Test
    void checkAvailability_withValidRoomType_returnsAvailabilityInfo() {
        // Arrange
        String roomType = "DELUXE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertNotNull(response.get("inventoryEndpoint"));
        assertTrue((Boolean) response.get("available"));
        verify(bookingService, times(1)).isRoomAvailable(roomType);
    }

    @Test
    void checkAvailability_withUnavailableRoom_returnsFalse() {
        // Arrange
        String roomType = "VILLA";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertFalse((Boolean) response.get("available"));
    }

    @Test
    void checkAvailability_withInvalidRoomType_returnsResponse() {
        // Arrange
        String roomType = "INVALID";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertFalse((Boolean) response.get("available"));
    }

    @Test
    void downloadReport_withValidMonth_returnsReportPath() {
        // Arrange
        String month = "March";
        String expectedMessage = "Report generated successfully";
        when(bookingService.generateReport(month)).thenReturn(expectedMessage);

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertNotNull(response.get("reportPath"));
        assertTrue(((String) response.get("reportPath")).contains(month));
        assertEquals(expectedMessage, response.get("message"));
        verify(bookingService, times(1)).generateReport(month);
    }

    @Test
    void downloadReport_withEmptyMonth_stillProcessesRequest() {
        // Arrange
        String month = "";
        String expectedMessage = "Report generated";
        when(bookingService.generateReport(month)).thenReturn(expectedMessage);

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertNotNull(response.get("reportPath"));
        assertEquals(expectedMessage, response.get("message"));
    }

    @Test
    void downloadReport_withSpecialCharactersInMonth_handlesCorrectly() {
        // Arrange
        String month = "Jan/2024";
        String expectedMessage = "Report generated";
        when(bookingService.generateReport(month)).thenReturn(expectedMessage);

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertNotNull(response.get("reportPath"));
        assertTrue(((String) response.get("reportPath")).contains(month));
    }

    @Test
    void createBooking_withNullSession_throwsException() {
        // Arrange
        String guestName = "John Doe";
        String roomType = "DELUXE";
        String checkIn = "2024-03-01";
        String checkOut = "2024-03-05";
        
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-12345678");
        
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(mockBooking);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            bookingController.createBooking(guestName, roomType, checkIn, checkOut, null);
        });
    }

    @Test
    void createBooking_withMultipleBookings_cachesAllBookings() {
        // Arrange
        Map<String, Object> booking1 = new HashMap<>();
        booking1.put("bookingId", "BK-11111111");
        booking1.put("guestName", "Guest 1");
        
        Map<String, Object> booking2 = new HashMap<>();
        booking2.put("bookingId", "BK-22222222");
        booking2.put("guestName", "Guest 2");
        
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(booking1)
            .thenReturn(booking2);

        // Act
        bookingController.createBooking("Guest 1", "STANDARD", "2024-03-01", "2024-03-03", session);
        bookingController.createBooking("Guest 2", "DELUXE", "2024-03-05", "2024-03-07", session);

        // Assert
        verify(bookingService, times(2)).createBooking(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void checkAvailability_withNullRoomType_handlesGracefully() {
        // Arrange
        when(bookingService.isRoomAvailable(null)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(null);

        // Assert
        assertNotNull(response);
        assertNull(response.get("roomType"));
        assertFalse((Boolean) response.get("available"));
    }
}

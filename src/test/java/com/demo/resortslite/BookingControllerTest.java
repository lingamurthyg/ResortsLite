package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateBooking_withValidParameters_returnsConfirmedBooking() {
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
        verify(bookingService, times(1)).createBooking(guestName, roomType, checkIn, checkOut);
        verify(session, times(1)).setAttribute("lastBooking", mockBooking);
        verify(session, times(1)).setAttribute("guestName", guestName);
    }

    @Test
    void testCreateBooking_withEmptyGuestName_stillProcesses() {
        // Arrange
        String guestName = "";
        String roomType = "STANDARD";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-03";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-87654321");
        mockBooking.put("guestName", guestName);

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
    void testCreateBooking_withNullParameters_handlesGracefully() {
        // Arrange
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-NULL001");

        when(bookingService.createBooking(null, null, null, null))
                .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
                null, null, null, null, session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
    }

    @Test
    void testGetBookingStatus_withValidBookingId_returnsBookingDetails() {
        // Arrange
        String bookingId = "BK-12345678";
        String guestName = "Jane Smith";
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("bookingId", bookingId);
        mockDetails.put("status", "confirmed");

        when(session.getAttribute("guestName")).thenReturn(guestName);
        when(bookingService.getBookingById(bookingId)).thenReturn(mockDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("bookingId"));
        assertEquals(guestName, result.get("sessionGuest"));
        assertNotNull(result.get("details"));
        verify(session, times(1)).getAttribute("guestName");
        verify(bookingService, times(1)).getBookingById(bookingId);
    }

    @Test
    void testGetBookingStatus_withNoSessionGuest_returnsNullSessionGuest() {
        // Arrange
        String bookingId = "BK-99999999";
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("bookingId", bookingId);

        when(session.getAttribute("guestName")).thenReturn(null);
        when(bookingService.getBookingById(bookingId)).thenReturn(mockDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertNull(result.get("sessionGuest"));
        assertEquals(bookingId, result.get("bookingId"));
    }

    @Test
    void testGetBookingStatus_withInvalidBookingId_returnsErrorDetails() {
        // Arrange
        String bookingId = "INVALID-ID";
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Booking not found");

        when(session.getAttribute("guestName")).thenReturn("Test User");
        when(bookingService.getBookingById(bookingId)).thenReturn(errorDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("bookingId"));
        assertNotNull(result.get("details"));
    }

    @Test
    void testCheckAvailability_withValidRoomType_returnsAvailabilityInfo() {
        // Arrange
        String roomType = "SUITE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertTrue((Boolean) response.get("available"));
        assertNotNull(response.get("inventoryEndpoint"));
        verify(bookingService, times(1)).isRoomAvailable(roomType);
    }

    @Test
    void testCheckAvailability_withInvalidRoomType_returnsUnavailable() {
        // Arrange
        String roomType = "INVALID_TYPE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertFalse((Boolean) response.get("available"));
    }

    @Test
    void testCheckAvailability_withNullRoomType_handlesGracefully() {
        // Arrange
        when(bookingService.isRoomAvailable(null)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(null);

        // Assert
        assertNotNull(response);
        assertNull(response.get("roomType"));
    }

    @Test
    void testDownloadReport_withValidMonth_returnsReportPath() {
        // Arrange
        String month = "March";
        String expectedMessage = "Report generated successfully";
        when(bookingService.generateReport(month)).thenReturn(expectedMessage);

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertTrue(response.get("reportPath").toString().contains(month));
        assertEquals(expectedMessage, response.get("message"));
        verify(bookingService, times(1)).generateReport(month);
    }

    @Test
    void testDownloadReport_withEmptyMonth_stillGeneratesPath() {
        // Arrange
        String month = "";
        when(bookingService.generateReport(month)).thenReturn("Report generated");

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertNotNull(response.get("reportPath"));
        assertNotNull(response.get("message"));
    }

    @Test
    void testDownloadReport_withSpecialCharactersInMonth_handlesGracefully() {
        // Arrange
        String month = "Jan/2024";
        when(bookingService.generateReport(month)).thenReturn("Report generated");

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertTrue(response.get("reportPath").toString().contains(month));
    }

    @Test
    void testCreateBooking_verifiesSessionAttributesAreSet() {
        // Arrange
        String guestName = "Test Guest";
        String roomType = "VILLA";
        String checkIn = "2024-05-01";
        String checkOut = "2024-05-10";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-TEST001");
        mockBooking.put("guestName", guestName);

        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        // Act
        bookingController.createBooking(guestName, roomType, checkIn, checkOut, session);

        // Assert
        verify(session).setAttribute(eq("lastBooking"), any(Map.class));
        verify(session).setAttribute("guestName", guestName);
    }

    @Test
    void testCheckAvailability_verifiesInventoryEndpointIsReturned() {
        // Arrange
        String roomType = "DELUXE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        String inventoryEndpoint = (String) response.get("inventoryEndpoint");
        assertNotNull(inventoryEndpoint);
        assertTrue(inventoryEndpoint.contains("inventory-service"));
    }
}

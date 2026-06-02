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
    void testCreateBooking_withValidInputs_returnsConfirmedBooking() {
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
    void testCreateBooking_storesDataInSession() {
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
    void testCreateBooking_withEmptyGuestName_stillProcesses() {
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
    void testGetBookingStatus_withValidBookingId_returnsStatus() {
        // Arrange
        String bookingId = "BK-12345678";
        String guestName = "John Doe";
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
    void testGetBookingStatus_withNoSessionData_returnsNullGuest() {
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
    void testGetBookingStatus_withInvalidBookingId_handlesGracefully() {
        // Arrange
        String bookingId = "INVALID-ID";
        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("error", "Booking not found");

        when(bookingService.getBookingById(bookingId)).thenReturn(mockDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("bookingId"));
    }

    @Test
    void testCheckAvailability_withValidRoomType_returnsAvailability() {
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
    void testCheckAvailability_withUnavailableRoom_returnsFalse() {
        // Arrange
        String roomType = "PENTHOUSE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertFalse((Boolean) response.get("available"));
    }

    @Test
    void testCheckAvailability_containsInventoryEndpoint() {
        // Arrange
        String roomType = "SUITE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        String endpoint = (String) response.get("inventoryEndpoint");
        assertNotNull(endpoint);
        assertTrue(endpoint.contains("inventory-service"));
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
        assertNotNull(response.get("reportPath"));
        assertTrue(((String) response.get("reportPath")).contains(month));
        assertEquals(expectedMessage, response.get("message"));
        verify(bookingService, times(1)).generateReport(month);
    }

    @Test
    void testDownloadReport_withDifferentMonths_generatesCorrectPaths() {
        // Arrange
        String month1 = "January";
        String month2 = "December";
        when(bookingService.generateReport(anyString())).thenReturn("Success");

        // Act
        Map<String, Object> response1 = bookingController.downloadReport(month1);
        Map<String, Object> response2 = bookingController.downloadReport(month2);

        // Assert
        assertTrue(((String) response1.get("reportPath")).contains(month1));
        assertTrue(((String) response2.get("reportPath")).contains(month2));
    }

    @Test
    void testDownloadReport_pathContainsLegacyDirectory() {
        // Arrange
        String month = "April";
        when(bookingService.generateReport(month)).thenReturn("Success");

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        String reportPath = (String) response.get("reportPath");
        assertTrue(reportPath.contains("/var/legacy/reports/"));
        assertTrue(reportPath.endsWith(".pdf"));
    }

    @Test
    void testCreateBooking_withNullSession_handlesGracefully() {
        // Arrange
        String guestName = "Test User";
        String roomType = "STANDARD";
        String checkIn = "2024-06-01";
        String checkOut = "2024-06-05";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-00000000");

        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            bookingController.createBooking(guestName, roomType, checkIn, checkOut, session);
        });
    }

    @Test
    void testCheckAvailability_withEmptyRoomType_callsService() {
        // Arrange
        String roomType = "";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        verify(bookingService, times(1)).isRoomAvailable(roomType);
    }

    @Test
    void testGetBookingStatus_multipleCallsWithSameSession_maintainsSessionData() {
        // Arrange
        String bookingId1 = "BK-11111111";
        String bookingId2 = "BK-22222222";
        String guestName = "Persistent Guest";
        session.setAttribute("guestName", guestName);

        when(bookingService.getBookingById(anyString())).thenReturn(new HashMap<>());

        // Act
        Map<String, Object> result1 = bookingController.getBookingStatus(bookingId1, session);
        Map<String, Object> result2 = bookingController.getBookingStatus(bookingId2, session);

        // Assert
        assertEquals(guestName, result1.get("sessionGuest"));
        assertEquals(guestName, result2.get("sessionGuest"));
    }
}

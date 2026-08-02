package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BookingController Test Suite")
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        session = new MockHttpSession();
    }

    @Test
    @DisplayName("createBooking - should create booking successfully with valid parameters")
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
    @DisplayName("createBooking - should store booking in session")
    void createBooking_shouldStoreBookingInSession() {
        // Arrange
        String guestName = "Jane Smith";
        String roomType = "SUITE";
        String checkIn = "2024-04-01";
        String checkOut = "2024-04-10";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-87654321");
        mockBooking.put("guestName", guestName);

        when(bookingService.createBooking(guestName, roomType, checkIn, checkOut))
                .thenReturn(mockBooking);

        // Act
        bookingController.createBooking(guestName, roomType, checkIn, checkOut, session);

        // Assert
        assertEquals(mockBooking, session.getAttribute("lastBooking"));
        assertEquals(guestName, session.getAttribute("guestName"));
    }

    @Test
    @DisplayName("createBooking - should handle empty guest name")
    void createBooking_withEmptyGuestName_shouldProcessRequest() {
        // Arrange
        String guestName = "";
        String roomType = "STANDARD";
        String checkIn = "2024-05-01";
        String checkOut = "2024-05-03";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-11111111");
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
    @DisplayName("getBookingStatus - should return booking status with session guest")
    void getBookingStatus_withValidBookingId_returnsBookingDetails() {
        // Arrange
        String bookingId = "BK-12345678";
        String guestName = "John Doe";
        session.setAttribute("guestName", guestName);

        Map<String, Object> mockBookingDetails = new HashMap<>();
        mockBookingDetails.put("bookingId", bookingId);
        mockBookingDetails.put("status", "confirmed");

        when(bookingService.getBookingById(bookingId)).thenReturn(mockBookingDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("bookingId"));
        assertEquals(guestName, result.get("sessionGuest"));
        assertEquals(mockBookingDetails, result.get("details"));
        verify(bookingService, times(1)).getBookingById(bookingId);
    }

    @Test
    @DisplayName("getBookingStatus - should handle null session guest")
    void getBookingStatus_withNoSessionGuest_returnsNullGuest() {
        // Arrange
        String bookingId = "BK-99999999";
        Map<String, Object> mockBookingDetails = new HashMap<>();
        mockBookingDetails.put("bookingId", bookingId);

        when(bookingService.getBookingById(bookingId)).thenReturn(mockBookingDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertNull(result.get("sessionGuest"));
        assertEquals(mockBookingDetails, result.get("details"));
    }

    @Test
    @DisplayName("checkAvailability - should return availability for valid room type")
    void checkAvailability_withValidRoomType_returnsAvailability() {
        // Arrange
        String roomType = "DELUXE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertNotNull(response.get("inventoryEndpoint"));
        assertEquals(true, response.get("available"));
        verify(bookingService, times(1)).isRoomAvailable(roomType);
    }

    @Test
    @DisplayName("checkAvailability - should return unavailable for invalid room type")
    void checkAvailability_withInvalidRoomType_returnsUnavailable() {
        // Arrange
        String roomType = "INVALID";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response);
        assertEquals(roomType, response.get("roomType"));
        assertEquals(false, response.get("available"));
    }

    @Test
    @DisplayName("checkAvailability - should include inventory endpoint in response")
    void checkAvailability_shouldIncludeInventoryEndpoint() {
        // Arrange
        String roomType = "SUITE";
        when(bookingService.isRoomAvailable(roomType)).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(roomType);

        // Assert
        assertNotNull(response.get("inventoryEndpoint"));
        assertTrue(response.get("inventoryEndpoint").toString().contains("inventory-service"));
    }

    @Test
    @DisplayName("downloadReport - should generate report for valid month")
    void downloadReport_withValidMonth_returnsReportPath() {
        // Arrange
        String month = "March";
        String reportMessage = "Report generated successfully";
        when(bookingService.generateReport(month)).thenReturn(reportMessage);

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertNotNull(response.get("reportPath"));
        assertTrue(response.get("reportPath").toString().contains(month));
        assertEquals(reportMessage, response.get("message"));
        verify(bookingService, times(1)).generateReport(month);
    }

    @Test
    @DisplayName("downloadReport - should handle empty month parameter")
    void downloadReport_withEmptyMonth_shouldProcessRequest() {
        // Arrange
        String month = "";
        String reportMessage = "Report generated";
        when(bookingService.generateReport(month)).thenReturn(reportMessage);

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertNotNull(response.get("reportPath"));
        assertEquals(reportMessage, response.get("message"));
    }

    @Test
    @DisplayName("downloadReport - should construct correct report path")
    void downloadReport_shouldConstructCorrectReportPath() {
        // Arrange
        String month = "January";
        when(bookingService.generateReport(month)).thenReturn("Success");

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        String reportPath = (String) response.get("reportPath");
        assertNotNull(reportPath);
        assertTrue(reportPath.contains("/var/legacy/reports/"));
        assertTrue(reportPath.contains(month));
        assertTrue(reportPath.endsWith("_bookings.pdf"));
    }

    @Test
    @DisplayName("createBooking - should handle null room type")
    void createBooking_withNullRoomType_shouldProcessRequest() {
        // Arrange
        String guestName = "Test Guest";
        String roomType = null;
        String checkIn = "2024-06-01";
        String checkOut = "2024-06-05";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-NULL001");

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
    @DisplayName("getBookingStatus - should handle empty booking ID")
    void getBookingStatus_withEmptyBookingId_shouldProcessRequest() {
        // Arrange
        String bookingId = "";
        Map<String, Object> mockBookingDetails = new HashMap<>();
        mockBookingDetails.put("error", "Booking not found");

        when(bookingService.getBookingById(bookingId)).thenReturn(mockBookingDetails);

        // Act
        Map<String, Object> result = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(result);
        assertEquals(bookingId, result.get("bookingId"));
    }
}

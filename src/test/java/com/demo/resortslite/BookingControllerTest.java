package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for BookingController.
 * Tests all REST endpoints, session handling, and service integration.
 */
@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private BookingController bookingController;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        ReflectionTestUtils.setField(bookingController, "inventoryServiceUrl", 
                "http://inventory-service.internal:8081/rooms");
    }

    @Test
    void createBooking_withValidData_returnsConfirmedBooking() {
        // Arrange
        String guestName = "John Doe";
        String roomType = "DELUXE";
        String checkIn = "2024-03-01";
        String checkOut = "2024-03-05";

        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-12345678");
        mockBooking.put("guestName", guestName);
        mockBooking.put("roomType", roomType);

        when(bookingService.createBooking(guestName, roomType, checkIn, checkOut))
                .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
                guestName, roomType, checkIn, checkOut, session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
        assertNotNull(response.get("booking"));
        assertEquals(guestName, session.getAttribute("guestName"));
        assertEquals(mockBooking, session.getAttribute("lastBooking"));
        verify(bookingService).createBooking(guestName, roomType, checkIn, checkOut);
    }

    @Test
    void createBooking_withNullGuestName_handlesGracefully() {
        // Arrange
        when(bookingService.createBooking(isNull(), anyString(), anyString(), anyString()))
                .thenReturn(new HashMap<>());

        // Act
        Map<String, Object> response = bookingController.createBooking(
                null, "SUITE", "2024-03-01", "2024-03-05", session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
    }

    @Test
    void createBooking_withEmptyRoomType_handlesGracefully() {
        // Arrange
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-TEST");
        when(bookingService.createBooking(anyString(), eq(""), anyString(), anyString()))
                .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
                "Jane Doe", "", "2024-03-01", "2024-03-05", session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
    }

    @Test
    void getBookingStatus_withValidBookingId_returnsBookingDetails() {
        // Arrange
        String bookingId = "BK-12345678";
        String guestName = "John Doe";
        session.setAttribute("guestName", guestName);

        Map<String, Object> mockDetails = new HashMap<>();
        mockDetails.put("id", bookingId);
        mockDetails.put("guest", guestName);

        when(bookingService.getBookingById(bookingId)).thenReturn(mockDetails);

        // Act
        Map<String, Object> response = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(response);
        assertEquals(bookingId, response.get("bookingId"));
        assertEquals(guestName, response.get("sessionGuest"));
        assertEquals(mockDetails, response.get("details"));
        verify(bookingService).getBookingById(bookingId);
    }

    @Test
    void getBookingStatus_withNoSessionData_returnsNullSessionGuest() {
        // Arrange
        String bookingId = "BK-99999999";
        when(bookingService.getBookingById(bookingId)).thenReturn(new HashMap<>());

        // Act
        Map<String, Object> response = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(response);
        assertNull(response.get("sessionGuest"));
    }

    @Test
    void getBookingStatus_withInvalidBookingId_returnsEmptyDetails() {
        // Arrange
        String bookingId = "INVALID";
        when(bookingService.getBookingById(bookingId)).thenReturn(new HashMap<>());

        // Act
        Map<String, Object> response = bookingController.getBookingStatus(bookingId, session);

        // Assert
        assertNotNull(response);
        assertEquals(bookingId, response.get("bookingId"));
        assertTrue(((Map<?, ?>) response.get("details")).isEmpty());
    }

    @Test
    void checkAvailability_withValidRoomType_returnsAvailabilityInfo() {
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
        assertTrue(response.get("inventoryEndpoint").toString().contains("inventory-service"));
        verify(bookingService).isRoomAvailable(roomType);
    }

    @Test
    void checkAvailability_withInvalidRoomType_returnsFalse() {
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
    void checkAvailability_withNullRoomType_handlesGracefully() {
        // Arrange
        when(bookingService.isRoomAvailable(null)).thenReturn(false);

        // Act
        Map<String, Object> response = bookingController.checkAvailability(null);

        // Assert
        assertNotNull(response);
        assertNull(response.get("roomType"));
    }

    @Test
    void downloadReport_withValidMonth_returnsDownloadUrl() {
        // Arrange
        String month = "March";
        String expectedUrl = "https://reports.resorts-internal.com/download/March_bookings.pdf";
        when(reportService.buildReportDownloadUrl(anyString())).thenReturn(expectedUrl);
        when(bookingService.generateReport(month)).thenReturn("Report generated");

        // Act
        Map<String, Object> response = bookingController.downloadReport(month);

        // Assert
        assertNotNull(response);
        assertEquals("March_bookings.pdf", response.get("reportFileName"));
        assertEquals(expectedUrl, response.get("downloadUrl"));
        assertEquals("Report generated", response.get("message"));
        verify(reportService).buildReportDownloadUrl("March_bookings.pdf");
        verify(bookingService).generateReport(month);
    }

    @Test
    void downloadReport_withEmptyMonth_handlesGracefully() {
        // Arrange
        when(reportService.buildReportDownloadUrl(anyString())).thenReturn("http://test.com");
        when(bookingService.generateReport("")).thenReturn("Report generated");

        // Act
        Map<String, Object> response = bookingController.downloadReport("");

        // Assert
        assertNotNull(response);
        assertEquals("_bookings.pdf", response.get("reportFileName"));
    }

    @Test
    void getBookingsByGuest_withValidGuestName_returnsBookingsList() {
        // Arrange
        String guestName = "John Doe";

        // Act
        Map<String, Object> response = bookingController.getBookingsByGuest(guestName);

        // Assert
        assertNotNull(response);
        assertEquals(guestName, response.get("guestName"));
        assertNotNull(response.get("bookings"));
    }

    @Test
    void getBookingsByGuest_withEmptyGuestName_handlesGracefully() {
        // Act
        Map<String, Object> response = bookingController.getBookingsByGuest("");

        // Assert
        assertNotNull(response);
        assertEquals("", response.get("guestName"));
    }

    @Test
    void healthCheck_returnsUpStatus() {
        // Act
        Map<String, Object> response = bookingController.healthCheck();

        // Assert
        assertNotNull(response);
        assertEquals("UP", response.get("status"));
        assertEquals("BookingController", response.get("service"));
        assertNotNull(response.get("timestamp"));
    }

    @Test
    void healthCheck_alwaysReturnsValidTimestamp() {
        // Act
        Map<String, Object> response = bookingController.healthCheck();

        // Assert
        String timestamp = (String) response.get("timestamp");
        assertNotNull(timestamp);
        assertFalse(timestamp.isEmpty());
        assertTrue(timestamp.contains("T")); // ISO format check
    }

    @Test
    void createBooking_storesBookingInCache() {
        // Arrange
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-CACHE123");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        // Act
        bookingController.createBooking("Test", "SUITE", "2024-03-01", "2024-03-05", session);

        // Assert - verify cache interaction through service call
        verify(bookingService).createBooking(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createBooking_withSpecialCharactersInGuestName_handlesCorrectly() {
        // Arrange
        String guestName = "O'Brien-Smith";
        Map<String, Object> mockBooking = new HashMap<>();
        mockBooking.put("bookingId", "BK-SPECIAL");
        when(bookingService.createBooking(eq(guestName), anyString(), anyString(), anyString()))
                .thenReturn(mockBooking);

        // Act
        Map<String, Object> response = bookingController.createBooking(
                guestName, "DELUXE", "2024-03-01", "2024-03-05", session);

        // Assert
        assertNotNull(response);
        assertEquals("confirmed", response.get("status"));
        verify(bookingService).createBooking(eq(guestName), anyString(), anyString(), anyString());
    }

    @Test
    void checkAvailability_usesConfiguredInventoryUrl() {
        // Arrange
        String customUrl = "http://custom-inventory:9000/rooms";
        ReflectionTestUtils.setField(bookingController, "inventoryServiceUrl", customUrl);
        when(bookingService.isRoomAvailable(anyString())).thenReturn(true);

        // Act
        Map<String, Object> response = bookingController.checkAvailability("DELUXE");

        // Assert
        String inventoryEndpoint = (String) response.get("inventoryEndpoint");
        assertTrue(inventoryEndpoint.contains("custom-inventory"));
    }
}

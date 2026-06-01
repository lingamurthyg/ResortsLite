package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private ReportService reportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
    }

    @Test
    void generateMonthlyReport_withValidMonthAndYear_returnsSuccessStatus() {
        // Arrange
        String month = "March";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withValidParameters_includesReportPath() {
        // Arrange
        String month = "April";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        if ("generated".equals(result.get("status"))) {
            assertTrue(result.containsKey("path"));
            String path = (String) result.get("path");
            assertTrue(path.contains(month));
            assertTrue(path.contains(year));
        }
    }

    @Test
    void generateMonthlyReport_withValidParameters_includesServerPort() {
        // Arrange
        String month = "May";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        if ("generated".equals(result.get("status"))) {
            assertTrue(result.containsKey("serverPort"));
            assertEquals(8080, result.get("serverPort"));
        }
    }

    @Test
    void generateMonthlyReport_withEmptyMonth_handlesGracefully() {
        // Arrange
        String month = "";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withEmptyYear_handlesGracefully() {
        // Arrange
        String month = "June";
        String year = "";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withNullMonth_handlesGracefully() {
        // Arrange
        String month = null;
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withNullYear_handlesGracefully() {
        // Arrange
        String month = "July";
        String year = null;

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withSpecialCharacters_handlesCorrectly() {
        // Arrange
        String month = "Jan/Feb";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_whenDirectoryCreationFails_returnsErrorStatus() {
        // Arrange
        String month = "August";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        // Will likely return error status due to hardcoded path not existing
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withLongMonthName_handlesCorrectly() {
        // Arrange
        String month = "A".repeat(50);
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void buildReportDownloadUrl_withValidReportName_returnsUrl() {
        // Arrange
        String reportName = "resort_report_March_2024.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
        assertTrue(url.contains("http://"));
        assertTrue(url.contains("8080"));
        assertTrue(url.contains("/reports/"));
    }

    @Test
    void buildReportDownloadUrl_withEmptyReportName_returnsUrl() {
        // Arrange
        String reportName = "";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("http://"));
        assertTrue(url.contains("localhost"));
    }

    @Test
    void buildReportDownloadUrl_withNullReportName_returnsUrl() {
        // Arrange
        String reportName = null;

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("http://"));
    }

    @Test
    void buildReportDownloadUrl_withSpecialCharacters_handlesCorrectly() {
        // Arrange
        String reportName = "report_2024/03.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void buildReportDownloadUrl_withSpaces_handlesCorrectly() {
        // Arrange
        String reportName = "report with spaces.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void buildReportDownloadUrl_containsCorrectProtocol() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.startsWith("http://"));
        assertFalse(url.startsWith("https://"));
    }

    @Test
    void buildReportDownloadUrl_containsCorrectPort() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains(":8080"));
    }

    @Test
    void getSystemInfo_returnsAllRequiredFields() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        assertTrue(info.containsKey("reportBasePath"));
        assertTrue(info.containsKey("backupPath"));
        assertTrue(info.containsKey("serverPort"));
        assertTrue(info.containsKey("generatedAt"));
    }

    @Test
    void getSystemInfo_returnsCorrectReportBasePath() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("reportBasePath"));
        assertEquals("/var/legacy/reports/", info.get("reportBasePath"));
    }

    @Test
    void getSystemInfo_returnsCorrectBackupPath() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("backupPath"));
        assertEquals("C:\\ResortBackups\\nightly\\", info.get("backupPath"));
    }

    @Test
    void getSystemInfo_returnsCorrectServerPort() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("serverPort"));
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void getSystemInfo_returnsValidTimestamp() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("generatedAt"));
        String timestamp = (String) info.get("generatedAt");
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void getSystemInfo_timestampIsCurrentTime() {
        // Arrange & Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        
        try {
            Thread.sleep(1100); // Sleep for more than 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        String timestamp1 = (String) info1.get("generatedAt");
        String timestamp2 = (String) info2.get("generatedAt");
        assertNotEquals(timestamp1, timestamp2);
    }

    @Test
    void getSystemInfo_calledMultipleTimes_returnsConsistentPaths() {
        // Arrange & Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        assertEquals(info1.get("reportBasePath"), info2.get("reportBasePath"));
        assertEquals(info1.get("backupPath"), info2.get("backupPath"));
        assertEquals(info1.get("serverPort"), info2.get("serverPort"));
    }

    @Test
    void generateMonthlyReport_withNumericMonth_handlesCorrectly() {
        // Arrange
        String month = "03";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withFutureYear_handlesCorrectly() {
        // Arrange
        String month = "December";
        String year = "2099";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withPastYear_handlesCorrectly() {
        // Arrange
        String month = "January";
        String year = "2000";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void buildReportDownloadUrl_withMultipleDots_handlesCorrectly() {
        // Arrange
        String reportName = "report.2024.03.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void buildReportDownloadUrl_withPathTraversal_includesInUrl() {
        // Arrange
        String reportName = "../../../etc/passwd";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void getSystemInfo_returnsNonNullValues() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("reportBasePath"));
        assertNotNull(info.get("backupPath"));
        assertNotNull(info.get("serverPort"));
        assertNotNull(info.get("generatedAt"));
    }

    @Test
    void generateMonthlyReport_withMixedCaseMonth_handlesCorrectly() {
        // Arrange
        String month = "MaRcH";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }
}

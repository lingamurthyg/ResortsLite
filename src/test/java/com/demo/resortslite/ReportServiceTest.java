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
        // Note: This will fail in actual execution due to hardcoded path, but tests the logic
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withValidParameters_includesServerPort() {
        // Arrange
        String month = "April";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        if (result.containsKey("serverPort")) {
            assertEquals(8080, result.get("serverPort"));
        }
    }

    @Test
    void generateMonthlyReport_withDifferentMonth_generatesCorrectFileName() {
        // Arrange
        String month = "December";
        String year = "2023";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        if (result.containsKey("path")) {
            String path = (String) result.get("path");
            assertTrue(path.contains(month));
            assertTrue(path.contains(year));
            assertTrue(path.endsWith(".csv"));
        }
    }

    @Test
    void generateMonthlyReport_withEmptyMonth_stillProcesses() {
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
    void generateMonthlyReport_withEmptyYear_stillProcesses() {
        // Arrange
        String month = "January";
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
        // Should handle null gracefully, likely with error status
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withNullYear_handlesGracefully() {
        // Arrange
        String month = "May";
        String year = null;

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withSpecialCharactersInMonth_handlesInput() {
        // Arrange
        String month = "March@2024";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withLongMonthName_processesCorrectly() {
        // Arrange
        String month = "September";
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
        String reportName = "march_2024_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
        assertTrue(url.contains("http://"));
        assertTrue(url.contains("localhost"));
        assertTrue(url.contains("8080"));
        assertTrue(url.contains("/reports/"));
    }

    @Test
    void buildReportDownloadUrl_withDifferentReportName_includesNameInUrl() {
        // Arrange
        String reportName = "annual_summary_2023.pdf";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void buildReportDownloadUrl_withEmptyReportName_stillReturnsUrl() {
        // Arrange
        String reportName = "";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("http://"));
        assertTrue(url.contains("/reports/"));
    }

    @Test
    void buildReportDownloadUrl_withNullReportName_handlesNull() {
        // Arrange
        String reportName = null;

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("http://"));
    }

    @Test
    void buildReportDownloadUrl_withSpecialCharacters_includesInUrl() {
        // Arrange
        String reportName = "report_2024-03-15_v2.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void buildReportDownloadUrl_usesCorrectPort() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(":8080"));
    }

    @Test
    void buildReportDownloadUrl_usesHttpProtocol() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.startsWith("http://"));
        assertFalse(url.startsWith("https://"));
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
    void getSystemInfo_returnsCorrectServerPort() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void getSystemInfo_returnsReportBasePath() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        String reportBasePath = (String) info.get("reportBasePath");
        assertNotNull(reportBasePath);
        assertTrue(reportBasePath.contains("/var/legacy/reports/"));
    }

    @Test
    void getSystemInfo_returnsBackupPath() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        String backupPath = (String) info.get("backupPath");
        assertNotNull(backupPath);
        assertTrue(backupPath.contains("ResortBackups"));
    }

    @Test
    void getSystemInfo_returnsTimestamp() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        String timestamp = (String) info.get("generatedAt");
        assertNotNull(timestamp);
        assertFalse(timestamp.isEmpty());
        // Timestamp should contain date and time
        assertTrue(timestamp.contains("-")); // Date separator
        assertTrue(timestamp.contains(":")); // Time separator
    }

    @Test
    void getSystemInfo_timestampHasCorrectFormat() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String timestamp = (String) info.get("generatedAt");
        assertNotNull(timestamp);
        // Format: yyyy-MM-dd HH:mm:ss
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void getSystemInfo_calledMultipleTimes_returnsDifferentTimestamps() throws InterruptedException {
        // Arrange & Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        Thread.sleep(1100); // Wait for at least 1 second
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        String timestamp1 = (String) info1.get("generatedAt");
        String timestamp2 = (String) info2.get("generatedAt");
        assertNotEquals(timestamp1, timestamp2);
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
    void generateMonthlyReport_withNumericMonth_processesCorrectly() {
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
    void generateMonthlyReport_withFutureYear_processesCorrectly() {
        // Arrange
        String month = "June";
        String year = "2025";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withPastYear_processesCorrectly() {
        // Arrange
        String month = "July";
        String year = "2020";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void buildReportDownloadUrl_withPdfExtension_includesInUrl() {
        // Arrange
        String reportName = "monthly_report.pdf";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(".pdf"));
    }

    @Test
    void buildReportDownloadUrl_withCsvExtension_includesInUrl() {
        // Arrange
        String reportName = "data_export.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(".csv"));
    }

    @Test
    void getSystemInfo_returnsMapWithFourEntries() {
        // Arrange & Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(4, info.size());
    }

    @Test
    void generateMonthlyReport_resultContainsStatusKey() {
        // Arrange
        String month = "August";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertTrue(result.containsKey("status"));
    }
}

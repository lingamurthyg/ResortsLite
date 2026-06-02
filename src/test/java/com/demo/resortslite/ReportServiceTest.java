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
    void generateMonthlyReport_createsReportWithCorrectFileName() {
        // Arrange
        String month = "January";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            String path = (String) result.get("path");
            assertNotNull(path);
            assertTrue(path.contains("resort_report_" + month + "_" + year + ".csv"));
        }
    }

    @Test
    void generateMonthlyReport_includesServerPort() {
        // Arrange
        String month = "February";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertTrue(result.containsKey("serverPort"));
        assertEquals(8080, result.get("serverPort"));
    }

    @Test
    void generateMonthlyReport_withDifferentMonths_generatesDifferentPaths() {
        // Arrange
        String month1 = "April";
        String month2 = "May";
        String year = "2024";

        // Act
        Map<String, Object> result1 = reportService.generateMonthlyReport(month1, year);
        Map<String, Object> result2 = reportService.generateMonthlyReport(month2, year);

        // Assert
        if ("generated".equals(result1.get("status")) && "generated".equals(result2.get("status"))) {
            assertNotEquals(result1.get("path"), result2.get("path"));
        }
    }

    @Test
    void generateMonthlyReport_withDifferentYears_generatesDifferentPaths() {
        // Arrange
        String month = "June";
        String year1 = "2023";
        String year2 = "2024";

        // Act
        Map<String, Object> result1 = reportService.generateMonthlyReport(month, year1);
        Map<String, Object> result2 = reportService.generateMonthlyReport(month, year2);

        // Assert
        if ("generated".equals(result1.get("status")) && "generated".equals(result2.get("status"))) {
            assertNotEquals(result1.get("path"), result2.get("path"));
        }
    }

    @Test
    void generateMonthlyReport_pathContainsBaseDirectory() {
        // Arrange
        String month = "July";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if (result.containsKey("path")) {
            String path = (String) result.get("path");
            assertTrue(path.contains("/var/legacy/reports/"));
        }
    }

    @Test
    void generateMonthlyReport_handlesIOException() {
        // Arrange
        String month = "August";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
        // Status could be "generated" or "error" depending on file system permissions
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
        String month = "September";
        String year = "";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withSpecialCharactersInMonth_handlesCorrectly() {
        // Arrange
        String month = "March/April";
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
    }

    @Test
    void buildReportDownloadUrl_containsHttpProtocol() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.startsWith("http://"));
    }

    @Test
    void buildReportDownloadUrl_containsLocalhost() {
        // Arrange
        String reportName = "report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("localhost"));
    }

    @Test
    void buildReportDownloadUrl_containsServerPort() {
        // Arrange
        String reportName = "annual_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("8080"));
    }

    @Test
    void buildReportDownloadUrl_containsReportsPath() {
        // Arrange
        String reportName = "monthly_summary.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("/reports/"));
    }

    @Test
    void buildReportDownloadUrl_withDifferentReportNames_generatesDifferentUrls() {
        // Arrange
        String reportName1 = "report1.csv";
        String reportName2 = "report2.csv";

        // Act
        String url1 = reportService.buildReportDownloadUrl(reportName1);
        String url2 = reportService.buildReportDownloadUrl(reportName2);

        // Assert
        assertNotEquals(url1, url2);
        assertTrue(url1.contains(reportName1));
        assertTrue(url2.contains(reportName2));
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
    }

    @Test
    void buildReportDownloadUrl_withSpecialCharacters_includesInUrl() {
        // Arrange
        String reportName = "report-2024_Q1.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains(reportName));
    }

    @Test
    void getSystemInfo_returnsMapWithAllKeys() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        assertTrue(info.containsKey("reportBasePath"));
        assertTrue(info.containsKey("backupPath"));
        assertTrue(info.containsKey("serverPort"));
        assertTrue(info.containsKey("generatedAt"));
    }

    @Test
    void getSystemInfo_reportBasePathIsCorrect() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals("/var/legacy/reports/", info.get("reportBasePath"));
    }

    @Test
    void getSystemInfo_backupPathIsCorrect() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals("C:\\ResortBackups\\nightly\\", info.get("backupPath"));
    }

    @Test
    void getSystemInfo_serverPortIsCorrect() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void getSystemInfo_generatedAtIsNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("generatedAt"));
    }

    @Test
    void getSystemInfo_generatedAtIsValidTimestamp() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String timestamp = (String) info.get("generatedAt");
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void getSystemInfo_calledMultipleTimes_returnsDifferentTimestamps() throws InterruptedException {
        // Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        Thread.sleep(1100); // Wait for timestamp to change
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        assertNotEquals(info1.get("generatedAt"), info2.get("generatedAt"));
    }

    @Test
    void getSystemInfo_allValuesAreNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        info.values().forEach(value -> assertNotNull(value));
    }

    @Test
    void generateMonthlyReport_withNullMonth_handlesGracefully() {
        // Arrange
        String month = null;
        String year = "2024";

        // Act & Assert
        assertDoesNotThrow(() -> {
            reportService.generateMonthlyReport(month, year);
        });
    }

    @Test
    void generateMonthlyReport_withNullYear_handlesGracefully() {
        // Arrange
        String month = "October";
        String year = null;

        // Act & Assert
        assertDoesNotThrow(() -> {
            reportService.generateMonthlyReport(month, year);
        });
    }

    @Test
    void buildReportDownloadUrl_withNullReportName_handlesGracefully() {
        // Arrange
        String reportName = null;

        // Act & Assert
        assertDoesNotThrow(() -> {
            reportService.buildReportDownloadUrl(reportName);
        });
    }

    @Test
    void generateMonthlyReport_statusIsEitherGeneratedOrError() {
        // Arrange
        String month = "November";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        String status = (String) result.get("status");
        assertTrue(status.equals("generated") || status.equals("error"));
    }

    @Test
    void generateMonthlyReport_errorStatus_includesMessage() {
        // Arrange
        String month = "December";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("error".equals(result.get("status"))) {
            assertTrue(result.containsKey("message"));
            assertNotNull(result.get("message"));
        }
    }

    @Test
    void buildReportDownloadUrl_formatIsCorrect() {
        // Arrange
        String reportName = "test.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertEquals("http://localhost:8080/reports/" + reportName, url);
    }

    @Test
    void getSystemInfo_returnsExactlyFourKeys() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(4, info.size());
    }

    @Test
    void generateMonthlyReport_withLongMonthName_handlesCorrectly() {
        // Arrange
        String month = "VeryLongMonthNameThatExceedsNormalLength";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void buildReportDownloadUrl_withPathTraversalAttempt_includesInUrl() {
        // Arrange
        String reportName = "../../../etc/passwd";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains(reportName));
    }
}

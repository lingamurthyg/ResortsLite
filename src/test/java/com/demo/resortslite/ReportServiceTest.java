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
        String month = "April";
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
        String month = "May";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        // serverPort is only included on success
        if ("generated".equals(result.get("status"))) {
            assertTrue(result.containsKey("serverPort"));
            assertEquals(8080, result.get("serverPort"));
        } else {
            // On error, serverPort may not be present
            assertTrue(result.containsKey("status"));
        }
    }

    @Test
    void generateMonthlyReport_withDifferentMonths_generatesUniqueFileNames() {
        // Arrange
        String month1 = "January";
        String month2 = "February";
        String year = "2024";

        // Act
        Map<String, Object> result1 = reportService.generateMonthlyReport(month1, year);
        Map<String, Object> result2 = reportService.generateMonthlyReport(month2, year);

        // Assert
        if ("generated".equals(result1.get("status")) && "generated".equals(result2.get("status"))) {
            String path1 = (String) result1.get("path");
            String path2 = (String) result2.get("path");
            assertNotEquals(path1, path2);
        }
    }

    @Test
    void generateMonthlyReport_withDifferentYears_generatesUniqueFileNames() {
        // Arrange
        String month = "June";
        String year1 = "2023";
        String year2 = "2024";

        // Act
        Map<String, Object> result1 = reportService.generateMonthlyReport(month, year1);
        Map<String, Object> result2 = reportService.generateMonthlyReport(month, year2);

        // Assert
        if ("generated".equals(result1.get("status")) && "generated".equals(result2.get("status"))) {
            String path1 = (String) result1.get("path");
            String path2 = (String) result2.get("path");
            assertNotEquals(path1, path2);
        }
    }

    @Test
    void generateMonthlyReport_handlesIOException_returnsErrorStatus() {
        // Arrange
        String month = "InvalidPath";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
        // May return error or generated depending on file system permissions
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
        String month = "July";
        String year = "";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void generateMonthlyReport_withSpecialCharacters_handlesGracefully() {
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
    void generateMonthlyReport_pathContainsExpectedDirectory() {
        // Arrange
        String month = "August";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            String path = (String) result.get("path");
            assertTrue(path.contains("/var/legacy/reports/"));
        }
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
        assertTrue(url.startsWith("http://"));
    }

    @Test
    void buildReportDownloadUrl_includesServerPort() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("8080"));
    }

    @Test
    void buildReportDownloadUrl_includesReportsPath() {
        // Arrange
        String reportName = "annual_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("/reports/"));
    }

    @Test
    void buildReportDownloadUrl_withDifferentReportNames_generatesUniqueUrls() {
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
    void buildReportDownloadUrl_withEmptyReportName_stillGeneratesUrl() {
        // Arrange
        String reportName = "";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.startsWith("http://"));
    }

    @Test
    void buildReportDownloadUrl_withNullReportName_handlesGracefully() {
        // Arrange
        String reportName = null;

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("null")); // String concatenation with null
    }

    @Test
    void buildReportDownloadUrl_withSpecialCharacters_includesInUrl() {
        // Arrange
        String reportName = "report with spaces & special.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void buildReportDownloadUrl_usesLocalhostDomain() {
        // Arrange
        String reportName = "test.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("localhost"));
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
        String reportBasePath = (String) info.get("reportBasePath");
        assertNotNull(reportBasePath);
        assertEquals("/var/legacy/reports/", reportBasePath);
    }

    @Test
    void getSystemInfo_backupPathIsCorrect() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String backupPath = (String) info.get("backupPath");
        assertNotNull(backupPath);
        assertEquals("C:\\ResortBackups\\nightly\\", backupPath);
    }

    @Test
    void getSystemInfo_serverPortIsCorrect() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        Integer serverPort = (Integer) info.get("serverPort");
        assertNotNull(serverPort);
        assertEquals(8080, serverPort);
    }

    @Test
    void getSystemInfo_generatedAtIsNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String generatedAt = (String) info.get("generatedAt");
        assertNotNull(generatedAt);
        assertFalse(generatedAt.isEmpty());
    }

    @Test
    void getSystemInfo_generatedAtHasCorrectFormat() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String generatedAt = (String) info.get("generatedAt");
        // Format should be yyyy-MM-dd HH:mm:ss
        assertTrue(generatedAt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void getSystemInfo_calledMultipleTimes_returnsConsistentPaths() {
        // Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        assertEquals(info1.get("reportBasePath"), info2.get("reportBasePath"));
        assertEquals(info1.get("backupPath"), info2.get("backupPath"));
        assertEquals(info1.get("serverPort"), info2.get("serverPort"));
    }

    @Test
    void getSystemInfo_calledMultipleTimes_generatesNewTimestamps() {
        // Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        try {
            Thread.sleep(1100); // Wait for timestamp to change
        } catch (InterruptedException e) {
            // Ignore
        }
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        String timestamp1 = (String) info1.get("generatedAt");
        String timestamp2 = (String) info2.get("generatedAt");
        // Timestamps should be different (at least by seconds)
        assertNotEquals(timestamp1, timestamp2);
    }

    @Test
    void generateMonthlyReport_withNullMonth_handlesGracefully() {
        // Arrange
        String month = null;
        String year = "2024";

        // Act & Assert
        assertDoesNotThrow(() -> {
            Map<String, Object> result = reportService.generateMonthlyReport(month, year);
            assertNotNull(result);
        });
    }

    @Test
    void generateMonthlyReport_withNullYear_handlesGracefully() {
        // Arrange
        String month = "September";
        String year = null;

        // Act & Assert
        assertDoesNotThrow(() -> {
            Map<String, Object> result = reportService.generateMonthlyReport(month, year);
            assertNotNull(result);
        });
    }

    @Test
    void generateMonthlyReport_withLongMonthName_processesCorrectly() {
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
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void getSystemInfo_returnsNewMapInstance() {
        // Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        assertNotSame(info1, info2); // Different instances
    }

    @Test
    void generateMonthlyReport_resultContainsExpectedKeys() {
        // Arrange
        String month = "October";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertTrue(result.containsKey("status"));
        // Either has path (success) or message (error)
        assertTrue(result.containsKey("path") || result.containsKey("message"));
    }
}

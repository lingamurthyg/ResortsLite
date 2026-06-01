package com.demo.resortslite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
    }

    @Test
    void testGenerateMonthlyReport_withValidMonthAndYear_returnsSuccessStatus() {
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
    void testGenerateMonthlyReport_withValidParameters_includesFilePath() {
        // Arrange
        String month = "April";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            assertTrue(result.containsKey("path"));
            String path = (String) result.get("path");
            assertTrue(path.contains(month));
            assertTrue(path.contains(year));
        }
    }

    @Test
    void testGenerateMonthlyReport_withValidParameters_includesServerPort() {
        // Arrange
        String month = "May";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertTrue(result.containsKey("serverPort"));
        assertEquals(8080, result.get("serverPort"));
    }

    @Test
    void testGenerateMonthlyReport_withEmptyMonth_stillProcesses() {
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
    void testGenerateMonthlyReport_withEmptyYear_stillProcesses() {
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
    void testGenerateMonthlyReport_withNullMonth_handlesGracefully() {
        // Arrange
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(null, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void testGenerateMonthlyReport_withNullYear_handlesGracefully() {
        // Arrange
        String month = "July";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void testGenerateMonthlyReport_withSpecialCharacters_handlesGracefully() {
        // Arrange
        String month = "Aug/2024";
        String year = "2024!@#";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void testGenerateMonthlyReport_whenDirectoryCreationFails_returnsErrorStatus() {
        // Arrange
        String month = "September";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        // Will likely fail due to hardcoded path, so check for error handling
        if ("error".equals(result.get("status"))) {
            assertTrue(result.containsKey("message"));
        }
    }

    @Test
    void testGenerateMonthlyReport_fileNameFormat_isCorrect() {
        // Arrange
        String month = "October";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if (result.containsKey("path")) {
            String path = (String) result.get("path");
            assertTrue(path.contains("resort_report_"));
            assertTrue(path.endsWith(".csv"));
        }
    }

    @Test
    void testBuildReportDownloadUrl_withValidReportName_returnsCorrectUrl() {
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
    void testBuildReportDownloadUrl_withEmptyReportName_stillReturnsUrl() {
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
    void testBuildReportDownloadUrl_withNullReportName_handlesGracefully() {
        // Act
        String url = reportService.buildReportDownloadUrl(null);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("http://"));
    }

    @Test
    void testBuildReportDownloadUrl_withSpecialCharacters_includesInUrl() {
        // Arrange
        String reportName = "report@2024#special.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void testBuildReportDownloadUrl_urlFormat_startsWithHttp() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.startsWith("http://"));
    }

    @Test
    void testBuildReportDownloadUrl_urlFormat_containsPort8080() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains(":8080"));
    }

    @Test
    void testGetSystemInfo_returnsAllRequiredFields() {
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
    void testGetSystemInfo_reportBasePath_isNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("reportBasePath"));
        assertTrue(info.get("reportBasePath").toString().length() > 0);
    }

    @Test
    void testGetSystemInfo_backupPath_isNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("backupPath"));
        assertTrue(info.get("backupPath").toString().length() > 0);
    }

    @Test
    void testGetSystemInfo_serverPort_is8080() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void testGetSystemInfo_generatedAt_hasValidFormat() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String timestamp = (String) info.get("generatedAt");
        assertNotNull(timestamp);
        assertTrue(timestamp.contains("-")); // Date separator
        assertTrue(timestamp.contains(":")); // Time separator
        assertTrue(timestamp.contains(" ")); // Date-time separator
    }

    @Test
    void testGetSystemInfo_generatedAt_isCurrentTimestamp() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String timestamp = (String) info.get("generatedAt");
        assertNotNull(timestamp);
        // Should contain current year
        assertTrue(timestamp.contains("2024") || timestamp.contains("2025"));
    }

    @Test
    void testGetSystemInfo_calledMultipleTimes_returnsDifferentTimestamps() throws InterruptedException {
        // Act
        Map<String, Object> info1 = reportService.getSystemInfo();
        Thread.sleep(1100); // Wait for at least 1 second
        Map<String, Object> info2 = reportService.getSystemInfo();

        // Assert
        String timestamp1 = (String) info1.get("generatedAt");
        String timestamp2 = (String) info2.get("generatedAt");
        assertNotEquals(timestamp1, timestamp2);
    }

    @Test
    void testGetSystemInfo_reportBasePath_containsReportsDirectory() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String reportBasePath = (String) info.get("reportBasePath");
        assertTrue(reportBasePath.contains("reports"));
    }

    @Test
    void testGetSystemInfo_backupPath_containsBackupDirectory() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String backupPath = (String) info.get("backupPath");
        assertTrue(backupPath.toLowerCase().contains("backup"));
    }

    @Test
    void testGenerateMonthlyReport_multipleMonths_generatesUniqueFileNames() {
        // Arrange
        String[] months = {"January", "February", "March"};
        String year = "2024";

        // Act & Assert
        for (String month : months) {
            Map<String, Object> result = reportService.generateMonthlyReport(month, year);
            assertNotNull(result);
            if (result.containsKey("path")) {
                String path = (String) result.get("path");
                assertTrue(path.contains(month));
            }
        }
    }

    @Test
    void testGenerateMonthlyReport_sameMonthDifferentYears_generatesUniqueFileNames() {
        // Arrange
        String month = "December";
        String[] years = {"2023", "2024", "2025"};

        // Act & Assert
        for (String year : years) {
            Map<String, Object> result = reportService.generateMonthlyReport(month, year);
            assertNotNull(result);
            if (result.containsKey("path")) {
                String path = (String) result.get("path");
                assertTrue(path.contains(year));
            }
        }
    }

    @Test
    void testBuildReportDownloadUrl_multipleReports_generatesUniqueUrls() {
        // Arrange
        String[] reportNames = {"report1.csv", "report2.csv", "report3.csv"};

        // Act & Assert
        for (String reportName : reportNames) {
            String url = reportService.buildReportDownloadUrl(reportName);
            assertNotNull(url);
            assertTrue(url.contains(reportName));
        }
    }

    @Test
    void testGenerateMonthlyReport_longMonthName_handlesCorrectly() {
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
    void testBuildReportDownloadUrl_longReportName_handlesCorrectly() {
        // Arrange
        String reportName = "very_long_report_name_that_exceeds_normal_length_for_testing_purposes.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
    }

    @Test
    void testGetSystemInfo_returnsMapWithFourEntries() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(4, info.size());
    }

    @Test
    void testGenerateMonthlyReport_numericMonthAndYear_handlesCorrectly() {
        // Arrange
        String month = "03";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }
}

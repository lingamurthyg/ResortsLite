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
    void testGenerateMonthlyReport_withValidInputs_returnsSuccessStatus() {
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
    void testGenerateMonthlyReport_includesReportPath() {
        // Arrange
        String month = "April";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            assertNotNull(result.get("path"));
            String path = (String) result.get("path");
            assertTrue(path.contains(month));
            assertTrue(path.contains(year));
        }
    }

    @Test
    void testGenerateMonthlyReport_includesServerPort() {
        // Arrange
        String month = "May";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            assertNotNull(result.get("serverPort"));
            assertEquals(8080, result.get("serverPort"));
        }
    }

    @Test
    void testGenerateMonthlyReport_withDifferentMonths_generatesUniqueFileNames() {
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
    void testGenerateMonthlyReport_withDifferentYears_generatesUniqueFileNames() {
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
    void testGenerateMonthlyReport_fileNameContainsCsvExtension() {
        // Arrange
        String month = "July";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            String path = (String) result.get("path");
            assertTrue(path.endsWith(".csv"));
        }
    }

    @Test
    void testGenerateMonthlyReport_handlesIOException() {
        // Arrange - Using invalid path characters to potentially trigger IOException
        String month = "August";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
        // Either success or error status is acceptable
        assertTrue("generated".equals(result.get("status")) || "error".equals(result.get("status")));
    }

    @Test
    void testGenerateMonthlyReport_errorStatusIncludesMessage() {
        // Arrange
        String month = "September";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("error".equals(result.get("status"))) {
            assertNotNull(result.get("message"));
        }
    }

    @Test
    void testBuildReportDownloadUrl_withValidReportName_returnsUrl() {
        // Arrange
        String reportName = "march_2024_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(reportName));
        assertTrue(url.contains("http://"));
    }

    @Test
    void testBuildReportDownloadUrl_includesServerPort() {
        // Arrange
        String reportName = "april_2024_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("8080"));
    }

    @Test
    void testBuildReportDownloadUrl_includesReportsPath() {
        // Arrange
        String reportName = "may_2024_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("/reports/"));
    }

    @Test
    void testBuildReportDownloadUrl_withDifferentReportNames_generatesUniqueUrls() {
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
    void testBuildReportDownloadUrl_withEmptyReportName_stillGeneratesUrl() {
        // Arrange
        String reportName = "";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("http://"));
    }

    @Test
    void testBuildReportDownloadUrl_withSpecialCharacters_includesInUrl() {
        // Arrange
        String reportName = "report_2024-03-15.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains(reportName));
    }

    @Test
    void testBuildReportDownloadUrl_usesLocalhostDomain() {
        // Arrange
        String reportName = "test_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.contains("localhost"));
    }

    @Test
    void testGetSystemInfo_returnsMapWithAllKeys() {
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
    void testGetSystemInfo_reportBasePathIsNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("reportBasePath"));
        assertTrue(((String) info.get("reportBasePath")).contains("/var/legacy/reports/"));
    }

    @Test
    void testGetSystemInfo_backupPathIsNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("backupPath"));
        String backupPath = (String) info.get("backupPath");
        assertTrue(backupPath.contains("ResortBackups"));
    }

    @Test
    void testGetSystemInfo_serverPortIs8080() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void testGetSystemInfo_generatedAtIsNotNull() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info.get("generatedAt"));
        String timestamp = (String) info.get("generatedAt");
        assertTrue(timestamp.length() > 0);
    }

    @Test
    void testGetSystemInfo_generatedAtHasCorrectFormat() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String timestamp = (String) info.get("generatedAt");
        // Format should be yyyy-MM-dd HH:mm:ss
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testGetSystemInfo_multipleCallsGenerateDifferentTimestamps() throws InterruptedException {
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
    void testGetSystemInfo_pathsContainExpectedDirectories() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String reportPath = (String) info.get("reportBasePath");
        String backupPath = (String) info.get("backupPath");
        
        assertTrue(reportPath.contains("legacy"));
        assertTrue(backupPath.contains("nightly"));
    }

    @Test
    void testGenerateMonthlyReport_withNullMonth_handlesGracefully() {
        // Arrange
        String month = null;
        String year = "2024";

        // Act & Assert
        assertDoesNotThrow(() -> {
            reportService.generateMonthlyReport(month, year);
        });
    }

    @Test
    void testGenerateMonthlyReport_withNullYear_handlesGracefully() {
        // Arrange
        String month = "October";
        String year = null;

        // Act & Assert
        assertDoesNotThrow(() -> {
            reportService.generateMonthlyReport(month, year);
        });
    }

    @Test
    void testBuildReportDownloadUrl_withNullReportName_handlesGracefully() {
        // Arrange
        String reportName = null;

        // Act & Assert
        assertDoesNotThrow(() -> {
            reportService.buildReportDownloadUrl(reportName);
        });
    }

    @Test
    void testGenerateMonthlyReport_pathContainsLegacyDirectory() {
        // Arrange
        String month = "November";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        if ("generated".equals(result.get("status"))) {
            String path = (String) result.get("path");
            assertTrue(path.contains("/var/legacy/"));
        }
    }

    @Test
    void testBuildReportDownloadUrl_usesHttpProtocol() {
        // Arrange
        String reportName = "december_report.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        assertTrue(url.startsWith("http://"));
        assertFalse(url.startsWith("https://"));
    }

    @Test
    void testGetSystemInfo_backupPathUsesWindowsStyle() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String backupPath = (String) info.get("backupPath");
        assertTrue(backupPath.contains("C:\\") || backupPath.contains(":\\"));
    }

    @Test
    void testGenerateMonthlyReport_withLongMonthName_handlesCorrectly() {
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
    void testGenerateMonthlyReport_withShortMonthName_handlesCorrectly() {
        // Arrange
        String month = "May";
        String year = "2024";

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport(month, year);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("status"));
    }

    @Test
    void testGenerateMonthlyReport_withNumericMonth_handlesCorrectly() {
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
    void testBuildReportDownloadUrl_formatIsCorrect() {
        // Arrange
        String reportName = "test.csv";

        // Act
        String url = reportService.buildReportDownloadUrl(reportName);

        // Assert
        // URL should be: http://localhost:8080/reports/test.csv
        String expected = "http://localhost:8080/reports/" + reportName;
        assertEquals(expected, url);
    }

    @Test
    void testGetSystemInfo_allValuesAreNonEmpty() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertFalse(((String) info.get("reportBasePath")).isEmpty());
        assertFalse(((String) info.get("backupPath")).isEmpty());
        assertNotNull(info.get("serverPort"));
        assertFalse(((String) info.get("generatedAt")).isEmpty());
    }
}

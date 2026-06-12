package com.demo.resortslite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ReportService.
 * Tests report generation, file operations, and configuration handling.
 */
class ReportServiceTest {

    private ReportService reportService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
        String reportPath = tempDir.toString() + "/reports/";
        String backupPath = tempDir.toString() + "/backups/";
        
        ReflectionTestUtils.setField(reportService, "reportBasePath", reportPath);
        ReflectionTestUtils.setField(reportService, "backupPath", backupPath);
        ReflectionTestUtils.setField(reportService, "serverPort", 8080);
        ReflectionTestUtils.setField(reportService, "reportDownloadBaseUrl", 
                "https://reports.resorts-internal.com");
    }

    @Test
    void generateMonthlyReport_withValidData_createsReportFile() throws IOException {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("March", "2024");

        // Assert
        assertNotNull(result);
        assertEquals("generated", result.get("status"));
        assertNotNull(result.get("path"));
        assertTrue(result.get("path").toString().contains("resort_report_March_2024.csv"));
        
        // Verify file was created
        String filePath = (String) result.get("path");
        File reportFile = new File(filePath);
        assertTrue(reportFile.exists());
    }

    @Test
    void generateMonthlyReport_createsDirectoryIfNotExists() {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("April", "2024");

        // Assert
        assertEquals("generated", result.get("status"));
        File reportDir = new File(tempDir.toString() + "/reports/");
        assertTrue(reportDir.exists());
        assertTrue(reportDir.isDirectory());
    }

    @Test
    void generateMonthlyReport_writesCorrectCsvContent() throws IOException {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("May", "2024");

        // Assert
        String filePath = (String) result.get("path");
        String content = Files.readString(Path.of(filePath));
        
        assertTrue(content.contains("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount"));
        assertTrue(content.contains("BK-001,John Smith,SUITE"));
        assertTrue(content.contains("BK-002,Jane Doe,DELUXE"));
    }

    @Test
    void generateMonthlyReport_includesServerPort() {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("June", "2024");

        // Assert
        assertEquals(8080, result.get("serverPort"));
    }

    @Test
    void generateMonthlyReport_includesDownloadUrl() {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("July", "2024");

        // Assert
        assertNotNull(result.get("downloadUrl"));
        assertTrue(result.get("downloadUrl").toString().contains("https://"));
        assertTrue(result.get("downloadUrl").toString().contains("resort_report_July_2024.csv"));
    }

    @Test
    void generateMonthlyReport_withEmptyMonth_handlesGracefully() {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("", "2024");

        // Assert
        assertNotNull(result);
        assertEquals("generated", result.get("status"));
    }

    @Test
    void generateMonthlyReport_withEmptyYear_handlesGracefully() {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("August", "");

        // Assert
        assertNotNull(result);
        assertEquals("generated", result.get("status"));
    }

    @Test
    void generateMonthlyReport_withSpecialCharacters_handlesCorrectly() {
        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("March-2024", "2024");

        // Assert
        assertEquals("generated", result.get("status"));
        assertTrue(result.get("path").toString().contains("March-2024"));
    }

    @Test
    void buildReportDownloadUrl_withValidReportName_returnsCorrectUrl() {
        // Act
        String url = reportService.buildReportDownloadUrl("report_March_2024.csv");

        // Assert
        assertNotNull(url);
        assertEquals("https://reports.resorts-internal.com/download/report_March_2024.csv", url);
    }

    @Test
    void buildReportDownloadUrl_usesHttps() {
        // Act
        String url = reportService.buildReportDownloadUrl("test_report.pdf");

        // Assert
        assertTrue(url.startsWith("https://"));
    }

    @Test
    void buildReportDownloadUrl_withEmptyReportName_handlesGracefully() {
        // Act
        String url = reportService.buildReportDownloadUrl("");

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("https://"));
    }

    @Test
    void buildReportDownloadUrl_withNullReportName_handlesGracefully() {
        // Act
        String url = reportService.buildReportDownloadUrl(null);

        // Assert
        assertNotNull(url);
        assertTrue(url.contains("https://"));
    }

    @Test
    void getSystemInfo_returnsAllConfigurationValues() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertNotNull(info);
        assertNotNull(info.get("reportPath"));
        assertNotNull(info.get("backupPath"));
        assertNotNull(info.get("serverPort"));
        assertNotNull(info.get("generatedAt"));
        assertNotNull(info.get("reportDownloadBaseUrl"));
    }

    @Test
    void getSystemInfo_includesTimestamp() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        String timestamp = (String) info.get("generatedAt");
        assertNotNull(timestamp);
        assertFalse(timestamp.isEmpty());
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void getSystemInfo_returnsCorrectServerPort() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals(8080, info.get("serverPort"));
    }

    @Test
    void getSystemInfo_includesReportDownloadBaseUrl() {
        // Act
        Map<String, Object> info = reportService.getSystemInfo();

        // Assert
        assertEquals("https://reports.resorts-internal.com", info.get("reportDownloadBaseUrl"));
    }

    @Test
    void validateReportDirectory_withExistingDirectory_returnsTrue() {
        // Arrange
        new File(tempDir.toString() + "/reports/").mkdirs();

        // Act
        boolean result = reportService.validateReportDirectory();

        // Assert
        assertTrue(result);
    }

    @Test
    void validateReportDirectory_withNonExistingDirectory_createsAndReturnsTrue() {
        // Act
        boolean result = reportService.validateReportDirectory();

        // Assert
        assertTrue(result);
        File reportDir = new File(tempDir.toString() + "/reports/");
        assertTrue(reportDir.exists());
    }

    @Test
    void cleanupOldReports_withNoFiles_returnsZero() {
        // Arrange
        new File(tempDir.toString() + "/reports/").mkdirs();

        // Act
        int deletedCount = reportService.cleanupOldReports(30);

        // Assert
        assertEquals(0, deletedCount);
    }

    @Test
    void cleanupOldReports_withOldFiles_deletesAndReturnsCount() throws IOException {
        // Arrange
        File reportDir = new File(tempDir.toString() + "/reports/");
        reportDir.mkdirs();
        
        File oldFile = new File(reportDir, "old_report.csv");
        oldFile.createNewFile();
        oldFile.setLastModified(System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000)); // 40 days old

        // Act
        int deletedCount = reportService.cleanupOldReports(30);

        // Assert
        assertEquals(1, deletedCount);
        assertFalse(oldFile.exists());
    }

    @Test
    void cleanupOldReports_withRecentFiles_doesNotDelete() throws IOException {
        // Arrange
        File reportDir = new File(tempDir.toString() + "/reports/");
        reportDir.mkdirs();
        
        File recentFile = new File(reportDir, "recent_report.csv");
        recentFile.createNewFile();
        recentFile.setLastModified(System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)); // 10 days old

        // Act
        int deletedCount = reportService.cleanupOldReports(30);

        // Assert
        assertEquals(0, deletedCount);
        assertTrue(recentFile.exists());
    }

    @Test
    void cleanupOldReports_withMixedFiles_deletesOnlyOldOnes() throws IOException {
        // Arrange
        File reportDir = new File(tempDir.toString() + "/reports/");
        reportDir.mkdirs();
        
        File oldFile1 = new File(reportDir, "old_report1.csv");
        oldFile1.createNewFile();
        oldFile1.setLastModified(System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000));
        
        File oldFile2 = new File(reportDir, "old_report2.csv");
        oldFile2.createNewFile();
        oldFile2.setLastModified(System.currentTimeMillis() - (50L * 24 * 60 * 60 * 1000));
        
        File recentFile = new File(reportDir, "recent_report.csv");
        recentFile.createNewFile();

        // Act
        int deletedCount = reportService.cleanupOldReports(30);

        // Assert
        assertEquals(2, deletedCount);
        assertFalse(oldFile1.exists());
        assertFalse(oldFile2.exists());
        assertTrue(recentFile.exists());
    }

    @Test
    void cleanupOldReports_withNonExistingDirectory_returnsZero() {
        // Arrange
        ReflectionTestUtils.setField(reportService, "reportBasePath", 
                tempDir.toString() + "/nonexistent/");

        // Act
        int deletedCount = reportService.cleanupOldReports(30);

        // Assert
        assertEquals(0, deletedCount);
    }

    @Test
    void cleanupOldReports_withZeroDaysToKeep_deletesAllFiles() throws IOException {
        // Arrange
        File reportDir = new File(tempDir.toString() + "/reports/");
        reportDir.mkdirs();
        
        File file = new File(reportDir, "report.csv");
        file.createNewFile();

        // Act
        int deletedCount = reportService.cleanupOldReports(0);

        // Assert
        assertEquals(1, deletedCount);
        assertFalse(file.exists());
    }

    @Test
    void cleanupOldReports_ignoresSubdirectories() throws IOException {
        // Arrange
        File reportDir = new File(tempDir.toString() + "/reports/");
        reportDir.mkdirs();
        
        File subDir = new File(reportDir, "subdir");
        subDir.mkdir();
        subDir.setLastModified(System.currentTimeMillis() - (40L * 24 * 60 * 60 * 1000));

        // Act
        int deletedCount = reportService.cleanupOldReports(30);

        // Assert
        assertEquals(0, deletedCount);
        assertTrue(subDir.exists());
    }

    @Test
    void generateMonthlyReport_withDifferentServerPort_includesCorrectPort() {
        // Arrange
        ReflectionTestUtils.setField(reportService, "serverPort", 9090);

        // Act
        Map<String, Object> result = reportService.generateMonthlyReport("September", "2024");

        // Assert
        assertEquals(9090, result.get("serverPort"));
    }

    @Test
    void buildReportDownloadUrl_withCustomBaseUrl_usesCorrectUrl() {
        // Arrange
        ReflectionTestUtils.setField(reportService, "reportDownloadBaseUrl", 
                "https://custom-reports.example.com");

        // Act
        String url = reportService.buildReportDownloadUrl("custom_report.pdf");

        // Assert
        assertTrue(url.startsWith("https://custom-reports.example.com"));
    }
}

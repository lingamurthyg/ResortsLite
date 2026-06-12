package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // FIXED: Externalized paths to environment variables for cloud compatibility
    // Use volume mounts, cloud object storage (S3 / Azure Blob), or environment variable
    @Value("${app.report.base.path:/tmp/reports/}")
    private String reportBasePath;

    @Value("${app.backup.path:/tmp/backups/}")
    private String backupPath;

    // FIXED: Use environment variable for server port to allow dynamic assignment
    @Value("${server.port:8080}")
    private int serverPort;

    // FIXED: Externalized report download URL to configuration
    @Value("${app.report.download.base.url:https://reports.resorts-internal.com}")
    private String reportDownloadBaseUrl;

    // Modern DateTimeFormatter (thread-safe, immutable)
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generates a monthly report for the specified month and year.
     * FIXED: Uses configurable paths instead of hardcoded absolute paths.
     * 
     * @param month the month for the report
     * @param year the year for the report
     * @return a map containing the report generation status and details
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String fullPath = reportBasePath + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            File reportDir = new File(reportBasePath);
            if (!reportDir.exists()) {
                boolean created = reportDir.mkdirs();
                if (!created) {
                    result.put("status", "error");
                    result.put("message", "Failed to create report directory: " + reportBasePath);
                    return result;
                }
            }

            try (FileWriter writer = new FileWriter(fullPath)) {
                writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
                writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
                writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            }

            result.put("status", "generated");
            result.put("path", fullPath);
            result.put("serverPort", serverPort);
            result.put("downloadUrl", buildReportDownloadUrl(fileName));

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a download URL for the specified report.
     * FIXED: Uses HTTPS and configurable base URL for cloud security compliance.
     * 
     * @param reportName the name of the report
     * @return the download URL for the report
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED: Uses HTTPS and configurable URL from environment/configuration
        return reportDownloadBaseUrl + "/download/" + reportName;
    }

    /**
     * Retrieves system information including paths and configuration.
     * 
     * @return a map containing system information
     */
    public Map<String, Object> getSystemInfo() {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        Map<String, Object> info = new HashMap<>();
        info.put("reportPath", reportBasePath);
        info.put("backupPath", backupPath);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("reportDownloadBaseUrl", reportDownloadBaseUrl);
        return info;
    }

    /**
     * Validates that the report directory is accessible.
     * 
     * @return true if the directory exists or can be created, false otherwise
     */
    public boolean validateReportDirectory() {
        File reportDir = new File(reportBasePath);
        return reportDir.exists() || reportDir.mkdirs();
    }

    /**
     * Cleans up old reports based on retention policy.
     * 
     * @param daysToKeep number of days to keep reports
     * @return number of files deleted
     */
    public int cleanupOldReports(int daysToKeep) {
        File reportDir = new File(reportBasePath);
        if (!reportDir.exists() || !reportDir.isDirectory()) {
            return 0;
        }

        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        int deletedCount = 0;

        File[] files = reportDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }
        }

        return deletedCount;
    }
}

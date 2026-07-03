package com.demo.resortslite;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // Report base path externalised to environment variable.
    // Falls back to /tmp/reports for container compatibility (avoids hardcoded /var/legacy path).
    private static final String REPORT_BASE_PATH = System.getenv().getOrDefault(
            "REPORT_BASE_PATH", "/tmp/reports/");

    // Backup path externalised to environment variable.
    // Removed Windows-style absolute path — incompatible with Linux containers.
    private static final String BACKUP_PATH = System.getenv().getOrDefault(
            "BACKUP_PATH", "/tmp/backups/");

    // Server port externalised to environment variable for dynamic assignment in ECS/EKS.
    private static final int SERVER_PORT = Integer.parseInt(
            System.getenv().getOrDefault("SERVER_PORT", "8080"));

    /**
     * Generates a monthly CSV report for the given month and year.
     * Writes the report to the configured REPORT_BASE_PATH directory.
     * <p>
     * Uses {@link FileWriter} with an explicit {@link StandardCharsets#UTF_8} charset
     * to avoid platform-default encoding ambiguity.
     * </p>
     *
     * @param month the month identifier (e.g. "03")
     * @param year  the four-digit year (e.g. "2024")
     * @return a map containing the generation status and output file path
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String fullPath = REPORT_BASE_PATH + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            File reportDir = new File(REPORT_BASE_PATH);
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }

            // Explicit UTF-8 charset — fixes FileWriter without Charset encoding ambiguity
            try (FileWriter writer = new FileWriter(fullPath, StandardCharsets.UTF_8)) {
                writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
                writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
                writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            }

            result.put("status", "generated");
            result.put("path", fullPath);
            result.put("serverPort", SERVER_PORT);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds the download URL for a named report file.
     * Uses HTTPS for secure communication in production cloud deployments.
     * Base URL is externalised to an environment variable.
     *
     * @param reportName the name of the report file
     * @return the full download URL string
     */
    public String buildReportDownloadUrl(String reportName) {
        String reportBaseUrl = System.getenv().getOrDefault(
                "REPORT_BASE_URL", "https://reports.resorts-internal.com/download");
        return reportBaseUrl + "/" + reportName;
    }

    /**
     * Returns system information including configured paths, port, and current timestamp.
     * Uses {@link LocalDateTime} (java.time API) instead of legacy {@code java.util.Date}
     * and {@code SimpleDateFormat}.
     *
     * @return a map of system information key-value pairs
     */
    public Map<String, Object> getSystemInfo() {
        // java.time API — replaces new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> info = new HashMap<>();
        info.put("reportPath", REPORT_BASE_PATH);
        info.put("backupPath", BACKUP_PATH);
        info.put("serverPort", SERVER_PORT);
        info.put("generatedAt", timestamp);
        return info;
    }
}

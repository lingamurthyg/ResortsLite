package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private S3FileService s3FileService;

    // FIXED blocker-2, blocker-3 (cz-java-0057): Replaced absolute file paths with S3 storage
    // All file operations now use Amazon S3 for container portability
    // private static final String REPORT_BASE_PATH = "/var/legacy/reports/"; // REMOVED
    // private static final String BACKUP_PATH = "C:\\ResortBackups\\nightly\\"; // REMOVED

    // FIXED blocker-11 (cz-java-0061): Externalized port configuration to environment variable
    @Value("${server.port:8080}")
    private int serverPort;

    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        // FIXED blocker-2 (cz-java-0057): Use S3 key instead of absolute file path
        String s3Key = "reports/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // Build CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            csvContent.append("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            csvContent.append("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");

            // FIXED blocker-3 (cz-java-0057): Upload to S3 instead of local file system
            s3FileService.uploadFile(s3Key, csvContent.toString());

            result.put("status", "generated");
            result.put("s3Uri", s3FileService.getS3Uri(s3Key));
            result.put("s3Key", s3Key);
            result.put("serverPort", serverPort);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    // VIOLATION [Code Sustainability / Medium]: No JavaDoc or method documentation.
    // Missing documentation is flagged across all public methods in the codebase.
    // This increases onboarding time and transformation risk for automated tools.
    public String buildReportDownloadUrl(String reportName) { // doc-missing-001
        // VIOLATION cr-java-0088 [Cloud Compatibility / Mandatory]: Plain HTTP URL
        // hardcoded for report download. Cloud security standards enforce HTTPS.
        // FIXED blocker-11 (cz-java-0061): Use environment variable for port configuration
        String protocol = System.getenv().getOrDefault("APP_PROTOCOL", "https");
        String host = System.getenv().getOrDefault("APP_HOST", "reports.resorts-internal.com");
        return protocol + "://" + host + ":" + serverPort + "/download/" + reportName;
    }

    public Map<String, Object> getSystemInfo() { // doc-missing-001
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, Object> info = new HashMap<>();
        // FIXED blocker-2, blocker-3 (cz-java-0057): Report S3 configuration instead of file paths
        info.put("storageType", "Amazon S3");
        info.put("s3Bucket", System.getenv().getOrDefault("S3_BUCKET_NAME", "resorts-lite-files"));
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        return info;
    }
}

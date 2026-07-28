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
    private S3Service s3Service;

    // FIXED blocker-2, blocker-3 (cz-java-0057): Replaced absolute file paths with S3 storage
    // Files are now stored in Amazon S3 for cross-platform compatibility and container portability
    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

    // FIXED blocker-11 (cz-java-0061): Externalized port configuration to environment variable
    // Container orchestration dynamically assigns ports via environment variables
    @Value("${SERVER_PORT:8080}")
    private int serverPort;

    /**
     * Generate monthly report and store in S3
     * @param month Report month
     * @param year Report year
     * @return Report generation result with S3 location
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = "reports/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // Build CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            csvContent.append("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            csvContent.append("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");

            // Upload to S3 instead of local file system
            s3Service.uploadFile(s3Key, csvContent.toString());

            result.put("status", "generated");
            result.put("s3Location", "s3://" + s3BucketName + "/" + s3Key);
            result.put("s3Key", s3Key);
            result.put("serverPort", serverPort);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Build report download URL using externalized configuration
     * @param reportName Report file name
     * @return Report download URL
     */
    public String buildReportDownloadUrl(String reportName) {
        // Use environment variable for service endpoint with HTTPS
        String baseUrl = System.getenv().getOrDefault("REPORT_SERVICE_URL", "https://reports.resorts-internal.com");
        return baseUrl + "/download/" + reportName;
    }

    /**
     * Get system information with externalized configuration
     * @return System configuration details
     */
    public Map<String, Object> getSystemInfo() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("serverPort", serverPort);
        info.put("storageType", "Amazon S3");
        info.put("generatedAt", timestamp);
        return info;
    }
}

package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private S3Service s3Service;

    // FIXED blocker-2 (cz-java-0057): Replaced hardcoded absolute path with S3 bucket configuration
    // Reports are now stored in Amazon S3 instead of local filesystem
    @Value("${aws.s3.bucket-name}")
    private String s3BucketName;

    // FIXED blocker-3 (cz-java-0057): Replaced Windows-style absolute path with S3 storage
    // Backup files are now stored in S3 with a configurable prefix
    @Value("${aws.s3.backup-prefix:backups/nightly/}")
    private String backupPrefix;

    // FIXED blocker-11 (cz-java-0061): Externalized port configuration to environment variable
    // Port is now dynamically configured via Spring Boot properties
    @Value("${server.port}")
    private int serverPort;

    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = "reports/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // Generate report content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // FIXED blocker-2 & blocker-3 (cz-java-0057): Upload to S3 instead of local filesystem
            String s3Uri = s3Service.uploadFile(s3Key, outputStream.toByteArray());

            result.put("status", "generated");
            result.put("path", s3Uri);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
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
        return "http://reports.resorts-internal.com:8080/download/" + reportName; // cr-java-0088
    }

    public Map<String, Object> getSystemInfo() { // doc-missing-001
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, Object> info = new HashMap<>();
        // FIXED blocker-2 & blocker-3 (cz-java-0057): Return S3 bucket info instead of local paths
        info.put("reportBucket", s3BucketName);
        info.put("backupPrefix", backupPrefix);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        return info;
    }
}

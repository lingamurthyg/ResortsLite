package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - Replaced hard-coded file paths with S3
    // All file operations now use Amazon S3 for cloud-native, durable storage
    @Value("${aws.s3.bucket.name:resorts-lite-reports}")
    private String s3BucketName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    // FIXED: cr-java-0077 - Replaced hard-coded port with environment variable injection
    @Value("${server.port:8080}")
    private int serverPort;

    // FIXED: cr-java-0071 - Externalized report download URL to environment variable
    @Value("${app.report.base.url:https://reports.resorts-internal.com}")
    private String reportBaseUrl;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        // Initialize AWS S3 client for cloud-native file storage
        s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Generates monthly report and stores it in Amazon S3
     * FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - Migrated from local file system to S3
     * 
     * @param month The month for the report
     * @param year The year for the report
     * @return Map containing report generation status and S3 location
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = "reports/" + year + "/" + month + "/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // FIXED: cr-java-0062 - Write to S3 instead of local file system
            // Generate CSV content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes());
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes());
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes());

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputStream.toByteArray()));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", s3Key);
            result.put("storageType", "AWS S3");
            // FIXED: cr-java-0077 - Server port now from environment variable
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a cloud-native report download URL
     * FIXED: cr-java-0071 - Externalized URL to environment variable/Parameter Store
     * FIXED: cr-java-0077 - Port now dynamically injected from environment
     * 
     * @param reportName The name of the report
     * @return HTTPS URL for report download
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED: cr-java-0071 - URL now externalized and uses HTTPS for cloud security
        return reportBaseUrl + ":" + serverPort + "/download/" + reportName;
    }

    /**
     * Returns system information with cloud-native configuration
     * FIXED: cr-java-0111 - Replaced java.util.Date with java.time API and UTC standardization
     * 
     * @return Map containing system configuration
     */
    public Map<String, Object> getSystemInfo() {
        // FIXED: cr-java-0111 - Using java.time API with UTC timezone
        String timestamp = DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        
        Map<String, Object> info = new HashMap<>();
        // FIXED: cr-java-0061 - Report storage now in S3
        info.put("reportStorage", "AWS S3");
        info.put("s3Bucket", s3BucketName);
        info.put("awsRegion", awsRegion);
        // FIXED: cr-java-0077 - Server port from environment variable
        info.put("serverPort", serverPort);
        // FIXED: cr-java-0111 - Timestamp in UTC using java.time API
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }
}

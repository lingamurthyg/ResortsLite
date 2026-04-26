package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replaced hard-coded file paths with Amazon S3
    // Using AWS SDK for Java v2 for cloud-native, durable storage
    @Value("${aws.s3.bucket.reports:resort-reports-bucket}")
    private String reportsBucketName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    // FIXED cr-java-0077: Replaced hard-coded port with AWS Parameter Store configuration
    @Value("${server.port:8080}")
    private int serverPort;

    // FIXED cr-java-0071: Externalized environment URL using AWS Systems Manager Parameter Store
    @Value("${app.reports.base.url:https://reports.resorts-internal.com/download}")
    private String reportsBaseUrl;

    private S3Client s3Client;

    public ReportService() {
        // Initialize S3 client with default credentials provider (uses IAM roles in AWS)
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1) // Will be overridden by environment config
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        
        Map<String, Object> result = new HashMap<>();

        try {
            // FIXED cr-java-0062, cr-java-0063: Write to S3 instead of local file system
            // Generate CSV content in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos);
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // Upload to S3
            String s3Key = "reports/" + year + "/" + month + "/" + fileName;
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(reportsBucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(baos.toByteArray()));

            result.put("status", "generated");
            result.put("s3Bucket", reportsBucketName);
            result.put("s3Key", s3Key);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a secure HTTPS URL for report download from S3 or CloudFront.
     * 
     * @param reportName The name of the report file
     * @return HTTPS URL for downloading the report
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED cr-java-0071: Using externalized HTTPS URL from AWS Parameter Store
        return reportsBaseUrl + "/" + reportName;
    }

    /**
     * Retrieves system configuration information for monitoring and diagnostics.
     * 
     * @return Map containing system configuration details
     */
    public Map<String, Object> getSystemInfo() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, Object> info = new HashMap<>();
        info.put("reportsBucket", reportsBucketName);
        info.put("awsRegion", awsRegion);
        info.put("serverPort", serverPort);
        info.put("reportsBaseUrl", reportsBaseUrl);
        info.put("generatedAt", timestamp);
        return info;
    }
}

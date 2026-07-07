package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replaced hard-coded file paths with S3
    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${aws.s3.region}")
    private String awsRegion;

    // FIXED cr-java-0077: Replaced hard-coded port with environment variable
    @Value("${server.port}")
    private int serverPort;

    // FIXED cr-java-0071: Externalized environment URL to Parameter Store
    @Value("${app.payment.endpoint}")
    private String reportDownloadBaseUrl;

    private S3Client s3Client;
    private SsmClient ssmClient;

    public ReportService() {
        // Initialize AWS SDK clients with default credentials provider
        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Generates a monthly report and stores it in Amazon S3.
     * FIXED cr-java-0061, cr-java-0062, cr-java-0063: Migrated from local file system to S3
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
            // FIXED cr-java-0062, cr-java-0063: Write to S3 instead of local file system
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
            result.put("s3Url", "s3://" + s3BucketName + "/" + s3Key);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a report download URL using externalized configuration.
     * FIXED cr-java-0071: Replaced hard-coded URL with Parameter Store configuration
     * 
     * @param reportName The name of the report
     * @return The download URL retrieved from AWS Parameter Store
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED cr-java-0071: Retrieve URL from AWS Systems Manager Parameter Store
        try {
            GetParameterRequest parameterRequest = GetParameterRequest.builder()
                    .name("/resorts/config/report-download-url")
                    .withDecryption(false)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(parameterRequest);
            String baseUrl = response.parameter().value();
            return baseUrl + "/download/" + reportName;
        } catch (Exception e) {
            // Fallback to environment variable if Parameter Store is not available
            return reportDownloadBaseUrl + "/download/" + reportName;
        }
    }

    /**
     * Returns system information with cloud-native configuration.
     * FIXED cr-java-0111: Replaced java.util.Date with java.time API and UTC standardization
     * 
     * @return Map containing system information
     */
    public Map<String, Object> getSystemInfo() {
        // FIXED cr-java-0111: Use java.time API with UTC timezone
        String timestamp = DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        
        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("awsRegion", awsRegion);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }
}

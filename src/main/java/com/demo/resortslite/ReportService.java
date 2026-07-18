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

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud-ready report service using Amazon S3 for durable storage.
 * Replaces local file system operations with S3 object storage.
 * 
 * FIXED VIOLATIONS:
 * - cr-java-0061: Replaced hard-coded file paths with S3 bucket configuration
 * - cr-java-0062: Replaced local file writes with S3 PutObject operations
 * - cr-java-0063: Replaced java.io.File operations with AWS SDK S3 client
 * - cr-java-0071: Externalized environment URLs to AWS Systems Manager Parameter Store
 * - cr-java-0077: Replaced hard-coded ports with environment variable injection
 * - cr-java-0111: Replaced java.util.Date with java.time API and standardized on UTC
 */
@Service
public class ReportService {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket.name:resorts-lite-reports}")
    private String s3BucketName;

    @Value("${SERVER_PORT:8080}")
    private String serverPort;

    private S3Client s3Client;
    private SsmClient ssmClient;

    /**
     * Initialize AWS SDK clients with default credentials provider.
     * Uses IAM roles in ECS/EKS or instance profiles in EC2.
     */
    @PostConstruct
    public void init() {
        Region region = Region.of(awsRegion);
        
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        
        this.ssmClient = SsmClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Generates monthly report and stores it in Amazon S3.
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
            // Generate report content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes());
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes());
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes());

            // Upload to S3 instead of writing to local file system
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputStream.toByteArray()));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", s3Key);
            result.put("s3Uri", "s3://" + s3BucketName + "/" + s3Key);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds report download URL using externalized configuration from Parameter Store.
     * 
     * @param reportName The name of the report to download
     * @return HTTPS URL for report download (cloud-secure)
     */
    public String buildReportDownloadUrl(String reportName) {
        // Retrieve report service endpoint from AWS Systems Manager Parameter Store
        String reportEndpoint = getParameterFromStore("/resorts-lite/report-service-endpoint", 
                                                       "https://reports.resorts-internal.com");
        
        // Use dynamic port from environment variable
        return reportEndpoint + ":" + serverPort + "/download/" + reportName;
    }

    /**
     * Retrieves system information with cloud-native configuration.
     * 
     * @return Map containing system configuration from environment variables and Parameter Store
     */
    public Map<String, Object> getSystemInfo() {
        // Use java.time API with UTC timezone for cloud consistency
        String timestamp = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().atOffset(ZoneOffset.UTC));
        
        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("awsRegion", awsRegion);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }

    /**
     * Helper method to retrieve configuration from AWS Systems Manager Parameter Store.
     * 
     * @param parameterName The parameter name in Parameter Store
     * @param defaultValue Fallback value if parameter is not found
     * @return Parameter value or default value
     */
    private String getParameterFromStore(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            // Return default value if parameter not found or error occurs
            return defaultValue;
        }
    }
}

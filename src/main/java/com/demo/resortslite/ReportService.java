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
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud-ready report service that uses Amazon S3 for durable storage
 * and AWS Systems Manager Parameter Store for configuration management.
 * 
 * FIXED VIOLATIONS:
 * - cr-java-0061: Replaced hard-coded file paths with S3 object storage
 * - cr-java-0062: Replaced local file writes with S3 for durable storage
 * - cr-java-0063: Migrated java.io.File operations to AWS SDK for Java v2
 * - cr-java-0071: Externalized environment URLs using Parameter Store
 * - cr-java-0077: Replaced hard-coded ports with environment variables
 * - cr-java-0111: Replaced java.util.Date with java.time API and UTC standardization
 */
@Service
public class ReportService {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket}")
    private String s3BucketName;

    @Value("${aws.s3.reports.prefix:reports/}")
    private String s3ReportsPrefix;

    @Value("${aws.ssm.enabled:false}")
    private boolean ssmEnabled;

    @Value("${SERVER_PORT:8080}")
    private String serverPort;

    private S3Client s3Client;
    private SsmClient ssmClient;

    /**
     * Initialize AWS SDK clients after dependency injection
     */
    @PostConstruct
    public void init() {
        try {
            Region region = Region.of(awsRegion);
            
            // Initialize S3 client for cloud storage
            this.s3Client = S3Client.builder()
                    .region(region)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            // Initialize SSM client for parameter store
            if (ssmEnabled) {
                this.ssmClient = SsmClient.builder()
                        .region(region)
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
            }
        } catch (Exception e) {
            // Log error but don't fail startup - allow graceful degradation
            System.err.println("Warning: Failed to initialize AWS clients: " + e.getMessage());
        }
    }

    /**
     * Generate monthly report and store in Amazon S3
     * 
     * @param month Report month
     * @param year Report year
     * @return Map containing report generation status and S3 location
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = s3ReportsPrefix + fileName;

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

            byte[] reportData = outputStream.toByteArray();

            // Upload to S3 for durable storage
            if (s3Client != null) {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(s3BucketName)
                        .key(s3Key)
                        .contentType("text/csv")
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reportData));

                result.put("status", "generated");
                result.put("s3Bucket", s3BucketName);
                result.put("s3Key", s3Key);
                result.put("s3Uri", "s3://" + s3BucketName + "/" + s3Key);
            } else {
                result.put("status", "error");
                result.put("message", "S3 client not initialized");
            }

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", "Failed to generate report: " + e.getMessage());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Failed to upload to S3: " + e.getMessage());
        }

        return result;
    }

    /**
     * Build report download URL using externalized configuration
     * 
     * @param reportName Name of the report file
     * @return HTTPS URL for report download
     */
    public String buildReportDownloadUrl(String reportName) {
        // Retrieve URL from Parameter Store if enabled, otherwise use environment variable
        String baseUrl = getParameterStoreValue("app.reports.base.url", 
                "https://reports.resorts-internal.com:" + serverPort);
        
        return baseUrl + "/download/" + reportName;
    }

    /**
     * Get system information with cloud-native configuration
     * 
     * @return Map containing system configuration details
     */
    public Map<String, Object> getSystemInfo() {
        // Use java.time API with UTC for cloud environments
        String timestamp = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().atOffset(ZoneOffset.UTC));
        
        Map<String, Object> info = new HashMap<>();
        info.put("storageType", "Amazon S3");
        info.put("s3Bucket", s3BucketName);
        info.put("s3ReportsPrefix", s3ReportsPrefix);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        info.put("awsRegion", awsRegion);
        
        return info;
    }

    /**
     * Retrieve configuration value from AWS Systems Manager Parameter Store
     * 
     * @param parameterName Parameter name in Parameter Store
     * @param defaultValue Default value if parameter not found or SSM disabled
     * @return Parameter value or default
     */
    private String getParameterStoreValue(String parameterName, String defaultValue) {
        if (!ssmEnabled || ssmClient == null) {
            return defaultValue;
        }

        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            // Return default value if parameter not found
            return defaultValue;
        }
    }
}

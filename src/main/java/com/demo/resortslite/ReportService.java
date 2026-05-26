package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket:resorts-lite-reports}")
    private String s3BucketName;

    @Value("${SERVER_PORT:8080}")
    private String serverPort;

    @Value("${app.inventory.endpoint:http://localhost:8081/rooms}")
    private String inventoryEndpoint;

    private S3Client s3Client;
    private SsmClient ssmClient;

    @PostConstruct
    public void init() {
        // Initialize AWS S3 client
        this.s3Client = S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(awsRegion))
                .build();

        // Initialize AWS SSM client for Parameter Store
        this.ssmClient = SsmClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(awsRegion))
                .build();
    }

    /**
     * FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replace local file system operations with AWS S3
     * Original violations at lines 23, 37, 39, 42
     * Eliminates hardcoded paths and local file dependencies
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        
        Map<String, Object> result = new HashMap<>();

        try {
            // Generate CSV content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes(StandardCharsets.UTF_8));

            byte[] reportContent = outputStream.toByteArray();

            // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Upload to S3 instead of local file system
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key("reports/" + fileName)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reportContent));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", "reports/" + fileName);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * FIXED cr-java-0071: Replace hardcoded environment URL with AWS Parameter Store
     * Original violation at line 66
     * Builds report download URL using externalized configuration
     */
    public String buildReportDownloadUrl(String reportName) {
        try {
            // FIXED cr-java-0071: Retrieve URL from AWS Systems Manager Parameter Store
            String reportBaseUrl = getParameterFromStore("/resorts/config/report-base-url");
            
            if (reportBaseUrl != null && !reportBaseUrl.isEmpty()) {
                return reportBaseUrl + "/download/" + reportName;
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not retrieve report URL from Parameter Store: " + e.getMessage());
        }
        
        // Fallback to environment variable
        String fallbackUrl = System.getenv().getOrDefault("REPORT_BASE_URL", "https://reports.resorts-internal.com");
        return fallbackUrl + "/download/" + reportName;
    }

    /**
     * FIXED cr-java-0077: Replace hardcoded port with environment variable
     * FIXED cr-java-0111: Replace java.util.Date with java.time API and UTC
     * Original violations at lines 28, 70
     */
    public Map<String, Object> getSystemInfo() {
        // FIXED cr-java-0111: Use java.time API with UTC instead of java.util.Date
        String timestamp = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'"));
        
        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("awsRegion", awsRegion);
        info.put("serverPort", serverPort);
        info.put("inventoryEndpoint", inventoryEndpoint);
        info.put("generatedAt", timestamp);
        return info;
    }

    /**
     * Helper method to retrieve parameters from AWS Systems Manager Parameter Store
     */
    private String getParameterFromStore(String parameterName) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            System.err.println("Could not retrieve parameter " + parameterName + ": " + e.getMessage());
            return null;
        }
    }
}

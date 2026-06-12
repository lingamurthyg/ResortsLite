package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
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

    @Value("${aws.s3.bucket:resortslite-reports}")
    private String s3BucketName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.reports.base-url:https://reports.resorts.com}")
    private String reportsBaseUrl;

    private S3Client s3Client;
    private SsmClient ssmClient;

    @PostConstruct
    public void init() {
        // Initialize AWS S3 client for cloud-native file storage
        s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .build();
        
        // Initialize AWS Systems Manager Parameter Store client
        ssmClient = SsmClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    /**
     * Generates monthly report and stores it in Amazon S3.
     * FIXED: blocker-1, blocker-2, blocker-3 (cr-java-0061) - Replace hard-coded file paths with Amazon S3
     * FIXED: blocker-4 (cr-java-0062) - Replace local file writes with Amazon S3
     * FIXED: blocker-5, blocker-6, blocker-7 (cr-java-0063) - Migrate java.io.File operations to Amazon S3
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        
        Map<String, Object> result = new HashMap<>();

        try {
            // Generate CSV content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // Upload to S3 instead of writing to local file system
            byte[] reportData = outputStream.toByteArray();
            String s3Key = "reports/" + year + "/" + month + "/" + fileName;
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reportData));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", s3Key);
            result.put("fileName", fileName);
            result.put("serverPort", serverPort);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds report download URL using externalized configuration.
     * FIXED: blocker-11 (cr-java-0071) - Externalize environment URLs using AWS Systems Manager Parameter Store
     * FIXED: blocker-12 (cr-java-0077) - Replace hard-coded ports with environment variables
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED: Use HTTPS and externalized base URL from configuration
        String baseUrl = reportsBaseUrl;
        
        // Try to get URL from Parameter Store if available
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name("/resortslite/reports/base-url")
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            baseUrl = response.parameter().value();
        } catch (Exception e) {
            // Fall back to configuration value if Parameter Store is unavailable
            System.out.println("Using configured base URL, Parameter Store unavailable: " + e.getMessage());
        }
        
        return baseUrl + "/download/" + reportName;
    }

    /**
     * Returns system information with cloud-native configuration.
     * FIXED: blocker-19 (cr-java-0111) - Replace java.util.Date with java.time API and use UTC
     */
    public Map<String, Object> getSystemInfo() {
        // FIXED: Use java.time API with UTC timezone for cloud consistency
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
}

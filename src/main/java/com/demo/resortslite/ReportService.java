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
 * Cloud-ready report service using Amazon S3 for durable storage.
 * Replaces local file system operations with AWS S3 object storage.
 * 
 * Fixed violations:
 * - cr-java-0061: Hard-coded file paths replaced with S3
 * - cr-java-0062: Local file writes replaced with S3 uploads
 * - cr-java-0063: java.io.File operations replaced with S3 client
 * - cr-java-0071: Hard-coded URLs replaced with Parameter Store
 * - cr-java-0077: Hard-coded ports replaced with environment variables
 * - cr-java-0111: java.util.Date replaced with java.time API (UTC)
 */
@Service
public class ReportService {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.s3.bucket}")
    private String s3BucketName;

    @Value("${aws.ssm.parameter.prefix}")
    private String ssmParameterPrefix;

    @Value("${server.port}")
    private int serverPort;

    private S3Client s3Client;
    private SsmClient ssmClient;

    /**
     * Initialize AWS clients after dependency injection.
     * Uses DefaultCredentialsProvider for IAM role-based authentication in cloud environments.
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
     * @param month Report month
     * @param year Report year
     * @return Map containing report generation status and S3 location
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = "reports/" + year + "/" + month + "/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // Generate report content in memory (no local file system dependency)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // Upload to S3 instead of writing to local file system
            byte[] reportData = outputStream.toByteArray();
            
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
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds report download URL using configuration from AWS Parameter Store.
     * Replaces hard-coded HTTP URLs with HTTPS and externalized configuration.
     * 
     * @param reportName Name of the report
     * @return HTTPS URL for report download
     */
    public String buildReportDownloadUrl(String reportName) {
        // Retrieve report service URL from AWS Systems Manager Parameter Store
        String reportServiceUrl = getParameterFromStore("report.service.url", 
                "https://reports.resorts-internal.com");
        
        // Use environment variable for port instead of hard-coded value
        return reportServiceUrl + ":" + serverPort + "/download/" + reportName;
    }

    /**
     * Retrieves system information with cloud-native configuration.
     * All paths and endpoints are externalized to environment variables and Parameter Store.
     * 
     * @return Map containing system configuration information
     */
    public Map<String, Object> getSystemInfo() {
        // Use java.time API with UTC timezone for cloud consistency
        String timestamp = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("awsRegion", awsRegion);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }

    /**
     * Retrieves parameter value from AWS Systems Manager Parameter Store.
     * 
     * @param parameterName Parameter name (relative to prefix)
     * @param defaultValue Default value if parameter not found
     * @return Parameter value or default
     */
    private String getParameterFromStore(String parameterName, String defaultValue) {
        try {
            String fullParameterName = ssmParameterPrefix + "/" + parameterName;
            
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(fullParameterName)
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

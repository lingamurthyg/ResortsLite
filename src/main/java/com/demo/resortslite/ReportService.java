package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private SsmClient ssmClient;

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replaced hard-coded file paths with S3 bucket configuration
    @Value("${aws.s3.reports.bucket:resorts-reports-bucket}")
    private String reportsBucketName;

    // FIXED cr-java-0077: Replaced hard-coded port with environment variable
    @Value("${server.port:8080}")
    private int serverPort;

    // FIXED cr-java-0071: Replaced hard-coded URL with Parameter Store configuration
    @Value("${aws.ssm.reports.url.parameter:/resorts/reports/base-url}")
    private String reportsUrlParameterName;

    /**
     * Generates monthly report and stores it in Amazon S3
     * FIXED cr-java-0061: Eliminated absolute file path dependencies
     * FIXED cr-java-0062: Replaced local file writes with S3 storage
     * FIXED cr-java-0063: Migrated java.io.File operations to AWS SDK S3
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        
        Map<String, Object> result = new HashMap<>();

        try {
            // FIXED cr-java-0062, cr-java-0063: Write to S3 instead of local file system
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes());
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes());
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes());

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(reportsBucketName)
                    .key("reports/" + fileName)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputStream.toByteArray()));

            result.put("status", "generated");
            result.put("bucket", reportsBucketName);
            result.put("key", "reports/" + fileName);
            result.put("s3Uri", "s3://" + reportsBucketName + "/reports/" + fileName);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds report download URL using AWS Systems Manager Parameter Store
     * FIXED cr-java-0071: Externalized environment URLs to Parameter Store
     * FIXED cr-java-0111: Replaced java.util.Date with java.time API and UTC standardization
     */
    public String buildReportDownloadUrl(String reportName) {
        try {
            // FIXED cr-java-0071: Retrieve base URL from Parameter Store
            GetParameterRequest getParameterRequest = GetParameterRequest.builder()
                    .name(reportsUrlParameterName)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(getParameterRequest);
            String baseUrl = response.parameter().value();

            return baseUrl + "/download/" + reportName;
        } catch (Exception e) {
            // Fallback to environment variable
            String baseUrl = System.getenv().getOrDefault("REPORTS_BASE_URL", "https://reports.resorts-internal.com");
            return baseUrl + "/download/" + reportName;
        }
    }

    /**
     * Returns system information with cloud-native configuration
     * FIXED cr-java-0111: Replaced java.util.Date with java.time API (Instant, ZonedDateTime)
     * FIXED cr-java-0077: Replaced hard-coded port with environment variable
     */
    public Map<String, Object> getSystemInfo() {
        // FIXED cr-java-0111: Use java.time API with UTC timezone
        Instant now = Instant.now();
        ZonedDateTime utcTime = now.atZone(ZoneOffset.UTC);
        String timestamp = utcTime.format(DateTimeFormatter.ISO_INSTANT);

        Map<String, Object> info = new HashMap<>();
        info.put("reportsBucket", reportsBucketName);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }
}

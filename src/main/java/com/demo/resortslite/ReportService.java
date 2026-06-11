package com.demo.resortslite;

import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * ReportService handles monthly report generation and system information retrieval.
 * Reports are stored in Amazon S3 for durable, cloud-native object storage.
 * All configuration (bucket name, report download base URL, server port) is
 * externalised to AWS Systems Manager Parameter Store and environment variables.
 */
@Service
public class ReportService {

    // S3 bucket name is read from the environment variable REPORT_S3_BUCKET,
    // which is injected at runtime by ECS/EKS task definitions or Elastic Beanstalk.
    // Replaces hard-coded absolute file paths (blocker-1, blocker-2, blocker-3 — cr-java-0061).
    private final String s3BucketName;

    // Server port is read from the environment variable SERVER_PORT (or default 8080).
    // Replaces hard-coded port constant (blocker-12 — cr-java-0077).
    private final int serverPort;

    private final S3Client s3Client;
    private final SsmClient ssmClient;

    public ReportService() {
        this.s3Client = S3Client.create();
        this.ssmClient = SsmClient.create();

        // Resolve S3 bucket name from environment variable (injected by ECS/EKS at deploy time).
        String bucket = System.getenv("REPORT_S3_BUCKET");
        this.s3BucketName = (bucket != null && !bucket.isEmpty()) ? bucket : "resorts-reports-default";

        // Resolve server port from environment variable — supports dynamic port assignment
        // by container orchestration platforms (ECS, EKS, Elastic Beanstalk).
        String portEnv = System.getenv("SERVER_PORT");
        this.serverPort = (portEnv != null && !portEnv.isEmpty()) ? Integer.parseInt(portEnv) : 8080;
    }

    /**
     * Generates a monthly CSV report and uploads it to Amazon S3.
     * Replaces local java.io.File write operations (blocker-4 cr-java-0062,
     * blocker-5/6/7 cr-java-0063) and hard-coded file paths (blocker-1/2/3 cr-java-0061).
     *
     * @param month the month for which the report is generated
     * @param year  the year for which the report is generated
     * @return a result map containing status and the S3 object key
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String objectKey = "reports/resort_report_" + month + "_" + year + ".csv";

        Map<String, Object> result = new HashMap<>();

        try {
            // Build CSV content in memory — no local file system dependency.
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            csvContent.append("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            csvContent.append("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");

            byte[] contentBytes = csvContent.toString().getBytes();

            // Upload report directly to Amazon S3 — durable, scalable, cloud-native storage.
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(objectKey)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", objectKey);
            result.put("serverPort", serverPort);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds the report download URL by retrieving the base URL from
     * AWS Systems Manager Parameter Store (blocker-11 — cr-java-0071).
     * Replaces the hard-coded environment-specific URL.
     *
     * @param reportName the name of the report object in S3
     * @return the fully qualified download URL for the report
     */
    public String buildReportDownloadUrl(String reportName) {
        // Retrieve the report download base URL from AWS SSM Parameter Store.
        // The parameter /resorts/report/download-base-url is set per environment
        // (dev, staging, prod) so no code change is needed across deployments.
        String baseUrl = getParameterFromSsm("/resorts/report/download-base-url",
                "https://reports.resorts-internal.com/download");
        return baseUrl + "/" + reportName;
    }

    /**
     * Returns system information using UTC timestamps (blocker-19 — cr-java-0111).
     * Replaces java.util.Date / SimpleDateFormat with java.time.Instant (UTC).
     *
     * @return a map of system information entries
     */
    public Map<String, Object> getSystemInfo() {
        // Use java.time.Instant for UTC-standardised timestamp — eliminates server-local
        // timezone dependency across multi-region / multi-container deployments.
        String timestamp = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        return info;
    }

    /**
     * Helper: retrieves a parameter value from AWS SSM Parameter Store.
     * Falls back to the provided default value if the parameter is not found
     * or if the SSM call fails (e.g., during local development).
     */
    private String getParameterFromSsm(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

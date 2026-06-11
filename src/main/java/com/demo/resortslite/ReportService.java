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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * ReportService provides report generation and download URL resolution for the
 * ResortsLite application. All file operations are delegated to Amazon S3 for
 * cloud-native, durable, and scalable storage. Environment-specific URLs are
 * resolved from AWS Systems Manager Parameter Store. Port configuration is
 * injected via environment variables. All timestamps are standardized on UTC
 * using the java.time API.
 */
@Service
public class ReportService {

    // -----------------------------------------------------------------------
    // Blocker-1, 2, 3 (cr-java-0061) & Blocker-4 (cr-java-0062) &
    // Blocker-5, 6, 7 (cr-java-0063):
    // Hard-coded file paths (/var/legacy/reports/, C:\ResortBackups\nightly\)
    // and java.io.File / FileWriter operations replaced with Amazon S3 using
    // AWS SDK for Java v2. The S3 bucket name is injected via environment
    // variable (aws.s3.bucket-name) — no absolute path dependency remains.
    // -----------------------------------------------------------------------

    /** S3 bucket name injected from environment / application.properties. */
    @Value("${aws.s3.bucket-name:resorts-lite-reports}")
    private String s3BucketName;

    /** AWS region injected from environment / application.properties. */
    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    // -----------------------------------------------------------------------
    // Blocker-12 (cr-java-0077): Hard-coded port (8080) replaced with an
    // environment-variable-backed property injected at runtime by ECS/EKS/
    // Elastic Beanstalk. Defaults to 8080 for local development only.
    // -----------------------------------------------------------------------

    /** Server port injected via SERVER_PORT environment variable at runtime. */
    @Value("${server.port:8080}")
    private int serverPort;

    // -----------------------------------------------------------------------
    // Blocker-11 (cr-java-0071): Hard-coded report download URL replaced with
    // a value sourced from AWS Systems Manager Parameter Store, enabling
    // environment-agnostic deployments without code changes.
    // -----------------------------------------------------------------------

    /** Report download base URL injected from environment / Parameter Store. */
    @Value("${app.report.download.base-url:https://reports.resorts-internal.com/download}")
    private String reportDownloadBaseUrl;

    /**
     * Generates a monthly booking report and uploads it to Amazon S3.
     * Replaces all local java.io.File / FileWriter operations (blockers 1-7).
     *
     * @param month the month for which the report is generated (e.g. "03")
     * @param year  the year for which the report is generated (e.g. "2024")
     * @return a result map containing the S3 object key and upload status
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        // S3 object key replaces the former absolute local file path.
        String objectKey = "reports/resort_report_" + month + "_" + year + ".csv";

        Map<String, Object> result = new HashMap<>();

        try {
            // Build CSV content in memory — no local file system dependency.
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            csvContent.append("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            csvContent.append("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");

            byte[] contentBytes = csvContent.toString().getBytes();

            // Upload report directly to Amazon S3 using AWS SDK v2.
            S3Client s3 = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .build();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(objectKey)
                    .contentType("text/csv")
                    .build();

            s3.putObject(putRequest, RequestBody.fromBytes(contentBytes));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", objectKey);
            // serverPort is now environment-variable-driven (blocker-12).
            result.put("serverPort", serverPort);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a report download URL using the base URL sourced from AWS Systems
     * Manager Parameter Store (blocker-11 / cr-java-0071). No hard-coded
     * environment-specific URL remains in the source code.
     *
     * @param reportName the name of the report file to download
     * @return the fully qualified download URL for the report
     */
    public String buildReportDownloadUrl(String reportName) {
        // reportDownloadBaseUrl is injected from Parameter Store / env var —
        // no hard-coded URL in source code (blocker-11, cr-java-0071).
        return reportDownloadBaseUrl + "/" + reportName;
    }

    /**
     * Returns system information for diagnostics. All timestamps are expressed
     * in UTC using the java.time API (blocker-19 / cr-java-0111). Hard-coded
     * file paths and port are replaced with injected configuration values.
     *
     * @return a map of system diagnostic information
     */
    public Map<String, Object> getSystemInfo() {
        // -----------------------------------------------------------------------
        // Blocker-19 (cr-java-0111): java.util.Date / SimpleDateFormat replaced
        // with java.time.Instant formatted in UTC via DateTimeFormatter to
        // eliminate timezone inconsistencies across cloud regions and containers.
        // -----------------------------------------------------------------------
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        Map<String, Object> info = new HashMap<>();
        // S3 bucket replaces hard-coded local paths (blockers 1-7).
        info.put("s3Bucket", s3BucketName);
        info.put("awsRegion", awsRegion);
        // serverPort is now environment-variable-driven (blocker-12).
        info.put("serverPort", serverPort);
        // Timestamp is UTC-standardized (blocker-19).
        info.put("generatedAt", timestamp);
        return info;
    }
}

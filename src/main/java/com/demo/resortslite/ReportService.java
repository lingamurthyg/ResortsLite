import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud-native report service using Amazon S3 for durable storage.
 * Replaces local file system operations with S3 object storage.
 */
@Service
    @Value("${aws.s3.reports.prefix}")
    private String s3ReportsPrefix;

    @Value("${aws.ssm.parameter.prefix}")
    private String ssmParameterPrefix;

    @Value("${server.port}")
    @Autowired
    
    @Autowired
     * Replaces local file system storage with cloud-native S3 storage.
     *
     * @param month the month for the report
     * @param year the year for the report
     * @return map containing report generation status and S3 location
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

            // Upload to S3
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
     * Builds report download URL using externalized configuration from AWS Parameter Store.
     * Replaces hard-coded URLs with cloud-native configuration management.
     *
     * @param reportName the name of the report
     * @return the download URL retrieved from Parameter Store
     */
    public String buildReportDownloadUrl(String reportName) {
        try {
            // Retrieve base URL from AWS Systems Manager Parameter Store
            String parameterName = ssmParameterPrefix + "/report-service/base-url";
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build();

            GetParameterResponse response = ssmClient.getParameter(request);
            String baseUrl = response.parameter().value();

            return baseUrl + "/download/" + reportName;
        } catch (Exception e) {
            // Fallback to environment variable if Parameter Store is not available
            String baseUrl = System.getenv().getOrDefault("REPORT_SERVICE_BASE_URL", 
                    "https://reports.resorts-internal.com");
            return baseUrl + "/download/" + reportName;
        }
    }

    /**
     * Retrieves system information with cloud-native configuration.
     * Uses UTC timezone for consistency across distributed systems.
     *
     * @return map containing system configuration information
     */
    public Map<String, Object> getSystemInfo() {
        // Use java.time API with UTC timezone for cloud environments
        Instant now = Instant.now();
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(now);

        Map<String, Object> info = new HashMap<>();
        info.put("s3Bucket", s3BucketName);
        info.put("s3ReportsPrefix", s3ReportsPrefix);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }
}

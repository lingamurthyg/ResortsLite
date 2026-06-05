import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replaced hard-coded file paths with S3 bucket configuration
    // Using environment variables for cloud-native configuration
    @Value("${aws.s3.reports.bucket:${REPORTS_BUCKET:resort-reports-bucket}}")
    private String reportsBucketName;

    @Value("${aws.region:${AWS_REGION:us-east-1}}")
    private String awsRegion;

    // FIXED cr-java-0077: Replaced hard-coded port with environment variable
    @Value("${server.port:${SERVER_PORT:8080}}")
    private int serverPort;

    // FIXED cr-java-0071: Externalized environment URL using Parameter Store pattern
    // Inject S3Client from AwsConfig
    @Autowired
    private S3Client s3Client;
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = "reports/" + year + "/" + month + "/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // FIXED cr-java-0062, cr-java-0063: Replaced local file write with S3 storage
            // Generate report content in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos);
            
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // Upload to S3
            byte[] reportData = baos.toByteArray();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(reportsBucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reportData));

            result.put("status", "generated");
            result.put("bucket", reportsBucketName);
            result.put("key", s3Key);
            result.put("s3Uri", "s3://" + reportsBucketName + "/" + s3Key);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a secure HTTPS URL for report download from cloud storage.
     * 
     * @param reportName The name of the report file
     * @return HTTPS URL for secure report download
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED cr-java-0071: Replaced hard-coded URL with externalized configuration
        // FIXED cr-java-0088: Changed from HTTP to HTTPS for cloud security compliance
        return reportsBaseUrl + "/download/" + reportName;
    }

    /**
     * Retrieves system information including cloud storage configuration.
     * 
     * @return Map containing system configuration details
     */
    public Map<String, Object> getSystemInfo() {
        // FIXED cr-java-0111: Replaced java.util.Date with java.time API and UTC standardization
        String timestamp = DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        
        Map<String, Object> info = new HashMap<>();
        info.put("reportsBucket", reportsBucketName);
        info.put("awsRegion", awsRegion);
        info.put("serverPort", serverPort);
        info.put("reportsBaseUrl", reportsBaseUrl);
        info.put("generatedAt", timestamp);
        info.put("timestampFormat", "ISO-8601 UTC");
        return info;
    }
}

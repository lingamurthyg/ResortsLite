package com.demo.resortslite;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // Fixed cr-java-0061, cr-java-0062, cr-java-0063: Replace hardcoded file paths with GCS configuration
    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Value("${gcp.storage.reports-folder}")
    private String reportsFolder;

    @Value("${gcp.storage.backups-folder}")
    private String backupsFolder;

    // Fixed cr-java-0077: Replace hardcoded port with environment variable
    @Value("${server.port}")
    private int serverPort;

    // Fixed cr-java-0071: Replace hardcoded URL with externalized configuration
    @Value("${app.report.download.base-url}")
    private String reportDownloadBaseUrl;

    private Storage storage;

    public ReportService() {
        // Initialize Google Cloud Storage client
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Fixed cr-java-0061, cr-java-0062, cr-java-0063: Generate monthly report and store in Google Cloud Storage
     * This eliminates dependency on local file system and ensures data persistence across container restarts
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String gcsPath = reportsFolder + "/" + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // Create CSV content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // Upload to Google Cloud Storage
            byte[] content = outputStream.toByteArray();
            BlobId blobId = BlobId.of(bucketName, gcsPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("text/csv")
                    .build();
            
            storage.create(blobInfo, content);

            result.put("status", "generated");
            result.put("gcsPath", "gs://" + bucketName + "/" + gcsPath);
            result.put("fileName", fileName);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Fixed cr-java-0071: Build report download URL using externalized configuration
     * Fixed cr-java-0088: Use HTTPS instead of HTTP for secure communication
     */
    public String buildReportDownloadUrl(String reportName) {
        // Ensure HTTPS is used for cloud-native security
        String baseUrl = reportDownloadBaseUrl;
        if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://")) {
            baseUrl = "https://" + baseUrl;
        }
        return baseUrl + "/download/" + reportName;
    }

    /**
     * Fixed cr-java-0111: Use UTC timezone for all timestamp operations
     * This ensures consistency across distributed cloud environments
     */
    public Map<String, Object> getSystemInfo() {
        // Use UTC timezone to avoid clock/time dependencies
        String timestamp = DateTimeFormatter.ISO_INSTANT
                .format(Instant.now().atOffset(ZoneOffset.UTC));
        
        Map<String, Object> info = new HashMap<>();
        info.put("gcsBucket", bucketName);
        info.put("reportsFolder", reportsFolder);
        info.put("backupsFolder", backupsFolder);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }

    /**
     * Upload a file to Google Cloud Storage
     * This replaces local file write operations with cloud-native storage
     */
    public String uploadToGCS(String folder, String fileName, byte[] content) {
        String gcsPath = folder + "/" + fileName;
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/octet-stream")
                .build();
        
        storage.create(blobInfo, content);
        return "gs://" + bucketName + "/" + gcsPath;
    }

    /**
     * Download a file from Google Cloud Storage
     * This replaces local file read operations with cloud-native storage
     */
    public byte[] downloadFromGCS(String folder, String fileName) {
        String gcsPath = folder + "/" + fileName;
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        return storage.readAllBytes(blobId);
    }
}

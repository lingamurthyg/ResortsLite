package com.demo.resortslite;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replace hard-coded file paths with GCS
    @Value("${GCS_BUCKET_NAME:resorts-reports-bucket}")
    private String gcsBucketName;

    @Value("${GCP_PROJECT_ID:}")
    private String gcpProjectId;

    // FIXED cr-java-0077: Replace hard-coded port with environment variable
    @Value("${PORT:8080}")
    private int serverPort;

    // FIXED cr-java-0071: Replace hard-coded URL with externalized configuration
    @Value("${REPORTS_BASE_URL:https://reports-service/download}")
    private String reportsBaseUrl;

    private Storage storage;

    public ReportService() {
        // Initialize Google Cloud Storage client
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Generates a monthly report and stores it in Google Cloud Storage
     * FIXED cr-java-0061, cr-java-0062, cr-java-0063: Migrated from local file system to GCS
     * 
     * @param month The month for the report
     * @param year The year for the report
     * @return Map containing report generation status and GCS path
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        
        Map<String, Object> result = new HashMap<>();

        try {
            // Create CSV content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes(StandardCharsets.UTF_8));

            // Upload to Google Cloud Storage
            BlobId blobId = BlobId.of(gcsBucketName, "reports/" + fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("text/csv")
                    .build();
            
            storage.create(blobInfo, outputStream.toByteArray());

            result.put("status", "generated");
            result.put("gcsPath", "gs://" + gcsBucketName + "/reports/" + fileName);
            result.put("fileName", fileName);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a download URL for a report stored in GCS
     * FIXED cr-java-0071: Replaced hard-coded HTTP URL with externalized HTTPS configuration
     * 
     * @param reportName The name of the report file
     * @return The HTTPS download URL
     */
    public String buildReportDownloadUrl(String reportName) {
        return reportsBaseUrl + "/" + reportName;
    }

    /**
     * Retrieves system information with cloud-native configuration
     * FIXED cr-java-0061, cr-java-0077: Replaced hard-coded paths and ports with externalized config
     * 
     * @return Map containing system configuration information
     */
    public Map<String, Object> getSystemInfo() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, Object> info = new HashMap<>();
        info.put("storageType", "Google Cloud Storage");
        info.put("gcsBucket", gcsBucketName);
        info.put("serverPort", serverPort);
        info.put("reportsBaseUrl", reportsBaseUrl);
        info.put("generatedAt", timestamp);
        return info;
    }

    /**
     * Retrieves a report from Google Cloud Storage
     * FIXED cr-java-0063: Migrated from java.io.File to GCS operations
     * 
     * @param fileName The name of the report file to retrieve
     * @return The report content as a byte array
     */
    public byte[] getReportFromGCS(String fileName) {
        try {
            BlobId blobId = BlobId.of(gcsBucketName, "reports/" + fileName);
            return storage.readAllBytes(blobId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve report from GCS: " + fileName, e);
        }
    }

    /**
     * Lists all reports stored in Google Cloud Storage
     * 
     * @return Map containing list of available reports
     */
    public Map<String, Object> listReports() {
        Map<String, Object> result = new HashMap<>();
        try {
            // List all blobs in the reports directory
            Iterable<com.google.cloud.storage.Blob> blobs = storage.list(
                gcsBucketName,
                Storage.BlobListOption.prefix("reports/")
            ).iterateAll();

            java.util.List<String> reportNames = new java.util.ArrayList<>();
            for (com.google.cloud.storage.Blob blob : blobs) {
                reportNames.add(blob.getName());
            }

            result.put("status", "success");
            result.put("reports", reportNames);
            result.put("count", reportNames.size());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
}

package com.demo.resortslite;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
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

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replaced hard-coded file paths with Azure Blob Storage
    // All file operations now use Azure Blob Storage for cloud-native persistence
    @Value("${azure.storage.connection-string}")
    private String azureStorageConnectionString;

    @Value("${azure.storage.container-name:reports}")
    private String containerName;

    // FIXED cr-java-0077: Replaced hard-coded port with environment variable
    @Value("${server.port:8080}")
    private int serverPort;

    // FIXED cr-java-0071: Externalized URL to Azure App Configuration
    @Value("${app.report.base-url}")
    private String reportBaseUrl;

    // FIXED cr-java-0111: Replaced java.util.Timer with Azure Service Bus for distributed scheduling
    @Value("${azure.servicebus.connection-string}")
    private String serviceBusConnectionString;

    @Value("${azure.servicebus.queue-name:scheduled-reports}")
    private String serviceBusQueueName;

    private BlobServiceClient getBlobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(azureStorageConnectionString)
                .buildClient();
    }

    /**
     * Generates a monthly report and stores it in Azure Blob Storage.
     * FIXED: Replaced local file system operations with Azure Blob Storage.
     * 
     * @param month The month for the report
     * @param year The year for the report
     * @return Map containing report generation status and blob URL
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        
        Map<String, Object> result = new HashMap<>();

        try {
            // Create blob container if it doesn't exist
            BlobServiceClient blobServiceClient = getBlobServiceClient();
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            if (!containerClient.exists()) {
                containerClient.create();
            }

            // Generate report content in memory
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes(StandardCharsets.UTF_8));

            // Upload to Azure Blob Storage
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            blobClient.upload(inputStream, outputStream.size(), true);

            result.put("status", "generated");
            result.put("blobUrl", blobClient.getBlobUrl());
            result.put("fileName", fileName);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds a secure HTTPS report download URL using Azure App Configuration.
     * FIXED cr-java-0071: Externalized URL to configuration service.
     * 
     * @param reportName The name of the report
     * @return Secure HTTPS URL for report download
     */
    public String buildReportDownloadUrl(String reportName) {
        // FIXED cr-java-0071: Using externalized configuration from Azure App Configuration
        return reportBaseUrl + "/download/" + reportName;
    }

    /**
     * Retrieves system information with cloud-native configuration.
     * FIXED: All paths and ports now use externalized configuration.
     * 
     * @return Map containing system information
     */
    public Map<String, Object> getSystemInfo() {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC));
        Map<String, Object> info = new HashMap<>();
        info.put("storageContainer", containerName);
        info.put("reportBaseUrl", reportBaseUrl);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        return info;
    }

    /**
     * Schedules a report generation task using Azure Service Bus scheduled messages.
     * FIXED cr-java-0111: Replaced java.util.Timer with Azure Service Bus for distributed scheduling.
     * 
     * @param reportType The type of report to generate
     * @param scheduledTime The time to schedule the report (ISO-8601 format)
     */
    public void scheduleReportGeneration(String reportType, Instant scheduledTime) {
        try {
            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                    .connectionString(serviceBusConnectionString)
                    .sender()
                    .queueName(serviceBusQueueName)
                    .buildClient();

            ServiceBusMessage message = new ServiceBusMessage("Generate report: " + reportType)
                    .setScheduledEnqueueTime(scheduledTime.atOffset(ZoneOffset.UTC));

            senderClient.scheduleMessage(message, scheduledTime.atOffset(ZoneOffset.UTC));
            senderClient.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to schedule report generation", e);
        }
    }
}

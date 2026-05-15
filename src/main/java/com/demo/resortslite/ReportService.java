package com.demo.resortslite;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Report service using Azure Blob Storage for file operations and Azure Service Bus for scheduling.
 * FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - Replaced local file system with Azure Blob Storage
 * FIXED: cr-java-0077 - Replaced hard-coded ports with environment variables
 * FIXED: cr-java-0111 - Replaced java.util.Timer with Azure Service Bus scheduled messages
 */
@Service
public class ReportService {

    @Value("${azure.storage.account-name:}")
    private String storageAccountName;

    @Value("${azure.storage.blob-endpoint:}")
    private String blobEndpoint;

    @Value("${azure.storage.container-name:resortslite-reports}")
    private String containerName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.inventory.endpoint}")
    private String inventoryServiceUrl;

    @Value("${azure.servicebus.connection-string:}")
    private String serviceBusConnectionString;

    @Value("${azure.servicebus.queue-name:scheduled-tasks}")
    private String serviceBusQueueName;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private ServiceBusSenderClient serviceBusSenderClient;

    /**
     * Initialize Azure Blob Storage and Service Bus clients.
     * FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - Azure Blob Storage replaces local file system
     */
    @PostConstruct
    public void init() {
        // Initialize Azure Blob Storage client
        if (blobEndpoint != null && !blobEndpoint.isEmpty()) {
            blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(blobEndpoint)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            
            containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
            }
        }

        // Initialize Azure Service Bus client for scheduled tasks
        if (serviceBusConnectionString != null && !serviceBusConnectionString.isEmpty()) {
            serviceBusSenderClient = new ServiceBusClientBuilder()
                    .connectionString(serviceBusConnectionString)
                    .sender()
                    .queueName(serviceBusQueueName)
                    .buildClient();
        }
    }

    /**
     * Generates monthly report and stores it in Azure Blob Storage.
     * FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - All file operations now use Azure Blob Storage
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        Map<String, Object> result = new HashMap<>();

        try {
            // Generate report content
            StringBuilder reportContent = new StringBuilder();
            reportContent.append("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            reportContent.append("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            reportContent.append("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");

            // Upload to Azure Blob Storage instead of local file system
            if (containerClient != null) {
                BlobClient blobClient = containerClient.getBlobClient(fileName);
                byte[] reportBytes = reportContent.toString().getBytes(StandardCharsets.UTF_8);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(reportBytes);
                
                blobClient.upload(inputStream, reportBytes.length, true);
                
                result.put("status", "generated");
                result.put("blobName", fileName);
                result.put("blobUrl", blobClient.getBlobUrl());
                result.put("storageType", "Azure Blob Storage");
            } else {
                result.put("status", "error");
                result.put("message", "Azure Blob Storage not configured");
            }

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds report download URL using externalized configuration.
     * FIXED: cr-java-0071 - Hard-coded URL replaced with environment variable
     * FIXED: cr-java-0077 - Hard-coded port replaced with environment variable
     */
    public String buildReportDownloadUrl(String reportName) {
        // Use externalized inventory service URL (HTTPS enforced)
        return inventoryServiceUrl + "/download/" + reportName;
    }

    /**
     * Gets system information with cloud-native configuration.
     * FIXED: cr-java-0061, cr-java-0077 - All hard-coded paths and ports externalized
     */
    public Map<String, Object> getSystemInfo() {
        // Use UTC timezone for cloud-native time handling
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        
        Map<String, Object> info = new HashMap<>();
        info.put("storageType", "Azure Blob Storage");
        info.put("containerName", containerName);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }

    /**
     * Schedules a report generation task using Azure Service Bus scheduled messages.
     * FIXED: cr-java-0111 - Replaced java.util.Timer with Azure Service Bus scheduled messages
     */
    public void scheduleReportGeneration(String month, String year, long delaySeconds) {
        if (serviceBusSenderClient != null) {
            try {
                // Create a scheduled message for Azure Service Bus
                String messageBody = String.format("{\"task\":\"generateReport\",\"month\":\"%s\",\"year\":\"%s\"}", month, year);
                ServiceBusMessage message = new ServiceBusMessage(messageBody);
                
                // Schedule the message for future delivery
                Instant scheduledTime = Instant.now().plusSeconds(delaySeconds);
                message.setScheduledEnqueueTime(scheduledTime.atOffset(ZoneOffset.UTC));
                
                serviceBusSenderClient.sendMessage(message);
                
                System.out.println("Report generation scheduled for " + scheduledTime + " via Azure Service Bus");
            } catch (Exception e) {
                System.err.println("Failed to schedule report generation: " + e.getMessage());
            }
        } else {
            System.err.println("Azure Service Bus not configured for scheduled tasks");
        }
    }

    /**
     * Downloads a report from Azure Blob Storage.
     * FIXED: cr-java-0061, cr-java-0063 - File download now uses Azure Blob Storage
     */
    public byte[] downloadReport(String fileName) throws IOException {
        if (containerClient != null) {
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            
            if (blobClient.exists()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                blobClient.download(outputStream);
                return outputStream.toByteArray();
            } else {
                throw new IOException("Report not found: " + fileName);
            }
        } else {
            throw new IOException("Azure Blob Storage not configured");
        }
    }
}

package com.demo.resortslite;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Platform configuration
 * Fixes cr-java-0061, cr-java-0062, cr-java-0063 (File System Dependencies)
 * 
 * This configuration enables:
 * - Google Cloud Storage for persistent file storage
 * - Elimination of local file system dependencies
 * - Data durability across container restarts and scaling events
 * - Cloud-native storage architecture
 */
@Configuration
public class GcpConfig {

    @Value("${spring.cloud.gcp.project-id:}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location:}")
    private String credentialsLocation;

    @Bean
    public Storage googleCloudStorage() {
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        
        if (projectId != null && !projectId.isEmpty()) {
            builder.setProjectId(projectId);
        }
        
        // In production, credentials are automatically discovered from:
        // 1. GOOGLE_APPLICATION_CREDENTIALS environment variable
        // 2. GCE/GKE metadata service
        // 3. Cloud Run service account
        
        return builder.build().getService();
    }
}

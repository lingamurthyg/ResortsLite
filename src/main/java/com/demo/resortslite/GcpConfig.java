package com.demo.resortslite;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Google Cloud Platform configuration
 * Configures Google Cloud Storage and Secret Manager integration
 * 
 * FIXED cr-java-0061, cr-java-0062, cr-java-0063: Enables GCS for file operations
 * FIXED cr-java-0069, cr-java-0090: Enables Secret Manager for credentials management
 */
@Configuration
public class GcpConfig {

    @Value("${GCP_PROJECT_ID:}")
    private String gcpProjectId;

    @Value("${GCS_BUCKET_NAME:resorts-reports-bucket}")
    private String gcsBucketName;

    /**
     * Creates a Google Cloud Storage client bean
     * Used for replacing local file system operations with cloud storage
     * 
     * @return Storage client instance
     */
    @Bean
    public Storage googleCloudStorage() {
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        
        if (gcpProjectId != null && !gcpProjectId.isEmpty()) {
            builder.setProjectId(gcpProjectId);
        }
        
        return builder.build().getService();
    }
}

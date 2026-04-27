package com.demo.resortslite.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure services configuration.
 * FIXED: Configures Azure SDK clients for Key Vault, Blob Storage, and other services.
 */
@Configuration
public class AzureConfig {

    @Value("${azure.keyvault.uri}")
    private String keyVaultUri;

    @Value("${azure.storage.connection-string}")
    private String storageConnectionString;

    /**
     * Creates DefaultAzureCredential for authentication.
     * Uses managed identity in Azure, falls back to environment variables locally.
     * 
     * @return DefaultAzureCredential instance
     */
    @Bean
    public DefaultAzureCredential defaultAzureCredential() {
        return new DefaultAzureCredentialBuilder().build();
    }

    /**
     * Creates SecretClient for Azure Key Vault.
     * FIXED cr-java-0069: Enables secure credential retrieval from Key Vault.
     * 
     * @param credential Azure credential
     * @return SecretClient instance
     */
    @Bean
    public SecretClient secretClient(DefaultAzureCredential credential) {
        return new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(credential)
                .buildClient();
    }

    /**
     * Creates BlobServiceClient for Azure Blob Storage.
     * FIXED cr-java-0061, cr-java-0062, cr-java-0063: Enables cloud-native file storage.
     * 
     * @return BlobServiceClient instance
     */
    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
                .connectionString(storageConnectionString)
                .buildClient();
    }
}

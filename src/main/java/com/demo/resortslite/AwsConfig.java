package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * AWS SDK configuration for cloud-native services
 * FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - S3 client for file storage
 * FIXED: cr-java-0069, cr-java-0090 - Secrets Manager client for credential management
 * FIXED: cr-java-0071 - Systems Manager client for configuration management
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * Creates S3 client for cloud-native file storage
     * FIXED: cr-java-0061, cr-java-0062, cr-java-0063 - Replaces local file system operations
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates Secrets Manager client for secure credential storage
     * FIXED: cr-java-0069 - Replaces hard-coded database credentials
     * FIXED: cr-java-0090 - Replaces file-based authentication
     */
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates Systems Manager client for parameter store access
     * FIXED: cr-java-0071 - Replaces hard-coded environment URLs
     */
    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

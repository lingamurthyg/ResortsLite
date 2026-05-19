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
 * AWS SDK configuration for cloud-native services.
 * Configures AWS clients for S3, Secrets Manager, and Systems Manager Parameter Store.
 * 
 * Uses DefaultCredentialsProvider which supports:
 * - IAM roles for EC2 instances
 * - IAM roles for ECS tasks
 * - IAM roles for EKS pods (IRSA)
 * - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * - AWS credentials file (~/.aws/credentials)
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Creates S3 client for object storage operations.
     * Used for storing reports and files in Amazon S3.
     * 
     * @return S3Client configured for the specified region
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates Secrets Manager client for retrieving database credentials.
     * Used for secure credential management without hard-coding.
     * 
     * @return SecretsManagerClient configured for the specified region
     */
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates Systems Manager client for retrieving configuration parameters.
     * Used for externalized configuration management.
     * 
     * @return SsmClient configured for the specified region
     */
    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

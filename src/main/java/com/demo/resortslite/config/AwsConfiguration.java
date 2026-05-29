package com.demo.resortslite.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * AWS Configuration for cloud-native services
 * Configures AWS SDK clients for S3, Secrets Manager, and Systems Manager Parameter Store
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 86400) // 24 hours
public class AwsConfiguration {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * FIXED cr-java-0061, cr-java-0062, cr-java-0063: S3 Client for file storage
     * Replaces local file system operations with Amazon S3 for durable, scalable storage
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * FIXED cr-java-0069, cr-java-0090: Secrets Manager Client for credential management
     * Enables centralized, encrypted secret storage with automatic rotation support
     */
    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * FIXED cr-java-0071: Systems Manager Client for Parameter Store
     * Enables externalized configuration management for environment-specific values
     */
    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * FIXED cr-java-0065, cr-java-0067: Redis Template for distributed session and cache
     * Configures RedisTemplate with proper serialization for Amazon ElastiCache
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

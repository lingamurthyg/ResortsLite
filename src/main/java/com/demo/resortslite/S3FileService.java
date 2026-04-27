package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * S3 File Service for containerized file storage operations.
 * Replaces local file system operations with Amazon S3 object storage.
 */
@Service
public class S3FileService {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Upload file content to S3
     * @param key S3 object key (file path)
     * @param content File content as string
     */
    public void uploadFile(String key, String content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(content));
    }

    /**
     * Download file content from S3
     * @param key S3 object key (file path)
     * @return File content as string
     */
    public String downloadFile(String key) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
            return new String(inputStream.readAllBytes());
        }
    }

    /**
     * Generate S3 object key from file path
     * @param filePath Original file path
     * @return S3 object key
     */
    public String generateS3Key(String filePath) {
        // Remove leading slashes and backslashes, replace path separators
        return filePath.replaceAll("^[/\\\\]+", "")
                .replace("\\", "/")
                .replace(":", "_");
    }

    /**
     * Get S3 URI for a given key
     * @param key S3 object key
     * @return S3 URI
     */
    public String getS3Uri(String key) {
        return String.format("s3://%s/%s", bucketName, key);
    }
}

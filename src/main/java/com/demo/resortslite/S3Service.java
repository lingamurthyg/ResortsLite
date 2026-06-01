package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service for managing file operations with Amazon S3.
 * Replaces local file system operations for container portability.
 */
@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    /**
     * Upload a file to S3
     * @param key The S3 object key (file path)
     * @param content The file content as bytes
     * @return The S3 URI of the uploaded file
     */
    public String uploadFile(String key, byte[] content) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
        return String.format("s3://%s/%s", bucketName, key);
    }

    /**
     * Upload a file to S3 from an input stream
     * @param key The S3 object key (file path)
     * @param inputStream The file content as input stream
     * @param contentLength The content length
     * @return The S3 URI of the uploaded file
     */
    public String uploadFile(String key, InputStream inputStream, long contentLength) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return String.format("s3://%s/%s", bucketName, key);
    }

    /**
     * Download a file from S3
     * @param key The S3 object key (file path)
     * @return InputStream of the file content
     */
    public InputStream downloadFile(String key) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    /**
     * Get the S3 URI for a given key
     * @param key The S3 object key
     * @return The full S3 URI
     */
    public String getS3Uri(String key) {
        return String.format("s3://%s/%s", bucketName, key);
    }

    /**
     * Get the bucket name
     * @return The configured S3 bucket name
     */
    public String getBucketName() {
        return bucketName;
    }
}

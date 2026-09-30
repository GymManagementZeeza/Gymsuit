package com.zeezaglobal.gymmanagement.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3-compatible object storage client.
 *
 * <p>Points at MinIO in docker-compose by default ({@code app.storage.endpoint}), but any
 * S3-compatible endpoint works — swap the env vars and the same code talks to a standalone
 * MinIO box with zero changes, since we only use the standard S3 API.
 */
@Configuration
public class S3StorageConfig {

    @Value("${app.storage.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${app.storage.region:us-east-1}")
    private String region;

    @Value("${app.storage.access-key:gymsuit}")
    private String accessKey;

    @Value("${app.storage.secret-key:gymsuit-minio-secret}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                // MinIO needs path-style URLs (bucket in the path, not the hostname).
                .forcePathStyle(true)
                .build();
    }
}

package com.zeezaglobal.gymmanagement.service;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * File uploads backed by S3-compatible storage (MinIO in docker-compose).
 *
 * <p>Two key spaces:
 * <ul>
 *   <li>{@code public/...} — world-readable (gym logos, etc.); served straight off MinIO,
 *       so the API never proxies bytes and every backend replica serves the same files</li>
 *   <li>anything else — private; fetch via a presigned URL when private files are needed</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${app.storage.bucket:gymsuit-uploads}")
    private String bucket;

    @Value("${app.storage.public-url:http://localhost:9000}")
    private String publicUrl;

    /**
     * Uploads bytes under {@code public/<key>} and returns the public URL.
     * Callers own the key layout, e.g. {@code gyms/42/logo-<uuid>.png}.
     */
    public String uploadPublic(String key, InputStream data, long size, String contentType) {
        String fullKey = "public/" + key;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fullKey)
                .contentType(contentType)
                .contentLength(size)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(data, size));
        log.info("Uploaded {} ({} bytes) to bucket {}", fullKey, size, bucket);
        return publicUrl + "/" + bucket + "/" + fullKey;
    }

    /** Deletes a previously uploaded key (pass the key without the {@code public/} prefix). */
    public void deletePublic(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key("public/" + key)
                .build();
        s3Client.deleteObject(request);
    }
}

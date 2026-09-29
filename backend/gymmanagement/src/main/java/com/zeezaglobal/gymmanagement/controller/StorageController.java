package com.zeezaglobal.gymmanagement.controller;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.zeezaglobal.gymmanagement.dto.GymResponse;
import com.zeezaglobal.gymmanagement.service.GymService;
import com.zeezaglobal.gymmanagement.service.StorageService;

import lombok.RequiredArgsConstructor;

/**
 * File uploads. Files live in MinIO (S3-compatible), so every backend replica
 * serves the same uploads and redeploys never lose them.
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/svg+xml");

    private final StorageService storageService;
    private final GymService gymService;

    @Value("${app.storage.max-upload-mb:5}")
    private long maxUploadMb;

    /**
     * Uploads a gym logo and sets it as the gym's logo in one call.
     * Returns the gym with the new logo URL.
     */
    @PostMapping("/gyms/{gymId}/logo")
    @PreAuthorize("@security.canManageGym(#gymId)")
    public GymResponse uploadGymLogo(@PathVariable Long gymId,
            @RequestParam("file") MultipartFile file) {
        validateImage(file);
        String extension = extensionOf(file.getOriginalFilename(), file.getContentType());
        String key = "gyms/" + gymId + "/logo-" + UUID.randomUUID() + extension;
        try {
            String url = storageService.uploadPublic(key, file.getInputStream(),
                    file.getSize(), file.getContentType());
            return gymService.updateLogoUrl(gymId, url);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file was uploaded");
        }
        if (!ALLOWED_LOGO_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PNG, JPEG, WebP or SVG images are allowed");
        }
        if (file.getSize() > maxUploadMb * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File must be under " + maxUploadMb + " MB");
        }
    }

    private String extensionOf(String filename, String contentType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0 && dot < filename.length() - 1) {
                String ext = filename.substring(dot).toLowerCase();
                if (ext.matches("\\.[a-z0-9]{2,5}")) {
                    return ext;
                }
            }
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> ".jpg";
        };
    }
}

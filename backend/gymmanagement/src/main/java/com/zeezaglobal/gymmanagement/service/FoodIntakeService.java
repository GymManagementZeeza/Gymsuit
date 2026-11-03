package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.FoodIntakeSyncRequest;
import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.FoodIntakeRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Mobile food-log upload. The apps keep food_log.json on-device today; this
 * endpoint lets them push entries to the database (upsert by the app's entry
 * id) so the trainer progress page can read calorie intake from the same
 * place as steps, heart rate and workouts. Until an app version uploads,
 * intake simply stays empty — it is never guessed.
 */
@Service
@RequiredArgsConstructor
public class FoodIntakeService {

    private final FoodIntakeRepository foodIntakeRepository;
    private final UserRepository userRepository;

    private static final int MAX_PHOTO_BYTES = 1_048_576;

    /**
     * Recompresses a photo to under 1 MB without ever dropping it. Photos
     * already under the limit pass through untouched. Oversized ones first
     * lose JPEG quality (0.85 → 0.35), then lose dimensions (×0.75 per
     * step) until they fit — a 100px image is far under the limit, so the
     * loop always terminates with a usable photo. An undecodable payload
     * is returned as-is rather than dropped.
     */
    private byte[] compressPhotoToLimit(byte[] original) {
        if (original.length <= MAX_PHOTO_BYTES) {
            return original;
        }
        try {
            java.awt.image.BufferedImage image =
                    javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original));
            if (image == null) {
                return original;
            }
            // JPEG has no alpha channel; flatten transparent images onto white.
            if (image.getColorModel().hasAlpha()) {
                java.awt.image.BufferedImage rgb = new java.awt.image.BufferedImage(
                        image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = rgb.createGraphics();
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = rgb;
            }
            java.awt.image.BufferedImage current = image;
            float quality = 0.85f;
            byte[] encoded = null;
            for (int attempt = 0; attempt < 24; attempt++) {
                encoded = encodeJpeg(current, quality);
                if (encoded != null && encoded.length <= MAX_PHOTO_BYTES) {
                    return encoded;
                }
                if (quality > 0.35f) {
                    quality -= 0.15f;
                } else {
                    int width = (int) (current.getWidth() * 0.75);
                    int height = (int) (current.getHeight() * 0.75);
                    if (width < 100 || height < 100) {
                        break;
                    }
                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(
                            width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = scaled.createGraphics();
                    g.drawImage(current, 0, 0, width, height, null);
                    g.dispose();
                    current = scaled;
                    quality = 0.75f;
                }
            }
            // Pathological fallback (unreachable in practice: a sub-100px
            // JPEG is far under 1 MB): return the smallest encoding produced.
            return encoded != null ? encoded : original;
        } catch (Exception ignored) {
            return original;
        }
    }

    private byte[] encodeJpeg(java.awt.image.BufferedImage image, float quality) {
        try {
            java.util.Iterator<javax.imageio.ImageWriter> writers =
                    javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                return null;
            }
            javax.imageio.ImageWriter writer = writers.next();
            try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                 javax.imageio.stream.ImageOutputStream ios =
                         javax.imageio.ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                javax.imageio.ImageWriteParam params = writer.getDefaultWriteParam();
                if (params.canWriteCompressed()) {
                    params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    params.setCompressionQuality(quality);
                }
                writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
                return out.toByteArray();
            } finally {
                writer.dispose();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Removes one entry the app deleted locally; a no-op when it was never synced. */
    @Transactional
    public void deleteByExternalId(String externalId, UserPrincipal principal) {
        foodIntakeRepository.findByUserIdAndExternalId(principal.getUserId(), externalId)
                .ifPresent(foodIntakeRepository::delete);
    }

    @Transactional
    public int sync(FoodIntakeSyncRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int count = 0;
        if (request.entries() == null) {
            return count;
        }
        for (FoodIntakeSyncRequest.Entry entry : request.entries()) {
            FoodIntakeRecord record = foodIntakeRepository
                    .findByUserIdAndExternalId(user.getId(), entry.externalId())
                    .orElseGet(() -> {
                        FoodIntakeRecord created = new FoodIntakeRecord();
                        created.setUser(user);
                        created.setExternalId(entry.externalId());
                        return created;
                    });
            record.setRecordDate(entry.recordDate());
            record.setMeal(entry.meal());
            record.setDishName(entry.dishName());
            record.setCalories(entry.calories());
            record.setProteinG(entry.proteinG());
            record.setCarbsG(entry.carbsG());
            record.setFatG(entry.fatG());
            if (entry.photoBase64() != null && !entry.photoBase64().isBlank()) {
                try {
                    byte[] photo = java.util.Base64.getDecoder().decode(entry.photoBase64());
                    // Never drop a photo for being too large: oversized
                    // photos are recompressed server-side to under 1 MB.
                    record.setPhotoData(compressPhotoToLimit(photo));
                    record.setPhotoContentType("image/jpeg");
                } catch (IllegalArgumentException ignored) {
                    // Malformed base64: keep any photo already stored.
                }
            }
            record.setUpdatedAt(LocalDateTime.now());
            foodIntakeRepository.save(record);
            count++;
        }
        return count;
    }
}

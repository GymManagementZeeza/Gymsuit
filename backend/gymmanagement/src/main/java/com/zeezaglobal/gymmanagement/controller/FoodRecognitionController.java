package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.FoodRecognitionResponse;
import com.zeezaglobal.gymmanagement.service.FoodRecognitionService;
import com.zeezaglobal.gymmanagement.service.FoodRecognitionService.FoodRecognitionUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Identify a dish from a photo. Authenticated like the other /api/mobile/**
 * endpoints. Forwards the upload to the Python inference service
 * (FoodImageTraining/inference_server.py).
 */
@RestController
@RequestMapping("/api/mobile/food")
@RequiredArgsConstructor
public class FoodRecognitionController {

    private final FoodRecognitionService foodRecognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<?> recognize(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No image uploaded. Send a photo as multipart field 'image'."));
        }
        if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Uploaded file is not an image."));
        }
        try {
            FoodRecognitionResponse result = foodRecognitionService.recognize(image);
            return ResponseEntity.ok(result);
        } catch (FoodRecognitionUnavailableException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not read uploaded image."));
        }
    }
}

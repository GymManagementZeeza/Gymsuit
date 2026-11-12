package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.ClientProgressResponse;
import com.zeezaglobal.gymmanagement.service.ClientProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Trainer / nutritionist view of one client's synced progress (read-only).
 * A trainer may only open their own assigned clients; owners, managers and
 * nutritionists may open any member of their gym.
 */
@RestController
@RequestMapping("/api/gyms/{gymId}/members/{memberId}/progress")
@RequiredArgsConstructor
public class ClientProgressController {

    private final ClientProgressService clientProgressService;

    @GetMapping
    @PreAuthorize("@security.canReadMemberNutrition(#gymId, #memberId)")
    public ClientProgressResponse getProgress(
            @PathVariable Long gymId,
            @PathVariable Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return clientProgressService.getProgress(gymId, memberId, from, to);
    }

    /** Serves one meal photo a client uploaded. Same guard as the progress view. */
    @GetMapping("/photo/{externalId}")
    @PreAuthorize("@security.canReadMemberNutrition(#gymId, #memberId)")
    public ResponseEntity<byte[]> getIntakePhoto(
            @PathVariable Long gymId,
            @PathVariable Long memberId,
            @PathVariable String externalId) {
        return clientProgressService.getIntakePhoto(gymId, memberId, externalId)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }
}

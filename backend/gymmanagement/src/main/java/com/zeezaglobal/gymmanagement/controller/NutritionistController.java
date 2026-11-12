package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.NutritionistClientResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteAcceptRequest;
import com.zeezaglobal.gymmanagement.dto.NutritionistInvitePreviewResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteRequest;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteResponse;
import com.zeezaglobal.gymmanagement.service.NutritionistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Standalone nutritionist workspace. /api/nutritionist/** requires a NUTRITIONIST token (checked in the service);
 * /api/nutritionist-invites/** is public — the token in the emailed link is the credential.
 * Client progress is read through the existing /api/gyms/{gymId}/members/{memberId}/progress endpoints,
 * which allow a nutritionist only for linked clients.
 */
@RestController
@RequiredArgsConstructor
public class NutritionistController {

    private final NutritionistService nutritionistService;

    @GetMapping("/api/nutritionist/clients")
    public List<NutritionistClientResponse> clients() {
        return nutritionistService.listClients();
    }

    @DeleteMapping("/api/nutritionist/clients/{memberId}")
    public ResponseEntity<Void> removeClient(@PathVariable Long memberId) {
        nutritionistService.removeClient(memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/nutritionist/invites")
    public List<NutritionistInviteResponse> invites() {
        return nutritionistService.listInvites();
    }

    @PostMapping("/api/nutritionist/invites")
    public ResponseEntity<NutritionistInviteResponse> invite(@Valid @RequestBody NutritionistInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nutritionistService.invite(request));
    }

    @DeleteMapping("/api/nutritionist/invites/{inviteId}")
    public ResponseEntity<Void> revoke(@PathVariable Long inviteId) {
        nutritionistService.revokeInvite(inviteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/nutritionist-invites/{token}")
    public NutritionistInvitePreviewResponse preview(@PathVariable String token) {
        return nutritionistService.preview(token);
    }

    @PostMapping("/api/nutritionist-invites/{token}/accept")
    public Map<String, String> accept(
            @PathVariable String token, @RequestBody(required = false) NutritionistInviteAcceptRequest signup) {
        return Map.of("nutritionistName", nutritionistService.accept(token, signup));
    }
}

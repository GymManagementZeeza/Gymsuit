package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.TrainerInviteAcceptRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerInvitePreviewResponse;
import com.zeezaglobal.gymmanagement.dto.TrainerInviteRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerInviteResponse;
import com.zeezaglobal.gymmanagement.service.TrainerInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Trainer client invitations. The trainer-scoped endpoints are authenticated
 * (a trainer invites for themselves, gym staff for any trainer in the gym).
 * The preview/accept endpoints are public: the token in the emailed link is
 * the credential, so an invitee needs no account or directory access to accept.
 */
@RestController
@RequiredArgsConstructor
public class TrainerInviteController {

    private final TrainerInviteService trainerInviteService;

    @PostMapping("/api/gyms/{gymId}/trainers/{trainerId}/invites")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public ResponseEntity<TrainerInviteResponse> invite(
            @PathVariable Long gymId,
            @PathVariable Long trainerId,
            @Valid @RequestBody TrainerInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainerInviteService.invite(gymId, trainerId, request));
    }

    @GetMapping("/api/gyms/{gymId}/trainers/{trainerId}/invites")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public List<TrainerInviteResponse> listInvites(
            @PathVariable Long gymId,
            @PathVariable Long trainerId) {
        return trainerInviteService.listInvites(gymId, trainerId);
    }

    @DeleteMapping("/api/gyms/{gymId}/trainers/{trainerId}/invites/{inviteId}")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public ResponseEntity<Void> revoke(
            @PathVariable Long gymId,
            @PathVariable Long trainerId,
            @PathVariable Long inviteId) {
        trainerInviteService.revoke(gymId, trainerId, inviteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/trainer-invites/{token}")
    public TrainerInvitePreviewResponse preview(@PathVariable String token) {
        return trainerInviteService.preview(token);
    }

    @PostMapping("/api/trainer-invites/{token}/accept")
    public Map<String, String> accept(
            @PathVariable String token,
            @RequestBody(required = false) TrainerInviteAcceptRequest request) {
        String trainerName = trainerInviteService.accept(token, request);
        return Map.of("message", "You are now a client of " + trainerName);
    }
}

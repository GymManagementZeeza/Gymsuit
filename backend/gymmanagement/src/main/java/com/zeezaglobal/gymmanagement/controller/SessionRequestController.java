package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.SessionRequestCreateRequest;
import com.zeezaglobal.gymmanagement.dto.SessionRequestRejectRequest;
import com.zeezaglobal.gymmanagement.dto.SessionRequestResponse;
import com.zeezaglobal.gymmanagement.service.SessionRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}/session-requests")
@RequiredArgsConstructor
public class SessionRequestController {

    private final SessionRequestService sessionRequestService;

    @GetMapping
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public List<SessionRequestResponse> getVisible(@PathVariable Long gymId) {
        return sessionRequestService.findVisible(gymId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.canAccessSessionRequest(#gymId, #id)")
    public SessionRequestResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return sessionRequestService.findById(gymId, id);
    }

    @PostMapping
    @PreAuthorize("@security.canCreateSessionRequest(#gymId)")
    public ResponseEntity<SessionRequestResponse> create(
            @PathVariable Long gymId, @Valid @RequestBody SessionRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionRequestService.create(gymId, request));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("@security.canRespondToSessionRequest(#gymId, #id)")
    public SessionRequestResponse approve(@PathVariable Long gymId, @PathVariable Long id) {
        return sessionRequestService.approve(gymId, id);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("@security.canRespondToSessionRequest(#gymId, #id)")
    public SessionRequestResponse reject(
            @PathVariable Long gymId, @PathVariable Long id, @RequestBody(required = false) SessionRequestRejectRequest request) {
        return sessionRequestService.reject(gymId, id, request);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("@security.canCancelSessionRequest(#gymId, #id)")
    public SessionRequestResponse cancel(@PathVariable Long gymId, @PathVariable Long id) {
        return sessionRequestService.cancel(gymId, id);
    }
}

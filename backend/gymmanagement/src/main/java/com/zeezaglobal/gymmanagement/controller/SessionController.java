package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.SessionEnrollmentResponse;
import com.zeezaglobal.gymmanagement.dto.SessionRequest;
import com.zeezaglobal.gymmanagement.dto.SessionResponse;
import com.zeezaglobal.gymmanagement.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public List<SessionResponse> getAll(@PathVariable Long gymId) {
        return sessionService.findAllByGym(gymId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public SessionResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return sessionService.findById(gymId, id);
    }

    @PostMapping
    @PreAuthorize("@security.canCreateSessionFor(#gymId, #request.trainerId())")
    public ResponseEntity<SessionResponse> create(@PathVariable Long gymId, @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(gymId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.canManageSession(#gymId, #id)")
    public SessionResponse update(@PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody SessionRequest request) {
        return sessionService.update(gymId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@security.canManageSession(#gymId, #id)")
    public ResponseEntity<Void> delete(@PathVariable Long gymId, @PathVariable Long id) {
        sessionService.delete(gymId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/members")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public List<SessionEnrollmentResponse> getAssignedMembers(@PathVariable Long gymId, @PathVariable Long sessionId) {
        return sessionService.listAssignedMembers(gymId, sessionId);
    }

    @PostMapping("/{sessionId}/members/{memberId}")
    @PreAuthorize("@security.canManageSession(#gymId, #sessionId)")
    public ResponseEntity<SessionEnrollmentResponse> assignMember(
            @PathVariable Long gymId, @PathVariable Long sessionId, @PathVariable Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.assignMember(gymId, sessionId, memberId));
    }

    @DeleteMapping("/{sessionId}/members/{memberId}")
    @PreAuthorize("@security.canManageSession(#gymId, #sessionId)")
    public ResponseEntity<Void> unassignMember(
            @PathVariable Long gymId, @PathVariable Long sessionId, @PathVariable Long memberId) {
        sessionService.unassignMember(gymId, sessionId, memberId);
        return ResponseEntity.noContent().build();
    }
}

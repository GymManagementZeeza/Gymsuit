package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.InviteManagerRequest;
import com.zeezaglobal.gymmanagement.dto.ManagerAccessUpdateRequest;
import com.zeezaglobal.gymmanagement.dto.TeamManagerResponse;
import com.zeezaglobal.gymmanagement.service.TeamService;
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

/** Team-access roster: the owner (or a manager granted the SETTINGS scope) invites and scopes managers by email. */
@RestController
@RequestMapping("/api/gyms/{gymId}/team/managers")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @PreAuthorize("@security.canManageGym(#gymId)")
    public List<TeamManagerResponse> list(@PathVariable Long gymId) {
        return teamService.listManagers(gymId);
    }

    @PostMapping
    @PreAuthorize("@security.canManageGym(#gymId)")
    public ResponseEntity<TeamManagerResponse> invite(
            @PathVariable Long gymId, @Valid @RequestBody InviteManagerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.inviteManager(gymId, request));
    }

    @PutMapping("/{managerId}/access")
    @PreAuthorize("@security.canManageGym(#gymId)")
    public TeamManagerResponse updateAccess(
            @PathVariable Long gymId, @PathVariable Long managerId, @RequestBody ManagerAccessUpdateRequest request) {
        return teamService.updateAccess(gymId, managerId, request.scopes());
    }

    @DeleteMapping("/{managerId}")
    @PreAuthorize("@security.canManageGym(#gymId)")
    public ResponseEntity<Void> remove(@PathVariable Long gymId, @PathVariable Long managerId) {
        teamService.removeManager(gymId, managerId);
        return ResponseEntity.noContent().build();
    }
}

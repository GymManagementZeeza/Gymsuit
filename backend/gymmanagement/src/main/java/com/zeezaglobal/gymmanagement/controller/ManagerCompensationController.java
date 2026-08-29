package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.CompensationRequest;
import com.zeezaglobal.gymmanagement.dto.CompensationResponse;
import com.zeezaglobal.gymmanagement.service.ManagerCompensationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gyms/{gymId}/managers/{managerId}/compensation")
@RequiredArgsConstructor
public class ManagerCompensationController {

    private final ManagerCompensationService compensationService;

    @GetMapping
    @PreAuthorize("@security.canViewManagerCompensation(#gymId, #managerId)")
    public CompensationResponse get(@PathVariable Long gymId, @PathVariable Long managerId) {
        return compensationService.get(gymId, managerId);
    }

    @PutMapping
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public CompensationResponse upsert(
            @PathVariable Long gymId, @PathVariable Long managerId, @Valid @RequestBody CompensationRequest request) {
        return compensationService.upsert(gymId, managerId, request);
    }
}

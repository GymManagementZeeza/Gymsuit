package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.CompensationRequest;
import com.zeezaglobal.gymmanagement.dto.CompensationResponse;
import com.zeezaglobal.gymmanagement.service.TrainerCompensationService;
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
@RequestMapping("/api/gyms/{gymId}/trainers/{trainerId}/compensation")
@RequiredArgsConstructor
public class TrainerCompensationController {

    private final TrainerCompensationService compensationService;

    @GetMapping
    @PreAuthorize("@security.canViewTrainerCompensation(#gymId, #trainerId)")
    public CompensationResponse get(@PathVariable Long gymId, @PathVariable Long trainerId) {
        return compensationService.get(gymId, trainerId);
    }

    @PutMapping
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public CompensationResponse upsert(
            @PathVariable Long gymId, @PathVariable Long trainerId, @Valid @RequestBody CompensationRequest request) {
        return compensationService.upsert(gymId, trainerId, request);
    }
}

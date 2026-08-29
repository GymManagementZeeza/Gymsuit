package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MembershipPlanRequest;
import com.zeezaglobal.gymmanagement.dto.MembershipPlanResponse;
import com.zeezaglobal.gymmanagement.service.MembershipPlanService;
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
@RequestMapping("/api/gyms/{gymId}/membership-plans")
@RequiredArgsConstructor
public class MembershipPlanController {

    private final MembershipPlanService membershipPlanService;

    @GetMapping
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public List<MembershipPlanResponse> getAll(@PathVariable Long gymId) {
        return membershipPlanService.findAllByGym(gymId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public MembershipPlanResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return membershipPlanService.findById(gymId, id);
    }

    @PostMapping
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public ResponseEntity<MembershipPlanResponse> create(
            @PathVariable Long gymId, @Valid @RequestBody MembershipPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membershipPlanService.create(gymId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public MembershipPlanResponse update(
            @PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody MembershipPlanRequest request) {
        return membershipPlanService.update(gymId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public ResponseEntity<Void> delete(@PathVariable Long gymId, @PathVariable Long id) {
        membershipPlanService.delete(gymId, id);
        return ResponseEntity.noContent().build();
    }
}

package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MemberNotifyRequest;
import com.zeezaglobal.gymmanagement.dto.MemberRequest;
import com.zeezaglobal.gymmanagement.dto.MemberResponse;
import com.zeezaglobal.gymmanagement.dto.MemberGoalRequest;
import com.zeezaglobal.gymmanagement.dto.MemberTrainerRequest;
import com.zeezaglobal.gymmanagement.dto.MemberWeightRequest;
import com.zeezaglobal.gymmanagement.dto.WeightLogResponse;
import com.zeezaglobal.gymmanagement.service.MemberService;
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
@RequestMapping("/api/gyms/{gymId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("@security.canViewMemberDirectory(#gymId)")
    public List<MemberResponse> getAll(@PathVariable Long gymId) {
        return memberService.findAllByGym(gymId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.canViewMember(#gymId, #id)")
    public MemberResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return memberService.findById(gymId, id);
    }

    @PostMapping
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<MemberResponse> create(@PathVariable Long gymId, @Valid @RequestBody MemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(gymId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public MemberResponse update(@PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return memberService.update(gymId, id, request);
    }

    @PutMapping("/{id}/weight")
    @PreAuthorize("@security.canViewMember(#gymId, #id)")
    public MemberResponse updateWeight(@PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody MemberWeightRequest request) {
        return memberService.updateWeight(gymId, id, request);
    }

    @PutMapping("/{id}/goal")
    @PreAuthorize("@security.canViewMember(#gymId, #id)")
    public MemberResponse updateGoal(@PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody MemberGoalRequest request) {
        return memberService.updateGoal(gymId, id, request);
    }

    @PutMapping("/{id}/trainer")
    @PreAuthorize("@security.canViewMember(#gymId, #id)")
    public MemberResponse assignTrainer(@PathVariable Long gymId, @PathVariable Long id, @RequestBody MemberTrainerRequest request) {
        return memberService.assignTrainer(gymId, id, request);
    }

    @GetMapping("/{id}/weight-history")
    @PreAuthorize("@security.canViewMember(#gymId, #id)")
    public List<WeightLogResponse> getWeightHistory(@PathVariable Long gymId, @PathVariable Long id) {
        return memberService.findWeightHistory(gymId, id);
    }

    @PostMapping("/{id}/notify")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<Void> notify(
            @PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody MemberNotifyRequest request) {
        memberService.notify(gymId, id, request);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<Void> delete(@PathVariable Long gymId, @PathVariable Long id) {
        memberService.delete(gymId, id);
        return ResponseEntity.noContent().build();
    }
}

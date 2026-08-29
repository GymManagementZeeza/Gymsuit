package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.CashPaymentRequest;
import com.zeezaglobal.gymmanagement.dto.ManualPaymentRequest;
import com.zeezaglobal.gymmanagement.dto.MemberPaymentResponse;
import com.zeezaglobal.gymmanagement.dto.OnlinePaymentRequest;
import com.zeezaglobal.gymmanagement.dto.TakePlanPaymentRequest;
import com.zeezaglobal.gymmanagement.service.MemberPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms/{gymId}")
@RequiredArgsConstructor
public class MemberPaymentController {

    private final MemberPaymentService memberPaymentService;

    @PostMapping("/members/{memberId}/payments/cash")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<MemberPaymentResponse> recordCash(
            @PathVariable Long gymId, @PathVariable Long memberId, @Valid @RequestBody CashPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberPaymentService.recordCashPayment(gymId, memberId, request));
    }

    @PostMapping("/members/{memberId}/payments/manual")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<MemberPaymentResponse> recordManual(
            @PathVariable Long gymId, @PathVariable Long memberId, @Valid @RequestBody ManualPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberPaymentService.recordManualPayment(gymId, memberId, request));
    }

    @PostMapping("/members/{memberId}/payments/plan")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<MemberPaymentResponse> takePlanPayment(
            @PathVariable Long gymId, @PathVariable Long memberId, @Valid @RequestBody TakePlanPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberPaymentService.recordPlanPayment(gymId, memberId, request));
    }

    @PostMapping("/members/{memberId}/payments/online")
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public ResponseEntity<MemberPaymentResponse> initiateOnline(
            @PathVariable Long gymId, @PathVariable Long memberId, @Valid @RequestBody OnlinePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberPaymentService.initiateOnlinePayment(gymId, memberId, request));
    }

    @GetMapping("/members/{memberId}/payments")
    @PreAuthorize("@security.canManageMemberBilling(#gymId, #memberId)")
    public List<MemberPaymentResponse> memberHistory(@PathVariable Long gymId, @PathVariable Long memberId) {
        return memberPaymentService.history(gymId, memberId);
    }

    @GetMapping("/member-payments")
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public List<MemberPaymentResponse> gymWideList(@PathVariable Long gymId) {
        return memberPaymentService.findVisible(gymId);
    }

    @GetMapping("/member-payments/{id}")
    @PreAuthorize("@security.canAccessMemberPayment(#gymId, #id)")
    public MemberPaymentResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return memberPaymentService.findById(gymId, id);
    }
}

package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.PaymentRequest;
import com.zeezaglobal.gymmanagement.dto.PaymentResponse;
import com.zeezaglobal.gymmanagement.dto.PaymentStatusUpdateRequest;
import com.zeezaglobal.gymmanagement.service.PaymentService;
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
@RequestMapping("/api/gyms/{gymId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("@security.canListPayments(#gymId)")
    public List<PaymentResponse> getVisible(@PathVariable Long gymId) {
        return paymentService.findVisible(gymId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.canAccessPayment(#gymId, #id)")
    public PaymentResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return paymentService.findById(gymId, id);
    }

    @PostMapping
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public ResponseEntity<PaymentResponse> create(@PathVariable Long gymId, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(gymId, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public PaymentResponse updateStatus(
            @PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody PaymentStatusUpdateRequest request) {
        return paymentService.updateStatus(gymId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@security.canManageFinance(#gymId)")
    public ResponseEntity<Void> delete(@PathVariable Long gymId, @PathVariable Long id) {
        paymentService.delete(gymId, id);
        return ResponseEntity.noContent().build();
    }
}

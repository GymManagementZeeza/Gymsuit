package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.TransactionRequest;
import com.zeezaglobal.gymmanagement.dto.TransactionResponse;
import com.zeezaglobal.gymmanagement.service.GymTransactionService;
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
@RequestMapping("/api/gyms/{gymId}/transactions")
@RequiredArgsConstructor
public class GymTransactionController {

    private final GymTransactionService transactionService;

    @GetMapping
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public List<TransactionResponse> list(@PathVariable Long gymId) {
        return transactionService.listForGym(gymId);
    }

    @PostMapping
    @PreAuthorize("@security.canManageStaff(#gymId)")
    public ResponseEntity<TransactionResponse> create(
            @PathVariable Long gymId, @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(gymId, request));
    }
}

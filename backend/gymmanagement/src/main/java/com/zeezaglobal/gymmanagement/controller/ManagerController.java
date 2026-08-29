package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.ManagerRequest;
import com.zeezaglobal.gymmanagement.dto.ManagerResponse;
import com.zeezaglobal.gymmanagement.service.ManagerService;
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
@RequestMapping("/api/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ManagerResponse> getAll() {
        return managerService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ManagerResponse getById(@PathVariable Long id) {
        return managerService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManagerResponse> create(@Valid @RequestBody ManagerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ManagerResponse update(@PathVariable Long id, @Valid @RequestBody ManagerRequest request) {
        return managerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

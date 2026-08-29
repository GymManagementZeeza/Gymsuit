package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.TrainerRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerResponse;
import com.zeezaglobal.gymmanagement.service.TrainerService;
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
@RequestMapping("/api/gyms/{gymId}/trainers")
@RequiredArgsConstructor
public class TrainerController {

    private final TrainerService trainerService;

    @GetMapping
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public List<TrainerResponse> getAll(@PathVariable Long gymId) {
        return trainerService.findAllByGym(gymId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.hasGymAccess(#gymId)")
    public TrainerResponse getById(@PathVariable Long gymId, @PathVariable Long id) {
        return trainerService.findById(gymId, id);
    }

    @PostMapping
    @PreAuthorize("@security.canManageTrainers(#gymId)")
    public ResponseEntity<TrainerResponse> create(@PathVariable Long gymId, @Valid @RequestBody TrainerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainerService.create(gymId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.canManageTrainer(#gymId, #id)")
    public TrainerResponse update(@PathVariable Long gymId, @PathVariable Long id, @Valid @RequestBody TrainerRequest request) {
        return trainerService.update(gymId, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@security.canManageTrainers(#gymId)")
    public ResponseEntity<Void> delete(@PathVariable Long gymId, @PathVariable Long id) {
        trainerService.delete(gymId, id);
        return ResponseEntity.noContent().build();
    }
}

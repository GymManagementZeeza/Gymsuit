package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.GymRequest;
import com.zeezaglobal.gymmanagement.dto.GymResponse;
import com.zeezaglobal.gymmanagement.dto.GymSearchResult;
import com.zeezaglobal.gymmanagement.service.GymService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gyms")
@RequiredArgsConstructor
public class GymController {

    private final GymService gymService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<GymResponse> getAll() {
        return gymService.findAll();
    }

    // Public — used by the member registration flow to let a new signup find their gym before they have an account.
    @GetMapping("/search")
    public List<GymSearchResult> search(@RequestParam String q) {
        return gymService.search(q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@security.hasGymAccess(#id)")
    public GymResponse getById(@PathVariable Long id) {
        return gymService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GymResponse> create(@Valid @RequestBody GymRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.canManageGym(#id)")
    public GymResponse update(@PathVariable Long id, @Valid @RequestBody GymRequest request) {
        return gymService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gymService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/owner/{managerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public GymResponse setOwner(@PathVariable Long gymId, @PathVariable Long managerId) {
        return gymService.setOwner(gymId, managerId);
    }

    @PostMapping("/{gymId}/managers/{managerId}")
    @PreAuthorize("@security.canManageGym(#gymId)")
    public ResponseEntity<GymResponse> addManager(@PathVariable Long gymId, @PathVariable Long managerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymService.addManager(gymId, managerId));
    }

    @DeleteMapping("/{gymId}/managers/{managerId}")
    @PreAuthorize("@security.canManageGym(#gymId)")
    public GymResponse removeManager(@PathVariable Long gymId, @PathVariable Long managerId) {
        return gymService.removeManager(gymId, managerId);
    }
}

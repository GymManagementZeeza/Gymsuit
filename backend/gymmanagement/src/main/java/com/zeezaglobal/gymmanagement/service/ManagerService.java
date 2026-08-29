package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.ManagerRequest;
import com.zeezaglobal.gymmanagement.dto.ManagerResponse;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerService {

    private final ManagerRepository managerRepository;

    public List<ManagerResponse> findAll() {
        return managerRepository.findAll().stream()
                .map(ManagerResponse::fromEntity)
                .toList();
    }

    public ManagerResponse findById(Long id) {
        return ManagerResponse.fromEntity(getManagerOrThrow(id));
    }

    public ManagerResponse create(ManagerRequest request) {
        Manager manager = new Manager();
        applyRequest(manager, request);
        return ManagerResponse.fromEntity(managerRepository.save(manager));
    }

    public ManagerResponse update(Long id, ManagerRequest request) {
        Manager manager = getManagerOrThrow(id);
        applyRequest(manager, request);
        return ManagerResponse.fromEntity(managerRepository.save(manager));
    }

    public void delete(Long id) {
        Manager manager = getManagerOrThrow(id);
        managerRepository.delete(manager);
    }

    Manager getManagerOrThrow(Long id) {
        return managerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + id));
    }

    private void applyRequest(Manager manager, ManagerRequest request) {
        manager.setFirstName(request.firstName());
        manager.setLastName(request.lastName());
        manager.setEmail(request.email());
        manager.setPhone(request.phone());
    }
}

package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.GymRequest;
import com.zeezaglobal.gymmanagement.dto.GymResponse;
import com.zeezaglobal.gymmanagement.dto.GymSearchResult;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymRepository gymRepository;
    private final ManagerService managerService;
    private final SecurityService securityService;

    public List<GymResponse> findAll() {
        return gymRepository.findAll().stream()
                .map(GymResponse::fromEntity)
                .toList();
    }

    public GymResponse findById(Long id) {
        GymResponse response = GymResponse.fromEntity(getGymOrThrow(id));
        UserPrincipal user = securityService.currentUser();
        if (user != null && user.getRole() == Role.MEMBER) {
            // Members can see their gym's profile, but not the owner/managers' personal contact details.
            return response.withoutStaffContacts();
        }
        return response;
    }

    public List<GymSearchResult> search(String query) {
        return gymRepository.findByNameContainingIgnoreCase(query).stream()
                .map(GymSearchResult::fromEntity)
                .toList();
    }

    public GymResponse create(GymRequest request) {
        Gym gym = new Gym();
        applyRequest(gym, request);
        return GymResponse.fromEntity(gymRepository.save(gym));
    }

    public GymResponse update(Long id, GymRequest request) {
        Gym gym = getGymOrThrow(id);
        applyRequest(gym, request);
        return GymResponse.fromEntity(gymRepository.save(gym));
    }

    public void delete(Long id) {
        Gym gym = getGymOrThrow(id);
        gymRepository.delete(gym);
    }

    public GymResponse setOwner(Long gymId, Long managerId) {
        Gym gym = getGymOrThrow(gymId);
        Manager manager = managerService.getManagerOrThrow(managerId);
        gym.setOwner(manager);
        return GymResponse.fromEntity(gymRepository.save(gym));
    }

    public GymResponse addManager(Long gymId, Long managerId) {
        Gym gym = getGymOrThrow(gymId);
        Manager manager = managerService.getManagerOrThrow(managerId);
        if (!gym.getManagers().add(manager)) {
            throw new ConflictException("Manager " + managerId + " is already assigned to gym " + gymId);
        }
        return GymResponse.fromEntity(gymRepository.save(gym));
    }

    public GymResponse removeManager(Long gymId, Long managerId) {
        Gym gym = getGymOrThrow(gymId);
        Manager manager = managerService.getManagerOrThrow(managerId);
        if (!gym.getManagers().remove(manager)) {
            throw new ResourceNotFoundException("Manager " + managerId + " is not assigned to gym " + gymId);
        }
        return GymResponse.fromEntity(gymRepository.save(gym));
    }

    Gym getGymOrThrow(Long id) {
        return gymRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with id: " + id));
    }

    private void applyRequest(Gym gym, GymRequest request) {
        gym.setName(request.name());
        gym.setAddressLine(request.addressLine());
        gym.setCity(request.city());
        gym.setState(request.state());
        gym.setPostalCode(request.postalCode());
        gym.setCountry(request.country());
        gym.setPhone(request.phone());
        gym.setEmail(request.email());
        gym.setLogoUrl(request.logoUrl());
        gym.setUpiId(request.upiId());
        if (request.joiningFee() != null) {
            gym.setJoiningFee(request.joiningFee());
            gym.setJoiningFeeCurrency(CurrencyCodes.validate(
                    request.joiningFeeCurrency() != null ? request.joiningFeeCurrency() : "INR"));
        }
    }
}

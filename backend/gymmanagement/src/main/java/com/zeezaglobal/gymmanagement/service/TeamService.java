package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.InviteManagerRequest;
import com.zeezaglobal.gymmanagement.dto.TeamManagerResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.GymManagerAccess;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.GymManagerAccessRepository;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import com.zeezaglobal.gymmanagement.repository.ManagerRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final GymRepository gymRepository;
    private final ManagerRepository managerRepository;
    private final UserRepository userRepository;
    private final GymManagerAccessRepository accessRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TeamManagerResponse> listManagers(Long gymId) {
        Gym gym = getGymOrThrow(gymId);
        return gym.getManagers().stream()
                .map(manager -> toResponse(gym, manager))
                .toList();
    }

    @Transactional
    public TeamManagerResponse inviteManager(Long gymId, InviteManagerRequest request) {
        Gym gym = getGymOrThrow(gymId);

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }

        Manager manager = new Manager();
        manager.setFirstName(request.firstName());
        manager.setLastName(request.lastName());
        manager.setEmail(request.email());
        manager.setPhone(request.phone());
        manager = managerRepository.save(manager);

        gym.getManagers().add(manager);
        gymRepository.save(gym);

        // Managers sign in via OTP/Google only, same as owners and members — the password is unusable filler.
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.MANAGER);
        user.setGym(gym);
        user.setManager(manager);
        user.setEnabled(true);
        userRepository.save(user);

        applyScopes(gym, manager, request.scopes());

        return toResponse(gym, manager);
    }

    @Transactional
    public TeamManagerResponse updateAccess(Long gymId, Long managerId, Set<ManagerAccessScope> scopes) {
        Gym gym = getGymOrThrow(gymId);
        Manager manager = getAssignedManagerOrThrow(gym, managerId);
        applyScopes(gym, manager, scopes);
        return toResponse(gym, manager);
    }

    @Transactional
    public void removeManager(Long gymId, Long managerId) {
        Gym gym = getGymOrThrow(gymId);
        Manager manager = getAssignedManagerOrThrow(gym, managerId);

        gym.getManagers().remove(manager);
        gymRepository.save(gym);
        accessRepository.deleteAllByGymIdAndManagerId(gymId, managerId);

        userRepository.findByEmail(manager.getEmail())
                .filter(user -> user.getRole() == Role.MANAGER
                        && user.getGym() != null
                        && gym.getId().equals(user.getGym().getId()))
                .ifPresent(userRepository::delete);
    }

    private void applyScopes(Gym gym, Manager manager, Set<ManagerAccessScope> scopes) {
        accessRepository.deleteAllByGymIdAndManagerId(gym.getId(), manager.getId());
        Set<ManagerAccessScope> safeScopes = scopes == null ? Set.of() : scopes;
        for (ManagerAccessScope scope : safeScopes) {
            GymManagerAccess access = new GymManagerAccess();
            access.setGym(gym);
            access.setManager(manager);
            access.setScope(scope);
            accessRepository.save(access);
        }
    }

    private TeamManagerResponse toResponse(Gym gym, Manager manager) {
        Set<ManagerAccessScope> scopes = accessRepository.findAllByGymIdAndManagerId(gym.getId(), manager.getId())
                .stream()
                .map(GymManagerAccess::getScope)
                .collect(Collectors.toSet());
        return TeamManagerResponse.fromEntity(manager, scopes);
    }

    private Manager getAssignedManagerOrThrow(Gym gym, Long managerId) {
        return gym.getManagers().stream()
                .filter(m -> m.getId().equals(managerId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Manager " + managerId + " is not assigned to gym " + gym.getId()));
    }

    private Gym getGymOrThrow(Long id) {
        return gymRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with id: " + id));
    }
}

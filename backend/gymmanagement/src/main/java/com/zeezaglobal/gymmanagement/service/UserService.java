package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.CurrentUserResponse;
import com.zeezaglobal.gymmanagement.dto.UserRequest;
import com.zeezaglobal.gymmanagement.dto.UserResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.entity.ManagerAccessScope;
import com.zeezaglobal.gymmanagement.repository.GymManagerAccessRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<Role> OWNER_CREATABLE_ROLES = Set.of(Role.MANAGER, Role.TRAINER, Role.MEMBER);
    private static final Set<Role> MANAGER_CREATABLE_ROLES = Set.of(Role.TRAINER, Role.MEMBER);

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final GymService gymService;
    private final ManagerService managerService;
    private final SecurityService securityService;
    private final PasswordEncoder passwordEncoder;
    private final GymManagerAccessRepository gymManagerAccessRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    public CurrentUserResponse getCurrentUser() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new BadRequestException("Not authenticated");
        }
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getUserId()));

        String displayName;
        if (user.getManager() != null) {
            displayName = user.getManager().getFirstName() + " " + user.getManager().getLastName();
        } else if (user.getTrainer() != null) {
            displayName = user.getTrainer().getFirstName() + " " + user.getTrainer().getLastName();
        } else if (user.getMember() != null) {
            displayName = user.getMember().getFirstName() + " " + user.getMember().getLastName();
        } else {
            displayName = user.getEmail();
        }

        Set<ManagerAccessScope> scopes = (user.getRole() == Role.MANAGER && user.getGym() != null && user.getManager() != null)
                ? gymManagerAccessRepository.findAllByGymIdAndManagerId(user.getGym().getId(), user.getManager().getId())
                        .stream().map(access -> access.getScope()).collect(Collectors.toSet())
                : Set.of();

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                displayName,
                user.getRole().name(),
                principal.getGymId(),
                principal.getTrainerId(),
                principal.getMemberId(),
                principal.getManagerId(),
                scopes
        );
    }

    public UserResponse create(UserRequest request) {
        UserPrincipal currentUser = securityService.currentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

        if (!isAdmin) {
            if (currentUser == null || (currentUser.getRole() != Role.OWNER && currentUser.getRole() != Role.MANAGER)) {
                throw new BadRequestException("Only an admin, gym owner, or manager can create users");
            }
            Set<Role> allowedRoles = currentUser.getRole() == Role.OWNER ? OWNER_CREATABLE_ROLES : MANAGER_CREATABLE_ROLES;
            if (!allowedRoles.contains(request.role())) {
                throw new BadRequestException(currentUser.getRole() + " may only create users with roles: " + allowedRoles);
            }
            if (!currentUser.getGymId().equals(request.gymId())) {
                throw new BadRequestException("You may only create users for your own gym");
            }
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);

        switch (request.role()) {
            case ADMIN -> {
                if (request.gymId() != null || request.trainerId() != null || request.memberId() != null) {
                    throw new BadRequestException("Admin users must not be associated with a gym, trainer, or member");
                }
            }
            case OWNER, MANAGER -> {
                if (request.gymId() == null) {
                    throw new BadRequestException(request.role() + " users must be associated with a gym");
                }
                if (request.memberId() != null) {
                    throw new BadRequestException(request.role() + " users must not be associated with a member record");
                }
                Gym gym = gymService.getGymOrThrow(request.gymId());
                user.setGym(gym);
                // An owner/manager may also be the gym's (only) trainer — small gyms are often run by one person
                // who holds all three roles. The OWNER/MANAGER role already grants full trainer-level access
                // (see SecurityService), so this link is purely to identify them as a trainer on sessions.
                if (request.trainerId() != null) {
                    Trainer trainer = trainerRepository.findByIdAndGymId(request.trainerId(), request.gymId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Trainer not found with id: " + request.trainerId() + " in gym: " + request.gymId()));
                    user.setTrainer(trainer);
                }
                // A MANAGER user may be linked to their own Manager business record (must actually be
                // assigned to this gym as owner or manager) — needed to scope "my own compensation/payments".
                if (request.managerId() != null) {
                    Manager manager = managerService.getManagerOrThrow(request.managerId());
                    boolean assignedToGym = gym.getManagers().stream().anyMatch(m -> m.getId().equals(manager.getId()))
                            || (gym.getOwner() != null && gym.getOwner().getId().equals(manager.getId()));
                    if (!assignedToGym) {
                        throw new BadRequestException("Manager " + request.managerId() + " is not assigned to gym " + request.gymId());
                    }
                    user.setManager(manager);
                }
            }
            case TRAINER -> {
                if (request.gymId() == null || request.trainerId() == null) {
                    throw new BadRequestException("Trainer users must be associated with a gym and a trainer record");
                }
                Gym gym = gymService.getGymOrThrow(request.gymId());
                Trainer trainer = trainerRepository.findByIdAndGymId(request.trainerId(), request.gymId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Trainer not found with id: " + request.trainerId() + " in gym: " + request.gymId()));
                user.setGym(gym);
                user.setTrainer(trainer);
            }
            case MEMBER -> {
                if (request.gymId() == null || request.memberId() == null) {
                    throw new BadRequestException("Member users must be associated with a gym and a member record");
                }
                Gym gym = gymService.getGymOrThrow(request.gymId());
                Member member = memberRepository.findByIdAndGymId(request.memberId(), request.gymId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Member not found with id: " + request.memberId() + " in gym: " + request.gymId()));
                user.setGym(gym);
                user.setMember(member);
            }
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    /**
     * Links an existing OWNER/MANAGER user to a trainer record in their own gym — for the common small-gym
     * case where the same person creates their own Trainer record after their account already exists.
     */
    public UserResponse linkTrainer(Long userId, Long trainerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.OWNER && user.getRole() != Role.MANAGER) {
            throw new BadRequestException("Only an owner or manager user can be linked to a trainer record");
        }
        if (user.getGym() == null) {
            throw new BadRequestException("User " + userId + " is not associated with a gym");
        }

        UserPrincipal currentUser = securityService.currentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isSelf = currentUser != null && currentUser.getUserId().equals(userId);
        boolean isSameGymOwnerOrManager = currentUser != null
                && (currentUser.getRole() == Role.OWNER || currentUser.getRole() == Role.MANAGER)
                && currentUser.getGymId().equals(user.getGym().getId());
        if (!isAdmin && !isSelf && !isSameGymOwnerOrManager) {
            throw new BadRequestException("You may not modify this user");
        }

        Trainer trainer = trainerRepository.findByIdAndGymId(trainerId, user.getGym().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trainer not found with id: " + trainerId + " in gym: " + user.getGym().getId()));

        user.setTrainer(trainer);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }
}

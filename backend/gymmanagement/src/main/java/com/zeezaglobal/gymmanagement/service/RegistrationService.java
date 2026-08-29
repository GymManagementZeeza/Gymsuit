package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.MemberRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.OwnerRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.SubscribeRequest;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import com.zeezaglobal.gymmanagement.repository.ManagerRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final ManagerRepository managerRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final MemberSubscriptionService memberSubscriptionService;

    public LoginResponse registerOwner(OwnerRegisterRequest request) {
        ensureEmailAvailable(request.email());

        Manager manager = new Manager();
        manager.setFirstName(request.firstName());
        manager.setLastName(request.lastName());
        manager.setEmail(request.email());
        manager.setPhone(request.phone());
        manager = managerRepository.save(manager);

        Gym gym = new Gym();
        gym.setName(request.gymName());
        gym.setAddressLine(request.addressLine());
        gym.setCity(request.city());
        gym.setState(request.state());
        gym.setPostalCode(request.postalCode());
        gym.setCountry(request.country());
        gym.setPhone(request.gymPhone());
        gym.setOwner(manager);
        gym = gymRepository.save(gym);

        User user = newUserShell(request.email(), Role.OWNER);
        user.setGym(gym);
        user.setManager(manager);
        user = userRepository.save(user);

        return authService.issueSession(new UserPrincipal(user));
    }

    public LoginResponse registerMember(MemberRegisterRequest request) {
        ensureEmailAvailable(request.email());

        Gym gym = gymRepository.findById(request.gymId())
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found with id: " + request.gymId()));

        Member member = new Member();
        member.setGym(gym);
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setDateOfBirth(request.dateOfBirth());
        member.setGender(request.gender());
        member.setWaiverAccepted(request.waiverAccepted());
        member.setJoinDate(LocalDate.now());
        member = memberRepository.save(member);

        User user = newUserShell(request.email(), Role.MEMBER);
        user.setGym(gym);
        user.setMember(member);
        user = userRepository.save(user);

        // The plan they picked is assigned right away but stays PENDING until the gym takes payment for it.
        memberSubscriptionService.subscribe(gym.getId(), member.getId(), new SubscribeRequest(request.planId()));

        return authService.issueSession(new UserPrincipal(user));
    }

    private User newUserShell(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        // These accounts sign in via OTP/Google only; a password is still required by the
        // schema, so store an unusable random one rather than relax the not-null constraint.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("A user with email " + email + " already exists");
        }
    }
}

package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.TrainerRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GymService gymService;
    private final PasswordEncoder passwordEncoder;

    public List<TrainerResponse> findAllByGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return trainerRepository.findAllByGymId(gymId).stream()
                .map(TrainerResponse::fromEntity)
                .toList();
    }

    public TrainerResponse findById(Long gymId, Long id) {
        return TrainerResponse.fromEntity(getTrainerOrThrow(gymId, id));
    }

    @Transactional
    public TrainerResponse create(Long gymId, TrainerRequest request) {
        Gym gym = gymService.getGymOrThrow(gymId);

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }

        Trainer trainer = new Trainer();
        trainer.setGym(gym);
        applyRequest(trainer, request);
        trainer = trainerRepository.save(trainer);

        provisionLogin(trainer, request.email());

        return TrainerResponse.fromEntity(trainer);
    }

    @Transactional
    public TrainerResponse update(Long gymId, Long id, TrainerRequest request) {
        Trainer trainer = getTrainerOrThrow(gymId, id);
        applyRequest(trainer, request);
        trainer = trainerRepository.save(trainer);

        provisionLogin(trainer, request.email());

        return TrainerResponse.fromEntity(trainer);
    }

    /**
     * Trainers sign in via OTP/Google only, same as everyone else — this login is what routes them to
     * /trainerdashboard after sign-in (see dashboardPathForRole on the frontend). Creates the login if
     * missing (covers trainers added before logins existed), or keeps an existing one in sync with the email.
     */
    private void provisionLogin(Trainer trainer, String email) {
        User user = userRepository.findByTrainerId(trainer.getId()).orElse(null);

        if (user == null) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                throw new ConflictException("A user with email " + email + " already exists");
            });
            user = new User();
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(Role.TRAINER);
            user.setGym(trainer.getGym());
            user.setTrainer(trainer);
            user.setEnabled(true);
        } else if (!user.getEmail().equalsIgnoreCase(email)) {
            User existingUser = user;
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getId().equals(existingUser.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException("A user with email " + email + " already exists");
                    });
        }

        user.setEmail(email);
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long gymId, Long id) {
        Trainer trainer = getTrainerOrThrow(gymId, id);
        if (trainerRepository.countByGymId(gymId) <= 1) {
            throw new ConflictException("Gym " + gymId + " must have at least one trainer");
        }
        List<Member> assigned = memberRepository.findAllByTrainerId(id);
        assigned.forEach(member -> member.setTrainer(null));
        memberRepository.saveAll(assigned);
        userRepository.findByTrainerId(id).ifPresent(userRepository::delete);
        trainerRepository.delete(trainer);
    }

    private Trainer getTrainerOrThrow(Long gymId, Long id) {
        return trainerRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + id + " in gym: " + gymId));
    }

    private void applyRequest(Trainer trainer, TrainerRequest request) {
        trainer.setFirstName(request.firstName());
        trainer.setLastName(request.lastName());
        trainer.setEmail(request.email());
        trainer.setPhone(request.phone());
        trainer.setSpecialization(request.specialization());
        trainer.setBio(request.bio());
        trainer.setHireDate(request.hireDate());
        trainer.setImageUrl(request.imageUrl());
        trainer.setCertificateUrl(request.certificateUrl());
    }
}

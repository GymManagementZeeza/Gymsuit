package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MemberNotifyRequest;
import com.zeezaglobal.gymmanagement.dto.MemberRequest;
import com.zeezaglobal.gymmanagement.dto.MemberResponse;
import com.zeezaglobal.gymmanagement.dto.MemberGoalRequest;
import com.zeezaglobal.gymmanagement.dto.MemberTrainerRequest;
import com.zeezaglobal.gymmanagement.dto.MemberWeightRequest;
import com.zeezaglobal.gymmanagement.dto.WeightLogResponse;
import com.zeezaglobal.gymmanagement.entity.ActivityType;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.NotificationChannel;
import com.zeezaglobal.gymmanagement.entity.NotificationType;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.GymTransaction;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WeightLog;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.CheckInRepository;
import com.zeezaglobal.gymmanagement.repository.GymTransactionRepository;
import com.zeezaglobal.gymmanagement.repository.MemberPaymentRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.MemberSubscriptionRepository;
import com.zeezaglobal.gymmanagement.repository.SessionEnrollmentRepository;
import com.zeezaglobal.gymmanagement.repository.SessionRequestRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WeightLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CheckInRepository checkInRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final SessionEnrollmentRepository sessionEnrollmentRepository;
    private final SessionRequestRepository sessionRequestRepository;
    private final GymTransactionRepository gymTransactionRepository;
    private final WeightLogRepository weightLogRepository;
    private final TrainerRepository trainerRepository;
    private final GymService gymService;
    private final GymActivityService activityService;
    private final PasswordEncoder passwordEncoder;

    public List<MemberResponse> findAllByGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return memberRepository.findAllByGymId(gymId).stream()
                .map(MemberResponse::fromEntity)
                .toList();
    }

    public MemberResponse findById(Long gymId, Long id) {
        return MemberResponse.fromEntity(getMemberOrThrow(gymId, id));
    }

    public MemberResponse create(Long gymId, MemberRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("A user with email " + request.email() + " already exists");
        }

        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = new Member();
        member.setGym(gym);
        applyRequest(member, request);
        member = memberRepository.save(member);

        // Every member gets a login the moment they're added — same email/OTP sign-in as everyone else,
        // routed to the member dashboard by their MEMBER role. No usable password: sign-in is passwordless.
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        user.setGym(gym);
        user.setMember(member);
        userRepository.save(user);
        logWeightIfPresent(member);

        activityService.record(gym, ActivityType.MEMBER_JOINED,
                member.getFirstName() + " " + member.getLastName() + " joined as a new member");
        return MemberResponse.fromEntity(member);
    }

    public MemberResponse update(Long gymId, Long id, MemberRequest request) {
        Member member = getMemberOrThrow(gymId, id);
        String previousEmail = member.getEmail();
        Double previousWeight = member.getWeightKg();
        applyRequest(member, request);
        member = memberRepository.save(member);

        if (!Objects.equals(previousWeight, member.getWeightKg())) {
            logWeightIfPresent(member);
        }

        if (previousEmail == null || !previousEmail.equalsIgnoreCase(request.email())) {
            userRepository.findByMemberId(member.getId()).ifPresent(user -> {
                userRepository.findByEmail(request.email())
                        .filter(existing -> !existing.getId().equals(user.getId()))
                        .ifPresent(existing -> {
                            throw new ConflictException("A user with email " + request.email() + " already exists");
                        });
                user.setEmail(request.email());
                userRepository.save(user);
            });
        }

        return MemberResponse.fromEntity(member);
    }

    /** Lets a member (or staff) record their own current weight — the one self-service field members can edit. */
    public MemberResponse updateWeight(Long gymId, Long id, MemberWeightRequest request) {
        Member member = getMemberOrThrow(gymId, id);
        member.setWeightKg(request.weightKg());
        member = memberRepository.save(member);
        logWeightIfPresent(member);
        return MemberResponse.fromEntity(member);
    }

    /** Lets a member set their own weight goal — a target weight by a target month. */
    public MemberResponse updateGoal(Long gymId, Long id, MemberGoalRequest request) {
        Member member = getMemberOrThrow(gymId, id);
        member.setGoalWeightKg(request.goalWeightKg());
        member.setGoalTargetDate(request.goalTargetDate());
        return MemberResponse.fromEntity(memberRepository.save(member));
    }

    /** Lets a member choose their own trainer (or staff assign one) — null trainerId unassigns. */
    public MemberResponse assignTrainer(Long gymId, Long id, MemberTrainerRequest request) {
        Member member = getMemberOrThrow(gymId, id);
        if (request.trainerId() == null) {
            member.setTrainer(null);
        } else {
            Trainer trainer = trainerRepository.findByIdAndGymId(request.trainerId(), gymId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Trainer not found with id: " + request.trainerId() + " in gym: " + gymId));
            member.setTrainer(trainer);
        }
        return MemberResponse.fromEntity(memberRepository.save(member));
    }

    public List<WeightLogResponse> findWeightHistory(Long gymId, Long id) {
        getMemberOrThrow(gymId, id);
        return weightLogRepository.findAllByMemberIdOrderByRecordedAtAsc(id).stream()
                .map(WeightLogResponse::fromEntity)
                .toList();
    }

    private void logWeightIfPresent(Member member) {
        if (member.getWeightKg() == null) {
            return;
        }
        WeightLog log = new WeightLog();
        log.setMember(member);
        log.setWeightKg(member.getWeightKg());
        log.setRecordedAt(LocalDateTime.now());
        weightLogRepository.save(log);
    }

    /**
     * Removing a member has to clear out everything else that points at them via a foreign key first —
     * their login, payments, subscriptions, check-ins, session requests/enrollments. Gym-ledger transactions
     * are gym accounting records, not the member's own, so those are kept and just detached instead of deleted.
     */
    @Transactional
    public void delete(Long gymId, Long id) {
        Member member = getMemberOrThrow(gymId, id);
        Gym gym = member.getGym();
        String name = member.getFirstName() + " " + member.getLastName();

        userRepository.findByMemberId(member.getId()).ifPresent(userRepository::delete);
        memberPaymentRepository.deleteAllByMemberId(member.getId());
        memberSubscriptionRepository.deleteAllByMemberId(member.getId());
        checkInRepository.deleteAllByMemberId(member.getId());
        sessionEnrollmentRepository.deleteAllByMemberId(member.getId());
        sessionRequestRepository.deleteAllByMemberId(member.getId());
        weightLogRepository.deleteAllByMemberId(member.getId());

        List<GymTransaction> transactions = gymTransactionRepository.findAllByMemberId(member.getId());
        transactions.forEach(transaction -> transaction.setMember(null));
        gymTransactionRepository.saveAll(transactions);

        memberRepository.delete(member);
        activityService.record(gym, ActivityType.MEMBER_REMOVED, name + " was removed from the gym");
    }

    /**
     * Records that a notification was sent to a member. WhatsApp is handled client-side via a
     * wa.me redirect (no server-side WhatsApp integration); SMS and email have no delivery channel
     * wired up yet, so those are recorded the same way but nothing is actually sent for them.
     */
    public void notify(Long gymId, Long id, MemberNotifyRequest request) {
        Member member = getMemberOrThrow(gymId, id);
        String name = member.getFirstName() + " " + member.getLastName();
        activityService.record(member.getGym(), ActivityType.NOTIFICATION_SENT,
                notificationLabel(request.type()) + " sent to " + name + " via " + channelLabel(request.channel())
                        + ": \"" + request.message() + "\"");
    }

    private String notificationLabel(NotificationType type) {
        return switch (type) {
            case PAYMENT_REMINDER -> "Payment reminder";
            case PROMOTION -> "Promotion";
            case COMEBACK_REMINDER -> "Come-back reminder";
            case CUSTOM -> "Message";
        };
    }

    private String channelLabel(NotificationChannel channel) {
        return switch (channel) {
            case WHATSAPP -> "WhatsApp";
            case SMS -> "SMS";
            case EMAIL -> "email";
        };
    }

    private Member getMemberOrThrow(Long gymId, Long id) {
        return memberRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id + " in gym: " + gymId));
    }

    private void applyRequest(Member member, MemberRequest request) {
        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setEmail(request.email());
        member.setPhone(request.phone());
        member.setDateOfBirth(request.dateOfBirth());
        member.setGender(request.gender());
        member.setHeightCm(request.heightCm());
        member.setWeightKg(request.weightKg());
        member.setBloodGroup(request.bloodGroup());
        member.setMedicalNotes(request.medicalNotes());
        member.setEmergencyContactName(request.emergencyContactName());
        member.setEmergencyContactPhone(request.emergencyContactPhone());
        member.setEmergencyContactRelationship(request.emergencyContactRelationship());
        member.setWaiverAccepted(request.waiverAccepted());
        member.setJoinDate(request.joinDate());
    }
}

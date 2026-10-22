package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MobileOnboardingResponse;
import com.zeezaglobal.gymmanagement.dto.MobileOnboardingSyncRequest;
import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WeightLog;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WeightLogRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileOnboardingService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final WeightLogRepository weightLogRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;
    private final com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository workoutRecordRepository;

    @Transactional
    public MobileOnboardingResponse syncOnboarding(MobileOnboardingSyncRequest request, UserPrincipal principal) {
        User user = null;
        if (principal != null && principal.getUserId() != null) {
            user = userRepository.findById(principal.getUserId()).orElse(null);
        }

        if (user == null && request.email() != null && !request.email().isBlank()) {
            String cleanEmail = request.email().trim().toLowerCase();
            user = userRepository.findByEmail(cleanEmail).orElse(null);
        }

        Gym defaultGym = null;
        if (user != null && user.getGym() != null) {
            defaultGym = user.getGym();
        } else {
            defaultGym = gymRepository.findAll().stream().findFirst().orElseGet(() -> {
                Gym gym = new Gym();
                gym.setName("GymSuit Global");
                gym.setEmail("support@gymsuit.app");
                gym.setPhone("18005550199");
                return gymRepository.save(gym);
            });
        }

        Member member = null;
        if (user != null && user.getMember() != null) {
            member = user.getMember();
        } else if (user != null) {
            member = new Member();
            member.setGym(defaultGym);
            member.setEmail(user.getEmail());
            member.setFirstName(user.getEmail());
            member.setLastName("");
            member.setPhone("0000000000");
            member.setJoinDate(LocalDate.now());
            member.setWaiverAccepted(true);
            member.setJoiningFeePaid(true);
        } else if (request.email() != null && !request.email().isBlank()) {
            String cleanEmail = request.email().trim().toLowerCase();
            member = memberRepository.findAll().stream()
                    .filter(m -> cleanEmail.equalsIgnoreCase(m.getEmail()))
                    .findFirst()
                    .orElseGet(() -> {
                        Member m = new Member();
                        Gym g = gymRepository.findAll().stream().findFirst().orElse(null);
                        m.setGym(g);
                        m.setEmail(cleanEmail);
                        m.setFirstName(cleanEmail);
                        m.setLastName("");
                        m.setPhone("0000000000");
                        m.setJoinDate(LocalDate.now());
                        m.setWaiverAccepted(true);
                        m.setJoiningFeePaid(true);
                        return m;
                    });
        }

        if (member == null) {
            // Create anonymous/standalone member profile
            member = new Member();
            member.setGym(defaultGym);
            member.setEmail("member-" + System.currentTimeMillis() + "@gymsuit.app");
            member.setFirstName("Member");
            member.setLastName("");
            member.setPhone("0000000000");
            member.setJoinDate(LocalDate.now());
            member.setWaiverAccepted(true);
            member.setJoiningFeePaid(true);
        }

        // Apply onboarding data
        if (request.workoutGoal() != null && !request.workoutGoal().isBlank()) {
            member.setWorkoutGoal(request.workoutGoal().trim());
        }
        if (request.heightCm() != null && request.heightCm() > 0) {
            member.setHeightCm(request.heightCm());
        }
        if (request.weightKg() != null && request.weightKg() > 0) {
            member.setWeightKg(request.weightKg());
        }
        if (request.targetWeightKg() != null && request.targetWeightKg() > 0) {
            member.setGoalWeightKg(request.targetWeightKg());
        }
        if (request.targetDate() != null) {
            member.setGoalTargetDate(request.targetDate());
        }
        if (request.workoutDaysPerWeek() != null && request.workoutDaysPerWeek() > 0) {
            member.setWorkoutDaysPerWeek(request.workoutDaysPerWeek());
        }

        if (request.dateOfBirth() != null) {
            member.setDateOfBirth(request.dateOfBirth());
        } else if (request.birthYear() != null && request.birthYear() > 1900 && request.birthYear() < 2100) {
            member.setDateOfBirth(LocalDate.of(request.birthYear(), 1, 1));
        }

        member = memberRepository.save(member);

        if (user != null && user.getMember() == null) {
            user.setMember(member);
            userRepository.save(user);
        }

        // Log initial weight into WeightLog table
        if (member.getWeightKg() != null && member.getWeightKg() > 0) {
            WeightLog logEntry = new WeightLog();
            logEntry.setMember(member);
            logEntry.setWeightKg(member.getWeightKg());
            logEntry.setRecordedAt(LocalDateTime.now());
            weightLogRepository.save(logEntry);
        }

        // Update DailyHealthRecord today
        if (user != null && member.getWeightKg() != null && member.getWeightKg() > 0) {
            final User targetUser = user;
            LocalDate today = LocalDate.now();
            Optional<DailyHealthRecord> recordOpt = dailyHealthRecordRepository.findByUserIdAndRecordDate(targetUser.getId(), today);
            DailyHealthRecord record = recordOpt.orElseGet(() -> {
                DailyHealthRecord r = new DailyHealthRecord();
                r.setUser(targetUser);
                r.setRecordDate(today);
                return r;
            });
            record.setWeightKg(member.getWeightKg());
            record.setUpdatedAt(LocalDateTime.now());
            dailyHealthRecordRepository.save(record);
        }

        Integer birthYear = member.getDateOfBirth() != null ? member.getDateOfBirth().getYear() : request.birthYear();

        log.info("Successfully synced onboarding data for member {} (goal: {}, weight: {} kg, target: {} kg)",
                member.getId(), member.getWorkoutGoal(), member.getWeightKg(), member.getGoalWeightKg());

        return new MobileOnboardingResponse(
                member.getId(),
                member.getWorkoutGoal(),
                member.getHeightCm(),
                member.getWeightKg(),
                member.getGoalWeightKg(),
                birthYear,
                member.getDateOfBirth(),
                member.getWorkoutDaysPerWeek(),
                member.getGoalTargetDate(),
                "Workout onboarding data synchronized successfully."
        );
    }

    @Transactional(readOnly = true)
    public MobileOnboardingResponse getOnboarding(UserPrincipal principal, String email) {
        User user = null;
        if (principal != null && principal.getUserId() != null) {
            user = userRepository.findById(principal.getUserId()).orElse(null);
        }

        if (user == null && email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        }

        Member member = null;
        if (user != null) {
            member = user.getMember();
        } else if (email != null && !email.isBlank()) {
            String cleanEmail = email.trim().toLowerCase();
            member = memberRepository.findAll().stream()
                    .filter(m -> cleanEmail.equalsIgnoreCase(m.getEmail()))
                    .findFirst()
                    .orElse(null);
        }

        if (member == null) {
            return new MobileOnboardingResponse(null, null, null, null, null, null, null, null, null, "No onboarding data found");
        }

        Integer birthYear = member.getDateOfBirth() != null ? member.getDateOfBirth().getYear() : null;

        return new MobileOnboardingResponse(
                member.getId(),
                member.getWorkoutGoal(),
                member.getHeightCm(),
                member.getWeightKg(),
                member.getGoalWeightKg(),
                birthYear,
                member.getDateOfBirth(),
                member.getWorkoutDaysPerWeek(),
                member.getGoalTargetDate(),
                "Onboarding data fetched successfully."
        );
    }

    @Transactional
    public java.util.Map<String, Object> resetOnboarding(UserPrincipal principal, String email) {
        User user = null;
        if (principal != null && principal.getUserId() != null) {
            user = userRepository.findById(principal.getUserId()).orElse(null);
        }

        if (user == null && email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        }

        Member member = null;
        if (user != null) {
            member = user.getMember();
        } else if (email != null && !email.isBlank()) {
            String cleanEmail = email.trim().toLowerCase();
            member = memberRepository.findAll().stream()
                    .filter(m -> cleanEmail.equalsIgnoreCase(m.getEmail()))
                    .findFirst()
                    .orElse(null);
        }

        if (member != null) {
            member.setWorkoutGoal(null);
            member.setHeightCm(null);
            member.setWeightKg(null);
            member.setGoalWeightKg(null);
            member.setGoalTargetDate(null);
            member.setWorkoutDaysPerWeek(null);
            weightLogRepository.deleteAllByMemberId(member.getId());
            memberRepository.save(member);
        }

        if (user != null) {
            workoutRecordRepository.deleteAllByUserId(user.getId());
            dailyHealthRecordRepository.findByUserIdAndRecordDate(user.getId(), LocalDate.now())
                    .ifPresent(record -> {
                        record.setWeightKg(null);
                        record.setUpdatedAt(LocalDateTime.now());
                        dailyHealthRecordRepository.save(record);
                    });
        }

        log.info("Reset workout onboarding data for member {}", member != null ? member.getId() : "null");
        return java.util.Map.of(
                "success", true,
                "message", "Workout and onboarding data deleted successfully."
        );
    }
}

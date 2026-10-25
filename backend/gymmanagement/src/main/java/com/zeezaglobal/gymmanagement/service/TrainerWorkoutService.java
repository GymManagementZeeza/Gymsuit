package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.TrainerWorkoutSuggestionRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerWorkoutSuggestionResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.SuggestedWorkoutExercise;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.TrainerWorkoutSuggestion;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerWorkoutSuggestionRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrainerWorkoutService {

    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final TrainerWorkoutSuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final PushNotificationService pushNotificationService;
    private final EmailService emailService;

    /**
     * A trainer sends today's workout to one of their clients. Sending again
     * the same day replaces the exercise list. The client gets a Firebase push
     * notification (if they have the app) and an email (if they have one).
     * Push/email failures never fail the send itself.
     */
    @Transactional
    public TrainerWorkoutSuggestionResponse sendSuggestion(Long gymId, Long trainerId, Long memberId,
                                                           TrainerWorkoutSuggestionRequest request) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        Role role = principal.getRole();
        boolean isStaff = role == Role.ADMIN || role == Role.OWNER || role == Role.MANAGER;
        boolean isSelfTrainer = role == Role.TRAINER && trainerId.equals(principal.getTrainerId());
        if (!isStaff && !isSelfTrainer) {
            throw new AccessDeniedException("Only a trainer (or gym staff) can send a workout to a client");
        }
        if (!isStaff && !gymId.equals(principal.getGymId())) {
            throw new AccessDeniedException("Trainer does not belong to this gym");
        }

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Gym not found"));
        Trainer trainer = trainerRepository.findByIdAndGymId(trainerId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found"));
        Member member = memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (isSelfTrainer && (member.getTrainer() == null || !member.getTrainer().getId().equals(trainerId))) {
            throw new AccessDeniedException("Member is not assigned to this trainer");
        }

        LocalDate today = LocalDate.now();
        TrainerWorkoutSuggestion suggestion = suggestionRepository
                .findByMemberIdAndSuggestionDate(memberId, today)
                .orElseGet(() -> {
                    TrainerWorkoutSuggestion created = new TrainerWorkoutSuggestion();
                    created.setGym(gym);
                    created.setTrainer(trainer);
                    created.setMember(member);
                    created.setSuggestionDate(today);
                    created.setCreatedAt(Instant.now());
                    return created;
                });
        suggestion.setTrainer(trainer);
        suggestion.setMessage(request.message());
        suggestion.getExercises().clear();
        for (int i = 0; i < request.exercises().size(); i++) {
            TrainerWorkoutSuggestionRequest.ExerciseInput input = request.exercises().get(i);
            SuggestedWorkoutExercise exercise = new SuggestedWorkoutExercise();
            exercise.setSuggestion(suggestion);
            exercise.setExerciseName(input.exerciseName().trim());
            exercise.setSets(input.sets());
            exercise.setReps(input.reps());
            exercise.setWeightKg(input.weightKg());
            exercise.setSortOrder(i);
            suggestion.getExercises().add(exercise);
        }
        suggestionRepository.save(suggestion);

        String trainerName = trainer.getFirstName() + " " + trainer.getLastName();
        String exerciseSummary = request.exercises().stream()
                .map(e -> e.exerciseName().trim())
                .limit(3)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        if (request.exercises().size() > 3) {
            exerciseSummary += " and more";
        }
        String title = "Today's workout from " + trainerName;
        String body = exerciseSummary.isBlank()
                ? "Your trainer sent you today's workout. Open GymSuit to see it."
                : "Today: " + exerciseSummary + ". Open GymSuit to see the full workout.";

        // Push: only if the member has a GymSuit app account with a device.
        try {
            userRepository.findByMemberId(memberId).ifPresent(user -> {
                Map<String, String> data = new HashMap<>();
                data.put("type", "TRAINER_WORKOUT");
                data.put("suggestionId", String.valueOf(suggestion.getId()));
                data.put("date", today.toString());
                pushNotificationService.sendToUser(user.getId(), title, body, data, "gymsuit_trainer");
            });
        } catch (Exception e) {
            log.warn("Trainer workout push failed for member {}: {}", memberId, e.getMessage());
        }

        // Email: only if the member has an email address on file.
        try {
            if (member.getEmail() != null && !member.getEmail().isBlank()) {
                emailService.sendEmail(member.getEmail(), title, workoutEmailHtml(trainerName, request));
            }
        } catch (Exception e) {
            log.warn("Trainer workout email failed for member {}: {}", memberId, e.getMessage());
        }

        return toResponse(suggestion, trainerName);
    }

    /** Today's suggested workout for the signed-in app user, or empty. */
    @Transactional(readOnly = true)
    public Optional<TrainerWorkoutSuggestionResponse> getTodaySuggestion(Long memberId) {
        return suggestionRepository.findByMemberIdAndSuggestionDate(memberId, LocalDate.now())
                .map(suggestion -> toResponse(suggestion,
                        suggestion.getTrainer().getFirstName() + " " + suggestion.getTrainer().getLastName()));
    }

    private TrainerWorkoutSuggestionResponse toResponse(TrainerWorkoutSuggestion suggestion, String trainerName) {
        List<TrainerWorkoutSuggestionResponse.ExerciseDto> exercises = suggestion.getExercises().stream()
                .sorted(Comparator.comparing(SuggestedWorkoutExercise::getSortOrder))
                .map(e -> new TrainerWorkoutSuggestionResponse.ExerciseDto(
                        e.getExerciseName(), e.getSets(), e.getReps(), e.getWeightKg()))
                .toList();
        return new TrainerWorkoutSuggestionResponse(
                suggestion.getId(), suggestion.getSuggestionDate(), trainerName,
                suggestion.getMessage(), exercises);
    }

    private String workoutEmailHtml(String trainerName, TrainerWorkoutSuggestionRequest request) {
        StringBuilder rows = new StringBuilder();
        for (TrainerWorkoutSuggestionRequest.ExerciseInput exercise : request.exercises()) {
            String weight = exercise.weightKg() > 0
                    ? String.format("%.1f kg", exercise.weightKg())
                    : "Bodyweight";
            rows.append("<tr><td style=\"padding:8px 12px;border-bottom:1px solid #eee;\">")
                    .append(escape(exercise.exerciseName().trim()))
                    .append("</td><td style=\"padding:8px 12px;border-bottom:1px solid #eee;\">")
                    .append(exercise.sets()).append(" sets × ").append(exercise.reps()).append(" reps")
                    .append("</td><td style=\"padding:8px 12px;border-bottom:1px solid #eee;\">")
                    .append(weight)
                    .append("</td></tr>");
        }
        String messagePart = request.message() != null && !request.message().isBlank()
                ? "<p>" + escape(request.message().trim()) + "</p>"
                : "";
        return "<html><body style=\"font-family:Arial,sans-serif;color:#222;\">"
                + "<h2>Today's workout from " + escape(trainerName) + "</h2>"
                + messagePart
                + "<table style=\"border-collapse:collapse;\">"
                + "<tr><th style=\"text-align:left;padding:8px 12px;border-bottom:2px solid #ddd;\">Exercise</th>"
                + "<th style=\"text-align:left;padding:8px 12px;border-bottom:2px solid #ddd;\">Sets × Reps</th>"
                + "<th style=\"text-align:left;padding:8px 12px;border-bottom:2px solid #ddd;\">Weight</th></tr>"
                + rows
                + "</table>"
                + "<p>Open the GymSuit app to see this workout in your workouts section.</p>"
                + "</body></html>";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MealPlanRequest;
import com.zeezaglobal.gymmanagement.dto.MealPlanResponse;
import com.zeezaglobal.gymmanagement.entity.MealPlan;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.NutritionistClient;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
import com.zeezaglobal.gymmanagement.repository.MealPlanRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistClientRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Meal recommendations (breakfast, lunch, evening snack, dinner) a standalone nutritionist sends to a linked
 * client. The client gets a push notification (if they use the app) and an email pointing them to the app,
 * where the plan shows on the Food tab. Push/email failures never fail the send itself.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanService {

    static final String PUSH_TYPE = "MEAL_PLAN";
    static final String PUSH_CHANNEL = "gymsuit_nutrition";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);

    private final MealPlanRepository mealPlanRepository;
    private final NutritionistClientRepository clientRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;
    private final PushNotificationService pushNotificationService;
    private final EventPublisher eventPublisher;

    @Value("${app.diet-url:https://diet.gymsuit.app}")
    private String dietUrl;

    @Value("${app.android-app-url:https://play.google.com/store/apps/details?id=ca.zeezaglobal.gymsuitapp}")
    private String androidAppUrl;

    /** Sends (or, for the same client and day, replaces) a meal plan, then notifies the client. */
    @Transactional
    public MealPlanResponse send(Long memberId, MealPlanRequest request) {
        Long me = requireNutritionist();
        NutritionistClient link = clientRepository.findByNutritionistIdAndMemberId(me, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        String breakfast = clean(request.breakfast());
        String lunch = clean(request.lunch());
        String snack = clean(request.eveningSnack());
        String dinner = clean(request.dinner());
        if (breakfast == null && lunch == null && snack == null && dinner == null) {
            throw new BadRequestException("Add a recommendation for at least one meal");
        }
        LocalDate date = request.date();
        if (date.isBefore(LocalDate.now().minusDays(1))) {
            throw new BadRequestException("Pick today or a future date for the meal plan");
        }

        Instant now = Instant.now();
        MealPlan plan = mealPlanRepository.findByNutritionistIdAndMemberIdAndPlanDate(me, memberId, date)
                .orElseGet(() -> {
                    MealPlan created = new MealPlan();
                    created.setNutritionist(link.getNutritionist());
                    created.setMember(link.getMember());
                    created.setPlanDate(date);
                    created.setCreatedAt(now);
                    return created;
                });
        plan.setBreakfast(breakfast);
        plan.setLunch(lunch);
        plan.setEveningSnack(snack);
        plan.setDinner(dinner);
        plan.setNote(clean(request.note()));
        plan.setUpdatedAt(now);
        plan = mealPlanRepository.save(plan);

        notifyClient(plan);
        return MealPlanResponse.fromEntity(plan);
    }

    /** The last few plans this nutritionist sent a client, newest date first. */
    @Transactional(readOnly = true)
    public List<MealPlanResponse> history(Long memberId) {
        Long me = requireNutritionist();
        if (!clientRepository.existsByNutritionistIdAndMemberId(me, memberId)) {
            throw new ResourceNotFoundException("Client not found");
        }
        return mealPlanRepository.findTop10ByNutritionistIdAndMemberIdOrderByPlanDateDesc(me, memberId).stream()
                .map(MealPlanResponse::fromEntity)
                .toList();
    }

    /** For the app: today's plan, or the most recent earlier one. */
    @Transactional(readOnly = true)
    public Optional<MealPlanResponse> currentForMember(Long memberId) {
        return mealPlanRepository
                .findFirstByMemberIdAndPlanDateLessThanEqualOrderByPlanDateDescUpdatedAtDesc(memberId, LocalDate.now())
                .map(MealPlanResponse::fromEntity);
    }

    private void notifyClient(MealPlan plan) {
        Member member = plan.getMember();
        String nutritionistName = MealPlanResponse.fromEntity(plan).nutritionistName();
        String dayLabel = plan.getPlanDate().equals(LocalDate.now()) ? "today" : plan.getPlanDate().format(DAY);
        String title = "Your meal plan from " + nutritionistName;
        String body = "Breakfast, lunch, snacks and dinner for " + dayLabel + " are ready. Open GymSuit to see them.";

        try {
            userRepository.findByMemberId(member.getId()).ifPresent(user -> {
                Map<String, String> data = new HashMap<>();
                data.put("type", PUSH_TYPE);
                data.put("mealPlanId", String.valueOf(plan.getId()));
                data.put("date", plan.getPlanDate().toString());
                pushNotificationService.sendToUser(user.getId(), title, body, data, PUSH_CHANNEL);
            });
        } catch (Exception e) {
            log.warn("Meal plan push failed for member {}: {}", member.getId(), e.getMessage());
        }

        try {
            String email = member.getEmail();
            if (email != null && !email.isBlank()) {
                List<MealPlanEmail.Meal> meals = List.of(
                        new MealPlanEmail.Meal("🍳", "Breakfast", plan.getBreakfast() != null),
                        new MealPlanEmail.Meal("🥗", "Lunch", plan.getLunch() != null),
                        new MealPlanEmail.Meal("🍎", "Evening snack", plan.getEveningSnack() != null),
                        new MealPlanEmail.Meal("🍲", "Dinner", plan.getDinner() != null));
                String dateLabel = plan.getPlanDate().equals(LocalDate.now()) ? "today" : plan.getPlanDate().format(DAY);
                eventPublisher.publishEmail(email, title,
                        MealPlanEmail.html(nutritionistName, member.getFirstName(), dateLabel, meals, androidAppUrl, dietUrl));
            }
        } catch (Exception e) {
            log.warn("Meal plan email failed for member {}: {}", member.getId(), e.getMessage());
        }
    }

    private Long requireNutritionist() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null || principal.getRole() != Role.NUTRITIONIST) {
            throw new AccessDeniedException("Only nutrition expert accounts can do this");
        }
        return principal.getUserId();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

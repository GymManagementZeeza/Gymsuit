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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    private static final long NUTRITIONIST = 50L;
    private static final long MEMBER = 7L;

    @Mock private MealPlanRepository mealPlanRepository;
    @Mock private NutritionistClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityService securityService;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private MealPlanService service;

    private NutritionistClient link;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dietUrl", "https://diet.gymsuit.app");
        ReflectionTestUtils.setField(service, "androidAppUrl",
                "https://play.google.com/store/apps/details?id=ca.zeezaglobal.gymsuitapp");

        User nutritionist = new User();
        nutritionist.setId(NUTRITIONIST);
        nutritionist.setRole(Role.NUTRITIONIST);
        nutritionist.setEmail("asha@example.test");
        nutritionist.setPassword("x");
        nutritionist.setFullName("Asha Menon");
        lenient().when(securityService.currentUser()).thenReturn(new UserPrincipal(nutritionist));

        Member member = new Member();
        member.setId(MEMBER);
        member.setFirstName("Ravi");
        member.setEmail("ravi@example.test");
        link = new NutritionistClient();
        link.setNutritionist(nutritionist);
        link.setMember(member);
        lenient().when(clientRepository.findByNutritionistIdAndMemberId(NUTRITIONIST, MEMBER)).thenReturn(Optional.of(link));
        lenient().when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(inv -> {
            MealPlan p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(1L);
            }
            return p;
        });
    }

    private static MealPlanRequest request(LocalDate date, String breakfast, String lunch, String snack, String dinner) {
        return new MealPlanRequest(date, breakfast, lunch, snack, dinner, "Drink 3 L of water");
    }

    @Test
    void savesPlanAndNotifiesClientByPushAndEmail() {
        User clientLogin = new User();
        clientLogin.setId(70L);
        when(userRepository.findByMemberId(MEMBER)).thenReturn(Optional.of(clientLogin));
        when(mealPlanRepository.findByNutritionistIdAndMemberIdAndPlanDate(NUTRITIONIST, MEMBER, LocalDate.now()))
                .thenReturn(Optional.empty());

        MealPlanResponse saved = service.send(MEMBER,
                request(LocalDate.now(), "  Oats, banana  ", "Rice, dal, salad", " ", "Grilled fish"));

        assertEquals("Oats, banana", saved.breakfast(), "trimmed");
        assertNull(saved.eveningSnack(), "blank meals are stored as empty");
        assertEquals("Asha Menon", saved.nutritionistName());

        ArgumentCaptor<Map<String, String>> data = ArgumentCaptor.forClass(Map.class);
        verify(pushNotificationService).sendToUser(eq(70L), anyString(), anyString(), data.capture(), eq("gymsuit_nutrition"));
        assertEquals("MEAL_PLAN", data.getValue().get("type"));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher).publishEmail(eq("ravi@example.test"), eq("Your meal plan from Asha Menon"), html.capture());
        assertTrue(html.getValue().contains("https://play.google.com/store/apps/details?id=ca.zeezaglobal.gymsuitapp"));
        assertTrue(html.getValue().contains("Hi Ravi"));
    }

    @Test
    void resendingSameDayReplacesThePlan() {
        MealPlan existing = new MealPlan();
        existing.setId(9L);
        existing.setNutritionist(link.getNutritionist());
        existing.setMember(link.getMember());
        existing.setPlanDate(LocalDate.now());
        existing.setBreakfast("old");
        existing.setCreatedAt(Instant.now());
        when(mealPlanRepository.findByNutritionistIdAndMemberIdAndPlanDate(NUTRITIONIST, MEMBER, LocalDate.now()))
                .thenReturn(Optional.of(existing));

        service.send(MEMBER, request(LocalDate.now(), "new", null, null, null));

        ArgumentCaptor<MealPlan> saved = ArgumentCaptor.forClass(MealPlan.class);
        verify(mealPlanRepository).save(saved.capture());
        assertSame(existing, saved.getValue());
        assertEquals("new", existing.getBreakfast());
    }

    @Test
    void rejectsEmptyPlan() {
        assertThrows(BadRequestException.class,
                () -> service.send(MEMBER, request(LocalDate.now(), " ", null, "", null)));
        verify(mealPlanRepository, never()).save(any());
    }

    @Test
    void rejectsClientsThatAreNotLinked() {
        when(clientRepository.findByNutritionistIdAndMemberId(NUTRITIONIST, 999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.send(999L, request(LocalDate.now(), "Oats", null, null, null)));
        verify(eventPublisher, never()).publishEmail(anyString(), anyString(), anyString());
        verify(pushNotificationService, never()).sendToUser(anyLong(), anyString(), anyString(), anyMap(), anyString());
    }

    @Test
    void onlyNutritionistsCanSend() {
        User trainer = new User();
        trainer.setId(3L);
        trainer.setRole(Role.TRAINER);
        trainer.setEmail("t@example.test");
        trainer.setPassword("x");
        when(securityService.currentUser()).thenReturn(new UserPrincipal(trainer));
        assertThrows(AccessDeniedException.class,
                () -> service.send(MEMBER, request(LocalDate.now(), "Oats", null, null, null)));
    }

    @Test
    void emailEscapesNamesAndShowsPlannedMeals() {
        String html = MealPlanEmail.html("Asha <i>M</i>", "Ravi", "today",
                List.of(new MealPlanEmail.Meal("🍳", "Breakfast", true),
                        new MealPlanEmail.Meal("🍲", "Dinner", false)),
                "https://play.google.com/store/apps/details?id=ca.zeezaglobal.gymsuitapp", "https://diet.gymsuit.app");
        assertTrue(html.contains("Asha &lt;i&gt;M&lt;/i&gt;"));
        assertTrue(html.contains("✓ Planned"));
        assertTrue(html.contains("href=\"https://play.google.com/store/apps/details?id=ca.zeezaglobal.gymsuitapp\""));
        assertTrue(!html.matches("(?s).*%\\d+\\$s.*"), "no unfilled placeholders");
    }
}

package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.Challenge;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.repository.ChallengeRepository;
import com.zeezaglobal.gymmanagement.repository.FoodIntakeRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistClientRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistInviteRepository;
import com.zeezaglobal.gymmanagement.repository.StepBadgeRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Self-service account deletion must clear every table with a foreign key to users before deleting the
 * user, and keep challenges the user created (creator cleared) for the other participants.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceDeleteAccountTest {

    @Mock private SecurityService securityService;
    @Mock private UserRepository userRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private FoodIntakeRepository foodIntakeRepository;
    @Mock private StepBadgeRepository stepBadgeRepository;
    @Mock private NutritionistClientRepository nutritionistClientRepository;
    @Mock private NutritionistInviteRepository nutritionistInviteRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.MealPlanRepository mealPlanRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.RefreshTokenRepository refreshTokenRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository dailyHealthRecordRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository workoutRecordRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.ChallengeParticipantRepository challengeParticipantRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.ChallengeDailyProgressRepository challengeDailyProgressRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.UserPointTransactionRepository userPointTransactionRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.HealthDeviceRepository healthDeviceRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.HealthUserSyncStateRepository healthUserSyncStateRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.HealthSourcePreferenceRepository healthSourcePreferenceRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.HealthCanonicalRecordRepository healthCanonicalRecordRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.HealthRawRecordRepository healthRawRecordRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.HealthRecordTombstoneRepository healthRecordTombstoneRepository;
    @Mock private com.zeezaglobal.gymmanagement.repository.OtpCodeRepository otpCodeRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void deletesAllUserOwnedRowsAndKeepsCreatedChallenges() {
        User user = new User();
        user.setId(7L);
        user.setEmail("someone@example.test");
        user.setPassword("x");
        user.setRole(Role.MEMBER);
        when(securityService.currentUser()).thenReturn(new UserPrincipal(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        Challenge created = new Challenge();
        created.setCreatedBy(user);
        when(challengeRepository.findAllByCreatedBy(user)).thenReturn(List.of(created));

        authService.deleteOwnAccount();

        // Previously missed: both tables have a foreign key to users and blocked the delete.
        InOrder order = inOrder(foodIntakeRepository, stepBadgeRepository, userRepository);
        order.verify(foodIntakeRepository).deleteAllByUserId(7L);
        order.verify(stepBadgeRepository).deleteAllByUserId(7L);
        order.verify(userRepository).delete(user);

        verify(nutritionistClientRepository).deleteAllByNutritionistId(7L);
        verify(nutritionistInviteRepository).deleteAllByNutritionistId(7L);
        verify(mealPlanRepository).deleteAllByNutritionistId(7L);
        assertNull(created.getCreatedBy(), "creator is cleared so the challenge survives for other participants");
    }
}

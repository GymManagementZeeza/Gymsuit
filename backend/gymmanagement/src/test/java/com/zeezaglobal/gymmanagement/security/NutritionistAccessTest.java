package com.zeezaglobal.gymmanagement.security;

import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistClientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/**
 * A standalone nutritionist (no gym) may read only members who accepted their invite,
 * never write member records, and never browse any gym's directory.
 */
@ExtendWith(MockitoExtension.class)
class NutritionistAccessTest {

    private static final long NUTRITIONIST_USER_ID = 99L;
    private static final long GYM_ID = 1L;
    private static final long LINKED_MEMBER_ID = 10L;
    private static final long OTHER_MEMBER_ID = 11L;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NutritionistClientRepository nutritionistClientRepository;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void signInAsStandaloneNutritionist() {
        User user = new User();
        user.setId(NUTRITIONIST_USER_ID);
        user.setEmail("nutrition@example.test");
        user.setPassword("x");
        user.setRole(Role.NUTRITIONIST);
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(memberRepository.findByIdAndGymId(LINKED_MEMBER_ID, GYM_ID)).thenReturn(Optional.of(new Member()));
        lenient().when(memberRepository.findByIdAndGymId(OTHER_MEMBER_ID, GYM_ID)).thenReturn(Optional.of(new Member()));
        lenient().when(nutritionistClientRepository.existsByNutritionistIdAndMemberId(NUTRITIONIST_USER_ID, LINKED_MEMBER_ID))
                .thenReturn(true);
        lenient().when(nutritionistClientRepository.existsByNutritionistIdAndMemberId(NUTRITIONIST_USER_ID, OTHER_MEMBER_ID))
                .thenReturn(false);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void readsOnlyLinkedClients() {
        assertTrue(securityService.canReadMemberNutrition(GYM_ID, LINKED_MEMBER_ID));
        assertFalse(securityService.canReadMemberNutrition(GYM_ID, OTHER_MEMBER_ID));
    }

    @Test
    void cannotBrowseAnyGymDirectory() {
        assertFalse(securityService.canViewMemberDirectory(GYM_ID));
    }

    @Test
    void cannotPassWriteGuardedChecks() {
        // canViewMember guards weight/goal/trainer edits — a nutritionist must not pass it, even for a linked client.
        assertFalse(securityService.canViewMember(GYM_ID, LINKED_MEMBER_ID));
        assertFalse(securityService.canManageCheckIn(GYM_ID, LINKED_MEMBER_ID));
    }
}

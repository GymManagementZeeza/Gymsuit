package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.NutritionistInviteAcceptRequest;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.NutritionistClient;
import com.zeezaglobal.gymmanagement.entity.NutritionistInvite;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistClientRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistInviteRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionistInviteAcceptTest {

    private static final String EMAIL = "client@example.test";

    @Mock private UserRepository userRepository;
    @Mock private NutritionistClientRepository clientRepository;
    @Mock private NutritionistInviteRepository inviteRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private IndependentGymService independentGymService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private NutritionistService service;

    private NutritionistInvite invite;

    @BeforeEach
    void pendingInvite() {
        User nutritionist = new User();
        nutritionist.setId(50L);
        nutritionist.setRole(Role.NUTRITIONIST);
        nutritionist.setFullName("Asha Menon");
        invite = new NutritionistInvite();
        invite.setNutritionist(nutritionist);
        invite.setEmail(EMAIL);
        invite.setToken("tok");
        invite.setStatus(TrainerInviteStatus.PENDING);
        invite.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(inviteRepository.findByToken("tok")).thenReturn(Optional.of(invite));
    }

    @Test
    void newEmailRegistersMemberInIndependentGymAndLinks() {
        Gym independent = new Gym();
        independent.setId(99L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(independentGymService.get()).thenReturn(independent);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(7L);
            return m;
        });

        String name = service.accept("tok", new NutritionistInviteAcceptRequest("Ravi", "Kumar", "9999999999"));

        assertEquals("Asha Menon", name);
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(user.capture());
        assertEquals(Role.MEMBER, user.getValue().getRole());
        assertEquals(EMAIL, user.getValue().getEmail());
        assertSame(independent, user.getValue().getMember().getGym());
        verify(clientRepository).save(any(NutritionistClient.class));
        assertEquals(TrainerInviteStatus.ACCEPTED, invite.getStatus());
    }

    @Test
    void newEmailWithoutDetailsIsRejected() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.accept("tok", null));
        verify(memberRepository, never()).save(any());
        assertEquals(TrainerInviteStatus.PENDING, invite.getStatus());
    }

    @Test
    void existingAppAccountIsLinkedWithoutCreatingAnything() {
        Member member = new Member();
        member.setId(8L);
        User existing = new User();
        existing.setEmail(EMAIL);
        existing.setRole(Role.MEMBER);
        existing.setMember(member);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

        service.accept("tok", null);

        verify(memberRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(clientRepository).save(any(NutritionistClient.class));
    }

    @Test
    void staffEmailCannotBeLinked() {
        User owner = new User();
        owner.setEmail(EMAIL);
        owner.setRole(Role.OWNER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        assertThrows(BadRequestException.class, () -> service.accept("tok", null));
        verify(clientRepository, never()).save(any());
    }
}

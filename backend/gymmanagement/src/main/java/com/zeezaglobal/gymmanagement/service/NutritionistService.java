package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistClientResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteAcceptRequest;
import com.zeezaglobal.gymmanagement.dto.NutritionistInvitePreviewResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteRequest;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistRegisterRequest;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.NutritionistClient;
import com.zeezaglobal.gymmanagement.entity.NutritionistInvite;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistClientRepository;
import com.zeezaglobal.gymmanagement.repository.NutritionistInviteRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Standalone nutrition experts: they sign up themselves (no gym), invite clients by email, and get
 * read-only access to the nutrition data of the members who accept.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionistService {

    private static final long INVITE_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final NutritionistClientRepository clientRepository;
    private final NutritionistInviteRepository inviteRepository;
    private final OtpService otpService;
    private final AuthService authService;
    private final SecurityService securityService;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final IndependentGymService independentGymService;
    private final com.zeezaglobal.gymmanagement.repository.MealPlanRepository mealPlanRepository;
    private final com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository dailyHealthRecordRepository;

    @Value("${app.diet-url:https://diet.gymsuit.app}")
    private String dietUrl;

    // ---- Sign-up --------------------------------------------------------------------------------------------

    /** Step 1 of sign-up: email a code to an address that has no GymSuit account yet. */
    public void sendSignupCode(String rawEmail) {
        String email = normalize(rawEmail);
        ensureEmailAvailable(email);
        otpService.generate(email);
    }

    /** Step 2 of sign-up: verify the code and create the gym-less NUTRITIONIST account. */
    @Transactional
    public LoginResponse register(NutritionistRegisterRequest request) {
        String email = normalize(request.email());
        otpService.verify(email, request.otp().trim());
        ensureEmailAvailable(email);

        User user = new User();
        user.setEmail(email);
        // Signs in via OTP/Google only; the schema still needs a password, so store unusable filler.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.NUTRITIONIST);
        user.setFullName(request.firstName().trim() + " " + request.lastName().trim());
        user.setEnabled(true);
        user = userRepository.save(user);
        return authService.issueSession(new UserPrincipal(user));
    }

    // ---- Clients --------------------------------------------------------------------------------------------

    public List<NutritionistClientResponse> listClients() {
        Long me = requireNutritionist();
        return clientRepository.findAllByNutritionistIdOrderByLinkedAtDesc(me).stream()
                .map(link -> NutritionistClientResponse.fromEntity(link, latestSyncedWeightKg(link.getMember().getId())))
                .toList();
    }

    /** The most recent weight the client's app synced, so the roster isn't stale when the profile weight is unset. */
    private Double latestSyncedWeightKg(Long memberId) {
        return userRepository.findByMemberId(memberId)
                .flatMap(u -> dailyHealthRecordRepository.findTopByUserIdAndWeightKgIsNotNullOrderByRecordDateDesc(u.getId()))
                .map(com.zeezaglobal.gymmanagement.entity.DailyHealthRecord::getWeightKg)
                .orElse(null);
    }

    @Transactional
    public void removeClient(Long memberId) {
        Long me = requireNutritionist();
        NutritionistClient link = clientRepository.findByNutritionistIdAndMemberId(me, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        clientRepository.delete(link);
        mealPlanRepository.deleteAllByNutritionistIdAndMemberId(me, memberId);
    }

    // ---- Invites --------------------------------------------------------------------------------------------

    public List<NutritionistInviteResponse> listInvites() {
        Long me = requireNutritionist();
        return inviteRepository.findAllByNutritionistIdOrderByCreatedAtDesc(me).stream()
                .map(NutritionistInviteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public NutritionistInviteResponse invite(NutritionistInviteRequest request) {
        Long me = requireNutritionist();
        User nutritionist = userRepository.findById(me)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        String email = normalize(request.email());
        if (email.equals(nutritionist.getEmail())) {
            throw new BadRequestException("You can't invite yourself");
        }

        userRepository.findByEmail(email)
                .filter(u -> u.getMember() != null)
                .filter(u -> clientRepository.existsByNutritionistIdAndMemberId(me, u.getMember().getId()))
                .ifPresent(u -> {
                    throw new ConflictException("This person is already one of your clients");
                });

        // Reuse an existing pending invite for this email so the earlier link keeps working.
        NutritionistInvite invite = inviteRepository
                .findByNutritionistIdAndEmailAndStatus(me, email, TrainerInviteStatus.PENDING)
                .orElseGet(() -> {
                    NutritionistInvite created = new NutritionistInvite();
                    created.setNutritionist(nutritionist);
                    created.setEmail(email);
                    created.setFirstName(blankToNull(request.firstName()));
                    created.setLastName(blankToNull(request.lastName()));
                    created.setToken(UUID.randomUUID().toString());
                    created.setStatus(TrainerInviteStatus.PENDING);
                    created.setCreatedAt(Instant.now());
                    created.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
                    return inviteRepository.save(created);
                });

        String name = displayName(nutritionist);
        String link = dietUrl.replaceAll("/+$", "") + "/diet/invite?token=" + invite.getToken();
        try {
            eventPublisher.publishEmail(email,
                    name + " invited you to share your nutrition on GymSuit",
                    NutritionistInviteEmail.html(name, email, link, dietUrl));
        } catch (AmqpException e) {
            log.error("Could not queue nutritionist invite email to {}: {}", email, e.getMessage(), e);
            throw new BadRequestException("Could not send the invite right now, please try again");
        }
        return NutritionistInviteResponse.fromEntity(invite);
    }

    @Transactional
    public void revokeInvite(Long inviteId) {
        Long me = requireNutritionist();
        NutritionistInvite invite = inviteRepository.findByIdAndNutritionistId(inviteId, me)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getStatus() == TrainerInviteStatus.PENDING) {
            invite.setStatus(TrainerInviteStatus.REVOKED);
            inviteRepository.save(invite);
        }
    }

    /** Public: what the accept page shows for a token. */
    public NutritionistInvitePreviewResponse preview(String token) {
        NutritionistInvite invite = getInviteOrThrow(token);
        User existing = userRepository.findByEmail(invite.getEmail()).orElse(null);
        boolean hasAccount = existing != null && existing.getMember() != null;
        boolean staffAccount = existing != null && existing.getMember() == null;
        return new NutritionistInvitePreviewResponse(
                displayName(invite.getNutritionist()),
                invite.getEmail(),
                invite.getStatus(),
                invite.getExpiresAt(),
                hasAccount,
                staffAccount);
    }

    /**
     * Public: accept an invite. The emailed token proves the invitee controls the address, so it is the only
     * credential needed. If that email already has a GymSuit member account it is linked as-is; if it has no
     * account at all, {@code signup} (name + phone) creates one in the Independent gym first. The new member
     * then signs in to the GymSuit app with that email and a code.
     */
    @Transactional
    public String accept(String token, NutritionistInviteAcceptRequest signup) {
        NutritionistInvite invite = getInviteOrThrow(token);
        if (invite.getStatus() != TrainerInviteStatus.PENDING) {
            throw new BadRequestException("This invitation is no longer valid");
        }
        if (Instant.now().isAfter(invite.getExpiresAt())) {
            invite.setStatus(TrainerInviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BadRequestException("This invitation has expired — ask your nutritionist to send a new one");
        }

        User client = userRepository.findByEmail(invite.getEmail()).orElse(null);
        Member member;
        if (client == null) {
            member = registerInvitedClient(invite.getEmail(), signup);
        } else if (client.getMember() != null) {
            member = client.getMember();
        } else {
            // A gym owner/trainer/manager or another nutritionist — no member profile holds their nutrition data.
            throw new BadRequestException(
                    invite.getEmail() + " is a staff account, not a GymSuit app account, so it can't be linked. "
                            + "Ask your nutritionist to invite the email you use in the GymSuit app.");
        }

        Long nutritionistId = invite.getNutritionist().getId();
        if (!clientRepository.existsByNutritionistIdAndMemberId(nutritionistId, member.getId())) {
            NutritionistClient link = new NutritionistClient();
            link.setNutritionist(invite.getNutritionist());
            link.setMember(member);
            link.setLinkedAt(Instant.now());
            clientRepository.save(link);
        }
        invite.setStatus(TrainerInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);
        return displayName(invite.getNutritionist());
    }

    /** Creates a GymSuit member account (member + MEMBER user) for an invited email with no account yet. */
    private Member registerInvitedClient(String email, NutritionistInviteAcceptRequest signup) {
        if (signup == null || isBlank(signup.firstName()) || isBlank(signup.lastName()) || isBlank(signup.phone())) {
            throw new BadRequestException("First name, last name and phone are required to create your account");
        }
        Member member = new Member();
        member.setGym(independentGymService.get());
        member.setFirstName(signup.firstName().trim());
        member.setLastName(signup.lastName().trim());
        member.setEmail(email);
        member.setPhone(signup.phone().trim());
        member.setJoinDate(LocalDate.now());
        member.setWaiverAccepted(true);
        member.setJoiningFeePaid(true);
        member = memberRepository.save(member);

        // Signs in to the app via email code; the schema still needs a password, so store unusable filler.
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(Role.MEMBER);
        user.setGym(member.getGym());
        user.setMember(member);
        user.setEnabled(true);
        userRepository.save(user);
        return member;
    }

    // ---- Helpers --------------------------------------------------------------------------------------------

    private Long requireNutritionist() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null || principal.getRole() != Role.NUTRITIONIST) {
            throw new AccessDeniedException("Only nutrition expert accounts can do this");
        }
        return principal.getUserId();
    }

    private NutritionistInvite getInviteOrThrow(String token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("An account with " + email + " already exists. Sign in instead.");
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String displayName(User user) {
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}

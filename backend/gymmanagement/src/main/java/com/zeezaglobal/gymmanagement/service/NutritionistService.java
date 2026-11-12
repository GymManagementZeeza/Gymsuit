package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistClientResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistInvitePreviewResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteRequest;
import com.zeezaglobal.gymmanagement.dto.NutritionistInviteResponse;
import com.zeezaglobal.gymmanagement.dto.NutritionistRegisterRequest;
import com.zeezaglobal.gymmanagement.entity.NutritionistClient;
import com.zeezaglobal.gymmanagement.entity.NutritionistInvite;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
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
                .map(NutritionistClientResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void removeClient(Long memberId) {
        Long me = requireNutritionist();
        NutritionistClient link = clientRepository.findByNutritionistIdAndMemberId(me, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        clientRepository.delete(link);
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
                    inviteEmailHtml(name, email, link));
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
        boolean hasAccount = userRepository.findByEmail(invite.getEmail())
                .map(u -> u.getMember() != null)
                .orElse(false);
        return new NutritionistInvitePreviewResponse(
                displayName(invite.getNutritionist()),
                invite.getEmail(),
                invite.getStatus(),
                invite.getExpiresAt(),
                hasAccount);
    }

    /**
     * Public: accept an invite. The emailed token proves the invitee controls the address. Their nutrition data
     * lives on their GymSuit member account, so one must already exist for this email.
     */
    @Transactional
    public String accept(String token) {
        NutritionistInvite invite = getInviteOrThrow(token);
        if (invite.getStatus() != TrainerInviteStatus.PENDING) {
            throw new BadRequestException("This invitation is no longer valid");
        }
        if (Instant.now().isAfter(invite.getExpiresAt())) {
            invite.setStatus(TrainerInviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BadRequestException("This invitation has expired — ask your nutritionist to send a new one");
        }

        User client = userRepository.findByEmail(invite.getEmail())
                .filter(u -> u.getMember() != null)
                .orElseThrow(() -> new BadRequestException(
                        "There's no GymSuit app account for " + invite.getEmail()
                                + " yet. Install the GymSuit app, sign up with this email, then open this link again."));

        Long nutritionistId = invite.getNutritionist().getId();
        Long memberId = client.getMember().getId();
        if (!clientRepository.existsByNutritionistIdAndMemberId(nutritionistId, memberId)) {
            NutritionistClient link = new NutritionistClient();
            link.setNutritionist(invite.getNutritionist());
            link.setMember(client.getMember());
            link.setLinkedAt(Instant.now());
            clientRepository.save(link);
        }
        invite.setStatus(TrainerInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);
        return displayName(invite.getNutritionist());
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String inviteEmailHtml(String nutritionistName, String email, String link) {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>" + escape(nutritionistName) + " invited you on GymSuit</h2>"
                + "<p>" + escape(nutritionistName) + " is a nutrition expert and would like to follow your food log, "
                + "macros and weight from the GymSuit app to guide your diet.</p>"
                + "<p>They can only view this — they can't change anything in your account.</p>"
                + "<p><a href=\"" + link + "\" style=\"display:inline-block;background:#2563eb;color:#fff;"
                + "padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;\">"
                + "Accept invitation</a></p>"
                + "<p style=\"font-size:12px;color:#777;\">Don't have the GymSuit app yet? Install it and sign up with "
                + escape(email) + " first, then open the link.</p>"
                + "<p style=\"font-size:12px;color:#777;\">Or open this link: " + link + "</p>"
                + "<p style=\"font-size:12px;color:#777;\">This invitation expires in 7 days. "
                + "If you weren't expecting it, you can ignore this email.</p>"
                + "</div>";
    }
}

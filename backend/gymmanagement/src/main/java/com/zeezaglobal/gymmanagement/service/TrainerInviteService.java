package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MemberRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerInviteAcceptRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerInvitePreviewResponse;
import com.zeezaglobal.gymmanagement.dto.TrainerInviteRequest;
import com.zeezaglobal.gymmanagement.dto.TrainerInviteResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.TrainerInvite;
import com.zeezaglobal.gymmanagement.entity.TrainerInviteStatus;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerInviteRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Email invitations from a trainer to a client. This is the only way a trainer
 * adds a client: trainers never see the gym's full member directory or the
 * unassigned-member pool. A trainer invites by email, the invitee opens the
 * link, and accepting assigns them to that trainer — creating their member
 * record first if they do not have one at this gym yet.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerInviteService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final long INVITE_TTL_DAYS = 7;

    private final TrainerInviteRepository inviteRepository;
    private final TrainerRepository trainerRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MemberService memberService;
    private final GymService gymService;
    private final SecurityService securityService;
    private final EventPublisher eventPublisher;

    @Value("${app.frontend-url:https://gymsuit.app}")
    private String frontendUrl;

    /** A trainer may invite for themselves; gym staff (owner/manager/admin) for any trainer in the gym. */
    private void requireInvitePermission(Long gymId, Long trainerId) {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        boolean isStaff = principal.getRole() == Role.ADMIN
                || principal.getRole() == Role.OWNER
                || principal.getRole() == Role.MANAGER;
        boolean isSelfTrainer = principal.getRole() == Role.TRAINER
                && trainerId.equals(principal.getTrainerId());
        if (!isStaff && !isSelfTrainer) {
            throw new AccessDeniedException("Only the trainer or gym staff can send this invite");
        }
        if (!isStaff && !gymId.equals(principal.getGymId())) {
            throw new AccessDeniedException("Trainer does not belong to this gym");
        }
        if (isStaff && principal.getRole() != Role.ADMIN && !gymId.equals(principal.getGymId())) {
            throw new AccessDeniedException("Trainer does not belong to this gym");
        }
    }

    private Trainer getTrainerOrThrow(Long gymId, Long trainerId) {
        return trainerRepository.findByIdAndGymId(trainerId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found"));
    }

    @Transactional
    public TrainerInviteResponse invite(Long gymId, Long trainerId, TrainerInviteRequest request) {
        requireInvitePermission(gymId, trainerId);
        Trainer trainer = getTrainerOrThrow(gymId, trainerId);
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid email address");
        }

        // Already this trainer's client? No invite needed.
        memberRepository.findByGymIdAndEmail(gymId, email)
                .filter(m -> m.getTrainer() != null && m.getTrainer().getId().equals(trainerId))
                .ifPresent(m -> {
                    throw new ConflictException("This person is already one of your clients");
                });

        // Reuse the existing pending invite for this trainer+email instead of
        // creating duplicates — the link stays valid either way.
        TrainerInvite invite = inviteRepository
                .findByGymIdAndTrainerIdAndEmailAndStatus(gymId, trainerId, email, TrainerInviteStatus.PENDING)
                .orElseGet(() -> {
                    TrainerInvite created = new TrainerInvite();
                    created.setGym(trainer.getGym());
                    created.setTrainer(trainer);
                    created.setEmail(email);
                    created.setFirstName(blankToNull(request.firstName()));
                    created.setLastName(blankToNull(request.lastName()));
                    created.setToken(UUID.randomUUID().toString());
                    created.setStatus(TrainerInviteStatus.PENDING);
                    created.setCreatedAt(Instant.now());
                    created.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
                    return inviteRepository.save(created);
                });

        String trainerName = trainer.getFirstName() + " " + trainer.getLastName();
        String link = frontendUrl.replaceAll("/+$", "") + "/invite?token=" + invite.getToken();
        try {
            eventPublisher.publishEmail(email,
                    trainerName + " invited you to train with them on GymSuit",
                    inviteEmailHtml(trainer.getGym().getName(), trainerName, link));
        } catch (AmqpException e) {
            log.error("Could not queue trainer invite email to {}", email, e.getMessage(), e);
            throw new BadRequestException("Could not send the invite right now, please try again");
        }
        return TrainerInviteResponse.fromEntity(invite);
    }

    public List<TrainerInviteResponse> listInvites(Long gymId, Long trainerId) {
        requireInvitePermission(gymId, trainerId);
        getTrainerOrThrow(gymId, trainerId);
        return inviteRepository.findAllByGymIdAndTrainerIdOrderByCreatedAtDesc(gymId, trainerId)
                .stream()
                .map(TrainerInviteResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void revoke(Long gymId, Long trainerId, Long inviteId) {
        requireInvitePermission(gymId, trainerId);
        TrainerInvite invite = inviteRepository.findByIdAndGymIdAndTrainerId(inviteId, gymId, trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
        if (invite.getStatus() == TrainerInviteStatus.PENDING) {
            invite.setStatus(TrainerInviteStatus.REVOKED);
            inviteRepository.save(invite);
        }
    }

    public TrainerInvitePreviewResponse preview(String token) {
        TrainerInvite invite = getInviteOrThrow(token);
        boolean existingMember = memberRepository
                .findByGymIdAndEmail(invite.getGym().getId(), invite.getEmail())
                .isPresent();
        return TrainerInvitePreviewResponse.fromEntity(invite,
                invite.getGym().getName(),
                invite.getTrainer().getFirstName() + " " + invite.getTrainer().getLastName(),
                existingMember);
    }

    @Transactional
    public String accept(String token, TrainerInviteAcceptRequest request) {
        TrainerInvite invite = getInviteOrThrow(token);
        if (invite.getStatus() != TrainerInviteStatus.PENDING) {
            throw new BadRequestException("This invitation is no longer valid");
        }
        if (Instant.now().isAfter(invite.getExpiresAt())) {
            invite.setStatus(TrainerInviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BadRequestException("This invitation has expired — ask your trainer to send a new one");
        }

        Long gymId = invite.getGym().getId();
        Long trainerId = invite.getTrainer().getId();
        Member member = memberRepository.findByGymIdAndEmail(gymId, invite.getEmail()).orElse(null);
        if (member == null) {
            if (request == null
                    || isBlank(request.firstName()) || isBlank(request.lastName()) || isBlank(request.phone())) {
                throw new BadRequestException("First name, last name and phone are required to join");
            }
            if (userRepository.findByEmail(invite.getEmail()).isPresent()) {
                throw new ConflictException(
                        "An account with this email already exists — sign in, then ask your trainer to resend the invite");
            }
            MemberRequest create = new MemberRequest(
                    request.firstName().trim(),
                    request.lastName().trim(),
                    invite.getEmail(),
                    request.phone().trim(),
                    request.dateOfBirth(),
                    request.gender(),
                    request.heightCm(),
                    request.weightKg(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    request.waiverAccepted() != null && request.waiverAccepted(),
                    LocalDate.now());
            memberService.create(gymId, create);
            member = memberRepository.findByGymIdAndEmail(gymId, invite.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Member could not be created"));
        }

        memberService.assignTrainer(gymId, member.getId(),
                new com.zeezaglobal.gymmanagement.dto.MemberTrainerRequest(trainerId));
        invite.setStatus(TrainerInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);
        return invite.getTrainer().getFirstName() + " " + invite.getTrainer().getLastName();
    }

    private TrainerInvite getInviteOrThrow(String token) {
        return inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String inviteEmailHtml(String gymName, String trainerName, String link) {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>You've been invited to train with " + escape(trainerName) + "</h2>"
                + "<p>" + escape(trainerName) + " from " + escape(gymName)
                + " invited you to become their client on GymSuit.</p>"
                + "<p>Accepting links you to this trainer — you won't see any other trainer's clients, "
                + "and they won't see yours.</p>"
                + "<p><a href=\"" + link + "\" style=\"display:inline-block;background:#2563eb;color:#fff;"
                + "padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;\">"
                + "Accept invitation</a></p>"
                + "<p style=\"font-size:12px;color:#777;\">Or open this link: " + link + "</p>"
                + "<p style=\"font-size:12px;color:#777;\">This invitation expires in 7 days. "
                + "If you weren't expecting it, you can ignore this email.</p>"
                + "</div>";
    }
}

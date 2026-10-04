package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.ChallengeCreateRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeDetailDto;
import com.zeezaglobal.gymmanagement.dto.ChallengeInviteRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeJoinRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeProgressRequest;
import com.zeezaglobal.gymmanagement.dto.ChallengeSummaryDto;
import com.zeezaglobal.gymmanagement.dto.LeaderboardEntryDto;
import com.zeezaglobal.gymmanagement.entity.Challenge;
import com.zeezaglobal.gymmanagement.entity.ChallengeInvite;
import com.zeezaglobal.gymmanagement.entity.ChallengeInviteStatus;
import com.zeezaglobal.gymmanagement.entity.ChallengeMetric;
import com.zeezaglobal.gymmanagement.entity.ChallengeParticipant;
import com.zeezaglobal.gymmanagement.entity.ChallengeStatus;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
import com.zeezaglobal.gymmanagement.repository.ChallengeInviteRepository;
import com.zeezaglobal.gymmanagement.repository.ChallengeParticipantRepository;
import com.zeezaglobal.gymmanagement.repository.ChallengeRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeService {

    // Ambiguous characters (0/O, 1/I/L) excluded so codes are easy to read aloud.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final long INVITE_TTL_DAYS = 7;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final ChallengeInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ChallengeDetailDto createChallenge(ChallengeCreateRequest request, UserPrincipal principal) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Challenge name is required");
        }
        if (request.metricType() == null) {
            throw new BadRequestException("Challenge metric type is required");
        }
        if (request.startDate() == null || request.endDate() == null) {
            throw new BadRequestException("Challenge start and end dates are required");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }
        // Allow the start date to be up to 1 day in the past to account for timezone differences
        // between the mobile client and the server.
        if (request.startDate().isBefore(LocalDate.now().minusDays(1))) {
            throw new BadRequestException("Start date cannot be in the past");
        }

        User creator = getUser(principal);

        Challenge challenge = new Challenge();
        challenge.setName(request.name().trim());
        challenge.setDescription(request.description());
        challenge.setMetricType(request.metricType());
        challenge.setStartDate(request.startDate());
        challenge.setEndDate(request.endDate());
        challenge.setInviteCode(generateUniqueInviteCode());
        challenge.setCreatedBy(creator);
        challenge.setCreatedAt(Instant.now());
        challenge = challengeRepository.save(challenge);

        // The creator automatically joins their own challenge.
        joinChallengeInternal(challenge, creator);

        log.info("Challenge {} created by user {}", challenge.getId(), creator.getId());
        return toDetailDto(challenge, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<ChallengeSummaryDto> listMyChallenges(UserPrincipal principal) {
        Long userId = principal.getUserId();
        return participantRepository.findByUserId(userId).stream()
                .map(participant -> toSummaryDto(participant.getChallenge(), userId))
                .sorted(Comparator.comparing(ChallengeSummaryDto::startDate).reversed()
                        .thenComparing(ChallengeSummaryDto::id))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChallengeDetailDto getChallengeDetail(Long challengeId, UserPrincipal principal) {
        Challenge challenge = getChallenge(challengeId);
        Long userId = principal.getUserId();
        requireParticipant(challengeId, userId);
        return toDetailDto(challenge, userId);
    }

    @Transactional
    public ChallengeDetailDto joinByCode(ChallengeJoinRequest request, UserPrincipal principal) {
        String code = request.code() == null ? "" : request.code().trim();
        if (code.isEmpty()) {
            throw new BadRequestException("Invite code is required");
        }
        Challenge challenge = challengeRepository.findByInviteCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("No challenge found for this invite code"));
        if (deriveStatus(challenge) == ChallengeStatus.ENDED) {
            throw new BadRequestException("This challenge has already ended");
        }

        User user = getUser(principal);
        // Idempotent: joining twice keeps the existing participant row.
        joinChallengeInternal(challenge, user);

        // If this user was email-invited, mark that invite as accepted.
        inviteRepository.findByChallengeIdAndEmailAndStatus(
                        challenge.getId(), user.getEmail(), ChallengeInviteStatus.PENDING)
                .ifPresent(invite -> {
                    invite.setStatus(ChallengeInviteStatus.ACCEPTED);
                    inviteRepository.save(invite);
                });

        log.info("User {} joined challenge {}", user.getId(), challenge.getId());
        return toDetailDto(challenge, user.getId());
    }

    @Transactional
    public String inviteParticipant(Long challengeId, ChallengeInviteRequest request, UserPrincipal principal) {
        Challenge challenge = getChallenge(challengeId);
        Long userId = principal.getUserId();
        requireParticipant(challengeId, userId);

        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid email address");
        }

        // Reuse the existing pending invite for this email+challenge instead of
        // creating duplicates (the code stays valid either way).
        ChallengeInvite invite = inviteRepository
                .findByChallengeIdAndEmailAndStatus(challengeId, email, ChallengeInviteStatus.PENDING)
                .orElseGet(() -> {
                    ChallengeInvite created = new ChallengeInvite();
                    created.setChallenge(challenge);
                    created.setEmail(email);
                    created.setToken(UUID.randomUUID().toString());
                    created.setStatus(ChallengeInviteStatus.PENDING);
                    created.setCreatedAt(Instant.now());
                    created.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
                    return inviteRepository.save(created);
                });

        String subject = "You're invited: " + challenge.getName() + " on GymSuit";
        try {
            eventPublisher.publishEmail(email, subject, challengeInviteEmailHtml(challenge));
        } catch (AmqpException e) {
            log.error("Could not queue challenge invite email to {}: {}", email, e.getMessage(), e);
            throw new BadRequestException("Could not send the invite right now, please try again");
        }

        log.info("Challenge {} invite queued for {} (inviter {})", challengeId, email, userId);
        return "Invite sent to " + email;
    }

    @Transactional
    public String submitProgress(Long challengeId, ChallengeProgressRequest request, UserPrincipal principal) {
        Long userId = principal.getUserId();
        ChallengeParticipant participant = participantRepository
                .findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a participant of this challenge"));

        // Absolute totals over the challenge date range, computed by the client.
        // A null field means "not reported" and leaves the stored total unchanged.
        if (request.steps() != null) {
            participant.setStepsTotal(request.steps());
        }
        if (request.workouts() != null) {
            participant.setWorkoutsTotal(request.workouts());
        }
        if (request.calories() != null) {
            participant.setCaloriesTotal(request.calories());
        }
        if (request.distanceKm() != null) {
            participant.setDistanceKmTotal(request.distanceKm());
        }
        participant.setLastProgressAt(Instant.now());
        participantRepository.save(participant);

        return "Progress updated";
    }

    @Transactional
    public String leaveChallenge(Long challengeId, UserPrincipal principal) {
        // 404 for an unknown challenge; leaving is otherwise idempotent.
        getChallenge(challengeId);
        Long userId = principal.getUserId();
        participantRepository.findByChallengeIdAndUserId(challengeId, userId)
                .ifPresent(participantRepository::delete);
        log.info("User {} left challenge {}", userId, challengeId);
        return "You have left the challenge";
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Challenge getChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));
    }

    private User getUser(UserPrincipal principal) {
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void requireParticipant(Long challengeId, Long userId) {
        if (participantRepository.findByChallengeIdAndUserId(challengeId, userId).isEmpty()) {
            throw new AccessDeniedException("You are not a participant of this challenge");
        }
    }

    private ChallengeParticipant joinChallengeInternal(Challenge challenge, User user) {
        return participantRepository.findByChallengeIdAndUserId(challenge.getId(), user.getId())
                .orElseGet(() -> {
                    ChallengeParticipant participant = new ChallengeParticipant();
                    participant.setChallenge(challenge);
                    participant.setUser(user);
                    participant.setJoinedAt(Instant.now());
                    return participantRepository.save(participant);
                });
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (challengeRepository.findByInviteCodeIgnoreCase(code).isEmpty()) {
                return code;
            }
        }
        // Practically unreachable; the unique DB constraint is the final guard.
        return UUID.randomUUID().toString().replace("-", "").substring(0, CODE_LENGTH).toUpperCase();
    }

    private ChallengeStatus deriveStatus(Challenge challenge) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(challenge.getStartDate())) {
            return ChallengeStatus.UPCOMING;
        }
        if (today.isAfter(challenge.getEndDate())) {
            return ChallengeStatus.ENDED;
        }
        return ChallengeStatus.ACTIVE;
    }

    private double metricValue(ChallengeParticipant participant, ChallengeMetric metric) {
        return switch (metric) {
            case STEPS -> participant.getStepsTotal();
            case WORKOUTS -> participant.getWorkoutsTotal();
            case CALORIES -> participant.getCaloriesTotal();
            case DISTANCE_KM -> participant.getDistanceKmTotal();
        };
    }

    private List<LeaderboardEntryDto> buildLeaderboard(Challenge challenge,
                                                        List<ChallengeParticipant> participants) {
        List<ChallengeParticipant> sorted = participants.stream()
                .sorted(Comparator
                        .comparingDouble((ChallengeParticipant p) -> metricValue(p, challenge.getMetricType()))
                        .reversed())
                .toList();
        List<LeaderboardEntryDto> entries = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            ChallengeParticipant participant = sorted.get(i);
            entries.add(new LeaderboardEntryDto(
                    participant.getUser().getId(),
                    displayName(participant.getUser()),
                    metricValue(participant, challenge.getMetricType()),
                    i + 1));
        }
        return entries;
    }

    private Integer findMyRank(List<LeaderboardEntryDto> leaderboard, Long userId) {
        return leaderboard.stream()
                .filter(entry -> entry.userId().equals(userId))
                .map(LeaderboardEntryDto::rank)
                .findFirst()
                .orElse(null);
    }

    private ChallengeSummaryDto toSummaryDto(Challenge challenge, Long userId) {
        List<ChallengeParticipant> participants = participantRepository.findByChallengeId(challenge.getId());
        List<LeaderboardEntryDto> leaderboard = buildLeaderboard(challenge, participants);
        return new ChallengeSummaryDto(
                challenge.getId(),
                challenge.getName(),
                challenge.getMetricType(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                deriveStatus(challenge),
                participants.size(),
                findMyRank(leaderboard, userId));
    }

    private ChallengeDetailDto toDetailDto(Challenge challenge, Long userId) {
        List<ChallengeParticipant> participants = participantRepository.findByChallengeId(challenge.getId());
        List<LeaderboardEntryDto> leaderboard = buildLeaderboard(challenge, participants);
        return new ChallengeDetailDto(
                challenge.getId(),
                challenge.getName(),
                challenge.getDescription(),
                challenge.getMetricType(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                deriveStatus(challenge),
                participants.size(),
                findMyRank(leaderboard, userId),
                challenge.getInviteCode(),
                challenge.getCreatedBy().getId().equals(userId),
                leaderboard);
    }

    private String displayName(User user) {
        Member member = user.getMember();
        if (member != null) {
            String first = member.getFirstName() == null ? "" : member.getFirstName().trim();
            String last = member.getLastName() == null ? "" : member.getLastName().trim();
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) {
                return full;
            }
        }
        String email = user.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "Member";
    }

    private String challengeInviteEmailHtml(Challenge challenge) {
        return "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                + "<h2>You've been invited to a fitness challenge!</h2>"
                + "<p>You've been invited to join <strong>" + escapeHtml(challenge.getName())
                + "</strong> on GymSuit.</p>"
                + "<p>Enter this invite code in the app:</p>"
                + "<div style=\"font-size: 36px; font-weight: bold; letter-spacing: 6px; color: #2563eb; margin: 20px 0;\">"
                + challenge.getInviteCode() + "</div>"
                + "<p>Open the GymSuit app, go to the <strong>Challenges</strong> tab, tap <strong>Join</strong> "
                + "and enter the code above.</p>"
                + "<p style=\"font-size: 12px; color: #777;\">Challenge runs "
                + challenge.getStartDate() + " to " + challenge.getEndDate() + ".</p>"
                + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />"
                + "<p style=\"font-size: 12px; color: #777;\">Sent from GymSuit Management Platform</p>"
                + "</div>";
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

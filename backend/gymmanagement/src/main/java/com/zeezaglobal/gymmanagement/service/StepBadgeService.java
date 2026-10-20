package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.StepBadgeDto;
import com.zeezaglobal.gymmanagement.entity.StepBadge;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.repository.StepBadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Awards step milestone badges from reconciled daily step totals and sends
 * the congratulatory email.
 *
 * <p>Detection: {@link HealthSyncService} calls {@link #awardBadgesIfEarned}
 * once per synced day with the reconciled {@code DailyHealthRecord} step
 * total. Detection uses the reconciled total — never a sum of sources — so
 * a phone and a watch both syncing the same day can't inflate the count.
 * The unique (user, threshold, date) constraint plus the
 * {@code existsBy...} check make re-syncs of the same day unable to
 * double-award a badge.
 *
 * <p>Email batching: a sync that creates one or more new badges results in
 * exactly ONE email covering all of them (typically one day's milestones).
 * Email failures are swallowed and logged so a mail outage can never break
 * a health sync.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StepBadgeService {

    /** Milestone thresholds in ascending order with their badge names. */
    public static final LinkedHashMap<Integer, String> MILESTONES = new LinkedHashMap<>();

    static {
        MILESTONES.put(5_000, "Stride Starter");
        MILESTONES.put(10_000, "Step Champion");
        MILESTONES.put(20_000, "Trail Blazer");
    }

    /** Resend content-id of the celebration GIF in the email HTML. */
    private static final String GIF_CID = "badge-celebration";
    /** Classpath resource of the celebration GIF (shipped with the app). */
    private static final String GIF_RESOURCE = "badge-celebration.gif";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    private final StepBadgeRepository badgeRepository;
    private final EmailService emailService;

    /**
     * Awards every milestone badge the given reconciled step count qualifies
     * for on the given walking day. A badge is granted at most once per
     * (user, threshold, date). Returns only the badges newly created by
     * this call.
     */
    @Transactional
    public List<StepBadge> awardBadgesIfEarned(User user, LocalDate date, long steps) {
        List<StepBadge> created = new ArrayList<>();
        for (Map.Entry<Integer, String> milestone : MILESTONES.entrySet()) {
            int threshold = milestone.getKey();
            if (steps < threshold) break; // thresholds are ascending
            if (badgeRepository.existsByUserIdAndThresholdAndBadgeDate(user.getId(), threshold, date)) {
                continue;
            }
            StepBadge badge = new StepBadge();
            badge.setUser(user);
            badge.setThreshold(threshold);
            badge.setBadgeName(milestone.getValue());
            badge.setStepsAchieved(steps);
            badge.setBadgeDate(date);
            badge.setAwardedAt(Instant.now());
            badgeRepository.save(badge);
            created.add(badge);
            log.info("Awarded step badge '{}' ({} steps) to user {} for {}",
                    milestone.getValue(), threshold, user.getId(), date);
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<StepBadgeDto> getBadges(User user) {
        return badgeRepository.findByUserIdOrderByAwardedAtDesc(user.getId())
                .stream().map(this::toDto).toList();
    }

    private StepBadgeDto toDto(StepBadge b) {
        return new StepBadgeDto(b.getId(), b.getThreshold(), b.getBadgeName(),
                b.getStepsAchieved(), b.getBadgeDate(), b.getAwardedAt());
    }

    /**
     * Sends one congratulatory email covering every badge created by a single
     * sync. The celebration GIF is embedded inline via a CID (it animates in
     * clients that render animated GIFs), and the heading gets a CSS keyframe
     * pop for clients that support animations (Apple Mail does). Any failure
     * is logged and swallowed — it must never break a health sync.
     */
    public void sendBadgeEmail(User user, List<StepBadge> newBadges) {
        if (newBadges == null || newBadges.isEmpty()) return;
        try {
            String to = user.getEmail();
            if (to == null || to.isBlank()) {
                log.warn("Skipping badge email for user {}: no email address", user.getId());
                return;
            }
            String subject = newBadges.size() == 1
                    ? "\uD83C\uDFC5 You earned the \"" + newBadges.get(0).getBadgeName() + "\" badge!"
                    : "\uD83C\uDFC5 You earned " + newBadges.size() + " GymSuit step badges!";
            String html = buildBadgeEmailHtml(user, newBadges);
            byte[] gif = loadCelebrationGif();
            if (gif != null) {
                emailService.sendEmailWithInlineImages(to, subject, html,
                        List.of(new EmailService.InlineImage(GIF_CID, "badge-celebration.gif", "image/gif", gif)));
            } else {
                emailService.sendEmail(to, subject, html);
            }
            log.info("Sent step badge email to user {} for {} new badge(s)", user.getId(), newBadges.size());
        } catch (Exception e) {
            log.warn("Step badge email failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private byte[] loadCelebrationGif() {
        try {
            ClassPathResource resource = new ClassPathResource(GIF_RESOURCE);
            if (!resource.exists()) {
                log.warn("Badge celebration GIF not found on classpath: {}", GIF_RESOURCE);
                return null;
            }
            return resource.getContentAsByteArray();
        } catch (IOException e) {
            log.warn("Could not load badge celebration GIF: {}", e.getMessage());
            return null;
        }
    }

    private String buildBadgeEmailHtml(User user, List<StepBadge> badges) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
        sb.append("<style>");
        sb.append("@keyframes badgePop { 0% { transform: scale(0.6); } 50% { transform: scale(1.12); } 100% { transform: scale(1); } }");
        sb.append(".badge-pop { animation: badgePop 0.9s ease-in-out 2; display: inline-block; }");
        sb.append("</style></head>");
        sb.append("<body style=\"margin:0;padding:0;background:#f6f8fc;\">");
        sb.append("<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#202124;padding:24px 16px;\">");

        sb.append("<div style=\"text-align:center;margin-bottom:4px;\">");
        sb.append("<img src=\"cid:").append(GIF_CID)
                .append("\" width=\"500\" height=\"300\" alt=\"Celebration\" ")
                .append("style=\"max-width:100%;height:auto;border-radius:20px;display:block;margin:0 auto;\"/>");
        sb.append("</div>");

        sb.append("<h2 class=\"badge-pop\" style=\"font-size:26px;text-align:center;margin:12px 0 4px;\">")
                .append("\uD83C\uDF89 Congrats, ").append(escape(displayName(user))).append("!</h2>");
        sb.append("<p style=\"font-size:15px;color:#5f6368;text-align:center;margin:0 0 20px;\">You just unlocked ")
                .append(badges.size() == 1 ? "a new step milestone badge" : badges.size() + " new step milestone badges")
                .append(" \u2014 keep walking! \uD83D\uDEB6</p>");

        for (StepBadge b : badges) {
            sb.append("<div style=\"background:#ffffff;border-radius:20px;padding:20px;margin:0 0 12px;box-shadow:0 2px 8px rgba(0,0,0,0.06);text-align:center;\">");
            sb.append("<div style=\"font-size:44px;line-height:1;\">\uD83C\uDFC5</div>");
            sb.append("<div class=\"badge-pop\" style=\"font-size:20px;font-weight:800;margin:8px 0 2px;\">")
                    .append(escape(b.getBadgeName())).append("</div>");
            sb.append("<div style=\"font-size:14px;color:#5f6368;\">")
                    .append(String.format("%,d", b.getThreshold())).append(" steps in a single day</div>");
            sb.append("<div style=\"margin-top:10px;\">");
            sb.append("<span style=\"display:inline-block;background:#eef4ff;color:#1d4ed8;border-radius:999px;padding:6px 14px;font-size:13px;font-weight:700;\">")
                    .append(String.format("%,d", b.getStepsAchieved())).append(" steps</span> ");
            sb.append("<span style=\"display:inline-block;background:#f1f5f9;color:#475569;border-radius:999px;padding:6px 14px;font-size:13px;\">")
                    .append(escape(b.getBadgeDate().format(DATE_FMT))).append("</span>");
            sb.append("</div></div>");
        }

        sb.append("<p style=\"font-size:13px;color:#5f6368;text-align:center;margin:20px 0 0;\">")
                .append("Open the GymSuit app to see all your badges \u2014 the ones you haven\u2019t earned yet are waiting on the Home screen.</p>");
        sb.append("<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:20px 0;\"/>");
        sb.append("<p style=\"font-size:12px;color:#999;text-align:center;\">Sent from GymSuit \uD83D\uDC99 Keep moving!</p>");
        sb.append("</div></body></html>");
        return sb.toString();
    }

    private String displayName(User user) {
        try {
            if (user.getMember() != null && user.getMember().getFirstName() != null
                    && !user.getMember().getFirstName().isBlank()) {
                return user.getMember().getFirstName().trim();
            }
        } catch (Exception e) {
            log.debug("Could not load member name for user {}", user.getId());
        }
        String email = user.getEmail() != null ? user.getEmail() : "there";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

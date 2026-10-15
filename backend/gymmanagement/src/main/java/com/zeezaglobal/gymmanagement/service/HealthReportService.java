package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import javax.imageio.ImageIO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Builds the periodic health report email from data the mobile apps sync
 * ({@link DailyHealthRecord} + {@link WorkoutRecord}) and renders it as a PDF.
 * All values are device-reported; nothing is fabricated here — days without
 * synced data simply show blanks/zeros.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthReportService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    private final DailyHealthRecordRepository dailyRepo;
    private final WorkoutRecordRepository workoutRepo;
    private final EmailService emailService;

    /** Daily step goal the email progress rings measure against (no per-user goal exists yet). */
    private static final long DAILY_STEP_GOAL = 10_000L;
    /** Resend content-id for the hero ring image. */
    private static final String HERO_RING_CID = "steps-ring-hero";

    /** Aggregated report content for one user and one date window. */
    public record ExerciseSummary(String title, int sessions, int sets, long reps, double volumeKg) {
    }

    public record ReportData(
            LocalDate start,
            LocalDate end,
            int totalWorkouts,
            int daysTrained,
            int totalSets,
            long totalReps,
            double totalVolumeKg,
            double workoutCalories,
            int workoutMinutes,
            long totalSteps,
            double avgSteps,
            double totalActiveCalories,
            double totalDistanceKm,
            Double avgSleepMinutes,
            Integer avgRestingHr,
            Double avgWeightKg,
            List<DailyHealthRecord> dailyRecords,
            List<WorkoutRecord> workouts,
            List<ExerciseSummary> exercises) {
    }

    @Transactional(readOnly = true)
    public ReportData buildReport(User user, LocalDate start, LocalDate end) {
        Long userId = user.getId();
        List<DailyHealthRecord> daily = dailyRepo.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, start, end);
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();
        List<WorkoutRecord> workouts = workoutRepo.findByUserIdAndStartTimeBetweenOrderByStartTimeDesc(userId, from, to);

        int totalSets = 0;
        long totalReps = 0;
        double totalVolumeKg = 0;
        double workoutCalories = 0;
        int workoutMinutes = 0;
        TreeSet<LocalDate> trainedDays = new TreeSet<>();
        Map<String, ExerciseSummary> byExercise = new LinkedHashMap<>();
        for (WorkoutRecord w : workouts) {
            totalSets += orZero(w.getSetCount());
            totalReps += orZero(w.getTotalReps());
            totalVolumeKg += orZero(w.getTotalVolumeKg());
            workoutCalories += orZero(w.getCaloriesBurned());
            workoutMinutes += orZero(w.getDurationMinutes());
            if (w.getStartTime() != null) {
                trainedDays.add(w.getStartTime().toLocalDate());
            }
            String title = w.getTitle() != null && !w.getTitle().isBlank() ? w.getTitle().trim() : "Workout";
            ExerciseSummary prev = byExercise.get(title);
            if (prev == null) {
                byExercise.put(title, new ExerciseSummary(title, 1, orZero(w.getSetCount()),
                        orZero(w.getTotalReps()), orZero(w.getTotalVolumeKg())));
            } else {
                byExercise.put(title, new ExerciseSummary(title, prev.sessions() + 1,
                        prev.sets() + orZero(w.getSetCount()), prev.reps() + orZero(w.getTotalReps()),
                        prev.volumeKg() + orZero(w.getTotalVolumeKg())));
            }
        }
        List<ExerciseSummary> exercises = new ArrayList<>(byExercise.values());
        exercises.sort(Comparator.comparingInt(ExerciseSummary::sessions).reversed());

        long totalSteps = 0;
        double totalActiveCalories = 0;
        double totalDistanceKm = 0;
        long sleepSum = 0;
        int sleepCount = 0;
        long restingHrSum = 0;
        int restingHrCount = 0;
        double weightSum = 0;
        int weightCount = 0;
        int daysWithSteps = 0;
        for (DailyHealthRecord r : daily) {
            if (r.getSteps() != null && r.getSteps() > 0) {
                totalSteps += r.getSteps();
                daysWithSteps++;
            }
            totalActiveCalories += orZero(r.getActiveCalories());
            if (r.getDistanceMeters() != null) {
                totalDistanceKm += r.getDistanceMeters() / 1000.0;
            }
            if (r.getSleepDurationMinutes() != null && r.getSleepDurationMinutes() > 0) {
                sleepSum += r.getSleepDurationMinutes();
                sleepCount++;
            }
            if (r.getRestingHeartRateBpm() != null && r.getRestingHeartRateBpm() > 0) {
                restingHrSum += r.getRestingHeartRateBpm();
                restingHrCount++;
            }
            if (r.getWeightKg() != null && r.getWeightKg() > 0) {
                weightSum += r.getWeightKg();
                weightCount++;
            }
        }

        return new ReportData(
                start, end,
                workouts.size(), trainedDays.size(),
                totalSets, totalReps, totalVolumeKg, workoutCalories, workoutMinutes,
                totalSteps, daysWithSteps > 0 ? (double) totalSteps / daysWithSteps : 0,
                totalActiveCalories, totalDistanceKm,
                sleepCount > 0 ? (double) sleepSum / sleepCount : null,
                restingHrCount > 0 ? (int) Math.round((double) restingHrSum / restingHrCount) : null,
                weightCount > 0 ? weightSum / weightCount : null,
                daily, workouts, exercises);
    }

    /** Display name for the email/PDF greeting. */
    @Transactional(readOnly = true)
    public String displayName(User user) {
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

    public String periodLabel(LocalDate start, LocalDate end) {
        if (start.equals(end)) {
            return start.format(PERIOD_FMT);
        }
        return start.format(PERIOD_FMT) + " - " + end.format(PERIOD_FMT);
    }

    // ------------------------------------------------------------------
    // Email — colorful Fitbit-style report (table layout + inline styles
    // so it renders in every mail client; step rings are inline PNGs via
    // Resend content-ids — CSS conic-gradient / SVG arcs are stripped by
    // Gmail, so raster rings are the only arcs that render there).
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public void sendReportEmail(User user, ReportData data, ReportData previous) {
        String name = displayName(user);
        String period = periodLabel(data.start(), data.end());
        String subject = "Your GymSuit health report — " + period;
        String html = buildEmailHtml(name, period, data, previous);
        String filename = "GymSuit-Health-Report-" + data.end() + ".pdf";
        try {
            byte[] pdf = renderPdf(name, period, data);
            Map<String, byte[]> inlineImages = stepRingImages(data);
            emailService.sendEmailWithInlineImagesAndAttachment(
                    user.getEmail(), subject, html, inlineImages, filename, pdf, "application/pdf");
            log.info("Sent health report to user {} ({} bytes pdf)", user.getId(), pdf.length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render health report PDF for user " + user.getId(), e);
        }
    }

    private String buildEmailHtml(String name, String period, ReportData d, ReportData p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;color:#202124;\">");

        // Header
        sb.append("<h2 style=\"font-size:24px;margin:0 0 4px;\">Hi, ").append(escape(name)).append("! 👋</h2>");
        sb.append("<p style=\"font-size:14px;color:#5f6368;margin:0 0 16px;\">Here are your stats for ")
                .append(escape(period)).append("</p>");

        // Day strip
        sb.append(dayStrip(d));

        // Hero card — total steps
        long bestSteps = bestDaySteps(d);
        sb.append("<div style=\"background:#f1f5fb;border-radius:24px;padding:32px 16px;text-align:center;margin:16px 0;\">");
        int heroPct = (int) Math.round(clampedFraction(d.totalSteps(),
                (long) daysInWindow(d) * DAILY_STEP_GOAL) * 100);
        sb.append("<img src=\"cid:").append(HERO_RING_CID)
                .append("\" width=\"120\" height=\"120\" alt=\"")
                .append(heroPct).append("% of weekly step goal\" style=\"display:block;margin:0 auto;\"/>");
        sb.append("<div style=\"font-size:44px;font-weight:800;margin:8px 0 0;\">").append(fmtInt(d.totalSteps())).append("</div>");
        sb.append("<div style=\"font-size:18px;color:#5f6368;\">total steps</div>");
        sb.append("<div style=\"margin-top:16px;\">");
        sb.append(pill("⭐ Best day · " + fmtInt(bestSteps), "up")).append(" ");
        sb.append(pill("Avg " + fmtInt(Math.round(d.avgSteps())) + " steps/day", "same")).append(" ");
        long stepDiff = d.totalSteps() - (p != null ? p.totalSteps() : 0);
        sb.append(deltaPill(fmtInt(Math.abs(stepDiff)), stepDiff, false));
        sb.append("</div></div>");

        // Stat card grid (3 columns)
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\">");
        gridRow(sb,
                statCard("🏋️", fmtInt(d.totalWorkouts()), "workout sessions",
                        p != null ? deltaSessions(d.totalWorkouts() - p.totalWorkouts()) : null),
                statCard("📅", d.daysTrained() + " of " + daysInWindow(d), "days trained", null),
                statCard("🔥", fmt1(d.totalActiveCalories()), "active calories",
                        p != null ? deltaPill(fmt1(Math.abs(d.totalActiveCalories() - p.totalActiveCalories())),
                                d.totalActiveCalories() - p.totalActiveCalories(), false) : null));
        gridRow(sb,
                statCard("📍", fmt1(d.totalDistanceKm()), "total kilometers",
                        p != null ? deltaPill(fmt1(Math.abs(d.totalDistanceKm() - p.totalDistanceKm())) + " km",
                                d.totalDistanceKm() - p.totalDistanceKm(), false) : null),
                d.avgSleepMinutes() != null
                        ? statCard("💤", fmtDuration(d.avgSleepMinutes().longValue()), "avg. sleep",
                                p != null && p.avgSleepMinutes() != null
                                        ? deltaPill(fmtDuration((long) Math.abs(d.avgSleepMinutes() - p.avgSleepMinutes())),
                                                d.avgSleepMinutes() - p.avgSleepMinutes(), false) : null)
                        : statCard("💤", "–", "avg. sleep", null),
                d.avgRestingHr() != null
                        ? statCard("❤️", d.avgRestingHr() + " bpm", "avg. resting heart rate",
                                p != null && p.avgRestingHr() != null
                                        ? deltaPill(Math.abs(d.avgRestingHr() - p.avgRestingHr()) + " bpm",
                                                (double) (d.avgRestingHr() - p.avgRestingHr()), true) : null)
                        : statCard("❤️", "–", "avg. resting heart rate", null));
        gridRow(sb,
                d.avgWeightKg() != null
                        ? statCard("⚖️", fmt1(d.avgWeightKg()) + " kg", "avg. weight",
                                p != null && p.avgWeightKg() != null
                                        ? weightPill(d.avgWeightKg() - p.avgWeightKg()) : null)
                        : statCard("⚖️", "–", "avg. weight", null),
                statCard("💪", fmt1(d.totalVolumeKg()) + " kg", "total volume lifted",
                        p != null ? deltaPill(fmt1(Math.abs(d.totalVolumeKg() - p.totalVolumeKg())) + " kg",
                                d.totalVolumeKg() - p.totalVolumeKg(), false) : null),
                statCard("⏱️", fmtInt(d.workoutMinutes()), "workout minutes",
                        p != null ? deltaSessions(d.workoutMinutes() - p.workoutMinutes()) : null));
        sb.append("</table>");

        // Top exercises
        sb.append("<h3 style=\"font-size:18px;margin:24px 0 8px;\">🏆 Top exercises</h3>");
        if (d.exercises().isEmpty()) {
            sb.append("<p style=\"font-size:14px;color:#5f6368;\">No workouts logged in this period — open the GymSuit app and log a session to see it here.</p>");
        } else {
            sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"font-size:14px;\">");
            int shown = 0;
            for (ExerciseSummary e : d.exercises()) {
                if (shown++ >= 5) break;
                sb.append("<tr><td style=\"padding:8px;border-bottom:1px solid #eee;\">💪 ")
                        .append(escape(e.title())).append("</td>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right;color:#5f6368;\">")
                        .append(e.sessions()).append(" sessions</td>")
                        .append("<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right;font-weight:bold;\">")
                        .append(fmt1(e.volumeKg())).append(" kg</td></tr>");
            }
            sb.append("</table>");
        }

        sb.append("<p style=\"font-size:14px;color:#5f6368;margin-top:20px;\">📎 The attached PDF has the full day-by-day breakdown of every metric and workout.</p>");
        sb.append("<hr style=\"border:none;border-top:1px solid #eee;margin:20px 0;\"/>");
        sb.append("<p style=\"font-size:12px;color:#999;\">Sent from GymSuit 💙 Keep moving!</p>");
        sb.append("</div>");
        return sb.toString();
    }

    /** 7-day strip with per-day steps; the best day is highlighted. */
    private String dayStrip(ReportData d) {
        Map<LocalDate, Long> stepsByDay = stepsByDay(d);
        LocalDate best = null;
        long bestSteps = -1;
        for (Map.Entry<LocalDate, Long> e : stepsByDay.entrySet()) {
            if (e.getValue() > bestSteps) {
                bestSteps = e.getValue();
                best = e.getKey();
            }
        }
        DateTimeFormatter dow = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        StringBuilder sb = new StringBuilder();
        sb.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"margin:8px 0 4px;\"><tr>");
        LocalDate day = d.start();
        int dayIndex = 0;
        while (!day.isAfter(d.end())) {
            Long steps = stepsByDay.get(day);
            boolean isBest = best != null && best.equals(day) && bestSteps > 0;
            sb.append("<td style=\"text-align:center;padding:4px;vertical-align:top;\">");
            if (isBest) {
                sb.append("<div style=\"background:#e6f4ea;border-radius:14px;padding:8px 2px;\">");
                sb.append("<div style=\"font-size:11px;font-weight:bold;\">⭐ Best day!</div>");
            } else {
                sb.append("<div style=\"padding:8px 2px;\">");
            }
            sb.append("<div style=\"font-size:12px;font-weight:bold;color:#202124;\">").append(day.format(dow)).append("</div>");
            int dayPct = (int) Math.round(clampedFraction(
                    steps != null ? steps : 0, DAILY_STEP_GOAL) * 100);
            sb.append("<img src=\"cid:").append(dayRingCid(dayIndex))
                    .append("\" width=\"40\" height=\"40\" alt=\"")
                    .append(dayPct).append("% of daily step goal\" style=\"display:block;margin:4px auto;\"/>");
            sb.append("<div style=\"font-size:11px;color:#5f6368;\">")
                    .append(steps != null && steps > 0 ? fmtInt(steps) : "–").append("</div>");
            sb.append("</div></td>");
            day = day.plusDays(1);
            dayIndex++;
        }
        sb.append("</tr></table>");
        return sb.toString();
    }

    /** Steps per day, keyed by date. Shared by the day strip and the ring images. */
    private Map<LocalDate, Long> stepsByDay(ReportData d) {
        Map<LocalDate, Long> stepsByDay = new LinkedHashMap<>();
        for (DailyHealthRecord r : d.dailyRecords()) {
            if (r.getSteps() != null) stepsByDay.put(r.getRecordDate(), r.getSteps());
        }
        return stepsByDay;
    }

    /** Fraction of a step goal, clamped to 0..1 (rings cap at a full circle). */
    private double clampedFraction(long steps, long goal) {
        if (goal <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (double) steps / goal));
    }

    private String dayRingCid(int dayIndex) {
        return "steps-ring-day-" + dayIndex;
    }

    /** Hero + per-day step rings as PNG bytes, keyed by their Resend content-id. */
    private Map<String, byte[]> stepRingImages(ReportData d) throws IOException {
        Map<String, byte[]> images = new LinkedHashMap<>();
        images.put(HERO_RING_CID, ringPng(120,
                clampedFraction(d.totalSteps(), (long) daysInWindow(d) * DAILY_STEP_GOAL), 12));
        Map<LocalDate, Long> stepsByDay = stepsByDay(d);
        LocalDate day = d.start();
        int dayIndex = 0;
        while (!day.isAfter(d.end())) {
            Long steps = stepsByDay.get(day);
            images.put(dayRingCid(dayIndex),
                    ringPng(40, clampedFraction(steps != null ? steps : 0, DAILY_STEP_GOAL), 5));
            day = day.plusDays(1);
            dayIndex++;
        }
        return images;
    }

    /**
     * A round progress ring as a transparent PNG: light track circle with a
     * green arc from the top, clockwise, {@code fraction} of a full circle.
     */
    private byte[] ringPng(int size, double fraction, int stroke) throws IOException {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = stroke / 2 + 2;
        int diameter = size - 2 * inset;
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xE2E8F0));
        g.drawArc(inset, inset, diameter, diameter, 0, 360);
        if (fraction > 0) {
            g.setColor(new Color(0x22C55E));
            g.drawArc(inset, inset, diameter, diameter, 90, -(int) Math.round(fraction * 360));
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private long bestDaySteps(ReportData d) {
        long best = 0;
        for (DailyHealthRecord r : d.dailyRecords()) {
            if (r.getSteps() != null && r.getSteps() > best) best = r.getSteps();
        }
        return best;
    }

    private void gridRow(StringBuilder sb, String c1, String c2, String c3) {
        sb.append("<tr>").append(c1).append(c2).append(c3).append("</tr>");
    }

    private String statCard(String icon, String value, String label, String pillHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<td width=\"33%\" style=\"padding:6px;vertical-align:top;\">");
        sb.append("<div style=\"background:#f1f5fb;border-radius:16px;padding:18px 6px;text-align:center;\">");
        sb.append("<div style=\"font-size:32px;line-height:1;\">").append(icon).append("</div>");
        sb.append("<div style=\"font-size:22px;font-weight:800;color:#202124;margin:8px 0 2px;\">")
                .append(escape(value)).append("</div>");
        sb.append("<div style=\"font-size:13px;color:#5f6368;\">").append(escape(label)).append("</div>");
        if (pillHtml != null) {
            sb.append("<div style=\"margin-top:10px;\">").append(pillHtml).append("</div>");
        }
        sb.append("</div></td>");
        return sb.toString();
    }

    private String pill(String text, String kind) {
        String bg;
        String fg;
        switch (kind) {
            case "up": bg = "#e6f4ea"; fg = "#137333"; break;
            case "down": bg = "#fef7e0"; fg = "#b06000"; break;
            default: bg = "#e8f0fe"; fg = "#1a73e8"; break;
        }
        return "<span style=\"display:inline-block;background:" + bg + ";color:" + fg
                + ";border-radius:999px;padding:5px 12px;font-size:12px;font-weight:bold;white-space:nowrap;\">"
                + escape(text) + "</span>";
    }

    /**
     * Week-over-week delta pill. {@code magnitude} is preformatted (e.g. "1,620" or "2.0 km").
     * {@code lowerIsGood} flips the color when a decrease is the healthy direction (resting HR).
     */
    private String deltaPill(String magnitude, double diff, boolean lowerIsGood) {
        if (Math.abs(diff) < 0.0005) {
            return pill("same as previous week", "same");
        }
        boolean good = lowerIsGood ? diff < 0 : diff > 0;
        String arrow = diff > 0 ? "↑" : "↓";
        String word = diff > 0 ? "more" : "lower";
        return pill(arrow + " " + magnitude + " " + word + " than last week", good ? "up" : "down");
    }

    private String deltaSessions(int diff) {
        if (diff == 0) return pill("same as previous week", "same");
        boolean good = diff > 0;
        return pill((diff > 0 ? "↑ " : "↓ ") + Math.abs(diff) + (diff > 0 ? " more" : " fewer") + " than last week",
                good ? "up" : "down");
    }

    /** Weight deltas stay neutral blue — direction isn't judged. */
    private String weightPill(double diff) {
        if (Math.abs(diff) < 0.0005) return pill("same as previous week", "same");
        String arrow = diff > 0 ? "↑" : "↓";
        return pill(arrow + " " + fmt1(Math.abs(diff)) + " kg vs last week", "same");
    }

    // ------------------------------------------------------------------
    // PDF
    // ------------------------------------------------------------------

    private byte[] renderPdf(String name, String period, ReportData d) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PdfWriter w = new PdfWriter(doc);

            w.title("GymSuit Health Report");
            w.subtitle("Prepared for " + name + "  ·  " + period);

            w.section("Summary");
            w.kv("Workouts", d.totalWorkouts() + " sessions (" + d.workoutMinutes() + " min)");
            w.kv("Gym frequency", d.daysTrained() + " of " + daysInWindow(d) + " days trained");
            w.kv("Sets / Reps", fmtInt(d.totalSets()) + " sets · " + fmtInt(d.totalReps()) + " reps");
            w.kv("Total volume", fmt1(d.totalVolumeKg()) + " kg");
            w.kv("Workout calories", fmt1(d.workoutCalories()) + " kcal");
            w.kv("Steps", fmtInt(d.totalSteps()) + " total · " + fmtInt(Math.round(d.avgSteps())) + " avg/day");
            w.kv("Active calories", fmt1(d.totalActiveCalories()) + " kcal");
            w.kv("Distance", fmt1(d.totalDistanceKm()) + " km");
            if (d.avgSleepMinutes() != null) {
                w.kv("Average sleep", fmtDuration(d.avgSleepMinutes().longValue()));
            }
            if (d.avgRestingHr() != null) {
                w.kv("Average resting heart rate", d.avgRestingHr() + " bpm");
            }

            w.section("Daily breakdown");
            if (d.dailyRecords().isEmpty()) {
                w.line("No daily health data synced in this period.");
            } else {
                w.table(
                        new String[]{"Date", "Steps", "Active kcal", "Distance", "Sleep", "Workouts"},
                        new float[]{150, 80, 85, 80, 80, 65},
                        rows(d));
            }

            w.section("Workout sessions");
            if (d.workouts().isEmpty()) {
                w.line("No workouts logged in this period.");
            } else {
                for (WorkoutRecord rec : d.workouts()) {
                    String when = rec.getStartTime() != null
                            ? rec.getStartTime().format(DateTimeFormatter.ofPattern("EEE, MMM d HH:mm", Locale.ENGLISH))
                            : d.end().format(DAY_FMT);
                    String title = rec.getTitle() != null && !rec.getTitle().isBlank() ? rec.getTitle().trim() : "Workout";
                    w.line(when + " - " + title);
                    w.line("    " + orZero(rec.getDurationMinutes()) + " min · "
                            + orZero(rec.getSetCount()) + " sets · "
                            + orZero(rec.getTotalReps()) + " reps · "
                            + fmt1(orZero(rec.getTotalVolumeKg())) + " kg"
                            + (rec.getCaloriesBurned() != null && rec.getCaloriesBurned() > 0
                            ? " · " + fmt1(rec.getCaloriesBurned()) + " kcal" : ""));
                }
            }

            w.section("Exercises done (" + period + ")");
            if (d.exercises().isEmpty()) {
                w.line("No exercises logged in this period.");
            } else {
                List<String[]> exRows = new ArrayList<>();
                for (ExerciseSummary e : d.exercises()) {
                    exRows.add(new String[]{
                            e.title(),
                            String.valueOf(e.sessions()),
                            fmtInt(e.sets()),
                            fmtInt(e.reps()),
                            fmt1(e.volumeKg()) + " kg"});
                }
                w.table(
                        new String[]{"Exercise", "Sessions", "Sets", "Reps", "Volume"},
                        new float[]{220, 70, 60, 80, 110},
                        exRows);
            }

            w.line("");
            w.small("Generated by GymSuit on "
                    + LocalDate.now().format(PERIOD_FMT)
                    + " from health data synced from your mobile app.");

            w.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private List<String[]> rows(ReportData d) {
        List<String[]> rows = new ArrayList<>();
        for (DailyHealthRecord r : d.dailyRecords()) {
            long workoutsThatDay = d.workouts().stream()
                    .filter(w -> w.getStartTime() != null && w.getStartTime().toLocalDate().equals(r.getRecordDate()))
                    .count();
            rows.add(new String[]{
                    r.getRecordDate().format(DAY_FMT),
                    r.getSteps() != null ? fmtInt(r.getSteps()) : "–",
                    r.getActiveCalories() != null ? fmt1(r.getActiveCalories()) : "–",
                    r.getDistanceMeters() != null ? fmt1(r.getDistanceMeters() / 1000.0) + " km" : "–",
                    r.getSleepDurationMinutes() != null && r.getSleepDurationMinutes() > 0
                            ? fmtDuration(r.getSleepDurationMinutes()) : "–",
                    String.valueOf(workoutsThatDay)});
        }
        return rows;
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private int daysInWindow(ReportData d) {
        return (int) (d.end().toEpochDay() - d.start().toEpochDay() + 1);
    }

    private static int orZero(Integer v) {
        return v != null ? v : 0;
    }

    private static long orZero(Long v) {
        return v != null ? v : 0L;
    }

    private static double orZero(Double v) {
        return v != null ? v : 0.0;
    }

    private static String fmtInt(long v) {
        return String.format(Locale.ENGLISH, "%,d", v);
    }

    private static String fmt1(double v) {
        return String.format(Locale.ENGLISH, "%,.1f", v);
    }

    private static String fmtDuration(long minutes) {
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Minimal PDFBox writer: text cursor with automatic page breaks,
     * key/value lines, and simple tables.
     */
    private static class PdfWriter {
        private final PDDocument doc;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        private static final float MARGIN = 50;
        private static final float PAGE_W = PDRectangle.A4.getWidth();
        private static final float PAGE_H = PDRectangle.A4.getHeight();
        private static final float BOTTOM = 60;

        PdfWriter(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PAGE_H - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM) {
                newPage();
            }
        }

        private void text(float x, float yPos, String text, PDType1Font font, float size) throws IOException {
            cs.setFont(font, size);
            cs.setNonStrokingColor(Color.BLACK);
            cs.beginText();
            cs.newLineAtOffset(x, yPos);
            cs.showText(safe(text));
            cs.endText();
        }

        /** PDFBox Type 1 fonts are WinAnsi — replace anything outside it. */
        private String safe(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                sb.append(c < 256 ? c : '?');
            }
            return sb.toString();
        }

        void title(String t) throws IOException {
            ensureSpace(40);
            text(MARGIN, y, t, PDType1Font.HELVETICA_BOLD, 20);
            y -= 28;
        }

        void subtitle(String t) throws IOException {
            ensureSpace(24);
            text(MARGIN, y, t, PDType1Font.HELVETICA, 11);
            y -= 24;
            rule();
        }

        void section(String t) throws IOException {
            ensureSpace(36);
            y -= 6;
            text(MARGIN, y, t, PDType1Font.HELVETICA_BOLD, 14);
            y -= 20;
        }

        void kv(String key, String value) throws IOException {
            ensureSpace(18);
            text(MARGIN, y, key + ":", PDType1Font.HELVETICA, 11);
            text(MARGIN + 220, y, value, PDType1Font.HELVETICA_BOLD, 11);
            y -= 16;
        }

        void line(String t) throws IOException {
            for (String part : wrap(t, 95)) {
                ensureSpace(16);
                text(MARGIN, y, part, PDType1Font.HELVETICA, 10);
                y -= 14;
            }
        }

        void small(String t) throws IOException {
            ensureSpace(16);
            text(MARGIN, y, t, PDType1Font.HELVETICA_OBLIQUE, 9);
            y -= 14;
        }

        void table(String[] headers, float[] widths, List<String[]> rows) throws IOException {
            float x0 = MARGIN;
            // header
            ensureSpace(22);
            float x = x0;
            cs.setNonStrokingColor(new Color(235, 238, 242));
            float totalW = 0;
            for (float wdt : widths) totalW += wdt;
            cs.addRect(x0, y - 4, totalW, 18);
            cs.fill();
            for (int i = 0; i < headers.length; i++) {
                text(x + 4, y, headers[i], PDType1Font.HELVETICA_BOLD, 10);
                x += widths[i];
            }
            y -= 18;
            // rows
            for (String[] row : rows) {
                ensureSpace(18);
                x = x0;
                for (int i = 0; i < row.length && i < widths.length; i++) {
                    text(x + 4, y, truncate(row[i], widths[i]), PDType1Font.HELVETICA, 10);
                    x += widths[i];
                }
                y -= 15;
                // light divider
                cs.setStrokingColor(new Color(220, 224, 230));
                cs.setLineWidth(0.5f);
                cs.moveTo(x0, y + 4);
                cs.lineTo(x0 + totalW, y + 4);
                cs.stroke();
            }
            y -= 8;
        }

        private void rule() throws IOException {
            cs.setStrokingColor(new Color(200, 206, 214));
            cs.setLineWidth(1f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(PAGE_W - MARGIN, y);
            cs.stroke();
            y -= 14;
        }

        private String truncate(String s, float colWidthPt) {
            if (s == null) return "";
            // ~5.5pt per char at 10pt Helvetica; leave padding.
            int max = Math.max(4, (int) ((colWidthPt - 10) / 5.5));
            return s.length() <= max ? s : s.substring(0, max - 1) + "..";
        }

        private List<String> wrap(String s, int maxChars) {
            List<String> out = new ArrayList<>();
            if (s == null || s.isEmpty()) {
                out.add("");
                return out;
            }
            String rest = s;
            while (rest.length() > maxChars) {
                int cut = rest.lastIndexOf(' ', maxChars);
                if (cut <= 0) cut = maxChars;
                out.add(rest.substring(0, cut));
                rest = rest.substring(cut).trim();
            }
            out.add(rest);
            return out;
        }

        void close() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }
    }
}

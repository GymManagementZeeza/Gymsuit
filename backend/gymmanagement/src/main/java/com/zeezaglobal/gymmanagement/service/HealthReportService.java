package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.entity.WorkoutRecord;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
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
        }

        return new ReportData(
                start, end,
                workouts.size(), trainedDays.size(),
                totalSets, totalReps, totalVolumeKg, workoutCalories, workoutMinutes,
                totalSteps, daysWithSteps > 0 ? (double) totalSteps / daysWithSteps : 0,
                totalActiveCalories, totalDistanceKm,
                sleepCount > 0 ? (double) sleepSum / sleepCount : null,
                restingHrCount > 0 ? (int) Math.round((double) restingHrSum / restingHrCount) : null,
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
    // Email
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public void sendReportEmail(User user, ReportData data) {
        String name = displayName(user);
        String period = periodLabel(data.start(), data.end());
        String subject = "Your GymSuit health report — " + period;
        String html = buildEmailHtml(name, period, data);
        String filename = "GymSuit-Health-Report-" + data.end() + ".pdf";
        try {
            byte[] pdf = renderPdf(name, period, data);
            emailService.sendEmailWithAttachment(user.getEmail(), subject, html, filename, pdf, "application/pdf");
            log.info("Sent health report to user {} ({} bytes pdf)", user.getId(), pdf.length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render health report PDF for user " + user.getId(), e);
        }
    }

    private String buildEmailHtml(String name, String period, ReportData d) {
        StringBuilder rows = new StringBuilder();
        statRow(rows, "Workouts", d.totalWorkouts() + " sessions");
        statRow(rows, "Gym frequency", d.daysTrained() + " of " + daysInWindow(d) + " days");
        statRow(rows, "Total volume", fmt1(d.totalVolumeKg()) + " kg");
        statRow(rows, "Steps", fmtInt(d.totalSteps()) + " total · " + fmtInt(Math.round(d.avgSteps())) + " avg/day");
        statRow(rows, "Active calories", fmt1(d.totalActiveCalories()) + " kcal");
        statRow(rows, "Distance", fmt1(d.totalDistanceKm()) + " km");
        if (d.avgSleepMinutes() != null) {
            statRow(rows, "Avg sleep", fmtDuration(d.avgSleepMinutes().longValue()));
        }
        if (d.avgRestingHr() != null) {
            statRow(rows, "Avg resting HR", d.avgRestingHr() + " bpm");
        }

        StringBuilder exRows = new StringBuilder();
        int shown = 0;
        for (ExerciseSummary e : d.exercises()) {
            if (shown++ >= 5) break;
            exRows.append("<tr><td style=\"padding:6px 8px;border-bottom:1px solid #eee;\">")
                    .append(escape(e.title())).append("</td>")
                    .append("<td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:right;\">")
                    .append(e.sessions()).append(" sessions</td>")
                    .append("<td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:right;\">")
                    .append(fmt1(e.volumeKg())).append(" kg</td></tr>");
        }
        String exerciseTable = exRows.length() > 0
                ? "<h3>Top exercises</h3><table style=\"width:100%;border-collapse:collapse;font-size:14px;\">" + exRows + "</table>"
                : "<p>No workouts logged in this period — open the GymSuit app and log a session to see it here.</p>";

        return "<div style=\"font-family:Arial,sans-serif;padding:20px;color:#333;max-width:600px;\">"
                + "<h2>Your GymSuit health report</h2>"
                + "<p>Hi " + escape(name) + ", here is your activity for <b>" + escape(period) + "</b>, "
                + "based on the health data synced from your phone.</p>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;margin:16px 0;\">" + rows + "</table>"
                + exerciseTable
                + "<p>The attached PDF has the full day-by-day breakdown of every metric and workout.</p>"
                + "<hr style=\"border:none;border-top:1px solid #eee;margin:20px 0;\"/>"
                + "<p style=\"font-size:12px;color:#777;\">Sent from GymSuit. Keep moving!</p>"
                + "</div>";
    }

    private void statRow(StringBuilder sb, String label, String value) {
        sb.append("<tr><td style=\"padding:6px 8px;border-bottom:1px solid #eee;color:#666;\">")
                .append(escape(label)).append("</td>")
                .append("<td style=\"padding:6px 8px;border-bottom:1px solid #eee;text-align:right;font-weight:bold;\">")
                .append(escape(value)).append("</td></tr>");
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

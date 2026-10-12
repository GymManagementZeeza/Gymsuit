package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import com.zeezaglobal.gymmanagement.service.HealthReportService.ReportData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Periodic health report email. Builds one report per user from the health
 * data their mobile app synced (daily records + workout log), attaches it as
 * a PDF, and emails it via Resend.
 *
 * <p>Testing: runs DAILY at 07:00 server time by default. For the weekly
 * rollout set:
 * <pre>
 * app.health-report.cron=0 0 7 * * MON
 * app.health-report.lookback-days=7
 * </pre>
 * The window always covers completed days (ends yesterday), so a Monday 7am
 * run reports the previous Monday–Sunday week.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthReportTask {

    private final HealthReportService reportService;
    private final DailyHealthRecordRepository dailyRepo;
    private final WorkoutRecordRepository workoutRepo;
    private final UserRepository userRepository;

    @Value("${app.health-report.enabled:true}")
    private boolean enabled;

    @Value("${app.health-report.lookback-days:7}")
    private int lookbackDays;

    @Scheduled(cron = "${app.health-report.cron:0 0 7 * * *}")
    public void run() {
        if (!enabled) {
            log.info("Health report task skipped (app.health-report.enabled=false)");
            return;
        }
        int days = Math.max(1, lookbackDays);
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(days - 1);

        Set<Long> userIds = new HashSet<>(dailyRepo.findUserIdsWithRecords(start, end));
        userIds.addAll(workoutRepo.findUserIdsWithWorkouts(
                start.atStartOfDay(), end.plusDays(1).atStartOfDay()));

        int sent = 0;
        int failed = 0;
        int skipped = 0;
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null || !user.isEnabled()
                        || user.getEmail() == null || user.getEmail().isBlank()) {
                    skipped++;
                    continue;
                }
                ReportData data = reportService.buildReport(user, start, end);
                reportService.sendReportEmail(user, data);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("Health report failed for user {}", userId, e);
            }
        }
        log.info("Health report run complete: {} sent, {} failed, {} skipped ({} to {})",
                sent, failed, skipped, start, end);
    }
}

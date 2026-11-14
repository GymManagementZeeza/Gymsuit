package com.zeezaglobal.gymmanagement.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Overall eating-habit score for the signed-in member, computed from the
 * food they actually logged (see EatingHabitScoreService). The score is
 * 0–100; it is null with label "No data yet" when nothing was logged in
 * the window, so the app never shows a made-up number.
 */
public record EatingHabitScoreResponse(
        /** 0–100, rounded. Null when no food was logged in the window. */
        Integer score,
        /** Excellent / Optimal / Fair / Needs attention / Poor, or "No data yet". */
        String label,
        /** Days in the window with at least one logged entry. */
        int daysLogged,
        int windowDays,
        LocalDate windowStart,
        LocalDate windowEnd,
        /** Mean daily calories across logged days (0 when no data). */
        double avgCaloriesKcal,
        /** Mean daily protein across logged days (0 when no data). */
        double avgProteinG,
        /** Protein target used: 1.6 g/kg of latest known weight, else 120 g. */
        double proteinTargetG,
        int calorieTargetKcal,
        /** Mean per-component points across logged days (each 0–25). */
        Components components,
        /**
         * Mean over logged days of (protein + balance points) / 50, as 0–100.
         * The nutrition-quality half of the score, shown on the meals page
         * as "Food Quality".
         */
        double foodQuality,
        /** One rounded score per logged day, oldest first. */
        List<DayScore> dailyScores
) {
    public record Components(
            double protein,
            double calories,
            double balance,
            double pattern
    ) {
    }

    public record DayScore(
            LocalDate date,
            int score
    ) {
    }
}

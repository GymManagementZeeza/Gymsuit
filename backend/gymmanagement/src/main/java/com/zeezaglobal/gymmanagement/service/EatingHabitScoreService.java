package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.EatingHabitScoreResponse;
import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.FoodIntakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Overall eating-habit score, 0–100, from the food the member actually
 * logged — never guessed, never filled in from averages. The score is the
 * mean of one per-day score over the trailing window (default 7 days);
 * only days with at least one logged entry count, and with no logged days
 * there is no score at all.
 *
 * <p>Each logged day earns up to 25 points in four components:</p>
 * <ul>
 *   <li><b>Protein (25):</b> daily protein vs 1.6 g per kg of the latest
 *   known body weight (120 g when no weight is on record). Full points at
 *   or above target, proportional below it. Extra protein is not
 *   penalised.</li>
 *   <li><b>Calories (25):</b> daily calories vs the calorie target
 *   (default 2000 kcal, the same target the app shows). Full points
 *   within ±10% of target, falling linearly to 0 at ±50% — under-eating
 *   and over-eating both cost points.</li>
 *   <li><b>Macro balance (25):</b> share of calories from protein 15–30%
 *   (10 pts), fat 20–35% (7.5 pts), carbs 45–65% (7.5 pts) — the standard
 *   acceptable ranges. Full points inside the range, falling linearly to 0
 *   at 15 percentage points outside it.</li>
 *   <li><b>Meal pattern (25):</b> distinct meal slots logged that day —
 *   breakfast, lunch, dinner, snack — 4 slots 15 pts, 3 slots 12, 2 slots
 *   8, 1 slot 4; plus distinct dishes — 5+ dishes 10 pts, 4 dishes 8,
 *   3 dishes 6, 2 dishes 4, 1 dish 2.</li>
 * </ul>
 *
 * <p>Labels: 90–100 Excellent, 75–89 Optimal, 60–74 Fair, 40–59 Needs
 * attention, 0–39 Poor. Food quality is the nutrition-quality half of
 * the score — the mean over logged days of (protein + balance points) /
 * 50, as 0–100 — and is what the meals page shows as "Food Quality".</p>
 *
 * <p>Deliberately not scored: glucose impact. GymSuit tracks no glucose
 * data, so no glucose number is shown anywhere — a glucose value here
 * would be invented.</p>
 */
@Service
@RequiredArgsConstructor
public class EatingHabitScoreService {

    private final FoodIntakeRepository foodIntakeRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;

    static final double PROTEIN_G_PER_KG = 1.6;
    static final double DEFAULT_PROTEIN_TARGET_G = 120.0;
    static final int DEFAULT_CALORIE_TARGET_KCAL = 2000;
    static final int MAX_WINDOW_DAYS = 30;
    static final int MIN_CALORIE_TARGET_KCAL = 1200;
    static final int MAX_CALORIE_TARGET_KCAL = 4000;

    /** Score over the trailing window for one member. */
    public EatingHabitScoreResponse scoreForUser(Long userId, int days, int calorieTargetKcal) {
        int windowDays = Math.max(1, Math.min(days, MAX_WINDOW_DAYS));
        int target = Math.max(MIN_CALORIE_TARGET_KCAL,
                Math.min(calorieTargetKcal, MAX_CALORIE_TARGET_KCAL));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(windowDays - 1);
        List<FoodIntakeRecord> records = foodIntakeRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, start, end);
        Double latestWeightKg = dailyHealthRecordRepository
                .findTopByUserIdAndWeightKgIsNotNullOrderByRecordDateDesc(userId)
                .map(DailyHealthRecord::getWeightKg)
                .orElse(null);
        return compute(records, latestWeightKg, target, end, windowDays);
    }

    /**
     * Pure scoring step, kept static so it is unit-testable without a
     * database. {@code records} may span any dates; only records inside
     * [end - windowDays + 1, end] are scored, grouped by record date.
     */
    static EatingHabitScoreResponse compute(
            List<FoodIntakeRecord> records,
            Double latestWeightKg,
            int calorieTargetKcal,
            LocalDate end,
            int windowDays) {
        LocalDate start = end.minusDays(windowDays - 1);
        double proteinTargetG = latestWeightKg != null && latestWeightKg > 0
                ? latestWeightKg * PROTEIN_G_PER_KG
                : DEFAULT_PROTEIN_TARGET_G;

        Map<LocalDate, DayTotals> byDay = new TreeMap<>();
        for (FoodIntakeRecord record : records) {
            if (record.getRecordDate() == null
                    || record.getRecordDate().isBefore(start)
                    || record.getRecordDate().isAfter(end)) {
                continue;
            }
            DayTotals totals = byDay.computeIfAbsent(record.getRecordDate(), d -> new DayTotals());
            totals.calories += nullToZero(record.getCalories());
            totals.proteinG += nullToZero(record.getProteinG());
            totals.carbsG += nullToZero(record.getCarbsG());
            totals.fatG += nullToZero(record.getFatG());
            if (record.getMeal() != null && !record.getMeal().isBlank()) {
                totals.mealSlots.add(record.getMeal().trim().toUpperCase());
            }
            if (record.getDishName() != null && !record.getDishName().isBlank()) {
                totals.dishes.add(record.getDishName().trim().toLowerCase());
            }
        }

        if (byDay.isEmpty()) {
            return new EatingHabitScoreResponse(
                    null, "No data yet", 0, windowDays, start, end,
                    0.0, 0.0, round1(proteinTargetG), calorieTargetKcal,
                    new EatingHabitScoreResponse.Components(0.0, 0.0, 0.0, 0.0),
                    0.0, List.of(),
                    "Log your meals for a few days and we'll show how your eating is going and what to improve.",
                    List.of(), healthTipFor(end));
        }

        List<EatingHabitScoreResponse.DayScore> dailyScores = new ArrayList<>();
        double proteinSum = 0.0;
        double caloriesSum = 0.0;
        double balanceSum = 0.0;
        double patternSum = 0.0;
        double foodQualitySum = 0.0;
        double kcalSum = 0.0;
        double proteinGSum = 0.0;
        double carbsGSum = 0.0;
        double fatGSum = 0.0;
        double slotCountSum = 0.0;
        double dishCountSum = 0.0;
        Map<String, Integer> slotDays = new TreeMap<>();
        for (Map.Entry<LocalDate, DayTotals> entry : byDay.entrySet()) {
            DayTotals day = entry.getValue();
            double protein = proteinPoints(day.proteinG, proteinTargetG);
            double calories = caloriePoints(day.calories, calorieTargetKcal);
            double balance = balancePoints(day);
            double pattern = patternPoints(day);
            double dayScore = protein + calories + balance + pattern;
            dailyScores.add(new EatingHabitScoreResponse.DayScore(
                    entry.getKey(), (int) Math.round(dayScore)));
            proteinSum += protein;
            caloriesSum += calories;
            balanceSum += balance;
            patternSum += pattern;
            foodQualitySum += (protein + balance) / 50.0 * 100.0;
            kcalSum += day.calories;
            proteinGSum += day.proteinG;
            carbsGSum += day.carbsG;
            fatGSum += day.fatG;
            slotCountSum += day.mealSlots.size();
            dishCountSum += day.dishes.size();
            for (String slot : day.mealSlots) {
                slotDays.merge(slot, 1, Integer::sum);
            }
        }

        int daysLogged = byDay.size();
        double overall = (proteinSum + caloriesSum + balanceSum + patternSum) / daysLogged;
        int score = (int) Math.round(overall);
        double[] avgPoints = {proteinSum / daysLogged, caloriesSum / daysLogged,
                balanceSum / daysLogged, patternSum / daysLogged};
        List<String> improvements = improvements(avgPoints, kcalSum / daysLogged, calorieTargetKcal,
                proteinGSum / daysLogged, proteinTargetG, kcalSum, proteinGSum, carbsGSum, fatGSum,
                slotCountSum / daysLogged, dishCountSum / daysLogged, slotDays, daysLogged, windowDays);
        String summary = summaryFor(score, avgPoints, daysLogged, windowDays);
        return new EatingHabitScoreResponse(
                score,
                labelFor(score),
                daysLogged,
                windowDays,
                start,
                end,
                round1(kcalSum / daysLogged),
                round1(proteinGSum / daysLogged),
                round1(proteinTargetG),
                calorieTargetKcal,
                new EatingHabitScoreResponse.Components(
                        round1(proteinSum / daysLogged),
                        round1(caloriesSum / daysLogged),
                        round1(balanceSum / daysLogged),
                        round1(patternSum / daysLogged)),
                round1(foodQualitySum / daysLogged),
                dailyScores,
                summary,
                improvements,
                healthTipFor(end));
    }

    private static final String[] COMPONENT_NAMES = {"protein", "calorie balance", "macro balance", "meal pattern"};
    private static final double GOOD_POINTS = 20.0;

    private static String summaryFor(int score, double[] avg, int daysLogged, int windowDays) {
        int best = 0;
        int worst = 0;
        for (int i = 1; i < avg.length; i++) {
            if (avg[i] > avg[best]) best = i;
            if (avg[i] < avg[worst]) worst = i;
        }
        String days = daysLogged + " of " + windowDays + " days logged";
        if (avg[worst] >= GOOD_POINTS) {
            return "Great work (" + days + "): all four areas are in good shape. Keep this routine going.";
        }
        String lead = score >= 75 ? "Good progress" : score >= 60 ? "Decent start" : "Room to improve";
        return lead + " (" + days + "). Your " + COMPONENT_NAMES[best] + " is your strongest area; "
                + COMPONENT_NAMES[worst] + " is the biggest opportunity.";
    }

    /** Specific, data-driven suggestions, weakest component first (max 3). */
    private static List<String> improvements(
            double[] avg, double avgKcal, int kcalTarget, double avgProteinG, double proteinTargetG,
            double kcalSum, double proteinGSum, double carbsGSum, double fatGSum,
            double avgSlots, double avgDishes, Map<String, Integer> slotDays,
            int daysLogged, int windowDays) {
        Integer[] order = {0, 1, 2, 3};
        java.util.Arrays.sort(order, (a, b) -> Double.compare(avg[a], avg[b]));
        List<String> out = new ArrayList<>();
        for (int idx : order) {
            if (avg[idx] >= GOOD_POINTS || out.size() >= 3) {
                continue;
            }
            switch (idx) {
                case 0 -> out.add("Protein: you average " + Math.round(avgProteinG) + " g a day against a target of "
                        + Math.round(proteinTargetG) + " g. Add eggs, Greek yogurt, paneer, dal, chicken, fish or tofu to your meals.");
                case 1 -> {
                    double ratio = avgKcal / kcalTarget;
                    out.add(ratio < 1.0
                            ? "Calories: you average " + Math.round(avgKcal) + " kcal against about " + kcalTarget
                                    + ". You may be under-eating; add a balanced snack or a larger portion at your lightest meal."
                            : "Calories: you average " + Math.round(avgKcal) + " kcal against about " + kcalTarget
                                    + ". Trim portions of calorie-dense foods, sugary drinks and fried snacks.");
                }
                case 2 -> out.add(macroTip(kcalSum, proteinGSum, carbsGSum, fatGSum));
                default -> {
                    if (avgSlots < 3.0) {
                        String missing = slotDays.getOrDefault("BREAKFAST", 0) < daysLogged / 2.0 + 0.01
                                && slotDays.getOrDefault("BREAKFAST", 0) < daysLogged ? "breakfast"
                                : slotDays.getOrDefault("LUNCH", 0) < daysLogged ? "lunch"
                                : slotDays.getOrDefault("DINNER", 0) < daysLogged ? "dinner" : "meals";
                        out.add("Meal pattern: you log about " + Math.round(avgSlots * 10) / 10.0
                                + " meal types a day. Try to log (and eat) regular " + missing + " and spread food across the day.");
                    } else {
                        out.add("Variety: you eat about " + Math.round(avgDishes) + " different dishes a day. Add more variety of vegetables, fruit, grains and proteins.");
                    }
                }
            }
        }
        if (daysLogged < Math.max(2, windowDays / 2) && out.size() < 3) {
            out.add("Log your food on more days: the score is based on " + daysLogged + " of the last "
                    + windowDays + " days, so more logging gives a truer picture.");
        }
        return out;
    }

    private static String macroTip(double kcal, double proteinG, double carbsG, double fatG) {
        if (kcal <= 0) {
            return "Macro balance: log calories and macros so we can check your protein, carb and fat split.";
        }
        double p = proteinG * 4.0 / kcal * 100.0;
        double c = carbsG * 4.0 / kcal * 100.0;
        double f = fatG * 9.0 / kcal * 100.0;
        double pDev = p < 15 ? 15 - p : Math.max(0, p - 30);
        double cDev = c < 45 ? 45 - c : Math.max(0, c - 65);
        double fDev = f < 20 ? 20 - f : Math.max(0, f - 35);
        if (fDev >= pDev && fDev >= cDev) {
            return f > 35
                    ? "Macro balance: about " + Math.round(f) + "% of calories come from fat (aim 20-35%). Cut back on fried food, butter and oils."
                    : "Macro balance: only " + Math.round(f) + "% of calories come from fat (aim 20-35%). Add nuts, seeds, olive oil or fish.";
        }
        if (cDev >= pDev) {
            return c > 65
                    ? "Macro balance: about " + Math.round(c) + "% of calories come from carbs (aim 45-65%). Swap some refined carbs for protein and vegetables."
                    : "Macro balance: only " + Math.round(c) + "% of calories come from carbs (aim 45-65%). Add whole grains, fruit or starchy vegetables.";
        }
        return p > 30
                ? "Macro balance: about " + Math.round(p) + "% of calories come from protein (aim 15-30%). Balance it with more vegetables and whole grains."
                : "Macro balance: only " + Math.round(p) + "% of calories come from protein (aim 15-30%). Include a protein source at every meal.";
    }

    private static final String[] HEALTH_TIPS = {
            "Drink water through the day; thirst is often mistaken for hunger.",
            "Fill half your plate with vegetables at lunch and dinner.",
            "Eat slowly: it takes about 20 minutes for fullness signals to reach your brain.",
            "Choose whole foods (fruit, nuts, whole grains) over packaged snacks most of the time.",
            "Get 7-9 hours of sleep. Poor sleep increases cravings and appetite.",
            "Include protein with every meal to stay full and support muscle recovery.",
            "Plan tomorrow's meals tonight so you are not deciding when hungry.",
    };

    static String healthTipFor(LocalDate date) {
        return HEALTH_TIPS[(int) (date.toEpochDay() % HEALTH_TIPS.length)];
    }

    private static String labelFor(int score) {
        if (score >= 90) {
            return "Excellent";
        }
        if (score >= 75) {
            return "Optimal";
        }
        if (score >= 60) {
            return "Fair";
        }
        if (score >= 40) {
            return "Needs attention";
        }
        return "Poor";
    }

    private static double proteinPoints(double proteinG, double targetG) {
        if (targetG <= 0) {
            return 0.0;
        }
        return Math.min(proteinG / targetG, 1.0) * 25.0;
    }

    private static double caloriePoints(double kcal, int targetKcal) {
        if (targetKcal <= 0) {
            return 0.0;
        }
        double ratio = kcal / targetKcal;
        if (ratio >= 0.9 && ratio <= 1.1) {
            return 25.0;
        }
        if (ratio < 0.9) {
            return 25.0 * Math.max(0.0, (ratio - 0.5) / 0.4);
        }
        return 25.0 * Math.max(0.0, (1.5 - ratio) / 0.4);
    }

    private static double balancePoints(DayTotals day) {
        if (day.calories <= 0) {
            return 0.0;
        }
        double proteinShare = day.proteinG * 4.0 / day.calories * 100.0;
        double fatShare = day.fatG * 9.0 / day.calories * 100.0;
        double carbsShare = day.carbsG * 4.0 / day.calories * 100.0;
        return rangePoints(proteinShare, 15.0, 30.0, 10.0)
                + rangePoints(fatShare, 20.0, 35.0, 7.5)
                + rangePoints(carbsShare, 45.0, 65.0, 7.5);
    }

    /** Full points inside [low, high]; linear falloff to 0 at 15 points outside. */
    private static double rangePoints(double value, double low, double high, double maxPoints) {
        if (value >= low && value <= high) {
            return maxPoints;
        }
        double outside = value < low ? low - value : value - high;
        return maxPoints * Math.max(0.0, 1.0 - outside / 15.0);
    }

    private static double patternPoints(DayTotals day) {
        double slots = switch (day.mealSlots.size()) {
            case 4 -> 15.0;
            case 3 -> 12.0;
            case 2 -> 8.0;
            case 1 -> 4.0;
            default -> 0.0;
        };
        double variety = switch (day.dishes.size()) {
            case 0 -> 0.0;
            case 1 -> 2.0;
            case 2 -> 4.0;
            case 3 -> 6.0;
            case 4 -> 8.0;
            default -> 10.0;
        };
        return slots + variety;
    }

    private static double nullToZero(Integer value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static final class DayTotals {
        double calories;
        double proteinG;
        double carbsG;
        double fatG;
        final Set<String> mealSlots = new HashSet<>();
        final Set<String> dishes = new HashSet<>();
    }
}

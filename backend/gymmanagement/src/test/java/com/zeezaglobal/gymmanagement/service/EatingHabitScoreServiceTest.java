package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.EatingHabitScoreResponse;
import com.zeezaglobal.gymmanagement.entity.DailyHealthRecord;
import com.zeezaglobal.gymmanagement.entity.FoodIntakeRecord;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.FoodIntakeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EatingHabitScoreServiceTest {

    private static final LocalDate END = LocalDate.of(2026, 11, 14);
    private static final double WEIGHT_KG = 92.0; // protein target 147.2 g

    @Mock
    private FoodIntakeRepository foodIntakeRepository;
    @Mock
    private DailyHealthRecordRepository dailyHealthRecordRepository;

    @InjectMocks
    private EatingHabitScoreService service;

    private static FoodIntakeRecord entry(LocalDate date, String meal, String dish,
                                          int kcal, double protein, double carbs, double fat) {
        FoodIntakeRecord record = new FoodIntakeRecord();
        record.setRecordDate(date);
        record.setMeal(meal);
        record.setDishName(dish);
        record.setCalories(kcal);
        record.setProteinG(protein);
        record.setCarbsG(carbs);
        record.setFatG(fat);
        return record;
    }

    /** 2000 kcal, 150 g protein, 45% carbs / 25% fat / 30% protein, 4 meals, 5 dishes. */
    private static List<FoodIntakeRecord> perfectDay(LocalDate date) {
        return List.of(
                entry(date, "BREAKFAST", "Oats with milk", 500, 20.0, 75.0, 13.3),
                entry(date, "LUNCH", "Chicken rice", 700, 60.0, 70.0, 18.9),
                entry(date, "SNACK", "Curd and fruit", 200, 15.0, 25.0, 4.4),
                entry(date, "DINNER", "Paneer chapati", 500, 50.0, 50.0, 16.7),
                entry(date, "DINNER", "Salad", 100, 5.0, 5.0, 2.3));
    }

    @Test
    void noRecordsHasNoScore() {
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                List.of(), WEIGHT_KG, 2000, END, 7);
        assertNull(response.score());
        assertEquals("No data yet", response.label());
        assertEquals(0, response.daysLogged());
        assertEquals(7, response.windowDays());
        assertTrue(response.dailyScores().isEmpty());
    }

    @Test
    void perfectDayScores100() {
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                perfectDay(END), WEIGHT_KG, 2000, END, 7);
        assertEquals(100, response.score());
        assertEquals("Excellent", response.label());
        assertEquals(1, response.daysLogged());
        assertEquals(25.0, response.components().protein());
        assertEquals(25.0, response.components().calories());
        assertEquals(25.0, response.components().balance());
        assertEquals(25.0, response.components().pattern());
        assertEquals(100.0, response.foodQuality());
        assertEquals(147.2, response.proteinTargetG());
    }

    @Test
    void recordsOutsideWindowAreIgnored() {
        List<FoodIntakeRecord> records = new java.util.ArrayList<>(perfectDay(END));
        records.add(entry(END.minusDays(20), "LUNCH", "Biryani", 3000, 40.0, 300.0, 150.0));
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                records, WEIGHT_KG, 2000, END, 7);
        assertEquals(100, response.score());
        assertEquals(1, response.daysLogged());
    }

    @Test
    void lowProteinDayLosesProteinPoints() {
        List<FoodIntakeRecord> records = List.of(
                entry(END, "BREAKFAST", "Puttu", 500, 10.0, 80.0, 15.0),
                entry(END, "LUNCH", "Rice and dal", 800, 15.0, 130.0, 20.0),
                entry(END, "DINNER", "Chapati and veg", 700, 15.0, 110.0, 20.0));
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                records, WEIGHT_KG, 2000, END, 7);
        assertTrue(response.components().protein() < 10.0,
                "40 g protein vs 147.2 g target should earn under 10 protein points");
        assertTrue(response.score() < 90, "low protein must keep the day below Excellent");
    }

    @Test
    void overeatingLosesAllCaloriePoints() {
        List<FoodIntakeRecord> records = List.of(
                entry(END, "LUNCH", "Biryani", 3000, 100.0, 300.0, 155.6));
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                records, WEIGHT_KG, 2000, END, 7);
        assertEquals(0.0, response.components().calories(),
                "3000 kcal vs a 2000 kcal target is +50%, so no calorie points");
        assertTrue(response.score() < 75);
    }

    @Test
    void singleMealDayLosesPatternPoints() {
        List<FoodIntakeRecord> records = List.of(
                entry(END, "BREAKFAST", "Dosa", 500, 10.0, 80.0, 15.0));
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                records, WEIGHT_KG, 2000, END, 7);
        assertEquals(6.0, response.components().pattern(),
                "one meal slot (4 pts) + one dish (2 pts)");
    }

    @Test
    void overallScoreIsMeanOfLoggedDays() {
        List<FoodIntakeRecord> records = new java.util.ArrayList<>(perfectDay(END.minusDays(1)));
        records.add(entry(END, "BREAKFAST", "Dosa", 500, 10.0, 80.0, 15.0));
        EatingHabitScoreResponse response = EatingHabitScoreService.compute(
                records, WEIGHT_KG, 2000, END, 7);
        assertEquals(2, response.daysLogged());
        assertEquals(2, response.dailyScores().size());
        assertEquals(64, response.score(), "mean of a 100 day and a ~28 day");
        assertEquals("Fair", response.label());
    }

    @Test
    void scoreForUserUsesLatestWeightAndWindow() {
        when(foodIntakeRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                eq(7L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(perfectDay(LocalDate.now()));
        DailyHealthRecord latest = new DailyHealthRecord();
        latest.setWeightKg(WEIGHT_KG);
        when(dailyHealthRecordRepository.findTopByUserIdAndWeightKgIsNotNullOrderByRecordDateDesc(7L))
                .thenReturn(Optional.of(latest));

        EatingHabitScoreResponse response = service.scoreForUser(7L, 7, 2000);
        assertEquals(100, response.score());
        assertEquals(147.2, response.proteinTargetG());
    }

    @Test
    void scoreForUserFallsBackTo120gProteinTargetWithoutWeight() {
        when(foodIntakeRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                eq(7L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(perfectDay(LocalDate.now()));
        when(dailyHealthRecordRepository.findTopByUserIdAndWeightKgIsNotNullOrderByRecordDateDesc(7L))
                .thenReturn(Optional.empty());

        EatingHabitScoreResponse response = service.scoreForUser(7L, 7, 2000);
        assertEquals(120.0, response.proteinTargetG());
        assertEquals(100, response.score());
    }
}

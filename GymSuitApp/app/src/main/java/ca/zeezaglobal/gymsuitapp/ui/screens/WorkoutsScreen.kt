package ca.zeezaglobal.gymsuitapp.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PREFS_NAME = "gymsuit_workout_plan"
private const val KEY_HAS_PLAN = "has_plan"
private const val KEY_DAYS_PER_WEEK = "days_per_week"
private const val KEY_SCHEDULED_DATES = "scheduled_dates"
private const val KEY_USER_WEIGHT = "user_body_weight_kg"

/**
 * Screen navigation modes for the Workout tab:
 * CALENDAR -> Monthly overview with pink tickmarks
 * PLAN_WIZARD_WEIGHT -> Ask weight if none recorded in app or Health Connect
 * PLAN_WIZARD_FREQUENCY -> Ask days per week
 * PLAN_WIZARD_DAYS -> Ask which days of the next 7 days
 * PLANNED_WORKOUTS_MAIN -> Main page with top bar sections matching days per week
 */
enum class WorkoutsViewMode {
    CALENDAR,
    PLAN_WIZARD_WEIGHT,
    PLAN_WIZARD_FREQUENCY,
    PLAN_WIZARD_DAYS,
    PLANNED_WORKOUTS_MAIN
}

data class PlannedExerciseItem(
    val name: String,
    val targetMuscle: String,
    val setsReps: String,
    val suggestedWeightKg: Double?,
    val note: String
)

data class PlannedDaySection(
    val dayNumber: Int,
    val title: String,
    val scheduledDate: LocalDate,
    val bodyParts: List<String>,
    val exercises: List<PlannedExerciseItem>
)

data class WorkoutPlanData(
    val daysPerWeek: Int,
    val bodyWeightKg: Double,
    val sections: List<PlannedDaySection>
)

@Composable
fun WorkoutsScreen(
    session: WorkoutSessionState = remember { WorkoutSessionState() }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val workoutStore = remember { WorkoutStore(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val planPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Screen State
    var currentViewMode by remember { mutableStateOf(WorkoutsViewMode.CALENDAR) }
    var isCheckingWeight by remember { mutableStateOf(false) }

    // Calendar State
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var completedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }

    // Plan Configuration Wizard State
    var userWeightKg by remember {
        val savedWeight = planPrefs.getFloat(KEY_USER_WEIGHT, 0f).toDouble()
        mutableDoubleStateOf(if (savedWeight > 20.0) savedWeight else 0.0)
    }
    var selectedDaysPerWeek by remember {
        val savedDays = planPrefs.getInt(KEY_DAYS_PER_WEEK, 4)
        mutableIntStateOf(savedDays.coerceIn(2, 6))
    }
    var selectedDatesList by remember { mutableStateOf<List<LocalDate>>(emptyList()) }
    var activePlan by remember { mutableStateOf<WorkoutPlanData?>(null) }
    var selectedSectionTabIndex by remember { mutableIntStateOf(0) }

    fun refreshCompletedDates() {
        val zone = ZoneId.systemDefault()
        val fromStore = workoutStore.getLoggedWorkouts().map {
            Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate()
        }.toSet()

        val prefs = context.getSharedPreferences("gymsuit_workouts", Context.MODE_PRIVATE)
        val fromPrefs = (prefs.getStringSet("completed_dates", emptySet()) ?: emptySet()).mapNotNull {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }.toSet()

        completedDates = fromStore + fromPrefs
    }

    // Load persisted plan on startup if present
    fun loadSavedPlan(): WorkoutPlanData? {
        val hasPlan = planPrefs.getBoolean(KEY_HAS_PLAN, false)
        if (!hasPlan) return null
        val daysCount = planPrefs.getInt(KEY_DAYS_PER_WEEK, 4)
        val weight = planPrefs.getFloat(KEY_USER_WEIGHT, 70f).toDouble()
        val rawDatesJson = planPrefs.getString(KEY_SCHEDULED_DATES, null)
        val dates = mutableListOf<LocalDate>()
        if (!rawDatesJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawDatesJson)
                for (i in 0 until arr.length()) {
                    dates.add(LocalDate.parse(arr.getString(i)))
                }
            } catch (_: Exception) {}
        }
        if (dates.isEmpty()) {
            val today = LocalDate.now()
            for (i in 0 until daysCount) {
                dates.add(today.plusDays(i.toLong()))
            }
        }
        return buildWorkoutPlan(daysCount, weight, dates)
    }

    LaunchedEffect(Unit) {
        refreshCompletedDates()
        try {
            if (healthConnectManager.isAvailable() && healthConnectManager.hasExercisePermission()) {
                val hcDates = healthConnectManager.readWorkoutDays()
                completedDates = completedDates + hcDates
            }
        } catch (_: Exception) {}

        // Check if a plan is already active
        activePlan = loadSavedPlan()
    }

    // Handler when user clicks "Plan Workouts" button
    fun startPlanningFlow() {
        coroutineScope.launch {
            isCheckingWeight = true
            var weightFound: Double? = null

            // 1. Check local saved plan weight
            val localWeight = planPrefs.getFloat(KEY_USER_WEIGHT, 0f).toDouble()
            if (localWeight > 25.0) {
                weightFound = localWeight
            }

            // 2. Check user_fitness_prefs if available
            if (weightFound == null) {
                val fitPrefs = context.getSharedPreferences("user_fitness_prefs", Context.MODE_PRIVATE)
                val fitWeight = fitPrefs.getFloat("user_body_weight_kg", 0f).toDouble()
                if (fitWeight > 25.0) {
                    weightFound = fitWeight
                }
            }

            // 3. Check Health Connect if available
            if (weightFound == null) {
                try {
                    if (healthConnectManager.isAvailable() && healthConnectManager.hasWeightPermission()) {
                        val hcWeight = healthConnectManager.readLatestWeight()
                        if (hcWeight != null && hcWeight > 25.0) {
                            weightFound = hcWeight
                        }
                    }
                } catch (_: Exception) {}
            }

            isCheckingWeight = false

            if (weightFound != null && weightFound > 25.0) {
                userWeightKg = weightFound
                // Body weight already recorded: advance directly to frequency question!
                currentViewMode = WorkoutsViewMode.PLAN_WIZARD_FREQUENCY
            } else {
                // No body weight recorded: ask for body weight
                currentViewMode = WorkoutsViewMode.PLAN_WIZARD_WEIGHT
            }
        }
    }

    // View Switching
    when (currentViewMode) {
        WorkoutsViewMode.CALENDAR -> {
            WorkoutsCalendarView(
                selectedYearMonth = selectedYearMonth,
                onYearMonthChange = { selectedYearMonth = it },
                completedDates = completedDates,
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it },
                activePlan = activePlan,
                isCheckingWeight = isCheckingWeight,
                onPlanWorkoutsClick = {
                    if (activePlan != null) {
                        currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
                    } else {
                        startPlanningFlow()
                    }
                },
                onRePlanClick = {
                    startPlanningFlow()
                }
            )
        }

        WorkoutsViewMode.PLAN_WIZARD_WEIGHT -> {
            BackHandler { currentViewMode = WorkoutsViewMode.CALENDAR }
            BodyWeightStepScreen(
                initialWeight = if (userWeightKg > 20.0) userWeightKg else 70.0,
                onBack = { currentViewMode = WorkoutsViewMode.CALENDAR },
                onContinue = { enteredWeight ->
                    userWeightKg = enteredWeight
                    planPrefs.edit().putFloat(KEY_USER_WEIGHT, enteredWeight.toFloat()).apply()
                    val fitPrefs = context.getSharedPreferences("user_fitness_prefs", Context.MODE_PRIVATE)
                    fitPrefs.edit().putFloat("user_body_weight_kg", enteredWeight.toFloat()).apply()
                    // Try writing to Health Connect if available
                    coroutineScope.launch {
                        try {
                            if (healthConnectManager.isAvailable()) {
                                healthConnectManager.insertWeight(enteredWeight)
                            }
                        } catch (_: Exception) {}
                    }
                    currentViewMode = WorkoutsViewMode.PLAN_WIZARD_FREQUENCY
                }
            )
        }

        WorkoutsViewMode.PLAN_WIZARD_FREQUENCY -> {
            BackHandler {
                val hasSavedWeight = planPrefs.getFloat(KEY_USER_WEIGHT, 0f) > 20f
                currentViewMode = if (!hasSavedWeight) WorkoutsViewMode.PLAN_WIZARD_WEIGHT else WorkoutsViewMode.CALENDAR
            }
            WorkoutFrequencyStepScreen(
                currentFrequency = selectedDaysPerWeek,
                onBack = {
                    val hasSavedWeight = planPrefs.getFloat(KEY_USER_WEIGHT, 0f) > 20f
                    currentViewMode = if (!hasSavedWeight) WorkoutsViewMode.PLAN_WIZARD_WEIGHT else WorkoutsViewMode.CALENDAR
                },
                onContinue = { frequency ->
                    selectedDaysPerWeek = frequency
                    // Reset selected dates to match new frequency
                    selectedDatesList = emptyList()
                    currentViewMode = WorkoutsViewMode.PLAN_WIZARD_DAYS
                }
            )
        }

        WorkoutsViewMode.PLAN_WIZARD_DAYS -> {
            BackHandler { currentViewMode = WorkoutsViewMode.PLAN_WIZARD_FREQUENCY }
            WorkoutDaysSelectionStepScreen(
                targetDaysCount = selectedDaysPerWeek,
                initialSelectedDates = selectedDatesList,
                onBack = { currentViewMode = WorkoutsViewMode.PLAN_WIZARD_FREQUENCY },
                onConfirmPlan = { chosenDates ->
                    selectedDatesList = chosenDates
                    val newPlan = buildWorkoutPlan(selectedDaysPerWeek, userWeightKg, chosenDates)
                    activePlan = newPlan

                    // Persist Plan
                    val datesJson = JSONArray()
                    chosenDates.forEach { datesJson.put(it.toString()) }
                    planPrefs.edit()
                        .putBoolean(KEY_HAS_PLAN, true)
                        .putInt(KEY_DAYS_PER_WEEK, selectedDaysPerWeek)
                        .putFloat(KEY_USER_WEIGHT, userWeightKg.toFloat())
                        .putString(KEY_SCHEDULED_DATES, datesJson.toString())
                        .apply()

                    selectedSectionTabIndex = 0
                    currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
                }
            )
        }

        WorkoutsViewMode.PLANNED_WORKOUTS_MAIN -> {
            BackHandler { currentViewMode = WorkoutsViewMode.CALENDAR }
            val plan = activePlan ?: buildWorkoutPlan(selectedDaysPerWeek, userWeightKg, selectedDatesList)
            PlannedWorkoutsMainScreen(
                plan = plan,
                selectedTabIndex = selectedSectionTabIndex,
                onTabSelect = { selectedSectionTabIndex = it },
                onBackToCalendar = { currentViewMode = WorkoutsViewMode.CALENDAR },
                onRePlan = { startPlanningFlow() },
                onStartWorkout = { daySection ->
                    session.start(daySection.bodyParts)
                }
            )
        }
    }
}

/**
 * Builds standard workout plan split routines customized to user bodyweight and selected days.
 */
private fun buildWorkoutPlan(
    daysCount: Int,
    weightKg: Double,
    dates: List<LocalDate>
): WorkoutPlanData {
    val effectiveWeight = if (weightKg > 20.0) weightKg else 70.0
    val sections = mutableListOf<PlannedDaySection>()

    for (i in 0 until daysCount) {
        val scheduledDate = dates.getOrNull(i) ?: LocalDate.now().plusDays(i.toLong())
        val dayNumber = i + 1

        val (title, bodyParts, exercises) = when (daysCount) {
            2 -> {
                if (i == 0) {
                    Triple(
                        "Full Body A (Chest, Back, Quads)",
                        listOf("chest", "back", "upper legs"),
                        listOf(
                            PlannedExerciseItem("Barbell Bench Press", "Chest", "4 sets × 8-10 reps", (effectiveWeight * 0.75).roundToHalf(), "Focus on controlled bar descent to mid-chest"),
                            PlannedExerciseItem("Lat Pulldown", "Back", "4 sets × 10-12 reps", (effectiveWeight * 0.65).roundToHalf(), "Squeeze lats at the bottom"),
                            PlannedExerciseItem("Barbell Full Squat", "Legs", "4 sets × 8-10 reps", (effectiveWeight * 0.90).roundToHalf(), "Maintain neutral spine and break parallel"),
                            PlannedExerciseItem("Overhead Dumbbell Press", "Shoulders", "3 sets × 10 reps", (effectiveWeight * 0.20).roundToHalf(), "Keep elbows at 45 degree angle"),
                            PlannedExerciseItem("Plank", "Core", "3 sets × 45-60s", null, "Keep core braced tight and glutes squeezed")
                        )
                    )
                } else {
                    Triple(
                        "Full Body B (Hamstrings, Shoulders, Arms)",
                        listOf("upper legs", "shoulders", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Deadlift", "Back/Legs", "4 sets × 6-8 reps", (effectiveWeight * 1.05).roundToHalf(), "Drive through heels, maintain neutral neck"),
                            PlannedExerciseItem("Incline Dumbbell Press", "Chest", "3 sets × 10 reps", (effectiveWeight * 0.25).roundToHalf(), "Target upper pectorals at 30 degree incline"),
                            PlannedExerciseItem("Dumbbell Biceps Curl", "Arms", "3 sets × 12 reps", (effectiveWeight * 0.15).roundToHalf(), "Supinate wrists at the peak contraction"),
                            PlannedExerciseItem("Triceps Pushdown", "Arms", "3 sets × 12 reps", (effectiveWeight * 0.30).roundToHalf(), "Lock elbows firmly at sides"),
                            PlannedExerciseItem("Hanging Knee Raise", "Core", "3 sets × 15 reps", null, "Control the swing and exhale on elevation")
                        )
                    )
                }
            }
            3 -> {
                when (i) {
                    0 -> Triple(
                        "Push (Chest, Shoulders & Triceps)",
                        listOf("chest", "shoulders", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Bench Press", "Chest", "4 sets × 8-10 reps", (effectiveWeight * 0.75).roundToHalf(), "Classic compound builder for upper torso"),
                            PlannedExerciseItem("Incline Dumbbell Press", "Chest", "3 sets × 10-12 reps", (effectiveWeight * 0.25).roundToHalf(), "Set bench to 30 degrees for upper chest"),
                            PlannedExerciseItem("Dumbbell Lateral Raise", "Shoulders", "4 sets × 12-15 reps", (effectiveWeight * 0.10).roundToHalf(), "Slight bend in elbows, lead with elbows"),
                            PlannedExerciseItem("Overhead Triceps Extension", "Triceps", "3 sets × 10-12 reps", (effectiveWeight * 0.22).roundToHalf(), "Deep stretch on the long head of triceps"),
                            PlannedExerciseItem("Cable Pushdown", "Triceps", "3 sets × 12 reps", (effectiveWeight * 0.30).roundToHalf(), "Full extension and lock out smoothly")
                        )
                    )
                    1 -> Triple(
                        "Pull (Back & Biceps)",
                        listOf("back", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Deadlift", "Back", "4 sets × 6 reps", (effectiveWeight * 1.10).roundToHalf(), "Heavy compound pull for entire posterior chain"),
                            PlannedExerciseItem("Barbell Bent-Over Row", "Back", "4 sets × 8-10 reps", (effectiveWeight * 0.60).roundToHalf(), "Pull bar toward lower abdomen"),
                            PlannedExerciseItem("Lat Pulldown", "Back", "3 sets × 10-12 reps", (effectiveWeight * 0.65).roundToHalf(), "Wide grip, pull towards upper chest"),
                            PlannedExerciseItem("Barbell Biceps Curl", "Biceps", "3 sets × 10 reps", (effectiveWeight * 0.35).roundToHalf(), "Strict form without swinging"),
                            PlannedExerciseItem("Hammer Curls", "Forearms/Biceps", "3 sets × 12 reps", (effectiveWeight * 0.16).roundToHalf(), "Neutral grip targeting the brachialis")
                        )
                    )
                    else -> Triple(
                        "Legs & Core",
                        listOf("upper legs", "waist"),
                        listOf(
                            PlannedExerciseItem("Barbell Full Squat", "Quads", "4 sets × 8-10 reps", (effectiveWeight * 0.90).roundToHalf(), "Deep squat with active knee tracking"),
                            PlannedExerciseItem("Romanian Deadlift", "Hamstrings", "4 sets × 10 reps", (effectiveWeight * 0.70).roundToHalf(), "Hinge at hips, feeling hamstring stretch"),
                            PlannedExerciseItem("Walking Dumbbell Lunges", "Quads/Glutes", "3 sets × 12 reps/leg", (effectiveWeight * 0.18).roundToHalf(), "Upright chest and steady strides"),
                            PlannedExerciseItem("Standing Calf Raise", "Calves", "4 sets × 15 reps", (effectiveWeight * 0.50).roundToHalf(), "2-second pause at top of each rep"),
                            PlannedExerciseItem("Hanging Leg Raise", "Core", "3 sets × 15 reps", null, "Raise knees/toes up to bar level")
                        )
                    )
                }
            }
            4 -> {
                when (i) {
                    0 -> Triple(
                        "Upper Body Strength (Chest & Back)",
                        listOf("chest", "back"),
                        listOf(
                            PlannedExerciseItem("Barbell Bench Press", "Chest", "4 sets × 6-8 reps", (effectiveWeight * 0.80).roundToHalf(), "Primary compound upper press"),
                            PlannedExerciseItem("Barbell Bent-Over Row", "Back", "4 sets × 8-10 reps", (effectiveWeight * 0.65).roundToHalf(), "Heavy back thickness builder"),
                            PlannedExerciseItem("Incline Dumbbell Press", "Chest", "3 sets × 10 reps", (effectiveWeight * 0.25).roundToHalf(), "Clavicular head development"),
                            PlannedExerciseItem("Lat Pulldown", "Back", "3 sets × 12 reps", (effectiveWeight * 0.65).roundToHalf(), "Back width builder"),
                            PlannedExerciseItem("Face Pulls", "Rear Delts", "3 sets × 15 reps", (effectiveWeight * 0.20).roundToHalf(), "Shoulder health and posture")
                        )
                    )
                    1 -> Triple(
                        "Lower Body Strength (Quads & Hamstrings)",
                        listOf("upper legs", "waist"),
                        listOf(
                            PlannedExerciseItem("Barbell Full Squat", "Quads", "4 sets × 6-8 reps", (effectiveWeight * 0.95).roundToHalf(), "Foundation for lower body power"),
                            PlannedExerciseItem("Romanian Deadlift", "Hamstrings", "4 sets × 8-10 reps", (effectiveWeight * 0.75).roundToHalf(), "Posterior chain hypertrophy"),
                            PlannedExerciseItem("Leg Press", "Quads", "3 sets × 12 reps", (effectiveWeight * 1.30).roundToHalf(), "Controlled eccentric movement"),
                            PlannedExerciseItem("Standing Calf Raise", "Calves", "4 sets × 15 reps", (effectiveWeight * 0.50).roundToHalf(), "Full extension and plantarflexion"),
                            PlannedExerciseItem("Plank Hold", "Core", "3 sets × 60s", null, "Anti-extension core stabilization")
                        )
                    )
                    2 -> Triple(
                        "Upper Body Hypertrophy (Shoulders & Arms)",
                        listOf("shoulders", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Seated Dumbbell Shoulder Press", "Shoulders", "4 sets × 10 reps", (effectiveWeight * 0.22).roundToHalf(), "Full shoulder development"),
                            PlannedExerciseItem("Lateral Raise", "Shoulders", "4 sets × 15 reps", (effectiveWeight * 0.10).roundToHalf(), "Strict medial deltoid isolation"),
                            PlannedExerciseItem("Dumbbell Biceps Curl", "Biceps", "3 sets × 12 reps", (effectiveWeight * 0.15).roundToHalf(), "Alternating arms with squeeze"),
                            PlannedExerciseItem("Skull Crushers", "Triceps", "3 sets × 12 reps", (effectiveWeight * 0.25).roundToHalf(), "Lower bar gently toward forehead"),
                            PlannedExerciseItem("Hammer Curls", "Forearms", "3 sets × 12 reps", (effectiveWeight * 0.16).roundToHalf(), "Brachioradialis emphasis")
                        )
                    )
                    else -> Triple(
                        "Lower Body & Core Hypertrophy",
                        listOf("upper legs", "waist"),
                        listOf(
                            PlannedExerciseItem("Dumbbell Goblet Squat", "Quads", "4 sets × 12 reps", (effectiveWeight * 0.35).roundToHalf(), "Vertical torso upright squat"),
                            PlannedExerciseItem("Lying Hamstring Curl", "Hamstrings", "4 sets × 12 reps", (effectiveWeight * 0.40).roundToHalf(), "Isolated knee flexion"),
                            PlannedExerciseItem("Dumbbell Walking Lunges", "Glutes/Quads", "3 sets × 12 reps/leg", (effectiveWeight * 0.18).roundToHalf(), "Dynamic leg drive"),
                            PlannedExerciseItem("Hanging Leg Raise", "Core", "4 sets × 15 reps", null, "Controlled lower abs workout"),
                            PlannedExerciseItem("Cable Woodchoppers", "Obliques", "3 sets × 15 reps/side", (effectiveWeight * 0.20).roundToHalf(), "Rotational core strength")
                        )
                    )
                }
            }
            5 -> {
                when (i) {
                    0 -> Triple(
                        "Chest & Triceps",
                        listOf("chest", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Bench Press", "Chest", "4 sets × 8-10 reps", (effectiveWeight * 0.75).roundToHalf(), "Main chest strength lift"),
                            PlannedExerciseItem("Incline Dumbbell Press", "Chest", "3 sets × 10-12 reps", (effectiveWeight * 0.25).roundToHalf(), "Upper chest hypertrophy"),
                            PlannedExerciseItem("Dumbbell Chest Flyes", "Chest", "3 sets × 12-15 reps", (effectiveWeight * 0.14).roundToHalf(), "Full pectoral stretch"),
                            PlannedExerciseItem("Triceps Rope Pushdown", "Triceps", "4 sets × 12 reps", (effectiveWeight * 0.28).roundToHalf(), "Spread rope at base"),
                            PlannedExerciseItem("Dips (or Bench Dips)", "Chest/Triceps", "3 sets × 12 reps", null, "Chest lean forward")
                        )
                    )
                    1 -> Triple(
                        "Back & Biceps",
                        listOf("back", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Deadlift", "Back", "4 sets × 6 reps", (effectiveWeight * 1.10).roundToHalf(), "Full posterior tension"),
                            PlannedExerciseItem("Lat Pulldown", "Back", "4 sets × 10 reps", (effectiveWeight * 0.65).roundToHalf(), "Controlled negative phase"),
                            PlannedExerciseItem("Seated Cable Row", "Back", "3 sets × 12 reps", (effectiveWeight * 0.60).roundToHalf(), "Pull into mid-torso"),
                            PlannedExerciseItem("Incline Dumbbell Curl", "Biceps", "3 sets × 12 reps", (effectiveWeight * 0.14).roundToHalf(), "Deep biceps stretch"),
                            PlannedExerciseItem("Preacher Curl", "Biceps", "3 sets × 10 reps", (effectiveWeight * 0.25).roundToHalf(), "Peak contraction isolation")
                        )
                    )
                    2 -> Triple(
                        "Legs & Calves",
                        listOf("upper legs"),
                        listOf(
                            PlannedExerciseItem("Barbell Full Squat", "Quads", "4 sets × 8 reps", (effectiveWeight * 0.90).roundToHalf(), "Core braced, deep depth"),
                            PlannedExerciseItem("Romanian Deadlift", "Hamstrings", "4 sets × 10 reps", (effectiveWeight * 0.70).roundToHalf(), "Feel stretch through glutes"),
                            PlannedExerciseItem("Leg Extension Machine", "Quads", "3 sets × 15 reps", (effectiveWeight * 0.45).roundToHalf(), "Pause at maximum extension"),
                            PlannedExerciseItem("Standing Calf Raise", "Calves", "4 sets × 15 reps", (effectiveWeight * 0.50).roundToHalf(), "High repetitions for calves"),
                            PlannedExerciseItem("Walking Lunges", "Glutes", "3 sets × 12 reps/leg", (effectiveWeight * 0.18).roundToHalf(), "Steady balance")
                        )
                    )
                    3 -> Triple(
                        "Shoulders & Traps",
                        listOf("shoulders"),
                        listOf(
                            PlannedExerciseItem("Overhead Barbell Press", "Shoulders", "4 sets × 8 reps", (effectiveWeight * 0.45).roundToHalf(), "Strict military press overhead"),
                            PlannedExerciseItem("Dumbbell Lateral Raise", "Shoulders", "4 sets × 15 reps", (effectiveWeight * 0.10).roundToHalf(), "Side delt width builder"),
                            PlannedExerciseItem("Reverse Pec Deck Fly", "Rear Delts", "4 sets × 15 reps", (effectiveWeight * 0.30).roundToHalf(), "Upper back and rear delts"),
                            PlannedExerciseItem("Dumbbell Shrugs", "Traps", "4 sets × 12 reps", (effectiveWeight * 0.35).roundToHalf(), "Elevate shoulders straight up"),
                            PlannedExerciseItem("Front Dumbbell Raise", "Shoulders", "3 sets × 12 reps", (effectiveWeight * 0.12).roundToHalf(), "Anterior delt isolation")
                        )
                    )
                    else -> Triple(
                        "Core & Conditioning",
                        listOf("waist", "cardio"),
                        listOf(
                            PlannedExerciseItem("Hanging Knee-to-Elbow", "Core", "4 sets × 12 reps", null, "Full abdominal contraction"),
                            PlannedExerciseItem("Cable Russian Twist", "Core", "3 sets × 15 reps", (effectiveWeight * 0.20).roundToHalf(), "Rotational torque on obliques"),
                            PlannedExerciseItem("Plank to Pushup", "Core/Shoulders", "3 sets × 10 reps", null, "Core dynamic stability"),
                            PlannedExerciseItem("Ab Wheel Rollout", "Core", "3 sets × 10 reps", null, "Deep lat and core anti-extension"),
                            PlannedExerciseItem("High Knees / Jump Rope", "Cardio", "4 rounds × 60s", null, "Endurance finisher")
                        )
                    )
                }
            }
            else -> {
                // 6 Days PPL x 2
                val isPPLSecondHalf = i >= 3
                val partSuffix = if (isPPLSecondHalf) "B" else "A"
                when (i % 3) {
                    0 -> Triple(
                        "Push $partSuffix (Chest, Shoulders & Triceps)",
                        listOf("chest", "shoulders", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Bench Press", "Chest", "4 sets × 8 reps", (effectiveWeight * 0.75).roundToHalf(), "Compound push power"),
                            PlannedExerciseItem("Incline Dumbbell Press", "Chest", "3 sets × 10 reps", (effectiveWeight * 0.25).roundToHalf(), "Upper chest development"),
                            PlannedExerciseItem("Dumbbell Lateral Raise", "Shoulders", "4 sets × 15 reps", (effectiveWeight * 0.10).roundToHalf(), "Strict medial delt isolate"),
                            PlannedExerciseItem("Cable Triceps Pushdown", "Triceps", "4 sets × 12 reps", (effectiveWeight * 0.30).roundToHalf(), "Triceps lateral head"),
                            PlannedExerciseItem("Dips", "Chest/Triceps", "3 sets × 10 reps", null, "Bodyweight finisher")
                        )
                    )
                    1 -> Triple(
                        "Pull $partSuffix (Back, Rear Delts & Biceps)",
                        listOf("back", "upper arms"),
                        listOf(
                            PlannedExerciseItem("Barbell Deadlift", "Back", "4 sets × 6 reps", (effectiveWeight * 1.10).roundToHalf(), "Posterior chain foundation"),
                            PlannedExerciseItem("Lat Pulldown", "Back", "4 sets × 10 reps", (effectiveWeight * 0.65).roundToHalf(), "Lat width builder"),
                            PlannedExerciseItem("Barbell Row", "Back", "3 sets × 10 reps", (effectiveWeight * 0.60).roundToHalf(), "Back thickness"),
                            PlannedExerciseItem("Face Pulls", "Rear Delts", "4 sets × 15 reps", (effectiveWeight * 0.20).roundToHalf(), "Scapular retraction"),
                            PlannedExerciseItem("Dumbbell Biceps Curl", "Biceps", "4 sets × 12 reps", (effectiveWeight * 0.15).roundToHalf(), "Full arm peak")
                        )
                    )
                    else -> Triple(
                        "Legs & Core $partSuffix",
                        listOf("upper legs", "waist"),
                        listOf(
                            PlannedExerciseItem("Barbell Full Squat", "Quads", "4 sets × 8 reps", (effectiveWeight * 0.90).roundToHalf(), "Main leg drive"),
                            PlannedExerciseItem("Romanian Deadlift", "Hamstrings", "4 sets × 10 reps", (effectiveWeight * 0.70).roundToHalf(), "Deep hamstring stretch"),
                            PlannedExerciseItem("Leg Press", "Quads", "3 sets × 12 reps", (effectiveWeight * 1.30).roundToHalf(), "Quad burnout"),
                            PlannedExerciseItem("Standing Calf Raise", "Calves", "4 sets × 15 reps", (effectiveWeight * 0.50).roundToHalf(), "Full calves flex"),
                            PlannedExerciseItem("Hanging Leg Raise", "Core", "3 sets × 15 reps", null, "Abdominal definition")
                        )
                    )
                }
            }
        }

        sections.add(
            PlannedDaySection(
                dayNumber = dayNumber,
                title = title,
                scheduledDate = scheduledDate,
                bodyParts = bodyParts,
                exercises = exercises
            )
        )
    }

    return WorkoutPlanData(
        daysPerWeek = daysCount,
        bodyWeightKg = effectiveWeight,
        sections = sections
    )
}

private fun Double.roundToHalf(): Double = Math.round(this * 2.0) / 2.0

// MARK: - Calendar Screen with Material You 3 "Plan Workouts" Button

@Composable
fun WorkoutsCalendarView(
    selectedYearMonth: YearMonth,
    onYearMonthChange: (YearMonth) -> Unit,
    completedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    activePlan: WorkoutPlanData?,
    isCheckingWeight: Boolean,
    onPlanWorkoutsClick: () -> Unit,
    onRePlanClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Row with Title & Month Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Workouts",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onYearMonthChange(selectedYearMonth.minusMonths(1)) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = Color(0xFF475569)
                        )
                    }
                    IconButton(
                        onClick = { onYearMonthChange(selectedYearMonth.plusMonths(1)) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // Monthly Workout Calendar Card (matches the uploaded design with pink circles and checkmarks)
        item {
            MonthlyWorkoutCalendarCard(
                yearMonth = selectedYearMonth,
                completedDates = completedDates,
                selectedDate = selectedDate,
                onSelectDate = onSelectDate
            )
        }

        // Active Plan Banner if plan already generated
        if (activePlan != null) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Workout Plan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${activePlan.daysPerWeek} Days / Week • ${activePlan.sections.size} Structured Routines",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        OutlinedButton(
                            onClick = onRePlanClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Re-plan", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Material You 3 "Plan Workouts" Button
        item {
            Button(
                onClick = onPlanWorkoutsClick,
                enabled = !isCheckingWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                if (isCheckingWeight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (activePlan != null) "View Workout Plan" else "Plan Workouts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Daily Summary / Selected Date Detail Card
        item {
            val isDateCompleted = selectedDate in completedDates
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDateCompleted) Color(0xFFFDF2F8) else Color(0xFFF1F5F9),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isDateCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Completed",
                                    tint = Color(0xFFBE185D),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (isDateCompleted) "Workout session completed" else "No workouts recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDateCompleted) Color(0xFFBE185D) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Step 1: Body Weight Input Screen

@Composable
fun BodyWeightStepScreen(
    initialWeight: Double,
    onBack: () -> Unit,
    onContinue: (Double) -> Unit
) {
    var weightText by remember { mutableStateOf(if (initialWeight > 20.0) initialWeight.toInt().toString() else "70") }
    val isValid = (weightText.toDoubleOrNull() ?: 0.0) in 25.0..300.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }
            Text(
                text = "Step 1: Body Weight",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Graphic / Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "What is your body weight?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "We use your weight to calculate personalized exercise volume, suggested resistance, and calorie burn.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weight Input Card
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { input ->
                                if (input.length <= 5 && input.all { it.isDigit() || it == '.' }) {
                                    weightText = input
                                }
                            },
                            textStyle = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.width(140.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Recommended range: 30 - 250 kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Continue Button
        Button(
            onClick = {
                val weight = weightText.toDoubleOrNull()
                if (weight != null && weight in 25.0..300.0) {
                    onContinue(weight)
                }
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// MARK: - Step 2: Workout Frequency Screen ("How many days a week will you workout?")

@Composable
fun WorkoutFrequencyStepScreen(
    currentFrequency: Int,
    onBack: () -> Unit,
    onContinue: (Int) -> Unit
) {
    var selectedDays by remember { mutableIntStateOf(currentFrequency) }

    val options = listOf(
        Triple(2, "2 Days / Week", "Light maintenance & full body stimulation"),
        Triple(3, "3 Days / Week", "Classic Push / Pull / Legs split (Recommended)"),
        Triple(4, "4 Days / Week", "Upper / Lower split with optimal rest days"),
        Triple(5, "5 Days / Week", "Dedicated body part focus & hypertrophy"),
        Triple(6, "6 Days / Week", "High volume Push / Pull / Legs rotation")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }
            Text(
                text = "Step 2: Training Frequency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "How many days a week will you workout?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Choose a realistic frequency you can stick with consistently throughout the week.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Options List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (days, label, subtitle) ->
                    val isSelected = selectedDays == days
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                        shadowElevation = if (isSelected) 0.dp else 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDays = days }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Continue Button
        Button(
            onClick = { onContinue(selectedDays) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// MARK: - Step 3: Days of the Next 7 Days Selection Screen

@Composable
fun WorkoutDaysSelectionStepScreen(
    targetDaysCount: Int,
    initialSelectedDates: List<LocalDate>,
    onBack: () -> Unit,
    onConfirmPlan: (List<LocalDate>) -> Unit
) {
    val today = remember { LocalDate.now() }
    val nextSevenDays = remember { (0..6).map { today.plusDays(it.toLong()) } }

    var selectedDates by remember {
        mutableStateOf(
            if (initialSelectedDates.isNotEmpty() && initialSelectedDates.size == targetDaysCount) {
                initialSelectedDates.toSet()
            } else {
                nextSevenDays.take(targetDaysCount).toSet()
            }
        )
    }

    val isComplete = selectedDates.size == targetDaysCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A)
                )
            }
            Text(
                text = "Step 3: Select Days",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Which days of the next seven days?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select exactly $targetDaysCount days you plan to workout:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
                Surface(
                    shape = CircleShape,
                    color = if (isComplete) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "${selectedDates.size} / $targetDaysCount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete) Color(0xFF16A34A) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Next 7 Days Selection List
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                nextSevenDays.forEach { date ->
                    val isSelected = date in selectedDates
                    val isToday = date == today
                    val weekdayFormatted = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
                    val dateFormatted = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                        shadowElevation = if (isSelected) 0.dp else 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    selectedDates = selectedDates - date
                                } else {
                                    if (selectedDates.size < targetDaysCount) {
                                        selectedDates = selectedDates + date
                                    }
                                }
                            }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.CalendarMonth,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = weekdayFormatted,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF0F172A)
                                        )
                                        if (isToday) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Today",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Color(0xFF64748B)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Confirm / Generate Plan Button
        Button(
            onClick = {
                val sorted = selectedDates.toList().sorted()
                onConfirmPlan(sorted)
            },
            enabled = isComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = if (isComplete) "Generate Workout Plan" else "Select $targetDaysCount Days (${selectedDates.size}/$targetDaysCount)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// MARK: - Step 4: The Main Planned Workouts Page with Top Bar Sections

@Composable
fun PlannedWorkoutsMainScreen(
    plan: WorkoutPlanData,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onBackToCalendar: () -> Unit,
    onRePlan: () -> Unit,
    onStartWorkout: (PlannedDaySection) -> Unit
) {
    val activeSection = plan.sections.getOrNull(selectedTabIndex) ?: plan.sections.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top App Bar
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onBackToCalendar) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back to Calendar",
                                tint = Color(0xFF0F172A)
                            )
                        }

                        Column {
                            Text(
                                text = "Weekly Workout Plan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${plan.daysPerWeek} Days / Week • Body Weight: ${plan.bodyWeightKg.toInt()} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onRePlan) {
                        Icon(
                            imageVector = Icons.Outlined.EditCalendar,
                            contentDescription = "Edit Plan",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Top Bar Sections (Tabs): Number of sections equals the number of days user works out!
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex.coerceIn(0, (plan.sections.size - 1).coerceAtLeast(0)),
                    edgePadding = 16.dp,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    plan.sections.forEachIndexed { index, section ->
                        val isSelected = index == selectedTabIndex
                        val dayLabel = "Day ${section.dayNumber}"
                        val dateLabel = section.scheduledDate.format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()))

                        Tab(
                            selected = isSelected,
                            onClick = { onTabSelect(index) },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = dayLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = dateLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section Content
        if (activeSection != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Overview Card
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Section ${activeSection.dayNumber} of ${plan.sections.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = activeSection.scheduledDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = activeSection.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "${activeSection.exercises.size} Exercises • ~50 mins • Weights tailored to ${plan.bodyWeightKg.toInt()} kg BW",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Exercises List Header
                item {
                    Text(
                        text = "Scheduled Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                // Exercise Cards
                items(activeSection.exercises) { exercise ->
                    PlannedExerciseCard(exercise = exercise)
                }

                // Material You 3 "Start Workout" Button
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onStartWorkout(activeSection) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Start Day ${activeSection.dayNumber} Workout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlannedExerciseCard(exercise: PlannedExerciseItem) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exercise.targetMuscle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = exercise.setsReps,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (exercise.suggestedWeightKg != null || exercise.note.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (exercise.suggestedWeightKg != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Suggested: ${exercise.suggestedWeightKg} kg",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    if (exercise.note.isNotBlank()) {
                        Text(
                            text = exercise.note,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Monthly calendar card rendering weekdays (M, T, W, T, F, S, S)
 * and highlighting completed dates with a light-pink circle & checkmark.
 */
@Composable
fun MonthlyWorkoutCalendarCard(
    yearMonth: YearMonth,
    completedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
    val daysInMonth = yearMonth.lengthOfMonth()
    // Monday = 1 ... Sunday = 7
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val leadingEmptyDays = firstDayOfWeek - 1

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Weekday Headers (M, T, W, T, F, S, S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                daysOfWeek.forEach { dayLabel ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Calendar Grid of Days
            val totalCells = leadingEmptyDays + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - leadingEmptyDays + 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val date = yearMonth.atDay(dayNumber)
                                    val isCompleted = date in completedDates
                                    val isSelected = date == selectedDate

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .then(
                                                when {
                                                    isCompleted -> Modifier.background(Color(0xFFFDF2F8)) // Soft light pink
                                                    isSelected -> Modifier.background(Color(0xFFF1F5F9))
                                                    else -> Modifier
                                                }
                                            )
                                            .clickable { onSelectDate(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            // Magenta/pink checkmark replacing the day number (matching reference)
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Completed on day $dayNumber",
                                                tint = Color(0xFFBE185D),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        } else {
                                            Text(
                                                text = dayNumber.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0F172A),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package ca.zeezaglobal.gymsuitapp.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMedia
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMuscleGroups
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Photo

private const val PREFS_NAME = "gymsuit_workout_plan"
private const val KEY_SELECTED_WORKOUTS = "selected_workouts_json"

/** Planned dates set (the days the user chose in the days step). */
private const val KEY_PLANNED_DATES = "planned_dates_json"
/** Legacy keys from removed flows; cleared on load. */
private const val KEY_HAS_PLAN = "has_plan"
/** Frequency step was removed; its key is dead and cleared on load. */
private const val KEY_DAYS_PER_WEEK = "days_per_week"
private const val KEY_SCHEDULED_DATES = "scheduled_dates"
private const val KEY_USER_WEIGHT = "user_body_weight_kg"

/** Default sets/reps for a workout picked from the exercise catalog. */
private const val DEFAULT_PLANNED_SETS_REPS = "3 sets × 10 reps"

/** Parses "N sets × M reps" back to (sets, reps); anything else -> the default (3, 10). */
private fun parseSetsReps(setsReps: String): Pair<Int, Int> {
    val numbers = Regex("\\d+").findAll(setsReps).mapNotNull { it.value.toIntOrNull() }.toList()
    val sets = numbers.getOrNull(0)?.takeIf { it in 1..100 } ?: 3
    val reps = numbers.getOrNull(1)?.takeIf { it in 1..100 } ?: 10
    return sets to reps
}

private fun formatSetsReps(sets: Int, reps: Int): String = "$sets sets × $reps reps"

/**
 * Screen navigation modes for the Workout tab:
 * CALENDAR -> Monthly overview; planned days are marked, tapping a day shows its planned workouts
 * PLAN_EDITOR -> Search + multi-select workouts for one day, then Save
 * SETUP_WEIGHT -> Body-weight step, asked first in the plan flow when not yet recorded
 * SELECT_DAYS -> Next-7-days selection, asked in the plan flow after weight
 * PLANNED_WORKOUTS_MAIN -> Plan page: one tab per planned date, each with its selected workouts
 */
enum class WorkoutsViewMode {
    CALENDAR,
    PLAN_EDITOR,
    SETUP_WEIGHT,
    SELECT_DAYS,
    PLANNED_WORKOUTS_MAIN
}

data class PlannedExerciseItem(
    val name: String,
    val targetMuscle: String,
    val setsReps: String,
    val suggestedWeightKg: Double?,
    val note: String,
    /** Catalog body part (e.g. "chest") — used to start a session from this day. */
    val bodyPart: String = ""
)

data class PlannedDaySection(
    val dayNumber: Int,
    val title: String,
    val scheduledDate: LocalDate,
    val bodyParts: List<String>,
    val exercises: List<PlannedExerciseItem>
)

/** A plan is exactly the workouts the user selected, keyed by date. */
data class WorkoutPlanData(
    val sections: List<PlannedDaySection>
) {
    val totalExercises: Int get() = sections.sumOf { it.exercises.size }
}

/** Catalog exercise -> a planned item with the default sets/reps. */
private fun Exercise.toPlannedItem(): PlannedExerciseItem = PlannedExerciseItem(
    name = name,
    targetMuscle = primaryMuscleLabel,
    setsReps = DEFAULT_PLANNED_SETS_REPS,
    suggestedWeightKg = null,
    note = "",
    bodyPart = bodyPart
)

/**
 * Builds the plan page sections from the planned dates (sorted), each with
 * that day's selected workouts (possibly empty — empty days get an empty tab).
 */
private fun buildPlanFromSelections(
    plannedDates: Set<LocalDate>,
    selections: Map<LocalDate, List<PlannedExerciseItem>>
): WorkoutPlanData {
    val sections = plannedDates.sorted().mapIndexed { index, date ->
        val exercises = selections[date].orEmpty()
        PlannedDaySection(
            dayNumber = index + 1,
            title = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
            scheduledDate = date,
            bodyParts = exercises.mapNotNull { it.bodyPart.takeIf { part -> part.isNotBlank() } }.distinct(),
            exercises = exercises
        )
    }
    return WorkoutPlanData(sections)
}

/** Serializes per-day selections: {"2026-10-20": [{name, muscle, setsReps, weight, note, bodyPart}, ...]}. */
private fun selectionsToJson(selections: Map<LocalDate, List<PlannedExerciseItem>>): String {
    val root = JSONObject()
    selections.forEach { (date, items) ->
        if (items.isNotEmpty()) {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(JSONObject().apply {
                    put("name", item.name)
                    put("muscle", item.targetMuscle)
                    put("setsReps", item.setsReps)
                    put("weight", item.suggestedWeightKg ?: JSONObject.NULL)
                    put("note", item.note)
                    put("bodyPart", item.bodyPart)
                })
            }
            root.put(date.toString(), arr)
        }
    }
    return root.toString()
}

/** Parses [selectionsToJson] output; malformed or missing data yields an empty plan (never a crash). */
private fun selectionsFromJson(raw: String?): Map<LocalDate, List<PlannedExerciseItem>> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        val root = JSONObject(raw)
        val out = mutableMapOf<LocalDate, List<PlannedExerciseItem>>()
        root.keys().forEach { key ->
            val date = try { LocalDate.parse(key) } catch (_: Exception) { null } ?: return@forEach
            val arr = root.optJSONArray(key) ?: return@forEach
            val items = List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                PlannedExerciseItem(
                    name = o.optString("name"),
                    targetMuscle = o.optString("muscle"),
                    setsReps = o.optString("setsReps", DEFAULT_PLANNED_SETS_REPS),
                    suggestedWeightKg = if (o.isNull("weight")) null else o.optDouble("weight"),
                    note = o.optString("note"),
                    bodyPart = o.optString("bodyPart")
                )
            }.filter { it.name.isNotBlank() }
            if (items.isNotEmpty()) out[date] = items
        }
        out
    } catch (_: Exception) {
        emptyMap()
    }
}

/** Serializes the planned dates set: ["2026-10-20", ...] (sorted). */
private fun plannedDatesToJson(dates: Set<LocalDate>): String {
    val arr = JSONArray()
    dates.sorted().forEach { arr.put(it.toString()) }
    return arr.toString()
}

/** Parses [plannedDatesToJson] output; malformed data yields an empty set (never a crash). */
private fun plannedDatesFromJson(raw: String?): Set<LocalDate> {
    if (raw.isNullOrBlank()) return emptySet()
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            try { LocalDate.parse(arr.getString(i)) } catch (_: Exception) { null }
        }.filterNotNull().toSet()
    } catch (_: Exception) {
        emptySet()
    }
}

@Composable
fun WorkoutsScreen(
    session: WorkoutSessionState = remember { WorkoutSessionState() }
) {
    val context = LocalContext.current
    val workoutStore = remember { WorkoutStore(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val planPrefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // Screen State
    var currentViewMode by remember { mutableStateOf(WorkoutsViewMode.CALENDAR) }

    // Calendar State
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var completedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }

    // The plan: exactly the workouts the user selected, keyed by date.
    var selections by remember { mutableStateOf<Map<LocalDate, List<PlannedExerciseItem>>>(emptyMap()) }
    var selectedSectionTabIndex by remember { mutableIntStateOf(0) }

    // Day currently open in the editor, and where Back should return to.
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    var editorReturnToPlan by remember { mutableStateOf(false) }

    // The planned dates: the days the user chose in the days step. The plan
    // page shows one tab per planned date, including dates with no workouts yet.
    // Invariant: selections keys are always a subset of plannedDates.
    var plannedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }

    // Body weight, asked first in the plan flow. Null = not yet recorded.
    var setupWeightKg by remember { mutableStateOf<Double?>(null) }
    // True when the weight step was opened from the plan page (tune icon):
    // Back/Continue from the weight step returns to the plan page.
    var setupReturnToPlan by remember { mutableStateOf(false) }
    // True when the days step was reached via the weight step (Back goes to weight).
    var daysBackToWeight by remember { mutableStateOf(false) }

    val activePlan = buildPlanFromSelections(plannedDates, selections)

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

    // Loads the saved plan + weight on startup. Dead legacy keys (has_plan,
    // scheduled_dates, days_per_week) are dropped. Plans saved before the
    // planned-dates set existed derive it from their workout dates.
    fun loadSavedSelections() {
        val loadedSelections = selectionsFromJson(planPrefs.getString(KEY_SELECTED_WORKOUTS, null))
        val loadedDates = if (planPrefs.contains(KEY_PLANNED_DATES)) {
            plannedDatesFromJson(planPrefs.getString(KEY_PLANNED_DATES, null))
        } else {
            loadedSelections.keys
        }
        plannedDates = loadedDates
        selections = loadedSelections.filterKeys { it in loadedDates }
        setupWeightKg = planPrefs.getFloat(KEY_USER_WEIGHT, 0f).toDouble().takeIf { it > 0 }
        if (planPrefs.contains(KEY_HAS_PLAN) || planPrefs.contains(KEY_SCHEDULED_DATES) ||
            planPrefs.contains(KEY_DAYS_PER_WEEK)
        ) {
            planPrefs.edit()
                .remove(KEY_HAS_PLAN)
                .remove(KEY_SCHEDULED_DATES)
                .remove(KEY_DAYS_PER_WEEK)
                .apply()
        }
    }

    /** Persists the planned dates + per-day selections together. */
    fun persistPlan() {
        planPrefs.edit()
            .putString(KEY_PLANNED_DATES, plannedDatesToJson(plannedDates))
            .putString(KEY_SELECTED_WORKOUTS, selectionsToJson(selections))
            .apply()
    }

    /**
     * Starts the plan flow: body weight first (skipped when already recorded),
     * then the next-7-days selection, then the plan page.
     */
    fun startPlanFlow() {
        setupReturnToPlan = false
        if (setupWeightKg == null) {
            currentViewMode = WorkoutsViewMode.SETUP_WEIGHT
        } else {
            daysBackToWeight = false
            currentViewMode = WorkoutsViewMode.SELECT_DAYS
        }
    }

    /** Re-opens the weight step from the plan page's tune icon. */
    fun openSetupFromPlanPage() {
        setupReturnToPlan = true
        currentViewMode = WorkoutsViewMode.SETUP_WEIGHT
    }

    /**
     * Confirms the days selection: the planned set keeps dates outside the
     * next 7 days and takes the chosen dates inside it; workouts for
     * de-selected dates are dropped.
     */
    fun confirmPlannedDays(dates: List<LocalDate>) {
        val today = LocalDate.now()
        val nextSevenDays = (0..6).map { today.plusDays(it.toLong()) }.toSet()
        val newDates = (plannedDates - nextSevenDays) + dates.toSet()
        plannedDates = newDates
        selections = selections.filterKeys { it in newDates }
        persistPlan()
        selectedSectionTabIndex = 0
        currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
    }

    /** Opens the editor for a day. `returnToPlan` keeps Back on the plan page. */
    fun openEditor(date: LocalDate, returnToPlan: Boolean) {
        editingDate = date
        editorReturnToPlan = returnToPlan
        currentViewMode = WorkoutsViewMode.PLAN_EDITOR
    }

    fun closeEditor() {
        editingDate = null
        currentViewMode = if (editorReturnToPlan) {
            WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
        } else {
            WorkoutsViewMode.CALENDAR
        }
    }

    /** Saves one day's selection and shows the plan page on that day's tab. */
    fun saveDayPlan(date: LocalDate, items: List<PlannedExerciseItem>) {
        val updatedSelections = selections.toMutableMap()
        if (items.isEmpty()) {
            updatedSelections.remove(date)
            plannedDates = plannedDates - date
        } else {
            updatedSelections[date] = items
            plannedDates = plannedDates + date
        }
        selections = updatedSelections
        persistPlan()
        selectedSectionTabIndex = plannedDates.sorted().indexOf(date).coerceAtLeast(0)
        editingDate = null
        currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
    }

    LaunchedEffect(Unit) {
        refreshCompletedDates()
        try {
            if (healthConnectManager.isAvailable() && healthConnectManager.hasExercisePermission()) {
                val hcDates = healthConnectManager.readWorkoutDays()
                completedDates = completedDates + hcDates
            }
        } catch (_: Exception) {}

        loadSavedSelections()
    }

    // View Switching, animated: screens fade + slide in as the user moves
    // through the flow (calendar <-> plan <-> editor <-> steps).
    AnimatedContent(
        targetState = currentViewMode,
        transitionSpec = {
            (fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                animationSpec = tween(250),
                initialOffsetX = { it / 8 }
            )) togetherWith (fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                animationSpec = tween(200),
                targetOffsetX = { -it / 8 }
            ))
        },
        label = "workouts-view"
    ) { viewMode ->
        when (viewMode) {
        WorkoutsViewMode.CALENDAR -> {
            WorkoutsCalendarView(
                selectedYearMonth = selectedYearMonth,
                onYearMonthChange = { selectedYearMonth = it },
                completedDates = completedDates,
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it },
                activePlan = activePlan.takeIf { it.sections.isNotEmpty() },
                plannedDates = plannedDates,
                plannedWorkoutsForSelectedDate = selections[selectedDate].orEmpty(),
                onPlanWorkoutsClick = { startPlanFlow() },
                onViewPlanClick = { currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN },
                onEditDayPlan = { date -> openEditor(date, returnToPlan = false) }
            )
        }

        WorkoutsViewMode.PLAN_EDITOR -> {
            val date = editingDate ?: selectedDate
            PlanWorkoutPickerScreen(
                date = date,
                initialSelection = selections[date].orEmpty(),
                workoutStore = workoutStore,
                onBack = { closeEditor() },
                onSave = { items -> saveDayPlan(date, items) }
            )
        }

        WorkoutsViewMode.SETUP_WEIGHT -> {
            BackHandler {
                currentViewMode = if (setupReturnToPlan) WorkoutsViewMode.PLANNED_WORKOUTS_MAIN else WorkoutsViewMode.CALENDAR
            }
            BodyWeightStepScreen(
                initialWeight = setupWeightKg ?: 0.0,
                onBack = {
                    currentViewMode = if (setupReturnToPlan) WorkoutsViewMode.PLANNED_WORKOUTS_MAIN else WorkoutsViewMode.CALENDAR
                },
                onContinue = { weight ->
                    setupWeightKg = weight
                    planPrefs.edit().putFloat(KEY_USER_WEIGHT, weight.toFloat()).apply()
                    if (setupReturnToPlan) {
                        currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
                    } else {
                        daysBackToWeight = true
                        currentViewMode = WorkoutsViewMode.SELECT_DAYS
                    }
                }
            )
        }

        WorkoutsViewMode.SELECT_DAYS -> {
            WorkoutDaysSelectionStepScreen(
                initialSelectedDates = plannedDates.toList(),
                onBack = {
                    currentViewMode = if (daysBackToWeight) {
                        WorkoutsViewMode.SETUP_WEIGHT
                    } else {
                        WorkoutsViewMode.CALENDAR
                    }
                },
                onConfirmPlan = { dates -> confirmPlannedDays(dates) }
            )
        }

        WorkoutsViewMode.PLANNED_WORKOUTS_MAIN -> {
            BackHandler { currentViewMode = WorkoutsViewMode.CALENDAR }
            PlannedWorkoutsMainScreen(
                plan = activePlan,
                selectedTabIndex = selectedSectionTabIndex,
                onTabSelect = { selectedSectionTabIndex = it },
                onBackToCalendar = { currentViewMode = WorkoutsViewMode.CALENDAR },
                onEditDay = { date -> openEditor(date, returnToPlan = true) },
                onStartWorkout = { daySection ->
                    session.start(daySection.bodyParts)
                },
                bodyWeightKg = setupWeightKg,
                onEditSetup = { openSetupFromPlanPage() }
            )
        }
        }
    }
}

// MARK: - Calendar Screen with Material You 3 "Plan Workouts" Button

@Composable
fun WorkoutsCalendarView(
    selectedYearMonth: YearMonth,
    onYearMonthChange: (YearMonth) -> Unit,
    completedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    activePlan: WorkoutPlanData?,
    plannedDates: Set<LocalDate>,
    plannedWorkoutsForSelectedDate: List<PlannedExerciseItem>,
    onPlanWorkoutsClick: () -> Unit,
    onViewPlanClick: () -> Unit,
    onEditDayPlan: (LocalDate) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
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
                onSelectDate = onSelectDate,
                plannedDates = plannedDates
            )
        }

        // Active Plan Banner if plan already generated
        item {
            AnimatedVisibility(
                visible = activePlan != null,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                activePlan?.let { plan ->
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
                                    text = "${plan.sections.size} planned days • ${plan.totalExercises} workouts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            OutlinedButton(
                                onClick = onViewPlanClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("View plan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Material You 3 "Plan Workouts" Button
        item {
            Button(
                onClick = onPlanWorkoutsClick,
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
                        text = "Plan Workouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Selected Date Detail Card: this day's planned workouts + completion state
        item {
            val plannedForDay = plannedWorkoutsForSelectedDate
            val isDateCompleted = selectedDate in completedDates
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isDateCompleted -> Color(0xFFFDF2F8)
                                plannedForDay.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color(0xFFF1F5F9)
                            },
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
                                        tint = if (plannedForDay.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
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
                                text = if (plannedForDay.isNotEmpty()) {
                                    "${plannedForDay.size} planned workouts"
                                } else {
                                    "No workouts planned"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (plannedForDay.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                            )
                            if (isDateCompleted) {
                                Text(
                                    text = "Workout session completed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFBE185D)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = plannedForDay.isNotEmpty(),
                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            plannedForDay.take(3).forEach { item ->
                                Text(
                                    text = "•  ${item.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF334155)
                                )
                            }
                            if (plannedForDay.size > 3) {
                                Text(
                                    text = "and ${plannedForDay.size - 3} more…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onEditDayPlan(selectedDate) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (plannedForDay.isNotEmpty()) "Edit workouts" else "Add workouts")
                    }
                }
            }
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
    onEditDay: (LocalDate) -> Unit,
    onStartWorkout: (PlannedDaySection) -> Unit,
    bodyWeightKg: Double? = null,
    onEditSetup: () -> Unit = {}
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
                        .padding(vertical = 12.dp),
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
                                text = if (bodyWeightKg != null) {
                                    "Body Weight: ${bodyWeightKg.toInt()} kg • ${plan.sections.size} planned days • ${plan.totalExercises} workouts"
                                } else {
                                    "${plan.sections.size} planned days • ${plan.totalExercises} workouts"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEditSetup) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Edit body weight",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (activeSection != null) {
                            IconButton(onClick = { onEditDay(activeSection.scheduledDate) }) {
                                Icon(
                                    imageVector = Icons.Outlined.EditCalendar,
                                    contentDescription = "Edit workouts for this day",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
        if (activeSection == null) {
            // Empty state: the plan holds only user-selected workouts, and none are saved yet.
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No workouts planned yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a day on the calendar and add the workouts you want to do.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBackToCalendar,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Back to calendar", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250)) + slideInHorizontally(
                        animationSpec = tween(250),
                        initialOffsetX = { it / 10 }
                    )) togetherWith (fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                        animationSpec = tween(200),
                        targetOffsetX = { -it / 10 }
                    ))
                },
                label = "plan-day"
            ) { section ->
                val day = section ?: return@AnimatedContent
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
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
                                        text = "Section ${day.dayNumber} of ${plan.sections.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = day.scheduledDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = day.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "${day.exercises.size} Exercises",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Empty day: no workouts selected yet for this date.
                if (day.exercises.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No workouts planned for this day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Add the workouts you want to do on this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Exercises List Header
                if (day.exercises.isNotEmpty()) item {
                    Text(
                        text = "Scheduled Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                // Exercise Cards
                items(day.exercises, key = { it.name }) { exercise ->
                    PlannedExerciseCard(exercise = exercise, modifier = Modifier.animateItem())
                }

                // Edit this day's selected workouts
                item {
                    OutlinedButton(
                        onClick = { onEditDay(day.scheduledDate) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
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
                                text = if (day.exercises.isEmpty()) "Add workouts" else "Edit workouts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Material You 3 "Start Workout" Button
                if (day.exercises.isNotEmpty()) item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onStartWorkout(day) },
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
                                text = "Start Day ${day.dayNumber} Workout",
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
}

// MARK: - Plan editor: search the catalog and multi-select workouts for one day

/** Small − / + stepper for the optional sets/reps editor. */
@Composable
private fun SetsRepsStepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean,
    canIncrease: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(end = 6.dp)
        )
        IconButton(onClick = onDecrease, enabled = canDecrease, modifier = Modifier.size(32.dp)) {
            Text(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        IconButton(onClick = onIncrease, enabled = canIncrease, modifier = Modifier.size(32.dp)) {
            Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
    }
}

/**
 * 64dp GIF thumbnail for a picker row: the exercise GIF (same backend-served
 * GIFs as the detail view) when one exists, else a photo-icon placeholder.
 * The WebView would swallow taps, so when [onClick] is given the thumbnail
 * gets its own clickable that forwards the tap (e.g. row toggle).
 */
@Composable
private fun PlanPickerGifThumb(
    gifUrl: String?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (gifUrl != null) {
            GifWebView(url = gifUrl, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "No GIF available",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/** Backend GIF URL for a planned item (which carries no Exercise), or null. */
private fun gifUrlForPlannedItem(name: String): String? =
    ExerciseMedia.localGifAsset(name)?.let { ExerciseMedia.GIF_BASE_URL + it }

@Composable
fun PlanWorkoutPickerScreen(
    date: LocalDate,
    initialSelection: List<PlannedExerciseItem>,
    workoutStore: WorkoutStore,
    onBack: () -> Unit,
    onSave: (List<PlannedExerciseItem>) -> Unit
) {
    var query by remember(date) { mutableStateOf("") }
    var selectedGroup by remember(date) { mutableStateOf<String?>(null) }
    val draft = remember(date) {
        mutableStateListOf<PlannedExerciseItem>().apply { addAll(initialSelection) }
    }
    // Name of the selected workout whose sets/reps editor is expanded (null = none).
    var editingSetsRepsFor by remember(date) { mutableStateOf<String?>(null) }
    // Save is only enabled when the selection actually changed; saving an
    // untouched (possibly empty) selection would be a silent no-op.
    val hasChanges = draft.toList() != initialSelection
    // Same search API the workout log flow uses: name / primary-muscle match + muscle-group filter.
    val filtered = remember(query, selectedGroup) {
        workoutStore.filteredExercises(query, selectedGroup)
    }
    val muscleGroups = remember { listOf<String?>(null) + ExerciseMuscleGroups.all }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top bar
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF0F172A)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plan workouts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${draft.size} selected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search workouts…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Muscle-group filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(muscleGroups) { group ->
                        val isSelected = selectedGroup == group
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                            shadowElevation = if (isSelected) 0.dp else 1.dp,
                            modifier = Modifier
                                .clickable { selectedGroup = group }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = group ?: "All",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Selected workouts (always visible, removable even if the catalog no longer lists them)
            if (draft.isNotEmpty()) {
                item {
                    Text(
                        text = "Selected (${draft.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                items(draft.toList(), key = { it.name }) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                PlanPickerGifThumb(
                                    gifUrl = gifUrlForPlannedItem(item.name),
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = item.targetMuscle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = item.setsReps,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Optional sets/reps: tapping expands the steppers below.
                                IconButton(onClick = {
                                    editingSetsRepsFor =
                                        if (editingSetsRepsFor == item.name) null else item.name
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit sets and reps for ${item.name}",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    if (editingSetsRepsFor == item.name) editingSetsRepsFor = null
                                    draft.remove(item)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove ${item.name}",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }
                            if (editingSetsRepsFor == item.name) {
                                val (sets, reps) = parseSetsReps(item.setsReps)
                                fun updateSetsReps(newSets: Int, newReps: Int) {
                                    val index = draft.indexOfFirst { it.name == item.name }
                                    if (index >= 0) {
                                        draft[index] = draft[index].copy(
                                            setsReps = formatSetsReps(newSets, newReps)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SetsRepsStepper(
                                        label = "Sets",
                                        value = sets,
                                        onDecrease = { updateSetsReps((sets - 1).coerceAtLeast(1), reps) },
                                        onIncrease = { updateSetsReps((sets + 1).coerceAtMost(20), reps) },
                                        canDecrease = sets > 1,
                                        canIncrease = sets < 20
                                    )
                                    SetsRepsStepper(
                                        label = "Reps",
                                        value = reps,
                                        onDecrease = { updateSetsReps(sets, (reps - 1).coerceAtLeast(1)) },
                                        onIncrease = { updateSetsReps(sets, (reps + 1).coerceAtMost(100)) },
                                        canDecrease = reps > 1,
                                        canIncrease = reps < 100
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Searchable catalog
            item {
                Text(
                    text = "Workouts (${filtered.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = "No workouts match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            items(filtered, key = { it.id + it.name }) { exercise ->
                val isSelected = draft.any { it.name == exercise.name }
                val onToggle: () -> Unit = {
                    if (isSelected) {
                        draft.removeAll { it.name == exercise.name }
                    } else {
                        draft.add(exercise.toPlannedItem())
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                    shadowElevation = if (isSelected) 0.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle() }
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                            modifier = Modifier.size(22.dp)
                        )
                        PlanPickerGifThumb(
                            gifUrl = exercise.gifUrl,
                            onClick = onToggle,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        ) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${exercise.primaryMuscleLabel} • ${exercise.equipment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Save bar (pinned at the bottom). The bottom padding clears the
        // floating bottom nav bar (same 120.dp convention as the calendar and
        // plan page) so Save stays visible and tappable.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!hasChanges) {
                Text(
                    text = if (draft.isEmpty()) {
                        "Select at least one workout to save a plan for this day."
                    } else {
                        "No changes to save."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = { onSave(draft.toList()) },
                enabled = hasChanges,
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
                    text = if (draft.isNotEmpty()) "Save plan (${draft.size} workouts)" else "Save plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 64dp GIF thumbnail for a planned exercise, or a photo-icon placeholder when
 * the exercise has no GIF yet (only 5 catalog exercises do). Same pattern as
 * the picker rows: the GIF animates via GifWebView, everything else gets the
 * placeholder.
 */
@Composable
private fun ExerciseGifThumbnail(exerciseName: String, modifier: Modifier = Modifier) {
    val gifFile = ExerciseMedia.localGifAsset(exerciseName)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.Center
    ) {
        if (gifFile != null) {
            GifWebView(
                url = ExerciseMedia.GIF_BASE_URL + gifFile,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Photo,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PlannedExerciseCard(exercise: PlannedExerciseItem, modifier: Modifier = Modifier) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseGifThumbnail(
                    exerciseName = exercise.name,
                    modifier = Modifier.size(64.dp)
                )
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
 * Monthly calendar card rendering weekdays (M, T, W, T, F, S, S),
 * highlighting completed dates with a light-pink circle & checkmark,
 * and marking planned dates with a dot.
 */
@Composable
fun MonthlyWorkoutCalendarCard(
    yearMonth: YearMonth,
    completedDates: Set<LocalDate>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    plannedDates: Set<LocalDate> = emptySet(),
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
                                    val isPlanned = date in plannedDates

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .then(
                                                when {
                                                    isCompleted -> Modifier.background(Color(0xFFFDF2F8)) // Soft light pink
                                                    isSelected -> Modifier.background(Color(0xFFF1F5F9))
                                                    isPlanned -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
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
                                                fontWeight = if (isPlanned) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isPlanned) MaterialTheme.colorScheme.primary else Color(0xFF0F172A),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        if (isPlanned && !isCompleted) {
                                            // Planned-day dot under the number
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 4.dp)
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
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
            .padding(top = 8.dp, bottom = 100.dp)
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
                text = "Your weight is shown on your plan page.",
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

// MARK: - Step 2: Days Selection ("Which days do you want to plan?")

@Composable
fun WorkoutDaysSelectionStepScreen(
    initialSelectedDates: List<LocalDate>,
    onBack: () -> Unit,
    onConfirmPlan: (List<LocalDate>) -> Unit
) {
    val today = remember { LocalDate.now() }
    val nextSevenDays = remember { (0..6).map { today.plusDays(it.toLong()) } }

    // Pre-fill with the currently planned dates that fall in the next 7 days.
    var selectedDates by remember {
        mutableStateOf(initialSelectedDates.filter { it in nextSevenDays }.toSet())
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(top = 8.dp, bottom = 100.dp)
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
                text = "Step 2: Select Days",
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
                text = "Which days do you want to plan?",
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
                    text = "Select the days you want to plan workouts for:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    // Weight lets the counter pill keep its natural width instead of
                    // being squeezed off the right edge.
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = if (selectedDates.isNotEmpty()) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "${selectedDates.size} selected",
                        maxLines = 1,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedDates.isNotEmpty()) Color(0xFF16A34A) else Color(0xFFD97706),
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
                                selectedDates = if (isSelected) {
                                    selectedDates - date
                                } else {
                                    selectedDates + date
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

        // Confirm Button
        Button(
            onClick = {
                val sorted = selectedDates.toList().sorted()
                onConfirmPlan(sorted)
            },
            enabled = selectedDates.isNotEmpty(),
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
                text = if (selectedDates.isNotEmpty()) "Continue" else "Select at least 1 day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

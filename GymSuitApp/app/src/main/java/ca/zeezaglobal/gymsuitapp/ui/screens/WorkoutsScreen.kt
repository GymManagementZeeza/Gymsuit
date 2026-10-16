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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMuscleGroups
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PREFS_NAME = "gymsuit_workout_plan"
private const val KEY_SELECTED_WORKOUTS = "selected_workouts_json"

/** Legacy keys from the auto-generated plan wizard (removed); cleared on load. */
private const val KEY_HAS_PLAN = "has_plan"
private const val KEY_DAYS_PER_WEEK = "days_per_week"
private const val KEY_SCHEDULED_DATES = "scheduled_dates"
private const val KEY_USER_WEIGHT = "user_body_weight_kg"

/** Default sets/reps for a workout picked from the exercise catalog. */
private const val DEFAULT_PLANNED_SETS_REPS = "3 sets × 10 reps"

/**
 * Screen navigation modes for the Workout tab:
 * CALENDAR -> Monthly overview; planned days are marked, tapping a day shows its planned workouts
 * PLAN_EDITOR -> Search + multi-select workouts for one day, then Save
 * PLANNED_WORKOUTS_MAIN -> Plan page: only the workouts the user selected, one tab per planned day
 */
enum class WorkoutsViewMode {
    CALENDAR,
    PLAN_EDITOR,
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

/** Builds the plan page sections from the user's per-day selections (non-empty days only, by date). */
private fun buildPlanFromSelections(
    selections: Map<LocalDate, List<PlannedExerciseItem>>
): WorkoutPlanData {
    val sections = selections.entries
        .filter { it.value.isNotEmpty() }
        .sortedBy { it.key }
        .mapIndexed { index, (date, exercises) ->
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

    val activePlan = buildPlanFromSelections(selections)
    val plannedDates = selections.filterValues { it.isNotEmpty() }.keys

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

    // Loads the saved plan on startup. Legacy auto-generated plan keys are dropped:
    // the plan page shows only user-selected workouts.
    fun loadSavedSelections() {
        selections = selectionsFromJson(planPrefs.getString(KEY_SELECTED_WORKOUTS, null))
        if (planPrefs.contains(KEY_HAS_PLAN) || planPrefs.contains(KEY_DAYS_PER_WEEK) ||
            planPrefs.contains(KEY_SCHEDULED_DATES) || planPrefs.contains(KEY_USER_WEIGHT)
        ) {
            planPrefs.edit()
                .remove(KEY_HAS_PLAN)
                .remove(KEY_DAYS_PER_WEEK)
                .remove(KEY_SCHEDULED_DATES)
                .remove(KEY_USER_WEIGHT)
                .apply()
        }
    }

    fun persistSelections(updated: Map<LocalDate, List<PlannedExerciseItem>>) {
        selections = updated
        planPrefs.edit().putString(KEY_SELECTED_WORKOUTS, selectionsToJson(updated)).apply()
    }

    /** Opens the editor for a day. `returnToPlan` keeps Back on the plan page. */
    fun openEditor(date: LocalDate, returnToPlan: Boolean) {
        editingDate = date
        editorReturnToPlan = returnToPlan
        currentViewMode = WorkoutsViewMode.PLAN_EDITOR
    }

    fun closeEditor() {
        editingDate = null
        currentViewMode = if (editorReturnToPlan) WorkoutsViewMode.PLANNED_WORKOUTS_MAIN else WorkoutsViewMode.CALENDAR
    }

    /** Saves one day's selection and shows the plan page on that day's tab. */
    fun saveDayPlan(date: LocalDate, items: List<PlannedExerciseItem>) {
        val updated = selections.toMutableMap()
        if (items.isEmpty()) updated.remove(date) else updated[date] = items
        persistSelections(updated)
        val sections = buildPlanFromSelections(updated).sections
        selectedSectionTabIndex = sections.indexOfFirst { it.scheduledDate == date }.coerceAtLeast(0)
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

    // View Switching
    when (currentViewMode) {
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
                onPlanWorkoutsClick = {
                    if (activePlan.sections.isNotEmpty()) {
                        selectedSectionTabIndex = 0
                        currentViewMode = WorkoutsViewMode.PLANNED_WORKOUTS_MAIN
                    } else {
                        openEditor(selectedDate, returnToPlan = false)
                    }
                },
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
                }
            )
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
    onEditDayPlan: (LocalDate) -> Unit
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
                onSelectDate = onSelectDate,
                plannedDates = plannedDates
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
                                text = "${activePlan.sections.size} planned days • ${activePlan.totalExercises} workouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        OutlinedButton(
                            onClick = onPlanWorkoutsClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View plan", fontSize = 12.sp)
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
                        text = if (activePlan != null) "View Workout Plan" else "Plan Workouts",
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

                    if (plannedForDay.isNotEmpty()) {
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
                                text = "${plan.sections.size} planned days • ${plan.totalExercises} workouts",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
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
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
                                text = "${activeSection.exercises.size} Exercises",
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

                // Edit this day's selected workouts
                item {
                    OutlinedButton(
                        onClick = { onEditDay(activeSection.scheduledDate) },
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
                                text = "Edit workouts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

// MARK: - Plan editor: search the catalog and multi-select workouts for one day

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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
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
                            }
                            IconButton(onClick = { draft.remove(item) }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove ${item.name}",
                                    tint = Color(0xFF64748B)
                                )
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
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                    shadowElevation = if (isSelected) 0.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) {
                                draft.removeAll { it.name == exercise.name }
                            } else {
                                draft.add(exercise.toPlannedItem())
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                            modifier = Modifier.size(22.dp)
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

        // Save bar (pinned at the bottom)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
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

package ca.zeezaglobal.gymsuitapp.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.HealthWorkoutSession
import ca.zeezaglobal.gymsuitapp.data.WorkoutOnboardingStore
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMedia
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.remote.WorkoutOnboardingApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// ============================================================================
// Workouts main page — Android rebuild mirroring the from-scratch iOS page
// (`WorkoutsView` completed state) section for section:
//
//   1. WorkoutStreakCardView        → week strip + day streak / personal best
//   2. WorkoutDaySummaryCardView    → Workout / Kcal / Minute for selected day
//   3. WorkoutBodyFocusSectionView  → focus pills + exercise GIF grid
//   4. WorkoutProgressProjectionCardView → weight projection graph + the
//      Record Weight sheet
//
// The page is hosted by the workout onboarding cover (PR #77) once
// `workout_goal_setup_completed` is true; the cover draws the back chevron,
// this screen draws only the page content, exactly like iOS.
// ============================================================================

// Design tokens copied from the iOS workout views.
private val WpBackground = Color(0xFFFAFBFD)
private val WpInk = Color(0xFF0F172A)
private val WpSlate = Color(0xFF64748B)
private val WpMuted = Color(0xFF94A3B8)
private val WpBlue = Color(0xFF0062FF)
private val WpPaleBlue = Color(0xFFEFF6FF)
private val WpBorder = Color(0xFFF1F5F9)
private val WpLightFill = Color(0xFFF1F5F9)
private val WpGifBackground = Color(0xFFF8FAFC)
private val WpFlameRed = Color(0xFFEF4444)
private val WpFlameOff = Color(0xFFCBD5E1)
private val WpHeaderGray = Color(0xFF8E8E93)
private val WpIconBlue = Color(0xFF93C5FD)
private val WpDashedLine = Color(0xFFCBD5E1)

/** iOS `Exercise.localGIFName` table, used by the Body Focus grid. */
private val FOCUS_LOCAL_GIF_FILES = mapOf(
    "barbell bench press" to "bench-press.gif",
    "barbell full squat" to "weighted-squat.gif",
    "weighted squat" to "weighted-squat.gif",
    "barbell deadlift" to "deadlift.gif",
    "dumbbell biceps curl" to "dumbbell-curl.gif",
    "dumbbell hammer curl" to "hammer-curl.gif",
    "lever seated fly" to "machine-chest-fly.gif",
    "dumbbell front raise" to "dumbbell-front-raise.gif",
    "dumbbell lateral raise" to "dumbbell-lateral-raise.gif",
    "cable pulldown" to "cable-pulldown.gif",
    "cable lat pulldown full range of motion" to
        "cable-lat-pulldown-full-range-of-motion.gif",
    "bodyweight squat" to "squat.gif",
    "squat" to "squat.gif"
)

/**
 * iOS `Exercise.gifURL`: a backend-provided `gif` filename wins, then the
 * local name table. Android's shared `Exercise.gifUrl` only knows the local
 * table (and a shorter one), so the Body Focus grid resolves URLs with this
 * iOS-identical helper instead of changing the shared model.
 */
private fun focusGifUrl(exercise: Exercise): String? {
    if (exercise.gif.isNotEmpty()) {
        val raw = exercise.gif.trim()
        val filename = if (raw.endsWith(".gif")) raw else "$raw.gif"
        return ExerciseMedia.GIF_BASE_URL + filename
    }
    val localFile = FOCUS_LOCAL_GIF_FILES[exercise.name.lowercase().trim()]
        ?: return null
    return ExerciseMedia.GIF_BASE_URL + localFile
}

/** The white card shared by iOS sections 1, 2 and 4 (r20, #F1F5F9 border, soft shadow). */
@Composable
private fun IosSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, WpBorder)
    ) {
        content()
    }
}

@Composable
fun WorkoutsScreen(
    session: WorkoutSessionState = remember { WorkoutSessionState() }
) {
    // `session` is part of the integration contract with the onboarding
    // cover and the dashboard tab (both call sites pass it); iOS receives
    // the same session object via the environment, and the rebuilt page
    // does not drive any of its UI from it.
    val context = LocalContext.current
    val workoutStore = remember { WorkoutStore(context) }
    val onboardingStore = remember { WorkoutOnboardingStore(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var loggedWorkouts by remember {
        mutableStateOf<List<LoggedWorkout>>(emptyList())
    }
    var healthSessions by remember {
        mutableStateOf<List<HealthWorkoutSession>>(emptyList())
    }
    var storedPersonalBest by remember {
        mutableIntStateOf(onboardingStore.personalBestDays)
    }
    var selectedFocus by remember { mutableStateOf("Forearm") }

    // iOS: `loadRealWorkouts` on appear (WorkoutStreakCardView) plus
    // `.task(id: selectedDate)` in the day summary — one reload covers both.
    LaunchedEffect(selectedDate) {
        loggedWorkouts = workoutStore.getLoggedWorkouts()
        if (healthConnectManager.isAvailable() &&
            healthConnectManager.hasExercisePermission()
        ) {
            healthSessions = healthConnectManager
                .readRecentWorkoutSessions(90L)
        }
        // iOS persists a new personal best as soon as one is computed.
        val days = loggedDaysOf(loggedWorkouts) +
            healthSessions.map { it.date }.toSet()
        val best = max(
            max(computeCurrentStreak(days, LocalDate.now()),
                computeLongestStreak(days)),
            onboardingStore.personalBestDays
        )
        if (best > onboardingStore.personalBestDays) {
            onboardingStore.personalBestDays = best
            storedPersonalBest = best
        }
    }

    val exercises = remember { workoutStore.exercises }
    val filteredExercises = remember(exercises, selectedFocus) {
        val matches = exercises.filter { matchesBodyFocus(it, selectedFocus) }
        // iOS: an empty filter result shows the whole catalog.
        if (matches.isEmpty()) exercises else matches
    }

    // iOS page: #FAFBFD, sections stacked with 20pt spacing, 20pt side
    // padding, 8pt top / 24pt bottom.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WpBackground),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp
        )
    ) {
        item {
            WorkoutStreakCard(
                selectedDate = selectedDate,
                onSelectDate = { selectedDate = it },
                allWorkoutDays = loggedDaysOf(loggedWorkouts) +
                    healthSessions.map { it.date }.toSet(),
                storedPersonalBest = storedPersonalBest
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            WorkoutDaySummaryCard(
                selectedDate = selectedDate,
                loggedWorkouts = loggedWorkouts,
                healthSessions = healthSessions
            )
        }
        // iOS adds 4pt of top padding before sections 3 and 4 (24 total).
        item { Spacer(Modifier.height(24.dp)) }
        bodyFocusItems(
            exercises = filteredExercises,
            selectedFocus = selectedFocus,
            onSelectFocus = { selectedFocus = it }
        )
        item { Spacer(Modifier.height(8.dp)) }
        item {
            WorkoutProgressProjectionCard(
                onboardingStore = onboardingStore,
                healthConnectManager = healthConnectManager
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Section 1 — streak card (iOS WorkoutStreakCardView)
// ----------------------------------------------------------------------------

private fun loggedDaysOf(workouts: List<LoggedWorkout>): Set<LocalDate> =
    workouts.map {
        Instant.ofEpochMilli(it.timestampMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSet()

/**
 * iOS `calculateCurrentStreak`: consecutive workout days ending today, or
 * ending yesterday when today has no workout yet; otherwise 0.
 */
private fun computeCurrentStreak(days: Set<LocalDate>, today: LocalDate): Int {
    if (days.isEmpty()) return 0
    var cursor = when {
        days.contains(today) -> today
        days.contains(today.minusDays(1)) -> today.minusDays(1)
        else -> return 0
    }
    var streak = 0
    while (days.contains(cursor)) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/** iOS `calculateMaxStreak`: longest run of consecutive days in the set. */
private fun computeLongestStreak(days: Set<LocalDate>): Int {
    if (days.isEmpty()) return 0
    var longest = 0
    var current = 0
    var last: LocalDate? = null
    for (day in days.sorted()) {
        val previous = last
        current = when {
            previous == null -> 1
            previous.plusDays(1) == day -> current + 1
            previous == day -> current
            else -> 1
        }
        if (current > longest) longest = current
        last = day
    }
    return longest
}

@Composable
private fun WorkoutStreakCard(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    allWorkoutDays: Set<LocalDate>,
    storedPersonalBest: Int
) {
    val today = LocalDate.now()
    val currentStreak = computeCurrentStreak(allWorkoutDays, today)
    val personalBest = max(
        max(currentStreak, computeLongestStreak(allWorkoutDays)),
        storedPersonalBest
    )
    // Current week, Sunday first (iOS `currentWeekDays`).
    // DayOfWeek.value is Mon=1..Sun=7, so value % 7 is days since Sunday.
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    val weekDays = (0..6).map { sunday.plusDays(it.toLong()) }
    val headerLetters = listOf("S", "M", "T", "W", "T", "F", "S")

    IosSectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Week grid (iOS top padding 4 above the columns).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                weekDays.forEachIndexed { index, date ->
                    StreakDayCell(
                        header = headerLetters[index],
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasWorkout = allWorkoutDays.contains(date),
                        onClick = { onSelectDate(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Divider (iOS: 18pt gap + 4pt extra top padding).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(1.dp)
                    .background(WpBorder)
            )
            // Stats row.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Day Streak",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = WpSlate
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Whatshot,
                            contentDescription = null,
                            tint = if (currentStreak > 0) WpFlameRed
                            else WpFlameOff,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currentStreak.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = WpInk
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Personal Best",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = WpSlate
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (personalBest == 1) "1 day"
                        else "$personalBest days",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = WpInk
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakDayCell(
    header: String,
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasWorkout: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFuture = date.isAfter(LocalDate.now())
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = header,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) WpBlue else WpHeaderGray
        )
        Spacer(Modifier.height(12.dp))
        // 44pt tap slot; the visible circle/ring inside is 36pt.
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasWorkout && isSelected && !isToday) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            3.dp,
                            WpBlue.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
            }
            when {
                hasWorkout -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(WpBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                isToday -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                2.dp,
                                WpBlue.copy(alpha = 0.85f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WpBlue
                        )
                    }
                }
                isSelected -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                2.dp,
                                WpBlue.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WpBlue
                        )
                    }
                }
                else -> {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 18.sp,
                        fontWeight = if (isFuture) FontWeight.SemiBold
                        else FontWeight.Bold,
                        color = if (isFuture) WpMuted else WpInk
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Section 2 — day summary card (iOS WorkoutDaySummaryCardView)
// ----------------------------------------------------------------------------

@Composable
private fun WorkoutDaySummaryCard(
    selectedDate: LocalDate,
    loggedWorkouts: List<LoggedWorkout>,
    healthSessions: List<HealthWorkoutSession>
) {
    val zone = ZoneId.systemDefault()
    val dayLogged = loggedWorkouts.filter {
        Instant.ofEpochMilli(it.timestampMillis)
            .atZone(zone).toLocalDate() == selectedDate
    }
    // iOS separates logged workouts already synced to the health store from
    // unsynced ones, whose values are estimated so nothing double-counts.
    val unsyncedLogged = dayLogged.filter { !it.syncedToHealth }
    val daySessions = healthSessions.filter { it.date == selectedDate }

    val workoutCount = daySessions.size + unsyncedLogged.size
    val healthMinutes = daySessions.sumOf { it.durationMinutes }
    val unsyncedMinutes = unsyncedLogged.sumOf { it.durationMinutes }.toLong()
    val totalMinutes = healthMinutes + unsyncedMinutes
    // iOS adds the HealthKit sessions' real kcal to an estimate for unsynced
    // logged workouts (duration × 7.5 kcal/min). Android writes its Health
    // Connect sessions without calories and Health Connect exposes no
    // per-session kcal, so the same 7.5 kcal/min estimate covers the synced
    // sessions too; counts and minutes are exact real data.
    val totalKcal = (totalMinutes * 7.5).roundToInt()

    IosSectionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp)
        ) {
            DaySummaryMetric(
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Filled.MilitaryTech,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                },
                value = workoutCount.toString(),
                label = "Workout",
                modifier = Modifier.weight(1f)
            )
            DaySummaryMetric(
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                },
                value = totalKcal.toString(),
                label = "Kcal",
                modifier = Modifier.weight(1f)
            )
            DaySummaryMetric(
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                },
                value = totalMinutes.toString(),
                label = "Minute",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DaySummaryMetric(
    icon: @Composable (Color) -> Unit,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // SF Symbol over a soft blue dot offset to the bottom-trailing corner
        // (iOS: 17pt #93C5FD circle at 85% opacity, offset 3,3, icon on top).
        Box(
            modifier = Modifier
                .size(32.dp)
                .padding(bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .background(
                        WpIconBlue.copy(alpha = 0.85f),
                        CircleShape
                    )
            )
            icon(WpInk)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = WpInk
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = WpSlate
        )
    }
}

// ----------------------------------------------------------------------------
// Section 3 — Body Focus (iOS WorkoutBodyFocusSectionView)
// ----------------------------------------------------------------------------

private val BODY_FOCUS_OPTIONS = listOf(
    "Abs", "Arm", "Forearm", "Chest", "Leg", "Back", "Shoulders"
)

/** Direct port of the iOS `matchesFocus(_:focus:)` filter. */
private fun matchesBodyFocus(exercise: Exercise, focus: String): Boolean {
    val name = exercise.name.lowercase()
    val bodyPart = exercise.bodyPart.lowercase()
    val primary = exercise.primaryMuscle.lowercase()
    val secondary = exercise.secondaryMuscles.map { it.lowercase() }
    return when (focus) {
        "Chest" -> bodyPart.contains("chest") ||
            primary.contains("chest") || primary.contains("pectoral")
        "Arm" -> bodyPart.contains("arm") ||
            primary.contains("biceps") || primary.contains("triceps") ||
            primary.contains("brachii")
        "Forearm" -> name.contains("hammer") ||
            primary.contains("forearm") ||
            primary.contains("brachioradialis") ||
            secondary.contains("forearms")
        "Back" -> bodyPart.contains("back") ||
            primary.contains("lat") || primary.contains("trapezius") ||
            name.contains("pulldown") || name.contains("deadlift")
        "Leg" -> bodyPart.contains("leg") ||
            primary.contains("quad") || primary.contains("glute") ||
            primary.contains("hamstring") ||
            name.contains("squat") || name.contains("deadlift")
        "Shoulders" -> bodyPart.contains("shoulder") ||
            primary.contains("deltoid") || name.contains("raise")
        "Abs" -> bodyPart.contains("waist") ||
            primary.contains("abs") || secondary.contains("abs") ||
            name.contains("squat")
        else -> true
    }
}

/**
 * Swift `.capitalized` approximation for catalog names/equipment:
 * first letter of each word uppercased, the rest lowercased.
 */
private fun titleCaseWords(value: String): String =
    value.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercaseChar() }
    }

private fun LazyListScope.bodyFocusItems(
    exercises: List<Exercise>,
    selectedFocus: String,
    onSelectFocus: (String) -> Unit
) {
    item {
        Text(
            text = "Body Focus",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = WpInk,
            modifier = Modifier.padding(start = 4.dp, bottom = 14.dp)
        )
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            BODY_FOCUS_OPTIONS.forEach { option ->
                val isSelected = option == selectedFocus
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) WpPaleBlue else WpLightFill)
                        .then(
                            if (isSelected) {
                                Modifier.border(1.5.dp, WpBlue, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelectFocus(option) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold
                        else FontWeight.Medium,
                        color = if (isSelected) WpBlue else WpMuted
                    )
                }
            }
        }
    }
    items(exercises.chunked(2)) { rowExercises ->
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            rowExercises.forEach { exercise ->
                BodyFocusExerciseCard(
                    exercise = exercise,
                    modifier = Modifier.weight(1f)
                )
            }
            if (rowExercises.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BodyFocusExerciseCard(
    exercise: Exercise,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, WpBorder)
    ) {
        Column(modifier = Modifier.clip(RoundedCornerShape(18.dp))) {
            BodyFocusGifView(
                url = focusGifUrl(exercise),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = titleCaseWords(exercise.name),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = WpInk,
                    maxLines = 2,
                    modifier = Modifier.height(38.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${exercise.muscleGroup} • " +
                        titleCaseWords(exercise.equipment),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = WpSlate,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * iOS `AnimatedGIFView`: shimmer while the GIF loads, the animated GIF when
 * ready, and the "Exercise Demo" placeholder on failure or missing URL.
 * Android renders the GIF in a WebView (same approach as the app's existing
 * `GifWebView`); the WebView's HTTP cache stands in for iOS's memory cache.
 */
@Composable
private fun BodyFocusGifView(url: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(WpGifBackground)) {
        if (url == null) {
            GifPlaceholder()
            return@Box
        }
        var isLoading by remember(url) { mutableStateOf(true) }
        var hasError by remember(url) { mutableStateOf(false) }
        if (isLoading && !hasError) {
            ShimmerBox(Modifier.fillMaxSize())
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            finishedUrl: String?
                        ) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                isLoading = false
                            }
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) webView.loadUrl(url)
            },
            modifier = Modifier.fillMaxSize()
        )
        if (hasError) {
            GifPlaceholder()
        }
    }
}

@Composable
private fun GifPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WpGifBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FitnessCenter,
            contentDescription = null,
            tint = WpMuted,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Exercise Demo",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = WpMuted
        )
    }
}

/** The iOS loading shimmer: a 1.3s diagonal gradient sweep. */
@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "gifShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gifShimmerProgress"
    )
    Box(
        modifier = modifier.drawBehind {
            val t = progress
            val startX = (-1.8f + 2.8f * t) * size.width
            val startY = (-1.2f + 2.2f * t) * size.height
            val endX = (0f + 2.8f * t) * size.width
            val endY = (-0.2f + 2.4f * t) * size.height
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF1F5F9),
                        Color.White.copy(alpha = 0.85f),
                        Color(0xFFE2E8F0)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                )
            )
        }
    )
}

// ----------------------------------------------------------------------------
// Section 4 — progress projection card (iOS WorkoutProgressProjectionCardView)
// ----------------------------------------------------------------------------

private fun parseIsoDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return try {
        LocalDate.parse(
            value,
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        )
    } catch (e: Exception) {
        null
    }
}

private fun formatWeightKg(value: Double): String =
    String.format(Locale.US, "%.1f kg", value)

private fun formatMonthDay(date: LocalDate): String =
    date.format(
        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    )

@Composable
private fun WorkoutProgressProjectionCard(
    onboardingStore: WorkoutOnboardingStore,
    healthConnectManager: HealthConnectManager
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val api = remember { WorkoutOnboardingApi(context) }
    var backendCurrentWeightKg by remember { mutableStateOf<Double?>(null) }
    var backendTargetWeightKg by remember { mutableStateOf<Double?>(null) }
    var backendTargetDate by remember { mutableStateOf<LocalDate?>(null) }
    var currentWeightKg by remember {
        mutableStateOf(onboardingStore.weightKg)
    }
    var targetWeightKg by remember {
        mutableStateOf(onboardingStore.targetWeightKg)
    }
    var showRecordSheet by remember { mutableStateOf(false) }

    // iOS `loadBackendData`: backend onboarding values win over local ones
    // and are written back locally, exactly like the @AppStorage writes.
    suspend fun loadBackendData() {
        api.fetchOnboarding().onSuccess { data ->
            data.targetWeightKg?.takeIf { it > 0 }?.let { value ->
                backendTargetWeightKg = value
                targetWeightKg = value
                onboardingStore.targetWeightKg = value
            }
            data.weightKg?.takeIf { it > 0 }?.let { value ->
                backendCurrentWeightKg = value
                currentWeightKg = value
                onboardingStore.weightKg = value
            }
            parseIsoDate(data.targetDate)?.let { backendTargetDate = it }
        }
    }
    LaunchedEffect(Unit) { loadBackendData() }

    val effectiveCurrent = backendCurrentWeightKg ?: currentWeightKg
    val effectiveTarget = backendTargetWeightKg ?: targetWeightKg
    val isGaining = effectiveTarget > effectiveCurrent
    val diffKg = abs(effectiveCurrent - effectiveTarget)
    val projectionDays = max(ceil(max(diffKg, 0.5) * 7).toInt(), 14)
    val today = LocalDate.now()
    val effectiveTargetDate = backendTargetDate
        ?: today.plusDays(projectionDays.toLong())

    IosSectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Based on your body data, you'll reach",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = WpSlate,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatWeightKg(effectiveTarget),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = WpInk,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = "on",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = WpInk,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = formatMonthDay(effectiveTargetDate),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = WpBlue,
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            ProjectionGraph(
                currentWeightKg = effectiveCurrent,
                targetWeightKg = effectiveTarget,
                isGaining = isGaining,
                targetDateLabel = formatMonthDay(effectiveTargetDate),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Spacer(Modifier.height(20.dp))
            // "Record Weight" button.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WpPaleBlue)
                    .border(
                        1.dp,
                        WpBlue.copy(alpha = 0.18f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { showRecordSheet = true }
                    .padding(vertical = 13.dp),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Scale,
                    contentDescription = null,
                    tint = WpBlue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Record Weight",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WpBlue
                )
            }
        }
    }

    if (showRecordSheet) {
        RecordWeightSheet(
            initialWeightKg = effectiveCurrent,
            onDismiss = { showRecordSheet = false },
            onSave = { newWeightKg ->
                // iOS `saveRecordedWeight`: update local + backend weight,
                // write the health-store weight, sync onboarding, reload.
                currentWeightKg = newWeightKg
                backendCurrentWeightKg = newWeightKg
                onboardingStore.weightKg = newWeightKg
                scope.launch {
                    healthConnectManager.insertWeight(newWeightKg)
                    api.syncOnboarding(
                        workoutGoal = onboardingStore.selectedGoalRaw,
                        heightCm = onboardingStore.heightCm,
                        weightKg = newWeightKg,
                        targetWeightKg = effectiveTarget,
                        birthYear = onboardingStore.birthYear,
                        workoutDaysPerWeek = onboardingStore.daysPerWeek,
                        targetDate = Date.from(
                            effectiveTargetDate
                                .atStartOfDay(ZoneOffset.UTC)
                                .toInstant()
                        )
                    )
                    loadBackendData()
                }
            }
        )
    }
}

/**
 * Direct port of the iOS projection canvas (`ProjectionGraphView` drawing):
 * gradient area fill, dashed guidelines, gradient S-curve, start dot + weight
 * label, glowing target dot + tooltip bubble, and the Today / target-date
 * axis labels. Text is drawn on the native canvas with the same sizes,
 * weights and colors as iOS.
 */
@Composable
private fun ProjectionGraph(
    currentWeightKg: Double,
    targetWeightKg: Double,
    isGaining: Boolean,
    targetDateLabel: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height
            val startX = 45.dp.toPx()
            val targetX = widthPx - 65.dp.toPx()
            val startY = if (isGaining) heightPx - 60.dp.toPx()
            else 38.dp.toPx()
            val targetY = if (isGaining) 38.dp.toPx()
            else heightPx - 60.dp.toPx()
            val bottomY = heightPx - 26.dp.toPx()
            val control1X = startX + (targetX - startX) * 0.45f
            val control1Y = if (isGaining) startY - 6.dp.toPx()
            else startY + 6.dp.toPx()
            val control2X = startX + (targetX - startX) * 0.55f

            // Gradient fill under the curve, extended to the card edges.
            val fillPath = Path().apply {
                moveTo(0f, if (isGaining) bottomY else startY)
                lineTo(startX, startY)
                cubicTo(control1X, control1Y, control2X, targetY,
                    targetX, targetY)
                lineTo(widthPx, targetY)
                lineTo(widthPx, bottomY)
                lineTo(0f, bottomY)
                close()
            }
            val fillColors = if (isGaining) {
                listOf(
                    Color(0xFF93C5FD).copy(alpha = 0.35f),
                    Color(0xFFC7D2FE).copy(alpha = 0.18f),
                    WpBackground.copy(alpha = 0f)
                )
            } else {
                listOf(
                    Color(0xFFFDA4AF).copy(alpha = 0.38f),
                    Color(0xFFE0E7FF).copy(alpha = 0.20f),
                    WpBackground.copy(alpha = 0f)
                )
            }
            drawPath(
                path = fillPath,
                brush = Brush.linearGradient(
                    colors = fillColors,
                    start = Offset(0f, 0f),
                    end = Offset(widthPx, heightPx)
                )
            )

            // Dashed guidelines from both points down to the axis.
            val dashEffect = PathEffect.dashPathEffect(
                floatArrayOf(4.dp.toPx(), 4.dp.toPx())
            )
            drawLine(
                color = WpDashedLine,
                start = Offset(startX, startY),
                end = Offset(startX, bottomY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = WpDashedLine,
                start = Offset(targetX, targetY),
                end = Offset(targetX, bottomY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            // The S-curve itself (4pt, gradient stroke, round caps/joins).
            val curvePath = Path().apply {
                moveTo(0f, startY)
                lineTo(startX, startY)
                cubicTo(control1X, control1Y, control2X, targetY,
                    targetX, targetY)
                lineTo(widthPx, targetY)
            }
            val strokeColors = if (isGaining) {
                listOf(
                    Color(0xFF3B82F6), Color(0xFF6366F1),
                    WpBlue, Color(0xFF38BDF8)
                )
            } else {
                listOf(
                    Color(0xFFF43F5E), Color(0xFFA855F7),
                    WpBlue, Color(0xFF38BDF8)
                )
            }
            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(colors = strokeColors),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Start dot.
            val startDotColor = if (isGaining) Color(0xFF3B82F6)
            else Color(0xFFF43F5E)
            drawCircle(
                color = startDotColor,
                radius = 5.dp.toPx(),
                center = Offset(startX, startY)
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(startX, startY),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Target glow + dot.
            drawCircle(
                color = WpBlue.copy(alpha = 0.20f),
                radius = 12.dp.toPx(),
                center = Offset(targetX, targetY)
            )
            drawCircle(
                color = WpBlue,
                radius = 6.5.dp.toPx(),
                center = Offset(targetX, targetY)
            )
            drawCircle(
                color = Color.White,
                radius = 6.5.dp.toPx(),
                center = Offset(targetX, targetY),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // ---- Text + tooltip (native canvas, iOS sizes/weights) ----
            val canvas = drawContext.canvas.nativeCanvas
            fun textPaint(sp: Float, weight: Int, color: Int): Paint =
                Paint().apply {
                    isAntiAlias = true
                    textSize = sp.sp.toPx()
                    this.color = color
                    typeface = when (weight) {
                        Typeface.BOLD -> Typeface.create(
                            Typeface.DEFAULT, Typeface.BOLD
                        )
                        600 -> Typeface.create(
                            "sans-serif-medium", Typeface.BOLD
                        )
                        500 -> Typeface.create(
                            "sans-serif-medium", Typeface.NORMAL
                        )
                        else -> Typeface.DEFAULT
                    }
                }
            fun centeredBaseline(paint: Paint, centerY: Float): Float {
                val metrics = paint.fontMetrics
                return centerY - (metrics.ascent + metrics.descent) / 2f
            }
            val inkArgb = 0xFF0F172A.toInt()
            val slateArgb = 0xFF64748B.toInt()
            val blueArgb = 0xFF0062FF.toInt()

            // Start weight label above the start point.
            val startLabelPaint = textPaint(14f, Typeface.BOLD, inkArgb)
                .apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(
                formatWeightKg(currentWeightKg),
                startX,
                centeredBaseline(
                    startLabelPaint, startY - 18.dp.toPx()
                ),
                startLabelPaint
            )

            // Tooltip bubble above the target point: "%.1f" + " kg".
            val numberText = String.format(
                Locale.US, "%.1f", targetWeightKg
            )
            val unitText = "kg"
            val numberPaint = textPaint(18f, Typeface.BOLD, -1)
            val unitPaint = textPaint(13f, 600, 0xEBFFFFFF.toInt())
            val gapPx = 3.dp.toPx()
            val textWidth = numberPaint.measureText(numberText) + gapPx +
                unitPaint.measureText(unitText)
            val bubbleWidth = textWidth + 28.dp.toPx()
            val bubbleHeight = 18.sp.toPx() + 14.dp.toPx()
            val triangleHeight = 6.dp.toPx()
            val totalHeight = bubbleHeight + triangleHeight - 1.dp.toPx()
            val tooltipCenterY = targetY - 40.dp.toPx()
            val bubbleTop = tooltipCenterY - totalHeight / 2f
            val bubbleLeft = targetX - bubbleWidth / 2f
            val bubblePaint = Paint().apply {
                isAntiAlias = true
                color = blueArgb
                style = Paint.Style.FILL
                setShadowLayer(
                    6.dp.toPx(), 0f, 3.dp.toPx(),
                    0x470062FF
                )
            }
            canvas.drawRoundRect(
                bubbleLeft, bubbleTop,
                bubbleLeft + bubbleWidth, bubbleTop + bubbleHeight,
                10.dp.toPx(), 10.dp.toPx(), bubblePaint
            )
            // Triangle under the bubble.
            val trianglePath = android.graphics.Path().apply {
                moveTo(targetX - 5.dp.toPx(), bubbleTop + bubbleHeight)
                lineTo(targetX + 5.dp.toPx(), bubbleTop + bubbleHeight)
                lineTo(targetX, bubbleTop + bubbleHeight + triangleHeight)
                close()
            }
            canvas.drawPath(trianglePath, bubblePaint)
            val textBaseline = centeredBaseline(
                numberPaint, bubbleTop + bubbleHeight / 2f
            )
            numberPaint.textAlign = Paint.Align.LEFT
            unitPaint.textAlign = Paint.Align.LEFT
            val textStartX = targetX - textWidth / 2f
            canvas.drawText(numberText, textStartX, textBaseline, numberPaint)
            canvas.drawText(
                unitText,
                textStartX + numberPaint.measureText(numberText) + gapPx,
                textBaseline,
                unitPaint
            )

            // Axis labels.
            val todayPaint = textPaint(14f, 500, slateArgb)
                .apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(
                "Today",
                startX,
                centeredBaseline(
                    todayPaint, heightPx - 8.dp.toPx()
                ),
                todayPaint
            )
            val datePaint = textPaint(16f, Typeface.BOLD, inkArgb)
                .apply { textAlign = Paint.Align.CENTER }
            canvas.drawText(
                targetDateLabel,
                targetX,
                centeredBaseline(
                    datePaint, heightPx - 8.dp.toPx()
                ),
                datePaint
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Record Weight sheet (iOS RecordWeightSheet)
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordWeightSheet(
    initialWeightKg: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var recordedWeightKg by remember {
        mutableStateOf(initialWeightKg)
    }
    var selectedUnit by remember { mutableStateOf("kg") }
    var recordDate by remember { mutableStateOf(LocalDate.now()) }
    var isSaving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(isSaving) {
        if (isSaving) {
            delay(300)
            onDismiss()
        }
    }

    fun displayWeight(): Double =
        if (selectedUnit == "kg") recordedWeightKg
        else recordedWeightKg * 2.20462

    // iOS `adjustWeight`: steppers move in the selected unit, clamped to
    // 30–250 kg or 66–550 lbs.
    fun adjust(delta: Double) {
        recordedWeightKg = if (selectedUnit == "kg") {
            (recordedWeightKg + delta).coerceIn(30.0, 250.0)
        } else {
            val newLbs = (recordedWeightKg * 2.20462 + delta)
                .coerceIn(66.0, 550.0)
            newLbs / 2.20462
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title row (iOS navigation bar: Cancel left, title centered).
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(text = "Cancel", color = WpBlue, fontSize = 16.sp)
                }
                Text(
                    text = "Record Weight",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = WpInk,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(24.dp))

            // Unit toggle (iOS segmented control: kg / lbs).
            SingleChoiceSegmentedButtonRow(modifier = Modifier.width(200.dp)) {
                listOf("kg", "lbs").forEachIndexed { index, unit ->
                    SegmentedButton(
                        selected = selectedUnit == unit,
                        onClick = { selectedUnit = unit },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index, count = 2
                        ),
                        label = {
                            Text(
                                text = unit,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Date row. Like iOS, the picked date is display-only — saving
            // writes the weight with today's date.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = WpBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = recordDate.format(
                        DateTimeFormatter.ofPattern(
                            "MMM d, yyyy", Locale.getDefault()
                        )
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = WpInk
                )
            }
            Spacer(Modifier.height(32.dp))

            // Big weight display.
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(
                        Locale.US, "%.1f", displayWeight()
                    ),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = WpInk,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = selectedUnit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = WpSlate,
                    modifier = Modifier.alignByBaseline()
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Current Recorded Weight",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = WpMuted
            )
            Spacer(Modifier.height(28.dp))

            // Steppers.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                listOf("-0.5", "-0.1", "+0.1", "+0.5").forEach { label ->
                    val delta = label.toDouble()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WpPaleBlue)
                            .clickable { adjust(delta) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WpBlue
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        onSave(recordedWeightKg)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WpBlue,
                    contentColor = Color.White
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save Weight",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = recordDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        // iOS caps the picker at today.
                        recordDate = if (picked.isAfter(LocalDate.now())) {
                            LocalDate.now()
                        } else {
                            picked
                        }
                    }
                    showDatePicker = false
                }) {
                    Text(text = "OK", color = WpBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Cancel", color = WpBlue)
                }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}

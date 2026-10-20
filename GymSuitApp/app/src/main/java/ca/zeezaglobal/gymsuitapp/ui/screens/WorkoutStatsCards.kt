package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMuscleGroups
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// MARK: - Workout insights cards (local data only, Android only)
//
// Inserted below the calendar on the workout page. All numbers come from the
// locally stored workout log (WorkoutStore.getLoggedWorkouts()); nothing is
// fetched from the network and no chart library is used — charts are drawn
// with androidx.compose.foundation.Canvas.

private val InkColor = Color(0xFF0F172A)
private val MutedColor = Color(0xFF64748B)
private val TrackColor = Color(0xFFF1F5F9)

/** Fixed pleasant palette per body-part group. */
private val BodyPartColors = mapOf(
    "Chest" to Color(0xFF6366F1),
    "Back" to Color(0xFFEC4899),
    "Shoulders" to Color(0xFFF59E0B),
    "Arms" to Color(0xFF10B981),
    "Legs" to Color(0xFF8B5CF6),
    "Core" to Color(0xFF06B6D4),
    "Cardio" to Color(0xFFF43F5E),
    "Other" to Color(0xFF84CC16)
)

private fun LoggedWorkout.localDate(): LocalDate =
    Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()

private data class BodyPartDetail(
    val group: String,
    val sets: Int,
    val reps: Int,
    val volumeKg: Double,
    val sharePct: Double
)

private data class WeekDetail(
    val label: String,
    val rangeLabel: String,
    val volumeKg: Double,
    val sessions: Int
)

private data class SessionDetail(
    val exerciseName: String,
    val sets: Int,
    val durationMinutes: Int
)

private data class DaySessionDetail(
    val dayLabel: String,
    val dateLabel: String,
    val isToday: Boolean,
    val sessions: List<SessionDetail>
)

private data class AllTimeStats(
    val workouts: Int,
    val sets: Int,
    val reps: Int,
    val volumeKg: Double,
    val avgDurationMin: Double,
    val longestStreak: Int,
    val thisMonthWorkouts: Int,
    val lastMonthWorkouts: Int
)

private data class ExerciseDetail(
    val name: String,
    val sets: Int,
    val reps: Int,
    val volumeKg: Double
)

/** Which stats card's detail dialog is open. */
private enum class StatsDetail(val title: String) {
    INTENSITY("Body Part Intensity"),
    VOLUME("Training Volume"),
    SESSIONS("Sessions This Week"),
    GRID("All-Time Stats"),
    EXERCISES("Exercise Breakdown")
}

private data class WorkoutStats(
    val hasData: Boolean,
    val intensityByBodyPart: List<Pair<String, Int>>, // (group, sets) last 7 days, desc
    val weeklyVolumeKg: List<Double>, // last 8 week totals, oldest -> newest
    val weeklyVolumeLabels: List<String>, // "d/M" week-start labels
    val sessionsByDay: List<Int>, // Mon..Sun of the current week
    val todayIndex: Int, // 0..6 index of today in sessionsByDay
    val totalWorkouts: Int,
    val setsLast7Days: Int,
    val volumeLast7DaysKg: Double,
    val dayStreak: Int,
    val topExercises: List<Pair<String, Int>>, // (name, sets) last 7 days, top 5
    val bodyPartDetail: List<BodyPartDetail>, // last 7 days, desc
    val weeklyDetail: List<WeekDetail>, // last 8 weeks, oldest -> newest
    val daySessions: List<DaySessionDetail>, // Mon..Sun of the current week
    val allTime: AllTimeStats,
    val exerciseDetail: List<ExerciseDetail> // last 7 days, full ranked list
)

private fun computeWorkoutStats(workoutStore: WorkoutStore): WorkoutStats {
    val workouts = workoutStore.getLoggedWorkouts()
    val today = LocalDate.now()
    if (workouts.isEmpty()) {
        return WorkoutStats(
            hasData = false,
            intensityByBodyPart = emptyList(),
            weeklyVolumeKg = emptyList(),
            weeklyVolumeLabels = emptyList(),
            sessionsByDay = List(7) { 0 },
            todayIndex = today.dayOfWeek.value - 1,
            totalWorkouts = 0,
            setsLast7Days = 0,
            volumeLast7DaysKg = 0.0,
            dayStreak = 0,
            topExercises = emptyList(),
            bodyPartDetail = emptyList(),
            weeklyDetail = emptyList(),
            daySessions = emptyList(),
            allTime = AllTimeStats(0, 0, 0, 0.0, 0.0, 0, 0, 0),
            exerciseDetail = emptyList()
        )
    }

    // Map each logged workout to a body-part group via exerciseId, then exerciseName.
    val bodyPartById = workoutStore.exercises.associate { it.id to it.bodyPart }
    val bodyPartByName = workoutStore.exercises.associate { it.name.lowercase().trim() to it.bodyPart }
    fun LoggedWorkout.bodyPartGroup(): String {
        val raw = bodyPartById[exerciseId]
            ?: bodyPartByName[exerciseName.lowercase().trim()]
            ?: ""
        return ExerciseMuscleGroups.groupFor(raw.lowercase().trim())
    }

    val last7Start = today.minusDays(6)
    val last7 = workouts.filter { it.localDate() in last7Start..today }

    val groupAgg = last7
        .groupBy { it.bodyPartGroup() }
        .mapValues { (_, ws) ->
            Triple(
                ws.sumOf { it.sets.size },
                ws.sumOf { w -> w.sets.sumOf { it.reps } },
                ws.sumOf { w -> w.sets.sumOf { s -> s.reps * s.weightKg } }
            )
        }
        .filterValues { it.first > 0 }
        .toList()
        .sortedByDescending { it.second.first }

    val intensityByBodyPart = groupAgg.map { it.first to it.second.first }

    val totalGroupSets = groupAgg.sumOf { it.second.first }.coerceAtLeast(1)
    val bodyPartDetail = groupAgg.map { (group, agg) ->
        BodyPartDetail(
            group = group,
            sets = agg.first,
            reps = agg.second,
            volumeKg = agg.third,
            sharePct = agg.first * 100.0 / totalGroupSets
        )
    }

    val thisWeekStart = today.with(DayOfWeek.MONDAY)
    val weekStarts = (7 downTo 0).map { thisWeekStart.minusWeeks(it.toLong()) }
    val weeklyVolumeKg = weekStarts.map { ws ->
        val we = ws.plusDays(6)
        workouts.filter { it.localDate() in ws..we }.sumOf { it.sets.sumOf { s -> s.reps * s.weightKg } }
    }
    val labelFmt = DateTimeFormatter.ofPattern("d/M", Locale.getDefault())
    val weeklyVolumeLabels = weekStarts.map { it.format(labelFmt) }

    val sessionsByDay = List(7) { i ->
        val d = thisWeekStart.plusDays(i.toLong())
        workouts.count { it.localDate() == d }
    }

    val trainedDates = workouts.map { it.localDate() }.toSet()
    var streakDay = if (today in trainedDates) today else today.minusDays(1)
    var streak = 0
    while (streakDay in trainedDates) {
        streak++
        streakDay = streakDay.minusDays(1)
    }

    val topExercises = last7
        .groupBy { it.exerciseName.ifBlank { "Workout" } }
        .mapValues { (_, ws) -> ws.sumOf { it.sets.size } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    val weeklyDetail = weekStarts.map { ws ->
        val we = ws.plusDays(6)
        val inWeek = workouts.filter { it.localDate() in ws..we }
        WeekDetail(
            label = ws.format(labelFmt),
            rangeLabel = "${ws.format(labelFmt)} – ${we.format(labelFmt)}",
            volumeKg = inWeek.sumOf { w -> w.sets.sumOf { s -> s.reps * s.weightKg } },
            sessions = inWeek.size
        )
    }

    val dayNameFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    val daySessions = List(7) { i ->
        val d = thisWeekStart.plusDays(i.toLong())
        DaySessionDetail(
            dayLabel = d.format(dayNameFmt),
            dateLabel = d.format(dateFmt),
            isToday = d == today,
            sessions = workouts.filter { it.localDate() == d }
                .sortedBy { it.timestampMillis }
                .map { w ->
                    SessionDetail(
                        exerciseName = w.exerciseName.ifBlank { "Workout" },
                        sets = w.sets.size,
                        durationMinutes = w.durationMinutes
                    )
                }
        )
    }

    val allSets = workouts.sumOf { it.sets.size }
    val allReps = workouts.sumOf { w -> w.sets.sumOf { it.reps } }
    val allVolume = workouts.sumOf { w -> w.sets.sumOf { s -> s.reps * s.weightKg } }
    val durations = workouts.map { it.durationMinutes }.filter { it > 0 }
    var longestStreak = 0
    var run = 0
    var prevDate: LocalDate? = null
    for (d in trainedDates.sorted()) {
        run = if (prevDate != null && d == prevDate!!.plusDays(1)) run + 1 else 1
        longestStreak = maxOf(longestStreak, run)
        prevDate = d
    }
    val thisMonth = YearMonth.now()
    val lastMonth = thisMonth.minusMonths(1)
    val allTime = AllTimeStats(
        workouts = workouts.size,
        sets = allSets,
        reps = allReps,
        volumeKg = allVolume,
        avgDurationMin = if (durations.isNotEmpty()) durations.average() else 0.0,
        longestStreak = longestStreak,
        thisMonthWorkouts = workouts.count { YearMonth.from(it.localDate()) == thisMonth },
        lastMonthWorkouts = workouts.count { YearMonth.from(it.localDate()) == lastMonth }
    )

    val exerciseDetail = last7
        .groupBy { it.exerciseName.ifBlank { "Workout" } }
        .map { (name, ws) ->
            ExerciseDetail(
                name = name,
                sets = ws.sumOf { it.sets.size },
                reps = ws.sumOf { w -> w.sets.sumOf { it.reps } },
                volumeKg = ws.sumOf { w -> w.sets.sumOf { s -> s.reps * s.weightKg } }
            )
        }
        .sortedByDescending { it.sets }

    return WorkoutStats(
        hasData = true,
        intensityByBodyPart = intensityByBodyPart,
        weeklyVolumeKg = weeklyVolumeKg,
        weeklyVolumeLabels = weeklyVolumeLabels,
        sessionsByDay = sessionsByDay,
        todayIndex = today.dayOfWeek.value - 1,
        totalWorkouts = workouts.size,
        setsLast7Days = last7.sumOf { it.sets.size },
        volumeLast7DaysKg = last7.sumOf { it.sets.sumOf { s -> s.reps * s.weightKg } },
        dayStreak = streak,
        topExercises = topExercises,
        bodyPartDetail = bodyPartDetail,
        weeklyDetail = weeklyDetail,
        daySessions = daySessions,
        allTime = allTime,
        exerciseDetail = exerciseDetail
    )
}

private fun formatKg(kg: Double): String = when {
    kg >= 1000 -> String.format(Locale.getDefault(), "%.1fk", kg / 1000)
    kg >= 100 -> kg.toInt().toString()
    kg >= 10 -> String.format(Locale.getDefault(), "%.0f", kg)
    else -> String.format(Locale.getDefault(), "%.1f", kg)
}

/**
 * Emits the workout insight cards as LazyColumn items. Call this inside the
 * calendar screen's LazyColumn, right below the MonthlyWorkoutCalendarCard item.
 */
fun LazyListScope.workoutStatsItems(workoutStore: WorkoutStore) {
    item(key = "workout_stats_cards") {
        val stats = remember { computeWorkoutStats(workoutStore) }
        var openDetail by remember { mutableStateOf<StatsDetail?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!stats.hasData) {
                EmptyStatsCard()
            } else {
                BodyPartIntensityCard(stats, onClick = { openDetail = StatsDetail.INTENSITY })
                VolumeTrendCard(stats, onClick = { openDetail = StatsDetail.VOLUME })
                SessionsWeekCard(stats, onClick = { openDetail = StatsDetail.SESSIONS })
                StatsGridCard(stats, onClick = { openDetail = StatsDetail.GRID })
                TopExercisesCard(stats, onClick = { openDetail = StatsDetail.EXERCISES })
            }
        }
        openDetail?.let { detail ->
            StatsDetailDialog(
                detail = detail,
                stats = stats,
                onDismiss = { openDetail = null }
            )
        }
    }
}

@Composable
private fun StatsCardContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var cardModifier: Modifier = Modifier.fillMaxWidth()
    if (onClick != null) cardModifier = cardModifier.clickable(onClick = onClick)
    ElevatedCard(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedColor
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun EmptyStatsCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "No workout data yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Log your first workout to see your insights here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BodyPartIntensityCard(stats: WorkoutStats, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val maxSets = stats.intensityByBodyPart.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    StatsCardContainer(
        title = "Body Part Intensity",
        subtitle = "Sets logged in the last 7 days · tap for details",
        icon = Icons.Filled.Whatshot,
        iconTint = Color(0xFFF43F5E),
        onClick = onClick
    ) {
        if (stats.intensityByBodyPart.isEmpty()) {
            Text(
                text = "Nothing trained in the last 7 days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedColor
            )
        } else {
            stats.intensityByBodyPart.forEachIndexed { index, (group, sets) ->
                val fraction = sets.toFloat() / maxSets
                val animated by animateFloatAsState(
                    targetValue = if (visible) fraction else 0f,
                    animationSpec = tween(durationMillis = 650, delayMillis = index * 70),
                    label = "intensity-$group"
                )
                val barColor = BodyPartColors[group] ?: Color(0xFF6366F1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = InkColor,
                        modifier = Modifier.width(84.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TrackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animated.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(barColor)
                        )
                    }
                    Text(
                        text = "$sets",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedColor,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeTrendCard(stats: WorkoutStats, onClick: () -> Unit) {
    StatsCardContainer(
        title = "Training Volume",
        subtitle = "Total kg lifted per week · last 8 weeks · tap for details",
        icon = Icons.Filled.BarChart,
        iconTint = Color(0xFF6366F1),
        onClick = onClick
    ) {
        val volumes = stats.weeklyVolumeKg
        val maxV = (volumes.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        val barColor = Color(0xFF6366F1)
        val lastBarColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val slotW = size.width / volumes.size
            val barW = slotW * 0.58f
            val maxBarH = size.height - 4.dp.toPx()
            volumes.forEachIndexed { i, v ->
                val h = (v / maxV * maxBarH).toFloat().coerceAtLeast(if (v > 0) 4.dp.toPx() else 0f)
                val left = slotW * i + (slotW - barW) / 2f
                drawRoundRect(
                    color = if (i == volumes.lastIndex) lastBarColor else barColor.copy(alpha = 0.55f),
                    topLeft = Offset(left, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stats.weeklyVolumeLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MutedColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = "This week so far: ${formatKg(volumes.lastOrNull() ?: 0.0)} kg",
            style = MaterialTheme.typography.bodySmall,
            color = MutedColor
        )
    }
}

@Composable
private fun SessionsWeekCard(stats: WorkoutStats, onClick: () -> Unit) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxCount = (stats.sessionsByDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val total = stats.sessionsByDay.sum()
    val highlight = MaterialTheme.colorScheme.primary

    StatsCardContainer(
        title = "Sessions This Week",
        subtitle = "$total session${if (total == 1) "" else "s"} · Mon – Sun · tap for details",
        icon = Icons.Filled.CalendarMonth,
        iconTint = Color(0xFF06B6D4),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stats.sessionsByDay.forEachIndexed { i, count ->
                val isToday = i == stats.todayIndex
                val fraction = count.toFloat() / maxCount
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) highlight else MutedColor
                    )
                    Box(
                        modifier = Modifier
                            .height(84.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val barH = if (count > 0) (8 + fraction * 76).dp else 0.dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(barH)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isToday) highlight
                                    else if (count > 0) Color(0xFF6366F1).copy(alpha = 0.45f)
                                    else TrackColor
                                )
                        )
                    }
                    Text(
                        text = dayLabels[i],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) highlight else MutedColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGridCard(stats: WorkoutStats, onClick: () -> Unit) {
    val tiles = listOf(
        QuadTile(Icons.Filled.Timer, "Total Workouts", stats.totalWorkouts.toString(), Color(0xFF8B5CF6)),
        QuadTile(Icons.Filled.FitnessCenter, "Sets · 7 days", stats.setsLast7Days.toString(), Color(0xFF10B981)),
        QuadTile(Icons.Filled.BarChart, "Volume · 7 days", "${formatKg(stats.volumeLast7DaysKg)} kg", Color(0xFF6366F1)),
        QuadTile(Icons.Filled.EmojiEvents, "Day Streak", stats.dayStreak.toString(), Color(0xFFF59E0B))
    )
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { tile ->
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(tile.tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tile.icon,
                                    contentDescription = null,
                                    tint = tile.tint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = tile.value,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = InkColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tile.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    // Keep the grid symmetric when the tile count is odd.
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class QuadTile(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val tint: Color
)

@Composable
private fun TopExercisesCard(stats: WorkoutStats, onClick: () -> Unit) {
    StatsCardContainer(
        title = "Top Exercises",
        subtitle = "Most sets in the last 7 days · tap for details",
        icon = Icons.Filled.TrendingUp,
        iconTint = Color(0xFF10B981),
        onClick = onClick
    ) {
        if (stats.topExercises.isEmpty()) {
            Text(
                text = "Nothing trained in the last 7 days.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedColor
            )
        } else {
            val maxSets = stats.topExercises.maxOf { it.second }.coerceAtLeast(1)
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            stats.topExercises.forEachIndexed { index, (name, sets) ->
                val fraction = sets.toFloat() / maxSets
                val animated by animateFloatAsState(
                    targetValue = if (visible) fraction else 0f,
                    animationSpec = tween(durationMillis = 600, delayMillis = index * 60),
                    label = "top-$index"
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = InkColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        Text(
                            text = "$sets sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(TrackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animated.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981))
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Stats detail dialogs (tap a card for more detail)

@Composable
private fun StatsDetailDialog(
    detail: StatsDetail,
    stats: WorkoutStats,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8F9FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkColor,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = InkColor
                            )
                        }
                    }
                }
                // Scrollable detail content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (detail) {
                        StatsDetail.INTENSITY -> IntensityDetailContent(stats)
                        StatsDetail.VOLUME -> VolumeDetailContent(stats)
                        StatsDetail.SESSIONS -> SessionsDetailContent(stats)
                        StatsDetail.GRID -> GridDetailContent(stats)
                        StatsDetail.EXERCISES -> ExercisesDetailContent(stats)
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkColor
            )
            content()
        }
    }
}

@Composable
private fun DetailEmptyText(text: String = "Nothing here yet — log workouts to see details.") {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MutedColor
    )
}

@Composable
private fun DetailPill(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedColor,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkColor,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun IntensityDetailContent(stats: WorkoutStats) {
    DetailSectionCard(title = "Last 7 days by body part") {
        val details = stats.bodyPartDetail
        if (details.isEmpty()) {
            DetailEmptyText("Nothing trained in the last 7 days.")
        } else {
            val strongest = details.first().group
            val weakest = details.last().group
            details.forEachIndexed { index, d ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(BodyPartColors[d.group] ?: Color(0xFF6366F1))
                        )
                        Text(
                            text = d.group,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (d.group == strongest) {
                            DetailPill("Strongest", Color(0xFF10B981))
                        } else if (details.size > 1 && d.group == weakest) {
                            DetailPill("Needs attention", Color(0xFFF59E0B))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${d.sets} sets", style = MaterialTheme.typography.bodySmall, color = MutedColor)
                        Text("${d.reps} reps", style = MaterialTheme.typography.bodySmall, color = MutedColor)
                        Text("${formatKg(d.volumeKg)} kg", style = MaterialTheme.typography.bodySmall, color = MutedColor)
                        Text(
                            "${d.sharePct.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = InkColor
                        )
                    }
                }
                if (index < details.lastIndex) {
                    HorizontalDivider(color = TrackColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun VolumeDetailContent(stats: WorkoutStats) {
    DetailSectionCard(title = "Weekly volume · last 8 weeks") {
        val details = stats.weeklyDetail
        if (details.isEmpty() || details.all { it.volumeKg <= 0 && it.sessions == 0 }) {
            DetailEmptyText()
        } else {
            details.reversed().forEachIndexed { index, w ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Week of ${w.rangeLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkColor
                        )
                        Text(
                            text = "${formatKg(w.volumeKg)} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${w.sessions} session${if (w.sessions == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                        val avg = if (w.sessions > 0) w.volumeKg / w.sessions else 0.0
                        Text(
                            text = "Avg ${formatKg(avg)} kg / session",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                    }
                }
                if (index < details.size - 1) {
                    HorizontalDivider(color = TrackColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun SessionsDetailContent(stats: WorkoutStats) {
    DetailSectionCard(title = "This week · Mon – Sun") {
        val days = stats.daySessions
        if (days.isEmpty() || days.all { it.sessions.isEmpty() }) {
            DetailEmptyText("No sessions logged this week yet.")
        } else {
            days.forEachIndexed { index, day ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${day.dayLabel} · ${day.dateLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (day.isToday) {
                            DetailPill("Today", MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (day.sessions.isEmpty()) {
                        Text(
                            text = "Rest day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                    } else {
                        day.sessions.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.exerciseName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                val dur = if (s.durationMinutes > 0) " · ${s.durationMinutes} min" else ""
                                Text(
                                    text = "${s.sets} sets$dur",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedColor
                                )
                            }
                        }
                    }
                }
                if (index < days.size - 1) {
                    HorizontalDivider(color = TrackColor, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun GridDetailContent(stats: WorkoutStats) {
    val a = stats.allTime
    DetailSectionCard(title = "All-time totals") {
        DetailRow("Workouts logged", a.workouts.toString())
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        DetailRow("Total sets", a.sets.toString())
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        DetailRow("Total reps", a.reps.toString())
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        DetailRow("Total volume", "${formatKg(a.volumeKg)} kg")
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        val avgDur = if (a.avgDurationMin > 0) "${a.avgDurationMin.toInt()} min / workout" else "—"
        DetailRow("Average duration", avgDur)
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        DetailRow("Longest streak", "$a.longestStreak day${if (a.longestStreak == 1) "" else "s"}")
    }
    DetailSectionCard(title = "Month over month") {
        DetailRow("This month", "${a.thisMonthWorkouts} workout${if (a.thisMonthWorkouts == 1) "" else "s"}")
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        DetailRow("Last month", "${a.lastMonthWorkouts} workout${if (a.lastMonthWorkouts == 1) "" else "s"}")
        HorizontalDivider(color = TrackColor, thickness = 1.dp)
        val delta = a.thisMonthWorkouts - a.lastMonthWorkouts
        val deltaText = when {
            delta > 0 -> "+$delta vs last month"
            delta < 0 -> "$delta vs last month"
            else -> "Same as last month"
        }
        val deltaColor = when {
            delta > 0 -> Color(0xFF10B981)
            delta < 0 -> Color(0xFFF43F5E)
            else -> MutedColor
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trend",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = deltaText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = deltaColor,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ExercisesDetailContent(stats: WorkoutStats) {
    DetailSectionCard(title = "All exercises · last 7 days") {
        val details = stats.exerciseDetail
        if (details.isEmpty()) {
            DetailEmptyText("Nothing trained in the last 7 days.")
        } else {
            val maxSets = details.maxOf { it.sets }.coerceAtLeast(1)
            details.forEachIndexed { index, e ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = e.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = InkColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        Text(
                            text = "${e.sets} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(TrackColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((e.sets.toFloat() / maxSets).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF10B981))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${e.reps} reps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                        Text(
                            text = "${formatKg(e.volumeKg)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedColor
                        )
                    }
                }
                if (index < details.lastIndex) {
                    HorizontalDivider(color = TrackColor, thickness = 1.dp)
                }
            }
        }
    }
}

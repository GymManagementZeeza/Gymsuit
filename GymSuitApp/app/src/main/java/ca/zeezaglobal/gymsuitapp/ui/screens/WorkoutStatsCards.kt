package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMuscleGroups
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
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
    val topExercises: List<Pair<String, Int>> // (name, sets) last 7 days, top 5
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
            topExercises = emptyList()
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

    val intensityByBodyPart = last7
        .groupBy { it.bodyPartGroup() }
        .mapValues { (_, ws) -> ws.sumOf { it.sets.size } }
        .filterValues { it > 0 }
        .toList()
        .sortedByDescending { it.second }

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
        topExercises = topExercises
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
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!stats.hasData) {
                EmptyStatsCard()
            } else {
                BodyPartIntensityCard(stats)
                VolumeTrendCard(stats)
                SessionsWeekCard(stats)
                StatsGridCard(stats)
                TopExercisesCard(stats)
            }
        }
    }
}

@Composable
private fun StatsCardContainer(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
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
private fun BodyPartIntensityCard(stats: WorkoutStats) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val maxSets = stats.intensityByBodyPart.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    StatsCardContainer(
        title = "Body Part Intensity",
        subtitle = "Sets logged in the last 7 days",
        icon = Icons.Filled.Whatshot,
        iconTint = Color(0xFFF43F5E)
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
private fun VolumeTrendCard(stats: WorkoutStats) {
    StatsCardContainer(
        title = "Training Volume",
        subtitle = "Total kg lifted per week · last 8 weeks",
        icon = Icons.Filled.BarChart,
        iconTint = Color(0xFF6366F1)
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
private fun SessionsWeekCard(stats: WorkoutStats) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxCount = (stats.sessionsByDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val total = stats.sessionsByDay.sum()
    val highlight = MaterialTheme.colorScheme.primary

    StatsCardContainer(
        title = "Sessions This Week",
        subtitle = "$total session${if (total == 1) "" else "s"} · Mon – Sun",
        icon = Icons.Filled.CalendarMonth,
        iconTint = Color(0xFF06B6D4)
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
private fun StatsGridCard(stats: WorkoutStats) {
    val tiles = listOf(
        QuadTile(Icons.Filled.Timer, "Total Workouts", stats.totalWorkouts.toString(), Color(0xFF8B5CF6)),
        QuadTile(Icons.Filled.FitnessCenter, "Sets · 7 days", stats.setsLast7Days.toString(), Color(0xFF10B981)),
        QuadTile(Icons.Filled.BarChart, "Volume · 7 days", "${formatKg(stats.volumeLast7DaysKg)} kg", Color(0xFF6366F1)),
        QuadTile(Icons.Filled.EmojiEvents, "Day Streak", stats.dayStreak.toString(), Color(0xFFF59E0B))
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
private fun TopExercisesCard(stats: WorkoutStats) {
    StatsCardContainer(
        title = "Top Exercises",
        subtitle = "Most sets in the last 7 days",
        icon = Icons.Filled.TrendingUp,
        iconTint = Color(0xFF10B981)
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

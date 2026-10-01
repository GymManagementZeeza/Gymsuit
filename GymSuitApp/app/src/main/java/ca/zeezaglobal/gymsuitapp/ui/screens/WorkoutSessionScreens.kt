package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSet
import kotlinx.coroutines.delay

// MARK: - Session state

/** Target number of exercises per body part that fills its progress bar. */
private const val EXERCISES_PER_PART_TARGET = 3
private const val DEFAULT_SET_COUNT = 3
private const val DEFAULT_REST_SECONDS = 60

private class DoneExercise(val exerciseId: String, val part: String, val sets: Int)

/** Hoisted state of one running workout: stopwatch plus what has been completed per body part. */
@Stable
class WorkoutSessionState {
    var active by mutableStateOf(false)
        private set
    var parts by mutableStateOf<List<String>>(emptyList())
        private set
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    var running by mutableStateOf(true)

    private val done = mutableStateListOf<DoneExercise>()

    fun start(bodyParts: List<String>) {
        parts = bodyParts
        elapsedSeconds = 0
        running = true
        done.clear()
        active = true
    }

    fun end() {
        active = false
        done.clear()
    }

    fun tick() {
        elapsedSeconds++
    }

    internal fun record(exercise: Exercise, part: String, sets: Int) {
        done.add(DoneExercise(exercise.id, part, sets))
    }

    fun exercisesDone(part: String): Int = done.filter { it.part == part }.map { it.exerciseId }.distinct().size
    fun setsDone(part: String): Int = done.filter { it.part == part }.sumOf { it.sets }
    fun isExerciseDone(exerciseId: String): Boolean = done.any { it.exerciseId == exerciseId }

    fun progress(part: String): Float =
        (exercisesDone(part).toFloat() / EXERCISES_PER_PART_TARGET).coerceIn(0f, 1f)

    fun overallProgress(): Float =
        if (parts.isEmpty()) 0f else parts.map { progress(it) }.average().toFloat()
}

private fun formatClock(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// MARK: - Session page: timer, pause/resume, progress per body part

@Composable
fun WorkoutSessionPage(
    session: WorkoutSessionState,
    onFinish: () -> Unit,
    onSelectBodyPart: (String) -> Unit
) {
    var confirmFinish by remember { mutableStateOf(false) }
    // The session can only be left by finishing it
    BackHandler { confirmFinish = true }

    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("End this workout?") },
            text = { Text("Your logged exercises are saved. The timer and progress will be cleared.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    onFinish()
                }) { Text("End workout") }
            },
            dismissButton = {
                TextButton(onClick = { confirmFinish = false }) { Text("Keep going") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Workout in progress",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Timer card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (session.running) "ELAPSED" else "PAUSED",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = formatClock(session.elapsedSeconds),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                LinearProgressIndicator(
                    progress = { session.overallProgress() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFF1E293B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { session.running = !session.running },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(
                            imageVector = if (session.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (session.running) "Pause" else "Resume",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { confirmFinish = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Body parts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tap a body part to pick an exercise",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(session.parts) { _, part ->
                val progress by animateFloatAsState(session.progress(part), label = "part_progress")
                val exercisesDone = session.exercisesDone(part)
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBodyPart(part) }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = part,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$exercisesDone/$EXERCISES_PER_PART_TARGET exercises • ${session.setsDone(part)} sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (progress >= 1f) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Done",
                                    tint = Color(0xFF10B981)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = if (progress >= 1f) Color(0xFF10B981) else Color(0xFF2563EB),
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Set-by-set logging with set stopwatch and rest countdown

private enum class SetPhase { WORKING, RESTING }

@Composable
fun SetBySetLogPage(
    exercise: Exercise,
    sessionRunning: Boolean,
    onBack: () -> Unit,
    onFinished: (LoggedWorkout) -> Unit
) {
    BackHandler(onBack = onBack)

    var totalSets by remember(exercise.id) { mutableIntStateOf(DEFAULT_SET_COUNT) }
    val completed = remember(exercise.id) { mutableStateListOf<WorkoutSet>() }
    var phase by remember(exercise.id) { mutableStateOf(SetPhase.WORKING) }
    var repsText by remember(exercise.id) { mutableStateOf("10") }
    var weightText by remember(exercise.id) { mutableStateOf("0") }
    var setSeconds by remember(exercise.id) { mutableIntStateOf(0) }
    var restTotal by remember(exercise.id) { mutableIntStateOf(DEFAULT_REST_SECONDS) }
    var restLeft by remember(exercise.id) { mutableIntStateOf(DEFAULT_REST_SECONDS) }
    var totalSeconds by remember(exercise.id) { mutableIntStateOf(0) }
    val startedAt = remember(exercise.id) { System.currentTimeMillis() }

    // One shared 1s tick; it stops while the session is paused
    LaunchedEffect(exercise.id, sessionRunning) {
        while (sessionRunning) {
            delay(1000)
            totalSeconds++
            if (phase == SetPhase.WORKING) {
                setSeconds++
            } else if (restLeft > 0) {
                restLeft--
                if (restLeft == 0) {
                    phase = SetPhase.WORKING
                    setSeconds = 0
                }
            }
        }
    }

    fun finish() {
        if (completed.isEmpty()) return
        onFinished(
            LoggedWorkout(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                timestampMillis = minOf(startedAt + totalSeconds * 1000L, System.currentTimeMillis()),
                sets = completed.toList(),
                durationMinutes = maxOf(1, (totalSeconds + 59) / 60),
                notes = ""
            )
        )
    }

    val currentSet = completed.size + 1
    val allDone = completed.size >= totalSets

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name.replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Text(
                    text = "${exercise.primaryMuscleLabel} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatClock(totalSeconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        exercise.gifUrl?.let { url ->
            GifWebView(
                url = url,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        // Set progress
        Text(
            text = if (allDone) "All $totalSets sets done" else "Set $currentSet of $totalSets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        LinearProgressIndicator(
            progress = { completed.size.toFloat() / totalSets },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
        )

        if (!allDone && phase == SetPhase.WORKING) {
            // Active set: stopwatch + reps/weight for this set only
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SET $currentSet • ${formatClock(setSeconds)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = repsText,
                            onValueChange = { repsText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Reps") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { v ->
                                weightText = v.filter { it.isDigit() || it == '.' }.take(6)
                            },
                            label = { Text("Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            completed.add(
                                WorkoutSet(
                                    reps = repsText.toIntOrNull() ?: 0,
                                    weightKg = weightText.toDoubleOrNull() ?: 0.0
                                )
                            )
                            if (completed.size < totalSets) {
                                restLeft = restTotal
                                phase = SetPhase.RESTING
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Complete set $currentSet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (!allDone) {
            // Rest countdown
            val restProgress by animateFloatAsState(
                targetValue = if (restTotal > 0) restLeft.toFloat() / restTotal else 0f,
                label = "rest_progress"
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "REST",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = formatClock(restLeft),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    LinearProgressIndicator(
                        progress = { restProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = Color(0xFF8B5CF6),
                        trackColor = Color(0xFF1E293B)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = {
                            restLeft += 15
                            restTotal += 15
                        }) { Text("+15s") }
                        Button(
                            onClick = {
                                restLeft = 0
                                phase = SetPhase.WORKING
                                setSeconds = 0
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            )
                        ) { Text("Skip rest", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        // Completed sets
        if (completed.isNotEmpty()) {
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    completed.forEachIndexed { index, set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Set ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 10.dp).weight(1f)
                            )
                            Text(
                                text = "${set.reps} reps × ${
                                    if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt() else set.weightKg
                                } kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (!allDone) {
                    OutlinedButton(
                        onClick = ::finish,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Finish early") }
                }
                Button(
                    onClick = {
                        if (allDone) finish() else totalSets++
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(if (allDone) "Save exercise" else "Add a set") }
                if (allDone) {
                    OutlinedButton(
                        onClick = {
                            totalSets++
                            phase = SetPhase.WORKING
                            setSeconds = 0
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Add a set") }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

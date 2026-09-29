package ca.zeezaglobal.gymsuitapp.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.PointsStore
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMuscleGroups
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSet
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val WorkoutGreen = Color(0xFF059669)
private val InkPrimary = Color(0xFF111827)
private val InkSecondary = Color(0xFF6B7280)
private val InkTertiary = Color(0xFF9CA3AF)
private val Divider = Color(0xFFE5E7EB)
private val SurfaceBg = Color(0xFFF9FAFB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen() {
    val context = LocalContext.current
    val workoutStore = remember { WorkoutStore(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()

    var tabIndex by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var loggingExercise by remember { mutableStateOf<Exercise?>(null) }
    var loggedWorkouts by remember { mutableStateOf(workoutStore.getLoggedWorkouts()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Workouts",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
            Spacer(Modifier.weight(1f))
            PointsPill()
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            listOf("Exercises", "History").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (tabIndex == 0) {
            ExercisesContent(
                workoutStore = workoutStore,
                query = query,
                onQueryChange = { query = it },
                selectedGroup = selectedGroup,
                onGroupSelected = { group -> selectedGroup = if (selectedGroup == group) null else group },
                onExerciseClick = { loggingExercise = it }
            )
        } else {
            HistoryContent(
                loggedWorkouts = loggedWorkouts,
                onDelete = { id ->
                    loggedWorkouts = workoutStore.deleteWorkout(id)
                }
            )
        }
    }

    loggingExercise?.let { exercise ->
        WorkoutLogSheet(
            exercise = exercise,
            onDismiss = { loggingExercise = null },
            onSaved = { workout ->
                loggedWorkouts = workoutStore.logWorkout(workout)
                PointsStore.init(context)
                PointsStore.awardForWorkout(
                    exerciseName = workout.exerciseName,
                    setCount = workout.sets.size,
                    durationMinutes = workout.durationMinutes
                )
                loggingExercise = null
                scope.launch {
                    val ok = healthConnectManager.insertExerciseSession(
                        exerciseName = workout.exerciseName,
                        start = Instant.ofEpochMilli(workout.timestampMillis)
                            .minusSeconds(workout.durationMinutes * 60L),
                        end = Instant.ofEpochMilli(workout.timestampMillis),
                        setCount = workout.sets.size,
                        totalReps = workout.totalReps,
                        totalVolumeKg = workout.totalVolumeKg
                    )
                    if (ok) loggedWorkouts = workoutStore.markSynced(workout.id)
                }
            }
        )
    }
}

@Composable
private fun ExercisesContent(
    workoutStore: WorkoutStore,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedGroup: String?,
    onGroupSelected: (String?) -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search exercises") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = InkTertiary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MuscleChip(label = "All", selected = selectedGroup == null, onClick = { onGroupSelected(null) })
            ExerciseMuscleGroups.all.forEach { group ->
                MuscleChip(label = group, selected = selectedGroup == group, onClick = { onGroupSelected(group) })
            }
        }
        Spacer(Modifier.height(10.dp))
        val results = workoutStore.filteredExercises(query, selectedGroup)
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (workoutStore.exercises.isEmpty()) "Exercise catalog failed to load."
                    else "No exercises match your search.",
                    color = InkSecondary, fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, bottom = 20.dp
                )
            ) {
                items(results, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise) })
                }
            }
        }
    }
}

@Composable
private fun MuscleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) WorkoutGreen else Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else InkPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WorkoutGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = WorkoutGreen)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary, maxLines = 1)
                Text(
                    "${exercise.primaryMuscleLabel} • ${exercise.equipment}",
                    fontSize = 12.sp, color = InkSecondary, maxLines = 1
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = InkTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HistoryContent(
    loggedWorkouts: List<LoggedWorkout>,
    onDelete: (String) -> Unit
) {
    if (loggedWorkouts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = WorkoutGreen.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                Text("No workouts logged yet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                Text("Pick an exercise and log your sets to start tracking.", fontSize = 14.sp, color = InkSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, bottom = 20.dp
            )
        ) {
            items(loggedWorkouts, key = { it.id }) { workout ->
                HistoryRow(workout = workout, onDelete = { onDelete(workout.id) })
            }
        }
    }
}

@Composable
private fun HistoryRow(workout: LoggedWorkout, onDelete: () -> Unit) {
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val dateStr = remember(workout.timestampMillis) {
        Instant.ofEpochMilli(workout.timestampMillis).atZone(ZoneId.systemDefault()).format(dateFmt)
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(workout.exerciseName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                Text(
                    "${workout.sets.size} sets • ${workout.totalReps} reps • ${workout.durationMinutes} min",
                    fontSize = 12.sp, color = InkSecondary
                )
                Text(dateStr, fontSize = 12.sp, color = InkTertiary)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${workout.totalVolumeKg.toInt()} kg", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (workout.syncedToHealth) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (workout.syncedToHealth) WorkoutGreen else Color(0xFFF59E0B),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        if (workout.syncedToHealth) "Synced" else "Not synced",
                        fontSize = 11.sp,
                        color = if (workout.syncedToHealth) WorkoutGreen else Color(0xFFF59E0B)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.7f))
            }
        }
    }
}

// MARK: - Log sheet

private class SetRowUi(reps: String, weight: String) {
    var repsText by mutableStateOf(reps)
    var weightText by mutableStateOf(weight)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutLogSheet(
    exercise: Exercise,
    onDismiss: () -> Unit,
    onSaved: (LoggedWorkout) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val setRows = remember(exercise.id) {
        mutableStateListOf<SetRowUi>().apply {
            repeat(3) { add(SetRowUi("10", "0")) }
        }
    }
    var durationMinutes by remember { mutableStateOf(30) }
    var notes by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(LocalDate.now()) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Log Workout", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkPrimary)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                    Text("${exercise.primaryMuscleLabel} • ${exercise.equipment}", fontSize = 13.sp, color = InkSecondary)
                    exercise.gifUrl?.let { url ->
                        GifWebView(
                            url = url,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    if (exercise.instructions.isNotEmpty()) {
                        Text("How to perform", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                        exercise.instructions.forEachIndexed { index, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${index + 1}.", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WorkoutGreen)
                                Text(step, fontSize = 14.sp, color = InkSecondary)
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sets", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            val last = setRows.lastOrNull()
                            setRows.add(SetRowUi(last?.repsText ?: "10", last?.weightText ?: "0"))
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = WorkoutGreen, modifier = Modifier.size(18.dp))
                            Text("Add set", color = WorkoutGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    setRows.forEachIndexed { index, row ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceBg)
                                .padding(10.dp)
                        ) {
                            Text("Set ${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary, modifier = Modifier.width(52.dp))
                            OutlinedTextField(
                                value = row.repsText,
                                onValueChange = { row.repsText = it.filter(Char::isDigit) },
                                label = { Text("Reps", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(80.dp)
                            )
                            OutlinedTextField(
                                value = row.weightText,
                                onValueChange = { row.weightText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("kg", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(90.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            if (setRows.size > 1) {
                                IconButton(onClick = { setRows.removeAt(index) }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Remove set", tint = Color(0xFFEF4444).copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Duration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { if (durationMinutes > 1) durationMinutes-- }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Less", tint = WorkoutGreen)
                        }
                        Text("$durationMinutes min", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = InkPrimary)
                        IconButton(onClick = { if (durationMinutes < 300) durationMinutes++ }) {
                            Icon(Icons.Filled.Add, contentDescription = "More", tint = WorkoutGreen)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Date", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> logDate = LocalDate.of(y, m + 1, d) },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Text(
                                logDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                color = WorkoutGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Notes (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            saveMessage?.let { Text(it, fontSize = 13.sp, color = InkSecondary, modifier = Modifier.fillMaxWidth()) }

            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    val sets = setRows.map {
                        WorkoutSet(reps = it.repsText.toIntOrNull() ?: 0, weightKg = it.weightText.toDoubleOrNull() ?: 0.0)
                    }
                    val timestamp = logDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        .coerceAtMost(Instant.now().toEpochMilli())
                    val workout = LoggedWorkout(
                        exerciseId = exercise.id,
                        exerciseName = exercise.name,
                        timestampMillis = timestamp,
                        sets = sets,
                        durationMinutes = durationMinutes,
                        notes = notes
                    )
                    onSaved(workout)
                },
                enabled = !isSaving && setRows.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = WorkoutGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving..." else "Log Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun GifWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0x00000000)
                loadUrl(url)
            }
        }
    )
}

package ca.zeezaglobal.gymsuitapp.ui.screens

import android.app.DatePickerDialog
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.PointsStore
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen() {
    val context = LocalContext.current
    val workoutStore = remember { WorkoutStore(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()

    var tabIndex by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var loggingExercise by remember { mutableStateOf<Exercise?>(null) }
    var loggedWorkouts by remember { mutableStateOf(workoutStore.getLoggedWorkouts()) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(Modifier.height(6.dp))

        // Material 3 Single Choice Segmented Button Row
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("Exercises", "History").forEachIndexed { index, label ->
                SegmentedButton(
                    selected = tabIndex == index,
                    onClick = { tabIndex = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

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
        // Material 3 Outlined Search Field with Pill Shape
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search exercises",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Material 3 Filter Chips Row
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedGroup == null,
                onClick = { onGroupSelected(null) },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (selectedGroup == null) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedGroup == null,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent
                )
            )

            ExerciseMuscleGroups.all.forEach { group ->
                val isSelected = selectedGroup == group
                FilterChip(
                    selected = isSelected,
                    onClick = { onGroupSelected(group) },
                    label = { Text(group, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val results = workoutStore.filteredExercises(query, selectedGroup)
        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (workoutStore.exercises.isEmpty()) "Exercise catalog failed to load."
                        else "No exercises match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(results, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = { onExerciseClick(exercise) })
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp, pressedElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${exercise.primaryMuscleLabel} • ${exercise.equipment}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun HistoryContent(
    loggedWorkouts: List<LoggedWorkout>,
    onDelete: (String) -> Unit
) {
    if (loggedWorkouts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "No workouts logged yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pick an exercise and log your sets to start tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
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
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = workout.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${workout.sets.size} sets • ${workout.totalReps} reps • ${workout.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${workout.totalVolumeKg.toInt()} kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (workout.syncedToHealth) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (workout.syncedToHealth) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (workout.syncedToHealth) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (workout.syncedToHealth) "Synced" else "Not synced",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (workout.syncedToHealth) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
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
    var durationMinutes by remember { mutableIntStateOf(30) }
    var notes by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(LocalDate.now()) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Workout",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Exercise Detail OutlinedCard
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${exercise.primaryMuscleLabel} • ${exercise.equipment}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        Text(
                            text = "How to perform",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        exercise.instructions.forEachIndexed { index, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Sets OutlinedCard
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = {
                                val last = setRows.lastOrNull()
                                setRows.add(SetRowUi(last?.repsText ?: "10", last?.weightText ?: "0"))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Add set", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    setRows.forEachIndexed { index, row ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Set ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(52.dp)
                            )
                            OutlinedTextField(
                                value = row.repsText,
                                onValueChange = { row.repsText = it.filter(Char::isDigit) },
                                label = { Text("Reps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(82.dp)
                            )
                            OutlinedTextField(
                                value = row.weightText,
                                onValueChange = { row.weightText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("kg") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(92.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            if (setRows.size > 1) {
                                IconButton(onClick = { setRows.removeAt(index) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Remove set",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Duration, Date & Notes Card
            OutlinedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Duration Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        FilledTonalIconButton(
                            onClick = { if (durationMinutes > 1) durationMinutes-- },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease duration", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "$durationMinutes min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { if (durationMinutes < 300) durationMinutes++ },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase duration", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Date Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> logDate = LocalDate.of(y, m + 1, d) },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = logDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Notes Field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Notes (optional)") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            saveMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Primary M3 Action Button
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = if (isSaving) "Saving..." else "Log Workout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
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

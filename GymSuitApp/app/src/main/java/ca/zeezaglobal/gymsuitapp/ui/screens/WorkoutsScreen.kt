package ca.zeezaglobal.gymsuitapp.ui.screens

import android.app.DatePickerDialog
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.PointsStore
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.HealthWorkoutSignals
import ca.zeezaglobal.gymsuitapp.data.model.Exercise
import ca.zeezaglobal.gymsuitapp.data.model.ExerciseMuscleGroups
import ca.zeezaglobal.gymsuitapp.data.model.LoggedWorkout
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

// MARK: - Workout Split & Body Parts Data Models
data class BodyPartItem(
    val id: String,
    val name: String,
    val subtitle: String
)

val defaultBodyParts = listOf(
    BodyPartItem(id = "chest", name = "Chest", subtitle = "Pecs, push-ups, dumbbells"),
    BodyPartItem(id = "back", name = "Back", subtitle = "Lats, traps, rows"),
    BodyPartItem(id = "shoulders", name = "Shoulders", subtitle = "Deltoids, presses"),
    BodyPartItem(id = "biceps", name = "Biceps", subtitle = "Curls, dumbbells"),
    BodyPartItem(id = "triceps", name = "Triceps", subtitle = "Extensions, dips"),
    BodyPartItem(id = "legs", name = "Legs", subtitle = "Quads, hamstrings, glutes"),
    BodyPartItem(id = "core", name = "Core / Abs", subtitle = "Planks, crunches, rotational"),
    BodyPartItem(id = "calves", name = "Calves", subtitle = "Calf raises, jumps")
)

data class SplitExerciseItem(
    val exerciseName: String,
    val catalogExerciseId: String,
    val type: String, // "Compound" or "Isolation"
    val muscles: String,
    val setsCount: Int,
    val targetReps: String,
    val targetRepsInt: Int,
    val targetWeight: String,
    val targetWeightKg: Double
)

data class WorkoutSplitPreset(
    val id: String,
    val name: String, // "Push", "Pull", "Legs"
    val title: String,
    val durationText: String,
    val difficultyText: String,
    val description: String,
    val exercises: List<SplitExerciseItem>
)

val workoutSplits = listOf(
    WorkoutSplitPreset(
        id = "push",
        name = "Push",
        title = "Push Day",
        durationText = "55-70 minutes",
        difficultyText = "Intermediate",
        description = "Focus on chest, anterior deltoids, and triceps with pressing mechanics. Prioritize clean form and controlled eccentrics on every repetition.",
        exercises = listOf(
            SplitExerciseItem(
                exerciseName = "Barbell Bench Press",
                catalogExerciseId = "0025",
                type = "Compound",
                muscles = "Chest, Front Deltoids, Triceps",
                setsCount = 4,
                targetReps = "8-10 Reps",
                targetRepsInt = 8,
                targetWeight = "65kg",
                targetWeightKg = 65.0
            ),
            SplitExerciseItem(
                exerciseName = "Overhead Shoulder Press",
                catalogExerciseId = "0997",
                type = "Compound",
                muscles = "Shoulders, Upper Chest, Triceps",
                setsCount = 3,
                targetReps = "8-12 Reps",
                targetRepsInt = 10,
                targetWeight = "40kg",
                targetWeightKg = 40.0
            ),
            SplitExerciseItem(
                exerciseName = "Incline Dumbbell Press",
                catalogExerciseId = "1254",
                type = "Compound",
                muscles = "Upper Chest, Anterior Deltoids",
                setsCount = 3,
                targetReps = "10-12 Reps",
                targetRepsInt = 10,
                targetWeight = "22kg",
                targetWeightKg = 22.0
            ),
            SplitExerciseItem(
                exerciseName = "Triceps Dips",
                catalogExerciseId = "0019",
                type = "Compound",
                muscles = "Triceps, Lower Chest",
                setsCount = 3,
                targetReps = "10-15 Reps",
                targetRepsInt = 12,
                targetWeight = "Bodyweight",
                targetWeightKg = 0.0
            )
        )
    ),
    WorkoutSplitPreset(
        id = "pull",
        name = "Pull",
        title = "Pull Day",
        durationText = "50-65 minutes",
        difficultyText = "Intermediate",
        description = "Target the entire posterior chain including lats, rhomboids, rear delts, and biceps. Focus on full scapular retraction and a squeeze at peak contraction.",
        exercises = listOf(
            SplitExerciseItem(
                exerciseName = "Pull-Ups",
                catalogExerciseId = "0652",
                type = "Compound",
                muscles = "Latissimus Dorsi, Upper Back, Biceps",
                setsCount = 4,
                targetReps = "6-10 Reps",
                targetRepsInt = 8,
                targetWeight = "Bodyweight",
                targetWeightKg = 0.0
            ),
            SplitExerciseItem(
                exerciseName = "Barbell Bent Over Row",
                catalogExerciseId = "0027",
                type = "Compound",
                muscles = "Lats, Rhomboids, Lower Back",
                setsCount = 4,
                targetReps = "8-10 Reps",
                targetRepsInt = 8,
                targetWeight = "60kg",
                targetWeightKg = 60.0
            ),
            SplitExerciseItem(
                exerciseName = "Cable Lat Pulldown",
                catalogExerciseId = "2330",
                type = "Compound",
                muscles = "Lats, Middle Back, Biceps",
                setsCount = 3,
                targetReps = "10-12 Reps",
                targetRepsInt = 10,
                targetWeight = "50kg",
                targetWeightKg = 50.0
            ),
            SplitExerciseItem(
                exerciseName = "Bicep Curls",
                catalogExerciseId = "1634",
                type = "Isolation",
                muscles = "Biceps Brachii, Forearms",
                setsCount = 3,
                targetReps = "12-15 Reps",
                targetRepsInt = 12,
                targetWeight = "14kg",
                targetWeightKg = 14.0
            )
        )
    ),
    WorkoutSplitPreset(
        id = "legs",
        name = "Legs",
        title = "Legs Day",
        durationText = "60-75 minutes",
        difficultyText = "Intermediate",
        description = "Focus on compound movements to maximize muscle recruitment and strength gains. Rest for 90-120 seconds between heavy sets.",
        exercises = listOf(
            SplitExerciseItem(
                exerciseName = "Squats",
                catalogExerciseId = "0043",
                type = "Compound",
                muscles = "Quadriceps, Hamstrings, Glutes",
                setsCount = 4,
                targetReps = "8-12 Reps",
                targetRepsInt = 10,
                targetWeight = "70kg",
                targetWeightKg = 70.0
            ),
            SplitExerciseItem(
                exerciseName = "Romanian Deadlifts",
                catalogExerciseId = "0085",
                type = "Compound",
                muscles = "Hamstrings, Glutes, Lower Back",
                setsCount = 4,
                targetReps = "8-10 Reps",
                targetRepsInt = 8,
                targetWeight = "80kg",
                targetWeightKg = 80.0
            ),
            SplitExerciseItem(
                exerciseName = "Leg Press",
                catalogExerciseId = "0739",
                type = "Compound",
                muscles = "Quadriceps, Glutes",
                setsCount = 3,
                targetReps = "10-12 Reps",
                targetRepsInt = 10,
                targetWeight = "120kg",
                targetWeightKg = 120.0
            ),
            SplitExerciseItem(
                exerciseName = "Lunges",
                catalogExerciseId = "0336",
                type = "Compound",
                muscles = "Quadriceps, Glutes, Calves",
                setsCount = 3,
                targetReps = "12-15 Reps",
                targetRepsInt = 12,
                targetWeight = "16kg",
                targetWeightKg = 16.0
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(session: WorkoutSessionState = remember { WorkoutSessionState() }) {
    val context = LocalContext.current
    val workoutStore = remember { WorkoutStore(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()

    var selectedSplitIndex by remember { mutableIntStateOf(2) } // Default to "Legs" matching reference design
    var isCatalogMode by remember { mutableStateOf(false) }
    var sessionPart by remember { mutableStateOf<String?>(null) }
    var setLogExercise by remember { mutableStateOf<Exercise?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    
    // Active logging exercise and its preset configuration
    var loggingExercise by remember { mutableStateOf<Exercise?>(null) }
    var loggingTargetReps by remember { mutableIntStateOf(10) }
    var loggingTargetWeightKg by remember { mutableStateOf(0.0) }
    var loggingSetCount by remember { mutableIntStateOf(3) }
    
    var loggedWorkouts by remember { mutableStateOf(workoutStore.getLoggedWorkouts()) }
    var selectedCalendarDate by remember { mutableStateOf(LocalDate.now()) }
    val completedDates = remember(loggedWorkouts) {
        loggedWorkouts.map {
            Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    // On-device AI recommendation state with loading shimmer
    var aiRecommendation by remember { mutableStateOf<WorkoutAiRecommendation?>(null) }
    var isAiLoading by remember { mutableStateOf(true) }

    // Selected body parts (max 3, none selected until the user picks them)
    var selectedBodyParts by remember { mutableStateOf(emptyList<String>()) }

    // Session stopwatch keeps counting even while a sub-page is open; pause stops it
    LaunchedEffect(session.active, session.running) {
        while (session.active && session.running) {
            delay(1000)
            session.tick()
        }
    }

    LaunchedEffect(selectedCalendarDate, completedDates) {
        isAiLoading = true
        // Simulate in-device AI model inferencing and show smooth shimmer
        withContext(Dispatchers.Default) {
            val health = try {
                healthConnectManager.readWorkoutSignals()
            } catch (e: Exception) {
                HealthWorkoutSignals()
            }
            val recommendation = OnDeviceWorkoutAiEngine.generateRecommendation(
                selectedDate = selectedCalendarDate,
                exercises = workoutStore.exercises,
                loggedWorkouts = loggedWorkouts,
                health = health
            )
            delay(650) // Realistic on-device neural/rule processing delay to display shimmer
            withContext(Dispatchers.Main) {
                aiRecommendation = recommendation
                isAiLoading = false
            }
        }
    }

    val onWorkoutSaved: (LoggedWorkout) -> Unit = { workout ->
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
                ca.zeezaglobal.gymsuitapp.di.AppComponent.from(context).healthSyncManager.sync()
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (session.active && setLogExercise != null) {
            val exercise = setLogExercise!!
            SetBySetLogPage(
                exercise = exercise,
                sessionRunning = session.running,
                onBack = { setLogExercise = null },
                onFinished = { workout ->
                    onWorkoutSaved(workout)
                    session.record(
                        exercise,
                        OnDeviceWorkoutAiEngine.bodyPartNameFor(exercise) ?: sessionPart.orEmpty(),
                        workout.sets.size
                    )
                    setLogExercise = null
                }
            )
        } else if (session.active && sessionPart != null) {
            BackHandler { sessionPart = null }
            BodyPartWorkoutPickerPage(
                bodyParts = listOf(sessionPart!!),
                exercises = workoutStore.exercises,
                onBack = { sessionPart = null },
                onExerciseClick = { exercise -> setLogExercise = exercise }
            )
        } else if (session.active) {
            WorkoutSessionPage(
                session = session,
                onFinish = { session.end() },
                onSelectBodyPart = { sessionPart = it }
            )
        } else if (isCatalogMode) {
            BackHandler { isCatalogMode = false }
            // Secondary bar to return to Split view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Exercises Catalog",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = { isCatalogMode = false },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Splits", style = MaterialTheme.typography.labelMedium)
                }
            }

            ExercisesContent(
                workoutStore = workoutStore,
                query = query,
                onQueryChange = { query = it },
                selectedGroup = selectedGroup,
                onGroupSelected = { group -> selectedGroup = if (selectedGroup == group) null else group },
                onExerciseClick = { exercise ->
                    loggingExercise = exercise
                    loggingTargetReps = 10
                    loggingTargetWeightKg = 0.0
                    loggingSetCount = 3
                }
            )
        } else {
            val currentSplit = workoutSplits[selectedSplitIndex.coerceIn(0, workoutSplits.lastIndex)]
            WorkoutSplitContent(
                split = currentSplit,
                selectedSplitIndex = selectedSplitIndex,
                selectedCalendarDate = selectedCalendarDate,
                completedDates = completedDates,
                aiRecommendation = aiRecommendation,
                isAiLoading = isAiLoading,
                selectedBodyParts = selectedBodyParts,
                recommendedBodyParts = aiRecommendation?.recommendedBodyParts.orEmpty(),
                onToggleBodyPart = { partName ->
                    if (selectedBodyParts.contains(partName)) {
                        selectedBodyParts = selectedBodyParts - partName
                    } else if (selectedBodyParts.size < 3) {
                        selectedBodyParts = selectedBodyParts + partName
                    }
                },
                onRemoveBodyPart = { partName ->
                    selectedBodyParts = selectedBodyParts - partName
                },
                onStartAiWorkout = {
                    session.start(selectedBodyParts)
                },
                onSelectCalendarDate = { selectedCalendarDate = it },
                onSelectSplit = { selectedSplitIndex = it },
                onOpenCatalog = { isCatalogMode = true },
                onExerciseClick = { splitItem ->
                    // Match catalog exercise or synthesize fallback
                    val matched = workoutStore.exercises.find { it.id == splitItem.catalogExerciseId }
                        ?: workoutStore.exercises.find { it.name.contains(splitItem.exerciseName, ignoreCase = true) }
                        ?: Exercise(
                            id = splitItem.catalogExerciseId,
                            name = splitItem.exerciseName,
                            bodyPart = currentSplit.name.lowercase(),
                            equipment = "Gym Equipment",
                            primaryMuscle = splitItem.muscles.split(",").firstOrNull()?.trim() ?: "Muscles",
                            secondaryMuscles = emptyList(),
                            instructions = listOf("Perform ${splitItem.exerciseName} with proper form for ${splitItem.targetReps}."),
                            gif = "",
                            img = ""
                        )
                    loggingExercise = matched
                    loggingTargetReps = splitItem.targetRepsInt
                    loggingTargetWeightKg = splitItem.targetWeightKg
                    loggingSetCount = splitItem.setsCount
                }
            )
        }
    }

    loggingExercise?.let { exercise ->
        WorkoutLogSheet(
            exercise = exercise,
            initialTargetReps = loggingTargetReps,
            initialTargetWeightKg = loggingTargetWeightKg,
            initialSetCount = loggingSetCount,
            onDismiss = { loggingExercise = null },
            onSaved = onWorkoutSaved
        )
    }
}

// MARK: - Workout Split Content (Matches Reference Design)
@Composable
private fun WorkoutSplitContent(
    split: WorkoutSplitPreset,
    selectedSplitIndex: Int,
    selectedCalendarDate: LocalDate,
    completedDates: Set<LocalDate>,
    aiRecommendation: WorkoutAiRecommendation?,
    isAiLoading: Boolean,
    selectedBodyParts: List<String>,
    recommendedBodyParts: List<String>,
    onToggleBodyPart: (String) -> Unit,
    onRemoveBodyPart: (String) -> Unit,
    onStartAiWorkout: () -> Unit,
    onSelectCalendarDate: (LocalDate) -> Unit,
    onSelectSplit: (Int) -> Unit,
    onOpenCatalog: () -> Unit,
    onExerciseClick: (SplitExerciseItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Top Week Calendar Strip (Matching Reference Image)
        item {
            WeeklyWorkoutCalendarStrip(
                selectedDate = selectedCalendarDate,
                completedDates = completedDates,
                onSelectDate = onSelectCalendarDate
            )
        }

        // AI Recommendation Card with shimmer effect (Directly below date selection)
        item {
            AiRecommendationCard(
                recommendation = aiRecommendation,
                isLoading = isAiLoading,
                selectedBodyParts = selectedBodyParts,
                onRemoveBodyPart = onRemoveBodyPart,
                onStartWorkout = onStartAiWorkout
            )
        }

        // Body Parts Section Container (Matching Reference Image)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Row: "Body parts" + "8 parts" badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Body parts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF) // Soft light blue badge background
                        ) {
                            Text(
                                text = "${defaultBodyParts.size} parts",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB), // Vibrant blue
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Subtitle
                    Text(
                        text = "Choose the focus for your next session",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B)
                    )

                    Spacer(Modifier.height(2.dp))

                    // List of 8 Body Part Cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        defaultBodyParts.forEach { part ->
                            val isSelected = selectedBodyParts.contains(part.name)
                            val canAdd = selectedBodyParts.size < 3
                            val isRecommended = part.name in recommendedBodyParts
                            val glow by rememberInfiniteTransition(label = "recommend_glow").animateFloat(
                                initialValue = 0.25f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "recommend_glow_alpha"
                            )

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    if (isRecommended) 2.dp else 1.dp,
                                    when {
                                        isRecommended -> Color(0xFF8B5CF6).copy(alpha = glow)
                                        isSelected -> Color(0xFF2563EB).copy(alpha = 0.4f)
                                        else -> Color(0xFFF1F5F9)
                                    }
                                ),
                                shadowElevation = if (isRecommended) (2 + 10 * glow).dp else 0.5.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isRecommended) Modifier.shadow(
                                            elevation = (4 + 12 * glow).dp,
                                            shape = RoundedCornerShape(18.dp),
                                            ambientColor = Color(0xFF8B5CF6),
                                            spotColor = Color(0xFF8B5CF6)
                                        ) else Modifier
                                    )
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onToggleBodyPart(part.name) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = part.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            if (isRecommended) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.AutoAwesome,
                                                            contentDescription = null,
                                                            tint = Color(0xFF7C3AED),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = "Recommended",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF7C3AED)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(
                                            text = part.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    // Circular Add / Check Button
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color(0xFF10B981) else if (canAdd) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Add,
                                                contentDescription = if (isSelected) "Selected" else "Add body part",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
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

// MARK: - Exercise Card Matching Reference Image
@Composable
private fun SplitExerciseCard(
    item: SplitExerciseItem,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Exercise Name + Sets Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f, fill = false)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${item.setsCount} Sets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Subtitle: Type & Muscles
            Text(
                text = "${item.type} • ${item.muscles}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Target Pills Row: Target Reps pill + Target Weight pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = item.targetReps,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = item.targetWeight,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// MARK: - Weekly Workout Calendar Strip Matching Reference Image
data class WorkoutCalendarDay(
    val date: LocalDate,
    val dayName: String, // "Mon", "Tue", etc.
    val dayNumber: String, // "24", "25", etc.
    val isCompleted: Boolean
)

@Composable
private fun WeeklyWorkoutCalendarStrip(
    selectedDate: LocalDate,
    completedDates: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current week starting from Monday to Sunday enclosing selectedDate
    val monday = remember(selectedDate) {
        selectedDate.with(DayOfWeek.MONDAY)
    }
    val weekDays = remember(monday, completedDates) {
        (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            WorkoutCalendarDay(
                date = date,
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                dayNumber = date.dayOfMonth.toString(),
                isCompleted = date in completedDates
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { day ->
            val isSelected = day.date == selectedDate
            CalendarDayCard(
                day = day,
                isSelected = isSelected,
                onClick = { onSelectDate(day.date) }
            )
        }
    }
}

@Composable
private fun CalendarDayCard(
    day: WorkoutCalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(52.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF0F172A) else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.dayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Text(
                text = day.dayNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF0F172A)
            )

            if (day.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else {
                Spacer(Modifier.size(18.dp))
            }
        }
    }
}

// MARK: - On-Device AI Workout Recommendation
data class WorkoutAiRecommendation(
    val splitTarget: String, // e.g. "CHEST & TRICEPS"
    val targetSplitIndex: Int,
    val headline: String,
    val description: String,
    val recommendedBodyParts: List<String> = emptyList()
)

object OnDeviceWorkoutAiEngine {
    private const val RECOMMEND_COUNT = 2

    /** Maps a catalog exercise to one of the names in [defaultBodyParts]. */
    fun bodyPartNameFor(exercise: Exercise): String? = when (exercise.bodyPart) {
        "chest" -> "Chest"
        "back" -> "Back"
        "shoulders" -> "Shoulders"
        "upper arms" -> if (exercise.primaryMuscle.contains("tricep", ignoreCase = true)) "Triceps" else "Biceps"
        "lower arms" -> "Biceps"
        "upper legs" -> "Legs"
        "lower legs" -> "Calves"
        "waist" -> "Core / Abs"
        else -> null
    }

    private fun bodyPartFromTitle(title: String): String? {
        val t = title.lowercase()
        return when {
            "chest" in t || "bench" in t || "push" in t -> "Chest"
            "back" in t || "pull" in t || "row" in t -> "Back"
            "shoulder" in t -> "Shoulders"
            "bicep" in t || "curl" in t -> "Biceps"
            "tricep" in t -> "Triceps"
            "leg" in t || "squat" in t || "lunge" in t -> "Legs"
            "calf" in t || "calves" in t -> "Calves"
            "core" in t || "abs" in t || "plank" in t -> "Core / Abs"
            else -> null
        }
    }

    private fun splitIndexFor(parts: List<String>): Int = when (parts.firstOrNull()) {
        "Chest", "Shoulders", "Triceps" -> 0
        "Back", "Biceps" -> 1
        else -> 2
    }

    fun generateRecommendation(
        selectedDate: LocalDate,
        exercises: List<Exercise>,
        loggedWorkouts: List<LoggedWorkout>,
        health: HealthWorkoutSignals = HealthWorkoutSignals()
    ): WorkoutAiRecommendation {
        val zone = ZoneId.systemDefault()
        val byId = exercises.associateBy { it.id }

        // Body part -> most recent day it was trained (up to and including the selected date)
        val lastTrained = HashMap<String, LocalDate>()
        loggedWorkouts.forEach { log ->
            val date = Instant.ofEpochMilli(log.timestampMillis).atZone(zone).toLocalDate()
            if (date.isAfter(selectedDate)) return@forEach
            val exercise = byId[log.exerciseId] ?: exercises.find { it.name.equals(log.exerciseName, true) }
            val part = exercise?.let(::bodyPartNameFor) ?: return@forEach
            if (lastTrained[part]?.isBefore(date) != false) lastTrained[part] = date
        }

        // Health Connect sessions: map to a body part when the title names one, else count as general load
        val hcLoadDays = HashSet<LocalDate>()
        var hasUnmappedHcToday = false
        health.sessions.forEach { session ->
            if (session.date.isAfter(selectedDate)) return@forEach
            hcLoadDays.add(session.date)
            val part = exercises.find { it.name.equals(session.title, true) }?.let(::bodyPartNameFor)
                ?: bodyPartFromTitle(session.title)
            if (part != null) {
                if (lastTrained[part]?.isBefore(session.date) != false) lastTrained[part] = session.date
            } else if (session.date == selectedDate) {
                hasUnmappedHcToday = true
            }
        }

        val trainedToday = defaultBodyParts.map { it.name }.filter { lastTrained[it] == selectedDate }

        if (trainedToday.size + (if (hasUnmappedHcToday) 1 else 0) >= RECOMMEND_COUNT) {
            return WorkoutAiRecommendation(
                splitTarget = "DONE",
                targetSplitIndex = splitIndexFor(trainedToday),
                headline = "Good job today!",
                description = if (trainedToday.isEmpty()) "Health Connect shows a workout today. That's enough for today, so rest up and recover."
                else "You trained ${trainedToday.joinToString(" & ")} today${if (hasUnmappedHcToday) " plus a Health Connect workout" else ""}. That's enough for today, so rest up and recover.",
                recommendedBodyParts = emptyList()
            )
        }

        // Never-trained first, then the longest-rested; anything already done today is skipped
        val candidates = defaultBodyParts.map { it.name }
            .filter { it !in trainedToday }
            .sortedByDescending { part ->
                lastTrained[part]?.let { java.time.temporal.ChronoUnit.DAYS.between(it, selectedDate) } ?: Long.MAX_VALUE
            }
        // Recovery check from Health Connect: recent training load and last night's sleep
        val loggedDays = lastTrained.values.toSet() + hcLoadDays
        val recentLoadDays = (1..3).count { selectedDate.minusDays(it.toLong()) in loggedDays }
        val shortSleep = health.lastSleepMinutes?.let { it < 360 } == true
        val heavyLoad = recentLoadDays >= 3
        val pickCount = if (shortSleep || heavyLoad) 1 else RECOMMEND_COUNT
        val picks = candidates.take(pickCount)
        val recoveryNote = buildString {
            if (shortSleep) {
                val m = health.lastSleepMinutes!!
                append(" You slept ${m / 60}h ${m % 60}m, so keep the volume light.")
            }
            if (heavyLoad) append(" You've trained 3 days in a row, so don't overdo it.")
        }

        val reasons = picks.joinToString(" ") { part ->
            val last = lastTrained[part]
            if (last == null) "$part hasn't been trained yet."
            else {
                val days = java.time.temporal.ChronoUnit.DAYS.between(last, selectedDate)
                "$part was last trained $days day${if (days == 1L) "" else "s"} ago."
            }
        }
        val intro = if (trainedToday.isNotEmpty())
            "You already trained ${trainedToday.joinToString(", ")} today, so switch to something fresh. "
        else ""

        return WorkoutAiRecommendation(
            splitTarget = picks.joinToString(" & ") { it.substringBefore(" /").uppercase() },
            targetSplitIndex = splitIndexFor(picks),
            headline = if (trainedToday.isNotEmpty()) "Next Up" else "Today's Recommendation",
            description = intro + reasons + recoveryNote,
            recommendedBodyParts = picks
        )
    }
}

@Composable
private fun AiRecommendationCard(
    recommendation: WorkoutAiRecommendation?,
    isLoading: Boolean,
    selectedBodyParts: List<String>,
    onRemoveBodyPart: (String) -> Unit,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading || recommendation == null) {
        AiRecommendationShimmerCard(modifier = modifier)
    } else {
        // High-contrast dark navy card matching the reference image
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A), // Dark navy slate matching reference design
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row: "Today's Recommendation" + Badge (e.g. "LEG DAY")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recommendation.headline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B) // Subtle container badge
                    ) {
                        Text(
                            text = recommendation.splitTarget,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // AI description sentence matching reference design
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8), // Soft slate gray
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )

                // Body Parts in AI Card (Flowing badges from list below)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Body parts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )

                        Text(
                            text = "${selectedBodyParts.size}/3 part limit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Pills stay composed until their exit animation finishes
                    val shownParts = remember { mutableStateListOf<String>() }
                    val pillStates = remember { HashMap<String, MutableTransitionState<Boolean>>() }
                    LaunchedEffect(selectedBodyParts) {
                        selectedBodyParts.forEach { part ->
                            val state = pillStates.getOrPut(part) { MutableTransitionState(false) }
                            state.targetState = true
                            if (part !in shownParts) shownParts.add(part)
                        }
                        shownParts.filter { it !in selectedBodyParts }
                            .forEach { pillStates[it]?.targetState = false }
                    }

                    // Pills Row with smooth add/remove animation
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(tween(300)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shownParts.toList().forEach { part ->
                          key(part) {
                            val visibleState = pillStates.getOrPut(part) { MutableTransitionState(false) }
                            LaunchedEffect(visibleState.currentState, visibleState.isIdle) {
                                if (visibleState.isIdle && !visibleState.currentState && part !in selectedBodyParts) {
                                    shownParts.remove(part)
                                    pillStates.remove(part)
                                }
                            }
                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.6f) + expandHorizontally(tween(300)),
                                exit = fadeOut(tween(250)) + scaleOut(tween(250))
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onRemoveBodyPart(part) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = part,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                          }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Big White Pill Button "Start Workout"
                Button(
                    onClick = onStartWorkout,
                    enabled = selectedBodyParts.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A),
                        disabledContainerColor = Color.White.copy(alpha = 0.6f),
                        disabledContentColor = Color(0xFF0F172A).copy(alpha = 0.7f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (selectedBodyParts.isEmpty()) "Select Body Part" else "Start Workout",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBodyParts.isEmpty()) Color(0xFF0F172A).copy(alpha = 0.7f) else Color(0xFF0F172A)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiRecommendationShimmerCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ai_card_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF334155),
            Color(0xFF1E293B)
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row Shimmer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush)
                )
            }

            // Description Lines Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )

            Spacer(Modifier.height(2.dp))

            // Button Shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(shimmerBrush)
            )
        }
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
    initialTargetReps: Int = 10,
    initialTargetWeightKg: Double = 0.0,
    initialSetCount: Int = 3,
    onDismiss: () -> Unit,
    onSaved: (LoggedWorkout) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val setRows = remember(exercise.id) {
        mutableStateListOf<SetRowUi>().apply {
            val weightStr = if (initialTargetWeightKg > 0) {
                if (initialTargetWeightKg % 1.0 == 0.0) initialTargetWeightKg.toInt().toString() else initialTargetWeightKg.toString()
            } else "0"
            val count = if (initialSetCount in 1..8) initialSetCount else 3
            repeat(count) { add(SetRowUi(initialTargetReps.toString(), weightStr)) }
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
                    // Today logs use the current time so the Health Connect session lands on today
                    val timestamp = if (logDate == LocalDate.now()) Instant.now().toEpochMilli()
                    else logDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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

// MARK: - Workout picker page (exercises of the selected body parts, with animated demos)
@Composable
private fun BodyPartWorkoutPickerPage(
    bodyParts: List<String>,
    exercises: List<Exercise>,
    onBack: () -> Unit,
    onExerciseClick: (Exercise) -> Unit
) {
    var filterPart by remember { mutableStateOf<String?>(null) }
    val results = remember(bodyParts, exercises, filterPart) {
        exercises.filter { ex ->
            val part = OnDeviceWorkoutAiEngine.bodyPartNameFor(ex)
            if (filterPart != null) part == filterPart else part in bodyParts
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column {
                Text(
                    text = "Choose a workout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${results.size} exercises for ${bodyParts.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (bodyParts.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterPart == null,
                    onClick = { filterPart = null },
                    label = { Text("All") },
                    shape = RoundedCornerShape(8.dp)
                )
                bodyParts.forEach { part ->
                    FilterChip(
                        selected = filterPart == part,
                        onClick = { filterPart = if (filterPart == part) null else part },
                        label = { Text(part) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "No exercises found for this body part.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(results, key = { it.id }) { exercise ->
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                val gif = exercise.gifUrl
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (gif != null) {
                                        GifWebView(url = gif, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = exercise.name.replaceFirstChar { it.uppercaseChar() },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "${exercise.primaryMuscleLabel} • ${exercise.equipment}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            // The WebView swallows touches, so capture taps on an overlay
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { onExerciseClick(exercise) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GifWebView(url: String, modifier: Modifier = Modifier) {
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

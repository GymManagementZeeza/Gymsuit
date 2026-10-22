package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.WorkoutOnboardingStore
import ca.zeezaglobal.gymsuitapp.data.model.WorkoutGoal
import ca.zeezaglobal.gymsuitapp.data.remote.WorkoutOnboardingApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The workout onboarding ("goal setup") flow, presented as a full-screen
 * cover over the dashboard — the Android mirror of the iOS WorkoutsView
 * presented via `fullScreenCover` when the Workouts bottom-nav tab is tapped.
 *
 * Until setup completes, a 7-step wizard collects goal, height, weight,
 * target weight, birth year and weekly frequency, then shows a projection
 * of the target date. Finishing syncs the answers to the backend
 * (POST /api/mobile/onboarding/sync) and flips `workout_goal_setup_completed`,
 * after which this same cover shows the normal workouts page.
 *
 * Visual design (colors, spacing, copy, images) mirrors the iOS screens
 * 1:1 — see GymSuitIOS Views/OnboardingView.swift ("Goal & Body Setup").
 */

// iOS palette (hex values as written in the Swift sources).
private val OnbBackground = Color(0xFFFAFBFD)
private val OnbBlue = Color(0xFF0066FF)
private val OnbTitle = Color(0xFF0F172A)
private val OnbStepLabel = Color(0xFF1E293B)
private val OnbSegmentOff = Color(0xFFE2E8F0)
private val OnbInfoBg = Color(0xFFF3F4FB)
private val OnbBodyText = Color(0xFF475569)
private val OnbButtonBlack = Color(0xFF111111)
private val OnbCardBorder = Color(0xFFECEEF2)
private val OnbTickMajor = Color(0xFFCBD5E1)
private val OnbTickMinor = Color(0xFFE2E8F0)
private val OnbTickLabel = Color(0xFF94A3B8)
private val OnbSelectedBg = Color(0xFFF1F4FD)
private val OnbTrackBg = Color(0xFFF1F5F9)

private const val LBS_PER_KG = 2.20462

private enum class WorkoutSetupStep {
    GOAL, HEIGHT, WEIGHT, TARGET_WEIGHT, BIRTH_YEAR, WORKOUT_FREQUENCY, PROJECTION;

    fun previous(): WorkoutSetupStep? = when (this) {
        GOAL -> null
        HEIGHT -> GOAL
        WEIGHT -> HEIGHT
        TARGET_WEIGHT -> WEIGHT
        BIRTH_YEAR -> TARGET_WEIGHT
        WORKOUT_FREQUENCY -> BIRTH_YEAR
        PROJECTION -> WORKOUT_FREQUENCY
    }

    fun next(): WorkoutSetupStep? = when (this) {
        GOAL -> HEIGHT
        HEIGHT -> WEIGHT
        WEIGHT -> TARGET_WEIGHT
        TARGET_WEIGHT -> BIRTH_YEAR
        BIRTH_YEAR -> WORKOUT_FREQUENCY
        WORKOUT_FREQUENCY -> PROJECTION
        PROJECTION -> null
    }

    /** Segment fills + header label, mirroring the iOS step headers. */
    val segmentFractions: List<Float>
        get() = when (this) {
            GOAL -> listOf(1f, 0f, 0f, 0f)
            HEIGHT -> listOf(1f, 20f / 44f, 0f, 0f)
            WEIGHT, TARGET_WEIGHT, BIRTH_YEAR -> listOf(1f, 1f, 0f, 0f)
            WORKOUT_FREQUENCY, PROJECTION -> listOf(1f, 1f, 1f, 0f)
        }

    val stepNumber: String
        get() = when (this) {
            GOAL -> "01"
            HEIGHT, WEIGHT, TARGET_WEIGHT, BIRTH_YEAR -> "02"
            WORKOUT_FREQUENCY, PROJECTION -> "03"
        }

    val stepTitle: String
        get() = when (this) {
            GOAL -> "GOAL & FOCUS"
            HEIGHT, WEIGHT, TARGET_WEIGHT, BIRTH_YEAR -> "ABOUT YOUR BODY"
            WORKOUT_FREQUENCY, PROJECTION -> "FITNESS ASSESSMENT"
        }
}

private enum class OnbHeightUnit { CM, FT }
private enum class OnbWeightUnit { KG, LBS }

@Composable
fun WorkoutOnboardingFlow(
    session: WorkoutSessionState,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { WorkoutOnboardingStore(context) }
    val api = remember { WorkoutOnboardingApi(context) }
    val scope = rememberCoroutineScope()

    var completed by remember { mutableStateOf(store.setupCompleted) }
    var step by remember { mutableStateOf(WorkoutSetupStep.GOAL) }

    // Wizard answers. Every change writes straight through to the store,
    // mirroring the iOS @AppStorage bindings.
    var goalRaw by remember { mutableStateOf(store.selectedGoalRaw) }
    var heightCm by remember { mutableStateOf(store.heightCm) }
    var weightKg by remember { mutableStateOf(store.weightKg) }
    var targetWeightKg by remember { mutableStateOf(store.targetWeightKg) }
    var birthYear by remember { mutableStateOf(store.birthYear) }
    var daysPerWeek by remember { mutableStateOf(store.daysPerWeek) }

    fun goBack() {
        val previous = step.previous()
        if (previous == null) onClose() else step = previous
    }

    // iOS: the cover's back chevron and swipe equivalents dismiss to Home.
    // While a guided session is active, back belongs to the session pages.
    BackHandler(enabled = !session.active) {
        if (completed) onClose() else goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnbBackground)
            .statusBarsPadding()
    ) {
        if (!completed) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    // iOS: move(edge:) + opacity with spring(response 0.35, damping 0.82).
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it } +
                            fadeIn(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))) togetherWith
                            (slideOutHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { -it } +
                                fadeOut(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)))
                    } else {
                        (slideInHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { -it } +
                            fadeIn(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))) togetherWith
                            (slideOutHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it } +
                                fadeOut(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)))
                    }
                },
                label = "WorkoutOnboardingSteps"
            ) { currentStep ->
                when (currentStep) {
                    WorkoutSetupStep.GOAL -> OnboardingGoalStep(
                        goal = WorkoutGoal.fromRawValue(goalRaw),
                        onGoalSelected = { goal ->
                            goalRaw = goal.rawValue
                            store.selectedGoalRaw = goal.rawValue
                        },
                        onBack = { goBack() },
                        onNext = { step = WorkoutSetupStep.HEIGHT }
                    )

                    WorkoutSetupStep.HEIGHT -> OnboardingHeightStep(
                        heightCm = heightCm,
                        onHeightChange = { value ->
                            heightCm = value
                            store.heightCm = value
                        },
                        onBack = { goBack() },
                        onNext = { step = WorkoutSetupStep.WEIGHT }
                    )

                    WorkoutSetupStep.WEIGHT -> OnboardingWeightStep(
                        weightKg = weightKg,
                        heightCm = heightCm,
                        onWeightChange = { value ->
                            weightKg = value
                            store.weightKg = value
                        },
                        onBack = { goBack() },
                        onNext = { step = WorkoutSetupStep.TARGET_WEIGHT }
                    )

                    WorkoutSetupStep.TARGET_WEIGHT -> OnboardingTargetWeightStep(
                        targetWeightKg = targetWeightKg,
                        currentWeightKg = weightKg,
                        goal = WorkoutGoal.fromRawValue(goalRaw),
                        onTargetChange = { value ->
                            targetWeightKg = value
                            store.targetWeightKg = value
                        },
                        onBack = { goBack() },
                        onNext = { step = WorkoutSetupStep.BIRTH_YEAR }
                    )

                    WorkoutSetupStep.BIRTH_YEAR -> OnboardingBirthYearStep(
                        birthYear = birthYear,
                        onBirthYearChange = { value ->
                            birthYear = value
                            store.birthYear = value
                        },
                        onBack = { goBack() },
                        onNext = { step = WorkoutSetupStep.WORKOUT_FREQUENCY }
                    )

                    WorkoutSetupStep.WORKOUT_FREQUENCY -> OnboardingFrequencyStep(
                        daysPerWeek = daysPerWeek,
                        onDaysChange = { value ->
                            daysPerWeek = value
                            store.daysPerWeek = value
                        },
                        onBack = { goBack() },
                        onNext = { step = WorkoutSetupStep.PROJECTION }
                    )

                    WorkoutSetupStep.PROJECTION -> OnboardingProjectionStep(
                        goal = WorkoutGoal.fromRawValue(goalRaw),
                        currentWeightKg = weightKg,
                        targetWeightKg = targetWeightKg,
                        onBack = { goBack() },
                        onFinish = { targetDate ->
                            // iOS order: onFinishWithTargetDate (starts the
                            // backend sync fire-and-forget) THEN onFinish.
                            scope.launch {
                                api.syncOnboarding(
                                    workoutGoal = goalRaw,
                                    heightCm = heightCm,
                                    weightKg = weightKg,
                                    targetWeightKg = targetWeightKg,
                                    birthYear = birthYear,
                                    workoutDaysPerWeek = daysPerWeek,
                                    targetDate = targetDate
                                )
                            }
                            store.setupCompleted = true
                            completed = true
                        }
                    )
                }
            }
        } else {
            // Setup complete: the workouts page inside the same cover, with
            // the iOS back chevron (white circle) that dismisses to Home.
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(enabled = !session.active) { onClose() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Back to Home",
                                tint = Color(0xFF111827),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .navigationBarsPadding()
                ) {
                    WorkoutsScreen(session = session)
                }
            }
        }
    }
}

// region Shared chrome (iOS setupHeaderView / setupBottomBar)

@Composable
private fun OnboardingStepHeader(
    step: WorkoutSetupStep,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = step.stepNumber,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                    color = OnbBlue
                )
                Text(
                    text = " ${step.stepTitle}",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                    color = OnbStepLabel
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                step.segmentFractions.forEachIndexed { index, fraction ->
                    if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(OnbSegmentOff)
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction)
                                    .background(OnbBlue)
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = OnbTitle,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OnboardingNextBar(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OnbBackground.copy(alpha = 0f),
                        OnbBackground.copy(alpha = 0.95f),
                        OnbBackground
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(percent = 50))
                    .clip(RoundedCornerShape(percent = 50))
                    .background(OnbButtonBlack)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NEXT",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color.White
                )
            }
        }
    }
}

// endregion

// region Goal step (iOS SetupGoalView)

@Composable
private fun OnboardingGoalStep(
    goal: WorkoutGoal,
    onGoalSelected: (WorkoutGoal) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.GOAL, onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "What's your main goal?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnbTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 6.dp)
                )
                WorkoutGoal.values().forEach { candidate ->
                    Spacer(modifier = Modifier.height(18.dp))
                    OnboardingGoalCard(
                        goal = candidate,
                        selected = candidate == goal,
                        onClick = { onGoalSelected(candidate) }
                    )
                    if (candidate == goal) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(OnbInfoBg)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = candidate.emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = candidate.bannerTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnbTitle
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = candidate.bannerDescription,
                                fontSize = 13.sp,
                                color = OnbBodyText,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
        OnboardingNextBar(onNext = onNext, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun OnboardingGoalCard(
    goal: WorkoutGoal,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .shadow(
                elevation = if (selected) 12.dp else 4.dp,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) OnbBlue else Color.White)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, OnbCardBorder, RoundedCornerShape(24.dp))
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.title,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else OnbTitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 22.dp)
            )
            Image(
                painter = painterResource(id = goal.imageRes),
                contentDescription = goal.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(122.dp)
                    .padding(end = 10.dp)
            )
        }
    }
}

// endregion

// region Height step (iOS SetupHeightView)

@Composable
private fun OnboardingHeightStep(
    heightCm: Double,
    onHeightChange: (Double) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var unit by remember { mutableStateOf(OnbHeightUnit.FT) }
    val currentHeightCm by rememberUpdatedState(heightCm)

    val totalInches = heightCm / 2.54
    val displayValue: String = when (unit) {
        OnbHeightUnit.CM -> heightCm.roundToInt().toString()
        OnbHeightUnit.FT -> {
            val rounded = totalInches.roundToInt()
            "${rounded / 12} ft ${rounded % 12} in"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.HEIGHT, onBack)
            Text(
                text = "What's your height?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = OnbTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            OnboardingHeightUnitToggle(
                unit = unit,
                onUnitChange = { unit = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Ruler area (iOS heightRuler, 380pt tall).
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .pointerInput(unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaDp = dragAmount.y / density.density
                                when (unit) {
                                    OnbHeightUnit.FT -> {
                                        val current = currentHeightCm / 2.54
                                        val updated = (current + deltaDp / 12f).coerceIn(36.0, 96.0)
                                        if (updated.roundToInt() != current.roundToInt()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onHeightChange(updated * 2.54)
                                    }
                                    OnbHeightUnit.CM -> {
                                        val updated = (currentHeightCm + deltaDp / 5f).coerceIn(90.0, 240.0)
                                        if (updated.roundToInt() != currentHeightCm.roundToInt()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        onHeightChange(updated)
                                    }
                                }
                            },
                            onDragEnd = {
                                when (unit) {
                                    OnbHeightUnit.FT ->
                                        onHeightChange((currentHeightCm / 2.54).roundToInt() * 2.54)
                                    OnbHeightUnit.CM ->
                                        onHeightChange(currentHeightCm.roundToInt().toDouble())
                                }
                            }
                        )
                    }
            ) {
                val midY = 190.dp
                // Big value + blue pointer capsule, left side.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = midY - 48.dp)
                        .padding(start = 80.dp)
                ) {
                    if (unit == OnbHeightUnit.FT) {
                        val rounded = totalInches.roundToInt()
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "${rounded / 12}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = OnbTitle)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ft",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnbTitle,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "${rounded % 12}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = OnbTitle)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "in",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnbTitle,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = displayValue, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = OnbTitle)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "cm",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnbTitle,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(this@BoxWithConstraints.maxWidth - 80.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(OnbBlue)
                    )
                }
                // Tick ruler on the right edge.
                HeightRulerCanvas(
                    heightCm = heightCm,
                    unit = unit,
                    modifier = Modifier.fillMaxSize()
                )
                // Top / bottom fades (iOS rulerFade).
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Brush.verticalGradient(listOf(OnbBackground, OnbBackground.copy(alpha = 0f))))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(Brush.verticalGradient(listOf(OnbBackground.copy(alpha = 0f), OnbBackground)))
                )
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
        OnboardingNextBar(onNext = onNext, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun OnboardingHeightUnitToggle(
    unit: OnbHeightUnit,
    onUnitChange: (OnbHeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .border(1.5.dp, OnbBlue.copy(alpha = 0.85f), RoundedCornerShape(percent = 50))
            .padding(3.dp)
    ) {
        OnbHeightUnit.values().forEach { candidate ->
            val selected = candidate == unit
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (selected) OnbBlue else Color.Transparent)
                    .clickable { onUnitChange(candidate) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (candidate == OnbHeightUnit.CM) "cm" else "ft",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else Color(0xFF334155)
                )
            }
        }
    }
}

@Composable
private fun HeightRulerCanvas(
    heightCm: Double,
    unit: OnbHeightUnit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        val rulerWidthPx = with(density) { 64.dp.toPx() }
        val startX = size.width - rulerWidthPx
        when (unit) {
            OnbHeightUnit.FT -> {
                val pointsPerInch = with(density) { 12.dp.toPx() }
                val totalInches = heightCm / 2.54
                val centerInch = floorToInt(totalInches)
                var inch = centerInch - 20
                while (inch <= centerInch + 20) {
                    if (inch in 0..120) {
                        val y = midY - (inch - totalInches).toFloat() * pointsPerInch
                        if (y >= -30f && y <= size.height + 30f) {
                            val isFoot = inch % 12 == 0
                            val isHalfFoot = inch % 6 == 0
                            val tickLength = with(density) {
                                when {
                                    isFoot -> 24.dp.toPx()
                                    isHalfFoot -> 16.dp.toPx()
                                    else -> 10.dp.toPx()
                                }
                            }
                            drawLine(
                                color = if (isFoot || isHalfFoot) OnbTickMajor else OnbTickMinor,
                                start = Offset(size.width - tickLength, y),
                                end = Offset(size.width, y),
                                strokeWidth = with(density) { 1.2.dp.toPx() }
                            )
                            if (isFoot) {
                                val label = "${inch / 12}"
                                val style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnbTickLabel
                                )
                                val layout = textMeasurer.measure(label, style)
                                drawText(
                                    textLayoutResult = layout,
                                    topLeft = Offset(
                                        x = startX - with(density) { 14.dp.toPx() } - layout.size.width / 2f,
                                        y = y - layout.size.height / 2f
                                    )
                                )
                            }
                        }
                    }
                    inch++
                }
            }
            OnbHeightUnit.CM -> {
                val pointsPerCm = with(density) { 5.dp.toPx() }
                // iOS anchors cm ticks to the rounded value at the center.
                val centerCm = heightCm.roundToInt()
                var cm = centerCm - 42
                while (cm <= centerCm + 42) {
                    if (cm in 0..300) {
                        val y = midY - (cm - centerCm).toFloat() * pointsPerCm
                        if (y >= -30f && y <= size.height + 30f) {
                            val isTen = cm % 10 == 0
                            val isFive = cm % 5 == 0
                            val tickLength = with(density) {
                                when {
                                    isTen -> 24.dp.toPx()
                                    isFive -> 16.dp.toPx()
                                    else -> 10.dp.toPx()
                                }
                            }
                            drawLine(
                                color = if (isTen || isFive) OnbTickMajor else OnbTickMinor,
                                start = Offset(size.width - tickLength, y),
                                end = Offset(size.width, y),
                                strokeWidth = with(density) { 1.2.dp.toPx() }
                            )
                            if (isTen) {
                                val label = "$cm"
                                val style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnbTickLabel
                                )
                                val layout = textMeasurer.measure(label, style)
                                drawText(
                                    textLayoutResult = layout,
                                    topLeft = Offset(
                                        x = startX - with(density) { 14.dp.toPx() } - layout.size.width / 2f,
                                        y = y - layout.size.height / 2f
                                    )
                                )
                            }
                        }
                    }
                    cm++
                }
            }
        }
    }
}

private fun floorToInt(value: Double): Int = kotlin.math.floor(value).toInt()

// endregion

// region Weight step (iOS SetupWeightView)

@Composable
private fun OnboardingWeightStep(
    weightKg: Double,
    heightCm: Double,
    onWeightChange: (Double) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val density = LocalDensity.current
    var unit by remember { mutableStateOf(OnbWeightUnit.LBS) }
    // iOS keeps a displayed value in the active unit, initialised on appear.
    var displayed by remember {
        mutableStateOf(
            if (weightKg > 0) weightKg * LBS_PER_KG else 164.3
        )
    }

    fun setDisplayed(value: Double) {
        displayed = value
        onWeightChange(if (unit == OnbWeightUnit.LBS) value / LBS_PER_KG else value)
    }

    val bmi = if (heightCm > 0) weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) else 0.0
    val bmiStatus: Triple<String, Color, String> = when {
        bmi < 18.5 -> Triple(
            "Underweight", Color(0xFF3B82F6),
            "A balanced diet and strength building will help you reach a healthy weight."
        )
        bmi < 25 -> Triple(
            "Normal", Color(0xFF16A34A),
            "You are in a healthy, prime range! Keep maintaining your great habits."
        )
        bmi < 30 -> Triple(
            "Overweight", Color(0xFFEA580C),
            "You only need a bit more sweat exercises to see a fitter you!"
        )
        else -> Triple(
            "Obese", Color(0xFFDC2626),
            "Targeted cardiovascular & nutrition plans will guide your transformation!"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.WEIGHT, onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "What's your current\nweight?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnbTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(30.dp))
                OnboardingWeightUnitToggle(
                    unit = unit,
                    onUnitChange = { newUnit ->
                        if (newUnit != unit) {
                            displayed = if (newUnit == OnbWeightUnit.LBS) displayed * LBS_PER_KG else displayed / LBS_PER_KG
                            unit = newUnit
                            onWeightChange(if (newUnit == OnbWeightUnit.LBS) displayed / LBS_PER_KG else displayed)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", displayed),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (unit == OnbWeightUnit.LBS) "lbs" else "kg",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbBodyText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(26.dp))
                OnboardingWeightRuler(
                    value = displayed,
                    minValue = 20.0,
                    maxValue = 500.0,
                    shadeFromValue = null,
                    majorLabelsEvery10 = false,
                    onValueChange = { setDisplayed(it) },
                    onDragEnd = { setDisplayed((displayed * 10).roundToInt() / 10.0) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
                // BMI card.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(OnbInfoBg)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Your BMI", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnbTitle)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${bmiStatus.first})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = bmiStatus.second
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = OnbTickMajor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = String.format(Locale.US, "%.1f", bmi),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = bmiStatus.second
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = bmiStatus.third,
                            fontSize = 13.sp,
                            color = OnbBodyText,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
        OnboardingNextBar(onNext = onNext, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun OnboardingWeightUnitToggle(
    unit: OnbWeightUnit,
    onUnitChange: (OnbWeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .border(1.5.dp, OnbBlue.copy(alpha = 0.85f), RoundedCornerShape(percent = 50))
            .padding(3.dp)
    ) {
        OnbWeightUnit.values().forEach { candidate ->
            val selected = candidate == unit
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (selected) OnbBlue else Color.Transparent)
                    .clickable { onUnitChange(candidate) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (candidate == OnbWeightUnit.KG) "kg" else "lbs",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else Color(0xFF334155)
                )
            }
        }
    }
}

/**
 * Horizontal drag ruler shared by the weight and target-weight steps
 * (iOS WeightRulerView). [value] is the centered value in the active unit;
 * 8pt per 0.1 unit. When [shadeFromValue] is set (target step), the span
 * between it and the centered value is shaded and major labels print only
 * every 10 units, as in iOS.
 */
@Composable
private fun OnboardingWeightRuler(
    value: Double,
    minValue: Double,
    maxValue: Double,
    shadeFromValue: Double?,
    majorLabelsEvery10: Boolean,
    onValueChange: (Double) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val currentValue by rememberUpdatedState(value)
    Box(
        modifier = modifier
            .height(90.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // 8pt per 0.1 unit == 80pt per unit (iOS: value =
                        // startValue - translation / 80, applied here per
                        // drag delta). Dragging left raises the value.
                        val delta = (dragAmount.x / density.density) / 80.0
                        onValueChange((currentValue - delta).coerceIn(minValue, maxValue))
                    },
                    onDragEnd = { onDragEnd() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midX = size.width / 2f
            val startY = with(density) { 16.dp.toPx() }
            val pointsPerTenth = with(density) { 8.dp.toPx() }

            if (shadeFromValue != null) {
                val shadeX = midX + (shadeFromValue - value).toFloat() * pointsPerTenth * 10f
                val left = minOf(midX, shadeX)
                drawRect(
                    color = Color(0xFFE0E7FF).copy(alpha = 0.55f),
                    topLeft = Offset(left, with(density) { 14.dp.toPx() }),
                    size = androidx.compose.ui.geometry.Size(
                        width = abs(shadeX - midX),
                        height = with(density) { 46.dp.toPx() }
                    )
                )
            }

            val centerTenth = (value * 10).roundToInt()
            val visibleTenths = (size.width / pointsPerTenth).toInt() + 4
            var tenth = centerTenth - visibleTenths
            while (tenth <= centerTenth + visibleTenths) {
                val tickValue = tenth / 10.0
                if (tickValue >= minValue - 1 && tickValue <= maxValue + 1) {
                    val x = midX + (tickValue - value).toFloat() * pointsPerTenth * 10f
                    if (x >= -20f && x <= size.width + 20f) {
                        val isWhole = tenth % 10 == 0
                        val isHalf = tenth % 5 == 0
                        val tickLength = with(density) {
                            when {
                                isWhole -> 28.dp.toPx()
                                isHalf -> 20.dp.toPx()
                                else -> 14.dp.toPx()
                            }
                        }
                        drawLine(
                            color = if (isWhole || isHalf) OnbTickMajor else OnbTickMinor,
                            start = Offset(x, startY),
                            end = Offset(x, startY + tickLength),
                            strokeWidth = with(density) { 1.2.dp.toPx() }
                        )
                        val showLabel = if (majorLabelsEvery10) (tenth % 100 == 0) else isWhole
                        if (showLabel) {
                            val label = "${(tickValue).roundToInt()}"
                            val style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnbTickLabel
                            )
                            val layout = textMeasurer.measure(label, style)
                            drawText(
                                textLayoutResult = layout,
                                topLeft = Offset(
                                    x = x - layout.size.width / 2f,
                                    y = startY + with(density) { 28.dp.toPx() } + with(density) { 16.dp.toPx() } - layout.size.height / 2f
                                )
                            )
                        }
                    }
                }
                tenth++
            }
        }
        // Center pointer capsule (iOS: fixed blue capsule at the center).
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(2.5.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(OnbBlue)
        )
        // Side fades.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(50.dp)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(OnbBackground, OnbBackground.copy(alpha = 0f))))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(50.dp)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(OnbBackground.copy(alpha = 0f), OnbBackground)))
        )
    }
}

// endregion

// region Target weight step (iOS SetupTargetWeightView)

@Composable
private fun OnboardingTargetWeightStep(
    targetWeightKg: Double,
    currentWeightKg: Double,
    goal: WorkoutGoal,
    onTargetChange: (Double) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var unit by remember { mutableStateOf(OnbWeightUnit.KG) }
    // iOS initialises the target on appear: the stored target if any, else a
    // goal-based default (current + 6 kg for muscle gain, 71 kg to lose),
    // and writes that default back immediately.
    val initial = remember {
        when {
            targetWeightKg > 0 -> targetWeightKg
            goal == WorkoutGoal.BUILD_MUSCLE -> if (currentWeightKg > 0) currentWeightKg + 6.0 else 78.0
            else -> 71.0
        }
    }
    var displayed by remember { mutableStateOf(initial) }
    LaunchedEffect(Unit) {
        if (targetWeightKg <= 0) onTargetChange(initial)
    }

    fun kgOf(value: Double): Double = if (unit == OnbWeightUnit.LBS) value / LBS_PER_KG else value
    fun setDisplayed(value: Double) {
        displayed = value
        onTargetChange(kgOf(value))
    }
    fun currentInUnit(): Double = if (unit == OnbWeightUnit.LBS) currentWeightKg * LBS_PER_KG else currentWeightKg

    val isLosing = targetWeightKg < currentWeightKg
    val pct = if (currentWeightKg > 0) abs(targetWeightKg - currentWeightKg) / currentWeightKg * 100.0 else 0.0
    val headline: String = when {
        goal == WorkoutGoal.BUILD_MUSCLE -> "Muscle & Strength goal!"
        pct > 18 -> "Challenging goal!"
        else -> "Achievable goal!"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.TARGET_WEIGHT, onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // "What's your **target** weight?" — "target" in blue (iOS
                // builds this as a two-run AttributedString).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "What's your target",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle,
                        textAlign = TextAlign.Center
                    )
                    Row {
                        Text(
                            text = "target ",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnbBlue
                        )
                        Text(
                            text = "weight?",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnbTitle
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                OnboardingWeightUnitToggle(
                    unit = unit,
                    onUnitChange = { newUnit ->
                        if (newUnit != unit) {
                            displayed = if (newUnit == OnbWeightUnit.LBS) displayed * LBS_PER_KG else displayed / LBS_PER_KG
                            unit = newUnit
                            onTargetChange(kgOf(displayed))
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (displayed == displayed.roundToInt().toDouble()) {
                            displayed.roundToInt().toString()
                        } else {
                            String.format(Locale.US, "%.1f", displayed)
                        },
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (unit == OnbWeightUnit.LBS) "lbs" else "kg",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbBodyText
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = if (isLosing) Icons.Filled.FastRewind else Icons.Filled.FastForward,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f %s", currentInUnit(), if (unit == OnbWeightUnit.LBS) "lbs" else "kg"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(26.dp))
                OnboardingWeightRuler(
                    value = displayed,
                    minValue = 20.0,
                    maxValue = 300.0,
                    shadeFromValue = currentInUnit(),
                    majorLabelsEvery10 = true,
                    onValueChange = { setDisplayed(it) },
                    onDragEnd = { setDisplayed((displayed * 10).roundToInt() / 10.0) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(40.dp))
                // Benefit card.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(OnbInfoBg)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = headline,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE11D48)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLosing) "You will lose " else "You will gain ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnbTitle
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%%", pct),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                        Text(
                            text = " of body weight",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnbTitle
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "You can see more significant health benefits:",
                        fontSize = 13.sp,
                        color = OnbBodyText,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "— Improve cardiovascular health",
                        fontSize = 13.sp,
                        color = OnbBodyText,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "— Reduce the risk of cancer",
                        fontSize = 13.sp,
                        color = OnbBodyText,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
        OnboardingNextBar(onNext = onNext, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// endregion

// region Birth year step (iOS SetupBirthYearView)

@Composable
private fun OnboardingBirthYearStep(
    birthYear: Int,
    onBirthYearChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val years = remember { (1940..2012).toList() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // iOS scrolls the stored year into the center when the step appears.
    LaunchedEffect(Unit) {
        val index = years.indexOf(birthYear)
        if (index >= 0) {
            val centerOffsetPx = with(density) { ((280.dp - 52.dp) / 2).roundToPx() }
            listState.scrollToItem(index, scrollOffset = -centerOffsetPx)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.BIRTH_YEAR, onBack)
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "What's your birth year?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnbTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(OnbInfoBg)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📋", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "It will help us to adjust the workout that best suits your age group.",
                        fontSize = 14.sp,
                        color = OnbBodyText,
                        lineHeight = 19.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                item { Spacer(modifier = Modifier.height(100.dp)) }
                items(years) { year ->
                    val distance = abs(year - birthYear)
                    val selected = year == birthYear
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 40.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) OnbSelectedBg else Color.Transparent)
                            .clickable { onBirthYearChange(year) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$year",
                            fontSize = when {
                                selected -> 34.sp
                                distance == 1 -> 26.sp
                                else -> 22.sp
                            },
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                selected -> OnbBlue
                                distance == 1 -> Color(0xFF64748B)
                                else -> OnbTickMajor
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
        OnboardingNextBar(onNext = onNext, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// endregion

// region Frequency step (iOS SetupFrequencyView)

@Composable
private fun OnboardingFrequencyStep(
    daysPerWeek: Int,
    onDaysChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var selectedDays by remember {
        mutableStateOf(if (daysPerWeek in 1..5) daysPerWeek else 2)
    }
    LaunchedEffect(Unit) {
        if (daysPerWeek !in 1..5) onDaysChange(2)
    }

    val description: String = when (selectedDays) {
        1 -> "A gentle start to build your routine and keep moving."
        2 -> "I have a bit of free time, but not too much."
        3 -> "Balanced and sustainable. A great sweet spot."
        4 -> "Dedicated to seeing rapid, powerful progress."
        5 -> "Full commitment to peak conditioning and strength."
        else -> "Consistency is the key to conquering your fitness goal!"
    }

    fun select(days: Int) {
        val clamped = days.coerceIn(1, 5)
        selectedDays = clamped
        onDaysChange(clamped)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.WORKOUT_FREQUENCY, onBack)
            Text(
                text = "How often would you like\nto work out?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OnbTitle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp)
            )
            Spacer(modifier = Modifier.height(36.dp))

            // Calendar graphic (iOS calendarGraphic, 160x160).
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFEEF2F6), RoundedCornerShape(22.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                                )
                            )
                    )
                    Text(
                        text = "$selectedDays",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 30.dp)
                    )
                    // Folded corner (bottom-right triangle).
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(18.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path = path, color = OnbSegmentOff)
                    }
                }
                // Binder rings across the top.
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-8).dp)
                ) {
                    repeat(6) { index ->
                        if (index > 0) Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF93C5FD),
                                            Color(0xFF3B82F6),
                                            Color(0xFF1D4ED8)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Text(
                text = description,
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 40.dp)
                    .padding(top = 24.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            // Stepper (iOS frequencyStepper).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "$selectedDays times/week",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnbTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(OnbTrackBg)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    select((fraction * 5f).roundToInt().coerceIn(1, 5))
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, _ ->
                                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                    select((fraction * 5f).roundToInt().coerceIn(1, 5))
                                }
                            }
                    )
                    Row(modifier = Modifier.fillMaxSize()) {
                        (1..5).forEach { stop ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stop == selectedDays) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .shadow(elevation = 3.dp, shape = CircleShape)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(4.dp, OnbBlue, CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(OnbTickMajor)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Less",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle,
                        modifier = Modifier.clickable { select(selectedDays - 1) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "More",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle,
                        modifier = Modifier.clickable { select(selectedDays + 1) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
        OnboardingNextBar(
            onNext = {
                onDaysChange(selectedDays)
                onNext()
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// endregion

// region Projection step (iOS SetupProjectionView)

@Composable
private fun OnboardingProjectionStep(
    goal: WorkoutGoal,
    currentWeightKg: Double,
    targetWeightKg: Double,
    onBack: () -> Unit,
    onFinish: (Date) -> Unit
) {
    val isGaining = goal == WorkoutGoal.BUILD_MUSCLE || targetWeightKg > currentWeightKg
    val diffKg = abs(currentWeightKg - targetWeightKg)
    val acceleratedDays = max(ceil(max(diffKg, 1.0) * 7.0).toInt(), 14)
    val standardDays = ceil(acceleratedDays * 1.5).toInt()
    val daysEarly = max(standardDays - acceleratedDays, 7)
    val targetDate = remember(acceleratedDays) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, acceleratedDays) }.time
    }
    val standardDate = remember(standardDays) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, standardDays) }.time
    }
    val monthDay = remember { SimpleDateFormat("MMM d", Locale.US) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingStepHeader(WorkoutSetupStep.PROJECTION, onBack)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "A more precise outlook is here!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnbBodyText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You'll reach",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnbBodyText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f kg on", targetWeightKg),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbTitle
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthDay.format(targetDate),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnbBlue
                    )
                }
                Spacer(modifier = Modifier.height(34.dp))
                OnboardingProjectionGraph(
                    isGaining = isGaining,
                    acceleratedDays = acceleratedDays,
                    targetDateLabel = monthDay.format(targetDate),
                    standardDateLabel = monthDay.format(standardDate),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "$daysEarly days early!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnbTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isGaining) {
                        "You have great potential to build lean muscle\nand hit your target ahead of schedule!"
                    } else {
                        "You have great potential to achieve your\ngoals ahead of schedule!"
                    },
                    fontSize = 15.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
        OnboardingNextBar(
            onNext = { onFinish(targetDate) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun OnboardingProjectionGraph(
    isGaining: Boolean,
    acceleratedDays: Int,
    targetDateLabel: String,
    standardDateLabel: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Canvas(modifier = modifier.height(260.dp)) {
        val width = size.width
        val height = size.height
        val startX = with(density) { 36.dp.toPx() }
        val targetX = width * 0.58f
        val baselineX = width - with(density) { 44.dp.toPx() }
        val startY = if (isGaining) height - with(density) { 68.dp.toPx() } else with(density) { 46.dp.toPx() }
        val targetY = if (isGaining) with(density) { 46.dp.toPx() } else height - with(density) { 68.dp.toPx() }
        val baselineY = targetY
        val axisBottom = height - with(density) { 30.dp.toPx() }

        fun acceleratedPoint(t: Float): Offset {
            // Cubic bezier matching the iOS path control points.
            val p0 = Offset(startX, startY)
            val p1 = Offset(startX + (targetX - startX) * 0.45f, startY + if (isGaining) -8f else 8f)
            val p2 = Offset(startX + (targetX - startX) * 0.55f, targetY)
            val p3 = Offset(targetX, targetY)
            val u = 1f - t
            val x = u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x
            val y = u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y
            return Offset(x, y)
        }

        // Gradient fill under the accelerated curve.
        val fillPath = Path().apply {
            moveTo(0f, if (isGaining) height else startY)
            lineTo(startX, startY)
            var i = 1
            while (i <= 24) {
                val point = acceleratedPoint(i / 24f)
                lineTo(point.x, point.y)
                i++
            }
            lineTo(width, targetY)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.linearGradient(
                colors = if (isGaining) {
                    listOf(
                        Color(0xFF93C5FD).copy(alpha = 0.35f),
                        Color(0xFFC7D2FE).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color(0xFFFDA4AF).copy(alpha = 0.35f),
                        Color(0xFFE0E7FF).copy(alpha = 0.2f),
                        Color.Transparent
                    )
                },
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )

        val dashEffect = PathEffect.dashPathEffect(
            floatArrayOf(with(density) { 4.dp.toPx() }, with(density) { 4.dp.toPx() })
        )
        val axisColor = OnbTickMajor
        // Dashed verticals.
        listOf(startX to startY, targetX to targetY, baselineX to baselineY).forEach { (x, y) ->
            drawLine(
                color = axisColor,
                start = Offset(x, y),
                end = Offset(x, axisBottom),
                strokeWidth = with(density) { 1.2.dp.toPx() },
                pathEffect = dashEffect
            )
        }
        // Dashed baseline S-curve.
        val baselinePath = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                startX + (baselineX - startX) * 0.5f,
                startY + if (isGaining) -14f else 14f,
                startX + (baselineX - startX) * 0.6f,
                baselineY,
                baselineX,
                baselineY
            )
        }
        drawPath(
            path = baselinePath,
            color = axisColor,
            style = Stroke(
                width = with(density) { 2.2.dp.toPx() },
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(with(density) { 6.dp.toPx() }, with(density) { 5.dp.toPx() })
                )
            )
        )
        // Accelerated curve (solid, gradient stroke).
        val acceleratedPath = Path().apply {
            moveTo(0f, startY)
            lineTo(startX, startY)
            cubicTo(
                startX + (targetX - startX) * 0.45f,
                startY + if (isGaining) -8f else 8f,
                startX + (targetX - startX) * 0.55f,
                targetY,
                targetX,
                targetY
            )
            lineTo(width, targetY)
        }
        drawPath(
            path = acceleratedPath,
            brush = Brush.horizontalGradient(
                colors = if (isGaining) {
                    listOf(Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFF0066FF), Color(0xFF10B981))
                } else {
                    listOf(Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF0066FF), Color(0xFF38BDF8))
                }
            ),
            style = Stroke(
                width = with(density) { 4.dp.toPx() },
                cap = StrokeCap.Round
            )
        )
        // "Faster" double chevron between the target and baseline points.
        val chevronColor = Color(0xFF38BDF8)
        val chevronMidX = (targetX + baselineX) / 2f
        listOf(-4f, 4f).forEach { dx ->
            val cx = chevronMidX + with(density) { dx.dp.toPx() }
            val chevron = Path().apply {
                moveTo(cx + with(density) { 4.dp.toPx() }, targetY - with(density) { 6.dp.toPx() })
                lineTo(cx - with(density) { 3.dp.toPx() }, targetY)
                lineTo(cx + with(density) { 4.dp.toPx() }, targetY + with(density) { 6.dp.toPx() })
            }
            drawPath(
                path = chevron,
                color = chevronColor,
                style = Stroke(width = with(density) { 2.5.dp.toPx() }, cap = StrokeCap.Round)
            )
        }
        // Dots.
        drawCircle(
            color = if (isGaining) Color(0xFF3B82F6) else Color(0xFFF43F5E),
            radius = with(density) { 6.dp.toPx() },
            center = Offset(startX, startY)
        )
        drawCircle(
            color = Color.White,
            radius = with(density) { 6.dp.toPx() },
            center = Offset(startX, startY),
            style = Stroke(width = with(density) { 2.5.dp.toPx() })
        )
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = with(density) { 5.5.dp.toPx() },
            center = Offset(baselineX, baselineY)
        )
        drawCircle(
            color = Color.White,
            radius = with(density) { 5.5.dp.toPx() },
            center = Offset(baselineX, baselineY),
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )
        drawCircle(
            color = OnbBlue.copy(alpha = 0.18f),
            radius = with(density) { 13.dp.toPx() },
            center = Offset(targetX, targetY)
        )
        drawCircle(
            color = OnbBlue,
            radius = with(density) { 7.dp.toPx() },
            center = Offset(targetX, targetY)
        )
        drawCircle(
            color = Color.White,
            radius = with(density) { 7.dp.toPx() },
            center = Offset(targetX, targetY),
            style = Stroke(width = with(density) { 2.5.dp.toPx() })
        )

        // Tooltip bubble above the target dot: "N Days".
        val tooltipText = "$acceleratedDays Days"
        val tooltipStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        val tooltipLayout = textMeasurer.measure(tooltipText, tooltipStyle)
        val bubblePadH = with(density) { 10.dp.toPx() }
        val bubblePadV = with(density) { 6.dp.toPx() }
        val bubbleWidth = tooltipLayout.size.width + bubblePadH * 2
        val bubbleHeight = tooltipLayout.size.height + bubblePadV * 2
        val bubbleLeft = targetX - bubbleWidth / 2f
        val bubbleTop = targetY - with(density) { 44.dp.toPx() } - bubbleHeight
        drawRoundRect(
            color = OnbBlue,
            topLeft = Offset(bubbleLeft, bubbleTop),
            size = androidx.compose.ui.geometry.Size(bubbleWidth, bubbleHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                with(density) { 10.dp.toPx() },
                with(density) { 10.dp.toPx() }
            )
        )
        val pointer = Path().apply {
            moveTo(targetX - with(density) { 6.dp.toPx() }, bubbleTop + bubbleHeight)
            lineTo(targetX + with(density) { 6.dp.toPx() }, bubbleTop + bubbleHeight)
            lineTo(targetX, bubbleTop + bubbleHeight + with(density) { 6.dp.toPx() })
            close()
        }
        drawPath(path = pointer, color = OnbBlue)
        drawText(
            textLayoutResult = tooltipLayout,
            topLeft = Offset(bubbleLeft + bubblePadH, bubbleTop + bubblePadV)
        )

        // X-axis labels.
        fun xLabel(text: String, centerX: Float, bold: Boolean, color: Color, sizeSp: Int) {
            val style = TextStyle(
                fontSize = sizeSp.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                color = color
            )
            val layout = textMeasurer.measure(text, style)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    x = centerX - layout.size.width / 2f,
                    y = height - with(density) { 14.dp.toPx() } - layout.size.height / 2f
                )
            )
        }
        xLabel("Today", startX, false, Color(0xFF64748B), 13)
        xLabel(targetDateLabel, targetX, true, OnbTitle, 15)
        xLabel(standardDateLabel, baselineX, false, Color(0xFF64748B), 13)
    }
}

// endregion

package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.DetailedSleepData
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.SleepStageSegment
import ca.zeezaglobal.gymsuitapp.data.SleepStageType
import ca.zeezaglobal.gymsuitapp.data.local.SleepAiSummaryEngine
import ca.zeezaglobal.gymsuitapp.data.local.SleepInsightResult
import java.time.LocalDate

/**
 * Dedicated Sleep Details Screen matching the reference design:
 * - Top app bar with back button, battery pill, "Sleep" title, and user profile avatar.
 * - AI-powered sleep summary card with badge, bold headline, and personalized insights.
 * - Multi-tier sleep stage hypnogram chart (Awake, REM, Light, Deep) with smooth curved
 *   transitions and timeline markers.
 * - Stage breakdown metrics list.
 */
@Composable
fun SleepDetailScreen(
    date: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var detailedSleep by remember {
        mutableStateOf<DetailedSleepData?>(null)
    }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(date) {
        isLoading = true
        detailedSleep = healthConnectManager.readDetailedSleepForDate(date)
        isLoading = false
    }

    val sleepData = detailedSleep
    val aiInsight: SleepInsightResult = remember(sleepData) {
        if (sleepData != null) {
            SleepAiSummaryEngine.generateInsight(sleepData)
        } else {
            SleepInsightResult(
                headline = "Analyzing sleep...",
                description = "Retrieving sleep cycles and stage breakdown from Health Connect.",
                badgeText = "Last night's sleep"
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFFF3F2F8), // Soft lavender/neutral tint matching reference
        topBar = {
            SleepDetailTopBar(
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Main Sleep Container Card matching reference
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Badge & Options Menu Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF3E8FF) // Subtle purple pill
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bedtime,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = aiInsight.badgeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6B21A8)
                                )
                            }
                        }

                        IconButton(
                            onClick = { /* menu action */ },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // AI Headline (Bold, human-like coaching e.g. "A fragmented and restless night")
                    Text(
                        text = aiInsight.headline,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // AI Description text
                    Text(
                        text = aiInsight.description,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Multi-Tier Hypnogram Sleep Stage Graph
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            SleepHypnogramChart(
                                sleepData = sleepData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sleep Stage Summary Breakdown Cards
            if (sleepData != null && sleepData.hasData) {
                SleepBreakdownGrid(sleepData = sleepData)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Top App Bar with back navigation, battery/time status pill, "Sleep" title, and user profile avatar.
 */
@Composable
private fun SleepDetailTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back button with battery indicator matching reference
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BatteryFull,
                        contentDescription = "Battery",
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "96%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }

        // Center: "Sleep" Title
        Text(
            text = "Sleep",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        // Right: User Profile Avatar
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.avatar_gaze_1),
                contentDescription = "User Avatar",
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

/**
 * Multi-tier Hypnogram chart that accurately draws the 4 sleep stages:
 * - Total awake (Soft berry pink / red `#DB2777` / `#F43F5E`)
 * - REM (Sky cyan `#38BDF8`)
 * - Light (Royal blue `#3B82F6`)
 * - Deep (Dark violet / indigo `#4C1D95` / `#581C87`)
 *
 * Connected by smooth curved transition channels matching the user's reference image.
 */
@Composable
private fun SleepHypnogramChart(
    sleepData: DetailedSleepData?,
    modifier: Modifier = Modifier
) {
    if (sleepData == null || !sleepData.hasData) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No detailed sleep stage records found",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
        return
    }

    val awakeFormatted = remember(sleepData.awakeMinutes) {
        val h = sleepData.awakeMinutes / 60
        val m = sleepData.awakeMinutes % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val remFormatted = remember(sleepData.remMinutes) {
        val h = sleepData.remMinutes / 60
        val m = sleepData.remMinutes % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val lightFormatted = remember(sleepData.lightMinutes) {
        val h = sleepData.lightMinutes / 60
        val m = sleepData.lightMinutes % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }
    val deepFormatted = remember(sleepData.deepMinutes) {
        val h = sleepData.deepMinutes / 60
        val m = sleepData.deepMinutes % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    Column(modifier = modifier) {
        // Stage Labels and Graphic Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tier 0: Awake Label
                Text(
                    text = "Total awake · $awakeFormatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )

                // Tier 1: REM Label
                Text(
                    text = "REM · $remFormatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )

                // Tier 2: Light Label
                Text(
                    text = "Light · $lightFormatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )

                // Tier 3: Deep Label
                Text(
                    text = "Deep · $deepFormatted",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            }

            // Custom Canvas rendering the background track pills, active stage bars, and vertical transition lines
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 4.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // 4 Lane Y coordinates
                val laneCount = 4
                val laneSpacing = canvasHeight / 3.8f
                val barHeight = 18.dp.toPx()
                val cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)

                val laneY = floatArrayOf(
                    0f,                  // Awake
                    laneSpacing * 0.95f, // REM
                    laneSpacing * 1.9f,  // Light
                    laneSpacing * 2.85f  // Deep
                )

                val trackBgColor = Color(0xFFE2E8F0).copy(alpha = 0.65f)

                // 1. Draw light grey background pill tracks for each of the 4 stages
                for (i in 0 until laneCount) {
                    val y = laneY[i]
                    drawRoundRect(
                        color = trackBgColor,
                        topLeft = Offset(0f, y),
                        size = Size(canvasWidth, barHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Stage distinct colors matching the reference picture
                val colorAwake = Color(0xFFDB2777) // Soft Berry/Red Pink
                val colorRem = Color(0xFF38BDF8)   // Sky Cyan
                val colorLight = Color(0xFF3B82F6) // Royal Blue
                val colorDeep = Color(0xFF4C1D95)  // Deep Indigo/Purple

                fun getColorForStage(stage: SleepStageType): Color = when (stage) {
                    SleepStageType.AWAKE -> colorAwake
                    SleepStageType.REM -> colorRem
                    SleepStageType.LIGHT -> colorLight
                    SleepStageType.DEEP -> colorDeep
                }

                fun getLaneIndex(stage: SleepStageType): Int = when (stage) {
                    SleepStageType.AWAKE -> 0
                    SleepStageType.REM -> 1
                    SleepStageType.LIGHT -> 2
                    SleepStageType.DEEP -> 3
                }

                val stages = sleepData.stages

                // 2. Draw vertical curved transition bridges between adjacent stage transitions
                for (i in 0 until stages.size - 1) {
                    val curr = stages[i]
                    val next = stages[i + 1]

                    val currLane = getLaneIndex(curr.stage)
                    val nextLane = getLaneIndex(next.stage)

                    if (currLane != nextLane) {
                        val transX = curr.endFraction * canvasWidth
                        val currY = laneY[currLane] + barHeight / 2
                        val nextY = laneY[nextLane] + barHeight / 2

                        val bridgeColor = getColorForStage(next.stage).copy(alpha = 0.7f)
                        val strokeWidth = 2.dp.toPx()

                        val path = Path().apply {
                            moveTo(transX, currY)
                            // Cubic curve for smooth transition
                            cubicTo(
                                transX, (currY + nextY) / 2,
                                transX, (currY + nextY) / 2,
                                transX, nextY
                            )
                        }

                        drawPath(
                            path = path,
                            color = bridgeColor,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }

                // 3. Draw active colored segments on their respective stage tracks
                for (segment in stages) {
                    val lane = getLaneIndex(segment.stage)
                    val y = laneY[lane]
                    val startX = segment.startFraction * canvasWidth
                    val segmentWidth = ((segment.endFraction - segment.startFraction) * canvasWidth).coerceAtLeast(3f)

                    val segmentColor = getColorForStage(segment.stage)

                    // Draw rounded pill or segment
                    drawRoundRect(
                        color = segmentColor,
                        topLeft = Offset(startX, y),
                        size = Size(segmentWidth, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2, barHeight / 2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // X-Axis Time Markers (Start Time, Mid Time, End Time)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sleepData.startTimeFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Text(
                text = sleepData.midTimeFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Text(
                text = sleepData.endTimeFormatted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}

/**
 * Breakdown metrics grid displaying durations and percentages for each sleep stage.
 */
@Composable
private fun SleepBreakdownGrid(sleepData: DetailedSleepData) {
    val totalMins = sleepData.totalSleepMinutes.coerceAtLeast(1)

    fun formatDurationAndPct(mins: Long): Pair<String, String> {
        val h = mins / 60
        val m = mins % 60
        val dur = if (h > 0) "${h}h ${m}m" else "${m}m"
        val pct = "${((mins.toFloat() / totalMins.toFloat()) * 100).toInt()}%"
        return dur to pct
    }

    val (awakeDur, awakePct) = formatDurationAndPct(sleepData.awakeMinutes)
    val (remDur, remPct) = formatDurationAndPct(sleepData.remMinutes)
    val (lightDur, lightPct) = formatDurationAndPct(sleepData.lightMinutes)
    val (deepDur, deepPct) = formatDurationAndPct(sleepData.deepMinutes)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SleepStageMetricCard(
            title = "Awake",
            duration = awakeDur,
            percentage = awakePct,
            accentColor = Color(0xFFDB2777),
            modifier = Modifier.weight(1f)
        )
        SleepStageMetricCard(
            title = "REM",
            duration = remDur,
            percentage = remPct,
            accentColor = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f)
        )
        SleepStageMetricCard(
            title = "Light",
            duration = lightDur,
            percentage = lightPct,
            accentColor = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
        SleepStageMetricCard(
            title = "Deep",
            duration = deepDur,
            percentage = deepPct,
            accentColor = Color(0xFF4C1D95),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SleepStageMetricCard(
    title: String,
    duration: String,
    percentage: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = duration,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = percentage,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
        }
    }
}

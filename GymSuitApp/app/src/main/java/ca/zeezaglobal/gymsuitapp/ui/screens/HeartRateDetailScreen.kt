package ca.zeezaglobal.gymsuitapp.ui.screens

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
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.DetailedHrvData
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.HrvBucket
import java.time.LocalDate

/**
 * Heart Rate Variability & Heart Rate Details Screen matching the reference design:
 * - Top app bar with back navigation, battery pill, "Heart Rate" title, and user profile avatar.
 * - Main Card: "Heart Rate Variability", bold readout e.g. "82 ms",
 *   vertical bead/scatter distribution graph with soft red accents (instead of orange),
 *   and X-axis time marks (12am, 4am, 8am, 12pm, 4pm, 8pm).
 * - Two bottom cards matching the reference:
 *   1. "Stress Level" -> e.g. "Low"
 *   2. "Ave. Variability" -> e.g. "92 ms"
 * - Heart rate summary metrics (Latest, Min, Max BPM).
 */
@Composable
fun HeartRateDetailScreen(
    date: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var hrvData by remember { mutableStateOf<DetailedHrvData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(date) {
        isLoading = true
        hrvData = healthConnectManager.readDetailedHrvForDate(date)
        isLoading = false
    }

    val data = hrvData

    Scaffold(
        containerColor = Color(0xFFF3F2F8), // Soft neutral background matching reference
        topBar = {
            HeartRateDetailTopBar(onBackClick = onBackClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Heart Rate Variability Card
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Header: Title & Big Stat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Heart Rate Variability",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = if (data != null && data.hasData) "${data.latestHrvMs}" else "--",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Scatter/Bead Distribution Graph using Soft Red instead of Orange
                    HrvScatterGraph(
                        buckets = data?.buckets ?: emptyList(),
                        hasData = data?.hasData == true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // X-Axis Time Marks: 12am, 4am, 8am, 12pm, 4pm, 8pm
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val labels = listOf("12am", "4am", "8am", "12pm", "4pm", "8pm")
                        labels.forEach { label ->
                            val isHighlighted = (label == "4pm")
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                color = if (isHighlighted) Color(0xFF1E293B) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Bottom Two Sub-Cards matching the design: Stress Level & Ave. Variability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stress Level Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Stress Level",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (data != null && data.hasData) data.stressLevel else "--",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                }

                // Ave. Variability Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Ave. Variability",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (data != null && data.hasData) "${data.aveVariabilityMs}" else "--",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }
            }

            // Supplementary Heart Rate Range Card
            if (data != null && data.hasData) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE4E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Resting & Peak BPM",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Min ${data.minBpm} bpm · Max ${data.maxBpm} bpm",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Text(
                            text = "${data.latestBpm} bpm",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE11D48)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Top App Bar with back button, battery indicator, centered "Heart Rate" title, and avatar.
 */
@Composable
private fun HeartRateDetailTopBar(
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

        // Center: "Heart Rate" Title
        Text(
            text = "Heart Rate",
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
 * Renders the vertical bead/scatter column graph using soft red tones (`#F43F5E`, `#FB7185`, `#E11D48`)
 * instead of orange, with thin vertical guideline grid marks behind each time column.
 */
@Composable
private fun HrvScatterGraph(
    buckets: List<HrvBucket>,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    if (!hasData || buckets.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No heart rate variability records for this day",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
        return
    }

    // Soft red color palette
    val softRedBase = Color(0xFFE11D48)       // Crimson red
    val softRedMedium = Color(0xFFF43F5E)     // Rose red
    val softRedTranslucent = Color(0xFFFB7185).copy(alpha = 0.55f) // Soft red transparent bead
    val gridLineColor = Color(0xFFF1F5F9)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val columnCount = 6
        val colWidth = canvasWidth / columnCount

        // 1. Draw light vertical guide lines for each time column
        for (i in 0 until columnCount) {
            val cx = (i + 0.5f) * colWidth
            drawLine(
                color = gridLineColor,
                start = Offset(cx, 0f),
                end = Offset(cx, canvasHeight),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Bead vertical scatter parameters: HRV ranges typically between 30ms and 130ms
        val minHrvRange = 30f
        val maxHrvRange = 130f

        // Real values only: each column plots the samples recorded in that time bucket
        val beadRadius = 7.dp.toPx()
        val pillWidth = 14.dp.toPx()

        for (i in 0 until columnCount) {
            val cx = (i + 0.5f) * colWidth
            // Normalise real values into 0..1; columns without samples draw nothing
            val pattern = buckets.getOrNull(i)?.samples.orEmpty()
                .map { ((it - minHrvRange) / (maxHrvRange - minHrvRange)).coerceIn(0.02f, 0.98f) }
            if (pattern.isEmpty()) continue

            // Group into continuous capsule pill or overlapping rounded beads
            val minYNorm = pattern.min()
            val maxYNorm = pattern.max()

            val topY = (1f - maxYNorm) * canvasHeight
            val botY = (1f - minYNorm) * canvasHeight

            // Draw underlying rounded vertical pill capsule
            val capsuleHeight = (botY - topY).coerceAtLeast(beadRadius * 2)
            drawRoundRect(
                color = softRedTranslucent,
                topLeft = Offset(cx - pillWidth / 2, topY),
                size = Size(pillWidth, capsuleHeight),
                cornerRadius = CornerRadius(pillWidth / 2, pillWidth / 2)
            )

            // Draw overlapping dense circular scatter beads inside the column
            for (beadNorm in pattern) {
                val cy = (1f - beadNorm) * canvasHeight
                drawCircle(
                    color = softRedMedium.copy(alpha = 0.85f),
                    radius = beadRadius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

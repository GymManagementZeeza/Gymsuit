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
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.CalorieActivityItem
import ca.zeezaglobal.gymsuitapp.data.DetailedCaloriesData
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import java.time.LocalDate

/**
 * Dedicated Calories Details Screen matching the uploaded design reference:
 * - Top app bar with back navigation, battery pill, "Calories" title, and profile avatar.
 * - Bold donut ring chart styled after the reference image (with deep purple/lavender segmented arcs,
 *   subtle transparent background track, and smooth rounded endpoints).
 * - Readout of total calories burned against 4,000 kcal goal.
 * - Activity breakdown list:
 *   1. Workouts & Exercises
 *   2. Steps & Walking
 *   3. Active Movement
 *   4. Resting Metabolism (BMR)
 */
@Composable
fun CaloriesDetailScreen(
    date: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_fitness_prefs", android.content.Context.MODE_PRIVATE) }
    var targetKcal by remember {
        mutableDoubleStateOf(prefs.getFloat("calorie_goal", 4000.0f).toDouble())
    }
    var showGoalDialog by remember { mutableStateOf(false) }

    val healthConnectManager = remember { HealthConnectManager(context) }
    var caloriesData by remember { mutableStateOf<DetailedCaloriesData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(date) {
        isLoading = true
        caloriesData = healthConnectManager.readDetailedCaloriesForDate(date)
        isLoading = false
    }

    val data = caloriesData

    if (showGoalDialog) {
        var inputGoalText by remember { mutableStateOf(targetKcal.toInt().toString()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = Color(0xFF5B4D8C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Daily Calorie Goal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Set your personal daily calorie burn target (kcal):",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    OutlinedTextField(
                        value = inputGoalText,
                        onValueChange = {
                            inputGoalText = it.filter { char -> char.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Daily Target (kcal)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5B4D8C),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputGoalText.toIntOrNull()
                        if (parsed == null || parsed < 500 || parsed > 15000) {
                            errorMessage = "Please enter a valid goal (500 - 15,000 kcal)"
                        } else {
                            targetKcal = parsed.toDouble()
                            prefs.edit().putFloat("calorie_goal", parsed.toFloat()).apply()
                            showGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5B4D8C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Target", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF3F2F8), // Soft lavender/neutral tint matching reference
        topBar = {
            CaloriesDetailTopBar(onBackClick = onBackClick)
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
            // Main Calories Ring Card matching the uploaded reference image
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row: Title & Total Burned with Clickable Goal Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDE9FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFF5B4D8C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Calories Burned",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        // Clickable Goal Pill to change goal
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F3FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showGoalDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Goal: ${"%,d".format(targetKcal.toInt())}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5B4D8C)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit Goal",
                                    tint = Color(0xFF5B4D8C),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Segmented Donut Ring matching reference picture
                    CaloriesDonutRing(
                        activities = data?.activities ?: emptyList(),
                        totalCalories = data?.totalCaloriesKcal ?: 0.0,
                        targetCalories = targetKcal,
                        hasData = data?.hasData == true,
                        modifier = Modifier.size(240.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress % Subtitle
                    val totalKcal = data?.totalCaloriesKcal ?: 0.0
                    val pct = if (totalKcal > 0) ((totalKcal / targetKcal) * 100).toInt() else 0
                    Text(
                        text = if (data?.hasData == true) "$pct% of daily ${"%,d".format(targetKcal.toInt())} kcal target achieved" else "No calorie records recorded for this day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Activity Breakdown Section
            Text(
                text = "Activity Breakdown",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Activity Breakdown Cards
            if (data != null && data.hasData && data.activities.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    data.activities.forEach { item ->
                        CalorieActivityRowCard(item = item)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activity breakdown data for this date",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Top App Bar with back navigation button, battery indicator, "Calories" title, and user profile avatar.
 */
@Composable
private fun CaloriesDetailTopBar(
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

        // Center: "Calories" Title
        Text(
            text = "Calories",
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
 * Donut Ring Graphic faithfully reproducing the uploaded reference image:
 * Features thick segmented rounded arcs with subtle gaps and gradient/solid purple-lavender layers.
 */
@Composable
private fun CaloriesDonutRing(
    activities: List<CalorieActivityItem>,
    totalCalories: Double,
    targetCalories: Double,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (hasData && totalCalories > 0) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 800),
        label = "DonutAnimation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 28.dp.toPx()
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - (strokePx / 2f)
            val arcRect = Rect(center = centerOffset, radius = radius)

            // 1. Light background full track
            val trackColor = Color(0xFFEDE9FE).copy(alpha = 0.6f)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (!hasData || activities.isEmpty()) {
                return@Canvas
            }

            // 2. Draw segmented arcs for each activity around the 360 ring
            var currentStartAngle = -90f
            val totalBurn = activities.sumOf { it.caloriesKcal }.coerceAtLeast(1.0)
            val gapAngle = 5f // clean segment separation gap

            for (activity in activities) {
                val fraction = (activity.caloriesKcal / totalBurn).toFloat()
                val rawSweep = fraction * 360f
                val sweep = (rawSweep - gapAngle).coerceAtLeast(4f) * animatedProgress

                drawArc(
                    color = Color(activity.colorHex),
                    startAngle = currentStartAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                currentStartAngle += rawSweep
            }
        }

        // Center readout (Total kcal and label)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasData) "${totalCalories.toInt()}" else "0",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "total kcal",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}

/**
 * Individual Activity Breakdown Row Card
 */
@Composable
private fun CalorieActivityRowCard(
    item: CalorieActivityItem
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(item.colorHex))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = item.durationOrCount,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.caloriesKcal.toInt()} kcal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "${item.percentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(item.colorHex)
                )
            }
        }
    }
}

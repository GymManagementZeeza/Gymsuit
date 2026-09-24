package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.annotation.DrawableRes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.ui.theme.GymSuitAppTheme

enum class DashboardTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PLAN("Plan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    WORKOUTS("Workouts", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    ANALYTICS("Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings);

    val icon: ImageVector get() = selectedIcon
}

data class DayItem(
    val dayName: String,
    val dayNumber: String
)

@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }

    var selectedDayIndex by remember { mutableIntStateOf(2) } // Wednesday (index 2) default selected

    val days = remember {
        listOf(
            DayItem("Mon", "18"),
            DayItem("Tue", "19"),
            DayItem("Wed", "20"),
            DayItem("Thu", "21"),
            DayItem("Fri", "22"),
            DayItem("Sat", "23"),
            DayItem("Sun", "24")
        )
    }

    val allGreetingTexts = remember {
        listOf(
            "How're You Today?",
            "Ready to Crush It?",
            "Time to Move!",
            "Let's Build Strength!",
            "Make Today Count!",
            "Stay Focused & Strong!",
            "Fuel Your Energy!",
            "Push Your Limits!",
            "One Step at a Time!",
            "Consistency is Key!"
        )
    }

    // Pick 2 random distinct greeting texts per app load / open
    val sessionGreetingTexts = remember {
        allGreetingTexts.shuffled().take(2)
    }

    var greetingIndex by remember { mutableIntStateOf(0) }

    // Cycle from the 1st text to the 2nd text once, then stop
    LaunchedEffect(Unit) {
        delay(3500)
        greetingIndex = 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (selectedTab != DashboardTab.SETTINGS) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(12.dp))

                // Top Header: Avatar & Notification Bell
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Avatar
                    Image(
                        painter = painterResource(id = R.drawable.trainer_1),
                        contentDescription = "User Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                    )

                    // Material 3 Notification Bell with M3 Shape and Badge
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                            ) {
                                Text(
                                    text = "3",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    ) {
                        FilledTonalIconButton(
                            onClick = { /* Handle notification tap */ },
                            shape = RoundedCornerShape(16.dp), // M3 Medium Shape token
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.statusBarsPadding())
            }

            // Smooth Animated Content Switching
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(tween(250))) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(250))) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut(tween(200)))
                    }
                },
                label = "TabAnimation",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    DashboardTab.HOME -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Greeting Banner
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Hello, Zihad",
                                    fontSize = 15.sp,
                                    color = Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedContent(
                                    targetState = sessionGreetingTexts[greetingIndex],
                                    transitionSpec = {
                                        (slideInVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            initialOffsetY = { height -> height }
                                        ) + fadeIn(tween(400))) togetherWith
                                                (slideOutVertically(
                                                    animationSpec = tween(300),
                                                    targetOffsetY = { height -> -height }
                                                ) + fadeOut(tween(250)))
                                    },
                                    label = "GreetingCycleAnimation"
                                ) { text ->
                                    Text(
                                        text = text,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Date Switcher Header (matching reference design: "Today" / selected date + navigation arrows & reset)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentDay = days[selectedDayIndex]
                                val dateLabel = if (selectedDayIndex == 2) "Today" else "${currentDay.dayName}, May ${currentDay.dayNumber}"

                                Text(
                                    text = dateLabel,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous day button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = selectedDayIndex > 0) {
                                                selectedDayIndex -= 1
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                                contentDescription = "Previous Day",
                                                tint = if (selectedDayIndex > 0) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Next day button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable(enabled = selectedDayIndex < days.lastIndex) {
                                                selectedDayIndex += 1
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                                contentDescription = "Next Day",
                                                tint = if (selectedDayIndex < days.lastIndex) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Reset to Today button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                selectedDayIndex = 2 // Reset to Today (Wed 20)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = "Reset to Today",
                                                tint = if (selectedDayIndex == 2) Color(0xFF94A3B8) else Color(0xFF1E293B),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 2-Column Dashboard Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    GymSuitScoreWidget()
                                    GrowthWidget()
                                }

                                // Right Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    HealthOverviewWidget()
                                    ActivityStatisticWidget()
                                }
                            }

                            // Extra bottom padding so floating nav bar doesn't obscure content
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                    DashboardTab.SETTINGS -> {
                        SettingsPageContent(
                            onBackClick = { selectedTab = DashboardTab.HOME }
                        )
                    }
                    else -> {
                        // Blank Page Content for other non-Home tabs
                        BlankPageContent(tab = tab)
                    }
                }
            }
        }

        // Floating Material 3 Navigation Bar docked at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            DashboardFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

@Composable
private fun BlankPageContent(tab: DashboardTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 60.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF08A),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = tab.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This is a blank ${tab.title.lowercase()} page.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GymSuitScoreWidget() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFDCFCE7), Color(0xFFA7F3D0))
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color(0xFF065F46),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Freud Score",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "36/375",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF064E3B)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5x6 Dot Grid Matrix
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(5) { rowIndex ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(6) { colIndex ->
                                val isHighlighted = (rowIndex == 1 && colIndex in 2..3) ||
                                        (rowIndex == 2 && colIndex in 2..3) ||
                                        (rowIndex == 3 && colIndex == 3)
                                val isLight = (rowIndex == 0 && colIndex == 5)

                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isHighlighted -> Color.White
                                                isLight -> Color.White.copy(alpha = 0.9f)
                                                else -> Color(0xFF059669).copy(alpha = 0.35f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthOverviewWidget() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

    var latestWeightKg by remember { mutableStateOf<Double?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }
    val isAvailable = remember { healthConnectManager.isAvailable() }

    // Load initial permission and weight
    LaunchedEffect(Unit) {
        if (isAvailable) {
            val granted = healthConnectManager.hasWeightPermission()
            hasPermission = granted
            if (granted) {
                latestWeightKg = healthConnectManager.readLatestWeight()
            }
        }
        isChecking = false
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Health overview",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display real body weight or fallback
            val displayWeightText = remember(latestWeightKg) {
                latestWeightKg?.let {
                    if (it % 1.0 == 0.0) {
                        it.toInt().toString()
                    } else {
                        String.format(java.util.Locale.US, "%.1f", it)
                    }
                } ?: "75"
            }

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = displayWeightText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "kg weight",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(7) { index ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(if (index == 3) 70.dp else 40.dp)
                                .background(
                                    if (index == 3) Color(0xFF9CA3AF) else Color(0xFFE5E7EB),
                                    shape = RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF3F4F6),
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .border(1.dp, Color(0xFF6EE7B7), CircleShape)
                ) {
                    Text(
                        text = "${displayWeightText}kg",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthWidget() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Growth",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                Text(
                    text = "8.7%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val months = listOf("Jan" to 0.5f, "Feb" to 0.35f, "Mar" to 0.85f, "Apr" to 0.45f)

                months.forEach { (month, ratio) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((65 * ratio).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (month == "Mar") Color(0xFFD8B4FE) else Color(0xFFF3E8FF)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = month,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityStatisticWidget() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Mood Statistic",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()

                    drawArc(
                        color = Color(0xFF6EE7B7),
                        startAngle = 130f,
                        sweepAngle = 220f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = Color(0xFFFDE047),
                        startAngle = 0f,
                        sweepAngle = 80f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = Color(0xFFF472B6),
                        startAngle = 90f,
                        sweepAngle = 35f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "235",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Happy Mins",
                        fontSize = 9.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF6EE7B7), label = "Happy")
                LegendItem(color = Color(0xFFFDE047), label = "Sad")
                LegendItem(color = Color(0xFFF472B6), label = "Angry")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF4B5563)
        )
    }
}

@Composable
private fun DashboardFloatingNavBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    // Main tabs inside the Floating Toolbar container (excluding Settings which acts as the paired FAB)
    val mainTabs = remember {
        DashboardTab.entries.filter { it != DashboardTab.SETTINGS }
    }
    val isSettingsSelected = selectedTab == DashboardTab.SETTINGS

    Row(
        modifier = Modifier.wrapContentSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Material 3 Floating Toolbar Container
        Surface(
            shape = RoundedCornerShape(28.dp), // M3 full round toolbar pill
            color = colorScheme.surfaceContainer,
            shadowElevation = 4.dp,
            tonalElevation = 2.dp,
            modifier = Modifier.height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mainTabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabInteractionSource = remember { MutableInteractionSource() }

                    // M3 Toolbar Item Slot (no ripple on tap)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (isSelected) colorScheme.secondaryContainer else Color.Transparent,
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = tabInteractionSource,
                                indication = null
                            ) { onTabSelected(tab) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(
                                    horizontal = if (isSelected) 16.dp else 12.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            // Show label on selection with M3 expressive animation
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(tween(180)),
                                exit = fadeOut(tween(140))
                            ) {
                                Row {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSecondaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // M3 Paired Floating Action Button (FAB) for Settings (no ripple on tap)
        val settingsInteractionSource = remember { MutableInteractionSource() }
        Surface(
            shape = RoundedCornerShape(16.dp), // M3 Medium Shape
            color = if (isSettingsSelected) colorScheme.primary else colorScheme.primaryContainer,
            shadowElevation = 4.dp,
            tonalElevation = 4.dp,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = settingsInteractionSource,
                    indication = null
                ) { onTabSelected(DashboardTab.SETTINGS) }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isSettingsSelected) DashboardTab.SETTINGS.selectedIcon else DashboardTab.SETTINGS.unselectedIcon,
                    contentDescription = DashboardTab.SETTINGS.title,
                    tint = if (isSettingsSelected) colorScheme.onPrimary else colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    GymSuitAppTheme {
        DashboardScreen()
    }
}

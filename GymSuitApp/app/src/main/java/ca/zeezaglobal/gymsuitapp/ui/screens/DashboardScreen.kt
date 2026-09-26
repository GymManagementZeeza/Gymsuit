package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.annotation.DrawableRes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import ca.zeezaglobal.gymsuitapp.data.CaloriesBreakdown
import ca.zeezaglobal.gymsuitapp.data.SleepSessionData
import ca.zeezaglobal.gymsuitapp.data.HeartRateSummaryData
import ca.zeezaglobal.gymsuitapp.di.AppComponent
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import ca.zeezaglobal.gymsuitapp.ui.theme.PoppinsFontFamily

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
    val dayNumber: String,
    val localDate: LocalDate
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }

    // Dynamically generate 30 days ending with Today (last index = 29 = Today)
    val today = remember { LocalDate.now() }
    val days = remember(today) {
        val dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        (29 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            DayItem(
                dayName = date.format(dayNameFormatter),
                dayNumber = date.dayOfMonth.toString(),
                localDate = date
            )
        }
    }

    // Default to Today (last index in the 30-day window)
    var selectedDayIndex by remember { mutableIntStateOf(29) }

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

    // Curated DiceBear Gaze avatars pool
    val gazeAvatars = remember {
        listOf(
            R.drawable.avatar_gaze_1,
            R.drawable.avatar_gaze_2,
            R.drawable.avatar_gaze_3,
            R.drawable.avatar_gaze_4,
            R.drawable.avatar_gaze_5,
            R.drawable.avatar_gaze_6,
            R.drawable.avatar_gaze_7,
            R.drawable.avatar_gaze_8
        )
    }

    // Pick a random avatar on each new app launch / open
    val userAvatarResId = remember { gazeAvatars.random() }

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

    var isRefreshing by remember { mutableStateOf(false) }
    var syncKey by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    // Material 3 Motion Curves & Tokens
    // Emphasized: (0.2, 0.0, 0.0, 1.0)
    // Emphasized Accelerate: (0.3, 0.0, 0.8, 0.15)
    // Emphasized Decelerate: (0.05, 0.7, 0.1, 1.0)
    val m3EmphasizedEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }
    val m3EmphasizedDecelerateEasing = remember { CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) }

    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    val appComponent = remember { AppComponent.from(context) }
    val viewModel = remember(appComponent) {
        appComponent.dashboardViewModelFactory.create(DashboardViewModel::class.java)
    }
    val aiSummaryState by viewModel.summaryState.collectAsState()

    var userSession by remember { mutableStateOf(appComponent.authManager.getSession()) }
    var showNamePromptDialog by remember { mutableStateOf(false) }
    var isSavingName by remember { mutableStateOf(false) }
    var nameErrorMessage by remember { mutableStateOf<String?>(null) }

    val rawFirstName = userSession?.firstName?.trim() ?: ""
    val rawLastName = userSession?.lastName?.trim() ?: ""
    val userEmail = userSession?.email?.trim() ?: ""
    val hasValidName = rawFirstName.isNotEmpty() && !rawFirstName.equals(userEmail, ignoreCase = true)

    LaunchedEffect(userSession) {
        if (!hasValidName && appComponent.authManager.isLoggedIn()) {
            showNamePromptDialog = true
        }
    }

    // Trigger AI summary whenever selected date changes (immediate loading, 3s hold debounce)
    val selectedLocalDate = days[selectedDayIndex].localDate
    LaunchedEffect(selectedLocalDate) {
        viewModel.onDateSelected(selectedLocalDate)
    }

    // Log complete Health data as formatted JSON on screen load
    LaunchedEffect(Unit) {
        healthConnectManager.logAllHealthDataAsJson()
    }

    fun triggerSync() {
        if (!isRefreshing) {
            isRefreshing = true
            coroutineScope.launch {
                // Read and log live Health Connect data as JSON
                healthConnectManager.logAllHealthDataAsJson()
                viewModel.onDateSelected(days[selectedDayIndex].localDate, forceRefresh = true)
                delay(1200)
                syncKey += 1
                isRefreshing = false
            }
        }
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
                    // User Avatar (DiceBear Gaze style - randomized on open)
                    Image(
                        painter = painterResource(id = userAvatarResId),
                        contentDescription = "User Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFFE5E7EB), CircleShape)
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
                        val pullToRefreshState = rememberPullToRefreshState()

                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { triggerSync() },
                            state = pullToRefreshState,
                            indicator = {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 16.dp)
                                ) {
                                    if (isRefreshing) {
                                        M3ExpressiveLoadingIndicator(
                                            size = 48.dp,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            indicatorColor = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        PullToRefreshDefaults.Indicator(
                                            state = pullToRefreshState,
                                            isRefreshing = false,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
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
                                val displayName = if (hasValidName) rawFirstName else "Athlete"
                                Text(
                                    text = "Hello, $displayName",
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
                                val monthNameFormatter = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
                                val dateLabel = if (selectedDayIndex == days.lastIndex) "Today" else "${currentDay.dayName}, ${currentDay.localDate.format(monthNameFormatter)} ${currentDay.dayNumber}"

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
                                                selectedDayIndex = days.lastIndex // Reset to Today
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = "Reset to Today",
                                                tint = if (selectedDayIndex == days.lastIndex) Color(0xFF94A3B8) else Color(0xFF1E293B),
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
                                    WorkoutCardWidget(
                                        days = days,
                                        syncTrigger = syncKey
                                    )
                                    SleepWidget(
                                        selectedDate = days[selectedDayIndex].localDate,
                                        syncTrigger = syncKey
                                    )
                                }

                                // Right Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    HealthOverviewWidget(syncTrigger = syncKey)
                                    CaloriesBurnedWidget(
                                        selectedDate = days[selectedDayIndex].localDate,
                                        syncTrigger = syncKey
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Heart Rate Section (Full Width below 2-column grid)
                            HeartRateWidget(
                                selectedDate = days[selectedDayIndex].localDate,
                                syncTrigger = syncKey
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // AI Health Summary Section below the cards
                            AiSummaryCardWidget(
                                uiState = aiSummaryState,
                                selectedDate = days[selectedDayIndex].localDate,
                                onRetry = { viewModel.retryCurrentDate() }
                            )

                            // Extra bottom padding so floating nav bar doesn't obscure content
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
                    DashboardTab.SETTINGS -> {
                        SettingsPageContent(
                            avatarResId = userAvatarResId,
                            onBackClick = { selectedTab = DashboardTab.HOME },
                            onLogoutClick = onLogout
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

        if (showNamePromptDialog) {
            NamePromptDialog(
                isLoading = isSavingName,
                errorMessage = nameErrorMessage,
                onDismiss = {
                    // Only allow dismiss if user already has a valid name
                    if (hasValidName) {
                        showNamePromptDialog = false
                    }
                },
                onSave = { firstName, lastName ->
                    isSavingName = true
                    nameErrorMessage = null
                    coroutineScope.launch {
                        val email = userSession?.email ?: ""
                        val result = appComponent.mobileAuthApi.updateProfile(
                            email = email,
                            firstName = firstName,
                            lastName = lastName
                        )
                        result.onSuccess { updatedSession ->
                            appComponent.authManager.saveSession(updatedSession)
                            userSession = updatedSession
                            isSavingName = false
                            showNamePromptDialog = false
                        }.onFailure { error ->
                            isSavingName = false
                            nameErrorMessage = error.message ?: "Failed to save profile. Please try again."
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun NamePromptDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (firstName: String, lastName: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = "Welcome to GymSuit!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please enter your name so we can personalize your experience.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        validationError = null
                    },
                    label = { Text("First Name *") },
                    placeholder = { Text("e.g. Alex") },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF111827),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name (optional)") },
                    placeholder = { Text("e.g. Morgan") },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF111827),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                val displayError = validationError ?: errorMessage
                if (displayError != null) {
                    Text(
                        text = displayError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.trim().isBlank()) {
                        validationError = "First name is required"
                    } else {
                        onSave(firstName.trim(), lastName.trim())
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    )
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
private fun WorkoutCardWidget(
    days: List<DayItem> = emptyList(),
    syncTrigger: Int = 0
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    val today = remember { LocalDate.now() }
    val currentYearMonth = remember(today) { YearMonth.from(today) }
    val daysInCurrentMonth = remember(currentYearMonth) { currentYearMonth.lengthOfMonth() }
    val monthName = remember(today) { today.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault())) }
    val currentMonthDates = remember(currentYearMonth, daysInCurrentMonth) {
        (1..daysInCurrentMonth).map { dayNum ->
            currentYearMonth.atDay(dayNum)
        }
    }
    val prefs = remember { context.getSharedPreferences("gymsuit_workouts", Context.MODE_PRIVATE) }

    // Manually toggled workout dates stored locally
    var localWorkoutDays by remember {
        val saved = prefs.getStringSet("completed_dates", emptySet()) ?: emptySet()
        mutableStateOf(saved.mapNotNull {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }.toSet())
    }

    var healthConnectWorkoutDays by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    val isAvailable = remember { healthConnectManager.isAvailable() }

    LaunchedEffect(syncTrigger) {
        if (isAvailable && healthConnectManager.hasExercisePermission()) {
            healthConnectWorkoutDays = healthConnectManager.readWorkoutDays()
        }
    }

    val allWorkoutDays = remember(localWorkoutDays, healthConnectWorkoutDays) {
        localWorkoutDays + healthConnectWorkoutDays
    }

    val workoutCountInMonth = remember(currentMonthDates, allWorkoutDays) {
        currentMonthDates.count { it in allWorkoutDays }
    }

    val totalRows = (daysInCurrentMonth + 5) / 6

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
                // Header Row: Icon, Title & Month Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FitnessCenter,
                            contentDescription = "Workout",
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Workout",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF065F46).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = monthName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Text: Completed Workouts in the current calendar month
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$workoutCountInMonth",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF064E3B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/$daysInCurrentMonth days",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46).copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dot Grid Matrix representing the days of the current month (e.g. 30 dots for September)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(totalRows) { rowIndex ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(6) { colIndex ->
                                val dayNum = rowIndex * 6 + colIndex + 1
                                if (dayNum <= daysInCurrentMonth) {
                                    val date = currentYearMonth.atDay(dayNum)
                                    val isToday = (date == today)
                                    val didWorkout = date in allWorkoutDays
                                    val isFuture = date > today

                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    didWorkout -> Color.White
                                                    isToday -> Color(0xFF059669).copy(alpha = 0.6f)
                                                    isFuture -> Color(0xFF059669).copy(alpha = 0.15f)
                                                    else -> Color(0xFF059669).copy(alpha = 0.35f)
                                                }
                                            )
                                            .then(
                                                if (isToday) {
                                                    // Highlight today's dot (e.g. 23rd dot)
                                                    if (didWorkout) {
                                                        Modifier.border(2.dp, Color(0xFF065F46), CircleShape)
                                                    } else {
                                                        Modifier.border(2.dp, Color.White, CircleShape)
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                val newSet = if (date in localWorkoutDays) {
                                                    localWorkoutDays - date
                                                } else {
                                                    localWorkoutDays + date
                                                }
                                                localWorkoutDays = newSet
                                                prefs.edit().putStringSet(
                                                    "completed_dates",
                                                    newSet.map { it.toString() }.toSet()
                                                ).apply()
                                            }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthOverviewWidget(
    syncTrigger: Int = 0
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

    var latestWeightKg by remember { mutableStateOf<Double?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }
    val isAvailable = remember { healthConnectManager.isAvailable() }

    // Load initial permission and weight, and reload whenever syncTrigger updates
    LaunchedEffect(syncTrigger) {
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
private fun SleepWidget(
    selectedDate: LocalDate = LocalDate.now(),
    syncTrigger: Int = 0
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var realSleepSessions by remember { mutableStateOf<List<SleepSessionData>>(emptyList()) }
    val isAvailable = remember { healthConnectManager.isAvailable() }

    LaunchedEffect(syncTrigger) {
        if (isAvailable && healthConnectManager.hasSleepPermission()) {
            realSleepSessions = healthConnectManager.readRecentSleepSessions()
        }
    }

    val zoneId = remember { ZoneId.systemDefault() }

    // Match real sleep sessions by the local date the session ended on or started on
    val sessionsForDay = remember(realSleepSessions, selectedDate) {
        realSleepSessions.filter { session ->
            val endLocalDate = session.endTime.atZone(zoneId).toLocalDate()
            val startLocalDate = session.startTime.atZone(zoneId).toLocalDate()
            endLocalDate == selectedDate || startLocalDate == selectedDate
        }
    }

    val hasData = sessionsForDay.isNotEmpty()
    val totalDurationMinutes = remember(sessionsForDay) {
        sessionsForDay.sumOf { it.durationMinutes }
    }
    val displaySleepTime = remember(hasData, totalDurationMinutes) {
        if (hasData) {
            val h = totalDurationMinutes / 60
            val m = totalDurationMinutes % 60
            "${h}h ${m}m"
        } else {
            "No data"
        }
    }
    val startTimeFormatted = remember(sessionsForDay) {
        sessionsForDay.minByOrNull { it.startTime }?.startTimeFormatted ?: "--"
    }
    val endTimeFormatted = remember(sessionsForDay) {
        sessionsForDay.maxByOrNull { it.endTime }?.endTimeFormatted ?: "--"
    }

    // Average daily sleep duration across recorded sessions
    val avgDailySleepMinutes = remember(realSleepSessions) {
        if (realSleepSessions.isEmpty()) null
        else {
            val dailyTotals = realSleepSessions
                .groupBy { it.endTime.atZone(zoneId).toLocalDate() }
                .values
                .map { sessions -> sessions.sumOf { it.durationMinutes } }
            if (dailyTotals.isEmpty()) null else dailyTotals.average().toInt()
        }
    }
    val avgSleepFormatted = remember(avgDailySleepMinutes) {
        avgDailySleepMinutes?.let { totalMins ->
            val h = totalMins / 60
            val m = totalMins % 60
            if (h > 0) "${h}h ${m}m" else "${m}m"
        } ?: "--"
    }

    // Determine if average sleep is trending up or down
    val isAverageGoingUp = remember(realSleepSessions) {
        if (realSleepSessions.isEmpty()) null
        else {
            val dayTotals = realSleepSessions
                .groupBy { it.endTime.atZone(zoneId).toLocalDate() }
                .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
                .toList()
                .sortedBy { it.first } // oldest to newest

            if (dayTotals.size >= 2) {
                val half = dayTotals.size / 2
                val olderAvg = dayTotals.take(half).map { it.second }.average()
                val recentAvg = dayTotals.drop(half).map { it.second }.average()
                recentAvg >= olderAvg
            } else if (dayTotals.isNotEmpty() && avgDailySleepMinutes != null) {
                (dayTotals.last().second) >= 420
            } else {
                null
            }
        }
    }

    val sleepHeatColors = remember(hasData) {
        if (hasData) {
            listOf(
                Color(0xFF34D399), // Green
                Color(0xFF059669), // Deep green
                Color(0xFFF97316), // Orange REM
                Color(0xFFEF4444), // Red Awake
                Color(0xFFFB923C), // Orange REM
                Color(0xFF059669)  // Deep green
            )
        } else {
            listOf(
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0)
            )
        }
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
            // Header Row: Icon & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = "Sleep",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sleep",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Average sleep duration on top of today's sleep text with Up/Down Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avg: $avgSleepFormatted",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                if (isAverageGoingUp != null) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAverageGoingUp) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAverageGoingUp) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                            contentDescription = if (isAverageGoingUp) "Trending Up" else "Trending Down",
                            tint = if (isAverageGoingUp) Color(0xFF16A34A) else Color(0xFFDC2626),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Duration readout
            Text(
                text = displaySleepTime,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (hasData) Color(0xFF111827) else Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sleep Start Time & End Time Header above the Heatmap Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = startTimeFormatted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = endTimeFormatted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Sleep Stage Heatmap Bar (Green = Deep sleep, Orange = REM sleep, Red = Awake during sleep)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = sleepHeatColors
                        )
                    )
            ) {
                if (hasData) {
                    // Segment marker dividers & pips along the timeline
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(6) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(8.dp)
                                    .background(Color.White.copy(alpha = 0.45f), CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stage legend indicators (Deep Sleep, REM, Awake)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF059669))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Deep",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF047857)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF97316))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "REM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC2410C)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Awake",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeartRateWidget(
    selectedDate: LocalDate = LocalDate.now(),
    syncTrigger: Int = 0
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var hrData by remember {
        mutableStateOf(healthConnectManager.defaultHeartRateSample())
    }
    val isAvailable = remember { healthConnectManager.isAvailable() }

    LaunchedEffect(selectedDate, syncTrigger) {
        if (isAvailable) {
            val hasHr = healthConnectManager.hasHeartRatePermission()
            if (hasHr) {
                hrData = healthConnectManager.readHeartRateDataForDate(selectedDate)
            } else {
                hrData = healthConnectManager.readHeartRateDataForDate(selectedDate)
            }
        } else {
            hrData = healthConnectManager.readHeartRateDataForDate(selectedDate)
        }
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
            // Header Row: Icon, Title & Latest BPM readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE4E6)), // Soft rose/red container
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Heart rate",
                            tint = Color(0xFFE11D48), // Elegant soft crimson/rose red
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Heart Rate",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                // Main BPM Metric with BPM unit label
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${hrData.latestBpm}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "bpm",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Heart Rate ECG Graph Canvas
            val points = hrData.points
            val minBpm = hrData.minBpm
            val maxBpm = hrData.maxBpm

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val boxWidth = maxWidth

                val minIdx = points.indexOfFirst { it.bpm == minBpm }.takeIf { it >= 0 } ?: 0
                val maxIdx = points.indexOfFirst { it.bpm == maxBpm }.takeIf { it >= 0 } ?: (points.size - 1)
                val minFraction = if (points.isNotEmpty()) (minIdx.toFloat() / (points.size - 1).coerceAtLeast(1)) else 0.15f
                val maxFraction = if (points.isNotEmpty()) (maxIdx.toFloat() / (points.size - 1).coerceAtLeast(1)) else 0.75f

                // Min BPM text label positioned horizontally above min point
                Text(
                    text = "$minBpm",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4B5563),
                    modifier = Modifier
                        .offset(x = (boxWidth * minFraction) - 8.dp, y = 0.dp)
                )

                // Max BPM text label positioned horizontally above max point
                Text(
                    text = "$maxBpm",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48),
                    modifier = Modifier
                        .offset(x = (boxWidth * maxFraction) - 10.dp, y = 0.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp, bottom = 4.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    if (points.isEmpty()) return@Canvas

                    val effectiveMin = (minBpm - 5).coerceAtLeast(30)
                    val effectiveMax = (maxBpm + 5).coerceAtLeast(effectiveMin + 20)
                    val bpmRange = (effectiveMax - effectiveMin).toFloat()

                    // Compute pixel coordinates
                    val coords = points.mapIndexed { idx, point ->
                        val x = (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * width
                        val normalizedY = (point.bpm - effectiveMin) / bpmRange
                        // Invert Y so higher bpm is higher up
                        val y = height - (normalizedY * (height - 16f)) - 8f
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    // Build smooth path
                    val strokePath = Path().apply {
                        if (coords.isNotEmpty()) {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                val prev = coords[i - 1]
                                val curr = coords[i]
                                val midX = (prev.x + curr.x) / 2f
                                val midY = (prev.y + curr.y) / 2f
                                quadraticTo(prev.x, prev.y, midX, midY)
                            }
                            lineTo(coords.last().x, coords.last().y)
                        }
                    }

                    // Find min coordinate and max coordinate by sample index
                    val minCoord = coords.getOrElse(minIdx) { coords.first() }
                    val maxCoord = coords.getOrElse(maxIdx) { coords.last() }

                    // Draw vertical dotted line to min point
                    val dotSpacing = 8f
                    var curY = 0f
                    while (curY < minCoord.y) {
                        drawLine(
                            color = Color(0xFFD1D5DB),
                            start = androidx.compose.ui.geometry.Offset(minCoord.x, curY),
                            end = androidx.compose.ui.geometry.Offset(minCoord.x, curY + 4f),
                            strokeWidth = 1.5f
                        )
                        curY += dotSpacing
                    }

                    // Draw vertical dotted line to max point
                    curY = 0f
                    while (curY < maxCoord.y) {
                        drawLine(
                            color = Color(0xFFFECDD3),
                            start = androidx.compose.ui.geometry.Offset(maxCoord.x, curY),
                            end = androidx.compose.ui.geometry.Offset(maxCoord.x, curY + 4f),
                            strokeWidth = 1.5f
                        )
                        curY += dotSpacing
                    }

                    // Draw ECG line with smooth soft red gradient
                    drawPath(
                        path = strokePath,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFB7185).copy(alpha = 0.5f),
                                Color(0xFFF43F5E),
                                Color(0xFFE11D48),
                                Color(0xFFF43F5E),
                                Color(0xFFFB7185).copy(alpha = 0.6f)
                            )
                        ),
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw min circle (grey/white bordered circle)
                    drawCircle(
                        color = Color(0xFF6B7280),
                        radius = 4.5f,
                        center = minCoord
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f,
                        center = minCoord
                    )

                    // Draw max circle (solid soft red circle)
                    drawCircle(
                        color = Color(0xFFE11D48),
                        radius = 5f,
                        center = maxCoord
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-axis Time Labels (4 evenly spaced times across the graph timeline)
            val timeAxisLabels = remember(points) {
                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                val zoneId = ZoneId.systemDefault()
                if (points.size >= 4) {
                    val n = points.size
                    val i0 = 0
                    val i1 = n / 3
                    val i2 = (2 * n) / 3
                    val i3 = n - 1
                    listOf(
                        points[i0].time.atZone(zoneId).format(timeFormatter),
                        points[i1].time.atZone(zoneId).format(timeFormatter),
                        points[i2].time.atZone(zoneId).format(timeFormatter),
                        points[i3].time.atZone(zoneId).format(timeFormatter)
                    )
                } else if (points.isNotEmpty()) {
                    listOf(
                        points.first().time.atZone(zoneId).format(timeFormatter),
                        "",
                        "",
                        points.last().time.atZone(zoneId).format(timeFormatter)
                    )
                } else {
                    listOf("2:23 PM", "3:03 PM", "3:43 PM", "4:23 PM")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                timeAxisLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaloriesBurnedWidget(
    selectedDate: LocalDate = LocalDate.now(),
    syncTrigger: Int = 0
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var caloriesBreakdown by remember { mutableStateOf<CaloriesBreakdown>(CaloriesBreakdown(0.0, 0.0, 0.0, 0.0, false)) }
    var hasPermission by remember { mutableStateOf(false) }
    val isAvailable = remember { healthConnectManager.isAvailable() }

    LaunchedEffect(selectedDate, syncTrigger) {
        if (isAvailable) {
            val granted = healthConnectManager.hasCaloriesPermission()
            hasPermission = granted
            if (granted) {
                caloriesBreakdown = healthConnectManager.readCaloriesBreakdownForDate(selectedDate)
            }
        }
    }

    val totalKcal = caloriesBreakdown.totalKcal
    val hasData = caloriesBreakdown.hasData && totalKcal > 0.0

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
            // Header: Flame Icon and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEDD5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = "Calories",
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Calories",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Single Calorie Ring with 4000 kcal Goal
            val targetKcal = 4000.0
            val currentKcal = if (hasData) totalKcal else 520.0
            val progressFraction = (currentKcal / targetKcal).coerceIn(0.0, 1.0).toFloat()
            val strokeWidthDp = 13.dp

            Box(
                modifier = Modifier
                    .size(136.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = strokeWidthDp.toPx()
                    val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    val radius = (size.minDimension / 2f) - (strokePx / 2f)
                    val ringRect = androidx.compose.ui.geometry.Rect(center = centerOffset, radius = radius)

                    // Background Track (Soft translucent red)
                    drawArc(
                        color = Color(0xFFFFE4E6),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = ringRect.topLeft,
                        size = ringRect.size,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // Active Progress Arc (Vibrant soft red gradient)
                    if (progressFraction > 0.005f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFB7185),
                                    Color(0xFFF43F5E),
                                    Color(0xFFE11D48),
                                    Color(0xFFFB7185)
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = progressFraction * 360f,
                            useCenter = false,
                            topLeft = ringRect.topLeft,
                            size = ringRect.size,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center Total Calorie Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${currentKcal.toInt()}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "kcal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target Footer Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal: ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "4,000 kcal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48)
                )
            }
        }
    }
}

@Composable
private fun FloatingCategoryBadge(
    label: String,
    angleDegrees: Float,
    textColor: Color,
    radiusDp: Float
) {
    val rad = Math.toRadians(angleDegrees.toDouble())
    val offsetX = (Math.cos(rad) * radiusDp).dp
    val offsetY = (Math.sin(rad) * radiusDp).dp

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier.offset(x = offsetX, y = offsetY)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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

/**
 * AI Summary view without cardview container.
 * Features larger Poppins font and smooth line-by-line animated appearance when loaded.
 */
@Composable
private fun AiSummaryCardWidget(
    uiState: AiSummaryUiState,
    selectedDate: LocalDate,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val isToday = (selectedDate == today)
    val dateTag = if (isToday) "Today" else selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        // Subtle, minimalist section header (no card, no elevated surface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "AI Summary",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Health Summary",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Daily shifts & insights",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9)
            ) {
                Text(
                    text = dateTag.uppercase(),
                    fontFamily = PoppinsFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body content
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(tween(350)) togetherWith fadeOut(tween(200))
            },
            label = "AiSummaryStateAnimation"
        ) { state ->
            when (state) {
                is AiSummaryUiState.Loading, AiSummaryUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        M3ExpressiveLoadingIndicator(
                            size = 44.dp,
                            isContained = true,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            indicatorColor = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Analyzing your health & activity data...",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                is AiSummaryUiState.Success -> {
                    // Line by line smooth animated presentation
                    AnimatedLineByLineSummary(
                        summaryText = state.summary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is AiSummaryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            fontFamily = PoppinsFontFamily,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF4F46E5)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Try Again",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Try Again",
                                fontFamily = PoppinsFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders sentences/lines with a staggered smooth fade-in and slide-up animation.
 * Features prominent, larger Poppins typography.
 */
@Composable
private fun AnimatedLineByLineSummary(
    summaryText: String,
    modifier: Modifier = Modifier
) {
    // Split into sentences / thoughts cleanly while keeping punctuation
    val lines = remember(summaryText) {
        val regex = Regex("(?<=[.!?])\\s+")
        val split = summaryText.split(regex).map { it.trim() }.filter { it.isNotBlank() }
        if (split.isNotEmpty()) split else listOf(summaryText.trim())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        lines.forEachIndexed { index, line ->
            var isVisible by remember(summaryText, index) { mutableStateOf(false) }

            LaunchedEffect(summaryText, index) {
                // Staggered reveal: each line appears smoothly with delay
                delay(index * 220L)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                ) + slideInVertically(
                    animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
                    initialOffsetY = { 20 }
                )
            ) {
                Text(
                    text = line,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    letterSpacing = (-0.1).sp
                )
            }
        }
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

/**
 * Material 3 Expressive Loading Indicator matching:
 * - https://m3.material.io/components/loading-indicator/overview
 * - https://github.com/material-components/material-components-android/blob/master/docs/components/LoadingIndicator.md
 *
 * Supports:
 * - Default (Contained): Active shape-morphing indicator housed within an elevated container (default 48dp).
 * - Uncontained: Direct shape-morphing indicator without a background container surface (default 36dp).
 * - Dynamic color tokens: indicatorColor = colorScheme.primary, containerColor = colorScheme.surfaceContainerHighest.
 */
@Composable
fun M3ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    isContained: Boolean = true,
    size: androidx.compose.ui.unit.Dp = if (isContained) 48.dp else 36.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    indicatorColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "M3LoadingIndicatorMotion")

    // Slow, graceful continuous rotation (4800ms)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "M3LoadingRotation"
    )

    // Smooth, leisurely shape morphing cycle (5400ms) with M3 Emphasized easing
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5400,
                easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // M3 Emphasized easing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "M3LoadingShapeMorph"
    )

    // Very gentle, calm breathing pulse (1800ms)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f) // M3 Emphasized Decelerate
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "M3LoadingScale"
    )

    val content = @Composable {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier
                    .size(if (isContained) size * 0.56f else size * 0.80f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    }
            ) {
                val canvasSize = this.size.minDimension
                val center = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = canvasSize * 0.44f

                // Continuous radial function R(theta, progress) for seamless vertex morphing
                val progress = morphProgress % 3f
                val totalSteps = 48
                val path = Path()

                val points = ArrayList<androidx.compose.ui.geometry.Offset>(totalSteps)

                for (i in 0 until totalSteps) {
                    val angleRad = (i * 2.0 * Math.PI) / totalSteps

                    // Shape 1 (Clover / 4-petal flower): r = 0.78 + 0.22 * cos(4 * theta)
                    val rShape1 = (0.78 + 0.22 * Math.cos(4.0 * angleRad)).toFloat()

                    // Shape 2 (Soft Rounded Diamond / 2-fold symmetric): r = 0.82 + 0.18 * cos(2 * theta)
                    val rShape2 = (0.82 + 0.18 * Math.cos(2.0 * angleRad)).toFloat()

                    // Shape 3 (Soft Rounded Triangle / 3-petal organic): r = 0.80 + 0.20 * cos(3 * theta)
                    val rShape3 = (0.80 + 0.20 * Math.cos(3.0 * angleRad)).toFloat()

                    // Smooth interpolation between shapes based on current progress phase
                    val currentR = when {
                        progress < 1f -> {
                            val t = progress
                            // Hermite smoothstep for velvet interpolation
                            val smoothT = t * t * (3f - 2f * t)
                            rShape1 * (1f - smoothT) + rShape2 * smoothT
                        }
                        progress < 2f -> {
                            val t = progress - 1f
                            val smoothT = t * t * (3f - 2f * t)
                            rShape2 * (1f - smoothT) + rShape3 * smoothT
                        }
                        else -> {
                            val t = progress - 2f
                            val smoothT = t * t * (3f - 2f * t)
                            rShape3 * (1f - smoothT) + rShape1 * smoothT
                        }
                    }

                    val r = baseRadius * currentR
                    val x = center.x + (r * Math.cos(angleRad)).toFloat()
                    val y = center.y + (r * Math.sin(angleRad)).toFloat()
                    points.add(androidx.compose.ui.geometry.Offset(x, y))
                }

                // Construct smooth closed spline curve through the points
                path.moveTo(points[0].x, points[0].y)
                for (i in 0 until totalSteps) {
                    val p0 = points[(i - 1 + totalSteps) % totalSteps]
                    val p1 = points[i]
                    val p2 = points[(i + 1) % totalSteps]
                    val p3 = points[(i + 2) % totalSteps]

                    // Catmull-Rom to Cubic Bezier conversion for silky continuous tangent curvature
                    val cp1x = p1.x + (p2.x - p0.x) / 6f
                    val cp1y = p1.y + (p2.y - p0.y) / 6f
                    val cp2x = p2.x - (p3.x - p1.x) / 6f
                    val cp2y = p2.y - (p3.y - p1.y) / 6f

                    path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                }
                path.close()

                drawPath(
                    path = path,
                    color = indicatorColor
                )
            }
        }
    }

    if (isContained) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            tonalElevation = 4.dp,
            modifier = modifier.size(size)
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier.size(size)
        ) {
            content()
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

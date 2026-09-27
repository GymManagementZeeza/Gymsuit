package ca.zeezaglobal.gymsuitapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ca.zeezaglobal.gymsuitapp.ui.screens.DashboardScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.HealthConnectPermissionScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.LoginScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.OnboardingScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.RegisterScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.SleepDetailScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.HeartRateDetailScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.CaloriesDetailScreen
import ca.zeezaglobal.gymsuitapp.ui.theme.GymSuitAppTheme

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.di.AppComponent
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppScreen {
    ONBOARDING,
    LOGIN,
    REGISTER,
    HEALTH_CONNECT_PERMISSION,
    DASHBOARD,
    SLEEP_DETAIL,
    HEART_RATE_DETAIL,
    CALORIES_DETAIL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymSuitAppTheme {
                val context = LocalContext.current
                val appComponent = remember { AppComponent.from(context) }
                val coroutineScope = rememberCoroutineScope()
                val healthConnectManager = remember { HealthConnectManager(context) }
                var selectedSleepDate by remember { mutableStateOf(LocalDate.now()) }
                var selectedHeartRateDate by remember { mutableStateOf(LocalDate.now()) }
                var selectedCaloriesDate by remember { mutableStateOf(LocalDate.now()) }

                // Check if user is already logged in
                val initialScreen = remember {
                    if (appComponent.authManager.isLoggedIn()) {
                        AppScreen.DASHBOARD
                    } else {
                        AppScreen.ONBOARDING
                    }
                }
                var currentScreen by remember { mutableStateOf(initialScreen) }

                fun navigateAfterLogin() {
                    coroutineScope.launch {
                        val alreadyAccepted = healthConnectManager.isAvailable() &&
                                healthConnectManager.hasAnyPermissions()
                        currentScreen = if (alreadyAccepted) {
                            AppScreen.DASHBOARD
                        } else {
                            AppScreen.HEALTH_CONNECT_PERMISSION
                        }
                    }
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> width / 3 } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(300))) togetherWith
                                    (slideOutHorizontally { width -> width / 3 } + fadeOut(tween(200)))
                        }
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(
                                onSkipClick = {
                                    currentScreen = AppScreen.LOGIN
                                },
                                onContinueClick = {
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                onBackClick = {
                                    currentScreen = AppScreen.ONBOARDING
                                },
                                onNavigateToRegister = {
                                    currentScreen = AppScreen.REGISTER
                                },
                                onLoginSuccess = {
                                    navigateAfterLogin()
                                }
                            )
                        }
                        AppScreen.REGISTER -> {
                            RegisterScreen(
                                onBackClick = {
                                    currentScreen = AppScreen.LOGIN
                                },
                                onNavigateToLogin = {
                                    currentScreen = AppScreen.LOGIN
                                },
                                onRegisterSuccess = {
                                    navigateAfterLogin()
                                }
                            )
                        }
                        AppScreen.HEALTH_CONNECT_PERMISSION -> {
                            HealthConnectPermissionScreen(
                                onContinue = {
                                    currentScreen = AppScreen.DASHBOARD
                                },
                                onSkip = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                onNavigateBack = {
                                    currentScreen = AppScreen.LOGIN
                                },
                                onLogout = {
                                    appComponent.authManager.clear()
                                    currentScreen = AppScreen.LOGIN
                                },
                                onNavigateToSleepDetail = { date ->
                                    selectedSleepDate = date
                                    currentScreen = AppScreen.SLEEP_DETAIL
                                },
                                onNavigateToHeartRateDetail = { date ->
                                    selectedHeartRateDate = date
                                    currentScreen = AppScreen.HEART_RATE_DETAIL
                                },
                                onNavigateToCaloriesDetail = { date ->
                                    selectedCaloriesDate = date
                                    currentScreen = AppScreen.CALORIES_DETAIL
                                }
                            )
                        }
                        AppScreen.SLEEP_DETAIL -> {
                            SleepDetailScreen(
                                date = selectedSleepDate,
                                onBackClick = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                        AppScreen.HEART_RATE_DETAIL -> {
                            HeartRateDetailScreen(
                                date = selectedHeartRateDate,
                                onBackClick = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                        AppScreen.CALORIES_DETAIL -> {
                            CaloriesDetailScreen(
                                date = selectedCaloriesDate,
                                onBackClick = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
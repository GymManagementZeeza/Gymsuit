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
import ca.zeezaglobal.gymsuitapp.ui.screens.LoginScreen
import ca.zeezaglobal.gymsuitapp.ui.screens.OnboardingScreen
import ca.zeezaglobal.gymsuitapp.ui.theme.GymSuitAppTheme

enum class AppScreen {
    ONBOARDING,
    LOGIN,
    DASHBOARD
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymSuitAppTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.ONBOARDING) }

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
                                onLoginClick = { email ->
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                onNavigateBack = {
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
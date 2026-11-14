package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.FoodStore
import android.content.Context
import ca.zeezaglobal.gymsuitapp.di.AppComponent
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBackClick: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appComponent = remember { AppComponent.from(context) }
    val colorScheme = MaterialTheme.colorScheme

    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Developer options (test-only): a gear on this screen autofills the test account's email.
    val devPrefs = remember { context.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE) }
    var devEnabled by remember { mutableStateOf(devPrefs.getBoolean(DEV_KEY_ENABLED, false)) }
    var showDevDialog by remember { mutableStateOf(false) }
    LaunchedEffect(devEnabled) {
        if (devEnabled && email.isBlank()) email = TEST_ACCOUNT_EMAIL
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        // Top Header Image with overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.light_gym_bg),
                contentDescription = "Light Gym Interior",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                colorScheme.surface.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            }

            // Settings (developer options)
            IconButton(
                onClick = { showDevDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 12.dp, top = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Curved Surface occupying bottom portion
        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 210.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (!otpSent) "Welcome Back!" else "Enter Verification Code",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (!otpSent) {
                            "Enter your email to receive a 6-digit one-time sign in code. No password required."
                        } else {
                            "We sent a 6-digit verification code to $email. Enter it below to log in."
                        },
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (infoMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = infoMessage ?: "",
                                color = Color(0xFF16A34A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Email Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Email Address",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                                infoMessage = null
                            },
                            enabled = !otpSent && !isLoading,
                            placeholder = {
                                Text(
                                    text = "member@gymsuit.com",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (otpSent) {
                                    TextButton(
                                        onClick = {
                                            otpSent = false
                                            otp = ""
                                            infoMessage = null
                                            errorMessage = null
                                        }
                                    ) {
                                        Text(
                                            text = "Change",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = if (otpSent) ImeAction.Next else ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // OTP Field (visible once OTP is requested)
                    if (otpSent) {
                        Spacer(modifier = Modifier.height(18.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Verification Code (OTP)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = otp,
                                onValueChange = {
                                    if (it.length <= 6) {
                                        otp = it
                                        errorMessage = null
                                    }
                                },
                                enabled = !isLoading,
                                placeholder = {
                                    Text(
                                        text = "123456",
                                        color = Color(0xFF9E9E9E),
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Key,
                                        contentDescription = null,
                                        tint = colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Didn't receive code? ",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Resend OTP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        if (isLoading) return@clickable
                                        isLoading = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val res = appComponent.mobileAuthApi.sendOtp(email, "login")
                                            isLoading = false
                                            res.fold(
                                                onSuccess = { msg ->
                                                    infoMessage = msg
                                                },
                                                onFailure = { err ->
                                                    errorMessage = err.message ?: "Failed to resend code"
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Action Button (Send OTP or Verify & Login)
                    Button(
                        onClick = {
                            if (!otpSent) {
                                if (email.isBlank()) {
                                    errorMessage = "Please enter your email address"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                infoMessage = null
                                coroutineScope.launch {
                                    val result = appComponent.mobileAuthApi.sendOtp(email, "login")
                                    isLoading = false
                                    result.fold(
                                        onSuccess = { msg ->
                                            otpSent = true
                                            infoMessage = msg
                                        },
                                        onFailure = { error ->
                                            errorMessage = error.message ?: "Could not send verification code"
                                        }
                                    )
                                }
                            } else {
                                if (otp.trim().length != 6) {
                                    errorMessage = "Please enter the 6-digit verification code"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                infoMessage = null
                                coroutineScope.launch {
                                    val result = appComponent.mobileAuthApi.verifyLoginOtp(email, otp)
                                    isLoading = false
                                    result.fold(
                                        onSuccess = { session ->
                                            FoodStore(context).claimFor(session.email)
                                            appComponent.authManager.saveSession(session)
                                            onLoginSuccess()
                                        },
                                        onFailure = { error ->
                                            errorMessage = error.message ?: "Incorrect or expired verification code"
                                        }
                                    )
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = if (!otpSent) "Send Verification Code" else "Verify & Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sign up link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            fontSize = 13.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Register",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }
        }

        if (showDevDialog) {
            AlertDialog(
                onDismissRequest = { showDevDialog = false },
                icon = { Icon(Icons.Outlined.DeveloperMode, contentDescription = null) },
                title = { Text("Developer options") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Enable developer options",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Autofills the test account email for sign-in.",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = devEnabled,
                                onCheckedChange = { on ->
                                    devEnabled = on
                                    devPrefs.edit().putBoolean(DEV_KEY_ENABLED, on).apply()
                                    if (on) {
                                        email = TEST_ACCOUNT_EMAIL
                                        otpSent = false
                                        otp = ""
                                        errorMessage = null
                                        infoMessage = null
                                    }
                                }
                            )
                        }
                        if (devEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.secondaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Test account: $TEST_ACCOUNT_EMAIL\nUse the fixed test code when it's enabled on the server.",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDevDialog = false }) { Text("Done") }
                }
            )
        }
    }
}

/** Test-only developer options for the login screen. */
private const val DEV_PREFS = "gymsuit_dev"
private const val DEV_KEY_ENABLED = "dev_enabled"
private const val TEST_ACCOUNT_EMAIL = "athul.6560@gmail.com"


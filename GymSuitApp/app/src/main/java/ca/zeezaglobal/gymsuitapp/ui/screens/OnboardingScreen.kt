package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.ui.theme.GymSuitAppTheme

@Composable
fun OnboardingScreen(
    onSkipClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Full screen gym background image
        Image(
            painter = painterResource(id = R.drawable.gym_bg),
            contentDescription = "Gym Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay for readability and moody gym lighting feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x40000000),
                            Color(0x22000000),
                            Color(0xBB000000),
                            Color(0xFA000000)
                        ),
                        startY = 0f
                    )
                )
        )

        // Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkipClick) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Section: Badge, Title, Subtitle, Page Indicator, Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // "CONNECT WITH YOUR TRAINER" Pill Tag
                Surface(
                    shape = CircleShape,
                    color = Color(0x452B2418),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44E0C070)),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // Stacked Trainer Avatars
                        Box(
                            modifier = Modifier
                                .width(54.dp)
                                .height(26.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            AvatarImage(
                                resId = R.drawable.trainer_1,
                                modifier = Modifier.offset(x = 0.dp)
                            )
                            AvatarImage(
                                resId = R.drawable.trainer_2,
                                modifier = Modifier.offset(x = 14.dp)
                            )
                            AvatarImage(
                                resId = R.drawable.avatar_3,
                                modifier = Modifier.offset(x = 28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "CONNECT WITH YOUR TRAINER",
                            color = Color(0xFFF3D286),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Headline Title
                Text(
                    text = "ELEVATE\nYOUR GYM\nJOURNEY",
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 46.sp,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Subtitle Description
                Text(
                    text = "Access customized workout plans, track progress, and stay directly connected with your personal trainer and gym.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Page Indicator Caps
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD580))
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = onContinueClick,
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarImage(
    resId: Int,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(1.5.dp, Color(0xFFF3D286), CircleShape)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    GymSuitAppTheme {
        OnboardingScreen()
    }
}

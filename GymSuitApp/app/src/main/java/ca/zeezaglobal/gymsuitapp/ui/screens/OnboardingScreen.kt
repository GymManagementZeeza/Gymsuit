package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.ui.theme.GymSuitAppTheme

data class OnboardingCarouselItem(
    val imageRes: Int,
    val tag: String,
    val headline: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSkipClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    val carouselItems = remember {
        listOf(
            OnboardingCarouselItem(
                imageRes = R.drawable.fitness_gym,
                tag = "Strength • Training",
                headline = "Elevate Your Gym Journey",
                description = "Customized progressive overload workout plans built to sculpt and strengthen your body."
            ),
            OnboardingCarouselItem(
                imageRes = R.drawable.fitness_workout,
                tag = "Cardio • Endurance",
                headline = "Push Beyond Your Limits",
                description = "High-energy training programs and real-time biometric stats to boost stamina and burn calories."
            ),
            OnboardingCarouselItem(
                imageRes = R.drawable.fitness_yoga,
                tag = "Mobility • Balance",
                headline = "Restore & Recover Smarter",
                description = "Guided recovery sessions, flexibility routines, and mindful wellness tracking for longevity."
            ),
            OnboardingCarouselItem(
                imageRes = R.drawable.fitness_cardio,
                tag = "HIIT • Power",
                headline = "High Intensity Conditioning",
                description = "Ignite your metabolism and maximize your athletic output with high-intensity circuit training."
            ),
            OnboardingCarouselItem(
                imageRes = R.drawable.fitness_stretching,
                tag = "Flexibility • Mind",
                headline = "Stay Centered & Flexible",
                description = "Targeted stretching routines to prevent injuries, release tension, and prime your body."
            )
        )
    }

    val carouselState = rememberCarouselState { carouselItems.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: App Tag & Skip Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = "GYMSUIT",
                        color = colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                TextButton(onClick = onSkipClick) {
                    Text(
                        text = "Skip",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Center: Official Material 3 Multi-Browse Carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = 300.dp,
                    itemSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                ) { index ->
                    val item = carouselItems[index]

                    Surface(
                        shape = RoundedCornerShape(32.dp), // M3 extra-large shape
                        color = colorScheme.surfaceContainerHigh,
                        shadowElevation = 4.dp,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = item.imageRes),
                                contentDescription = item.headline,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Scrim gradient for readability
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.55f),
                                                Color.Black.copy(alpha = 0.92f)
                                            )
                                        )
                                    )
                            )

                            // Content overlay: Tag, Headline, and Description
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 22.dp, vertical = 26.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Text(
                                        text = item.tag,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = item.headline,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 26.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.description,
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Section: Get Started Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onContinueClick,
                    shape = RoundedCornerShape(24.dp), // M3 expressive pill
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    GymSuitAppTheme {
        OnboardingScreen()
    }
}

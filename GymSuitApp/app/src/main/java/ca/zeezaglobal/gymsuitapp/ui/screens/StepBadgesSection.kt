package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.collectAsState
import ca.zeezaglobal.gymsuitapp.data.StepBadgeStore
import ca.zeezaglobal.gymsuitapp.data.model.StepBadge
import ca.zeezaglobal.gymsuitapp.data.model.StepBadgeConfig
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

/**
 * "Step Badges" section for the Home screen: one card per milestone
 * threshold (5k / 10k / 20k). Earned badges show in full color with the
 * date earned; unearned ones stay greyed out with a lock so users see
 * what's next.
 */
@Composable
fun StepBadgesSection() {
    val context = LocalContext.current
    val badges by StepBadgeStore.badges.collectAsState()

    LaunchedEffect(Unit) {
        StepBadgeStore.refresh(context)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Step Badges",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = "Hit a daily step milestone to unlock",
            fontSize = 13.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(StepBadgeConfig.milestones) { (threshold, name) ->
                val earned = badges
                    .filter { it.threshold == threshold }
                    .maxByOrNull { it.awardedAt ?: Instant.EPOCH }
                BadgeCard(threshold = threshold, name = name, badge = earned)
            }
        }
    }
}

@Composable
private fun BadgeCard(threshold: Int, name: String, badge: StepBadge?) {
    val earned = badge != null
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (earned) Color(0xFFFFF8E1) else Color(0xFFF3F4F6)
        ),
        border = if (earned) BorderStroke(1.5.dp, Color(0xFFFFB300)) else null,
        modifier = Modifier.width(152.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (earned) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                contentDescription = name,
                tint = if (earned) Color(0xFFFFB300) else Color(0xFF9CA3AF),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (earned) Color(0xFF111827) else Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (earned) {
                    "%,d steps".format(badge.stepsAchieved)
                } else {
                    "%,d steps".format(threshold)
                },
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )
            if (earned && badge.date != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = badge.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

/**
 * Watches the badge list and shows the celebration overlay exactly once per
 * badge (seen ids are persisted in SharedPreferences). If several badges are
 * new at once, the newest is celebrated first; the rest follow on the next
 * refresh.
 */
@Composable
fun BadgeCelebrationHost() {
    val context = LocalContext.current
    val badges by StepBadgeStore.badges.collectAsState()
    var dismissedId by remember { mutableLongStateOf(-1L) }

    val newestUnseen = badges
        .filter { !StepBadgeStore.isSeen(context, it.id) && it.id != dismissedId }
        .maxByOrNull { it.awardedAt ?: Instant.EPOCH }

    if (newestUnseen != null) {
        BadgeCelebrationOverlay(badge = newestUnseen) {
            StepBadgeStore.markSeen(context, newestUnseen.id)
            dismissedId = newestUnseen.id
        }
    }
}

@Composable
private fun BadgeCelebrationOverlay(badge: StepBadge, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(badge.id) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            ConfettiBurst(modifier = Modifier.matchParentSize())
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = badge.badgeName,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Badge Unlocked!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = badge.badgeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "%,d steps in a day".format(badge.threshold),
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "You walked %,d steps".format(badge.stepsAchieved),
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        Text(text = "Keep it up", color = Color.White)
                    }
                }
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val phaseOffset: Float,
    val color: Color,
    val sizeDp: Float,
    val drift: Float,
    val cycles: Float,
    val rotationTurns: Float
)

private val confettiColors = listOf(
    Color(0xFFFFC72C), Color(0xFFFF6384), Color(0xFF48BB78),
    Color(0xFF4299E1), Color(0xFFED8936), Color(0xFF9F7AEA),
    Color(0xFF2DD4BF), Color(0xFFFBB6CE)
)

/** Canvas-drawn confetti burst — no new dependencies. */
@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat(),
                phaseOffset = Random.nextFloat(),
                color = confettiColors.random(),
                sizeDp = Random.nextFloat() * 7f + 5f,
                drift = Random.nextFloat() * 0.2f - 0.1f,
                cycles = Random.nextFloat() * 2f + 1f,
                rotationTurns = Random.nextFloat() * 2f - 1f
            )
        }
    }
    val progress by rememberInfiniteTransition(label = "confetti")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing)),
            label = "confettiProgress"
        )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            val t = (progress + p.phaseOffset).mod(1f)
            val y = t * (h + 60f) - 30f
            val x = ((p.x + p.drift * sin(t * Math.PI * 2 * p.cycles).toFloat()) % 1f) * w
            val alpha = 1f - t * t
            val s = p.sizeDp.dp.toPx()
            rotate(degrees = p.rotationTurns * t * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - s / 2f, y - s / 4f),
                    size = Size(s, s / 2f)
                )
            }
        }
    }
}

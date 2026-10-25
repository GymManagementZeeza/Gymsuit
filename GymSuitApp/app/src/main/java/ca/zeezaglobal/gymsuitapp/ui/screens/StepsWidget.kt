package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/** Daily step target shown on the card. No step goal is stored anywhere in
 *  the app yet, so this matches the iOS card's "Goal: 10,000 steps" and
 *  the floating bubble's ring target. */
const val DAILY_STEPS_GOAL = 10_000

/**
 * Home card tracking daily steps for the selected day. Mirrors the iOS
 * Steps card (count + 10,000-step goal) and the Sleep/Calories widgets'
 * card style. Tapping opens [StepsDetailScreen] with the 7-day trend.
 */
@Composable
fun StepsWidget(
    selectedDate: LocalDate = LocalDate.now(),
    syncTrigger: Int = 0,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var steps by remember { mutableLongStateOf(0L) }

    LaunchedEffect(selectedDate, syncTrigger) {
        steps = try {
            healthConnectManager.readProgressTotals(selectedDate, selectedDate).steps
        } catch (e: Exception) {
            0L
        }
    }

    val progress = (steps.toFloat() / DAILY_STEPS_GOAL).coerceIn(0f, 1f)
    val formatted = NumberFormat.getNumberInstance(Locale.US).format(steps)
    val goalFormatted = NumberFormat.getNumberInstance(Locale.US).format(DAILY_STEPS_GOAL)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.DirectionsWalk,
                        contentDescription = "Steps",
                        tint = Color(0xFF0062FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Steps",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (steps > 0) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatted,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "steps",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF0062FF),
                    trackColor = Color(0xFFE5E7EB)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(progress * 100).toInt()}% of $goalFormatted goal",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            } else {
                Text(
                    text = "No data yet",
                    fontSize = 15.sp,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Goal: $goalFormatted steps",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

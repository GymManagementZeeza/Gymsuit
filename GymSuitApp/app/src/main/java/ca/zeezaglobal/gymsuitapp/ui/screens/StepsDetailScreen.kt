package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Daily step trends, mirroring the iOS StepsDetailView: the 7 days ending
 * on [date] as a bar chart, plus 7-day total, daily average, and a per-day
 * list. Steps come from Health Connect (same source as the home card,
 * including the on-device pedometer fallback for today).
 */
@Composable
fun StepsDetailScreen(
    date: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var dailySteps by remember { mutableStateOf<List<Pair<LocalDate, Long>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(date) {
        isLoading = true
        val days = (6 downTo 0).map { date.minusDays(it.toLong()) }
        dailySteps = days.map { day ->
            day to try {
                healthConnectManager.readProgressTotals(day, day).steps
            } catch (e: Exception) {
                0L
            }
        }
        isLoading = false
    }

    val total = dailySteps.sumOf { it.second }
    val average = if (dailySteps.isNotEmpty()) total / dailySteps.size else 0L
    val maxSteps = dailySteps.maxOfOrNull { it.second } ?: 0L
    val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    val fullFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        containerColor = Color(0xFFF3F2F8),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF111827)
                    )
                }
                Text(
                    text = "Steps",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0062FF))
                }
                return@Column
            }

            // Summary: 7-day total + daily average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStat(
                    label = "7-day total",
                    value = numberFormat.format(total),
                    modifier = Modifier.weight(1f)
                )
                SummaryStat(
                    label = "Daily average",
                    value = numberFormat.format(average),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar chart card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last 7 days",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailySteps.forEach { (day, count) ->
                            val fraction = if (maxSteps > 0) count.toFloat() / maxSteps else 0f
                            val barHeight = if (count > 0) {
                                (140f * fraction).dp.coerceAtLeast(4.dp)
                            } else {
                                0.dp
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = if (count > 0) compactSteps(count) else "",
                                    fontSize = 10.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (day == date) Color(0xFF0062FF) else Color(0xFF93C5FD)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day.format(dayFormatter),
                                    fontSize = 11.sp,
                                    color = if (day == date) Color(0xFF111827) else Color(0xFF6B7280),
                                    fontWeight = if (day == date) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Per-day list
            dailySteps.reversed().forEach { (day, count) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = day.format(fullFormatter),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${numberFormat.format(count)} steps",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "${(count.toFloat() / DAILY_STEPS_GOAL * 100).toInt()}% of goal",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
    }
}

private fun compactSteps(steps: Long): String =
    if (steps >= 1000) {
        String.format(Locale.US, "%.1fk", steps / 1000f)
    } else {
        steps.toString()
    }

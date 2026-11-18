package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import java.time.LocalDate
import java.util.Locale

/**
 * The vitals shown on the home card, in the same order as the reference
 * card: heart rate, respiratory rate, body temperature, blood oxygen, sleep.
 * Values are Doubles so one strip can plot them all (sleep in minutes).
 */
private enum class VitalsKind(
    val label: String,
    val icon: ImageVector
) {
    HEART_RATE("Heart rate", Icons.Outlined.Favorite),
    RESPIRATORY("Respiratory rate", Icons.Outlined.Air),
    TEMPERATURE("Body temperature", Icons.Outlined.Thermostat),
    OXYGEN("Blood oxygen", Icons.Outlined.WaterDrop),
    SLEEP("Sleep", Icons.Outlined.Bed)
}

/** Big value + unit for the selected vital, e.g. ("72", "bpm") or ("7h 14m", ""). */
private fun formatVitalBig(kind: VitalsKind, value: Double?): Pair<String, String> {
    if (value == null) return "—" to ""
    return when (kind) {
        VitalsKind.HEART_RATE -> "${value.toInt()}" to "bpm"
        VitalsKind.RESPIRATORY -> "${value.toInt()}" to "breaths/min"
        VitalsKind.TEMPERATURE -> String.format(Locale.US, "%.1f", value) to "°C"
        VitalsKind.OXYGEN -> "${value.toInt()}" to "%"
        VitalsKind.SLEEP -> {
            val mins = value.toLong()
            "${mins / 60}h ${mins % 60}m" to ""
        }
    }
}

/** Compact value under an icon, e.g. "72", "36.6°", "98%", "7h 14m". */
private fun formatVitalShort(kind: VitalsKind, value: Double?): String {
    if (value == null) return "—"
    return when (kind) {
        VitalsKind.HEART_RATE -> "${value.toInt()}"
        VitalsKind.RESPIRATORY -> "${value.toInt()}"
        VitalsKind.TEMPERATURE -> String.format(Locale.US, "%.1f°", value)
        VitalsKind.OXYGEN -> "${value.toInt()}%"
        VitalsKind.SLEEP -> {
            val mins = value.toLong()
            "${mins / 60}h ${mins % 60}m"
        }
    }
}

/**
 * Home card for overall vitals, sitting in the left column beside the Steps
 * card. The icon row picks the vital; the strip above it plots that vital's
 * last 5 days (rightmost dot = the selected day) and the big number shows
 * the selected day's value. A vital with no Health Connect data shows "—",
 * never a made-up number. Matches the Steps/Sleep widgets' card style.
 */
@Composable
fun VitalsWidget(
    selectedDate: LocalDate = LocalDate.now(),
    syncTrigger: Int = 0
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var selectedVital by remember { mutableStateOf(VitalsKind.HEART_RATE) }
    var todayValues by remember { mutableStateOf<Map<VitalsKind, Double?>>(emptyMap()) }
    var stripValues by remember { mutableStateOf<List<Double?>>(emptyList()) }

    LaunchedEffect(selectedDate, syncTrigger) {
        val heartRate = try {
            healthConnectManager.tryReadHeartRateDataForDate(selectedDate)
                ?.takeIf { it.hasData }?.latestBpm?.toDouble()
        } catch (e: Exception) {
            null
        }
        val sleepMinutes = try {
            healthConnectManager.readDetailedSleepForDate(selectedDate)
                .takeIf { it.hasData }?.totalSleepMinutes?.toDouble()
        } catch (e: Exception) {
            null
        }
        val temperature = try {
            healthConnectManager.readBodyTemperatureForDate(selectedDate)
        } catch (e: Exception) {
            null
        }
        val oxygen = try {
            healthConnectManager.readOxygenSaturationForDate(selectedDate)
        } catch (e: Exception) {
            null
        }
        val respiratory = try {
            healthConnectManager.readRespiratoryRateForDate(selectedDate)
        } catch (e: Exception) {
            null
        }
        todayValues = mapOf(
            VitalsKind.HEART_RATE to heartRate,
            VitalsKind.RESPIRATORY to respiratory,
            VitalsKind.TEMPERATURE to temperature,
            VitalsKind.OXYGEN to oxygen,
            VitalsKind.SLEEP to sleepMinutes
        )
    }

    LaunchedEffect(selectedDate, syncTrigger, selectedVital) {
        val days = (4 downTo 0).map { selectedDate.minusDays(it.toLong()) }
        stripValues = days.map { day ->
            try {
                when (selectedVital) {
                    VitalsKind.HEART_RATE ->
                        healthConnectManager.tryReadHeartRateDataForDate(day)
                            ?.takeIf { it.hasData }?.latestBpm?.toDouble()
                    VitalsKind.SLEEP ->
                        healthConnectManager.readDetailedSleepForDate(day)
                            .takeIf { it.hasData }?.totalSleepMinutes?.toDouble()
                    VitalsKind.TEMPERATURE ->
                        healthConnectManager.readBodyTemperatureForDate(day)
                    VitalsKind.OXYGEN ->
                        healthConnectManager.readOxygenSaturationForDate(day)
                    VitalsKind.RESPIRATORY ->
                        healthConnectManager.readRespiratoryRateForDate(day)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val (bigValue, bigUnit) = formatVitalBig(selectedVital, todayValues[selectedVital])

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
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = "Vitals",
                    tint = Color(0xFF0062FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vitals",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Last 5 days",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = bigValue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                if (bigUnit.isNotEmpty()) {
                    Text(
                        text = bigUnit,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Text(
                text = selectedVital.label,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5-day trend strip: one dot per day, higher dot = higher value.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                val count = stripValues.size
                if (count == 0) return@Canvas
                val present = stripValues.filterNotNull()
                val min = present.minOrNull() ?: 0.0
                val max = present.maxOrNull() ?: 0.0
                val range = max - min
                val leftPad = size.width * 0.09f
                val usableWidth = size.width - leftPad * 2
                val topPad = 14.dp.toPx()
                val usableHeight = size.height - topPad * 2
                stripValues.forEachIndexed { index, value ->
                    val x = leftPad + usableWidth * (if (count > 1) index.toFloat() / (count - 1) else 0.5f)
                    val fraction =
                        if (value == null || range <= 0.0) 0.5f
                        else ((value - min) / range).toFloat()
                    val y = topPad + usableHeight * (1f - fraction)
                    val isLast = index == count - 1
                    val radius = (if (isLast) 9.dp else 7.dp).toPx()
                    val dotColor =
                        if (value == null) Color(0xFFCBD5E1) else Color(0xFF0062FF)
                    drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VitalsKind.entries.forEach { kind ->
                    val selected = kind == selectedVital
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedVital = kind }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = kind.icon,
                            contentDescription = kind.label,
                            tint = if (selected) Color(0xFF0062FF) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatVitalShort(kind, todayValues[kind]),
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color(0xFF111827) else Color(0xFF6B7280)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color(0xFF0062FF) else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

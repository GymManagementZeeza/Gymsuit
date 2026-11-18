package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val vitalsDateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val vitalsDayLetterFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

/**
 * Full vitals detail page, opened from the home Vitals card. Same date
 * toggle as the Sleep/Calories detail pages (previous day, tap the date for
 * a calendar picker, next day — no future dates). One card per vital with
 * the selected day's value, a 7-day dot trend ending on that day, and the
 * 7-day average / min / max. A vital with no Health Connect data shows
 * "No data yet", never a made-up number.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsDetailScreen(
    date: LocalDate = LocalDate.now(),
    onBackClick: () -> Unit = {},
    onDateChange: (LocalDate) -> Unit = {}
) {
    val context = LocalContext.current
    val healthConnectManager = remember { HealthConnectManager(context) }
    var weekValues by remember { mutableStateOf<Map<VitalsKind, List<Double?>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val isToday = date >= today
    val dateLabel = if (date == today) "Today" else date.format(vitalsDateFmt)
    val days = remember(date) { (6 downTo 0).map { date.minusDays(it.toLong()) } }

    LaunchedEffect(date) {
        isLoading = true
        weekValues = VitalsKind.entries.associateWith { kind ->
            days.map { day -> healthConnectManager.readVitalForDate(kind, day) }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }
                Text(
                    text = "Vitals",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Date toggle — same pattern as the Weight/Sleep detail pages.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onDateChange(date.minusDays(1)) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = "Previous day",
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isToday) { onDateChange(date.plusDays(1)) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Next day",
                            tint = if (isToday) Color(0xFFCBD5E1) else Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                Text(
                    text = "Loading…",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            } else {
                VitalsKind.entries.forEach { kind ->
                    VitalDetailCard(
                        kind = kind,
                        days = days,
                        values = weekValues[kind] ?: List(7) { null }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.of("UTC")).toLocalDate() <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun VitalDetailCard(
    kind: VitalsKind,
    days: List<LocalDate>,
    values: List<Double?>
) {
    val todayValue = values.lastOrNull()
    val present = values.filterNotNull()
    val (bigValue, bigUnit) = formatVitalBig(kind, todayValue)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
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
                    imageVector = kind.icon,
                    contentDescription = kind.label,
                    tint = Color(0xFF0062FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = kind.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = bigValue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    softWrap = false
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

            if (present.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                VitalTrendStrip(values)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    days.forEach { day ->
                        Text(
                            text = day.format(vitalsDayLetterFmt).take(1),
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Avg ${formatVitalShort(kind, present.average())}  ·  " +
                        "Min ${formatVitalShort(kind, present.min())}  ·  " +
                        "Max ${formatVitalShort(kind, present.max())}",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "Last 7 days",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No data yet",
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

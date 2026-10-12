package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeMetric
import ca.zeezaglobal.gymsuitapp.data.model.ParticipantDailySeries
import ca.zeezaglobal.gymsuitapp.data.model.formatMetricValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One distinct color per participant (challenges cap at 10 members). */
private val ChartPalette = listOf(
    Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFDC2626), Color(0xFFD97706),
    Color(0xFF7C3AED), Color(0xFF0891B2), Color(0xFFDB2777), Color(0xFF65A30D),
    Color(0xFF4F46E5), Color(0xFF0D9488)
)

private val IsoDateFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

/** Days from the challenge start through today (or the end date, whichever is first). */
internal fun buildChallengeDayList(start: String, end: String): List<LocalDate> {
    return try {
        val s = LocalDate.parse(start, IsoDateFormat)
        val e = LocalDate.parse(end, IsoDateFormat)
        val last = if (LocalDate.now().isBefore(e)) LocalDate.now() else e
        val days = mutableListOf<LocalDate>()
        var d = s
        var guard = 0
        while (!d.isAfter(last) && guard < 366) {
            days.add(d)
            d = d.plusDays(1)
            guard++
        }
        days
    } catch (e: Exception) {
        emptyList()
    }
}

/** Cumulative value of a series on a given day key (yyyy-MM-dd); missing days count as 0. */
internal fun seriesValueOn(series: ParticipantDailySeries, dayKey: String): Double =
    series.entries.firstOrNull { it.date == dayKey }?.value ?: 0.0

private fun axisLabelPaint() = android.graphics.Paint().apply {
    color = android.graphics.Color.parseColor("#9CA3AF")
    textSize = 30f
    isAntiAlias = true
}

/**
 * Line chart of every participant's cumulative progress over the challenge days.
 * Includes a legend with each participant's running total.
 */
@Composable
internal fun ProgressLineChart(
    days: List<LocalDate>,
    series: List<ParticipantDailySeries>,
    metric: ChallengeMetric,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty() || series.isEmpty()) return

    val cumulative: List<List<Double>> = remember(days, series) {
        series.map { s ->
            var running = 0.0
            days.map { day ->
                running += seriesValueOn(s, day.format(IsoDateFormat))
                running
            }
        }
    }
    val maxY = (cumulative.flatten().maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val paint = remember { axisLabelPaint() }
    val dayLabelFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }

    Canvas(modifier = modifier) {
        val leftPad = 110f
        val rightPad = 16f
        val topPad = 16f
        val bottomPad = 56f
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0 || h <= 0) return@Canvas

        fun x(i: Int): Float =
            if (days.size <= 1) leftPad + w / 2f
            else leftPad + (i.toFloat() / (days.size - 1)) * w
        fun y(v: Double): Float = topPad + h - (v / maxY).toFloat() * h

        // Horizontal gridlines with y labels.
        listOf(0.0, maxY / 2.0, maxY).forEach { gv ->
            val yy = y(gv)
            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(leftPad, yy),
                end = Offset(leftPad + w, yy),
                strokeWidth = 2f
            )
            drawContext.canvas.nativeCanvas.drawText(
                formatAxisValue(metric, gv), 4f, yy + 10f, paint
            )
        }

        // X labels: first, middle, last.
        listOf(0, days.size / 2, days.size - 1).distinct().forEach { i ->
            drawContext.canvas.nativeCanvas.drawText(
                days[i].format(dayLabelFmt), x(i) - 36f, size.height - 14f, paint
            )
        }

        cumulative.forEachIndexed { sIdx, points ->
            val color = ChartPalette[sIdx % ChartPalette.size]
            val path = Path()
            points.forEachIndexed { i, v ->
                if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
            }
            drawPath(path, color, style = Stroke(width = 6f))
            points.forEachIndexed { i, v ->
                drawCircle(color, radius = 7f, center = Offset(x(i), y(v)))
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        series.forEachIndexed { sIdx, s ->
            val total = cumulative.getOrNull(sIdx)?.lastOrNull() ?: 0.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ChartPalette[sIdx % ChartPalette.size])
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = s.displayName,
                    fontSize = 13.sp,
                    color = Color(0xFF374151),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatMetricValue(metric, total),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            }
        }
    }
}

/** Bar chart of one participant's per-day values. */
@Composable
internal fun DailyBarChart(
    days: List<LocalDate>,
    values: List<Double>,
    metric: ChallengeMetric,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val maxV = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val paint = remember { axisLabelPaint() }
    val dayLabelFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val barColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val leftPad = 12f
        val rightPad = 12f
        val topPad = 34f
        val bottomPad = 56f
        val w = size.width - leftPad - rightPad
        val h = size.height - topPad - bottomPad
        if (w <= 0 || h <= 0) return@Canvas

        val slot = w / days.size
        val barW = (slot * 0.62f).coerceAtLeast(4f)
        days.forEachIndexed { i, _ ->
            val v = values.getOrElse(i) { 0.0 }
            val barH = (v / maxV).toFloat() * h
            if (barH > 1f) {
                val cx = leftPad + slot * i + slot / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(cx - barW / 2f, topPad + h - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }

        drawContext.canvas.nativeCanvas.drawText(
            formatMetricValue(metric, maxV), leftPad, topPad - 10f, paint
        )

        val step = when {
            days.size <= 7 -> 1
            days.size <= 14 -> 2
            days.size <= 31 -> 5
            else -> 10
        }
        days.forEachIndexed { i, day ->
            if (i % step == 0 || i == days.size - 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    day.format(dayLabelFmt),
                    (leftPad + slot * i).coerceAtMost(size.width - 90f),
                    size.height - 14f,
                    paint
                )
            }
        }
    }
}

private fun formatAxisValue(metric: ChallengeMetric, value: Double): String = when (metric) {
    ChallengeMetric.STEPS -> if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
    ChallengeMetric.WORKOUTS -> value.toInt().toString()
    ChallengeMetric.CALORIES -> if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
    ChallengeMetric.DISTANCE_KM -> "%.0f".format(value)
}

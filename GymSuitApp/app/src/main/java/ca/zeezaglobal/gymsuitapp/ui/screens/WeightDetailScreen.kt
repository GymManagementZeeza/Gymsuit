package ca.zeezaglobal.gymsuitapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.WeightRecord
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.WeightEntry
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private enum class WeightRange(val label: String) { D("D"), W("W"), M("M"), M3("3M"), Y("Y") }

private data class WeightPeriod(val start: LocalDate, val endExclusive: LocalDate, val title: String)

private data class WeightBucket(val label: String, val avgKg: Double)

private fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else String.format(Locale.US, "%.1f", kg)

private fun fmt(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())

private fun periodFor(range: WeightRange, offset: Int, today: LocalDate): WeightPeriod = when (range) {
    WeightRange.D -> {
        val d = today.minusDays(offset.toLong())
        WeightPeriod(d, d.plusDays(1), if (offset == 0) "Today" else d.format(fmt("EEE, MMM d")))
    }
    WeightRange.W -> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
        val end = monday.plusDays(7)
        WeightPeriod(monday, end, "${monday.format(fmt("MMM d"))} – ${end.minusDays(1).format(fmt("MMM d"))}")
    }
    WeightRange.M -> {
        val ym = YearMonth.from(today).minusMonths(offset.toLong())
        WeightPeriod(ym.atDay(1), ym.plusMonths(1).atDay(1), ym.format(fmt("MMMM yyyy")))
    }
    WeightRange.M3 -> {
        val last = YearMonth.from(today).minusMonths(offset * 3L)
        val first = last.minusMonths(2)
        WeightPeriod(
            first.atDay(1), last.plusMonths(1).atDay(1),
            "${first.format(fmt("MMM"))} – ${last.format(fmt("MMM yyyy"))}"
        )
    }
    WeightRange.Y -> {
        val year = today.year - offset
        WeightPeriod(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1), year.toString())
    }
}

private fun bucketsFor(range: WeightRange, entries: List<WeightEntry>, zone: ZoneId): List<WeightBucket> {
    fun date(e: WeightEntry) = e.time.atZone(zone).toLocalDate()
    return when (range) {
        WeightRange.D -> entries.map { WeightBucket(it.time.atZone(zone).format(fmt("h:mm a")), it.kg) }
        WeightRange.W -> entries.groupBy(::date).map { (d, list) ->
            WeightBucket(d.format(fmt("EEE, MMM d")), list.map { it.kg }.average())
        }
        WeightRange.M, WeightRange.M3 -> entries.groupBy {
            date(it).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.map { (monday, list) ->
            WeightBucket(
                "${monday.format(fmt("MMM d"))} – ${monday.plusDays(6).format(fmt("MMM d"))}",
                list.map { it.kg }.average()
            )
        }
        WeightRange.Y -> entries.groupBy { YearMonth.from(date(it)) }.map { (ym, list) ->
            WeightBucket(ym.format(fmt("MMMM")), list.map { it.kg }.average())
        }
    }.reversed()
}

/**
 * Connected button group: the selected option morphs into a full pill while
 * the others keep small inner corners and rounded outer ends.
 */
@Composable
private fun WeightRangeSelector(selected: WeightRange, onSelected: (WeightRange) -> Unit) {
    val options = WeightRange.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            val outer = 28.dp
            val inner = 8.dp
            val spec = tween<androidx.compose.ui.unit.Dp>(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            val startRadius by animateDpAsState(if (isSelected || index == 0) outer else inner, spec, label = "startRadius")
            val endRadius by animateDpAsState(if (isSelected || index == options.lastIndex) outer else inner, spec, label = "endRadius")
            val shape = RoundedCornerShape(
                topStart = startRadius, bottomStart = startRadius,
                topEnd = endRadius, bottomEnd = endRadius
            )
            Button(
                onClick = { onSelected(option) },
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 0.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(option.label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightDetailScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val manager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val isAvailable = remember { healthConnectAvailable(manager) }
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }

    var history by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    var hasReadPermission by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var range by remember { mutableStateOf(WeightRange.M3) }
    var offset by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Pair<Double, Instant>?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    val weightPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class)
        )
    }

    suspend fun save(kg: Double, time: Instant) {
        if (manager.insertWeight(kg, time)) {
            snackbar.showSnackbar("Weight saved: ${formatKg(kg)} kg")
            reloadKey++
        } else {
            snackbar.showSnackbar("Could not save weight. Check Health Connect permissions.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val pending = pendingSave
        pendingSave = null
        if (pending != null) {
            scope.launch {
                if (granted.contains(HealthPermission.getWritePermission(WeightRecord::class))) {
                    save(pending.first, pending.second)
                } else {
                    snackbar.showSnackbar("Allow weight write access in Health Connect to add weight.")
                }
            }
        }
        reloadKey++
    }

    LaunchedEffect(reloadKey) {
        isLoading = true
        if (isAvailable) {
            hasReadPermission = manager.hasWeightPermission()
            history = if (hasReadPermission) manager.readWeightHistory() else emptyList()
        }
        isLoading = false
    }

    val period = remember(range, offset) { periodFor(range, offset, today) }
    val inPeriod = remember(history, period) {
        history.filter {
            val d = it.time.atZone(zone).toLocalDate()
            !d.isBefore(period.start) && d.isBefore(period.endExclusive)
        }
    }
    val average = inPeriod.takeIf { it.isNotEmpty() }?.map { it.kg }?.average()
    val change = if (inPeriod.size >= 2) inPeriod.last().kg - inPeriod.first().kg else null
    val buckets = remember(inPeriod, range) { bucketsFor(range, inPeriod, zone) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Weight") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add weight")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                WeightRangeSelector(
                    selected = range,
                    onSelected = {
                        range = it
                        offset = 0
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = period.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(onClick = { offset++ }) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous period")
                    }
                    FilledTonalIconButton(onClick = { offset-- }, enabled = offset > 0) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next period")
                    }
                    FilledTonalIconButton(onClick = { offset = 0 }, enabled = offset > 0) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Back to current period")
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = average?.let { formatKg(Math.round(it * 10) / 10.0) } ?: "--",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (range == WeightRange.D) "kg" else "kg (avg)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (change != null && change != 0.0) {
                        val abs = String.format(Locale.US, "%.1f", Math.abs(change))
                        Text(
                            text = "$abs kg ${if (change > 0) "gained" else "lost"} over period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                when {
                    !isAvailable -> InfoText("Health Connect is not available on this device.")
                    isLoading -> InfoText("Loading…")
                    !hasReadPermission -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoText("Allow Health Connect to share your weight to see your data.")
                        Button(onClick = { permissionLauncher.launch(weightPermissions) }) { Text("Allow access") }
                    }
                    inPeriod.isEmpty() -> InfoText("No weight entries in this period.")
                    else -> WeightChart(entries = inPeriod, range = range, period = period, zone = zone)
                }
            }

            if (buckets.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(period.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (range == WeightRange.D) "Weight" else "Weight (avg)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(buckets) { bucket ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bucket.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${formatKg(Math.round(bucket.avgKg * 10) / 10.0)} kg",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddWeightSheet(
            initialKg = history.lastOrNull()?.kg,
            onDismiss = { showAddSheet = false },
            onSave = { kg, date ->
                showAddSheet = false
                val time = if (date == today) Instant.now()
                else date.atTime(12, 0).atZone(zone).toInstant()
                scope.launch {
                    if (manager.hasWeightWritePermission()) {
                        save(kg, time)
                    } else {
                        pendingSave = kg to time
                        permissionLauncher.launch(weightPermissions)
                    }
                }
            }
        )
    }
}

private fun healthConnectAvailable(manager: HealthConnectManager) = manager.isAvailable()

@Composable
private fun InfoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWeightSheet(
    initialKg: Double?,
    onDismiss: () -> Unit,
    onSave: (Double, LocalDate) -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now() }
    var input by remember { mutableStateOf(initialKg?.let(::formatKg) ?: "") }
    var date by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add weight", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = input,
                onValueChange = { v -> input = v.filter { it.isDigit() || it == '.' }.take(6) },
                label = { Text("Weight (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(date.format(fmt("MMM d, yyyy")))
            }

            val kg = input.toDoubleOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = { kg?.let { onSave(it, date) } },
                    enabled = kg != null && kg in 20.0..500.0
                ) { Text("Save") }
            }
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
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun WeightChart(
    entries: List<WeightEntry>,
    range: WeightRange,
    period: WeightPeriod,
    zone: ZoneId
) {
    val minKg = entries.minOf { it.kg }
    val maxKg = entries.maxOf { it.kg }
    val lo = Math.floor(minKg - 1).toInt()
    var hi = Math.ceil(maxKg + 1).toInt()
    if ((hi - lo) % 3 != 0) hi += 3 - (hi - lo) % 3 // keep the 4 axis labels on whole numbers
    val step = (hi - lo) / 3
    val axisLabels = (0..3).map { hi - step * it }

    val startMs = period.start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMs = period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    val span = (endMs - startMs).coerceAtLeast(1L)

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface

    Column {
        Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(modifier = Modifier.weight(1f).fillMaxSize()) {
                val inset = 6.dp.toPx()
                val w = size.width - inset * 2
                val h = size.height - inset * 2
                repeat(4) { i ->
                    val y = inset + h * i / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val pts = entries.map {
                    val x = (it.time.toEpochMilli() - startMs).toFloat() / span * w
                    val y = h - ((it.kg - lo) / (hi - lo).toFloat()) * h
                    Offset(inset + x, inset + y.toFloat())
                }
                if (pts.size > 1) {
                    val path = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path, lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                pts.forEach {
                    drawCircle(surface, radius = 5.dp.toPx(), center = it)
                    drawCircle(lineColor, radius = 3.5.dp.toPx(), center = it)
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                axisLabels.forEach {
                    Text(it.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        val labelFormat = when (range) {
            WeightRange.D -> fmt("h a")
            WeightRange.W -> fmt("EEE")
            WeightRange.M -> fmt("MMM d")
            WeightRange.M3, WeightRange.Y -> fmt("MMM")
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (0..3).forEach { i ->
                val ms = startMs + span * i / 4
                Text(
                    Instant.ofEpochMilli(ms).atZone(zone).format(labelFormat),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package ca.zeezaglobal.gymsuitapp.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeDetail
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeMetric
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeStatus
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeSummary
import ca.zeezaglobal.gymsuitapp.data.model.LeaderboardEntry
import ca.zeezaglobal.gymsuitapp.data.model.formatMetricValue
import ca.zeezaglobal.gymsuitapp.data.remote.ChallengeApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface ChallengeRoute {
    data object List : ChallengeRoute
    data object Create : ChallengeRoute
    data class Detail(val id: String) : ChallengeRoute
}

private val ApiDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Composable
fun ChallengeScreen() {
    var route by remember { mutableStateOf<ChallengeRoute>(ChallengeRoute.List) }
    when (val r = route) {
        is ChallengeRoute.List -> ChallengeListContent(
            onCreate = { route = ChallengeRoute.Create },
            onOpen = { id -> route = ChallengeRoute.Detail(id) }
        )
        is ChallengeRoute.Create -> CreateChallengeContent(
            onBack = { route = ChallengeRoute.List },
            onCreated = { id -> route = ChallengeRoute.Detail(id) }
        )
        is ChallengeRoute.Detail -> ChallengeDetailContent(
            challengeId = r.id,
            onBack = { route = ChallengeRoute.List }
        )
    }
}

// ---------------------------------------------------------------------------
// List
// ---------------------------------------------------------------------------

@Composable
private fun ChallengeListContent(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val api = remember { ChallengeApi(context) }
    val scope = rememberCoroutineScope()

    var challenges by remember { mutableStateOf<List<ChallengeSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var joinCode by remember { mutableStateOf("") }
    var joinLoading by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            error = null
            api.listChallenges()
                .onSuccess { challenges = it }
                .onFailure { error = it.message ?: "Couldn't load challenges" }
            isLoading = false
        }
    }

    fun join() {
        if (joinCode.isBlank()) {
            joinError = "Enter the invite code"
            return
        }
        scope.launch {
            joinLoading = true
            joinError = null
            api.joinByCode(joinCode)
                .onSuccess { detail ->
                    joinCode = ""
                    onOpen(detail.id)
                }
                .onFailure { joinError = it.message ?: "Couldn't join challenge" }
            joinLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Challenges",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Compete with friends, stay consistent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(onClick = { load() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            }

            // Join with invite code
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Join with invite code",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it; joinError = null },
                                placeholder = { Text("Enter code") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { join() },
                                enabled = !joinLoading
                            ) {
                                Text("Join")
                            }
                        }
                        if (joinLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        joinError?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Error state
            error?.let {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Loading state
            if (isLoading && challenges.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Empty state
            if (!isLoading && challenges.isEmpty() && error == null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No challenges yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create one or join with an invite code to compete with friends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Challenge list
            items(challenges, key = { it.id }) { challenge ->
                ChallengeSummaryCard(challenge = challenge, onClick = { onOpen(challenge.id) })
            }
        }

        // FAB
        ExtendedFloatingActionButton(
            onClick = onCreate,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Create challenge") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 4.dp)
        )
    }
}

@Composable
private fun ChallengeSummaryCard(challenge: ChallengeSummary, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(challenge.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${challenge.metric.label}  •  ${prettyDateRange(challenge.startDate, challenge.endDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${challenge.participantCount} participants",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                challenge.myRank?.let { rank ->
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Your rank: #$rank",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Create
// ---------------------------------------------------------------------------

@Composable
private fun CreateChallengeContent(
    onBack: () -> Unit,
    onCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val api = remember { ChallengeApi(context) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf(ChallengeMetric.STEPS) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun showDatePicker(initial: LocalDate?, minToday: Boolean, onPicked: (LocalDate) -> Unit) {
        val baseDate = initial ?: LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            baseDate.year, baseDate.monthValue - 1, baseDate.dayOfMonth
        ).apply {
            if (minToday) {
                // Allow today or yesterday to prevent timezone boundary glitches
                datePicker.minDate = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            }
            show()
        }
    }

    fun create() {
        when {
            name.isBlank() -> {
                error = "Give your challenge a name"
                return
            }
            startDate == null -> {
                error = "Please select a start date"
                return
            }
            endDate == null -> {
                error = "Please select an end date"
                return
            }
            endDate!!.isBefore(startDate) -> {
                error = "End date can't be before the start date"
                return
            }
        }
        scope.launch {
            creating = true
            error = null
            api.createChallenge(
                name = name,
                description = description,
                metricType = metric.apiValue,
                startDate = startDate!!.format(ApiDateFormat),
                endDate = endDate!!.format(ApiDateFormat)
            ).onSuccess { detail ->
                onCreated(detail.id)
            }.onFailure {
                error = it.message ?: "Couldn't create challenge"
            }
            creating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 120.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Create challenge",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name & description
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Challenge name") },
                    placeholder = { Text("e.g. October Step Battle") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dates & Challenge Type Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Start Date & End Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateInputBox(
                        label = "Start Date",
                        date = startDate,
                        onClick = { showDatePicker(startDate, minToday = true) { startDate = it } },
                        modifier = Modifier.weight(1f)
                    )
                    DateInputBox(
                        label = "End Date",
                        date = endDate,
                        onClick = { showDatePicker(endDate ?: startDate, minToday = true) { endDate = it } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Challenge Type Section
                Text(
                    text = "Challenge Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChallengeMetric.entries.forEach { m ->
                        ChallengeTypeCard(
                            metric = m,
                            isSelected = metric == m,
                            onClick = { metric = m },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Error
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Create button
        Button(
            onClick = { create() },
            enabled = !creating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (creating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create challenge")
            }
        }
    }
}

@Composable
private fun DateInputBox(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = date?.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
                    ?: "Select date",
                style = MaterialTheme.typography.bodyMedium,
                color = if (date != null) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun ChallengeTypeCard(
    metric: ChallengeMetric,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val backgroundColor = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569)
    val iconColor = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)

    Card(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ChallengeTypeIcon(
                metric = metric,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChallengeTypeIcon(
    metric: ChallengeMetric,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (metric) {
        ChallengeMetric.STEPS -> {
            StepsOutlineIcon(
                tint = tint,
                modifier = modifier
            )
        }
        ChallengeMetric.WORKOUTS -> {
            Icon(
                imageVector = Icons.Outlined.FitnessCenter,
                contentDescription = null,
                tint = tint,
                modifier = modifier.graphicsLayer { rotationZ = -45f }
            )
        }
        ChallengeMetric.CALORIES -> {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = tint,
                modifier = modifier
            )
        }
        ChallengeMetric.DISTANCE_KM -> {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = tint,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun StepsOutlineIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.75.dp.toPx()
        val w = size.width
        val h = size.height

        // Left footprint outline
        val leftPath = Path().apply {
            moveTo(0.30f * w, 0.20f * h)
            cubicTo(0.18f * w, 0.20f * h, 0.10f * w, 0.30f * h, 0.10f * w, 0.44f * h)
            cubicTo(0.10f * w, 0.56f * h, 0.15f * w, 0.64f * h, 0.16f * w, 0.72f * h)
            cubicTo(0.16f * w, 0.84f * h, 0.22f * w, 0.94f * h, 0.30f * w, 0.94f * h)
            cubicTo(0.38f * w, 0.94f * h, 0.42f * w, 0.84f * h, 0.42f * w, 0.72f * h)
            cubicTo(0.40f * w, 0.60f * h, 0.32f * w, 0.52f * h, 0.36f * w, 0.40f * h)
            cubicTo(0.39f * w, 0.28f * h, 0.42f * w, 0.20f * h, 0.30f * w, 0.20f * h)
            close()
        }

        // Right footprint outline (higher and offset right)
        val rightPath = Path().apply {
            moveTo(0.70f * w, 0.06f * h)
            cubicTo(0.58f * w, 0.06f * h, 0.61f * w, 0.14f * h, 0.64f * w, 0.26f * h)
            cubicTo(0.68f * w, 0.38f * h, 0.60f * w, 0.46f * h, 0.58f * w, 0.58f * h)
            cubicTo(0.58f * w, 0.70f * h, 0.62f * w, 0.80f * h, 0.70f * w, 0.80f * h)
            cubicTo(0.78f * w, 0.80f * h, 0.84f * w, 0.70f * h, 0.84f * w, 0.58f * h)
            cubicTo(0.85f * w, 0.50f * h, 0.90f * w, 0.42f * h, 0.90f * w, 0.30f * h)
            cubicTo(0.90f * w, 0.16f * h, 0.82f * w, 0.06f * h, 0.70f * w, 0.06f * h)
            close()
        }

        drawPath(
            path = leftPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = rightPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Detail
// ---------------------------------------------------------------------------

@Composable
private fun ChallengeDetailContent(
    challengeId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val api = remember { ChallengeApi(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    val scope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<ChallengeDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var lastSynced by remember { mutableStateOf<String?>(null) }
    var inviteEmail by remember { mutableStateOf("") }
    var inviteSending by remember { mutableStateOf(false) }
    var inviteMessage by remember { mutableStateOf<String?>(null) }
    var inviteIsError by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    fun syncProgress(d: ChallengeDetail) {
        scope.launch {
            syncing = true
            syncError = null
            try {
                val start = LocalDate.parse(d.startDate, ApiDateFormat)
                val end = LocalDate.parse(d.endDate, ApiDateFormat)
                val totals = healthConnectManager.readProgressTotals(start, end)
                api.postProgress(d.id, totals)
                    .onSuccess {
                        lastSynced = "Last synced just now"
                        api.getDetail(d.id).onSuccess { detail = it }
                    }
                    .onFailure { syncError = it.message ?: "Couldn't sync progress" }
            } catch (e: Exception) {
                syncError = "Couldn't read health data for the challenge dates"
            }
            syncing = false
        }
    }

    fun load(autoSync: Boolean) {
        scope.launch {
            isLoading = true
            error = null
            api.getDetail(challengeId)
                .onSuccess {
                    detail = it
                    isLoading = false
                    if (autoSync) syncProgress(it)
                }
                .onFailure {
                    error = it.message ?: "Couldn't load challenge"
                    isLoading = false
                }
        }
    }

    fun sendInvite() {
        val d = detail ?: return
        if (inviteEmail.isBlank()) {
            inviteMessage = "Enter an email address"
            inviteIsError = true
            return
        }
        scope.launch {
            inviteSending = true
            inviteIsError = false
            inviteMessage = null
            api.inviteEmail(d.id, inviteEmail)
                .onSuccess {
                    inviteMessage = it
                    inviteEmail = ""
                }
                .onFailure {
                    inviteMessage = it.message ?: "Couldn't send invite"
                    inviteIsError = true
                }
            inviteSending = false
        }
    }

    fun leave() {
        scope.launch {
            leaving = true
            api.leaveChallenge(challengeId)
                .onSuccess { onBack() }
                .onFailure {
                    syncError = it.message ?: "Couldn't leave challenge"
                    showLeaveConfirm = false
                }
            leaving = false
        }
    }

    LaunchedEffect(challengeId) { load(autoSync = true) }

    // Leave confirmation dialog
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = {
                Text(
                    "Leave challenge?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "Your progress will be removed from the leaderboard.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { leave() },
                    enabled = !leaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Leave") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp, bottom = 120.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = detail?.name ?: "Challenge",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(onClick = { detail?.let { syncProgress(it) } }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Sync progress"
                )
            }
        }

        // Loading state
        if (isLoading && detail == null) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // Error state
        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }

        val d = detail ?: return@Column
        Spacer(modifier = Modifier.height(16.dp))

        // Status + meta card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(d.status)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${d.participantCount} participants",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${d.metric.label}  •  ${prettyDateRange(d.startDate, d.endDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (d.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = d.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                d.myRank?.let { rank ->
                    Text(
                        text = "Your rank: #$rank",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                } ?: Text(
                    text = "You're not ranked yet — sync your progress below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Invite code card
        ElevatedCard(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Challenge invite code", d.inviteCode))
                copied = true
                Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2000)
                    copied = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Invite code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = d.inviteCode.ifBlank { "—" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = if (copied) "Copied!" else "Tap to copy",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (copied) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy invite code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Invite by email
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Invite by email",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it; inviteMessage = null },
                        placeholder = { Text("friend@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { sendInvite() },
                        enabled = !inviteSending
                    ) {
                        Text("Send")
                    }
                }
                if (inviteSending) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                inviteMessage?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (inviteIsError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress sync card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your progress",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                syncing -> "Syncing with Health Connect…"
                                lastSynced != null -> lastSynced!!
                                else -> "Not synced yet"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { syncProgress(d) },
                        enabled = !syncing
                    ) {
                        Text("Sync now")
                    }
                }
                if (syncing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                syncError?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Leaderboard card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (d.leaderboard.isEmpty()) {
                    Text(
                        text = "No activity recorded yet — sync your progress to appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column {
                        d.leaderboard.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
                            }
                            LeaderboardRow(
                                entry = entry,
                                metric = d.metric,
                                isMe = d.myRank != null && entry.rank == d.myRank
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Leave challenge button
        OutlinedButton(
            onClick = { showLeaveConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Leave challenge")
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    metric: ChallengeMetric,
    isMe: Boolean
) {
    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else androidx.compose.ui.graphics.Color.Transparent
        ),
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = when (entry.rank) {
                    1 -> MaterialTheme.colorScheme.tertiaryContainer
                    2 -> MaterialTheme.colorScheme.surfaceContainerHighest
                    3 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${entry.rank}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = entry.displayName + if (isMe) " (you)" else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        trailingContent = {
            Text(
                text = formatMetricValue(metric, entry.value),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun StatusChip(status: ChallengeStatus) {
    val (containerColor, labelColor) = when (status) {
        ChallengeStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ChallengeStatus.UPCOMING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ChallengeStatus.ENDED -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

private fun prettyDateRange(start: String, end: String): String {
    return try {
        val fmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val s = LocalDate.parse(start, ApiDateFormat).format(fmt)
        val e = LocalDate.parse(end, ApiDateFormat).format(fmt)
        "$s – $e"
    } catch (e: Exception) {
        "$start – $end"
    }
}

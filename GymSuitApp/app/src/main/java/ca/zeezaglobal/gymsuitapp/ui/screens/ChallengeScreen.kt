package ca.zeezaglobal.gymsuitapp.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import ca.zeezaglobal.gymsuitapp.ui.theme.PoppinsFontFamily
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Challenges",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Compete with friends, stay consistent",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                IconButton(onClick = { load() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF6B7280)
                    )
                }
            }
        }

        item {
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Create challenge", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            WhiteCard {
                Text(
                    text = "Join with invite code",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                    Button(
                        onClick = { join() },
                        enabled = !joinLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join")
                    }
                }
                if (joinLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                joinError?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = it, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        }

        error?.let {
            item {
                WhiteCard {
                    Text(text = it, color = Color(0xFFDC2626), fontSize = 13.sp)
                }
            }
        }

        if (isLoading && challenges.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        if (!isLoading && challenges.isEmpty() && error == null) {
            item {
                WhiteCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No challenges yet",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF111827)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create one or join with an invite code to compete with friends.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
        }

        items(challenges, key = { it.id }) { challenge ->
            ChallengeSummaryCard(challenge = challenge, onClick = { onOpen(challenge.id) })
        }
    }
}

@Composable
private fun ChallengeSummaryCard(challenge: ChallengeSummary, onClick: () -> Unit) {
    WhiteCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = challenge.name,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF111827),
                modifier = Modifier.weight(1f)
            )
            StatusPill(challenge.status)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${challenge.metric.label}  •  ${prettyDateRange(challenge.startDate, challenge.endDate)}",
            fontSize = 13.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${challenge.participantCount} participants",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
            challenge.myRank?.let { rank ->
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your rank: #$rank",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Create
// ---------------------------------------------------------------------------

private val MetricIcons: Map<ChallengeMetric, ImageVector> = mapOf(
    ChallengeMetric.STEPS to Icons.Filled.DirectionsWalk,
    ChallengeMetric.WORKOUTS to Icons.Filled.FitnessCenter,
    ChallengeMetric.CALORIES to Icons.Filled.Whatshot,
    ChallengeMetric.DISTANCE_KM to Icons.Filled.Route
)

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
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var error by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun showDatePicker(initial: LocalDate, minToday: Boolean, onPicked: (LocalDate) -> Unit) {
        DatePickerDialog(
            context,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d)) },
            initial.year, initial.monthValue - 1, initial.dayOfMonth
        ).apply {
            if (minToday) datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    fun create() {
        when {
            name.isBlank() -> {
                error = "Give your challenge a name"
                return
            }
            endDate.isBefore(startDate) -> {
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
                startDate = startDate.format(ApiDateFormat),
                endDate = endDate.format(ApiDateFormat)
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
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp)
    ) {
        ScreenHeader(title = "Create challenge", onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        WhiteCard {
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

        Spacer(modifier = Modifier.height(12.dp))

        WhiteCard {
            Text(
                text = "Compete on",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChallengeMetric.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { m ->
                            MetricOption(
                                metric = m,
                                selected = metric == m,
                                onSelect = { metric = m },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        WhiteCard {
            DateRow(
                label = "Start date",
                date = startDate,
                onClick = { showDatePicker(startDate, minToday = true) { startDate = it } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DateRow(
                label = "End date",
                date = endDate,
                onClick = { showDatePicker(endDate, minToday = true) { endDate = it } }
            )
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color(0xFFDC2626), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { create() },
            enabled = !creating,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (creating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Create challenge", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MetricOption(
    metric: ChallengeMetric,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) primary.copy(alpha = 0.12f) else Color(0xFFF9FAFB),
        border = BorderStroke(
            1.5.dp,
            if (selected) primary else Color(0xFFE5E7EB)
        ),
        modifier = modifier.clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = MetricIcons[metric] ?: Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = if (selected) primary else Color(0xFF6B7280),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = metric.label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (selected) Color(0xFF111827) else Color(0xFF4B5563)
            )
        }
    }
}

@Composable
private fun DateRow(label: String, date: LocalDate, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFF4B5563))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF3F4F6),
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111827),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
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

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave challenge?", fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold) },
            text = { Text("Your progress will be removed from the leaderboard.") },
            confirmButton = {
                TextButton(
                    onClick = { leave() },
                    enabled = !leaving
                ) { Text("Leave", color = Color(0xFFDC2626)) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp)
    ) {
        ScreenHeader(
            title = detail?.name ?: "Challenge",
            onBack = onBack,
            trailing = {
                IconButton(onClick = { detail?.let { syncProgress(it) } }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Sync progress",
                        tint = Color(0xFF6B7280)
                    )
                }
            }
        )

        if (isLoading && detail == null) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            WhiteCard { Text(text = it, color = Color(0xFFDC2626), fontSize = 13.sp) }
            return@Column
        }

        val d = detail ?: return@Column
        Spacer(modifier = Modifier.height(12.dp))

        // Status + meta
        WhiteCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(d.status)
                Text(
                    text = "${d.participantCount} participants",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${d.metric.label}  •  ${prettyDateRange(d.startDate, d.endDate)}",
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
            if (d.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = d.description, fontSize = 14.sp, color = Color(0xFF374151))
            }
            Spacer(modifier = Modifier.height(10.dp))
            d.myRank?.let { rank ->
                Text(
                    text = "Your rank: #$rank",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } ?: Text(
                text = "You're not ranked yet — sync your progress below",
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Invite code
        WhiteCard(
            modifier = Modifier.clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Challenge invite code", d.inviteCode))
                copied = true
                Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
                scope.launch {
                    delay(2000)
                    copied = false
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Invite code",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = d.inviteCode.ifBlank { "—" },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        fontFamily = PoppinsFontFamily,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = if (copied) "Copied!" else "Tap to copy",
                        fontSize = 12.sp,
                        color = if (copied) Color(0xFF16A34A) else Color(0xFF9CA3AF)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy invite code",
                    tint = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Invite by email
        WhiteCard {
            Text(
                text = "Invite by email",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                Button(
                    onClick = { sendInvite() },
                    enabled = !inviteSending,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send")
                }
            }
            inviteMessage?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = if (inviteIsError) Color(0xFFDC2626) else Color(0xFF16A34A)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress sync
        WhiteCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your progress",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = when {
                            syncing -> "Syncing with Health Connect…"
                            lastSynced != null -> lastSynced!!
                            else -> "Not synced yet"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
                OutlinedButton(
                    onClick = { syncProgress(d) },
                    enabled = !syncing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sync now")
                }
            }
            if (syncing) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            syncError?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = it, color = Color(0xFFDC2626), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Leaderboard
        WhiteCard {
            Text(
                text = "Leaderboard",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (d.leaderboard.isEmpty()) {
                Text(
                    text = "No activity recorded yet — sync your progress to appear here.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    d.leaderboard.forEach { entry ->
                        LeaderboardRow(
                            entry = entry,
                            metric = d.metric,
                            isMe = d.myRank != null && entry.rank == d.myRank
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { showLeaveConfirm = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Leave challenge", color = Color(0xFFDC2626), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    metric: ChallengeMetric,
    isMe: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (entry.rank) {
                        1 -> Color(0xFFFEF3C7)
                        2 -> Color(0xFFF3F4F6)
                        3 -> Color(0xFFFFF7ED)
                        else -> Color(0xFFF9FAFB)
                    }
                )
        ) {
            Text(
                text = "${entry.rank}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF374151)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.displayName + if (isMe) " (you)" else "",
            fontSize = 14.sp,
            fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal,
            color = Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMetricValue(metric, entry.value),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827)
        )
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun WhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF111827)
            )
        }
        Text(
            text = title,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF111827),
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun StatusPill(status: ChallengeStatus) {
    val (bg, fg) = when (status) {
        ChallengeStatus.ACTIVE -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        ChallengeStatus.UPCOMING -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        ChallengeStatus.ENDED -> Color(0xFFF3F4F6) to Color(0xFF6B7280)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = status.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

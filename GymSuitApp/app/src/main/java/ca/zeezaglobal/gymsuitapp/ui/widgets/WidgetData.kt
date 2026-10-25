package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import ca.zeezaglobal.gymsuitapp.MainActivity
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeMetric
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeStatus
import ca.zeezaglobal.gymsuitapp.data.remote.ChallengeApi
import ca.zeezaglobal.gymsuitapp.ui.screens.DAILY_STEPS_GOAL
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Content for one home-screen widget, plus the code that loads it and pushes
 * it to every placed instance. Every loader is failure-proof: a missing
 * permission, a logged-out session, or a dead network degrades to an honest
 * placeholder instead of ever crashing the widget host.
 */
data class WidgetContent(
    val title: String,
    val value: String,
    val subtitle: String,
    /** 0..100 progress for the steps widget; null hides the bar. */
    val progressPercent: Int? = null
)

data class MemberProgress(val name: String, val progress: String)

data class ChallengeWidgetContent(
    val value: String,
    val subtitle: String,
    val members: List<MemberProgress> = emptyList()
)

private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

/** Per-widget challenge choice, stored by appWidgetId. Null = automatic. */
private const val CHALLENGE_WIDGET_PREFS = "gymsuit_challenge_widgets"
private fun prefs(context: Context) =
    context.getSharedPreferences(CHALLENGE_WIDGET_PREFS, Context.MODE_PRIVATE)

fun getChallengeSelection(context: Context, appWidgetId: Int): String? =
    prefs(context).getString("widget_$appWidgetId", null)

fun saveChallengeSelection(context: Context, appWidgetId: Int, challengeId: String?) {
    prefs(context).edit().apply {
        if (challengeId == null) remove("widget_$appWidgetId") else putString("widget_$appWidgetId", challengeId)
    }.apply()
}

fun clearChallengeSelection(context: Context, appWidgetId: Int) {
    prefs(context).edit().remove("widget_$appWidgetId").apply()
}

suspend fun loadStepsContent(context: Context): WidgetContent {
    return try {
        val steps = HealthConnectManager(context).readProgressTotals(LocalDate.now(), LocalDate.now()).steps
        if (steps > 0) {
            WidgetContent(
                title = "Steps",
                value = numberFormat.format(steps),
                subtitle = "${(steps.toFloat() / DAILY_STEPS_GOAL * 100).toInt()}% of ${numberFormat.format(DAILY_STEPS_GOAL)} goal",
                progressPercent = (steps.toFloat() / DAILY_STEPS_GOAL * 100).toInt().coerceIn(0, 100)
            )
        } else {
            WidgetContent("Steps", "—", "No data yet")
        }
    } catch (e: Exception) {
        WidgetContent("Steps", "—", "No data yet")
    }
}

suspend fun loadHeartRateContent(context: Context): WidgetContent {
    val manager = HealthConnectManager(context)
    return try {
        // Same retry loop as the in-app heart rate card: Health Connect
        // reads fail transiently (rate limiting), and a single attempt
        // used to leave the widget stuck on "No data yet".
        var summary: ca.zeezaglobal.gymsuitapp.data.HeartRateSummaryData? = null
        for (attempt in 0 until 3) {
            val fresh = manager.tryReadHeartRateDataForDate(LocalDate.now())
            if (fresh != null && fresh.hasData) {
                summary = fresh
                break
            }
            if (attempt < 2) delay(800)
        }
        val today = summary
        if (today != null) {
            return WidgetContent(
                title = "Heart rate",
                value = "${today.latestBpm} bpm",
                subtitle = "Min ${today.minBpm} · Max ${today.maxBpm} bpm"
            )
        }
        // No samples today yet (e.g. the watch hasn't synced): fall back to
        // the most recent day with data in the last week, labeled as such,
        // instead of showing an empty widget all day.
        for (daysAgo in 1..6) {
            val day = LocalDate.now().minusDays(daysAgo.toLong())
            val past = manager.tryReadHeartRateDataForDate(day)
            if (past != null && past.hasData) {
                val label = if (daysAgo == 1) {
                    "Yesterday"
                } else {
                    day.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
                }
                return WidgetContent(
                    title = "Heart rate",
                    value = "${past.latestBpm} bpm",
                    subtitle = "$label · Min ${past.minBpm} · Max ${past.maxBpm} bpm"
                )
            }
        }
        val hint = if (!manager.hasHeartRatePermission()) {
            "Heart rate access not granted"
        } else {
            "No data yet"
        }
        WidgetContent("Heart rate", "—", hint)
    } catch (e: Exception) {
        WidgetContent("Heart rate", "—", "No data yet")
    }
}

suspend fun loadWorkoutsContent(context: Context): WidgetContent {
    return try {
        val workouts = WorkoutStore(context).getLoggedWorkouts()
        val weekStart = LocalDate.now().minusDays(6)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val thisWeek = workouts.count { it.timestampMillis >= weekStart }
        val last = workouts.firstOrNull()
        val lastText = last?.let {
            val date = java.time.Instant.ofEpochMilli(it.timestampMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
            "Last: ${it.exerciseName} · $date"
        } ?: "Log your first workout"
        WidgetContent(
            title = "Workouts",
            value = "$thisWeek this week",
            subtitle = lastText
        )
    } catch (e: Exception) {
        WidgetContent("Workouts", "—", "No data yet")
    }
}

private fun formatMetricValue(metric: ChallengeMetric, value: Double): String =
    when (metric) {
        ChallengeMetric.STEPS -> numberFormat.format(value.toLong())
        ChallengeMetric.WORKOUTS -> value.toInt().toString()
        ChallengeMetric.CALORIES -> "${numberFormat.format(value.toLong())} kcal"
        ChallengeMetric.DISTANCE_KM -> String.format(Locale.US, "%.1f km", value)
    }

/**
 * Challenge widget content for one widget instance. [challengeId] is the
 * user's picker choice for that widget, or null for automatic (first active
 * challenge). Shows the leaderboard's top 3 members with their progress.
 */
suspend fun loadChallengesContent(context: Context, challengeId: String?): ChallengeWidgetContent {
    if (!AuthManager(context.applicationContext).isLoggedIn()) {
        return ChallengeWidgetContent("Log in", "Open GymSuit to see challenges")
    }
    val api = ChallengeApi(context)
    return try {
        if (challengeId == null) {
            val result = api.listChallenges()
            result.fold(
                onSuccess = { challenges ->
                    val active = challenges.filter { it.status == ChallengeStatus.ACTIVE }
                    if (active.isEmpty()) {
                        ChallengeWidgetContent("No active", "Tap Challenges to choose one")
                    } else {
                        loadChallengesContent(context, active.first().id)
                    }
                },
                onFailure = {
                    ChallengeWidgetContent("—", "Couldn't load challenges")
                }
            )
        } else {
            val result = api.getDetail(challengeId)
            result.fold(
                onSuccess = { detail ->
                    val members = detail.leaderboard
                        .sortedBy { it.rank }
                        .take(3)
                        .map { MemberProgress(it.displayName, formatMetricValue(detail.metric, it.value)) }
                    val rankText = detail.myRank?.let { " · You're #$it" } ?: ""
                    ChallengeWidgetContent(
                        value = detail.name,
                        subtitle = "${detail.participantCount} members · ${detail.metric.label}$rankText",
                        members = members
                    )
                },
                onFailure = {
                    ChallengeWidgetContent("—", "Couldn't load challenges")
                }
            )
        }
    } catch (e: Exception) {
        ChallengeWidgetContent("—", "Couldn't load challenges")
    }
}

private fun openAppIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
    return PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun pickChallengeIntent(context: Context, appWidgetId: Int): PendingIntent {
    val intent = Intent(context, ChallengeWidgetConfigActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    return PendingIntent.getActivity(
        context, appWidgetId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun buildViews(context: Context, layoutRes: Int, content: WidgetContent): RemoteViews {
    val views = RemoteViews(context.packageName, layoutRes)
    views.setTextViewText(R.id.widget_title, content.title)
    views.setTextViewText(R.id.widget_value, content.value)
    views.setTextViewText(R.id.widget_subtitle, content.subtitle)
    content.progressPercent?.let {
        views.setProgressBar(R.id.widget_progress, 100, it, false)
    }
    views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
    return views
}

private fun buildChallengeViews(
    context: Context,
    appWidgetId: Int,
    content: ChallengeWidgetContent
): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_challenges)
    views.setTextViewText(R.id.widget_title, "Challenges")
    views.setTextViewText(R.id.widget_value, content.value)
    views.setTextViewText(R.id.widget_subtitle, content.subtitle)
    // Tapping the title opens the challenge picker for this widget; tapping
    // anywhere else opens the app.
    views.setOnClickPendingIntent(R.id.widget_title, pickChallengeIntent(context, appWidgetId))
    views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
    val nameIds = listOf(R.id.member_name_1, R.id.member_name_2, R.id.member_name_3)
    val progressIds = listOf(R.id.member_progress_1, R.id.member_progress_2, R.id.member_progress_3)
    val rowIds = listOf(R.id.member_row_1, R.id.member_row_2, R.id.member_row_3)
    for (i in 0 until 3) {
        if (i < content.members.size) {
            views.setTextViewText(nameIds[i], "#${i + 1} ${content.members[i].name}")
            views.setTextViewText(progressIds[i], content.members[i].progress)
            views.setViewVisibility(rowIds[i], View.VISIBLE)
        } else {
            views.setViewVisibility(rowIds[i], View.GONE)
        }
    }
    return views
}

/**
 * Refreshes every placed instance of all four widgets. Called by the
 * periodic worker, immediately whenever a widget is added, and whenever the
 * app comes to the foreground (foreground Health Connect reads succeed even
 * when background reads are restricted). Challenge widgets are refreshed
 * per instance so each can show its own picked challenge.
 */
suspend fun refreshAllWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val shared = listOf(
        Triple(StepsAppWidget::class.java, R.layout.widget_steps, loadStepsContent(context)),
        Triple(HeartRateAppWidget::class.java, R.layout.widget_heart_rate, loadHeartRateContent(context)),
        Triple(WorkoutsAppWidget::class.java, R.layout.widget_workouts, loadWorkoutsContent(context))
    )
    for ((provider, layoutRes, content) in shared) {
        val views = buildViews(context, layoutRes, content)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, provider))
        for (id in ids) {
            appWidgetManager.updateAppWidget(id, views)
        }
    }
    val challengeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ChallengesAppWidget::class.java))
    val detailCache = mutableMapOf<String?, ChallengeWidgetContent>()
    for (id in challengeIds) {
        val selection = getChallengeSelection(context, id)
        val content = detailCache.getOrPut(selection) { loadChallengesContent(context, selection) }
        appWidgetManager.updateAppWidget(id, buildChallengeViews(context, id, content))
    }
}

package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ca.zeezaglobal.gymsuitapp.MainActivity
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.HealthConnectManager
import ca.zeezaglobal.gymsuitapp.data.WorkoutStore
import ca.zeezaglobal.gymsuitapp.data.local.AuthManager
import ca.zeezaglobal.gymsuitapp.data.model.ChallengeStatus
import ca.zeezaglobal.gymsuitapp.data.remote.ChallengeApi
import ca.zeezaglobal.gymsuitapp.ui.screens.DAILY_STEPS_GOAL
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

private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

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
    return try {
        val summary = HealthConnectManager(context).readHeartRateDataForDate(LocalDate.now())
        if (summary.hasData) {
            WidgetContent(
                title = "Heart rate",
                value = "${summary.latestBpm} bpm",
                subtitle = "Min ${summary.minBpm} · Max ${summary.maxBpm} bpm"
            )
        } else {
            WidgetContent("Heart rate", "—", "No data yet")
        }
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

suspend fun loadChallengesContent(context: Context): WidgetContent {
    if (!AuthManager(context.applicationContext).isLoggedIn()) {
        return WidgetContent("Challenges", "Log in", "Open GymSuit to see challenges")
    }
    return try {
        val result = ChallengeApi(context).listChallenges()
        result.fold(
            onSuccess = { challenges ->
                val active = challenges.filter { it.status == ChallengeStatus.ACTIVE }
                if (active.isEmpty()) {
                    WidgetContent("Challenges", "No active", "Join one from the Challenges tab")
                } else {
                    val first = active.first()
                    val rank = first.myRank?.let { " · #$it" } ?: ""
                    WidgetContent(
                        title = "Challenges",
                        value = "${active.size} active",
                        subtitle = "${first.name}$rank"
                    )
                }
            },
            onFailure = {
                WidgetContent("Challenges", "—", "Couldn't load challenges")
            }
        )
    } catch (e: Exception) {
        WidgetContent("Challenges", "—", "Couldn't load challenges")
    }
}

private fun openAppIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
    return PendingIntent.getActivity(
        context, 0, intent,
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

/**
 * Refreshes every placed instance of all four widgets. Called by the
 * periodic worker and immediately whenever a widget is added.
 */
suspend fun refreshAllWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val jobs = listOf(
        Triple(StepsAppWidget::class.java, R.layout.widget_steps, loadStepsContent(context)),
        Triple(HeartRateAppWidget::class.java, R.layout.widget_heart_rate, loadHeartRateContent(context)),
        Triple(WorkoutsAppWidget::class.java, R.layout.widget_workouts, loadWorkoutsContent(context)),
        Triple(ChallengesAppWidget::class.java, R.layout.widget_challenges, loadChallengesContent(context))
    )
    for ((provider, layoutRes, content) in jobs) {
        val views = buildViews(context, layoutRes, content)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, provider))
        for (id in ids) {
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

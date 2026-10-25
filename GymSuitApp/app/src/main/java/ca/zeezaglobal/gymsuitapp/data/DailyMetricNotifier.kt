package ca.zeezaglobal.gymsuitapp.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ca.zeezaglobal.gymsuitapp.MainActivity
import ca.zeezaglobal.gymsuitapp.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Daily metric summary notification, posted from the background health sync
 * ([HealthSyncWorker]) so users get a reason to open the app — and opening
 * the app runs a fresh sync (see MainActivity).
 *
 * Rules, deliberately boring:
 * - At most one notification per calendar day, and only on a sync that runs
 *   at or after 6 PM local time, so "today" actually has a day's worth of
 *   steps in it. WorkManager timing is approximate (Doze, battery), so on
 *   days with no evening run there is simply no notification that day.
 * - Only posts when there is something to report (steps > 0 or a sleep
 *   record). Sleep prefers today's record and falls back to yesterday's,
 *   since last night's sleep is often attributed to the previous day.
 * - Silently skips when notifications are disabled or POST_NOTIFICATIONS
 *   was not granted. Never throws: a notification failure must never fail
 *   the sync that triggered it.
 * - Tapping the notification opens MainActivity (which syncs on launch).
 */
object DailyMetricNotifier {

    private const val TAG = "DailyMetricNotifier"
    private const val CHANNEL_ID = "gymsuit_daily_metrics"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS = "metric_notification_prefs"
    private const val KEY_LAST_NOTIFIED_DATE = "last_notified_date"
    private const val EVENING_HOUR = 18

    suspend fun maybeNotify(context: Context) {
        try {
            if (LocalTime.now().hour < EVENING_HOUR) return
            val today = LocalDate.now()
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getString(KEY_LAST_NOTIFIED_DATE, null) == today.toString()) return
            if (!isNotificationAllowed(context)) return

            val hc = HealthConnectManager(context.applicationContext)
            val steps = hc.readProgressTotals(today, today).steps
            var sleepMinutes = hc.readDetailedSleepForDate(today)
                .let { if (it.hasData) it.totalSleepMinutes else 0L }
            if (sleepMinutes <= 0) {
                sleepMinutes = hc.readDetailedSleepForDate(today.minusDays(1))
                    .let { if (it.hasData) it.totalSleepMinutes else 0L }
            }
            if (steps <= 0 && sleepMinutes <= 0) return

            val lines = mutableListOf<String>()
            if (steps > 0) {
                lines.add("You walked ${NumberFormat.getNumberInstance(Locale.US).format(steps)} steps today.")
            }
            if (sleepMinutes > 0) {
                lines.add("You slept ${formatDuration(sleepMinutes)} last night.")
            }
            val body = lines.joinToString("\n")

            ensureChannel(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pending = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Your day in GymSuit")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_DATE, today.toString()).apply()
            Log.i(TAG, "Daily metric notification posted (steps=$steps, sleepMin=$sleepMinutes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post daily metric notification", e)
        }
    }

    private fun isNotificationAllowed(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily metrics",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Evening summary of your steps and sleep, from the background health sync."
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun formatDuration(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }
}

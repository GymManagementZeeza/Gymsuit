package ca.zeezaglobal.gymsuitapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ca.zeezaglobal.gymsuitapp.MainActivity
import ca.zeezaglobal.gymsuitapp.R
import ca.zeezaglobal.gymsuitapp.data.FoodStore
import ca.zeezaglobal.gymsuitapp.data.MealType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** One daily reminder: morning, noon, evening and night. */
enum class MealSlot(val time: LocalTime, val meal: MealType, val title: String, val text: String) {
    MORNING(LocalTime.of(8, 30), MealType.BREAKFAST, "Good morning", "Log your breakfast to start the day on track."),
    NOON(LocalTime.of(12, 30), MealType.LUNCH, "Lunch time", "Don't forget to log what you ate for lunch."),
    EVENING(LocalTime.of(17, 30), MealType.SNACK, "Evening check-in", "Had a snack or tea? Log it now."),
    NIGHT(LocalTime.of(20, 30), MealType.DINNER, "Dinner time", "Log your dinner to complete today's food diary.")
}

object MealReminders {
    private const val PREFS = "meal_reminders"
    private const val KEY_ENABLED = "enabled"
    private const val CHANNEL_ID = "meal_reminders"
    private const val SLOT_KEY = "slot"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Re-applies the schedule on app start if reminders are on. */
    fun restore(context: Context) {
        if (isEnabled(context)) schedule(context)
    }

    private fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        MealSlot.entries.forEach { slot ->
            val request = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayUntilNext(slot.time).toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString(SLOT_KEY, slot.name).build())
                .setConstraints(Constraints.NONE)
                .build()
            workManager.enqueueUniquePeriodicWork("meal_reminder_${slot.name}", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    private fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context)
        MealSlot.entries.forEach { workManager.cancelUniqueWork("meal_reminder_${it.name}") }
    }

    private fun delayUntilNext(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(now.toLocalDate(), time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    internal fun slotFrom(data: Data): MealSlot? = data.getString(SLOT_KEY)?.let { runCatching { MealSlot.valueOf(it) }.getOrNull() }

    internal fun notify(context: Context, slot: MealSlot) {
        if (!hasNotificationPermission(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Meal reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders to log your meals"
                }
            )
        }
        val open = PendingIntent.getActivity(
            context, slot.ordinal,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(slot.title)
            .setContentText(slot.text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(2000 + slot.ordinal, notification)
    }
}

class MealReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val slot = MealReminders.slotFrom(inputData) ?: return Result.success()
        if (!MealReminders.isEnabled(applicationContext)) return Result.success()

        // Skip the reminder if that meal is already logged today
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val alreadyLogged = FoodStore(applicationContext).getAll().any {
            it.meal == slot.meal &&
                java.time.Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() == today
        }
        if (!alreadyLogged) MealReminders.notify(applicationContext, slot)
        return Result.success()
    }
}

package ca.zeezaglobal.gymsuitapp.data

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Runs [HealthSyncManager.sync] in the background so health data keeps flowing
 * to the backend (and into the health report email) without opening the app.
 *
 * Notes:
 * - WorkManager guarantees *periodic*, not exact timing: with a 4h interval the
 *   OS may shift runs (Doze, battery optimizations). This is an OS limit, not a bug.
 * - The worker is a no-op when logged out or when Health Connect permissions
 *   are missing — [HealthSyncManager.sync] already guards both.
 * - On Android 14+, background Health Connect reads need the user to grant
 *   "Allow all the time" in the Health Connect app; otherwise background runs
 *   simply find no new data.
 */
class HealthSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val ok = HealthSyncManager(applicationContext).sync()
            Log.i(TAG, "Background health sync finished (ok=$ok)")
            if (ok) {
                // Evening daily summary ("You walked X steps today...").
                // Never fails the sync: the notifier swallows its own errors.
                DailyMetricNotifier.maybeNotify(applicationContext)
            }
            // Success even when nothing synced (logged out / no permissions):
            // retrying would just burn battery for the same outcome.
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background health sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HealthSyncWorker"
        const val UNIQUE_WORK_NAME = "gymsuit_background_health_sync"
    }
}

object BackgroundHealthSync {

    /** How often to attempt a background sync. WorkManager minimum is 15 minutes. */
    private const val INTERVAL_HOURS = 4L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(HealthSyncWorker.UNIQUE_WORK_NAME)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            HealthSyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(HealthSyncWorker.UNIQUE_WORK_NAME)
    }
}

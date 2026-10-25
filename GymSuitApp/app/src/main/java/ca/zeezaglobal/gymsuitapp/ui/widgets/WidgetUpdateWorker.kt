package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Refreshes all four home-screen widgets. Runs every 30 minutes (WorkManager
 * periodic, so it survives reboots) and once immediately whenever a widget is
 * added, so a newly placed widget populates instead of sitting on its
 * placeholder. Never fails hard: [refreshAllWidgets] degrades per widget.
 */
class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        refreshAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "gymsuit_widget_update"
        private const val INTERVAL_MINUTES = 30L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .addTag(UNIQUE_WORK_NAME)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun enqueueOneShot(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .addTag(UNIQUE_WORK_NAME)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME + "_once",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

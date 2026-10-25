package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home-screen widget: workouts logged in the last 7 days + last workout. */
class WorkoutsAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateWorker.enqueueOneShot(context)
    }
}

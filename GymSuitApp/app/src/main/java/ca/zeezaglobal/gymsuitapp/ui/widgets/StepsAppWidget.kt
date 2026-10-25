package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home-screen widget: today's steps with progress toward the 10,000 goal. */
class StepsAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateWorker.enqueueOneShot(context)
    }
}

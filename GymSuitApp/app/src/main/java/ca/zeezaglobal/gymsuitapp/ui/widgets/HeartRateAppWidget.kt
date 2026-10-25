package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home-screen widget: latest heart rate today with the day's min/max. */
class HeartRateAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateWorker.enqueueOneShot(context)
    }
}

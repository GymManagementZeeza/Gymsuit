package ca.zeezaglobal.gymsuitapp.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Home-screen widget: active group fitness challenges + your rank. */
class ChallengesAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdateWorker.enqueueOneShot(context)
    }
}

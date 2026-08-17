package ca.rplnet.btopwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TermuxWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val fg = Prefs.getFgColor(context)
            val bg = Prefs.getBgColor(context)
            val text = BtopRenderer.renderTermuxPanel(context)

            val views = RemoteViews(context.packageName, R.layout.widget_termux)
            views.setTextViewText(R.id.widget_text, text)
            views.setTextColor(R.id.widget_text, fg)
            views.setInt(R.id.widget_root, "setBackgroundColor", bg)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        AppWidgetManager.getInstance(context).updateAppWidgetProviderInfo(
            android.content.ComponentName(context, TermuxWidgetProvider::class.java),
            null
        )
        RefreshWorker.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId)
    }
}

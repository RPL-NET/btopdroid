package ca.rplnet.btopwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class BtopWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "ca.rplnet.btopwidget.ACTION_REFRESH"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val stats = SystemStats.collect(context)
            val fg = Prefs.getFgColor(context)
            val bg = Prefs.getBgColor(context)
            val username = Prefs.getUsername(context)
            val hostname = Prefs.getHostname(context)
            // largeur fixe en caracteres — c'est autoSizeTextType (uniform)
            // dans le layout qui ajuste la taille de police pour que ca rentre
            // peu importe la vraie taille du widget, plus fiable que d'essayer
            // de deviner les dp reels (fragile d'un launcher OEM a l'autre)
            val text = BtopRenderer.render(context, stats, username, hostname, 44)

            val views = RemoteViews(context.packageName, R.layout.widget_btop)
            views.setTextViewText(R.id.widget_text, text)
            views.setTextColor(R.id.widget_text, fg)
            views.setInt(R.id.widget_root, "setBackgroundColor", bg)

            val refreshIntent = Intent(context, BtopWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, widgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        // le widget a ete redimensionne sur l'ecran d'accueil -> redessine
        // au nouveau format plutot que d'attendre le prochain cycle de 15min
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        RefreshWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        RefreshWorker.cancel(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, BtopWidgetProvider::class.java)
            )
            for (widgetId in widgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}

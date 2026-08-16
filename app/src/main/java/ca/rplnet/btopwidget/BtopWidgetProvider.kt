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

        // resolution fixe des bitmaps de graph — l'ImageView les scale en
        // fitXY, donc la resolution reelle importe peu tant qu'elle garde
        // un ratio raisonnable et une taille lisible.
        private const val GRAPH_W = 480
        private const val GRAPH_H = 90
        private const val METER_H = 60

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val stats = SystemStats.collect(context)
            val fg = Prefs.getFgColor(context)
            val bg = Prefs.getBgColor(context)
            val username = Prefs.getUsername(context)
            val hostname = Prefs.getHostname(context)

            val ramHistory = HistoryStore.push(context, "ram", stats.ramUsedPct)
            val cpuHistory = if (stats.cpuPct != null) {
                HistoryStore.push(context, "cpu", stats.cpuPct)
            } else {
                HistoryStore.get(context, "cpu")
            }

            val views = RemoteViews(context.packageName, R.layout.widget_btop)
            views.setTextViewText(R.id.widget_header, BtopRenderer.renderHeader(stats, username, hostname))
            views.setTextViewText(R.id.widget_footer, BtopRenderer.renderFooter(stats))
            views.setTextColor(R.id.widget_header, fg)
            views.setTextColor(R.id.widget_footer, fg)
            views.setInt(R.id.widget_root, "setBackgroundColor", bg)

            val cpuLabel = "cpu(${stats.cpuSource})"
            views.setImageViewBitmap(
                R.id.graph_ram,
                GraphRenderer.render(ramHistory, stats.ramUsedPct, "ram", fg, bg, GRAPH_W, GRAPH_H)
            )
            views.setImageViewBitmap(
                R.id.graph_cpu,
                GraphRenderer.render(cpuHistory, stats.cpuPct ?: -1, cpuLabel, fg, bg, GRAPH_W, GRAPH_H)
            )
            views.setImageViewBitmap(
                R.id.meter_bat,
                GraphRenderer.renderMeter(
                    stats.batteryPct, "bat",
                    "%.2fV  %.1f°C%s".format(stats.batteryVoltageV, stats.batteryTempC, if (stats.charging) "  ⚡" else ""),
                    fg, bg, GRAPH_W, METER_H
                )
            )
            views.setImageViewBitmap(
                R.id.meter_disk,
                GraphRenderer.renderMeter(
                    stats.storageUsedPct, "dsk",
                    "%.1fG / %.1fG".format(stats.storageUsedGb, stats.storageTotalGb),
                    fg, bg, GRAPH_W, METER_H
                )
            )

            // tap sur le widget = refresh immediat (bypass le throttling
            // de updatePeriodMillis, minimum 30min impose par Android)
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

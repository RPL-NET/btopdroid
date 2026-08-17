package ca.rplnet.btopwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class BtopWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "ca.rplnet.btopwidget.ACTION_REFRESH"

        // repartition approximative de la hauteur dispo entre le header et
        // les 5 panels, doit rester en phase avec les layout_weight du XML
        private const val HEADER_WEIGHT = 0.7f
        private const val TOTAL_WEIGHT = 5.5f // header(0.7) + cpu(1) + ram(1) + bat(0.8) + dsk(0.8) + net(1.2)

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
            val netUpHistory = HistoryStore.pushRaw(context, "netup", stats.netUpKbps)
            val netDownHistory = HistoryStore.pushRaw(context, "netdown", stats.netDownKbps)

            val size = WidgetSizing.get(context, appWidgetManager, widgetId)
            val panelWidth = size.widthPx
            val availableHeight = (size.heightPx - 12).coerceAtLeast(80) // 12px = padding du root layout
            val unitHeight = (availableHeight / TOTAL_WEIGHT).toInt()
            val headerHeight = (unitHeight * HEADER_WEIGHT).toInt()
            val meterHeight = (unitHeight * 0.8f).toInt()
            val netHeight = (unitHeight * 1.2f).toInt()
            // taille de police UNIQUE pour tous les panels, calculee sur le
            // plus petit panel (meter) — sinon chaque bitmap dessine sa
            // propre taille proportionnelle a SA hauteur et ca devient
            // incoherent d'un panel a l'autre (cpu/ram plus gros texte que
            // bat/dsk juste parce que leur panel est plus haut)
            val textSizePx = (meterHeight * 0.22f).coerceAtLeast(10f)

            val views = RemoteViews(context.packageName, R.layout.widget_btop)
            views.setInt(R.id.widget_root, "setBackgroundColor", bg)

            views.setImageViewBitmap(
                R.id.widget_header,
                GraphBitmapRenderer.renderHeader(
                    username, hostname, stats.uptimeStr, stats.clock, stats.date,
                    fg, bg, panelWidth, headerHeight, textSizePx
                )
            )

            views.setImageViewBitmap(
                R.id.graph_cpu,
                GraphBitmapRenderer.renderAreaGraph(
                    cpuHistory, "cpu(${stats.cpuSource})",
                    "${stats.cpuPct?.toString() ?: "n/a"}%  ${stats.cpuFreqCurMhz ?: "?"}/${stats.cpuFreqMaxMhz ?: "?"}MHz",
                    stats.cpuPct ?: 0, MetricColors.CPU, fg, bg, panelWidth, unitHeight, textSizePx
                )
            )
            views.setImageViewBitmap(
                R.id.graph_ram,
                GraphBitmapRenderer.renderAreaGraph(
                    ramHistory, "ram", "${stats.ramUsedPct}%", stats.ramUsedPct,
                    MetricColors.RAM, fg, bg, panelWidth, unitHeight, textSizePx
                )
            )
            val batChargeTag = if (stats.charging) "chg" else "bat"
            views.setImageViewBitmap(
                R.id.meter_bat,
                GraphBitmapRenderer.renderMeter(
                    stats.batteryPct, batChargeTag,
                    "%.1fV  %.0fC".format(stats.batteryVoltageV, stats.batteryTempC),
                    MetricColors.BATTERY, fg, bg, panelWidth, meterHeight, textSizePx
                )
            )
            views.setImageViewBitmap(
                R.id.meter_disk,
                GraphBitmapRenderer.renderMeter(
                    stats.storageUsedPct, "dsk",
                    "%.1fG / %.1fG".format(stats.storageUsedGb, stats.storageTotalGb),
                    MetricColors.DISK, fg, bg, panelWidth, meterHeight, textSizePx
                )
            )
            views.setImageViewBitmap(
                R.id.graph_net,
                GraphBitmapRenderer.renderNetMirror(
                    netUpHistory, netDownHistory, stats.netUpKbps, stats.netDownKbps,
                    fg, bg, panelWidth, netHeight, textSizePx
                )
            )

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
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        // fix du bug "resize fige a la taille d'ajout" — sans cet appel, un
        // widget deja place sur l'ecran d'accueil garde les metadonnees
        // maxResizeWidth/Height du moment ou il a ete ajoute, peu importe
        // les mises a jour d'APK subsequentes (confirme empiriquement)
        AppWidgetManager.getInstance(context).updateAppWidgetProviderInfo(
            ComponentName(context, BtopWidgetProvider::class.java),
            null
        )
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
                ComponentName(context, BtopWidgetProvider::class.java)
            )
            for (widgetId in widgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}

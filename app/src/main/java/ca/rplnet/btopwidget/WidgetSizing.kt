package ca.rplnet.btopwidget

import android.appwidget.AppWidgetManager
import android.content.Context

// Dimensions reelles du widget en pixels — pas de conversion vers un nombre
// de "caracteres" devine (ancienne approche, fragile, abandonnee). Pour du
// Bitmap Canvas on veut juste width/height en px, direct depuis les dp
// rapportes par le systeme.
object WidgetSizing {
    data class Size(val widthPx: Int, val heightPx: Int)

    fun get(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int): Size {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
        val density = context.resources.displayMetrics.density
        return Size((widthDp * density).toInt(), (heightDp * density).toInt())
    }
}

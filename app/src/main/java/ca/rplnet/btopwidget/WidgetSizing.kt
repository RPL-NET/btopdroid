package ca.rplnet.btopwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface

// Calcule combien de caracteres monospace tiennent dans la largeur reelle
// du widget, pour que le cadre s'adapte plutot que d'utiliser une largeur
// fixe (qui deborde ou laisse un trou vide selon la taille choisie par
// l'usager sur son ecran d'accueil).
object WidgetSizing {

    private const val TEXT_SIZE_SP = 10f
    private const val PADDING_DP = 12f // padding du layout (6dp x2)

    fun charWidth(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int): Int {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        // certains launchers ne peuplent que MIN_WIDTH, d'autres MAX_WIDTH —
        // on prend le plus grand des deux pour refleter la vraie taille choisie
        val minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        val maxWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidthDp)
        val widthDp = maxOf(minWidthDp, maxWidthDp)
        val density = context.resources.displayMetrics.density
        val scaledDensity = context.resources.displayMetrics.scaledDensity

        val availableWidthPx = (widthDp - PADDING_DP) * density

        val paint = Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = TEXT_SIZE_SP * scaledDensity
        }
        val charWidthPx = paint.measureText("0")

        if (charWidthPx <= 0) return 32 // fallback si mesure impossible

        return (availableWidthPx / charWidthPx).toInt().coerceIn(24, 90)
    }
}

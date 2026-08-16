package ca.rplnet.btopwidget

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "btopdroid_prefs"
    private const val KEY_FG_COLOR = "fg_color"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_BAR_STYLE = "bar_style"

    // presets couleur: nom -> (foreground, background)
    val COLOR_PRESETS = linkedMapOf(
        "vert classique" to Pair(0xFF33FF33.toInt(), 0xFF000000.toInt()),
        "ambre rétro" to Pair(0xFFFFB000.toInt(), 0xFF000000.toInt()),
        "cyan" to Pair(0xFF00FFFF.toInt(), 0xFF000000.toInt()),
        "blanc sur noir" to Pair(0xFFFFFFFF.toInt(), 0xFF000000.toInt()),
        "rouge alerte" to Pair(0xFFFF3333.toInt(), 0xFF000000.toInt())
    )

    // styles de barre: nom -> (caractere rempli, caractere vide)
    val BAR_STYLES = linkedMapOf(
        "blocs pleins" to Pair("█", "░"),
        "ascii classique" to Pair("#", "-"),
        "égal" to Pair("=", " "),
        "carrés" to Pair("■", "□")
    )

    fun getFgColor(context: Context): Int =
        prefs(context).getInt(KEY_FG_COLOR, COLOR_PRESETS.values.first().first)

    fun getBgColor(context: Context): Int =
        prefs(context).getInt(KEY_BG_COLOR, COLOR_PRESETS.values.first().second)

    fun getBarStyle(context: Context): Pair<String, String> {
        val key = prefs(context).getString(KEY_BAR_STYLE, null)
        return BAR_STYLES[key] ?: BAR_STYLES.values.first()
    }

    fun setColorPreset(context: Context, name: String) {
        val pair = COLOR_PRESETS[name] ?: return
        prefs(context).edit()
            .putInt(KEY_FG_COLOR, pair.first)
            .putInt(KEY_BG_COLOR, pair.second)
            .apply()
    }

    fun setBarStyle(context: Context, name: String) {
        if (!BAR_STYLES.containsKey(name)) return
        prefs(context).edit().putString(KEY_BAR_STYLE, name).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

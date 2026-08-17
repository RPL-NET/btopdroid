package ca.rplnet.btopwidget

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "btopdroid_prefs"
    private const val KEY_FG_COLOR = "fg_color"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_USERNAME = "username"
    private const val KEY_HOSTNAME = "hostname"
    private const val KEY_LIVE_UPDATE = "live_update_enabled"

    fun isLiveUpdateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LIVE_UPDATE, false)

    fun setLiveUpdateEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LIVE_UPDATE, enabled).apply()
    }

    // presets couleur: nom -> (foreground, background)
    val COLOR_PRESETS = linkedMapOf(
        "vert classique" to Pair(0xFF33FF33.toInt(), 0xFF000000.toInt()),
        "ambre rétro" to Pair(0xFFFFB000.toInt(), 0xFF000000.toInt()),
        "cyan" to Pair(0xFF00FFFF.toInt(), 0xFF000000.toInt()),
        "blanc sur noir" to Pair(0xFFFFFFFF.toInt(), 0xFF000000.toInt()),
        "rouge alerte" to Pair(0xFFFF3333.toInt(), 0xFF000000.toInt())
    )

    fun getFgColor(context: Context): Int =
        prefs(context).getInt(KEY_FG_COLOR, COLOR_PRESETS.values.first().first)

    fun getBgColor(context: Context): Int =
        prefs(context).getInt(KEY_BG_COLOR, COLOR_PRESETS.values.first().second)

    fun setColorPreset(context: Context, name: String) {
        val pair = COLOR_PRESETS[name] ?: return
        prefs(context).edit()
            .putInt(KEY_FG_COLOR, pair.first)
            .putInt(KEY_BG_COLOR, pair.second)
            .apply()
    }

    fun getUsername(context: Context): String =
        prefs(context).getString(KEY_USERNAME, "user") ?: "user"

    fun getHostname(context: Context): String =
        prefs(context).getString(KEY_HOSTNAME, android.os.Build.MODEL.lowercase().replace(" ", "-")) ?: "android"

    fun setIdentity(context: Context, username: String, hostname: String) {
        prefs(context).edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_HOSTNAME, hostname)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

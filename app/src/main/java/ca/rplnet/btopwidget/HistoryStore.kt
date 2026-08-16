package ca.rplnet.btopwidget

import android.content.Context

// Garde un petit historique de samples par metrique (ram/cpu/bat) pour
// dessiner de vrais graphs dans le temps, pas juste la valeur instantanee.
// Stocke en CSV dans SharedPreferences — pas besoin d'une vraie DB pour 30 points.
object HistoryStore {
    private const val PREFS_NAME = "btopdroid_history"
    private const val MAX_POINTS = 30

    fun push(context: Context, key: String, value: Int): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = get(context, key).toMutableList()
        current.add(value.coerceIn(0, 100))
        while (current.size > MAX_POINTS) current.removeAt(0)
        prefs.edit().putString(key, current.joinToString(",")).apply()
        return current
    }

    fun get(context: Context, key: String): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}

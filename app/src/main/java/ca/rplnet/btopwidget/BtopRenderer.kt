package ca.rplnet.btopwidget

// Texte pur ASCII, uniquement pour le header (aucun graph/cadre ici — voir
// GraphBitmapRenderer pour les panels dessines en Canvas)
object BtopRenderer {

    fun renderHeader(s: Stats, username: String, hostname: String): String {
        val chargeTag = if (s.charging) "chg" else "bat"
        return "$username@$hostname  up ${s.uptimeStr}\n" +
            "${s.clock}  ${s.date}  $chargeTag ${s.batteryPct}% ${"%.1f".format(s.batteryVoltageV)}V ${"%.0f".format(s.batteryTempC)}C"
    }

    fun renderTermuxPanel(context: android.content.Context): CharSequence {
        val lines = TermuxLog.readTail(context)
        return when {
            lines == null -> "termux\n(pas configure — voir app)"
            lines.isEmpty() -> "termux\n(session vide)"
            else -> "termux\n" + lines.joinToString("\n")
        }
    }
}

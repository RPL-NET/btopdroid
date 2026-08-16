package ca.rplnet.btopwidget

object BtopRenderer {

    // labels a gauche ("cpu(top)  ", "net ↑    ", etc.) + colonne de valeur a
    // droite ("100%", "999K/s") prennent un espace fixe, le reste va au graph
    private const val LABEL_WIDTH = 9
    private const val VALUE_RESERVED = 8

    fun render(context: android.content.Context, s: Stats, username: String, hostname: String, frameWidth: Int): String {
        val width = frameWidth.coerceAtLeast(28)
        val graphWidth = (width - LABEL_WIDTH - VALUE_RESERVED).coerceAtLeast(6)

        val ramHistory = HistoryStore.push(context, "ram", s.ramUsedPct)
        val cpuHistory = if (s.cpuPct != null) {
            HistoryStore.push(context, "cpu", s.cpuPct)
        } else {
            HistoryStore.get(context, "cpu")
        }
        val netUpHistory = HistoryStore.pushRaw(context, "netup", s.netUpKbps)
        val netDownHistory = HistoryStore.pushRaw(context, "netdown", s.netDownKbps)

        val sb = StringBuilder()

        sb.append(topBorder(width, " $username@$hostname ", " up ${s.uptimeStr} ")).append("\n")

        val left = "${s.clock}  ${s.date}"
        val chargeTag = if (s.charging) "chg" else "bat"
        val right = "$chargeTag ${s.batteryPct}% ${"%.1f".format(s.batteryVoltageV)}V ${"%.0f".format(s.batteryTempC)}°"
        sb.append(row(width, left, right)).append("\n")

        sb.append(midBorder(width)).append("\n")

        val cpuLabel = "cpu(${s.cpuSource})".padEnd(LABEL_WIDTH).take(LABEL_WIDTH)
        sb.append(row(width, "$cpuLabel${Sparkline.render(cpuHistory, graphWidth)}", (s.cpuPct?.toString() ?: "n/a") + "%")).append("\n")

        val ramLabel = "ram".padEnd(LABEL_WIDTH)
        sb.append(row(width, "$ramLabel${Sparkline.render(ramHistory, graphWidth)}", "${s.ramUsedPct}%")).append("\n")

        sb.append(midBorder(width)).append("\n")

        val dskLabel = "dsk".padEnd(LABEL_WIDTH)
        sb.append(row(width, "$dskLabel${meterBar(s.storageUsedPct, graphWidth)}", "${s.storageUsedPct}%")).append("\n")

        sb.append(midBorder(width)).append("\n")

        // panel reseau miroir: up "suspendu du plafond" au-dessus, down
        // "pose au sol" en dessous — comme le panel net de btop
        val upLabel = "net ↑".padEnd(LABEL_WIDTH)
        sb.append(row(width, "$upLabel${Sparkline.renderUpperAutoScale(netUpHistory, graphWidth)}", "${s.netUpKbps}K/s")).append("\n")
        val downLabel = "net ↓".padEnd(LABEL_WIDTH)
        sb.append(row(width, "$downLabel${Sparkline.renderAutoScale(netDownHistory, graphWidth)}", "${s.netDownKbps}K/s")).append("\n")

        sb.append(bottomBorder(width))

        return sb.toString()
    }

    private fun topBorder(width: Int, leftLabel: String, rightLabel: String): String {
        val used = leftLabel.length + rightLabel.length
        val fill = (width - used - 2).coerceAtLeast(1)
        return "┌─$leftLabel${"─".repeat(fill)}$rightLabel─┐"
    }

    private fun midBorder(width: Int): String = "├" + "─".repeat(width) + "┤"

    private fun bottomBorder(width: Int): String = "└" + "─".repeat(width) + "┘"

    private fun row(width: Int, left: String, right: String): String {
        val content = left.length + right.length
        val fill = (width - content).coerceAtLeast(1)
        return "│$left${" ".repeat(fill)}$right│"
    }

    private fun meterBar(pct: Int, width: Int): String {
        val clamped = pct.coerceIn(0, 100)
        val filled = (clamped * width) / 100
        return "[" + "█".repeat(filled) + "░".repeat((width - 2 - filled).coerceAtLeast(0)) + "]"
    }
}

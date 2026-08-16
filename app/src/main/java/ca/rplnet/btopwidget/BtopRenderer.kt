package ca.rplnet.btopwidget

object BtopRenderer {

    // largeur interne du cadre en caracteres — fixe pour l'instant, un widget
    // plus large va juste avoir de l'espace vide autour (amelioration future:
    // calculer dynamiquement depuis les dimensions reelles du widget)
    private const val WIDTH = 42
    private const val GRAPH_WIDTH = 28

    fun render(context: android.content.Context, s: Stats, username: String, hostname: String): String {
        val ramHistory = HistoryStore.push(context, "ram", s.ramUsedPct)
        val cpuHistory = if (s.cpuPct != null) {
            HistoryStore.push(context, "cpu", s.cpuPct)
        } else {
            HistoryStore.get(context, "cpu")
        }
        val netUpHistory = HistoryStore.pushRaw(context, "netup", s.netUpKbps)
        val netDownHistory = HistoryStore.pushRaw(context, "netdown", s.netDownKbps)

        val sb = StringBuilder()

        // cadre du haut avec user@host a gauche, uptime a droite
        sb.append(topBorder(" $username@$hostname ", " up ${s.uptimeStr} ")).append("\n")

        // ligne heure/date a gauche, batterie a droite
        val left = "${s.clock}  ${s.date}"
        val battIcon = if (s.charging) "⚡" else " "
        val right = "bat ${s.batteryPct}%$battIcon ${"%.1f".format(s.batteryVoltageV)}V ${"%.0f".format(s.batteryTempC)}°"
        sb.append(row(left, right)).append("\n")

        sb.append(midBorder()).append("\n")

        val cpuLabel = "cpu(${s.cpuSource})".padEnd(9).take(9)
        sb.append(row("$cpuLabel${Sparkline.render(cpuHistory, GRAPH_WIDTH)}", (s.cpuPct?.toString() ?: "n/a") + "%")).append("\n")

        val ramLabel = "ram".padEnd(9)
        sb.append(row("$ramLabel${Sparkline.render(ramHistory, GRAPH_WIDTH)}", "${s.ramUsedPct}%")).append("\n")

        sb.append(midBorder()).append("\n")

        val dskLabel = "dsk".padEnd(9)
        val dskBar = meterBar(s.storageUsedPct, GRAPH_WIDTH)
        sb.append(row("$dskLabel$dskBar", "${s.storageUsedPct}%")).append("\n")

        sb.append(midBorder()).append("\n")

        val upLabel = "net ↑".padEnd(9)
        sb.append(row("$upLabel${Sparkline.renderAutoScale(netUpHistory, GRAPH_WIDTH)}", "${s.netUpKbps}K/s")).append("\n")
        val downLabel = "net ↓".padEnd(9)
        sb.append(row("$downLabel${Sparkline.renderAutoScale(netDownHistory, GRAPH_WIDTH)}", "${s.netDownKbps}K/s")).append("\n")

        sb.append(bottomBorder())

        return sb.toString()
    }

    private fun topBorder(leftLabel: String, rightLabel: String): String {
        val used = leftLabel.length + rightLabel.length
        val fill = (WIDTH - used - 2).coerceAtLeast(1)
        return "┌─$leftLabel${"─".repeat(fill)}$rightLabel─┐"
    }

    private fun midBorder(): String = "├" + "─".repeat(WIDTH) + "┤"

    private fun bottomBorder(): String = "└" + "─".repeat(WIDTH) + "┘"

    private fun row(left: String, right: String): String {
        val content = left.length + right.length
        val fill = (WIDTH - content).coerceAtLeast(1)
        return "│$left${" ".repeat(fill)}$right│"
    }

    private fun meterBar(pct: Int, width: Int): String {
        val clamped = pct.coerceIn(0, 100)
        val filled = (clamped * width) / 100
        return "[" + "█".repeat(filled) + "░".repeat((width - 2 - filled).coerceAtLeast(0)) + "]"
    }
}

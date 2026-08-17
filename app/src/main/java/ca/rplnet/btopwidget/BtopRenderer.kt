package ca.rplnet.btopwidget

import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan

object BtopRenderer {

    private const val LABEL_WIDTH = 9
    private const val VALUE_RESERVED = 8
    // largeur reduite (etait 44) pour laisser autoSizeTextType grossir la
    // police et mieux remplir la hauteur du widget, comme demande
    private const val DEFAULT_WIDTH = 32

    // degrade fonce -> pale selon l'intensite de la valeur, comme btop.
    // Alpha varie de 90 (dim, valeur basse) a 255 (plein, valeur haute).
    private fun colorFor(fgColor: Int, pct: Int): Int {
        val alpha = 90 + (pct.coerceIn(0, 100) * 165 / 100)
        return (alpha shl 24) or (fgColor and 0x00FFFFFF)
    }

    fun render(
        context: android.content.Context,
        s: Stats,
        username: String,
        hostname: String,
        fgColor: Int,
        frameWidth: Int = DEFAULT_WIDTH
    ): SpannableStringBuilder {
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

        val sb = SpannableStringBuilder()

        sb.append(topBorder(width, " $username@$hostname ", " up ${s.uptimeStr} ")).append("\n")

        val left = "${s.clock}  ${s.date}"
        val chargeTag = if (s.charging) "chg" else "bat"
        val right = "$chargeTag ${s.batteryPct}% ${"%.1f".format(s.batteryVoltageV)}V ${"%.0f".format(s.batteryTempC)}C"
        appendRow(sb, width, left, right)
        sb.append("\n")

        sb.append(midBorder(width)).append("\n")

        val cpuLabel = "cpu(${s.cpuSource})".padEnd(LABEL_WIDTH).take(LABEL_WIDTH)
        appendGraphRow(sb, width, cpuLabel, Sparkline.render(cpuHistory, graphWidth), fgColor, (s.cpuPct?.toString() ?: "n/a") + "%")
        sb.append("\n")

        val ramLabel = "ram".padEnd(LABEL_WIDTH)
        appendGraphRow(sb, width, ramLabel, Sparkline.render(ramHistory, graphWidth), fgColor, "${s.ramUsedPct}%")
        sb.append("\n")

        sb.append(midBorder(width)).append("\n")

        val dskLabel = "dsk".padEnd(LABEL_WIDTH)
        appendRow(sb, width, "$dskLabel${meterBar(s.storageUsedPct, graphWidth)}", "${s.storageUsedPct}%")
        sb.append("\n")

        sb.append(midBorder(width)).append("\n")

        // mirror comme btop: la rangee du haut "pose sur son plancher" (blocs
        // bas, grandit vers le bas de l'ecran), celle du dessous "pend du
        // plafond" (blocs hauts, grandit vers le haut) — les deux se
        // rencontrent visuellement a la frontiere entre les deux lignes
        val downLabel = "net dn".padEnd(LABEL_WIDTH)
        appendGraphRow(sb, width, downLabel, Sparkline.renderAutoScale(netDownHistory, graphWidth), fgColor, "${s.netDownKbps}K/s")
        sb.append("\n")
        val upLabel = "net up".padEnd(LABEL_WIDTH)
        appendGraphRow(sb, width, upLabel, Sparkline.renderUpperAutoScale(netUpHistory, graphWidth), fgColor, "${s.netUpKbps}K/s")
        sb.append("\n")

        sb.append(bottomBorder(width))

        return sb
    }

    private fun appendGraphRow(
        sb: SpannableStringBuilder,
        width: Int,
        label: String,
        graph: List<Pair<Char, Int>>,
        fgColor: Int,
        value: String
    ) {
        val contentLen = label.length + graph.size + value.length
        val fill = (width - contentLen).coerceAtLeast(1)

        sb.append("│").append(label)
        for ((ch, pct) in graph) {
            val start = sb.length
            sb.append(ch)
            sb.setSpan(ForegroundColorSpan(colorFor(fgColor, pct)), start, sb.length, 0)
        }
        sb.append(" ".repeat(fill)).append(value).append("│")
    }

    private fun appendRow(sb: SpannableStringBuilder, width: Int, left: String, right: String) {
        val content = left.length + right.length
        val fill = (width - content).coerceAtLeast(1)
        sb.append("│").append(left).append(" ".repeat(fill)).append(right).append("│")
    }

    private fun topBorder(width: Int, leftLabel: String, rightLabel: String): String {
        val used = leftLabel.length + rightLabel.length
        val fill = (width - used - 2).coerceAtLeast(1)
        return "┌─$leftLabel${"─".repeat(fill)}$rightLabel─┐"
    }

    private fun midBorder(width: Int): String = "├" + "─".repeat(width) + "┤"

    private fun bottomBorder(width: Int): String = "└" + "─".repeat(width) + "┘"

    private fun meterBar(pct: Int, width: Int): String {
        val clamped = pct.coerceIn(0, 100)
        val filled = (clamped * width) / 100
        return "[" + "█".repeat(filled) + "░".repeat((width - 2 - filled).coerceAtLeast(0)) + "]"
    }
}

package ca.rplnet.btopwidget

object BtopRenderer {

    private const val BAR_WIDTH = 14

    private fun bar(pct: Int): String {
        val clamped = pct.coerceIn(0, 100)
        val filled = (clamped * BAR_WIDTH) / 100
        return "[" + "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled) + "]"
    }

    fun render(s: Stats): String {
        val sb = StringBuilder()
        sb.append("${s.clock}  ${s.date}\n")
        sb.append("uptime ${s.uptimeStr}\n")
        sb.append("─".repeat(BAR_WIDTH + 8)).append("\n")

        sb.append("ram  ${bar(s.ramUsedPct)} ${s.ramUsedPct}%\n")
        sb.append("     ${s.ramUsedMb}M / ${s.ramTotalMb}M\n")

        sb.append("dsk  ${bar(s.storageUsedPct)} ${s.storageUsedPct}%\n")
        sb.append("     %.1fG / %.1fG\n".format(s.storageUsedGb, s.storageTotalGb))

        val cpuLine = if (s.cpuPct != null) {
            "cpu  ${bar(s.cpuPct)} ${s.cpuPct}%\n"
        } else {
            "cpu  n/a (bloqué par le systeme)\n"
        }
        sb.append(cpuLine)

        val battIcon = if (s.charging) "⚡" else " "
        sb.append("bat  ${bar(s.batteryPct)} ${s.batteryPct}%$battIcon\n")

        sb.append("net  ↓${s.netDownKbps}KB/s ↑${s.netUpKbps}KB/s")

        return sb.toString()
    }
}

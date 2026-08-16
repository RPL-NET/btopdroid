package ca.rplnet.btopwidget

object BtopRenderer {

    fun renderHeader(s: Stats): String {
        return "${s.clock}  ${s.date}\nuptime ${s.uptimeStr}"
    }

    fun renderFooter(s: Stats): String {
        val sb = StringBuilder()
        sb.append("dsk %.1fG / %.1fG (%d%%)\n".format(s.storageUsedGb, s.storageTotalGb, s.storageUsedPct))
        sb.append("net  ↓${s.netDownKbps}KB/s ↑${s.netUpKbps}KB/s")
        return sb.toString()
    }
}

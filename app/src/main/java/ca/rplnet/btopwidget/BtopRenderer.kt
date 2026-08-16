package ca.rplnet.btopwidget

object BtopRenderer {

    fun renderHeader(s: Stats, username: String, hostname: String): String {
        return "$username@$hostname\n${s.clock}  ${s.date}\nuptime ${s.uptimeStr}"
    }

    fun renderFooter(s: Stats): String {
        return "net  ↓${s.netDownKbps}KB/s  ↑${s.netUpKbps}KB/s"
    }
}

package ca.rplnet.btopwidget

// Genere un mini-graph en caracteres de blocs Unicode (comme btop), pas une
// ligne dessinee — chaque caractere represente un niveau 0-8 (9 paliers).
object Sparkline {
    private val BLOCKS = charArrayOf(' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')

    // downsample/upsample une liste de valeurs 0-100 vers exactement `width`
    // caracteres, en prenant les points les plus recents
    fun render(values: List<Int>, width: Int): String {
        if (values.isEmpty()) return " ".repeat(width)

        val recent = if (values.size >= width) {
            values.takeLast(width)
        } else {
            List(width - values.size) { 0 } + values
        }

        return recent.joinToString("") { v ->
            val idx = ((v.coerceIn(0, 100) * 8) / 100)
            BLOCKS[idx].toString()
        }
    }

    // pour des valeurs sans max fixe (ex: KB/s reseau) — normalise par le
    // max observe dans la fenetre, comme le fait btop pour ses graphs reseau
    fun renderAutoScale(values: List<Long>, width: Int): String {
        if (values.isEmpty()) return " ".repeat(width)
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val asPct = values.map { (it * 100 / max).toInt() }
        return render(asPct, width)
    }
}

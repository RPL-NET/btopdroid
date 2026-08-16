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

    // variante "suspendue du plafond" pour un graph miroir (up au-dessus,
    // down en dessous, comme le panel reseau de btop). Le jeu de caracteres
    // "upper eighth" complet n'existe pas dans le bloc Unicode standard
    // (risque de tofu sur certaines polices Android/OEM) — on se limite donc
    // a 3 paliers surs: ▔ (1/8 haut) ▀ (1/2 haut) █ (plein), ce qui suffit a
    // donner un vrai effet miroir sans dependre de glyphes exotiques.
    private val UPPER_BLOCKS = charArrayOf(' ', '▔', '▔', '▀', '▀', '▀', '█', '█', '█')

    fun renderUpperAutoScale(values: List<Long>, width: Int): String {
        if (values.isEmpty()) return " ".repeat(width)
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val recent = if (values.size >= width) {
            values.takeLast(width)
        } else {
            List(width - values.size) { 0L } + values
        }
        return recent.joinToString("") { v ->
            val pct = (v * 100 / max).toInt().coerceIn(0, 100)
            UPPER_BLOCKS[(pct * 8) / 100].toString()
        }
    }
}

package ca.rplnet.btopwidget

// Genere un mini-graph en caracteres de blocs Unicode (comme btop), pas une
// ligne dessinee — chaque caractere represente un niveau 0-8 (9 paliers).
// Retourne les paires (caractere, pourcentage) pour permettre au renderer
// d'appliquer un degrade de couleur fonce->pale par caractere, comme btop.
object Sparkline {
    private val BLOCKS = charArrayOf(' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█')
    private val UPPER_BLOCKS = charArrayOf(' ', '▔', '▔', '▀', '▀', '▀', '█', '█', '█')

    fun render(values: List<Int>, width: Int): List<Pair<Char, Int>> {
        if (values.isEmpty()) return List(width) { Pair(' ', 0) }
        val recent = if (values.size >= width) values.takeLast(width)
        else List(width - values.size) { 0 } + values

        return recent.map { v ->
            val pct = v.coerceIn(0, 100)
            Pair(BLOCKS[(pct * 8) / 100], pct)
        }
    }

    fun renderAutoScale(values: List<Long>, width: Int): List<Pair<Char, Int>> {
        if (values.isEmpty()) return List(width) { Pair(' ', 0) }
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return render(values.map { (it * 100 / max).toInt() }, width)
    }

    fun renderUpperAutoScale(values: List<Long>, width: Int): List<Pair<Char, Int>> {
        if (values.isEmpty()) return List(width) { Pair(' ', 0) }
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val recent = if (values.size >= width) values.takeLast(width)
        else List(width - values.size) { 0L } + values

        return recent.map { v ->
            val pct = (v * 100 / max).toInt().coerceIn(0, 100)
            Pair(UPPER_BLOCKS[(pct * 8) / 100], pct)
        }
    }
}

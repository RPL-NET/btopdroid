package ca.rplnet.btopwidget

// Genere un mini-graph en caracteres ASCII purs — les caracteres Unicode de
// blocs/boite (▁▂▃█ ┌─│) ne sont PAS garantis monospace sur toutes les
// polices Android (confirme sur Motorola: la police de fallback utilisee
// pour ces glyphes a une largeur d'avance differente de l'ASCII, ce qui
// casse tout l'alignement). ASCII pur = garanti safe partout.
object Sparkline {
    private val LEVELS = charArrayOf(' ', '.', ':', '-', '=', 'x', '+', '*', '#')

    fun render(values: List<Int>, width: Int): List<Pair<Char, Int>> {
        if (values.isEmpty()) return List(width) { Pair(' ', 0) }
        val recent = if (values.size >= width) values.takeLast(width)
        else List(width - values.size) { 0 } + values

        return recent.map { v ->
            val pct = v.coerceIn(0, 100)
            Pair(LEVELS[(pct * 8) / 100], pct)
        }
    }

    fun renderAutoScale(values: List<Long>, width: Int): List<Pair<Char, Int>> {
        if (values.isEmpty()) return List(width) { Pair(' ', 0) }
        val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return render(values.map { (it * 100 / max).toInt() }, width)
    }

    // meme charset — la distinction "suspendu du plafond" se fait juste par
    // la position de la rangee (au-dessus vs en-dessous), pas par un jeu de
    // caracteres different, pour rester 100% ASCII-safe
    fun renderUpperAutoScale(values: List<Long>, width: Int): List<Pair<Char, Int>> =
        renderAutoScale(values, width)
}

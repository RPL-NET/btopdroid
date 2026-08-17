package ca.rplnet.btopwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

// Rendu Canvas->Bitmap, injecte dans le widget via ImageView.setImageViewBitmap().
// Choisi plutot que du texte Unicode apres avoir confirme (v0.13.1) que les
// glyphes de boite/blocs (┌─│▁▂▃█) n'ont pas une largeur monospace garantie
// sur toutes les polices Android — un Bitmap dessine au pixel pres n'a
// aucun risque de ce genre, peu importe la police du systeme.
object GraphBitmapRenderer {

    private val labelPaint = Paint().apply {
        isAntiAlias = true
        setTypeface(Typeface.MONOSPACE)
    }
    private val gridPaint = Paint().apply { strokeWidth = 1f }
    private val fillPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

    // graph auto-scale sur le min/max observe dans la fenetre (pas 0-100 fixe)
    // pour que des metriques stables comme la RAM produisent quand meme une
    // courbe visible plutot qu'une ligne plate ecrasee dans le bas de l'echelle
    private fun normalize(values: List<Int>): List<Float> {
        if (values.isEmpty()) return emptyList()
        val min = values.min()
        val max = values.max().coerceAtLeast(min + 10) // plancher: jamais moins de 10% d'ecart affiche
        return values.map { (it - min).toFloat() / (max - min) }
    }

    // vrai style btop: des mini-carres/barres dessinees au pixel, pas une
    // courbe lisse — chaque point d'historique est un petit rectangle avec
    // un espace entre chacun, hauteur proportionnelle a la valeur normalisee
    fun renderAreaGraph(
        history: List<Int>,
        label: String,
        valueText: String,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val textSize = heightPx * 0.22f
        labelPaint.textSize = textSize
        labelPaint.color = fgColor
        canvas.drawText("$label $valueText", 4f, textSize, labelPaint)

        val graphTop = textSize + 6f
        val graphBottom = heightPx - 4f
        val graphLeft = 4f
        val graphRight = widthPx - 4f
        val graphHeight = (graphBottom - graphTop).coerceAtLeast(1f)

        gridPaint.color = fgColor
        gridPaint.alpha = 35
        canvas.drawLine(graphLeft, graphTop, graphRight, graphTop, gridPaint)
        canvas.drawLine(graphLeft, graphBottom, graphRight, graphBottom, gridPaint)

        val norm = normalize(history)
        if (norm.isNotEmpty()) {
            val n = norm.size
            val totalWidth = graphRight - graphLeft
            val barSlot = totalWidth / n
            val barWidth = (barSlot * 0.7f).coerceAtLeast(1f)
            val gap = barSlot - barWidth

            norm.forEachIndexed { i, v ->
                val barHeight = v * graphHeight
                val left = graphLeft + i * barSlot + gap / 2f
                val top = graphBottom - barHeight
                val right = left + barWidth

                // degrade fonce->pale par barre selon l'intensite, comme btop
                fillPaint.color = fgColor
                fillPaint.alpha = 90 + (v * 165).toInt().coerceIn(0, 165)
                canvas.drawRect(left, top, right, graphBottom, fillPaint)
            }
        }

        return bmp
    }

    fun renderMeter(
        pct: Int,
        label: String,
        subtitle: String,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val textSize = heightPx * 0.22f
        labelPaint.textSize = textSize
        labelPaint.color = fgColor
        canvas.drawText("$label ${pct.coerceIn(0, 100)}%", 4f, textSize, labelPaint)

        val barTop = textSize + 8f
        val barBottom = heightPx - textSize - 6f
        val barLeft = 4f
        val barRight = widthPx - 4f

        val outline = Paint().apply {
            color = fgColor; alpha = 100; style = Paint.Style.STROKE; strokeWidth = 2f
        }
        canvas.drawRect(barLeft, barTop, barRight, barBottom, outline)

        val fillWidth = (barRight - barLeft) * (pct.coerceIn(0, 100) / 100f)
        fillPaint.color = fgColor
        fillPaint.alpha = 170
        canvas.drawRect(barLeft, barTop, barLeft + fillWidth, barBottom, fillPaint)

        val subPaint = Paint().apply {
            color = fgColor; alpha = 210; isAntiAlias = true
            setTypeface(Typeface.MONOSPACE)
            this.textSize = textSize * 0.85f
        }
        canvas.drawText(subtitle, 4f, heightPx - 4f, subPaint)

        return bmp
    }

    // les deux graphs (up/down) dans UN seul bitmap, vrai miroir pixel-parfait
    // (up grandit depuis le haut vers le centre, down depuis le bas vers le
    // centre) — impossible a faire proprement en texte, trivial en Canvas
    fun renderNetMirror(
        upHistory: List<Long>,
        downHistory: List<Long>,
        upKbps: Long,
        downKbps: Long,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val textSize = heightPx * 0.16f
        labelPaint.textSize = textSize
        labelPaint.color = fgColor
        canvas.drawText("net ↑${upKbps}K/s ↓${downKbps}K/s", 4f, textSize, labelPaint)

        val graphTop = textSize + 6f
        val graphBottom = heightPx - 4f
        val centerY = (graphTop + graphBottom) / 2f
        val graphLeft = 4f
        val graphRight = widthPx - 4f
        val halfHeight = (centerY - graphTop).coerceAtLeast(1f)

        gridPaint.color = fgColor
        gridPaint.alpha = 45
        canvas.drawLine(graphLeft, centerY, graphRight, centerY, gridPaint)

        val maxUp = (upHistory.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val maxDown = (downHistory.maxOrNull() ?: 0L).coerceAtLeast(1L)

        // mini-carres comme le reste des graphs, pas une courbe lisse
        fun drawSide(values: List<Long>, max: Long, goingUp: Boolean) {
            if (values.isEmpty()) return
            val n = values.size
            val totalWidth = graphRight - graphLeft
            val barSlot = totalWidth / n
            val barWidth = (barSlot * 0.7f).coerceAtLeast(1f)
            val gap = barSlot - barWidth

            values.forEachIndexed { i, v ->
                val frac = (v.toFloat() / max).coerceIn(0f, 1f)
                val barHeight = frac * halfHeight
                val left = graphLeft + i * barSlot + gap / 2f
                val right = left + barWidth
                val top: Float
                val bottom: Float
                if (goingUp) { top = centerY - barHeight; bottom = centerY }
                else { top = centerY; bottom = centerY + barHeight }

                fillPaint.color = fgColor
                fillPaint.alpha = 90 + (frac * 165).toInt().coerceIn(0, 165)
                canvas.drawRect(left, top, right, bottom, fillPaint)
            }
        }

        drawSide(upHistory, maxUp, goingUp = true)
        drawSide(downHistory, maxDown, goingUp = false)

        return bmp
    }
}

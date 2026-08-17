package ca.rplnet.btopwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // courbe lissee entre les points (au lieu d'une ligne brisee point-a-point)
    // via des points de controle a mi-chemin, technique classique pour un
    // rendu "fluide" sans dependre d'aucune police/glyphe
    private fun smoothPath(points: List<Pair<Float, Float>>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points[0].first, points[0].second)
        for (i in 0 until points.size - 1) {
            val (x0, y0) = points[i]
            val (x1, y1) = points[i + 1]
            val midX = (x0 + x1) / 2f
            val midY = (y0 + y1) / 2f
            path.quadTo(x0, y0, midX, midY)
        }
        val last = points.last()
        path.lineTo(last.first, last.second)
        return path
    }

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
        if (norm.size >= 2) {
            val stepX = (graphRight - graphLeft) / (norm.size - 1)
            val points = norm.mapIndexed { i, v ->
                Pair(graphLeft + stepX * i, graphBottom - v * graphHeight)
            }
            val line = smoothPath(points)

            val fill = Path(line)
            fill.lineTo(graphRight, graphBottom)
            fill.lineTo(graphLeft, graphBottom)
            fill.close()

            fillPaint.color = fgColor
            fillPaint.alpha = 65
            canvas.drawPath(fill, fillPaint)

            linePaint.color = fgColor
            linePaint.alpha = 255
            canvas.drawPath(line, linePaint)
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

        fun drawSide(values: List<Long>, max: Long, goingUp: Boolean) {
            if (values.size < 2) return
            val stepX = (graphRight - graphLeft) / (values.size - 1)
            val points = values.mapIndexed { i, v ->
                val frac = (v.toFloat() / max).coerceIn(0f, 1f)
                val y = if (goingUp) centerY - frac * halfHeight else centerY + frac * halfHeight
                Pair(graphLeft + stepX * i, y)
            }
            val line = smoothPath(points)

            val fill = Path(line)
            fill.lineTo(graphRight, centerY)
            fill.lineTo(graphLeft, centerY)
            fill.close()

            fillPaint.color = fgColor
            fillPaint.alpha = 65
            canvas.drawPath(fill, fillPaint)

            linePaint.color = fgColor
            linePaint.alpha = 255
            canvas.drawPath(line, linePaint)
        }

        drawSide(upHistory, maxUp, goingUp = true)
        drawSide(downHistory, maxDown, goingUp = false)

        return bmp
    }
}

package ca.rplnet.btopwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

// Dessine un vrai graph temporel (comme les panels btop), pas des barres statiques.
// RemoteViews ne permet pas de Canvas custom directement dans le layout — le
// contournement standard est de dessiner sur un Bitmap ici, puis de le pousser
// via setImageViewBitmap() sur une ImageView du widget.
object GraphRenderer {

    fun render(
        history: List<Int>,
        currentPct: Int,
        label: String,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val padding = 4f
        val graphTop = 22f
        val graphBottom = heightPx - padding
        val graphLeft = padding
        val graphRight = widthPx - padding
        val graphHeight = graphBottom - graphTop

        // grille horizontale legere, effet "panel" btop
        val gridPaint = Paint().apply {
            color = fgColor
            alpha = 40
            strokeWidth = 1f
        }
        for (i in 0..2) {
            val y = graphTop + (graphHeight * i / 2)
            canvas.drawLine(graphLeft, y, graphRight, y, gridPaint)
        }

        // aire remplie sous la courbe, comme un panel btop
        if (history.size >= 2) {
            val stepX = (graphRight - graphLeft) / (history.size - 1)
            val fillPath = Path()
            val linePath = Path()

            history.forEachIndexed { i, value ->
                val x = graphLeft + stepX * i
                val y = graphBottom - (value.coerceIn(0, 100) / 100f) * graphHeight
                if (i == 0) {
                    fillPath.moveTo(x, graphBottom)
                    fillPath.lineTo(x, y)
                    linePath.moveTo(x, y)
                } else {
                    fillPath.lineTo(x, y)
                    linePath.lineTo(x, y)
                }
            }
            fillPath.lineTo(graphRight, graphBottom)
            fillPath.close()

            val fillPaint = Paint().apply {
                color = fgColor
                alpha = 60
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawPath(fillPath, fillPaint)

            val linePaint = Paint().apply {
                color = fgColor
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawPath(linePath, linePaint)
        }

        // label + valeur courante en overlay, style terminal
        val textPaint = Paint().apply {
            color = fgColor
            textSize = 18f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val valueText = if (currentPct >= 0) "$label ${currentPct}%" else "$label n/a"
        canvas.drawText(valueText, padding, 16f, textPaint)

        return bmp
    }
}

package ca.rplnet.btopwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

// Rendu Canvas->Bitmap, injecte dans le widget via ImageView.setImageViewBitmap().
// Choisi plutot que du texte Unicode apres avoir confirme (v0.13.1) que les
// glyphes de boite/blocs (┌─│▁▂▃█) n'ont pas une largeur monospace garantie
// sur toutes les polices Android — un Bitmap dessine au pixel pres n'a
// aucun risque de ce genre, peu importe la police du systeme.
object GraphBitmapRenderer {

    // constantes de style partagees par tous les panels, pour une coherence
    // visuelle stricte (meme padding, meme rayon de coin, meme epaisseur
    // de bordure partout — plus d'incoherences panel a panel)
    private const val PADDING = 6f
    private const val CORNER_RADIUS = 0f // coins carres, style terminal — pas de pilules arrondies
    private const val BORDER_STROKE = 2f
    private const val SCANLINE_SPACING = 3f // effet CRT subtil

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
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = BORDER_STROKE
        isAntiAlias = true
    }
    private val titleBgPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val scanlinePaint = Paint().apply { color = Color.BLACK; alpha = 22; strokeWidth = 1f }

    // effet CRT/terminal subtil — fines lignes horizontales sombres a
    // intervalle regulier, par-dessus tout le contenu du panel
    private fun drawScanlines(canvas: Canvas, widthPx: Int, heightPx: Int) {
        var y = 0f
        while (y < heightPx) {
            canvas.drawLine(0f, y, widthPx.toFloat(), y, scanlinePaint)
            y += SCANLINE_SPACING
        }
    }

    // Cadre de panel style btop: bordure arrondie + titre incruste dans la
    // bordure du haut (un petit rectangle de la couleur de fond "coupe" la
    // ligne de bordure pour laisser respirer le texte du titre, exactement
    // le look des panels btop). Retourne le rect interieur utilisable pour
    // dessiner le contenu du panel (graph/meter), une fois la bordure/titre
    // retires de l'espace disponible.
    private fun drawPanelChrome(
        canvas: Canvas,
        widthPx: Int,
        heightPx: Int,
        title: String,
        fgColor: Int,
        bgColor: Int,
        textSizePx: Float
    ): RectF {
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val borderRect = RectF(BORDER_STROKE, BORDER_STROKE, w - BORDER_STROKE, h - BORDER_STROKE)

        borderPaint.color = fgColor
        borderPaint.alpha = 140
        canvas.drawRoundRect(borderRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        // titre incruste dans la bordure du haut, prefixe "$" style prompt
        val promptTitle = "$ $title"
        val titleTextSize = textSizePx * 0.82f
        labelPaint.textSize = titleTextSize
        labelPaint.color = fgColor
        val titleWidth = labelPaint.measureText(promptTitle)
        val titleX = PADDING + 4f
        val titleY = BORDER_STROKE + titleTextSize * 0.85f

        titleBgPaint.color = bgColor
        canvas.drawRect(titleX - 3f, 0f, titleX + titleWidth + 3f, BORDER_STROKE + 1f, titleBgPaint)
        canvas.drawText(promptTitle, titleX, titleY, labelPaint)

        // rect interieur dispo pour le contenu, sous le titre incruste
        val contentTop = titleY + PADDING * 0.6f
        return RectF(
            PADDING + BORDER_STROKE,
            contentTop,
            w - PADDING - BORDER_STROKE,
            h - PADDING - BORDER_STROKE
        )
    }

    // derive une couleur d'intensite a partir de la couleur de base choisie
    // par l'usager, en glissant la teinte vers le rouge quand la charge
    // augmente — comme le degrade vert->jaune->rouge de btop, mais ancre
    // sur SA couleur plutot que hardcode. Reste a la teinte de base sous
    // ~55%, glisse progressivement vers le rouge au-dessus.
    private fun thresholdColor(baseColor: Int, pct: Int): Int {
        val clamped = pct.coerceIn(0, 100)
        if (clamped <= 55) return baseColor
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val t = ((clamped - 55) / 45f).coerceIn(0f, 1f)
        // interpole la teinte vers 0 (rouge), en passant par 40 (orange/jaune)
        val targetHue = 40f * (1f - t) + 0f * t
        hsv[0] = hsv[0] * (1f - t) + targetHue * t
        hsv[1] = hsv[1].coerceAtLeast(0.85f)
        return Color.HSVToColor(Color.alpha(baseColor), hsv)
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

    // vrai style btop: chaque point d'historique est une ligne verticale
    // avec un degrade de couleur (fonce a la base, couleur pleine en haut)
    // — pas une courbe/aire lissee. C'est litteralement ce que fait btop
    // avec ses colonnes de caracteres braille en mode "high color".
    private fun drawGradientColumns(
        canvas: Canvas,
        values: List<Float>,
        left: Float,
        right: Float,
        baselineY: Float,
        farY: Float, // extremite max possible (haut si ca grandit vers le haut, bas si vers le bas)
        accentColor: Int
    ) {
        if (values.isEmpty()) return
        val n = values.size
        val totalWidth = right - left
        val colSlot = totalWidth / n
        val colWidth = (colSlot * 0.75f).coerceAtLeast(1.5f)

        val dim = (0x33 shl 24) or (accentColor and 0x00FFFFFF) // meme teinte, tres sombre a la base
        val bright = (0xFF shl 24) or (accentColor and 0x00FFFFFF)
        val range = farY - baselineY // negatif si ca grandit vers le haut

        val colPaint = Paint().apply { isAntiAlias = false }
        values.forEachIndexed { i, v ->
            val x = left + i * colSlot + (colSlot - colWidth) / 2f
            val tipY = baselineY + v * range
            if (kotlin.math.abs(baselineY - tipY) < 1f) return@forEachIndexed

            colPaint.shader = android.graphics.LinearGradient(
                x, baselineY, x, tipY, dim, bright, android.graphics.Shader.TileMode.CLAMP
            )
            val top = minOf(baselineY, tipY)
            val bottom = maxOf(baselineY, tipY)
            canvas.drawRect(x, top, x + colWidth, bottom, colPaint)
        }
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

    fun renderAreaGraph(
        history: List<Int>,
        label: String,
        valueText: String,
        currentPct: Int,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int,
        textSizePx: Float
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val accent = thresholdColor(fgColor, currentPct)
        val inner = drawPanelChrome(canvas, widthPx, heightPx, "$label $valueText", fgColor, bgColor, textSizePx)

        gridPaint.color = fgColor
        gridPaint.alpha = 30
        canvas.drawLine(inner.left, inner.top, inner.right, inner.top, gridPaint)
        canvas.drawLine(inner.left, inner.bottom, inner.right, inner.bottom, gridPaint)

        val norm = normalize(history)
        drawGradientColumns(canvas, norm, inner.left, inner.right, inner.bottom, inner.top, accent)

        drawScanlines(canvas, widthPx, heightPx)

        return bmp
    }

    fun renderMeter(
        pct: Int,
        label: String,
        subtitle: String,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int,
        textSizePx: Float
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val clamped = pct.coerceIn(0, 100)
        val accent = thresholdColor(fgColor, clamped)
        val inner = drawPanelChrome(canvas, widthPx, heightPx, "$label $clamped%", fgColor, bgColor, textSizePx)

        val subTextSize = textSizePx * 0.78f
        val barTop = inner.top
        val barBottom = inner.bottom - subTextSize - 4f
        val barLeft = inner.left
        val barRight = inner.right
        val barRadius = 0f // carre, coherent avec le reste du style terminal

        val barRect = RectF(barLeft, barTop, barRight, barBottom.coerceAtLeast(barTop + 4f))

        // meter en blocs segmentes, genre VU-metre/LED — pas une barre pleine
        val segCount = ((barRect.width() / 14f).toInt()).coerceIn(8, 30)
        val segGap = 2f
        val segWidth = (barRect.width() - segGap * (segCount - 1)) / segCount
        val litSegments = (clamped * segCount / 100f).toInt()

        val litPaint = Paint().apply { style = Paint.Style.FILL; isAntiAlias = false }
        val unlitOutline = Paint().apply {
            color = fgColor; alpha = 60; style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = false
        }

        for (i in 0 until segCount) {
            val segLeft = barRect.left + i * (segWidth + segGap)
            val segRect = RectF(segLeft, barRect.top, segLeft + segWidth, barRect.bottom)
            if (i < litSegments) {
                // degrade dim->accent le long des segments allumes, meme esprit que les graphs
                val t = if (segCount > 1) i.toFloat() / (segCount - 1) else 1f
                val dimmed = (((0x55 + (0xAA * t)).toInt().coerceIn(0, 255)) shl 24) or (accent and 0x00FFFFFF)
                litPaint.color = dimmed
                canvas.drawRect(segRect, litPaint)
            } else {
                canvas.drawRect(segRect, unlitOutline)
            }
        }

        val subPaint = Paint().apply {
            color = fgColor; alpha = 200; isAntiAlias = true
            setTypeface(Typeface.MONOSPACE)
            textSize = subTextSize
        }
        canvas.drawText(subtitle, inner.left, inner.bottom, subPaint)

        drawScanlines(canvas, widthPx, heightPx)

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
        heightPx: Int,
        textSizePx: Float
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val inner = drawPanelChrome(
            canvas, widthPx, heightPx,
            "net ↑${upKbps}K/s ↓${downKbps}K/s",
            fgColor, bgColor, textSizePx
        )

        val centerY = (inner.top + inner.bottom) / 2f
        gridPaint.color = fgColor
        gridPaint.alpha = 40
        canvas.drawLine(inner.left, centerY, inner.right, centerY, gridPaint)

        val maxUp = (upHistory.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val maxDown = (downHistory.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val upAccent = thresholdColor(fgColor, ((upKbps.toFloat() / maxUp) * 100).toInt())
        val downAccent = thresholdColor(fgColor, ((downKbps.toFloat() / maxDown) * 100).toInt())

        fun drawSide(values: List<Long>, max: Long, goingUp: Boolean, accent: Int) {
            if (values.isEmpty()) return
            val fracs = values.map { (it.toFloat() / max).coerceIn(0f, 1f) }
            val farY = if (goingUp) inner.top else inner.bottom
            drawGradientColumns(canvas, fracs, inner.left, inner.right, centerY, farY, accent)
        }

        drawSide(upHistory, maxUp, goingUp = true, upAccent)
        drawSide(downHistory, maxDown, goingUp = false, downAccent)

        drawScanlines(canvas, widthPx, heightPx)

        return bmp
    }

    // header en bitmap pour la meme coherence visuelle (cadre) que les
    // autres panels, au lieu d'un TextView plat qui flotte sans chrome
    fun renderHeader(
        username: String,
        hostname: String,
        uptime: String,
        clock: String,
        date: String,
        fgColor: Int,
        bgColor: Int,
        widthPx: Int,
        heightPx: Int,
        textSizePx: Float
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val inner = drawPanelChrome(canvas, widthPx, heightPx, "$username@$hostname", fgColor, bgColor, textSizePx)

        val infoPaint = Paint().apply {
            color = fgColor; alpha = 220; isAntiAlias = true
            setTypeface(Typeface.MONOSPACE)
            textSize = textSizePx * 0.85f
        }
        val line = "$clock  $date  up $uptime"
        canvas.drawText(line, inner.left, inner.bottom, infoPaint)

        drawScanlines(canvas, widthPx, heightPx)

        return bmp
    }
}

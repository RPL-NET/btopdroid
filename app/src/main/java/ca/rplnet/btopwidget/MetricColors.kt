package ca.rplnet.btopwidget

import android.graphics.Color

// Palette directement extraite du code source reel de btop
// (src/btop_theme.cpp, Default_theme) — btop assigne une famille de couleur
// distincte par type de metrique plutot qu'une seule teinte globale:
// cpu_start/mid/end, used_start/mid/end (memoire), cached_* (dispo pour
// disque), available_* (repris ici pour batterie, l'amber colle bien au
// sens "charge"), download_*/upload_* (reseau). Chaque famille est un
// degrade 3 points (bas charge -> milieu -> haute charge), exactement le
// meme systeme que btop utilise pour ses graphs de charge.
enum class MetricColors(val start: Int, val mid: Int, val end: Int) {
    // vert -> jaune -> rouge, identique a cpu_start/mid/end de btop
    CPU(Color.parseColor("#77ca9b"), Color.parseColor("#cbc06c"), Color.parseColor("#dc4c4c")),

    // rouge sombre -> rouge -> rose vif, identique a used_start/mid/end
    // (btop colore la memoire "used" en rouge, pas en vert — surprenant
    // mais coherent avec l'idee que RAM utilisee = pression)
    RAM(Color.parseColor("#592b26"), Color.parseColor("#d9626d"), Color.parseColor("#ff4769")),

    // cyan sombre -> cyan -> bleu vif, identique a cached_start/mid/end
    DISK(Color.parseColor("#163350"), Color.parseColor("#74e6fc"), Color.parseColor("#26c5ff")),

    // ambre sombre -> ambre -> or, identique a available_start/mid/end —
    // convention naturelle "charge" pour la batterie
    BATTERY(Color.parseColor("#4e3f0e"), Color.parseColor("#ffd77a"), Color.parseColor("#ffb814")),

    // indigo, identique a download_start/mid/end
    NET_DOWN(Color.parseColor("#291f75"), Color.parseColor("#4f43a3"), Color.parseColor("#b0a9de")),

    // magenta/violet, identique a upload_start/mid/end
    NET_UP(Color.parseColor("#620665"), Color.parseColor("#7d4180"), Color.parseColor("#dcafde"));

    // interpole start->mid sous 50%, mid->end au-dessus — meme logique que
    // le degrade 3 points de btop applique selon le niveau de charge
    fun colorFor(pct: Int): Int {
        val clamped = pct.coerceIn(0, 100)
        return if (clamped <= 50) {
            lerpColor(start, mid, clamped / 50f)
        } else {
            lerpColor(mid, end, (clamped - 50) / 50f)
        }
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        val a = Color.alpha(from) + ((Color.alpha(to) - Color.alpha(from)) * tt).toInt()
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * tt).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * tt).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * tt).toInt()
        return Color.argb(a, r, g, b)
    }
}

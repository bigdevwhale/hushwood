package app.hushwood.ui.pix

import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Keeper
import app.hushwood.domain.Season
import app.hushwood.domain.Stroke

/**
 * A guardian's portrait, coming out of the paper pixel by pixel. Each sitting paints its share, tinted by its
 * hour (dawn rose, day gold, dusk amber, night blue), so no two finished portraits are coloured alike.
 */
object Portraits {

    private val paper = rgb(232, 224, 204)

    fun tint(p: Daypart) = when (p) {
        Daypart.DAWN -> rgb(250, 160, 170)
        Daypart.DAY -> rgb(250, 214, 110)
        Daypart.DUSK -> rgb(240, 120, 70)
        Daypart.NIGHT -> rgb(80, 110, 210)
    }

    /** The finished picture, before any sitting's colour: the guardian in a round window of its own hour. */
    fun full(b: Beast): Px {
        // The window is sized to the animal, so a wren fills its own as a bear does.
        val s = Beasts.frame(b, 0)
        val w = (maxOf(s.px.w, s.px.h) * 1.45f).toInt().coerceAtLeast(22)
        val h = w + 4
        val px = Px(w, h)
        val home = when (b) {
            Beast.OWL -> Daypart.NIGHT
            Beast.HEDGEHOG, Beast.FOX -> Daypart.DUSK
            Beast.WREN, Beast.DEER -> Daypart.DAWN
            else -> Daypart.DAY
        }
        val light = Light.of(home)
        val cx = w / 2f
        val cy = h / 2f
        for (y in 0 until h) for (x in 0 until w) {
            val u = (x + 0.5f - cx) / (w / 2f - 1)
            val v = (y + 0.5f - cy) / (h / 2f - 1)
            if (u * u + v * v > 1f) continue
            val t = y / (h * 0.68f)
            val sky = if (t < 0.55f) mixc(light.top, light.mid, t / 0.55f) else mixc(light.mid, light.low, ((t - 0.55f) / 0.45f).coerceAtMost(1f))
            val q = ((t + bayer(x, y) * 0.12f) * 6).toInt() / 6f
            px.set(x, y, if (y < h * 0.68f) mixc(sky, light.low, q * 0.2f) else light.grade(Clearing.grass(Season.SUMMER)[if (y < h * 0.76f) 3 else if (noise(x, y, 3) > 0.2f) 1 else 2]))
        }
        if (home == Daypart.NIGHT) for (i in 0 until 14) px.set(((noise(i, 1, 9) + 0.5f) * w).toInt(), ((noise(i, 2, 9) + 0.5f) * h * 0.5f).toInt(), rgb(230, 230, 210))
        val ox = (cx - s.ax).toInt()
        val oy = (h * 0.84f - s.ay).toInt()
        px.blit(s.px, ox, oy)
        // A dark ring round the window.
        val ring = BooleanArray(w * h) { px.c[it].al() > 0 }
        px.ring(ring, rgb(40, 34, 40))
        return px
    }

    /** The order the pixels come in: in soft patches, as if dabbed on with a brush. */
    private fun order(px: Px): List<Int> = px.c.indices.filter { px.c[it].al() > 0 }.sortedBy { i ->
        val x = i % px.w
        val y = i / px.w
        noise(x / 5, y / 5, 17) * 2f + noise(x, y, 18)
    }

    /** The portrait as far as [strokes] have painted it; the rest is a faint pencil ghost on paper. */
    fun paint(b: Beast, strokes: List<Stroke>): Px {
        val src = full(b)
        val ord = order(src)
        val out = Px(src.w, src.h)
        // The ghost: paper inside the window, the outline in pencil.
        for (i in ord) {
            val c = src.c[i]
            out.c[i] = if (lum(c) < 0.22f) mixc(paper, rgb(120, 110, 100), 0.45f) else paper
        }
        val total = strokes.sumOf { it.units }.coerceAtMost(Keeper.WHOLE)
        val n = (ord.size.toLong() * total / Keeper.WHOLE).toInt()
        var done = 0
        var acc = 0
        for (s in strokes) {
            acc += s.units
            val upTo = (ord.size.toLong() * acc.coerceAtMost(Keeper.WHOLE) / Keeper.WHOLE).toInt()
            val tint = tint(s.part)
            while (done < upTo && done < n) {
                val i = ord[done]
                out.c[i] = mixc(src.c[i], tint, 0.26f)
                done++
            }
        }
        return out
    }
}

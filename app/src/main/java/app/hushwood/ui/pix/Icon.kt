package app.hushwood.ui.pix

import app.hushwood.domain.Beast
import app.hushwood.domain.Spot
import kotlin.math.hypot

/** The app's icon: you on a stump under the moon, a fox asleep beside you, fireflies in the grass, in a round window. */
object Icon {
    const val SIZE = 72

    fun paint(): Px {
        val px = Px(SIZE, SIZE)
        val c = SIZE / 2f
        val r = c - 1.5f
        val top = rgb(18, 24, 60)
        val low = rgb(74, 82, 136)
        val ground = Ramp(rgb(14, 30, 30), rgb(22, 44, 40), rgb(32, 60, 50), rgb(46, 78, 60))
        val horizon = 46
        fun inside(x: Int, y: Int) = hypot(x + 0.5f - c, y + 0.5f - c) <= r
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            if (!inside(x, y)) continue
            val t = (y / horizon.toFloat()).coerceIn(0f, 1f)
            val q = ((t + bayer(x, y) * 0.1f) * 7).toInt() / 7f
            px.set(x, y, mixc(top, low, q))
        }
        for (i in 0 until 26) {
            val x = ((noise(i, 1, 41) + 0.5f) * SIZE).toInt()
            val y = ((noise(i, 2, 42) + 0.5f) * horizon * 0.8f).toInt()
            if (inside(x, y)) px.set(x, y, mixc(low, rgb(250, 246, 220), 0.5f + (noise(i, 3, 43) + 0.5f) * 0.5f))
        }
        // The moon, with its glow.
        px.glow(52f, 17f, 15f, rgb(170, 180, 230), 70, 4)
        for (y in 9..25) for (x in 44..60) {
            val d = hypot(x + 0.5f - 52f, y + 0.5f - 17f)
            if (d > 6.5f) continue
            val shade = hypot(x + 0.5f - 49.5f, y + 0.5f - 15.5f) < 6f
            px.set(x, y, if (shade) rgb(56, 66, 108) else if (noise(x / 2, y / 2, 5) > 0.25f) rgb(220, 218, 196) else rgb(248, 244, 220))
        }
        // A row of spruces, dark against the sky, and the meadow below.
        var x = -2f
        var i = 0
        while (x < SIZE + 3) {
            val h = 11f + (noise(i, 4, 44) + 0.5f) * 8f
            Clearing.spruceTree(px, x, horizon.toFloat(), h, h * 0.45f, Ramp(rgb(10, 22, 30), rgb(14, 30, 38), rgb(20, 40, 46), rgb(28, 52, 54), rgb(36, 62, 60)), null, false, 300 + i)
            x += 5f + (noise(i, 5, 45) + 0.5f) * 3f
            i++
        }
        for (y in horizon until SIZE) for (xx in 0 until SIZE) {
            if (!inside(xx, y)) continue
            val l = 0.35f + (y - horizon) / 40f + noise(xx, y / 2, 46) * 0.3f
            px.set(xx, y, Clearing.tone(ground, l, xx, y))
        }
        // The stump, you on it, the fox curled up beside.
        val sx = 25f
        val sy = 52f
        px.shade(rect(sx - 8f, sy, 16f, 9f), column(sx, 8f), Ramp(rgb(40, 24, 26), rgb(70, 42, 36), rgb(104, 66, 48), rgb(140, 96, 64)), hi = 3)
        px.shade(ellipse(sx, sy, 8.5f, 2.6f), facing(0f, -1f, 0.3f), Ramp(rgb(120, 84, 58), rgb(170, 126, 86), rgb(206, 168, 118)), lo = 0)
        val night = { p: Px -> Px(p.w, p.h).also { o -> for (k in p.c.indices) { val v = p.c[k]; if (v.al() > 0) o.c[k] = (v and 0xFF000000.toInt()) or (mixc(v, mixc(v, rgb(60, 70, 130), 0.45f), 0.6f) and 0xFFFFFF) } } }
        val you = Things.sitter(Spot.STUMP, 0f, 0)
        px.blit(night(you.px), (sx - you.ax).toInt(), (sy - you.ay).toInt() + 1)
        val fox = Beasts.frame(Beast.FOX, 0, Pose.SLEEP)
        px.blit(night(fox.px), 50 - fox.ax, 63 - fox.ay)
        // Fireflies.
        for ((fx, fy) in listOf(12 to 50, 40 to 47, 58 to 52, 18 to 60, 63 to 44)) {
            px.glow(fx + 0.5f, fy + 0.5f, 3f, rgb(220, 240, 120), 120, 2)
            px.set(fx, fy, rgb(250, 255, 190))
        }
        // Nothing outside the window, then its rim.
        for (y in 0 until SIZE) for (x2 in 0 until SIZE) if (!inside(x2, y)) px.c[y * SIZE + x2] = 0
        // The rim of the window.
        val solid = BooleanArray(SIZE * SIZE) { px.c[it].al() > 0 }
        px.ring(solid, rgb(14, 20, 22))
        return px
    }
}

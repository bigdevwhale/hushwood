package app.hushwood.ui.pix

import app.hushwood.domain.Year
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * A year as the cut face of a stump: twelve rings from the heart (January) to the bark (December). Each day is
 * a sliver of its month's ring, darker and wider-grained the longer you sat that day; a quiet day is pale wood.
 */
object Rings {
    private val pale = Ramp(rgb(176, 136, 96), rgb(206, 170, 124), rgb(226, 196, 152), rgb(240, 218, 180))
    private val dark = Ramp(rgb(70, 38, 30), rgb(110, 62, 40), rgb(146, 90, 54), rgb(180, 120, 72))
    private val bark = Ramp(rgb(40, 26, 26), rgb(66, 44, 36), rgb(96, 66, 50))

    fun paint(year: Year, size: Int = 96): Px {
        val px = Px(size, size)
        val c = size / 2f
        val r = size / 2f - 1
        val heart = r * 0.08f
        val barkW = r * 0.1f
        val ringW = (r - heart - barkW) / 12f
        val most = year.most.coerceAtLeast(1)
        for (y in 0 until size) for (x in 0 until size) {
            // A slightly wobbly trunk, as real wood is.
            val a = atan2(y + 0.5f - c, x + 0.5f - c)
            val wob = 1f + 0.04f * kotlin.math.sin(a * 3 + 1) + 0.02f * kotlin.math.sin(a * 7)
            val d = hypot(x + 0.5f - c, y + 0.5f - c) / wob
            if (d > r) continue
            if (d > r - barkW) {
                px.set(x, y, bark[((noise(x, y, 3) + 0.5f) * 3).toInt().coerceIn(0, 2)])
                continue
            }
            if (d < heart) { px.set(x, y, dark[1]); continue }
            val m = ((d - heart) / ringW).toInt().coerceIn(0, 11)
            val inRing = ((d - heart) / ringW) - m
            // The day of the month from the angle, clockwise from the top.
            val turn = ((a + Math.PI / 2).let { if (it < 0) it + 2 * Math.PI else it } / (2 * Math.PI)).toFloat()
            val len = LocalDate.of(year.year, m + 1, 1).lengthOfMonth()
            val day = (turn * len).toInt().coerceIn(0, len - 1) + 1
            val mins = year.minutes[LocalDate.of(year.year, m + 1, day)] ?: 0
            val k = (mins / most.toFloat()).coerceIn(0f, 1f)
            // The ring's late wood: a thin dark line at its outer edge.
            val late = inRing > 0.82f
            var col = if (mins > 0) mixc(dark[3], dark[0], 0.25f + k * 0.75f) else mixc(pale[2], pale[1], noise(x / 2, y / 2, 5) + 0.5f)
            if (late) col = mixc(col, dark[0], 0.5f)
            if (mins > 0 && bayer(x, y) > 0.35f - k * 0.3f && !late) col = mixc(col, dark[0], 0.25f)
            px.set(x, y, col)
        }
        // A crack from the heart, as stumps have.
        px.line((c + 1).toInt(), (c).toInt(), (c + r * 0.45f).toInt(), (c - r * 0.3f).toInt(), dark[0])
        return px
    }
}

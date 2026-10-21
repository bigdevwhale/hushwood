package app.hushwood.ui.pix

import app.hushwood.domain.Daypart
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.domain.Spot
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The colours of one hour: the sky from top to horizon, the haze that swallows the far things, and a grade laid
 * over everything on the ground (dim and blue at night, rosy at dawn, amber at dusk).
 */
class Light(
    val top: Int,
    val mid: Int,
    val low: Int,
    val haze: Int,
    val mul: FloatArray,
    val tint: Int,
    val tintK: Float,
    val sat: Float,
) {
    fun grade(c: Int): Int {
        val l = lum(c) * 255f
        val r = (l + (c.r() - l) * sat) * mul[0]
        val g = (l + (c.g() - l) * sat) * mul[1]
        val b = (l + (c.b() - l) * sat) * mul[2]
        val k = tintK
        return (c and 0xFF000000.toInt()) or (rgb(
            (r + (tint.r() - r) * k).roundToInt().coerceIn(0, 255),
            (g + (tint.g() - g) * k).roundToInt().coerceIn(0, 255),
            (b + (tint.b() - b) * k).roundToInt().coerceIn(0, 255),
        ) and 0xFFFFFF)
    }

    companion object {
        fun of(p: Daypart): Light = when (p) {
            Daypart.DAWN -> Light(rgb(84, 92, 150), rgb(200, 152, 176), rgb(252, 206, 168), rgb(236, 196, 192), floatArrayOf(1f, 0.92f, 0.94f), rgb(255, 176, 160), 0.12f, 0.82f)
            Daypart.DAY -> Light(rgb(112, 160, 212), rgb(164, 200, 228), rgb(222, 232, 232), rgb(206, 222, 226), floatArrayOf(1f, 1f, 1f), rgb(255, 250, 230), 0.04f, 0.92f)
            Daypart.DUSK -> Light(rgb(60, 54, 112), rgb(176, 100, 126), rgb(250, 164, 100), rgb(214, 140, 128), floatArrayOf(1f, 0.84f, 0.8f), rgb(255, 128, 92), 0.15f, 0.78f)
            Daypart.NIGHT -> Light(rgb(10, 14, 36), rgb(22, 32, 68), rgb(42, 58, 96), rgb(38, 50, 86), floatArrayOf(0.42f, 0.49f, 0.72f), rgb(30, 48, 104), 0.2f, 0.5f)
        }
    }
}

/**
 * Where things are in a picture [w] x [h] of [place]. Everything is designed on a grid 180 units wide and
 * measured up from the bottom edge, then scaled by [k] to the picture's real pixels.
 */
class Layout(val w: Int, val h: Int, val place: Place = Place.FOREST) {
    val k = w / 180f

    fun x(u: Float): Int = (u * k).roundToInt()
    fun y(up: Float): Int = h - (up * k).roundToInt()
    fun x(u: Int) = x(u.toFloat())
    fun y(up: Int) = y(up.toFloat())

    val horizon = y(
        when (place) {
            Place.FOREST -> 128f
            Place.LAKE -> 132f
            Place.MOUNTAINS -> 124f
            Place.SEA -> 112f
        }
    )
    val meadow = horizon + (10 * k).toInt()

    /** 0 at the far edge of the ground, 1 at the bottom of the picture. */
    fun depth(y: Int) = ((y - meadow).toFloat() / (h - meadow)).coerceIn(0f, 1f)

    /** The stream's middle and half-width on row [y], in pixels: out of the trees on the right, out of the bottom. */
    fun stream(y: Int): Pair<Float, Float> {
        val d = depth(y)
        // In the forest it comes out of the trees and bends away, leaving past the right edge; in the mountains it runs down.
        val c = if (place == Place.MOUNTAINS) 150f - 18f * d + 6f * sin(d * 6f) else 132f + 62f * Math.pow(d.toDouble(), 1.25).toFloat() + 6f * sin(d * 7f)
        return c * k to (1.6f + d * (if (place == Place.MOUNTAINS) 6f else 9f)) * k
    }

    /** The path from the bottom edge up into the distance. */
    fun path(y: Int): Pair<Float, Float> {
        val d = depth(y)
        val c = when (place) {
            Place.MOUNTAINS -> 70f - 26f * d + 10f * sin(d * 6f)
            Place.SEA -> 112f - 20f * d
            else -> 100f - 10f * d + 9f * sin(d * 4.2f + 0.6f)
        }
        return c * k to (1f + d * 9f) * k
    }

    /** The lake's near shore, as the row (in pixels) where water gives way to land at column [x]. */
    fun shore(x: Int): Int {
        val u = x / k
        return y(58f + 26f * (u / 180f) + 6f * sin(u / 19f))
    }

    /** The sea's tide line. */
    fun tide(x: Int): Int {
        val u = x / k
        return y(46f + 18f * (u / 180f) + 3f * sin(u / 13f))
    }

    /** Where each practice's place is in this picture, in pixels: where you sit or stand. */
    fun spot(s: Spot): Pair<Int, Int> {
        val (u, up) = when (place) {
            Place.FOREST -> when (s) {
                Spot.STUMP -> 64f to 46f
                Spot.STREAM -> 146f to 52f
                Spot.STONE -> 34f to 82f
                Spot.FIRE -> 87f to 65f
                Spot.PATH -> 98f to 96f
            }
            Place.LAKE -> when (s) {
                Spot.STUMP -> 52f to 34f
                Spot.STREAM -> 150f to 50f
                Spot.STONE -> 26f to 66f
                Spot.FIRE -> 92f to 26f
                Spot.PATH -> 86f to 56f
            }
            Place.MOUNTAINS -> when (s) {
                Spot.STUMP -> 62f to 50f
                Spot.STREAM -> 118f to 38f
                Spot.STONE -> 34f to 90f
                Spot.FIRE -> 92f to 72f
                Spot.PATH -> 70f to 104f
            }
            Place.SEA -> when (s) {
                Spot.STUMP -> 76f to 36f
                Spot.STREAM -> 128f to 26f
                Spot.STONE -> 26f to 92f
                Spot.FIRE -> 44f to 54f
                Spot.PATH -> 104f to 76f
            }
        }
        return x(u) to y(up)
    }

    /** The fire ring, in pixels. */
    val fire: Pair<Int, Int>
        get() = when (place) {
            Place.FOREST -> x(112) to y(66)
            Place.LAKE -> x(116) to y(28)
            Place.MOUNTAINS -> x(116) to y(72)
            Place.SEA -> x(68) to y(56)
        }
}

/**
 * Paints the places, still: sky, the far shapes in layers of haze, the ground with its water and path, and the
 * spots where you practise. What moves (water glints, grass, the animals) is drawn over it every frame.
 */
object Clearing {
    val spruce = Ramp(rgb(12, 28, 32), rgb(18, 42, 42), rgb(26, 60, 50), rgb(38, 80, 58), rgb(56, 102, 66), rgb(82, 126, 78))
    val bark = Ramp(rgb(70, 66, 76), rgb(150, 144, 140), rgb(206, 200, 188), rgb(236, 232, 220), rgb(252, 250, 242))
    val dirt = Ramp(rgb(52, 36, 34), rgb(86, 62, 48), rgb(124, 92, 64), rgb(160, 124, 84), rgb(192, 160, 112))
    val sand = Ramp(rgb(120, 98, 76), rgb(168, 142, 106), rgb(206, 182, 140), rgb(230, 212, 172), rgb(246, 236, 206))
    val stone = Ramp(rgb(40, 40, 52), rgb(68, 68, 80), rgb(100, 100, 108), rgb(134, 132, 136), rgb(170, 168, 166), rgb(204, 202, 196))
    val rock = Ramp(rgb(46, 40, 50), rgb(74, 66, 74), rgb(106, 96, 98), rgb(140, 128, 124), rgb(176, 164, 152), rgb(210, 200, 186))
    val wood = Ramp(rgb(46, 26, 30), rgb(80, 46, 40), rgb(118, 74, 52), rgb(158, 108, 70), rgb(196, 148, 98), rgb(226, 190, 140))
    val drift = Ramp(rgb(70, 62, 62), rgb(112, 102, 96), rgb(152, 142, 130), rgb(188, 180, 164), rgb(220, 214, 198))
    val moss = Ramp(rgb(30, 54, 36), rgb(52, 84, 44), rgb(84, 116, 54), rgb(124, 150, 70))
    val snow = Ramp(rgb(112, 124, 156), rgb(150, 164, 192), rgb(190, 202, 222), rgb(222, 230, 242), rgb(246, 248, 252))

    fun grass(s: Season) = when (s) {
        Season.SPRING -> Ramp(rgb(38, 70, 50), rgb(60, 102, 58), rgb(92, 138, 68), rgb(134, 172, 86), rgb(180, 202, 116))
        Season.SUMMER -> Ramp(rgb(26, 54, 42), rgb(42, 82, 50), rgb(66, 112, 58), rgb(100, 144, 70), rgb(146, 176, 94))
        Season.AUTUMN -> Ramp(rgb(66, 52, 40), rgb(104, 82, 48), rgb(146, 114, 58), rgb(186, 146, 76), rgb(218, 184, 110))
        Season.WINTER -> snow
    }

    fun leaves(s: Season) = when (s) {
        Season.SPRING -> Ramp(rgb(46, 86, 48), rgb(84, 132, 58), rgb(130, 176, 76), rgb(180, 210, 110), rgb(222, 236, 160))
        Season.SUMMER -> Ramp(rgb(24, 56, 40), rgb(42, 88, 50), rgb(70, 122, 60), rgb(110, 158, 76), rgb(160, 192, 104))
        Season.AUTUMN -> Ramp(rgb(110, 54, 30), rgb(170, 96, 34), rgb(214, 148, 46), rgb(240, 194, 76), rgb(252, 230, 140))
        Season.WINTER -> Ramp(rgb(60, 50, 50), rgb(90, 76, 70), rgb(120, 102, 90), rgb(150, 132, 116), rgb(180, 162, 144))
    }

    /** Index into a ramp from a light level, dithered. */
    fun tone(ramp: Ramp, l: Float, x: Int, y: Int, d: Float = 0.6f) = ramp[(l * (ramp.size - 1) + bayer(x, y) * d).roundToInt().coerceIn(0, ramp.size - 1)]

    fun fade(r: Ramp, to: Int, t: Float) = Ramp(*IntArray(r.size) { mixc(r[it], to, t) })

    /* ---------- The sky ---------- */

    fun sky(px: Px, lay: Layout, p: Daypart, light: Light) {
        val k = lay.k
        val hz = lay.horizon + (6 * k).toInt()
        for (y in 0 until min(px.h, hz + (40 * k).toInt())) {
            val t = (y.toFloat() / hz).coerceIn(0f, 1f)
            for (x in 0 until px.w) {
                // Ordered dither across ten bands, the way an old game would ramp a sky.
                val tt = (t + bayer(x, y) * 0.08f).coerceIn(0f, 1f)
                val q = (tt * 10).roundToInt() / 10f
                val c = if (q < 0.55f) mixc(light.top, light.mid, q / 0.55f) else mixc(light.mid, light.low, (q - 0.55f) / 0.45f)
                px.set(x, y, c)
            }
        }
        when (p) {
            Daypart.NIGHT -> {
                val star = rgb(240, 236, 214)
                for (i in 0 until (110 * k).toInt()) {
                    val x = ((noise(i, 3, 11) + 0.5f) * px.w).toInt()
                    val y = ((noise(i, 7, 12) + 0.5f) * hz * 0.85f).toInt()
                    val b = noise(i, 1, 13) + 0.5f
                    px.set(x, y, mixc(light.mid, star, 0.3f + b * 0.65f))
                    if (b > 0.9f) for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) px.set(x + dx, y + dy, mixc(light.mid, star, 0.4f))
                }
                // A faint band of the Milky Way.
                for (y in 0 until (hz * 0.7f).toInt()) for (x in 0 until px.w) {
                    val band = abs(y - (px.w - x) * 0.55f - hz * 0.05f) / (22f * k)
                    if (band < 1f && smooth(x / (5f * k), y / (5f * k), 14) + bayer(x, y) * 0.35f > 0.05f + band * 0.35f) px.set(x, y, rgb(120, 130, 180), (46 * (1 - band)).toInt())
                }
                moon(px, 136f * k, hz * 0.2f, 7f * k)
            }
            Daypart.DAWN -> {
                sun(px, 40f * k, hz - 6f * k, 9f * k, rgb(255, 230, 190), rgb(255, 196, 160))
                clouds(px, lay, light, rgb(255, 214, 196), 0.5f, 1)
            }
            Daypart.DUSK -> {
                sun(px, 150f * k, hz - 4f * k, 10f * k, rgb(255, 214, 140), rgb(255, 150, 96))
                clouds(px, lay, light, rgb(255, 176, 130), 0.55f, 2)
            }
            Daypart.DAY -> clouds(px, lay, light, rgb(250, 252, 255), 0.7f, 3)
        }
    }

    private fun sun(px: Px, cx: Float, cy: Float, r: Float, core: Int, halo: Int) {
        px.glow(cx, cy, r * 3.2f, halo, 70, 5)
        px.glow(cx, cy, r * 1.8f, halo, 90, 3)
        for (y in (cy - r).toInt()..(cy + r).toInt()) for (x in (cx - r).toInt()..(cx + r).toInt()) {
            val d = kotlin.math.hypot(x + 0.5f - cx, y + 0.5f - cy)
            if (d <= r) px.set(x, y, if (d > r - 1.2f) mixc(core, halo, 0.5f) else core)
        }
    }

    private fun moon(px: Px, cx: Float, cy: Float, r: Float) {
        px.glow(cx, cy, r * 3f, rgb(120, 140, 190), 60, 5)
        for (y in (cy - r - 1).toInt()..(cy + r + 1).toInt()) for (x in (cx - r - 1).toInt()..(cx + r + 1).toInt()) {
            val d = kotlin.math.hypot(x + 0.5f - cx, y + 0.5f - cy)
            if (d > r) continue
            // A waxing crescent's shadow on the left, and soft seas.
            val dark = kotlin.math.hypot(x + 0.5f - (cx - r * 0.5f), y + 0.5f - (cy - r * 0.15f)) < r * 0.92f
            var c = if (dark) rgb(46, 58, 92) else rgb(240, 236, 210)
            if (!dark && noise(x / 2, y / 2, 5) > 0.22f) c = rgb(212, 210, 192)
            if (!dark && d > r - 1.2f) c = rgb(250, 248, 230)
            px.set(x, y, c)
        }
    }

    private fun clouds(px: Px, lay: Layout, light: Light, lit: Int, kk: Float, salt: Int) {
        val k = lay.k
        val hz = lay.horizon
        repeat(5) { i ->
            val cy = hz * (0.14f + 0.15f * i) + noise(i, salt, 3) * 10f * k
            val cx = (noise(i, salt, 4) + 0.5f) * px.w
            val len = (24f + (noise(i, salt, 5) + 0.5f) * 40f) * k
            val th = 4.5f * k
            for (y in (cy - th * 1.4f).toInt()..(cy + th).toInt()) for (x in (cx - len).toInt()..(cx + len).toInt()) {
                val u = (x - cx) / len
                val v = (y - cy) / th
                val puff = 1f - u * u - v * v * (if (v > 0) 2.2f else 0.8f) + noise(x / 4, y / 2, salt + i) * 0.5f
                if (puff <= 0f) continue
                val base = px[x, y]
                val c = if (v > 0.25f) mixc(base, light.mid, 0.4f) else mixc(base, lit, kk * (0.6f + 0.4f * (1 - v)))
                if (puff + bayer(x, y) * 0.4f > 0.2f) px.set(x, y, c)
            }
        }
    }

    /* ---------- Trees and bushes ---------- */

    /** One spruce from its base (pixels), lit from the left, needles roughened, snow on its tiers in winter. */
    fun spruceTree(px: Px, bx: Float, by: Float, h: Float, w: Float, ramp: Ramp, outline: Int?, snowy: Boolean, salt: Int) {
        val top = by - h
        val tiers = max(3, (h / 9f).roundToInt())
        val tierH = h / tiers
        for (y in top.toInt()..by.toInt()) {
            val t = ((y - top) / h).coerceIn(0f, 1f)
            val inTier = ((y - top) % tierH) / tierH
            val hw = (w / 2f) * (0.12f + 0.88f * t) * (0.5f + 0.5f * inTier) + noise(y, salt, 9) * 0.9f
            if (hw < 0.4f) { px.set(bx.toInt(), y, ramp[2]); continue }
            for (x in (bx - hw).toInt()..(bx + hw).toInt()) {
                val u = (x + 0.5f - bx) / hw
                // Needle clumps: drooping strokes catching the light at their tips.
                var l = 0.55f - u * 0.4f - inTier * 0.25f + noise(x, y, salt) * 0.35f
                if ((x + y * 2) % 5 == 0 && inTier > 0.5f) l += 0.18f
                if (abs(u) > 0.92f) l -= 0.25f
                var c = tone(Ramp(*IntArray(ramp.size - 1) { ramp[it] }), l, x, y)
                if (snowy && inTier < 0.32f && u < 0.55f && noise(x, y, salt + 4) > -0.2f) c = if (u < -0.2f) snow[4] else snow[3]
                px.set(x, y, c)
            }
            if (outline != null) {
                px.set((bx - hw).toInt() - 1, y, outline, 120)
                px.set((bx + hw).toInt() + 1, y, outline, 120)
            }
        }
        px.rect((bx - 1).toInt(), by.toInt(), 2, 3, mixc(ramp[0], wood[1], 0.5f))
    }

    /** A birch (pixels): white trunk with dark marks, drooping branches and a loose crown at the top (bare in winter). */
    fun birch(px: Px, bx: Float, by: Float, h: Float, tw: Float, s: Season, salt: Int, crown: Boolean = true) {
        val top = by - h
        val leaves = leaves(s)
        val k = (h / 160f).coerceIn(0.35f, 1.6f)
        for (i in 0 until 9) {
            val y0 = top + h * (0.04f + 0.05f * i)
            val dir = if (i % 2 == 0) 1 else -1
            val len = (10f + (noise(i, salt, 1) + 0.5f) * 14f) * k
            val mx = bx + dir * len * 0.6f
            val my = y0 - len * 0.35f
            px.line(bx.toInt(), y0.toInt(), mx.toInt(), my.toInt(), bark[0])
            px.line(mx.toInt(), my.toInt(), (bx + dir * len).toInt(), (my + 3 * k).toInt(), bark[0])
        }
        for (y in top.toInt()..by.toInt()) {
            val t = (y - top) / h
            val hw = tw / 2f * (0.45f + 0.55f * t)
            for (x in (bx - hw - 0.5f).toInt()..(bx + hw).toInt()) {
                val u = (x + 0.5f - bx) / max(0.6f, hw)
                if (abs(u) > 1.1f) continue
                var i = if (u < -0.25f) 4 else if (u < 0.3f) 3 else 2
                if (u > 0.75f) i = 1
                val mark = noise(x / 4, y, salt + 2) > 0.34f - t * 0.1f && (y % 3 != 0)
                val foot = t > 0.92f && noise(x, y, salt + 3) > -0.1f
                px.set(x, y, if (mark || foot) bark[0] else bark[i])
            }
        }
        if (!crown) return
        if (s == Season.WINTER) {
            for (i in 0 until (40 * k).toInt()) {
                val x = bx + noise(i, salt, 4) * 40f * k
                val y = top + (noise(i, salt, 5) + 0.5f) * h * 0.5f
                px.set(x.toInt(), y.toInt(), rgb(96, 70, 82), 160)
            }
            return
        }
        for (i in 0 until 14) {
            val cx = bx + noise(i, salt, 6) * 34f * k
            val cy = top + h * (0.02f + 0.035f * i) + noise(i, salt, 7) * 6f * k
            val r = (6f + (noise(i, salt, 8) + 0.5f) * 5f) * (0.5f + 0.5f * k)
            leafBlob(px, cx, cy, r, r * 0.75f, leaves, salt + i)
        }
    }

    /** A rounded clump of leaves, lit from above left, with ragged edges and leaf-sized flecks. */
    fun leafBlob(px: Px, cx: Float, cy: Float, rx: Float, ry: Float, ramp: Ramp, salt: Int, outline: Boolean = false) {
        for (y in (cy - ry - 2).toInt()..(cy + ry + 2).toInt()) for (x in (cx - rx - 2).toInt()..(cx + rx + 2).toInt()) {
            val u = (x + 0.5f - cx) / rx
            val v = (y + 0.5f - cy) / ry
            val d = u * u + v * v + noise(x / 2, y / 2, salt) * 0.9f
            if (d > 1f) continue
            var l = 0.6f - u * 0.3f - v * 0.45f + noise(x, y, salt + 1) * 0.5f
            if (noise(x / 2, y / 2, salt + 2) > 0.3f) l -= 0.25f
            px.set(x, y, if (outline && d > 0.84f) ramp[0] else tone(ramp, l, x, y, 0.7f))
        }
    }

    /** A rounded rock (pixels), stratified, with moss or snow on top. */
    fun boulder(px: Px, cx: Float, cy: Float, rx: Float, ry: Float, ramp: Ramp, s: Season, salt: Int) {
        px.shade(ellipse(cx, cy, rx, ry), dome(cx, cy - ry * 0.3f, rx, ry * 1.3f, 0.3f), ramp, texture = { x, y -> noise(x / 2, y, salt) * 0.2f + if ((y + x / 5) % 4 == 0) -0.12f else 0f }, rim = ramp[4])
        if (s == Season.WINTER) {
            for (y in (cy - ry).toInt()..(cy - ry * 0.3f).toInt()) for (x in (cx - rx * 0.85f).toInt()..(cx + rx * 0.85f).toInt()) {
                val u = (x - cx) / (rx * 0.85f)
                if (u * u + ((y - cy + ry * 0.6f) / (ry * 0.45f)).let { it * it } < 1f) px.set(x, y, snow[if (u < 0) 4 else 3])
            }
        } else for (i in 0 until (rx * 1.2f).toInt()) {
            val x = cx - rx * 0.8f + i * 1.4f
            px.set(x.toInt(), (cy + ry * 0.4f + (i % 3)).toInt(), moss[1 + i % 3])
        }
    }

    /* ---------- Ground ---------- */

    /** Grass (or snow) from row [from] down, hazier far away, with blades and flowers near. */
    fun meadow(px: Px, lay: Layout, from: Int, s: Season, haze: Int, keep: (Int, Int) -> Boolean = { _, _ -> false }, ramp: Ramp = grass(s)) {
        val winter = s == Season.WINTER
        for (y in from until lay.h) {
            val d = lay.depth(y)
            for (x in 0 until lay.w) {
                if (keep(x, y)) continue
                val blade = noise(x, y / (1 + (d * 3).toInt()), 41)
                var l = 0.42f + d * 0.25f + blade * (0.25f + d * 0.3f) + noise(x / 9, y / 5, 42) * 0.3f
                if (!winter && (y + x / 2) % 3 == 0 && blade > 0.2f) l += 0.2f
                px.set(x, y, mixc(tone(ramp, l, x, y), haze, (1 - d) * 0.45f))
            }
        }
        if (!winter) flowers(px, lay, s, from)
    }

    private fun flowers(px: Px, lay: Layout, s: Season, from: Int) {
        val colors = when (s) {
            Season.SPRING -> listOf(rgb(250, 246, 220), rgb(250, 220, 90), rgb(170, 180, 250))
            Season.SUMMER -> listOf(rgb(250, 246, 230), rgb(214, 120, 170), rgb(120, 150, 240), rgb(250, 210, 80))
            else -> listOf(rgb(220, 110, 40), rgb(200, 70, 40), rgb(240, 180, 60))
        }
        val n = if (lay.place == Place.MOUNTAINS) 260 else 140
        for (i in 0 until (n * lay.k).toInt()) {
            val x = ((noise(i, 1, 81) + 0.5f) * lay.w).toInt()
            val y = from + 6 + ((noise(i, 2, 82) + 0.5f) * (lay.h - from - 8)).toInt()
            if (!px.has(x, y)) continue
            val c = colors[(i * 7) % colors.size]
            px.set(x, y, c)
            if (lay.depth(y) > 0.5f) { px.set(x, y + 1, mixc(c, rgb(30, 60, 40), 0.6f)); if (lay.depth(y) > 0.75f) px.set(x + 1, y, mixc(c, rgb(255, 255, 255), 0.3f)) }
        }
    }

    /** Soft mist lying along row [cy] (pixels), [hh] tall. */
    fun mist(px: Px, cy: Float, hh: Float, haze: Int, strength: Int, salt: Int) {
        for (y in (cy - hh).toInt()..(cy + hh).toInt()) for (x in 0 until px.w) {
            val t = 1f - abs(y - cy) / hh
            if (t > 0 && t * 0.9f + bayer(x, y) * 0.45f + smooth(x / 9f, y / 3f, salt) * 0.5f > 0.45f) px.set(x, y, haze, (t * strength).toInt())
        }
    }

    /** A row of spruces (pixels) across the picture at [base], faded into the haze by [fade]. */
    fun spruceRow(px: Px, lay: Layout, base: Float, minH: Float, varH: Float, fade: Float, haze: Int, winter: Boolean, salt: Int, gap: ClosedFloatingPointRange<Float>? = null, from: Float = 0f, to: Float = lay.w.toFloat()) {
        val ramp = fade(spruce, haze, fade)
        var x = from - 4f
        var i = 0
        while (x < to + 6) {
            val hh = minH + (noise(i, salt, 21) + 0.5f) * varH
            if (gap != null && x in gap) { x += 7f * lay.k; i++; continue }
            spruceTree(px, x, base + noise(i, salt, 23) * 2f * lay.k, hh, hh * 0.42f, ramp, null, winter, i * 7 + salt)
            x += (5f + (noise(i, salt, 22) + 0.5f) * 6f) * lay.k
            i++
        }
    }

    /** Water reflecting what's above [line] (pixels), from row [line] down while [inside] holds, with ripples. */
    fun reflect(px: Px, line: Int, rows: IntRange, water: Int, light: Light, inside: (Int, Int) -> Boolean, salt: Int) {
        val src = px.c.copyOf()
        for (y in rows) {
            val dy = y - line
            for (x in 0 until px.w) {
                if (!inside(x, y)) continue
                val wob = (sin(y * 0.9f + x * 0.05f) * (1 + dy / 30f)).roundToInt()
                val sy = (line - dy * 1.15f).toInt().coerceIn(0, px.h - 1)
                val sx = (x + wob).coerceIn(0, px.w - 1)
                var c = mixc(src[sy * px.w + sx], water, 0.42f + min(0.35f, dy / 160f))
                // Long glints on the ripples.
                if ((y % 3 == 0) && noise(x / 5, y, salt) > 0.28f) c = mixc(c, light.low, 0.35f)
                if ((y % 4 == 1) && noise(x / 7, y, salt + 1) < -0.3f) c = mixc(c, rgb(10, 20, 30), 0.15f)
                px.set(x, y, c)
            }
        }
    }

    /* ---------- The places ---------- */

    /** One place without anyone in it, for an hour and a season. */
    fun paint(w: Int, h: Int, p: Daypart, s: Season, place: Place = Place.FOREST): Px {
        val lay = Layout(w, h, place)
        val light = Light.of(p)
        val skyPx = Px(w, h)
        sky(skyPx, lay, p, light)
        val ground = Px(w, h)
        val water = IntArray(w * h)
        when (place) {
            Place.FOREST -> forest(ground, lay, light, s, water)
            Place.LAKE -> lake(ground, skyPx, lay, light, s, p)
            Place.MOUNTAINS -> mountains(ground, lay, light, s, water)
            Place.SEA -> sea(ground, skyPx, lay, light, s, p)
        }
        // Grade the ground for the hour and lay it over the sky.
        for (i in ground.c.indices) {
            val c = ground.c[i]
            if (c.al() == 0) continue
            val graded = light.grade(c)
            // Water only takes half of the hour's colour, so it stays water.
            val out = if (water[i] != 0 && water[i] == c) mixc(graded, c, if (p == Daypart.NIGHT) 0.3f else 0.5f) else graded
            ground.c[i] = (c and 0xFF000000.toInt()) or (out and 0xFFFFFF)
        }
        skyPx.blit(ground, 0, 0)
        if (place == Place.LAKE) {
            // The lake mirrors the finished sky and far shore.
            val line = lay.horizon + (14 * lay.k).toInt()
            val water = mixc(light.mid, rgb(20, 40, 60), if (p == Daypart.NIGHT) 0.2f else 0.45f)
            reflect(skyPx, line, line until h, water, light, { x, y -> y < lay.shore(x) && !ground.has(x, y) }, 401)
            shoreDetails(skyPx, lay, light, s, p)
        }
        return skyPx
    }

    private fun forest(g: Px, lay: Layout, light: Light, s: Season, water: IntArray) {
        val k = lay.k
        val hz = lay.horizon.toFloat()
        val haze = light.haze
        val winter = s == Season.WINTER
        for (x in 0 until lay.w) {
            val top = hz - 14 * k + (sin(x / (23f * k)) * 5 + sin(x / (9f * k) + 1) * 2) * k
            for (y in top.toInt() until (hz + 14 * k).toInt()) g.set(x, y, mixc(spruce[1], haze, 0.72f))
        }
        spruceRow(g, lay, hz + 6 * k, 18 * k, 10 * k, 0.62f, haze, winter, 0)
        mist(g, hz + 6 * k, 10 * k, haze, 150, 31)
        spruceRow(g, lay, hz + 12 * k, 26 * k, 14 * k, 0.4f, haze, winter, 1, gap = 94f * k..110f * k)
        mist(g, hz + 14 * k, 8 * k, haze, 120, 32)
        // Groves on both sides close the clearing in.
        val mid = fade(spruce, haze, 0.22f)
        for ((i, x) in listOf(-2f, 8f, 18f, 30f).withIndex()) spruceTree(g, x * k, hz + (26f + i * 3) * k, (44f + i * 5 - i * i * 3) * k, 20f * k, mid, null, winter, 140 + i)
        for ((i, x) in listOf(166f, 156f, 146f).withIndex()) spruceTree(g, x * k, hz + (22f + i * 2) * k, (50f - i * 7) * k, 20f * k, mid, null, winter, 150 + i)
        birch(g, 40f * k, hz + 24 * k, 52f * k, 3f * k, s, 160)
        birch(g, 154f * k, hz + 20 * k, 46f * k, 3f * k, s, 161)
        meadow(g, lay, lay.meadow - 2, s, haze, keep = { x, y -> g.has(x, y) && y <= hz + 40 * k })
        mist(g, lay.meadow + 4f * k, 8f * k, haze, 90, 43)
        path(g, lay, haze, winter)
        stream(g, lay, light, s, water)
        // Bushes at the edges, where the shy ones wait.
        val lv = leaves(if (winter) Season.AUTUMN else s)
        val bush = if (winter) Ramp(rgb(48, 40, 44), rgb(74, 62, 60), rgb(100, 86, 78), rgb(124, 110, 98)) else fade(lv, spruce[2], 0.45f)
        leafBlob(g, 22f * k, lay.y(98).toFloat(), 16f * k, 9f * k, bush, 71, true)
        leafBlob(g, 8f * k, lay.y(92).toFloat(), 12f * k, 10f * k, bush, 72, true)
        leafBlob(g, 132f * k, lay.y(118).toFloat(), 13f * k, 6f * k, fade(bush, haze, 0.25f), 73, true)
        // Thickets in the bottom right, below where the stream leaves.
        leafBlob(g, 170f * k, lay.y(18).toFloat(), 22f * k, 13f * k, bush, 74, true)
        leafBlob(g, 150f * k, lay.y(8).toFloat(), 14f * k, 8f * k, bush, 75, true)
        boulder(g, 34f * k, lay.y(76).toFloat(), 18f * k, 7f * k, stone, s, 5)
        fireRing(g, lay, s)
        stump(g, lay.x(64).toFloat(), lay.y(46).toFloat(), k, s)
        // A birch on the left frames the picture; the stream keeps the right side open.
        birch(g, 10f * k, lay.y(10).toFloat(), lay.h - 30f * k, 9f * k, s, 93)
        // The owl's branch.
        g.line(lay.x(13), lay.y(150), lay.x(34), lay.y(156), bark[0])
        g.line(lay.x(13), lay.y(151), lay.x(32), lay.y(157), bark[1])
    }

    private fun lake(g: Px, sky: Px, lay: Layout, light: Light, s: Season, p: Daypart) {
        val k = lay.k
        val hz = lay.horizon.toFloat()
        val haze = light.haze
        val winter = s == Season.WINTER
        // Far hills and the far shore's forest, low over the water.
        for (x in 0 until lay.w) {
            val top = hz - 22 * k + (sin(x / (31f * k)) * 8 + sin(x / (11f * k) + 2) * 3) * k
            for (y in top.toInt() until (hz + 10 * k).toInt()) g.set(x, y, mixc(spruce[1], haze, 0.75f))
        }
        spruceRow(g, lay, hz + 8 * k, 12 * k, 8 * k, 0.55f, haze, winter, 2)
        birch(g, 70f * k, hz + 9 * k, 22f * k, 1.5f * k, s, 170)
        mist(g, hz + 8 * k, 6 * k, haze, 140, 33)
        // The water is left empty here; it's filled by reflecting the finished picture.
        val line = (hz + 14 * k).toInt()
        // The near shore: grass with a sandy edge, the reeds.
        meadow(g, lay, line, s, haze, keep = { x, y -> y < lay.shore(x) })
        for (x in 0 until lay.w) {
            val sy = lay.shore(x)
            for (j in 0 until (3 * k).toInt() + 2) g.set(x, sy + j, tone(if (winter) snow else sand, 0.5f - j * 0.1f + noise(x, j, 9) * 0.3f, x, sy + j))
        }
        if (!winter) reedsAt(g, lay, s, (0 until 20).map { i -> val x = ((noise(i, 1, 501) + 0.5f) * 0.55f * lay.w).toInt(); x to lay.shore(x) + 1 })
        path(g, lay, haze, winter, sandy = true)
        boulder(g, 24f * k, lay.y(62).toFloat(), 14f * k, 7f * k, stone, s, 7)
        boulder(g, 14f * k, lay.y(54).toFloat(), 7f * k, 4f * k, stone, s, 8)
        fireRing(g, lay, s)
        log(g, lay.x(52).toFloat(), lay.y(32).toFloat(), 26f * k, 4f * k, wood, s)
        // Birches leaning over the water on the left, a spruce on the right.
        birch(g, 6f * k, lay.y(30).toFloat(), lay.h - 70f * k, 7f * k, s, 171)
        spruceTree(g, lay.w + 4f, lay.y(16).toFloat(), 120f * k, 46f * k, spruce, spruce[0], winter, 92)
    }

    /** The pier, drawn over the reflected water. */
    private fun shoreDetails(px: Px, lay: Layout, light: Light, s: Season, p: Daypart) {
        val k = lay.k
        val plank = Ramp(*IntArray(wood.size) { light.grade(wood[it]) })
        val y0 = lay.y(48)
        val x0 = lay.x(128)
        // Posts into the water, then the planks.
        for (i in 0 until 4) {
            val x = x0 + (i * 15 * k).toInt()
            px.shade(rect(x.toFloat(), y0.toFloat(), 2.2f * k, 9f * k), column(x + 1.1f * k, 1.1f * k), plank, hi = 3)
        }
        for (x in x0 - (2 * k).toInt() until lay.w) for (j in 0 until (5 * k).toInt()) {
            val y = y0 - (5 * k).toInt() + j
            val seam = (x - x0) % (6 * k).toInt().coerceAtLeast(3) == 0
            px.set(x, y, if (j == 0) plank[4] else if (seam) plank[1] else tone(plank, 0.6f - j * 0.08f + noise(x, y, 6) * 0.2f, x, y))
        }
        if (s == Season.WINTER) for (x in x0 until lay.w) px.set(x, y0 - (5 * k).toInt(), Clearing.snow[4])
        // Water lilies near the shore.
        if (s == Season.SUMMER) for (i in 0 until 9) {
            val x = lay.x(60 + i * 9)
            val y = lay.shore(x) - (6 + (i % 3) * 4) * k.toInt()
            if (!px.has(x, y)) continue
            for (dx in -2..2) px.set(x + dx, y, light.grade(rgb(70, 120, 60)))
            if (i % 2 == 0) { px.set(x, y - 1, light.grade(rgb(250, 240, 240))); px.set(x + 1, y - 1, light.grade(rgb(250, 220, 120))) }
        }
    }

    private fun mountains(g: Px, lay: Layout, light: Light, s: Season, water: IntArray) {
        val k = lay.k
        val hz = lay.horizon.toFloat()
        val haze = light.haze
        val winter = s == Season.WINTER
        // Three ranges of peaks, farther ones paler and snowier, lit on the faces turned to the left.
        val ranges = listOf(Triple(112f, 0.68f, 501), Triple(80f, 0.46f, 502), Triple(48f, 0.24f, 503))
        for ((ri, r) in ranges.withIndex()) {
            val (height, fade, salt) = r
            val ramp = fade(rock, haze, fade)
            val snowR = fade(snow, haze, fade * 0.7f)
            val ridge = FloatArray(lay.w + 2) { xi ->
                val u = (xi - 1) / k
                var top = 0f
                for (pk in 0 until 4) {
                    val p0 = (noise(pk, ri, salt) + 0.5f) * 200f - 10f
                    val ph = height * (0.55f + 0.45f * (noise(pk, ri, salt + 1) + 0.5f))
                    top = max(top, ph - abs(u - p0) * (1.1f + ri * 0.25f))
                }
                top + smooth(u / 4f, ri.toFloat(), salt + 2) * 6f + smooth(u / 1.5f, ri + 9f, salt + 3) * 2f
            }
            val bottom = hz + (8 + ri * 7) * k
            for (x in 0 until lay.w) {
                val rh = ridge[x + 1] * k
                val top = hz - rh + ri * 6 * k
                val slope = (ridge[x + 2] - ridge[x]) * 0.5f
                for (y in top.toInt() until bottom.toInt()) {
                    val depth = (y - top) / (rh + 1)
                    // Gullies run down the faces; the light catches the left-facing ones.
                    val gully = smooth(x / (2.6f * k), y / (12f * k), salt + 4)
                    var l = 0.5f + (slope * 0.9f).coerceIn(-0.32f, 0.32f) + gully * 0.3f * (1 - depth * 0.85f) - depth * 0.2f + smooth(x / (8f * k), y / (6f * k), salt + 5) * 0.15f
                    val snowLine = (if (winter) 0.9f else 0.36f - ri * 0.08f) + smooth(x / (3f * k), y / (5f * k), salt + 6) * 0.25f
                    val c = if (depth < snowLine && ridge[x + 1] > 30f) tone(snowR, l + 0.25f, x, y, 0.4f) else tone(ramp, l, x, y, 0.4f)
                    g.set(x, y, c)
                }
            }
            mist(g, bottom - 2 * k, 7f * k, haze, 140 - ri * 30, 510 + ri)
        }
        meadow(g, lay, (hz + 22 * k).toInt(), s, haze)
        // Spruces where the slopes meet the meadow.
        // Two groves, with the trail and the brook coming out between them.
        spruceRow(g, lay, hz + 26 * k, 14 * k, 10 * k, 0.3f, haze, winter, 3, from = 0f, to = 48f * k)
        spruceRow(g, lay, hz + 25 * k, 14 * k, 10 * k, 0.3f, haze, winter, 4, from = 166f * k)
        mist(g, hz + 24f * k, 5f * k, haze, 70, 515)
        // Scree and boulders.
        for (i in 0 until 9) {
            val x = (noise(i, 1, 520) + 0.5f) * lay.w
            val y = lay.y(30f + (noise(i, 2, 521) + 0.5f) * 70f).toFloat()
            boulder(g, x, y, (3f + (noise(i, 3, 522) + 0.5f) * 4f) * k, (2f + (noise(i, 4, 523) + 0.5f) * 2f) * k, rock, s, 530 + i)
        }
        boulder(g, 34f * k, lay.y(84).toFloat(), 18f * k, 9f * k, rock, s, 540)
        boulder(g, 62f * k, lay.y(44).toFloat(), 15f * k, 6f * k, rock, s, 541)
        path(g, lay, haze, winter)
        stream(g, lay, light, s, water)
        fireRing(g, lay, s)
        // A stone cairn on the right, prayer-flag colours on a cord.
        val cx = lay.x(164).toFloat()
        var cy = lay.y(70).toFloat()
        for (i in 0 until 5) {
            val r = (5f - i * 0.8f) * k
            boulder(g, cx + noise(i, 1, 550) * 2f, cy, r, r * 0.55f, stone, Season.SUMMER, 551 + i)
            cy -= r * 1.0f
        }
        val flags = listOf(rgb(60, 110, 200), rgb(240, 240, 230), rgb(200, 60, 50), rgb(70, 150, 80), rgb(240, 200, 60))
        for (i in 0 until 7) {
            val x = cx - (3 + i * 3) * k
            val y = cy + 2 * k + i * i * 0.18f * k
            g.rect(x.toInt(), y.toInt(), (2 * k).toInt(), (3 * k).toInt(), flags[i % 5])
        }
    }

    private fun sea(g: Px, sky: Px, lay: Layout, light: Light, s: Season, p: Daypart) {
        val k = lay.k
        val hz = lay.horizon
        val winter = s == Season.WINTER
        val haze = light.haze
        // The sea from the horizon to the tide line: sky-coloured, darker and bluer near, with rows of waves.
        val deep = mixc(light.top, rgb(20, 50, 70), 0.5f)
        for (y in hz until lay.h) for (x in 0 until lay.w) {
            val tide = lay.tide(x)
            if (y > tide) continue
            val t = (y - hz).toFloat() / (tide - hz).coerceAtLeast(1)
            var c = mixc(mixc(light.low, light.mid, 0.4f), deep, t * 0.8f + bayer(x, y) * 0.08f)
            val waveRow = (y - hz) % (2 + (t * 6).toInt()) == 0
            if (waveRow && noise(x / (2 + (t * 6).toInt()), y, 601) > 0.15f) c = mixc(c, light.low, 0.4f)
            if (y > tide - 3 * k) c = mixc(c, rgb(240, 246, 250), 0.6f - (tide - y) / (3 * k) * 0.5f)
            g.set(x, y, c)
        }
        // The sun or moon's path on the water.
        val glintX = if (p == Daypart.DUSK) 150f * k else if (p == Daypart.DAWN) 40f * k else 136f * k
        for (y in hz until hz + (40 * k).toInt()) for (x in (glintX - 12 * k).toInt()..(glintX + 12 * k).toInt()) {
            if (y > lay.tide(x)) continue
            if ((y % 2 == 0) && noise(x / 2, y, 602) > 0.1f + abs(x - glintX) / (14f * k)) g.set(x, y, mixc(light.low, rgb(255, 250, 230), 0.6f))
        }
        // A headland far right with a lighthouse.
        for (x in lay.x(140) until lay.w) {
            val top = hz - (6 + (x - lay.x(140)) / (2f * k) * 0.4f) * k
            for (y in top.toInt() until hz + 1) g.set(x, y, mixc(rock[2], haze, 0.55f))
        }
        val lx = lay.x(168)
        val ltop = hz - (34 * k).toInt()
        for (y in ltop until hz - (12 * k).toInt()) {
            val band = ((y - ltop) / (3 * k)).toInt() % 2 == 0
            for (dx in 0 until (3 * k).toInt()) g.set(lx + dx, y, mixc(if (band) rgb(200, 60, 50) else rgb(240, 236, 226), haze, 0.4f))
        }
        g.rect(lx - 1, ltop - (2 * k).toInt(), (3 * k).toInt() + 2, (2 * k).toInt(), mixc(rgb(255, 230, 150), haze, 0.2f))
        // The beach and dunes.
        for (x in 0 until lay.w) {
            val tide = lay.tide(x)
            for (y in tide + 1 until lay.h) {
                val wet = y < tide + 6 * k
                val d = lay.depth(y)
                val dune = smooth(x / (24f * k), y / (7f * k), 610) * 0.5f
                var l = 0.55f + d * 0.15f + dune + noise(x, y, 611) * 0.15f
                if (wet) l -= 0.25f
                if (((x + y * 3) / 2) % 9 == 0 && !wet) l += 0.1f
                g.set(x, y, mixc(tone(if (winter) snow else sand, l, x, y), haze, (1 - d) * 0.3f))
            }
        }
        // Marram grass on the dunes, low left.
        val marram = if (winter) Ramp(rgb(120, 110, 90), rgb(160, 150, 120), rgb(196, 186, 150)) else Ramp(rgb(90, 110, 70), rgb(130, 150, 90), rgb(176, 186, 120))
        for (i in 0 until (70 * k).toInt()) {
            val x = ((noise(i, 1, 620) + 0.5f) * 90f * k).toInt()
            val y = lay.y(10f + (noise(i, 2, 621) + 0.5f) * 30f)
            if (y < lay.tide(x) + 8 * k) continue
            val hh = (4 + (noise(i, 3, 622) + 0.5f) * 6) * k
            g.line(x, y, x + (if (i % 2 == 0) 2 else -2), (y - hh).toInt(), marram[i % 3])
        }
        // The cliff on the left: layered rock, grass on top where puffins nest.
        for (x in 0 until lay.x(52)) {
            val u = x / k
            val top = lay.y(150f - u * 0.9f + noise(x / 3, 1, 630) * 6f)
            val foot = lay.y(48f + u * 0.3f)
            for (y in top until foot) {
                val strata = sin((y + u * 0.4f * k) / (3.2f * k))
                var l = 0.55f - u / 120f + strata * 0.2f + noise(x / 2, y / 3, 631) * 0.3f
                if (x > lay.x(46f - (y - top) * 0.05f)) l -= 0.3f
                g.set(x, y, tone(rock, l, x, y))
            }
            for (j in 0 until (4 * k).toInt()) g.set(x, top + j - (2 * k).toInt(), tone(if (winter) snow else grass(s), 0.6f - j * 0.12f + noise(x, j, 632) * 0.3f, x, top + j))
        }
        boulder(g, 150f * k, lay.y(62).toFloat(), 14f * k, 6f * k, rock, s, 640)
        boulder(g, 166f * k, lay.y(58).toFloat(), 9f * k, 5f * k, rock, s, 641)
        // Tide pools.
        for (i in 0 until 3) {
            val cx = (118f + i * 14f) * k
            val cy = lay.y(22f - i * 2f).toFloat()
            for (y in (cy - 2 * k).toInt()..(cy + 2 * k).toInt()) for (x in (cx - 6 * k).toInt()..(cx + 6 * k).toInt()) {
                val u = (x - cx) / (6 * k)
                val v = (y - cy) / (2 * k)
                if (u * u + v * v < 1f) g.set(x, y, if (v < -0.4f) rock[1] else mixc(light.mid, rgb(30, 60, 80), 0.4f))
            }
        }
        fireRing(g, lay, s)
        log(g, lay.x(76).toFloat(), lay.y(32).toFloat(), 30f * k, 4.5f * k, drift, s)
    }

    /* ---------- Pieces of the ground ---------- */

    private fun path(g: Px, lay: Layout, haze: Int, winter: Boolean, sandy: Boolean = false) {
        for (y in lay.meadow until lay.h) {
            val (cx, hw) = lay.path(y)
            if (lay.place == Place.LAKE && y < lay.shore(cx.toInt()) + 4 * lay.k) continue
            val d = lay.depth(y)
            for (x in (cx - hw - 1).toInt()..(cx + hw + 1).toInt()) {
                val u = (x + 0.5f - cx) / hw
                if (abs(u) > 1f + noise(x, y, 51) * 0.4f) continue
                // Grass creeping in at the edges.
                if (abs(u) > 0.7f && noise(x, y / 2, 54) > 0.15f) continue
                val ramp = if (winter) snow else if (sandy) sand else dirt
                var l = 0.5f + noise(x, y, 52) * 0.4f - abs(u) * 0.2f
                if (abs(u) > 0.8f) l -= 0.25f
                if (!winter && noise(x, y, 53) > 0.42f) l += 0.3f
                g.set(x, y, mixc(tone(ramp, l, x, y), haze, (1 - d) * 0.4f))
            }
        }
    }

    /**
     * The stream: clear teal water whatever the sky does, with foam streaks running down it, a dark wet bank and
     * grass hanging over the edge, reeds and stepping stones. Its pixels are noted in [water] so the hour's grade
     * only half-touches them and it never turns the colour of the path.
     */
    private fun stream(g: Px, lay: Layout, light: Light, s: Season, water: IntArray) {
        val winter = s == Season.WINTER
        val k = lay.k
        val teal = Ramp(rgb(20, 56, 74), rgb(32, 90, 112), rgb(58, 134, 156), rgb(104, 182, 198), rgb(180, 224, 230))
        val ramp = Ramp(*IntArray(teal.size) { mixc(teal[it], light.mid, 0.18f) })
        val foam = rgb(232, 246, 248)
        val grassR = grass(s)
        for (y in lay.meadow - 1 until lay.h) {
            val (cx, hw) = lay.stream(y)
            val d = lay.depth(y)
            for (x in (cx - hw - 3 * k).toInt()..(cx + hw + 3 * k).toInt()) {
                val u = (x + 0.5f - cx) / hw
                if (abs(u) > 1f + 1.6f * k / hw) continue
                if (abs(u) > 1f) {
                    // The wet bank, darker on the far side, with grass blades leaning over the water.
                    val bank = if (winter) snow[1] else if (u < 0) rgb(44, 46, 34) else rgb(34, 36, 28)
                    g.set(x, y, mixc(bank, light.haze, (1 - d) * 0.3f))
                    continue
                }
                val ice = winter && abs(u) > 0.3f
                // Deeper in the middle, catching the light near the banks; ripples in rows.
                var l = 0.42f + (1 - abs(u)) * 0.18f + smooth(x / (3f * k), y / (1.4f * k), 61) * 0.35f
                val ripple = (y + (x / (2 * k)).toInt()) % (3 + (d * 3).toInt()) == 0
                if (ripple) l += 0.22f
                var c = if (ice) snow[3 - (abs(u) * 2).toInt().coerceIn(0, 2)] else tone(ramp, l, x, y, 0.5f)
                if (!ice && smooth(x / (1.6f * k), y / (0.7f * k), 62) > 0.28f && abs(u) < 0.8f) c = mixc(c, foam, 0.75f)
                c = mixc(c, light.haze, (1 - d) * 0.25f)
                g.set(x, y, c)
                if (x in 0 until g.w) water[y * g.w + x] = g[x, y]
            }
            // Grass hanging over the edge.
            if (!winter && y % 2 == 0) for (side in listOf(-1f, 1f)) {
                val ex = (cx + side * (hw + 1)).toInt()
                if (noise(ex, y, 63) > 0.0f) { g.set(ex - side.toInt(), y, grassR[2]); g.set(ex - side.toInt() * 2, y - 1, grassR[3]) }
            }
        }
        if (!winter) reedsAt(g, lay, s, (0 until 14).map { i ->
            val y = lay.y(46 + i * 5)
            val (cx, hw) = lay.stream(y)
            (cx - hw - 2 - (noise(i, 1, 91) + 0.5f) * 3 * k).toInt() to y
        })
        // Stepping stones, each with a ring of foam round it.
        for (i in 0 until 3) {
            val y = lay.y(62 + i * 11)
            val (cx, _) = lay.stream(y)
            val sx = cx + (i - 1) * 3 * k
            for (a in 0 until 10) {
                val ang = a / 10f * 6.283f
                g.set((sx + kotlin.math.cos(ang) * 4f * k).toInt(), (y + sin(ang) * 2.2f * k).toInt(), foam)
            }
            boulder(g, sx, y.toFloat(), 3f * k, 1.6f * k, stone, s, 95 + i)
        }
    }

    private fun reedsAt(g: Px, lay: Layout, s: Season, at: List<Pair<Int, Int>>) {
        val ramp = if (s == Season.AUTUMN) Ramp(rgb(90, 70, 40), rgb(140, 110, 60), rgb(190, 160, 96)) else Ramp(rgb(40, 70, 44), rgb(70, 110, 56), rgb(120, 156, 80))
        at.forEachIndexed { i, (x, y) ->
            val hh = ((5 + (noise(i, 2, 92) + 0.5f) * 6) * lay.k).toInt()
            for (j in 0 until hh) g.set(x + if (j > hh * 0.7f) 1 else 0, y - j, ramp[if (j > hh / 2) 2 else 1])
            g.set(x - 1, y - hh / 3, ramp[0])
            if (i % 3 == 0) for (j in 0..(2 * lay.k).toInt()) { g.set(x + 1, y - hh - j, rgb(92, 56, 36)); g.set(x + 2, y - hh - j, rgb(120, 74, 46)) }
        }
    }

    private fun stump(g: Px, cx: Float, top: Float, k: Float, s: Season) {
        for (dx in listOf(-12f, 11f)) g.shade(ellipse(cx + dx * k, top + 14f * k, 5f * k, 2.6f * k), dome(cx + dx * k, top + 14f * k, 5f * k, 2.6f * k), wood, lo = 0, hi = 3)
        g.shade(rect(cx - 11f * k, top, 22f * k, 14f * k), column(cx, 11f * k), wood, texture = { x, y -> if ((x + y / 3) % 3 == 0) -0.25f else noise(x, y, 3) * 0.1f }, hi = 4)
        val ring = Ramp(rgb(120, 82, 56), rgb(176, 130, 86), rgb(214, 172, 118), rgb(234, 204, 152))
        for (y in (top - 4 * k).toInt()..(top + 3 * k).toInt()) for (x in (cx - 12 * k).toInt()..(cx + 12 * k).toInt()) {
            val u = (x + 0.5f - cx) / (11.5f * k)
            val v = (y + 0.5f - top) / (3.4f * k)
            val d = u * u + v * v
            if (d > 1f) continue
            val r = kotlin.math.sqrt(d)
            val c = when {
                d > 0.8f -> wood[1]
                ((r * 7).toInt() % 2 == 0) -> ring[2]
                else -> ring[3]
            }
            g.set(x, y, if (s == Season.WINTER && v < 0.4f) snow[if (d > 0.7f) 3 else 4] else c)
        }
        if (s != Season.WINTER) for (i in 0 until (14 * k).toInt()) g.set((cx + 3 * k + i % 6).toInt(), (top + 3 * k + i / 6 + (i % 3)).toInt(), moss[1 + i % 3])
    }

    /** A log lying on the ground, end towards you, cut face showing rings. */
    private fun log(g: Px, cx: Float, cy: Float, len: Float, r: Float, ramp: Ramp, s: Season) {
        g.shade(rect(cx - len / 2, cy - r, len, r * 2), log(cy, r), ramp, texture = { x, y -> if ((y + x / 6) % 3 == 0) -0.2f else noise(x, y, 7) * 0.1f }, hi = ramp.size - 1)
        g.shade(ellipse(cx + len / 2, cy, r * 0.6f, r), facing(), Ramp(rgb(120, 82, 56), rgb(176, 130, 86), rgb(214, 172, 118), rgb(234, 204, 152)), lo = 1)
        if (s == Season.WINTER) for (x in (cx - len / 2).toInt() until (cx + len / 2).toInt()) g.set(x, (cy - r).toInt(), snow[4])
    }

    private fun fireRing(g: Px, lay: Layout, s: Season) {
        val k = lay.k
        val (fx, fy) = lay.fire
        val cx = fx.toFloat()
        val cy = fy.toFloat()
        if (lay.place == Place.FOREST) {
            g.shade(rect(cx - 34f * k, cy + 1f * k, 18f * k, 5f * k), log(cy + 3.5f * k, 2.5f * k), wood, hi = 4)
            g.shade(ellipse(cx - 16f * k, cy + 3.5f * k, 2f * k, 2.5f * k), facing(), Ramp(rgb(120, 82, 56), rgb(176, 130, 86), rgb(214, 172, 118)), outline = false)
        }
        g.shade(ellipse(cx, cy + 1f, 8f * k, 3f * k), facing(0f, -0.4f, 1f), Ramp(rgb(40, 36, 40), rgb(64, 58, 60), rgb(90, 84, 84)), outline = false)
        for (i in 0 until 11) {
            val a = i / 11f * 2 * kotlin.math.PI.toFloat()
            val x = cx + kotlin.math.cos(a) * 9.5f * k
            val y = cy + 1f + sin(a) * 3.6f * k
            g.shade(ellipse(x, y, 2.4f * k, 1.7f * k), dome(x, y, 2.4f * k, 1.7f * k), stone, lo = 1)
        }
        g.line((cx - 3 * k).toInt(), (cy + k).toInt(), (cx + 3 * k).toInt(), (cy - k).toInt(), wood[2])
        g.line((cx - 3 * k).toInt(), (cy - k).toInt(), (cx + 3 * k).toInt(), (cy + k).toInt(), wood[3])
        if (s == Season.WINTER && lay.place == Place.FOREST) for (i in 0 until (9 * k).toInt()) g.set((cx - 24 * k + i).toInt(), (cy + k).toInt(), snow[4])
    }
}

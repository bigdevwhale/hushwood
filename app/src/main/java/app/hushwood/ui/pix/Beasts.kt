package app.hushwood.ui.pix

import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Season
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A frame and where its feet are: the point that stands on the ground (or floats on the water). */
class Sprite(val px: Px, val ax: Int, val ay: Int)

enum class Pose { IDLE, SLEEP }

/**
 * Draws in design units scaled by [k], so the animals are designed small and painted at the picture's grain:
 * every shape gets more pixels, every pixel of detail (a glint in an eye, a whisker) stays one pixel.
 */
class Kit(w: Float, h: Float, val k: Float = Beasts.K) {
    val px = Px(kotlin.math.ceil(w * k).toInt() + 1, kotlin.math.ceil(h * k).toInt() + 1)

    fun e(cx: Float, cy: Float, rx: Float, ry: Float = rx) = ellipse(cx * k, cy * k, rx * k, ry * k)
    fun d(cx: Float, cy: Float, rx: Float, ry: Float = rx, flat: Float = 0f) = dome(cx * k, cy * k, rx * k, ry * k, flat)
    fun p(vararg u: Float) = poly(*FloatArray(u.size) { u[it] * k })
    fun r(x: Float, y: Float, w: Float, h: Float) = rect(x * k, y * k, w * k, h * k)
    fun col(cx: Float, rx: Float) = column(cx * k, rx * k)
    fun x(u: Float) = (u * k).toInt()
    fun y(u: Float) = (u * k).toInt()
    fun dot(x: Float, y: Float, c: Int, a: Int = 255) = px.set(x(x), y(y), c, a)
    fun line(x0: Float, y0: Float, x1: Float, y1: Float, c: Int) = px.line(x(x0), y(y0), x(x1), y(y1), c)

    /** A thick stroke (a leg, a neck) as a run of small discs. */
    fun limb(x0: Float, y0: Float, x1: Float, y1: Float, w: Float, ramp: Ramp, lo: Int = 1, hi: Int = ramp.size - 1) {
        val n = (kotlin.math.hypot(x1 - x0, y1 - y0) * k).toInt().coerceAtLeast(2)
        val shapes = (0..n).map { i -> val t = i / n.toFloat(); e(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w / 2f) }
        px.shade(union(*shapes.toTypedArray()), col((x0 + x1) / 2f, w / 2f + kotlin.math.abs(x1 - x0) / 2f), ramp, lo = lo, hi = hi)
    }

    fun sprite(ax: Float, ay: Float) = Sprite(px, x(ax), y(ay))
}

/**
 * The animals of every place, seen from the side and facing right, painted on the phone a frame at a time:
 * lit shapes, fur and feathers in strokes, an eye with a glint, whiskers, claws.
 */
object Beasts {
    const val FRAMES = 8

    /** Units to pixels: the pictures are 270 pixels across for a 180-unit design. */
    const val K = 1.5f

    private val cache = HashMap<String, Sprite>()

    /** A frame, facing left with [flip], and toned for the hour [part] so a fox at night isn't lit like noon. */
    fun frame(b: Beast, f: Int, pose: Pose = Pose.IDLE, season: Season = Season.SUMMER, flip: Boolean = false, part: Daypart? = null): Sprite {
        val winter = season == Season.WINTER
        val key = "${b.name}/${f % FRAMES}/$pose/$winter/$flip/$part"
        return cache.getOrPut(key) {
            val s = paint(b, f % FRAMES, pose, winter)
            val px = if (part != null) graded(s.px, part) else s.px
            if (flip) Sprite(mirror(px), s.px.w - 1 - s.ax, s.ay) else Sprite(px, s.ax, s.ay)
        }
    }

    /** Toned for the hour, a little brighter than the ground so the animals still read against it. */
    fun graded(p: Px, part: Daypart): Px {
        val light = Light.of(part)
        val o = Px(p.w, p.h)
        for (i in p.c.indices) {
            val c = p.c[i]
            if (c.al() == 0) continue
            o.c[i] = (c and 0xFF000000.toInt()) or (mixc(c, light.grade(c), if (part == Daypart.NIGHT) 0.72f else 0.8f) and 0xFFFFFF)
        }
        return o
    }

    fun mirror(p: Px): Px {
        val o = Px(p.w, p.h)
        for (y in 0 until p.h) for (x in 0 until p.w) o.c[y * p.w + (p.w - 1 - x)] = p.c[y * p.w + x]
        return o
    }

    /** Draws a hand-made map: one character per pixel, '.' is empty. */
    fun stamp(px: Px, map: Array<String>, pal: Map<Char, Int>, ox: Int = 0, oy: Int = 0) {
        map.forEachIndexed { y, row -> row.forEachIndexed { x, ch -> pal[ch]?.let { px.set(ox + x, oy + y, it) } } }
    }

    private fun paint(b: Beast, f: Int, pose: Pose, winter: Boolean): Sprite {
        val sleep = pose == Pose.SLEEP
        val blink = sleep || f == 6
        return when (b) {
            Beast.TIT -> songbird(f, sleep, TIT_LOOK)
            Beast.ROBIN -> songbird(f, sleep, ROBIN_LOOK)
            Beast.WREN -> songbird(f, sleep, WREN_LOOK)
            Beast.KINGFISHER -> songbird(f, sleep, KINGFISHER_LOOK)
            Beast.CHOUGH -> songbird(f, sleep, CHOUGH_LOOK)
            Beast.SANDPIPER -> songbird(f, sleep, SANDPIPER_LOOK)
            Beast.FROG -> frog(f)
            Beast.FIREFLIES -> glimmer(f, rgb(250, 250, 170), rgb(210, 230, 110))
            Beast.SPARKLE -> glimmer(f, rgb(200, 250, 255), rgb(90, 200, 230))
            Beast.DRAGONFLY -> dragonfly(f)
            Beast.BUTTERFLY -> butterfly(f)
            Beast.CRANES -> skein(f)
            Beast.EAGLE -> eagle(f)
            Beast.HEDGEHOG -> hedgehog(f, sleep)
            Beast.SQUIRREL -> squirrel(f, blink)
            Beast.HARE -> hare(f, blink, winter)
            Beast.FOX -> if (sleep) foxAsleep(f) else fox(f, blink)
            Beast.OWL -> owl(f, blink)
            Beast.HERON -> wader(f, blink, sleep, crane = false)
            Beast.CRANE -> wader(f, blink, sleep, crane = true)
            Beast.DEER -> if (sleep) deerLying(f, DEER_R) else deer(f, blink, if (winter) DEER_WINTER else DEER_R, false)
            Beast.WHITE_DEER -> deer(f, blink, WHITE_R, true)
            Beast.BEAR -> if (sleep) bearAsleep(f) else bear(f, blink)
            Beast.DUCK -> duck(f, blink)
            Beast.SWAN -> swan(f, blink)
            Beast.BEAVER -> beaver(f, blink)
            Beast.OTTER -> otter(f, blink)
            Beast.MOOSE -> moose(f, blink)
            Beast.PIKA -> pika(f, blink)
            Beast.MARMOT -> marmot(f, blink)
            Beast.IBEX -> ibex(f, blink)
            Beast.LYNX -> lynx(f, blink)
            Beast.SNOW_LEOPARD -> leopard(f, blink)
            Beast.CRAB -> crab(f)
            Beast.GULL -> gull(f, blink)
            Beast.PUFFIN -> puffin(f, blink)
            Beast.SEAL -> seal(f, blink)
            Beast.WHALE -> whale(f)
        }
    }

    /* ---------- Colours ---------- */

    private val INK = rgb(24, 20, 28)
    private val GLINT = rgb(250, 250, 240)
    private val HOG = Ramp(rgb(36, 28, 30), rgb(70, 54, 48), rgb(110, 88, 70), rgb(150, 124, 96), rgb(190, 170, 136), rgb(226, 214, 186))
    private val SKIN = Ramp(rgb(96, 66, 56), rgb(150, 112, 88), rgb(200, 162, 126), rgb(230, 204, 168))
    private val RUFOUS = Ramp(rgb(60, 24, 22), rgb(108, 46, 30), rgb(158, 74, 40), rgb(200, 108, 54), rgb(228, 150, 84), rgb(244, 196, 140))
    private val CREAM = Ramp(rgb(110, 92, 86), rgb(170, 152, 136), rgb(218, 204, 182), rgb(240, 232, 214), rgb(252, 250, 240))
    private val HARE_R = Ramp(rgb(54, 44, 40), rgb(96, 80, 66), rgb(140, 118, 92), rgb(178, 156, 122), rgb(212, 194, 160), rgb(236, 224, 198))
    private val SNOWHARE = Ramp(rgb(120, 124, 150), rgb(170, 174, 196), rgb(212, 214, 228), rgb(236, 238, 244), rgb(250, 250, 252))
    private val FOX_R = Ramp(rgb(60, 22, 24), rgb(112, 40, 30), rgb(168, 70, 36), rgb(214, 108, 46), rgb(236, 150, 72), rgb(248, 196, 128))
    private val OWL_R = Ramp(rgb(40, 28, 28), rgb(72, 50, 40), rgb(108, 78, 56), rgb(146, 110, 76), rgb(182, 146, 104), rgb(214, 186, 144))
    private val DISC = Ramp(rgb(120, 92, 70), rgb(170, 136, 100), rgb(208, 180, 140), rgb(232, 214, 180))
    private val GREY = Ramp(rgb(52, 56, 70), rgb(86, 92, 108), rgb(124, 130, 144), rgb(162, 168, 178), rgb(200, 204, 210), rgb(234, 236, 238))
    private val BLACK = Ramp(rgb(16, 14, 20), rgb(32, 30, 38), rgb(52, 50, 60), rgb(80, 80, 92), rgb(120, 122, 136))
    private val WHITE = Ramp(rgb(150, 158, 176), rgb(196, 202, 214), rgb(224, 228, 234), rgb(242, 244, 246), rgb(252, 252, 252))
    private val DEER_R = Ramp(rgb(60, 34, 28), rgb(104, 60, 40), rgb(146, 90, 54), rgb(184, 124, 74), rgb(214, 160, 106), rgb(236, 198, 150))
    private val DEER_WINTER = Ramp(rgb(54, 44, 42), rgb(90, 76, 68), rgb(126, 108, 94), rgb(160, 140, 122), rgb(192, 176, 156), rgb(222, 210, 192))
    private val WHITE_R = Ramp(rgb(150, 160, 190), rgb(192, 200, 220), rgb(222, 228, 238), rgb(240, 244, 248), rgb(252, 252, 255), rgb(255, 255, 255))
    private val BEAR_R = Ramp(rgb(30, 20, 22), rgb(56, 36, 32), rgb(86, 56, 42), rgb(118, 80, 56), rgb(150, 108, 74), rgb(184, 144, 102))
    private val MUZZLE = Ramp(rgb(90, 64, 46), rgb(140, 106, 76), rgb(180, 146, 110), rgb(210, 182, 146))
    private val BEAK = Ramp(rgb(120, 90, 30), rgb(190, 150, 50), rgb(232, 196, 90), rgb(250, 226, 150))
    private val MOOSE_R = Ramp(rgb(30, 22, 22), rgb(52, 38, 32), rgb(78, 58, 44), rgb(106, 82, 60), rgb(138, 110, 82), rgb(172, 146, 112))
    private val ANTLER = Ramp(rgb(110, 92, 70), rgb(160, 140, 108), rgb(206, 190, 156), rgb(234, 224, 196))
    private val BEAVER_R = Ramp(rgb(40, 24, 20), rgb(70, 44, 30), rgb(104, 68, 42), rgb(140, 96, 60), rgb(172, 128, 84))
    private val OTTER_R = Ramp(rgb(34, 24, 22), rgb(62, 44, 34), rgb(96, 70, 50), rgb(130, 98, 70), rgb(166, 132, 98), rgb(204, 176, 140))
    private val TAWNY = Ramp(rgb(70, 50, 36), rgb(116, 84, 56), rgb(160, 120, 76), rgb(198, 158, 104), rgb(226, 194, 140), rgb(244, 226, 190))
    private val IBEX_R = Ramp(rgb(46, 38, 34), rgb(82, 68, 56), rgb(120, 102, 82), rgb(156, 136, 110), rgb(190, 172, 142), rgb(220, 206, 180))
    private val HORN = Ramp(rgb(60, 50, 44), rgb(100, 86, 70), rgb(144, 128, 104), rgb(184, 170, 142))
    private val LYNX_R = Ramp(rgb(70, 50, 40), rgb(120, 90, 66), rgb(166, 130, 94), rgb(200, 166, 122), rgb(226, 200, 160), rgb(244, 230, 204))
    private val LEOPARD = Ramp(rgb(110, 110, 120), rgb(160, 160, 166), rgb(200, 198, 196), rgb(226, 224, 218), rgb(244, 242, 236), rgb(252, 252, 250))
    private val SEAL_R = Ramp(rgb(40, 42, 50), rgb(70, 74, 84), rgb(104, 108, 116), rgb(140, 142, 146), rgb(176, 176, 176), rgb(208, 206, 200))
    private val CRAB_R = Ramp(rgb(100, 30, 26), rgb(160, 54, 34), rgb(210, 90, 50), rgb(240, 140, 80), rgb(252, 190, 130))
    private val PINK = Ramp(rgb(150, 90, 90), rgb(200, 130, 120), rgb(234, 170, 150))
    private val ORANGE = Ramp(rgb(150, 60, 20), rgb(210, 100, 30), rgb(244, 150, 50), rgb(255, 200, 110))

    /** Fur: short strokes running the way the hair lies, darker in the gaps. */
    private fun fur(salt: Int, kk: Float = 0.18f): (Int, Int) -> Float = { x, y ->
        val strand = noise(x + y / 3, y / 3, salt)
        strand * kk + if (noise(x / 2 + y / 4, y / 2, salt + 7) > 0.34f) -0.14f else 0f
    }

    /** Feathers: scalloped rows. */
    private fun feathers(kk: Float = 0.2f): (Int, Int) -> Float = { x, y ->
        val r = y / 3
        val xx = x + (r % 2) * 2
        if (y % 3 == 2 && xx % 4 != 0) -kk else if (xx % 4 == 1 && y % 3 == 0) kk * 0.6f else 0f
    }

    private fun spots(salt: Int, kk: Float = 0.45f): (Int, Int) -> Float = { x, y -> if (noise(x / 2, y / 2, salt) > 0.3f) -kk else noise(x, y, salt + 1) * 0.1f }

    /** An eye with its glint, two pixels across; closed it's a curved lid. */
    private fun eye(kit: Kit, x: Float, y: Float, closed: Boolean, lid: Int, iris: Int = INK, big: Boolean = false) {
        val px = kit.px
        val ex = kit.x(x)
        val ey = kit.y(y)
        if (closed) {
            px.set(ex - 1, ey, lid); px.set(ex, ey + 1, lid); px.set(ex + 1, ey, lid)
            return
        }
        px.set(ex, ey, iris); px.set(ex - 1, ey, iris); px.set(ex, ey + 1, INK); px.set(ex - 1, ey + 1, INK)
        if (big) { px.set(ex + 1, ey, iris); px.set(ex + 1, ey + 1, INK); px.set(ex, ey - 1, lid) }
        px.set(ex - 1, ey, GLINT)
    }

    /** Whiskers: a few pale dots fanning from the muzzle. */
    private fun whiskers(kit: Kit, x: Float, y: Float, c: Int = rgb(240, 236, 220)) {
        val px = kit.px
        val bx = kit.x(x)
        val by = kit.y(y)
        for (i in 1..2) { px.set(bx + i, by - 1 + i / 2, c, 110); px.set(bx + i, by + 1 + i / 2, c, 80) }
    }

    /* ---------- Small birds, from lit shapes ---------- */

    /** How a small songbird is coloured and shaped: the same body, different patches. */
    private class Look(
        val back: Ramp,
        val belly: Ramp,
        val head: Ramp,
        val cap: Ramp? = null,
        val cheek: Int? = null,
        val bib: Ramp? = null,
        val wing: Ramp,
        val bar: Int? = null,
        val beak: Ramp,
        val beakLen: Float = 2.2f,
        val legs: Int,
        val legLen: Float = 2f,
        val tailUp: Boolean = false,
        val stripe: Boolean = false,
    )

    private val TIT_LOOK = Look(
        back = Ramp(rgb(70, 86, 50), rgb(104, 124, 68), rgb(138, 156, 86), rgb(170, 184, 110)),
        belly = Ramp(rgb(150, 116, 40), rgb(200, 166, 58), rgb(236, 206, 90), rgb(250, 236, 150)),
        head = BLACK, cap = BLACK, cheek = rgb(244, 244, 236), bib = BLACK,
        wing = Ramp(rgb(50, 62, 84), rgb(80, 98, 124), rgb(116, 136, 160), rgb(160, 176, 194)), bar = rgb(236, 236, 230),
        beak = Ramp(rgb(30, 28, 34), rgb(60, 56, 64), rgb(90, 86, 94)), beakLen = 1.6f, legs = rgb(90, 96, 114), stripe = true,
    )
    private val ROBIN_LOOK = Look(
        back = Ramp(rgb(70, 56, 44), rgb(104, 84, 62), rgb(136, 112, 82), rgb(166, 140, 106)),
        belly = Ramp(rgb(170, 166, 156), rgb(210, 206, 196), rgb(236, 232, 222), rgb(250, 248, 242)),
        head = Ramp(rgb(70, 56, 44), rgb(104, 84, 62), rgb(136, 112, 82), rgb(166, 140, 106)),
        bib = Ramp(rgb(150, 60, 30), rgb(204, 96, 46), rgb(236, 134, 70), rgb(250, 176, 110)),
        wing = Ramp(rgb(60, 48, 38), rgb(92, 74, 56), rgb(124, 100, 76), rgb(150, 126, 98)),
        beak = Ramp(rgb(30, 26, 26), rgb(56, 48, 46), rgb(80, 72, 70)), beakLen = 1.6f, legs = rgb(140, 110, 96),
    )
    private val WREN_LOOK = Look(
        back = Ramp(rgb(72, 46, 32), rgb(112, 76, 50), rgb(150, 108, 70), rgb(184, 142, 98)),
        belly = Ramp(rgb(150, 120, 90), rgb(196, 166, 126), rgb(222, 200, 160), rgb(240, 226, 194)),
        head = Ramp(rgb(72, 46, 32), rgb(112, 76, 50), rgb(150, 108, 70), rgb(184, 142, 98)), cheek = rgb(220, 196, 160),
        wing = Ramp(rgb(60, 38, 28), rgb(96, 64, 44), rgb(132, 94, 62), rgb(164, 126, 88)), bar = rgb(60, 38, 28),
        beak = Ramp(rgb(60, 44, 36), rgb(96, 76, 62), rgb(130, 108, 90)), beakLen = 2f, legs = rgb(150, 116, 96), tailUp = true,
    )
    private val KINGFISHER_LOOK = Look(
        back = Ramp(rgb(20, 70, 110), rgb(30, 110, 160), rgb(56, 160, 210), rgb(120, 210, 240)),
        belly = Ramp(rgb(150, 66, 30), rgb(204, 106, 50), rgb(236, 146, 80), rgb(250, 190, 130)),
        head = Ramp(rgb(20, 70, 110), rgb(30, 110, 160), rgb(56, 160, 210), rgb(120, 210, 240)), cheek = rgb(236, 140, 74), bib = Ramp(rgb(200, 200, 190), rgb(236, 236, 228), rgb(252, 252, 246)),
        wing = Ramp(rgb(16, 56, 90), rgb(26, 90, 136), rgb(44, 130, 180), rgb(90, 180, 220)),
        beak = Ramp(rgb(20, 18, 22), rgb(40, 38, 44), rgb(70, 68, 74)), beakLen = 4.2f, legs = rgb(220, 90, 70),
    )
    private val CHOUGH_LOOK = Look(
        back = BLACK, belly = BLACK, head = BLACK,
        wing = Ramp(rgb(14, 12, 20), rgb(28, 26, 40), rgb(48, 48, 70), rgb(78, 80, 110)),
        beak = Ramp(rgb(170, 140, 30), rgb(226, 196, 60), rgb(250, 230, 120)), beakLen = 2.4f, legs = rgb(210, 60, 50), legLen = 2.6f,
    )
    private val SANDPIPER_LOOK = Look(
        back = Ramp(rgb(80, 66, 54), rgb(120, 102, 82), rgb(160, 140, 112), rgb(196, 178, 146)),
        belly = WHITE, head = Ramp(rgb(80, 66, 54), rgb(120, 102, 82), rgb(160, 140, 112), rgb(196, 178, 146)), cheek = rgb(236, 232, 220),
        wing = Ramp(rgb(64, 52, 44), rgb(100, 84, 68), rgb(140, 120, 96), rgb(180, 160, 130)), bar = rgb(230, 226, 214),
        beak = Ramp(rgb(30, 28, 30), rgb(54, 50, 52), rgb(80, 76, 78)), beakLen = 3.4f, legs = rgb(110, 110, 80), legLen = 4f,
    )

    /**
     * A small bird about 12 units long: round body, round head, the tail, a folded wing with its feather edges,
     * a glinting eye. It pecks now and then, and asleep it's a puffed ball with the head tucked in.
     */
    private fun songbird(f: Int, sleep: Boolean, l: Look): Sprite {
        val kit = Kit(15f, 11f + l.legLen)
        val px = kit.px
        val ground = 9f + l.legLen
        val peck = !sleep && (f == 3 || f == 4)
        val hop = if (!sleep && f == 1) -0.6f else 0f
        if (!sleep) {
            kit.line(5.6f, 8.6f + hop, 5.4f, ground, l.legs); kit.line(7.2f, 8.6f + hop, 7.4f, ground, l.legs)
            kit.dot(6.2f, ground, l.legs); kit.dot(8.2f, ground, l.legs)
        }
        // The tail, cocked up for a wren.
        val tail = if (l.tailUp) kit.p(3.2f, 6f + hop, 0.4f, 0.6f + hop, 1.8f, 0.2f + hop, 4.6f, 5f + hop) else kit.p(3.4f, 5.6f + hop, 0f, 8.4f + hop, 0.6f, 9.4f + hop, 4.4f, 7.6f + hop)
        px.shade(tail, facing(-0.2f, -0.3f, 1f), l.wing, lo = 0, hi = 2)
        val body = kit.e(6.4f, 6.4f + hop + (if (sleep) -0.2f else 0f), 3.9f + if (sleep) 0.5f else 0f, 3f + if (sleep) 0.6f else 0f)
        px.shade(body, kit.d(6.4f, 6f + hop, 3.9f, 3.2f), l.back, texture = feathers(0.1f))
        px.shade(and(body, kit.r(5f, 6.8f + hop, 6f, 4f)), kit.d(7f, 6.4f + hop, 3f, 3f), l.belly, outline = false, lo = 1)
        // The folded wing with pale feather edges.
        val wing = kit.e(5.6f, 5.9f + hop, 2.8f, 1.7f)
        px.shade(wing, kit.d(5.6f, 5.6f + hop, 2.8f, 1.9f), l.wing, lo = 0, hi = 3)
        for (i in 0 until 3) kit.dot(3.6f + i * 1.1f, 6.6f + hop + i * 0.2f, l.wing[3])
        l.bar?.let { kit.line(4.2f, 5.2f + hop, 7f, 5f + hop, it) }
        if (sleep) {
            val head = kit.e(8.6f, 4f + hop, 2.5f, 2.3f)
            px.shade(head, kit.d(8.6f, 3.8f + hop, 2.5f, 2.3f), l.head)
            l.cap?.let { px.shade(and(head, kit.r(6f, 1f + hop, 6f, 2.2f)), facing(), it, outline = false, lo = 1, hi = 2) }
            kit.dot(9.2f, 3.9f + hop, l.head[0]); kit.dot(9.9f, 3.9f + hop, l.head[0])
            return kit.sprite(6.6f, ground)
        }
        val hy = if (peck) 1.2f else 0f
        val hx = if (peck) 0.6f else 0f
        val head = kit.e(9.4f + hx, 3.6f + hop + hy, 2.5f, 2.3f)
        px.shade(head, kit.d(9.4f + hx, 3.3f + hop + hy, 2.5f, 2.3f), l.head)
        l.cap?.let { px.shade(and(head, kit.r(6.8f + hx, 1f + hop + hy, 6f, 2.1f)), facing(), it, outline = false, lo = 1, hi = 3) }
        l.cheek?.let { c -> px.shade(and(head, kit.e(9.6f + hx, 4.4f + hop + hy, 1.5f, 1f)), facing(), Ramp(mixc(c, INK, 0.2f), c), outline = false) }
        l.bib?.let { px.shade(and(kit.e(9.2f + hx, 6f + hop + hy * 0.5f, 2.2f, 2.6f), union(head, body)), facing(), it, outline = false, lo = 1, hi = it.size - 1) }
        if (l.stripe) kit.line(8.6f, 7f + hop, 8.4f, 9.2f + hop, INK)
        val bx = 11.6f + hx
        val by = 3.6f + hop + hy
        px.shade(kit.p(bx - 0.4f, by - 0.7f, bx + l.beakLen, by + 0.1f, bx - 0.4f, by + 0.8f), facing(0f, -0.4f, 1f), l.beak, outline = false, lo = 0)
        kit.dot(bx + 0.3f, by + 0.4f, l.beak[0])
        eye(kit, 10.2f + hx, 3f + hop + hy, f == 6, l.head[0])
        return kit.sprite(6.6f, ground)
    }

    private fun frog(f: Int): Sprite {
        val kit = Kit(14f, 9f)
        val px = kit.px
        val g = Ramp(rgb(40, 66, 36), rgb(64, 100, 50), rgb(98, 140, 70), rgb(142, 178, 96), rgb(190, 210, 130))
        val belly = Ramp(rgb(150, 150, 90), rgb(196, 192, 130), rgb(226, 222, 170))
        px.shade(kit.e(4.4f, 6.6f, 3.4f, 1.8f), kit.d(4.4f, 6.2f, 3.4f, 2f), g, texture = spots(51, 0.3f))
        px.shade(kit.p(2f, 4.6f, 7f, 2.4f, 12f, 4.4f, 11.6f, 6.6f, 6f, 7.4f, 2f, 6.6f), kit.d(7f, 4.6f, 5f, 3f), g, texture = spots(52, 0.3f))
        px.shade(and(kit.e(9f, 6.2f, 3.6f, 1.6f), kit.r(6f, 5.6f, 7f, 3f)), facing(), belly, outline = false, lo = 1)
        kit.limb(9.6f, 6f, 10.6f, 8f, 1f, g, lo = 1)
        px.shade(kit.e(9.6f, 2.6f, 1.5f), kit.d(9.6f, 2.4f, 1.5f), g, lo = 1)
        if (f == 6) kit.line(9f, 2.6f, 10.4f, 2.6f, g[0]) else { kit.dot(9.6f, 2.6f, rgb(230, 196, 80)); kit.dot(10f, 2.6f, INK); kit.dot(9.6f, 2.2f, GLINT) }
        kit.dot(12f, 4.6f, g[0])
        if (f % 4 < 2) px.shade(kit.e(11f, 6f, 1.4f, 1.2f), facing(), belly, lo = 1)
        return kit.sprite(6f, 8.4f)
    }

    /** A point of light: a firefly, or the sea glowing where it breaks. */
    private fun glimmer(f: Int, core: Int, halo: Int): Sprite {
        val px = Px(9, 9)
        px.set(4, 4, core)
        if (f % 4 < 2) {
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) px.set(4 + dx, 4 + dy, halo, 170)
            for ((dx, dy) in listOf(1 to 1, -1 to -1, 1 to -1, -1 to 1)) px.set(4 + dx, 4 + dy, halo, 70)
        }
        return Sprite(px, 4, 8)
    }

    private fun dragonfly(f: Int): Sprite {
        val px = Px(11, 6)
        val body = listOf(rgb(40, 120, 170), rgb(70, 170, 210))
        for (i in 0 until 8) px.set(1 + i, 3, body[i % 2])
        px.set(9, 3, rgb(30, 80, 120)); px.set(9, 2, rgb(120, 200, 230))
        val wing = rgb(220, 240, 250)
        val up = f % 2 == 0
        for (i in 0 until 3) { px.set(5 + i, if (up) 1 else 2, wing, 170); px.set(5 + i, if (up) 5 else 4, wing, 150) }
        return Sprite(px, 5, 5)
    }

    /** An Apollo butterfly: white wings with red eye-spots, flapping. */
    private fun butterfly(f: Int): Sprite {
        val px = Px(9, 8)
        val open = f % 4 < 2
        val w = rgb(244, 242, 232)
        val edge = rgb(70, 70, 70)
        if (open) {
            for ((x, y) in listOf(1 to 1, 2 to 1, 1 to 2, 2 to 2, 3 to 2, 1 to 3, 2 to 3, 6 to 1, 7 to 1, 5 to 2, 6 to 2, 7 to 2, 6 to 3, 7 to 3, 2 to 5, 3 to 5, 5 to 5, 6 to 5)) px.set(x, y, w)
            px.set(2, 2, rgb(210, 50, 50)); px.set(6, 2, rgb(210, 50, 50)); px.set(1, 1, edge); px.set(7, 1, edge)
        } else {
            for ((x, y) in listOf(3 to 1, 3 to 2, 5 to 1, 5 to 2, 3 to 3, 5 to 3)) px.set(x, y, w)
            px.set(3, 2, rgb(210, 50, 50))
        }
        for (y in 2..5) px.set(4, y, rgb(40, 36, 40))
        return Sprite(px, 4, 7)
    }

    /** Five cranes in a V, wings up and down. */
    private fun skein(f: Int): Sprite {
        val up = arrayOf("k.......k", ".k.....k.", "..kk.kk..", "....kkkkn", "....k....")
        val down = arrayOf(".........", ".........", "...kkkkkn", "..k.k..k.", ".k.......")
        val px = Px(66, 22)
        val pal = mapOf('k' to rgb(52, 50, 62), 'n' to rgb(90, 80, 84))
        listOf(0 to 16, 13 to 11, 27 to 5, 40 to 11, 54 to 16).forEachIndexed { i, (x, y) ->
            stamp(px, if ((f + i) % 4 < 2) up else down, pal, x, y - 4)
        }
        return Sprite(px, 33, 21)
    }

    /** A golden eagle soaring, fingers spread at the wingtips. */
    private fun eagle(f: Int): Sprite {
        val kit = Kit(30f, 10f)
        val px = kit.px
        val flex = if (f % 4 < 2) 0f else 0.6f
        val r = Ramp(rgb(40, 28, 24), rgb(72, 50, 36), rgb(110, 78, 50), rgb(150, 112, 70), rgb(196, 156, 100), rgb(230, 200, 140))
        // Broad wings held in a shallow V, the hand feathers spread like fingers.
        for (side in listOf(-1f, 1f)) {
            val wing = kit.p(15f, 4.2f, 15f + side * 5f, 1.8f - flex, 15f + side * 11f, 1.2f - flex * 1.5f, 15f + side * 13.4f, 2f - flex * 1.6f, 15f + side * 12.6f, 4.4f - flex, 15f + side * 6f, 6.2f, 15f, 6f)
            px.shade(wing, facing(0f, -0.6f, 1f), r, texture = feathers(0.12f), lo = 0, hi = 3)
            px.shade(and(wing, kit.p(15f, 3.4f, 15f + side * 9f, 2.2f - flex, 15f + side * 9f, 3.6f - flex, 15f, 5f)), facing(), r, outline = false, lo = 3, hi = 4)
            for (i in 0 until 4) kit.line(15f + side * (11.4f + i * 0.6f), 3f - flex * 1.4f + i * 0.4f, 15f + side * (12.6f + i * 0.9f), 2.4f - flex * 1.6f + i * 0.6f, r[0])
        }
        px.shade(kit.e(15f, 4.6f, 2.6f, 1.6f), kit.d(15f, 4.4f, 2.6f, 1.6f), r)
        px.shade(kit.e(18f, 3.6f, 1.4f), kit.d(18f, 3.4f, 1.4f), Ramp(rgb(120, 90, 50), rgb(180, 140, 80), rgb(224, 190, 120)))
        kit.dot(19.4f, 3.8f, BEAK[2])
        px.shade(kit.p(12.6f, 4.4f, 9.6f, 3.8f, 9.6f, 6.2f, 12.6f, 5.6f), facing(), r, lo = 1, hi = 3)
        return kit.sprite(15f, 9f)
    }

    /* ---------- Forest ---------- */

    private fun hedgehog(f: Int, sleep: Boolean): Sprite {
        val kit = Kit(20f, 13f)
        val px = kit.px
        val breath = if (f % 4 < 2) 0f else 0.4f
        val quills: (Int, Int) -> Float = { x, y -> if ((x + y * 2) % 3 == 0) -0.32f else if ((x * 2 + y) % 5 == 0) 0.22f else noise(x, y, 3) * 0.18f }
        if (sleep) {
            px.shade(kit.e(9.5f, 7.5f, 7f, 5f + breath), kit.d(9.5f, 7.5f, 7f, 5f), HOG, texture = quills)
            spikes(kit, 9.5f, 7.5f, 7f, 5f, 0f, PI.toFloat() * 2)
            kit.dot(14f, 9.6f, SKIN[2]); kit.dot(14.6f, 9.6f, INK)
            return kit.sprite(9f, 12.4f)
        }
        for (fx in listOf(5f, 7f, 11.5f, 13f)) kit.dot(fx, 12.2f, SKIN[0])
        px.shade(kit.e(8f, 8f, 7f, 4.5f + breath), kit.d(8f, 8f, 7f, 4.5f), HOG, texture = quills)
        spikes(kit, 8f, 8f, 7f, 4.5f, PI.toFloat() * 0.95f, PI.toFloat() * 2.05f)
        // The pointed face, sniffing, with a wet black nose.
        val sniff = if (f == 2 || f == 3) 0.6f else 0f
        px.shade(kit.p(12f, 5.6f, 18f + sniff, 9.4f, 12.6f, 12f), kit.d(14f, 9f, 4f, 3f), SKIN, texture = fur(4, 0.1f))
        kit.dot(18f + sniff, 9.4f, INK); kit.dot(18.4f + sniff, 9f, rgb(90, 80, 90))
        kit.dot(13.4f, 7.6f, SKIN[1])
        eye(kit, 15f, 8.2f, f == 6, SKIN[0])
        whiskers(kit, 16.6f, 10f, SKIN[3])
        return kit.sprite(9f, 12.6f)
    }

    private fun spikes(kit: Kit, cx: Float, cy: Float, rx: Float, ry: Float, a0: Float, a1: Float) {
        var a = a0
        var i = 0
        while (a < a1) {
            val x = cx + cos(a) * rx
            val y = cy + sin(a) * ry
            val len = 1.4f + (i % 3) * 0.4f
            val ox = cx + cos(a) * (rx + len)
            val oy = cy + sin(a) * (ry + len)
            kit.line(x, y, ox, oy, if (i % 2 == 0) HOG[1] else HOG[3])
            kit.dot(ox, oy, HOG[5])
            a += 0.32f
            i++
        }
    }

    private fun squirrel(f: Int, blink: Boolean): Sprite {
        val kit = Kit(18f, 19f)
        val px = kit.px
        val sway = if (f % 4 < 2) 0f else 0.6f
        val tail = union(kit.e(4.5f - sway, 9f, 4f, 7f), kit.e(6.5f - sway, 2.8f, 3.4f, 2.4f))
        px.shade(tail, kit.d(4.5f, 7f, 5f, 8f), RUFOUS, texture = fur(5, 0.26f), rim = RUFOUS[5])
        for (i in 0 until 12) {
            val a = i / 12f * PI.toFloat() + PI.toFloat() * 0.6f
            kit.dot(4.5f - sway + cos(a) * 4.4f, 9f + sin(a) * 7.4f, RUFOUS[4], 200)
        }
        px.shade(kit.e(10f, 12.5f, 4.2f, 5f), kit.d(10f, 12f, 4.2f, 5f), RUFOUS, texture = fur(6))
        px.shade(kit.e(11.4f, 13.6f, 2f, 3f), kit.d(11.2f, 13f, 2f, 3f), CREAM, outline = false, lo = 2)
        px.shade(kit.e(9f, 17.4f, 3.2f, 1.2f), facing(), RUFOUS, lo = 1, hi = 3)
        kit.dot(11.6f, 17.8f, RUFOUS[0])
        val nod = if (f == 3 || f == 4) 0.7f else 0f
        px.shade(kit.e(12.5f, 6.5f + nod, 3.6f, 3f), kit.d(12.5f, 6.5f, 3.6f, 3f), RUFOUS, texture = fur(7, 0.1f))
        px.shade(and(kit.e(13.4f, 7.8f + nod, 2.4f, 1.4f), kit.r(11f, 7.4f + nod, 6f, 3f)), facing(), CREAM, outline = false, lo = 2)
        px.shade(kit.p(11f, 4.5f + nod, 11.2f, 0.4f + nod, 13.2f, 4f + nod), facing(), RUFOUS, lo = 1)
        kit.dot(11.2f, 0.6f + nod, RUFOUS[0]); kit.dot(11.6f, 1.2f + nod, RUFOUS[1])
        px.shade(kit.e(14.8f, 10.2f, 1.6f, 1.5f), kit.d(14.8f, 10f, 1.6f, 1.5f), Ramp(rgb(90, 56, 30), rgb(140, 96, 50), rgb(190, 140, 80), rgb(220, 180, 120)), lo = 0)
        kit.dot(13.6f, 10.8f, RUFOUS[1]); kit.dot(14f, 11.4f, RUFOUS[1])
        kit.dot(16f, 7.2f + nod, INK)
        eye(kit, 13.6f, 5.8f + nod, blink, RUFOUS[1])
        whiskers(kit, 15.6f, 7.4f + nod)
        return kit.sprite(10f, 18.4f)
    }

    private fun hare(f: Int, blink: Boolean, winter: Boolean): Sprite {
        val r = if (winter) SNOWHARE else HARE_R
        val kit = Kit(20f, 21f)
        val px = kit.px
        val twitch = if (f == 2) -0.8f else 0f
        px.shade(kit.e(8f, 15f, 7.2f, 5.2f), kit.d(8f, 14f, 7.2f, 5.5f), r, texture = fur(8))
        px.shade(kit.e(6f, 17f, 4.2f, 3f), kit.d(6f, 17f, 4f, 3f), r, texture = fur(9), lo = 1)
        px.shade(kit.e(7.4f, 19.6f, 3.6f, 1f), facing(), r, lo = 1, hi = 3)
        px.shade(kit.e(1.6f, 14.5f, 1.6f, 1.6f), kit.d(1.6f, 14.2f, 1.6f, 1.6f), CREAM, lo = 2)
        val ear1 = kit.p(10f, 9f, 8.5f, 1.5f, 10.4f, 0.8f, 12f, 8f)
        val ear2 = kit.p(12f, 9f, 13f + twitch, 1.6f, 15f + twitch, 2.2f, 13.6f, 9f)
        px.shade(ear1, facing(-0.3f, 0f, 1f), r, lo = 1, texture = fur(10, 0.08f))
        px.shade(ear2, facing(-0.3f, 0f, 1f), r, lo = 2, texture = fur(11, 0.08f))
        px.shade(and(ear2, kit.p(12.8f, 8.6f, 13.6f + twitch, 2.8f, 14.2f + twitch, 3.2f, 13.6f, 8.6f)), facing(), PINK, outline = false)
        kit.dot(9f, 1.2f, INK); kit.dot(9.6f, 1f, INK); kit.dot(10.2f, 1f, INK); kit.dot(14f + twitch, 2f, INK); kit.dot(14.6f + twitch, 2.3f, INK)
        px.shade(kit.e(13.5f, 10f, 4f, 3.4f), kit.d(13.5f, 10f, 4f, 3.4f), r, texture = fur(12, 0.1f))
        px.shade(and(kit.e(15.4f, 11.4f, 2f, 1.4f), kit.r(13f, 10.6f, 6f, 3f)), facing(), CREAM, outline = false, lo = 2)
        kit.dot(17.2f, 10.6f, rgb(140, 90, 90))
        eye(kit, 15f, 9f, blink, r[1], iris = rgb(150, 100, 50), big = true)
        whiskers(kit, 16.6f, 11.2f)
        return kit.sprite(9f, 20.4f)
    }

    private fun fox(f: Int, blink: Boolean): Sprite {
        val kit = Kit(26f, 22f)
        val px = kit.px
        val tip = if (f % 4 < 2) 0f else 0.8f
        val tail = kit.e(11f, 19f, 9.5f + tip * 0.5f, 2.6f)
        px.shade(tail, kit.d(11f, 18.5f, 9.5f, 3f), FOX_R, texture = fur(11, 0.24f), rim = FOX_R[5])
        px.shade(and(tail, kit.r(18f + tip, 15f, 6f, 8f)), facing(), CREAM, outline = false, lo = 2)
        px.shade(kit.e(10f, 13f, 6.5f, 6.5f), kit.d(10f, 12.5f, 6.5f, 6.5f), FOX_R, texture = fur(12))
        for (lx in listOf(14.6f, 16.8f)) {
            px.shade(kit.r(lx, 13.6f, 1.6f, 5.6f), kit.col(lx + 0.8f, 0.8f), FOX_R, lo = 1, hi = 3)
            px.shade(kit.r(lx, 16.4f, 1.6f, 2.8f), kit.col(lx + 0.8f, 0.8f), BLACK, outline = false, lo = 1, hi = 3)
            kit.dot(lx + 1.6f, 19f, BLACK[3])
        }
        val bib = kit.e(15f, 11.5f, 3.6f, 4.5f)
        px.shade(bib, kit.d(15f, 11f, 3.6f, 4.5f), CREAM, lo = 1, texture = fur(13, 0.12f))
        for (i in 0 until 5) kit.dot(12f + i * 0.9f, 15.6f + (i % 2) * 0.5f, CREAM[2])
        val flick = if (f == 3) 0.8f else 0f
        px.shade(kit.p(13.6f, 5f, 14.2f, -0.2f + flick, 17f, 3f), facing(), FOX_R, lo = 1)
        px.shade(kit.p(17f, 3.5f, 19f, -0.2f, 20.2f, 4.5f), facing(), FOX_R, lo = 2)
        px.shade(kit.p(17.8f, 3.6f, 18.9f, 1.2f, 19.5f, 4f), facing(), CREAM, outline = false, lo = 1, hi = 3)
        kit.dot(14.2f, 0.2f + flick, BLACK[1]); kit.dot(19f, 0.1f, BLACK[1]); kit.dot(14.4f, 0.8f + flick, BLACK[2])
        px.shade(kit.e(17.5f, 6.5f, 4f, 3.3f), kit.d(17.5f, 6.5f, 4f, 3.3f), FOX_R, texture = fur(14, 0.1f))
        val snout = kit.p(19f, 5.2f, 25f, 7.3f, 24.5f, 8.4f, 19f, 9.5f)
        px.shade(snout, kit.d(21f, 7.5f, 4f, 2f), FOX_R, lo = 2)
        px.shade(and(union(snout, kit.e(17.5f, 6.5f, 4f, 3.3f)), kit.r(16f, 7.9f, 10f, 4f)), facing(), CREAM, outline = false, lo = 2)
        kit.dot(25f, 7.2f, INK); kit.dot(24.6f, 6.8f, rgb(90, 80, 90))
        kit.line(20.6f, 6.8f, 22.4f, 7.6f, FOX_R[1])
        eye(kit, 20.2f, 6f, blink, FOX_R[1], iris = rgb(200, 140, 40))
        whiskers(kit, 23.4f, 8.6f)
        return kit.sprite(12f, 21.6f)
    }

    private fun foxAsleep(f: Int): Sprite {
        val kit = Kit(24f, 14f)
        val px = kit.px
        val breath = if (f % 4 < 2) 0f else 0.4f
        px.shade(kit.e(12f, 8.5f, 10f, 5f + breath), kit.d(12f, 8f, 10f, 5.5f), FOX_R, texture = fur(14))
        val tail = kit.e(14f, 10.5f, 8.5f, 2.6f)
        px.shade(tail, kit.d(14f, 10f, 8.5f, 3f), FOX_R, texture = fur(15, 0.24f), rim = FOX_R[5])
        px.shade(and(tail, kit.r(19f, 7f, 6f, 6f)), facing(), CREAM, outline = false, lo = 2)
        px.shade(kit.p(5f, 5f, 4.5f, 1f, 7.5f, 3.5f), facing(), FOX_R, lo = 1)
        px.shade(kit.p(8f, 4.5f, 9.5f, 1.2f, 10.5f, 5f), facing(), FOX_R, lo = 2)
        kit.dot(4.6f, 1.2f, BLACK[1]); kit.dot(9.5f, 1.4f, BLACK[1])
        eye(kit, 7.6f, 6.6f, true, FOX_R[0])
        return kit.sprite(12f, 13.4f)
    }

    private fun owl(f: Int, blink: Boolean): Sprite {
        val kit = Kit(16f, 21f)
        val px = kit.px
        for (tx in listOf(5.4f, 6.2f, 9.6f, 10.4f)) kit.dot(tx, 20.2f, rgb(170, 160, 120))
        px.shade(kit.e(8f, 13f, 5.6f, 7f), kit.d(8f, 12f, 5.6f, 7f), OWL_R, texture = { x, y -> if ((x * 3 + y) % 5 == 0) -0.28f else if ((x + y * 2) % 7 == 0) 0.18f else 0f })
        px.shade(and(kit.e(8f, 14f, 3.4f, 5f), kit.r(4f, 10f, 8f, 10f)), kit.d(8f, 13f, 3.4f, 5f), Ramp(rgb(110, 84, 60), rgb(160, 126, 90), rgb(204, 170, 126), rgb(230, 206, 168)), outline = false, texture = { x, y -> if (y % 3 == 0 && x % 2 == 0) -0.3f else 0f })
        for (wx in listOf(3.6f, 12.4f)) px.shade(kit.e(wx, 13.5f, 2.2f, 5f), kit.d(wx, 13f, 2.2f, 5f), OWL_R, lo = 0, hi = 3, texture = { _, y -> if (y % 3 == 0) -0.25f else 0f })
        px.shade(kit.p(3f, 5f, 2f, 0.4f, 6f, 4f), facing(), OWL_R, lo = 1)
        px.shade(kit.p(13f, 5f, 14f, 0.4f, 10f, 4f), facing(), OWL_R, lo = 1)
        px.shade(kit.e(8f, 6.5f, 5f, 3.8f), kit.d(8f, 6.5f, 5f, 3.8f), DISC, lo = 0, line = OWL_R[1])
        kit.line(8f, 3.2f, 8f, 7.6f, DISC[1])
        for (ex in listOf(5.6f, 10.4f)) {
            if (blink) kit.line(ex - 1f, 6.4f, ex + 1f, 6.4f, DISC[0]) else {
                px.shade(kit.e(ex, 6.2f, 1.4f), facing(), Ramp(rgb(200, 110, 30), rgb(236, 150, 48), rgb(255, 200, 100)), lo = 0, line = INK)
                kit.dot(ex, 6.2f, INK); kit.dot(ex + 0.6f, 6.2f, INK); kit.dot(ex - 0.6f, 5.6f, GLINT)
            }
        }
        px.shade(kit.p(7.3f, 7.6f, 8.7f, 7.6f, 8f, 9.2f), facing(), Ramp(rgb(50, 44, 40), rgb(90, 80, 70), rgb(130, 120, 106)), outline = false)
        return kit.sprite(8f, 20.4f)
    }

    /** The heron, or with [crane] the crane: long legs, a long neck, a long beak. */
    private fun wader(f: Int, blink: Boolean, sleep: Boolean, crane: Boolean): Sprite {
        val kit = Kit(20f, 34f)
        val px = kit.px
        val legs = if (crane) rgb(52, 50, 56) else rgb(170, 150, 100)
        kit.line(7f, 21.5f, 7f, 33.4f, legs); kit.line(9f, 21.5f, 10f, 33.4f, legs)
        kit.line(7.2f, 21.5f, 7.2f, 27f, mixc(legs, INK, 0.3f))
        kit.dot(6f, 33.4f, legs); kit.dot(8f, 33.4f, legs); kit.dot(11f, 33.4f, legs)
        val body = kit.p(2f, 21.5f, 4f, 16.5f, 9f, 14.5f, 12.5f, 16f, 11f, 20.5f, 6f, 22.5f)
        px.shade(body, kit.d(7f, 18f, 6f, 4f), GREY, texture = feathers(0.08f), rim = GREY[5])
        px.shade(kit.p(2.5f, 21f, 5f, 17f, 10f, 16.5f, 8f, 20.5f), kit.d(6f, 19f, 4f, 2.5f), GREY, lo = 0, hi = 3)
        for (i in 0 until 4) kit.line(3f + i * 1.4f, 20.6f - i * 0.6f, 4.4f + i * 1.4f, 19f - i * 0.6f, GREY[1])
        if (crane) px.shade(kit.p(0f, 17f, 5f, 18f, 3f, 25f, 0.5f, 24f), facing(), BLACK, lo = 1, texture = { x, _ -> if (x % 2 == 0) -0.2f else 0f })
        else kit.line(9f, 15.4f, 11.6f, 16.8f, rgb(50, 50, 60))
        val dark = if (crane) BLACK else GREY
        val bob = if (!sleep && (f == 3 || f == 4)) 0.7f else 0f
        if (sleep) {
            px.shade(kit.e(11.5f, 13.5f, 2.8f, 2.4f), kit.d(11.5f, 13.5f, 2.8f, 2.4f), dark, lo = 2)
            kit.line(13f, 14f, 16f, 15f, if (crane) rgb(70, 70, 70) else BEAK[2])
            eye(kit, 12.2f, 13f, true, if (crane) GREY[4] else GREY[1])
            if (crane) kit.dot(11f, 11.2f, rgb(200, 40, 40))
            return kit.sprite(8f, 33.4f)
        }
        val neckC = if (crane) BLACK else Ramp(GREY[2], GREY[3], GREY[4], GREY[5])
        kit.limb(10.4f, 15.4f, 11.2f, 11f, 1.8f, neckC, lo = 1)
        kit.limb(11.2f, 11f, 12.6f, 6.6f + bob, 1.6f, neckC, lo = 1)
        if (crane) kit.line(12.4f, 6.6f + bob, 11.6f, 12f, GREY[5]) else for (i in 0 until 5) kit.dot(11.6f + i * 0.2f, 8f + i * 1.4f + bob, INK)
        px.shade(kit.e(13.5f, 5f + bob, 2.6f, 2f), kit.d(13.5f, 5f, 2.6f, 2f), if (crane) BLACK else GREY, lo = 2)
        if (crane) {
            kit.dot(12.6f, 3.2f + bob, rgb(200, 40, 40)); kit.dot(13.2f, 3.2f + bob, rgb(220, 60, 50))
            kit.line(11.4f, 5.8f + bob, 13.2f, 6.4f + bob, GREY[5])
        } else {
            kit.line(12f, 4f + bob, 7.4f, 3.4f + bob, INK); kit.line(12f, 4.4f + bob, 8f, 4.2f + bob, BLACK[2])
        }
        val bk = if (crane) Ramp(rgb(50, 50, 50), rgb(80, 80, 80), rgb(120, 120, 120), rgb(150, 150, 150)) else BEAK
        val len = if (crane) 3.4f else 5.4f
        px.shade(kit.p(15.4f, 4.4f + bob, 15.6f + len, 5.4f + bob, 15.4f, 6f + bob), facing(0f, -0.5f, 1f), bk, outline = false, lo = 0)
        eye(kit, 14.4f, 4.4f + bob, blink, GREY[2], iris = if (crane) rgb(200, 60, 40) else rgb(230, 200, 60))
        return kit.sprite(8f, 33.4f)
    }

    private fun deer(f: Int, blink: Boolean, r: Ramp, white: Boolean): Sprite {
        val kit = Kit(28f, 29f)
        val px = kit.px
        val dark = if (white) r[1] else r[0]
        for ((x, back) in listOf(5f to true, 8f to true, 18f to false, 20.4f to false)) {
            val c = if (back) r[1] else r[2]
            kit.limb(x, 17f, x + (if (back) -0.4f else 0.3f), 23f, 1.4f, Ramp(r[0], c, mixc(c, r[3], 0.4f)), lo = 0)
            kit.line(x + (if (back) -0.4f else 0.3f), 23f, x, 28f, c)
            kit.dot(x, 28.4f, dark); kit.dot(x + 0.6f, 28.4f, dark)
        }
        val graze = f == 4 || f == 5
        px.shade(kit.e(12f, 15f, 9.4f, 5f), kit.d(12f, 14f, 9.4f, 5.4f), r, texture = fur(17, 0.12f), rim = r[5])
        px.shade(kit.e(3.6f, 14f, 2.4f, 3f), kit.d(3.6f, 13.6f, 2.4f, 3f), CREAM, lo = 2, outline = false)
        val nx = if (graze) 1f else 0f
        val ny = if (graze) 4f else 0f
        px.shade(kit.p(15.4f, 14f, 18.6f + nx, 6f + ny, 23f + nx, 6.5f + ny, 21.6f, 14.6f), kit.d(19f, 10f, 3.4f, 5f), r, lo = 1, texture = fur(18, 0.1f))
        px.shade(kit.e(22.5f + nx, 5.5f + ny, 3.2f, 2.6f), kit.d(22.5f, 5.5f, 3.2f, 2.6f), r, texture = fur(19, 0.08f))
        px.shade(kit.p(24f + nx, 4.8f + ny, 27.5f + nx, 6.5f + ny, 24.5f + nx, 8.2f + ny), facing(), r, lo = 2)
        px.shade(and(kit.e(26f + nx, 7.4f + ny, 1.6f, 1f), kit.r(24f + nx, 6.8f + ny, 4f, 2f)), facing(), CREAM, outline = false, lo = 3)
        kit.dot(27.2f + nx, 6.4f + ny, if (white) r[1] else INK); kit.dot(27.2f + nx, 6f + ny, if (white) r[2] else rgb(90, 80, 90))
        val ear = if (f == 2) -0.8f else 0f
        px.shade(kit.p(20.5f + nx, 3.5f + ny, 19f + nx + ear, -0.2f + ny, 21.8f + nx, 2.6f + ny), facing(), r, lo = 1)
        kit.dot(20.4f + nx + ear * 0.5f, 1.6f + ny, r[0])
        val ac = if (white) r[2] else rgb(110, 90, 70)
        kit.line(22f + nx, 3.2f + ny, 22.4f + nx, 0.2f + ny, ac); kit.dot(23.2f + nx, 0.8f + ny, ac); kit.dot(21.6f + nx, 0.6f + ny, ac)
        eye(kit, 24f + nx, 4.8f + ny, blink, r[1], big = true)
        if (white) {
            val glow = Px(px.w, px.h)
            for (y in 0 until px.h) for (x in 0 until px.w) if (px.has(x, y)) {
                for (dy in -1..1) for (dx in -1..1) if (!px.has(x + dx, y + dy)) glow.set(x + dx, y + dy, rgb(220, 236, 255), 70)
            }
            glow.blit(px, 0, 0)
            return Sprite(glow, kit.x(12f), kit.y(28.4f))
        }
        return kit.sprite(12f, 28.4f)
    }

    private fun deerLying(f: Int, r: Ramp): Sprite {
        val kit = Kit(28f, 16f)
        val px = kit.px
        val breath = if (f % 4 < 2) 0f else 0.4f
        px.shade(kit.e(12f, 11f, 10f, 4.4f + breath), kit.d(12f, 10.5f, 10f, 4.8f), r, texture = fur(19, 0.12f), rim = r[5])
        px.shade(kit.e(4f, 11f, 2.2f, 2.4f), facing(), CREAM, lo = 2, outline = false)
        kit.line(15f, 15.2f, 21f, 15.2f, r[0])
        px.shade(kit.e(19.5f, 7f, 3f, 2.6f), kit.d(19.5f, 7f, 3f, 2.6f), r, texture = fur(20, 0.08f))
        px.shade(kit.p(21f, 6.2f, 24.5f, 8f, 21.5f, 9.6f), facing(), r, lo = 2)
        px.shade(kit.p(17.5f, 5f, 16f, 1.6f, 19f, 4f), facing(), r, lo = 1)
        kit.dot(24.4f, 7.8f, INK)
        eye(kit, 20.6f, 6.8f, true, r[0])
        return kit.sprite(12f, 15.4f)
    }

    private fun bear(f: Int, blink: Boolean): Sprite {
        val kit = Kit(36f, 24f)
        val px = kit.px
        for ((x, back) in listOf(5f to true, 10f to false, 20f to true, 25f to false)) {
            px.shade(kit.r(x, 13f, 4.5f, 10f), kit.col(x + 2.25f, 2.25f), BEAR_R, lo = if (back) 0 else 1, hi = if (back) 3 else 4, texture = fur(30 + x.toInt(), 0.1f))
            for (c in 0 until 3) kit.dot(x + 1.6f + c * 1.1f, 23.2f, rgb(200, 190, 170))
        }
        px.shade(kit.e(15f, 12f, 12f, 7f), kit.d(15f, 11f, 12f, 7.5f), BEAR_R, texture = fur(20, 0.16f), rim = BEAR_R[5])
        px.shade(kit.e(21f, 8.5f, 6.5f, 5f), kit.d(21f, 8f, 6.5f, 5f), BEAR_R, texture = fur(21, 0.16f), outline = false)
        val sniff = if (f == 3 || f == 4) 0.7f else 0f
        px.shade(kit.e(28f, 11f + sniff, 5f, 4.4f), kit.d(28f, 11f, 5f, 4.4f), BEAR_R, texture = fur(22, 0.12f))
        px.shade(kit.e(32f, 12.4f + sniff, 2.8f, 2.2f), kit.d(32f, 12f, 2.8f, 2.2f), MUZZLE, lo = 0)
        kit.dot(34.4f, 11.6f + sniff, INK); kit.dot(34.8f, 12f + sniff, INK); kit.dot(34.2f, 11.2f + sniff, rgb(90, 80, 90))
        kit.line(32f, 13.6f + sniff, 33.6f, 13.8f + sniff, MUZZLE[0])
        px.shade(kit.e(26f, 6.6f + sniff, 1.9f, 1.9f), kit.d(26f, 6.6f, 1.9f, 1.9f), BEAR_R, lo = 1)
        kit.dot(26f, 6.8f + sniff, MUZZLE[1])
        eye(kit, 30f, 9.8f + sniff, blink, BEAR_R[1])
        return kit.sprite(16f, 23.4f)
    }

    private fun bearAsleep(f: Int): Sprite {
        val kit = Kit(36f, 16f)
        val px = kit.px
        val breath = if (f % 4 < 2) 0f else 0.5f
        px.shade(kit.e(15f, 9f, 13f, 6f + breath), kit.d(15f, 8f, 13f, 6.5f), BEAR_R, texture = fur(23, 0.16f), rim = BEAR_R[5])
        px.shade(kit.e(28f, 14.5f, 4.4f, 1.4f), facing(), BEAR_R, lo = 1)
        px.shade(kit.e(29f, 11f, 5f, 3.8f), kit.d(29f, 11f, 5f, 3.8f), BEAR_R, texture = fur(24, 0.1f))
        px.shade(kit.e(33f, 12.6f, 2.4f, 1.8f), facing(), MUZZLE, lo = 0)
        kit.dot(35f, 12.2f, INK)
        px.shade(kit.e(27f, 7.4f, 1.8f, 1.6f), kit.d(27f, 7.4f, 1.8f, 1.6f), BEAR_R, lo = 1)
        eye(kit, 30f, 10.2f, true, BEAR_R[0])
        return kit.sprite(16f, 15.4f)
    }

    /* ---------- Lake ---------- */

    /** A mallard drake afloat: grey flank, chestnut breast, green head, white collar, blue in the wing. */
    private fun duck(f: Int, blink: Boolean): Sprite {
        val kit = Kit(22f, 12f)
        val px = kit.px
        val bob = if (f % 4 < 2) 0f else 0.4f
        val flank = Ramp(rgb(90, 88, 84), rgb(140, 136, 128), rgb(184, 180, 170), rgb(214, 210, 200), rgb(236, 232, 224))
        px.shade(kit.p(2f, 6.4f + bob, 0.6f, 4.6f + bob, 3.4f, 5.2f + bob), facing(), BLACK, lo = 1)
        kit.dot(1.6f, 4f + bob, BLACK[2])
        px.shade(kit.p(1.6f, 6.4f + bob, 4f, 4.4f + bob, 14f, 4.4f + bob, 17f, 7f + bob, 15.6f, 10f + bob, 4f, 10f + bob), kit.d(9f, 6.6f, 8f, 4f), flank, texture = { x, y -> if (y % 2 == 0 && x % 3 == 0) -0.12f else 0f })
        px.shade(kit.e(15.2f, 7.4f + bob, 3f, 2.8f), kit.d(15f, 7f, 3f, 2.8f), Ramp(rgb(70, 30, 30), rgb(110, 50, 40), rgb(150, 76, 56), rgb(180, 110, 80)), outline = false)
        px.shade(kit.e(8f, 5.8f + bob, 5f, 1.6f), kit.d(8f, 5.6f, 5f, 1.6f), Ramp(rgb(70, 60, 50), rgb(110, 96, 80), rgb(150, 136, 116)), lo = 0)
        kit.dot(5.4f, 6.2f + bob, rgb(60, 80, 200)); kit.dot(6f, 6.2f + bob, rgb(90, 120, 230)); kit.dot(6.6f, 6.2f + bob, rgb(60, 80, 200))
        kit.line(14.4f, 4.6f + bob, 16.4f, 4.6f + bob, rgb(250, 250, 246))
        val dip = if (f == 4) 0.8f else 0f
        px.shade(kit.e(16.6f, 2.8f + bob + dip, 2.6f, 2.2f), kit.d(16.4f, 2.6f, 2.6f, 2.2f), Ramp(rgb(10, 50, 30), rgb(20, 80, 50), rgb(40, 120, 70), rgb(90, 170, 110)))
        px.shade(kit.p(18.4f, 2.4f + bob + dip, 21.6f, 3.4f + bob + dip, 21f, 4.2f + bob + dip, 18.4f, 4f + bob + dip), facing(0f, -0.4f, 1f), BEAK, outline = false)
        kit.dot(21.4f, 3.4f + bob + dip, INK)
        eye(kit, 17.4f, 2.2f + bob + dip, blink, rgb(20, 80, 50))
        for (x in 1 until 17) kit.dot(x.toFloat() + (f % 2) * 0.6f, 10.2f, rgb(230, 240, 246), 150)
        return kit.sprite(9f, 10f)
    }

    private fun swan(f: Int, blink: Boolean): Sprite {
        val kit = Kit(28f, 22f)
        val px = kit.px
        val bob = if (f % 4 < 2) 0f else 0.4f
        px.shade(kit.e(12f, 16f + bob, 10f, 4.4f), kit.d(12f, 15f, 10f, 4.6f), WHITE, texture = feathers(0.06f))
        val wing = kit.p(3f, 15f + bob, 6f, 9.4f + bob, 13f, 8.4f + bob, 18f, 12f + bob, 16f, 15.6f + bob, 6f, 16f + bob)
        px.shade(wing, kit.d(11f, 12f, 7f, 4f), WHITE, texture = { x, y -> if ((x + y) % 4 == 0) -0.16f else 0f }, rim = WHITE[4])
        for (i in 0 until 6) kit.line(5f + i * 2f, 14.6f + bob, 6f + i * 2f, 11f + bob - i * 0.2f, WHITE[1])
        kit.limb(19f, 15f + bob, 20.4f, 10f + bob, 2.2f, WHITE, lo = 1)
        kit.limb(20.4f, 10f + bob, 19.6f, 5f + bob, 1.8f, WHITE, lo = 1)
        kit.limb(19.6f, 5f + bob, 21f, 2.6f + bob, 1.8f, WHITE, lo = 1)
        px.shade(kit.e(22f, 3f + bob, 2.4f, 1.8f), kit.d(22f, 2.8f, 2.4f, 1.8f), WHITE)
        px.shade(kit.p(23.4f, 2.6f + bob, 27.4f, 4f + bob, 23.4f, 4.6f + bob), facing(), ORANGE, outline = false)
        px.shade(kit.e(23.6f, 3.4f + bob, 1f, 1.2f), facing(), BLACK, outline = false)
        eye(kit, 22.4f, 2.6f + bob, blink, WHITE[1])
        for (x in 2 until 23) kit.dot(x.toFloat() + (f % 2) * 0.6f, 20.2f, rgb(230, 240, 246), 150)
        return kit.sprite(12f, 20f)
    }

    private fun beaver(f: Int, blink: Boolean): Sprite {
        val kit = Kit(24f, 13f)
        val px = kit.px
        px.shade(kit.e(3.6f, 10.6f, 3.8f, 1.4f), facing(0f, -0.4f, 1f), Ramp(rgb(30, 26, 26), rgb(56, 50, 48), rgb(84, 78, 74)), texture = { x, y -> if ((x + y) % 2 == 0) -0.2f else 0f })
        px.shade(kit.e(11f, 7.4f, 7.2f, 4.6f), kit.d(11f, 7f, 7.2f, 4.8f), BEAVER_R, texture = fur(40, 0.2f), rim = BEAVER_R[4])
        px.shade(kit.e(10f, 11.6f, 2.4f, 1f), facing(), BEAVER_R, lo = 0, hi = 2)
        val chew = if (f % 4 < 2) 0f else 0.3f
        px.shade(kit.e(17.6f, 6.4f, 3.6f, 3f), kit.d(17.6f, 6.2f, 3.6f, 3f), BEAVER_R, texture = fur(41, 0.12f))
        px.shade(kit.e(20.4f, 7.4f + chew, 1.6f, 1.4f), facing(), MUZZLE, lo = 0)
        kit.dot(21.6f, 6.6f, INK)
        kit.dot(20.6f, 8.8f + chew, rgb(230, 140, 50)); kit.dot(21f, 8.8f + chew, rgb(230, 140, 50))
        kit.line(16f, 10f, 22f, 9f + chew, rgb(110, 80, 50))
        kit.dot(18.8f, 9.6f, BEAVER_R[1])
        px.shade(kit.e(16f, 3.8f, 1f), facing(), BEAVER_R, lo = 1)
        eye(kit, 18.6f, 5.4f, blink, BEAVER_R[1])
        whiskers(kit, 21f, 7.6f)
        return kit.sprite(11f, 12.4f)
    }

    private fun otter(f: Int, blink: Boolean): Sprite {
        val kit = Kit(26f, 13f)
        val px = kit.px
        val lift = if (f == 3 || f == 4) -0.8f else 0f
        px.shade(kit.p(0.4f, 9.6f, 4f, 7.6f, 6f, 9.4f, 3f, 10.4f), kit.d(3f, 9f, 3f, 1.4f), OTTER_R, lo = 1)
        px.shade(kit.e(11f, 8.4f, 8f, 3.4f), kit.d(11f, 8f, 8f, 3.4f), OTTER_R, texture = fur(42, 0.12f), rim = OTTER_R[5])
        for (lx in listOf(7f, 15f)) px.shade(kit.e(lx, 11.4f, 1.8f, 1.2f), facing(), OTTER_R, lo = 0, hi = 2)
        px.shade(kit.p(16f, 7f, 19f, 3.6f + lift, 21f, 4.6f + lift, 19f, 9.6f), kit.d(18.6f, 6.6f, 2.4f, 3.6f), OTTER_R, lo = 1)
        px.shade(kit.e(21f, 4.2f + lift, 2.8f, 2.2f), kit.d(21f, 4f, 2.8f, 2.2f), OTTER_R, texture = fur(43, 0.08f))
        px.shade(and(kit.p(17.6f, 9f, 19.4f, 5f + lift, 23f, 5.4f + lift, 20f, 9.4f), kit.r(17f, 5.2f + lift, 7f, 5f)), facing(), CREAM, outline = false, lo = 1)
        kit.dot(23.6f, 4f + lift, INK)
        kit.dot(19.6f, 2.4f + lift, OTTER_R[1])
        eye(kit, 21.6f, 3.4f + lift, blink, OTTER_R[1])
        whiskers(kit, 22.6f, 4.8f + lift)
        return kit.sprite(11f, 12f)
    }

    /** A bull moose: high shoulders, a long drooping muzzle, the bell, and broad palmate antlers. */
    private fun moose(f: Int, blink: Boolean): Sprite {
        val kit = Kit(34f, 34f)
        val px = kit.px
        val legC = Ramp(rgb(90, 80, 70), rgb(140, 128, 110), rgb(180, 170, 150))
        for ((x, back) in listOf(6f to true, 9f to true, 20f to false, 22.4f to false)) {
            kit.limb(x, 20f, x, 27f, 2f, MOOSE_R, lo = 0, hi = if (back) 2 else 3)
            kit.limb(x, 27f, x, 32.4f, 1.3f, legC, lo = 0)
            kit.dot(x, 33f, INK); kit.dot(x + 0.6f, 33f, INK)
        }
        px.shade(kit.e(14f, 17f, 10.6f, 6f), kit.d(14f, 16f, 10.6f, 6.4f), MOOSE_R, texture = fur(44, 0.14f), rim = MOOSE_R[5])
        px.shade(kit.e(19.6f, 12.4f, 6.6f, 5.4f), kit.d(19.6f, 12f, 6.6f, 5.4f), MOOSE_R, texture = fur(45, 0.16f), outline = false)
        for (i in 0 until 6) kit.dot(15f + i * 1.4f, 7.6f - (i % 2) * 0.5f, MOOSE_R[0])
        val dip = if (f == 4 || f == 5) 1f else 0f
        px.shade(kit.p(23f, 11f, 26f, 8f + dip, 32.6f, 11.6f + dip, 33.4f, 14.4f + dip, 30f, 15f + dip, 25f, 15f), kit.d(28f, 11.6f, 5f, 3.4f), MOOSE_R, texture = fur(46, 0.1f))
        px.shade(kit.e(31.6f, 13f + dip, 2.2f, 2f), kit.d(31.6f, 12.6f, 2.2f, 2f), MOOSE_R, lo = 1)
        kit.dot(33f, 12.6f + dip, INK)
        px.shade(kit.p(25f, 15f, 27f, 15f + dip, 26f, 19.4f), facing(), MOOSE_R, lo = 0, hi = 2)
        val antler = kit.p(22f, 8.4f + dip, 20f, 4f + dip, 16f, 1.6f + dip, 17.6f, 0.4f + dip, 22.6f, 2.6f + dip, 24.4f, 6.6f + dip)
        px.shade(antler, kit.d(20f, 4f, 4f, 3f, 0.4f), ANTLER, texture = { x, y -> if ((x + y) % 4 == 0) -0.15f else 0f })
        for (i in 0 until 5) kit.line(16.6f + i * 1.3f, 1.4f + dip - i * 0.1f, 16f + i * 1.4f, 0f + dip, ANTLER[2])
        px.shade(kit.p(24f, 7f + dip, 26.6f, 3.2f + dip, 28.4f, 3.6f + dip, 26f, 8f + dip), facing(), ANTLER, lo = 1)
        px.shade(kit.p(23.6f, 9.6f + dip, 22.4f, 7.4f + dip, 24.4f, 8.2f + dip), facing(), MOOSE_R, lo = 1)
        eye(kit, 27f, 10f + dip, blink, MOOSE_R[1])
        return kit.sprite(14f, 33f)
    }

    /* ---------- Mountains ---------- */

    private fun pika(f: Int, blink: Boolean): Sprite {
        val kit = Kit(14f, 10f)
        val px = kit.px
        val r = Ramp(rgb(60, 50, 44), rgb(100, 86, 72), rgb(140, 122, 100), rgb(176, 158, 130), rgb(206, 192, 166))
        val munch = if (f % 4 < 2) 0f else 0.3f
        px.shade(kit.e(6f, 6.2f, 5f, 3.4f), kit.d(6f, 5.8f, 5f, 3.6f), r, texture = fur(47, 0.18f))
        px.shade(kit.e(9.4f, 4.6f, 2.8f, 2.4f), kit.d(9.4f, 4.4f, 2.8f, 2.4f), r, texture = fur(48, 0.1f))
        px.shade(kit.e(7.8f, 2f, 1.4f), kit.d(7.8f, 2f, 1.4f), r, lo = 1, line = r[0])
        kit.dot(7.4f, 1.4f, r[4])
        kit.dot(12f, 5.2f + munch, INK)
        kit.line(11.2f, 6.2f + munch, 13f, 7.4f, rgb(80, 130, 60)); kit.dot(13f, 7f, rgb(240, 200, 80)); kit.dot(12.6f, 7.8f, rgb(200, 120, 200))
        for (fx in listOf(4f, 8f)) kit.dot(fx, 9.4f, r[1])
        eye(kit, 10.2f, 3.8f, blink, r[1])
        whiskers(kit, 11.6f, 5.6f)
        return kit.sprite(6f, 9.4f)
    }

    /** A marmot standing up on its hind legs, the sentinel. */
    private fun marmot(f: Int, blink: Boolean): Sprite {
        val kit = Kit(14f, 18f)
        val px = kit.px
        val turn = if (f in 3..5) 0.5f else 0f
        px.shade(kit.p(3f, 14.6f, 1f, 17f, 3.6f, 17.4f), facing(), Ramp(rgb(40, 30, 26), rgb(70, 54, 44), rgb(100, 80, 64)), lo = 0)
        px.shade(kit.e(7f, 11.4f, 4.4f, 5.6f), kit.d(7f, 11f, 4.4f, 5.8f), TAWNY, texture = fur(49, 0.18f), rim = TAWNY[5])
        px.shade(and(kit.e(8f, 12f, 2.6f, 4.4f), kit.r(6f, 7f, 6f, 10f)), facing(), CREAM, outline = false, lo = 2)
        px.shade(kit.e(6.4f, 17f, 2.8f, 1f), facing(), TAWNY, lo = 1, hi = 3)
        px.shade(kit.e(9.6f, 10f, 1.4f, 1.6f), kit.d(9.6f, 10f, 1.4f, 1.6f), TAWNY, lo = 1)
        kit.dot(10.4f, 11f, TAWNY[0])
        px.shade(kit.e(8f + turn, 4.6f, 3.2f, 2.8f), kit.d(8f, 4.4f, 3.2f, 2.8f), TAWNY, texture = fur(50, 0.1f))
        px.shade(and(kit.e(8f + turn, 3.6f, 3.2f, 1.6f), kit.r(5f, 2f, 7f, 2.4f)), facing(), Ramp(rgb(60, 46, 36), rgb(90, 70, 54)), outline = false)
        px.shade(and(kit.e(10.2f + turn, 5.4f, 1.6f, 1.2f), kit.r(8f, 4.6f, 5f, 3f)), facing(), CREAM, outline = false, lo = 2)
        kit.dot(11.2f + turn, 4.8f, INK)
        px.shade(kit.e(6.4f + turn, 1.8f, 1f), facing(), TAWNY, lo = 1)
        eye(kit, 9.4f + turn, 3.8f, blink, TAWNY[1])
        whiskers(kit, 10.6f + turn, 5.8f)
        return kit.sprite(6.6f, 17.4f)
    }

    /** An alpine ibex: a sturdy goat with great ridged horns swept back, and a beard. */
    private fun ibex(f: Int, blink: Boolean): Sprite {
        val kit = Kit(30f, 30f)
        val px = kit.px
        for ((x, back) in listOf(6f to true, 9f to true, 19f to false, 21.4f to false)) {
            kit.limb(x, 18f, x + (if (back) -0.3f else 0.3f), 28.4f, 2.2f, IBEX_R, lo = 0, hi = if (back) 2 else 3)
            kit.line(x - 0.6f, 25f, x + 0.6f, 25f, IBEX_R[0])
            kit.dot(x, 28.8f, INK); kit.dot(x + 0.6f, 28.8f, INK)
        }
        px.shade(kit.e(13f, 16.4f, 9.6f, 6.2f), kit.d(13f, 15.6f, 9.6f, 6.4f), IBEX_R, texture = fur(51, 0.14f), rim = IBEX_R[5])
        px.shade(and(kit.e(13f, 18.6f, 8f, 2.4f), kit.r(5f, 18f, 16f, 4f)), facing(), Ramp(IBEX_R[3], IBEX_R[4]), outline = false)
        px.shade(kit.p(2.6f, 12f, 1.4f, 10.4f, 3.4f, 11f), facing(), IBEX_R, lo = 1)
        val look = if (f in 2..4) 0.6f else 0f
        px.shade(kit.p(18f, 13f, 21f, 7f, 24.6f, 7.4f, 23f, 15f), kit.d(21f, 11f, 3f, 4f), IBEX_R, texture = fur(52, 0.1f))
        px.shade(kit.p(21f, 6f + look, 25f, 6f + look, 28.4f, 9.6f + look, 27f, 11f + look, 22f, 10f + look), kit.d(24f, 8f, 4f, 2.6f), IBEX_R, texture = fur(53, 0.08f))
        px.shade(kit.p(24.4f, 10.4f + look, 26f, 10.8f + look, 25f, 13.6f + look), facing(), Ramp(rgb(50, 40, 34), rgb(80, 66, 54)), outline = false)
        kit.dot(28.4f, 9.6f + look, INK)
        // The horns: a thick ridged arc swept back from the brow, tapering.
        val arc = (0..10).map { i ->
            val a = PI.toFloat() * (0.05f + i / 10f * 0.75f)
            val cx = 18.4f + cos(a) * 4.6f
            val cy = 7f + look - sin(a) * 6.4f
            kit.e(cx, cy, 1.5f - i * 0.09f)
        }
        px.shade(union(*arc.toTypedArray()), kit.d(18f, 3f, 5f, 5f), HORN, texture = { x, y -> if ((x + y) % 3 == 0) -0.4f else 0f })
        px.shade(kit.p(21.6f, 6.6f + look, 20.6f, 4.6f + look, 22.6f, 5.6f + look), facing(), IBEX_R, lo = 1)
        eye(kit, 24.4f, 7.6f + look, blink, IBEX_R[1], iris = rgb(180, 140, 60))
        return kit.sprite(13f, 28.8f)
    }

    /** A sitting lynx: tufted ears, a ruff of whiskers, spots, a short black-tipped tail. */
    private fun lynx(f: Int, blink: Boolean): Sprite {
        val kit = Kit(22f, 24f)
        val px = kit.px
        px.shade(kit.e(3.4f, 20.6f, 3.4f, 1.6f), facing(), LYNX_R, lo = 1)
        kit.dot(0.6f, 20.4f, INK); kit.dot(1f, 20.8f, INK)
        px.shade(kit.e(9f, 15.4f, 6.4f, 7f), kit.d(9f, 15f, 6.4f, 7f), LYNX_R, texture = spots(54, 0.32f), rim = LYNX_R[5])
        px.shade(kit.e(8.4f, 21.6f, 4.4f, 1.6f), facing(), LYNX_R, lo = 1, hi = 4)
        for (lx in listOf(12.8f, 14.8f)) {
            px.shade(kit.r(lx, 14f, 1.8f, 8f), kit.col(lx + 0.9f, 0.9f), LYNX_R, lo = 1, hi = 4, texture = spots(55, 0.25f))
            kit.dot(lx + 1.6f, 22f, LYNX_R[1])
        }
        px.shade(and(kit.e(13.6f, 12.4f, 3.4f, 4f), kit.r(10f, 9f, 8f, 8f)), facing(), CREAM, outline = false, lo = 2, texture = spots(56, 0.2f))
        val twitch = if (f == 2) 0.6f else 0f
        px.shade(kit.e(15f, 6.6f, 4.4f, 3.8f), kit.d(15f, 6.4f, 4.4f, 3.8f), LYNX_R, texture = spots(57, 0.24f))
        px.shade(kit.p(11.4f, 7f, 10.4f, 10.4f, 13f, 9.4f, 15f, 11f, 17.4f, 9.6f, 19f, 10.6f, 19.6f, 7.6f), facing(), CREAM, lo = 1, outline = false)
        for (i in 0 until 3) kit.dot(12.4f + i * 2.4f, 10.2f, LYNX_R[1])
        px.shade(kit.p(12f, 4.4f, 12.6f + twitch, 0.8f, 14.4f, 3.4f), facing(), LYNX_R, lo = 1)
        px.shade(kit.p(15.4f, 3.2f, 17.6f, 0.6f, 18f, 4f), facing(), LYNX_R, lo = 2)
        kit.line(12.6f + twitch, 0.8f, 12.6f + twitch, 0f, INK)
        kit.line(17.6f, 0.6f, 17.8f, 0f, INK)
        px.shade(and(kit.e(18f, 7.8f, 2f, 1.6f), kit.r(16f, 7f, 5f, 3f)), facing(), CREAM, outline = false, lo = 2)
        kit.dot(19.8f, 7.2f, rgb(120, 80, 80))
        eye(kit, 17f, 5.8f, blink, LYNX_R[1], iris = rgb(200, 170, 70))
        whiskers(kit, 18.6f, 8.4f)
        return kit.sprite(9f, 22.4f)
    }

    /** A snow leopard lying along a rock, its thick tail curled round. */
    private fun leopard(f: Int, blink: Boolean): Sprite {
        val kit = Kit(34f, 14f)
        val px = kit.px
        val sway = if (f % 4 < 2) 0f else 0.6f
        val rosettes: (Int, Int) -> Float = { x, y -> val n = noise(x / 3, y / 3, 58); if (n > 0.28f) -0.5f else if (n > 0.18f) -0.15f else noise(x, y, 59) * 0.1f }
        px.shade(kit.p(4f, 10f, 0.4f, 9f - sway, 0f, 12.4f - sway, 2f, 13.4f, 6f, 12.4f), kit.d(2.6f, 11f, 3f, 2f), LEOPARD, texture = rosettes, lo = 1)
        px.shade(kit.e(14f, 9f, 11f, 4f), kit.d(14f, 8.4f, 11f, 4.4f), LEOPARD, texture = rosettes, rim = LEOPARD[5])
        for (lx in listOf(21f, 25f)) px.shade(kit.e(lx, 12.4f, 2.6f, 1.2f), facing(), LEOPARD, lo = 1)
        px.shade(kit.e(26.6f, 6.4f, 4f, 3.4f), kit.d(26.6f, 6.2f, 4f, 3.4f), LEOPARD, texture = rosettes)
        px.shade(kit.e(24.4f, 3.4f, 1.2f), facing(), LEOPARD, lo = 1)
        px.shade(and(kit.e(29.4f, 7.4f, 1.8f, 1.4f), kit.r(27.4f, 6.6f, 4f, 3f)), facing(), WHITE, outline = false, lo = 2)
        kit.dot(30.6f, 6.6f, rgb(120, 90, 90))
        eye(kit, 28f, 5.6f, blink, LEOPARD[1], iris = rgb(150, 170, 150))
        whiskers(kit, 29.6f, 7.6f)
        return kit.sprite(14f, 13f)
    }

    /* ---------- Sea ---------- */

    private fun crab(f: Int): Sprite {
        val kit = Kit(12f, 8f)
        val px = kit.px
        val snap = if (f % 4 < 2) 0f else 0.6f
        for (i in 0 until 3) { kit.line(3.4f - i * 0.6f, 5f, 1.6f - i * 0.6f + 0.6f, 7.4f, CRAB_R[1]); kit.line(8.6f + i * 0.6f, 5f, 10.4f + i * 0.6f - 0.6f, 7.4f, CRAB_R[1]) }
        px.shade(kit.e(6f, 4.6f, 3.8f, 2.2f), kit.d(6f, 4.2f, 3.8f, 2.4f), CRAB_R, texture = { x, y -> noise(x, y, 60) * 0.15f })
        for ((cx, dir) in listOf(1.6f to -1f, 10.4f to 1f)) {
            kit.line(cx - dir * 0.6f, 3.6f, cx, 2f, CRAB_R[2])
            px.shade(kit.e(cx, 1.6f, 1.3f, 1f), kit.d(cx, 1.4f, 1.3f, 1f), CRAB_R, lo = 1)
            kit.dot(cx + dir * 0.6f, 0.8f - snap + 0.6f, CRAB_R[3])
        }
        kit.dot(5f, 2.2f, INK); kit.dot(7f, 2.2f, INK); kit.dot(5f, 2.8f, CRAB_R[2]); kit.dot(7f, 2.8f, CRAB_R[2])
        return kit.sprite(6f, 7.4f)
    }

    private fun gull(f: Int, blink: Boolean): Sprite {
        val kit = Kit(20f, 16f)
        val px = kit.px
        val legs = rgb(220, 150, 140)
        kit.line(8f, 11f, 8f, 15.4f, legs); kit.line(10f, 11f, 10.4f, 15.4f, legs)
        kit.dot(9f, 15.4f, legs); kit.dot(11.4f, 15.4f, legs)
        px.shade(kit.p(1f, 7f, 5f, 6.4f, 5f, 9f, 2f, 9.4f), facing(), BLACK, lo = 1, hi = 3)
        kit.dot(1.6f, 7.6f, WHITE[4]); kit.dot(2.8f, 8.4f, WHITE[4])
        px.shade(kit.e(9.4f, 8.4f, 6f, 3.4f), kit.d(9.4f, 8f, 6f, 3.6f), WHITE)
        px.shade(kit.e(8f, 7.4f, 5.6f, 2.2f), kit.d(8f, 7f, 5.6f, 2.2f), GREY, lo = 2, texture = feathers(0.12f))
        for (i in 0 until 3) kit.dot(4f + i * 2f, 8.4f, WHITE[4])
        val tilt = if (f in 3..4) -0.6f else 0f
        px.shade(kit.e(14.6f, 4f + tilt, 2.6f, 2.4f), kit.d(14.6f, 3.8f, 2.6f, 2.4f), WHITE)
        px.shade(kit.p(16.6f, 3.6f + tilt, 20f, 4.4f + tilt, 19.6f, 5.4f + tilt, 16.6f, 5.2f + tilt), facing(), BEAK, outline = false)
        kit.dot(18.6f, 5f + tilt, rgb(220, 60, 50))
        eye(kit, 15.6f, 3.4f + tilt, blink, WHITE[1], iris = rgb(220, 200, 80))
        return kit.sprite(9f, 15.4f)
    }

    private fun puffin(f: Int, blink: Boolean): Sprite {
        val kit = Kit(12f, 16f)
        val px = kit.px
        px.shade(kit.e(5.6f, 15.2f, 2.6f, 0.9f), facing(), ORANGE, lo = 1)
        px.shade(kit.e(5.4f, 10f, 4f, 5.2f), kit.d(5.4f, 9.6f, 4f, 5.4f), BLACK)
        px.shade(and(kit.e(6.4f, 10.6f, 3f, 4.4f), kit.r(5f, 6f, 6f, 10f)), kit.d(6.4f, 10.4f, 3f, 4.4f), WHITE, outline = false, lo = 1)
        val nod = if (f == 3) 0.6f else 0f
        px.shade(kit.e(6f, 4f + nod, 3.2f, 3f), kit.d(6f, 3.8f, 3.2f, 3f), BLACK)
        px.shade(and(kit.e(6.6f, 4.4f + nod, 2.4f, 2.2f), kit.r(5f, 2.6f, 5f, 5f)), facing(), Ramp(rgb(170, 170, 176), rgb(226, 226, 230), rgb(246, 246, 248)), outline = false)
        val beak = kit.p(8.6f, 2.6f + nod, 11.6f, 4.6f + nod, 8.6f, 6.6f + nod)
        px.shade(beak, facing(), ORANGE, lo = 1, line = rgb(120, 50, 20))
        kit.line(9.2f, 3.4f + nod, 9.2f, 5.8f + nod, rgb(80, 90, 120)); kit.line(9.8f, 3.6f + nod, 9.8f, 5.6f + nod, rgb(240, 200, 80))
        if (blink) kit.dot(7.4f, 3.8f + nod, BLACK[2]) else { kit.dot(7.4f, 3.8f + nod, INK); kit.dot(7.4f, 3.4f + nod, rgb(220, 60, 50)); kit.dot(6.8f, 3.6f + nod, rgb(150, 150, 160)) }
        return kit.sprite(5.6f, 15.6f)
    }

    private fun seal(f: Int, blink: Boolean): Sprite {
        val kit = Kit(28f, 13f)
        val px = kit.px
        val lift = if (f in 2..5) -0.6f else 0f
        px.shade(kit.p(0f, 9f, 3f, 8f, 3f, 11f, 0.4f, 12f), facing(), SEAL_R, lo = 1)
        px.shade(kit.p(2f, 10f, 8f, 6f, 18f, 5f, 23f, 7f, 22f, 11.6f, 4f, 12f), kit.d(13f, 8.6f, 11f, 4f), SEAL_R, texture = spots(61, 0.25f), rim = SEAL_R[5])
        px.shade(kit.e(16f, 11f, 2.6f, 1f), facing(), SEAL_R, lo = 0, hi = 2)
        px.shade(kit.e(23.6f, 4.6f + lift, 3.4f, 3f), kit.d(23.6f, 4.4f, 3.4f, 3f), SEAL_R, texture = spots(62, 0.15f))
        px.shade(and(kit.e(26.2f, 5.4f + lift, 1.6f, 1.4f), kit.r(24.6f, 4.6f + lift, 4f, 3f)), facing(), SEAL_R, outline = false, lo = 3)
        kit.dot(27.4f, 4.8f + lift, INK)
        val ex = kit.x(24.4f)
        val ey = kit.y(3.6f + lift)
        if (blink) { px.set(ex - 1, ey, SEAL_R[0]); px.set(ex, ey, SEAL_R[0]) } else {
            px.rect(ex - 1, ey - 1, 3, 2, INK); px.set(ex - 1, ey - 1, GLINT)
        }
        whiskers(kit, 26.6f, 5.6f + lift)
        return kit.sprite(13f, 12f)
    }

    /** Far out: a dark back rising, and a spout. */
    private fun whale(f: Int): Sprite {
        val kit = Kit(24f, 14f)
        val px = kit.px
        val back = Ramp(rgb(30, 36, 50), rgb(50, 60, 76), rgb(80, 90, 104))
        px.shade(kit.p(2f, 13f, 6f, 10f, 16f, 9.6f, 21f, 12f, 22f, 13f), kit.d(12f, 12f, 10f, 3f), back, lo = 0)
        px.shade(kit.p(14f, 10f, 15.6f, 8f, 16.6f, 10f), facing(), back, lo = 0)
        if (f < 5) {
            val h = 2f + f * 1.2f
            for (i in 0 until 10) {
                val a = i / 10f
                kit.dot(6f + sin(a * 6f) * (1 + f * 0.5f), 10f - h * a, rgb(230, 240, 246), (220 * (1 - a * 0.5f)).toInt())
            }
        }
        return kit.sprite(12f, 13f)
    }
}

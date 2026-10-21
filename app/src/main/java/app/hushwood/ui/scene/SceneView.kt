package app.hushwood.ui.scene

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.domain.Spot
import app.hushwood.domain.Trust
import app.hushwood.ui.pix.Actor
import app.hushwood.ui.pix.Beasts
import app.hushwood.ui.pix.Clearing
import app.hushwood.ui.pix.Layout
import app.hushwood.ui.pix.Motion
import app.hushwood.ui.pix.Px
import app.hushwood.ui.pix.Stage
import app.hushwood.ui.pix.Things
import app.hushwood.ui.pix.bayer
import app.hushwood.ui.pix.noise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

/** Switches for drawing to a picture instead of a screen: everything holds still, at a fixed hour. */
object Art {
    @Volatile var still = false
    @Volatile var fixedPart: Daypart? = null
    @Volatile var fixedSeason: Season? = null
}

/** Everything that decides how the picture looks right now. */
@Immutable
data class SceneSpec(
    val place: Place,
    val part: Daypart,
    val season: Season,
    val present: List<Beast> = emptyList(),
    val guardian: Beast? = null,
    val trust: Trust? = null,
    /** Where you sit, or null when nobody is in the picture. */
    val spot: Spot? = null,
    /** In a sitting: the guardian sleeps, the fire is lit at the fire. */
    val sitting: Boolean = false,
    /** 0 empty .. 1 full lungs, for the motes gathering round you while you breathe; null when not breathing. */
    val breath: (() -> Float)? = null,
    val offering: Offering? = null,
)

private const val ART_W = 270
private const val DISSOLVE_MS = 1400L

/**
 * The place, alive: the painted background, water glinting, grass swaying, the animals in their frames, snow or
 * leaves or fireflies in the air, and you. Drawn at the picture's own grain and blown up pixel for pixel.
 */
@Composable
fun SceneView(spec: SceneSpec, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        if (wPx <= 0f || hPx <= 0f) return@BoxWithConstraints
        val scale = (wPx / ART_W).roundToInt().coerceAtLeast(2)
        val artW = ceil(wPx / scale).toInt()
        val artH = ceil(hPx / scale).toInt()
        val lay = remember(artW, artH, spec.place) { Layout(artW, artH, spec.place) }
        // Painted off the main thread; for a still picture, right away so it's there when the picture is taken.
        val painted by produceState<ImageBitmap?>(null, artW, artH, spec.place, spec.part, spec.season) {
            if (!Art.still) value = withContext(Dispatchers.Default) { Clearing.paint(artW, artH, spec.part, spec.season, spec.place).bitmap() }
        }
        val bg = if (Art.still) remember(artW, artH, spec.place, spec.part, spec.season) { Clearing.paint(artW, artH, spec.part, spec.season, spec.place).bitmap() } else painted

        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            if (Art.still) return@LaunchedEffect
            while (true) {
                withFrameMillis { }
                now = System.currentTimeMillis()
                delay(70)
            }
        }

        // Who came and when, and who's leaving: they dissolve in and out of the picture.
        val since = remember { mutableStateMapOf<Beast, Long>() }
        val leaving = remember { mutableStateMapOf<Beast, Long>() }
        LaunchedEffect(spec.present) {
            val t = if (Art.still) 0L else System.currentTimeMillis()
            for (b in spec.present) if (b !in since) { since[b] = t; leaving.remove(b) }
            for (b in since.keys.toList()) if (b !in spec.present) { since.remove(b); leaving[b] = t }
        }
        val drawn = (spec.present + leaving.keys.filter { now - (leaving[it] ?: 0) < DISSOLVE_MS }).distinct().sortedBy { it.ordinal }
        val actors = remember(drawn, spec.guardian, spec.trust, spec.spot, spec.sitting, lay) { Stage.cast(lay, drawn, spec.guardian, spec.trust, spec.spot, spec.sitting) }
        val images = remember { HashMap<Px, ImageBitmap>() }
        val glints = remember(lay) { glintsOf(lay) }

        Canvas(Modifier.fillMaxSize()) {
            val ox = ((size.width - artW * scale) / 2f).roundToInt()
            val oy = ((size.height - artH * scale) / 2f).roundToInt()
            val img = bg ?: return@Canvas
            fun blit(p: Px, x: Int, y: Int) {
                val bmp = images.getOrPut(p) { p.bitmap() }
                drawImage(bmp, IntOffset.Zero, IntSize(p.w, p.h), IntOffset(ox + x * scale, oy + y * scale), IntSize(p.w * scale, p.h * scale), filterQuality = FilterQuality.None)
            }
            fun dot(x: Int, y: Int, c: Int, a: Float = 1f) {
                drawRect(Color(c).copy(alpha = a), Offset((ox + x * scale).toFloat(), (oy + y * scale).toFloat()), Size(scale.toFloat(), scale.toFloat()))
            }
            // The bitmap's own size: while a new one is painted after a resize, the old one is drawn as it was.
            drawImage(img, IntOffset.Zero, IntSize(img.width, img.height), IntOffset(ox, oy), IntSize(img.width * scale, img.height * scale), filterQuality = FilterQuality.None)

            val t = if (Art.still) 12f else (now % 1_000_000L) / 1000f
            // The fire flickers quickly; wings and fireflies at an easy pace; grass on a slow wind.
            val f = if (Art.still) 0 else ((now / 180) % Beasts.FRAMES).toInt()
            val slow = if (Art.still) 0 else ((now / 420) % Beasts.FRAMES).toInt()
            val sway = if (Art.still) 0 else Motion.sway(now)
            val night = spec.part == Daypart.NIGHT

            // Light dancing on the water.
            for ((i, g) in glints.withIndex()) if (sin(t * 1.3f + i * 2.1f) > 0.55f) dot(g.first, g.second, 0xFFF4F8FF.toInt(), if (night) 0.35f else 0.7f)

            // The fire, lit when you sit at it, embers otherwise.
            val (fx, fy) = lay.fire
            val lit = spec.sitting && spec.spot == Spot.FIRE
            val fire = if (lit) Things.fire(f) else Things.embers(f)
            blit(cached("fire/${f % 4}/$lit/${spec.part}") { Beasts.graded(fire.px, spec.part) }, fx - fire.ax, fy - fire.ay)

            // What's left on the seat for the animals.
            if (spec.offering != null && !(spec.sitting && spec.spot == Spot.STUMP)) {
                val (sx, sy) = lay.spot(Spot.STUMP)
                val o = cached("offer/${spec.offering}/${spec.part}") { Beasts.graded(Things.offering(spec.offering), spec.part) }
                blit(o, sx - o.w / 2 + lay.x(3), sy - o.h - 1)
            }

            val ground = actors.filter { !it.beast.swarm && !it.beast.flying }.sortedBy { it.y }
            val seat = spec.spot?.let { lay.spot(it) }
            var you = seat == null
            fun drawYou() {
                val (x, y) = seat ?: return
                val fill = spec.breath?.invoke() ?: 0f
                val s = Things.sitter(spec.spot!!, fill, if (spec.sitting) f else 0)
                val p = cached("you/${spec.spot}/${fill > 0.5f}/${(f % 4) / 2}/${spec.part}") { Beasts.graded(s.px, spec.part) }
                blit(p, x - s.ax, y - s.ay)
            }
            for (a in ground) {
                if (!you && a.y > seat!!.second) { drawYou(); you = true }
                drawActor(a, spec, now, since, leaving, ::blit, ::dot)
            }
            if (!you) drawYou()

            // Breath: motes gather round you on the in-breath and spread on the out-breath.
            spec.breath?.let { br ->
                val (x, y) = seat ?: return@let
                val fill = br()
                val r = lay.k * (10f + (1 - fill) * 18f)
                for (i in 0 until 12) {
                    val a = i / 12f * 6.283f + t * 0.2f
                    val mx = x + (kotlin.math.cos(a) * r).toInt()
                    val my = y - lay.x(8) + (sin(a) * r * 0.55f).toInt()
                    dot(mx, my, 0xFFFFF4C8.toInt(), 0.55f + 0.45f * fill)
                }
            }

            for (a in actors.filter { it.beast.flying }) {
                val (x, y) = Stage.flight(lay, a.beast, t)
                val s = Beasts.frame(a.beast, slow, part = spec.part, flip = a.beast == Beast.EAGLE && kotlin.math.sin(t * 0.25f) < 0)
                blit(s.px, x - s.ax, y - s.ay)
            }
            for (a in actors.filter { it.beast.swarm }) {
                val alpha = fadeOf(a.beast, now, since, leaving)
                for ((i, p) in Stage.swarm(lay, a.beast, t).withIndex()) {
                    if (Visibility.hidden(i, alpha)) continue
                    val s = Beasts.frame(a.beast, slow + i, part = if (a.beast == Beast.FIREFLIES || a.beast == Beast.SPARKLE) null else spec.part)
                    blit(s.px, p.first - s.ax, p.second - s.ay)
                }
            }

            weather(lay, spec, t, ::dot)
            val front = cached("front/${spec.place}/${spec.season}/$sway/${spec.part}") { Beasts.graded(Things.grassFront(lay, spec.season, sway), spec.part) }
            blit(front, 0, artH - front.h)
        }
    }
}

private val named = HashMap<String, Px>()

/** A Px built once per key, so its bitmap is made once too. */
private fun cached(key: String, make: () -> Px): Px = named.getOrPut(key, make)

/** 0..1: how far into the picture [b] has dissolved. */
private fun fadeOf(b: Beast, now: Long, since: Map<Beast, Long>, leaving: Map<Beast, Long>): Float {
    if (Art.still) return 1f
    leaving[b]?.let { return (1f - (now - it) / DISSOLVE_MS.toFloat()).coerceIn(0f, 1f) }
    val s = since[b] ?: return 1f
    return ((now - s) / DISSOLVE_MS.toFloat()).coerceIn(0f, 1f)
}

private object Visibility {
    fun hidden(i: Int, alpha: Float) = alpha < 1f && (noise(i, 7, 9) + 0.5f) > alpha
}

private fun drawActor(
    a: Actor, spec: SceneSpec, now: Long,
    since: Map<Beast, Long>, leaving: Map<Beast, Long>,
    blit: (Px, Int, Int) -> Unit, dot: (Int, Int, Int, Float) -> Unit,
) {
    // Each animal breathes at its own pace.
    val ff = if (Art.still) 0 else Motion.frame(a.beast.ordinal * 13 + a.x, now)
    if (a.eyes) {
        if (ff == 6) return
        dot(a.x - 2, a.y, 0xFFFAE68C.toInt(), 1f); dot(a.x + 2, a.y, 0xFFFAE68C.toInt(), 1f)
        return
    }
    val s = Beasts.frame(a.beast, ff, a.pose, spec.season, a.flip, spec.part)
    val alpha = if (a.guardian) 1f else fadeOf(a.beast, now, since, leaving)
    if (alpha >= 1f) {
        blit(s.px, a.x - s.ax, a.y - s.ay)
    } else if (alpha > 0f) {
        // An ordered dissolve: pixels appear in a dither pattern, as old games faded things in.
        for (y in 0 until s.px.h) for (x in 0 until s.px.w) {
            val c = s.px.c[y * s.px.w + x]
            if ((c ushr 24) == 0) continue
            if (bayer(x, y) + 0.5f < alpha) dot(a.x - s.ax + x, a.y - s.ay + y, c, 1f)
        }
    }
    if (a.pose == app.hushwood.ui.pix.Pose.SLEEP && spec.sitting) {
        // A small z drifting up.
        val k = ((now / 400) % 6).toInt()
        val zx = a.x + s.px.w / 3 + k / 2
        val zy = a.y - s.px.h - 2 - k
        val c = 0xFFE8F0FF.toInt()
        for (i in 0..2) dot(zx + i, zy, c, 0.8f - k * 0.1f)
        dot(zx + 1, zy + 1, c, 0.8f - k * 0.1f)
        for (i in 0..2) dot(zx + i, zy + 2, c, 0.8f - k * 0.1f)
    }
}

/** Sparkles on whatever water this place has. */
private fun glintsOf(lay: Layout): List<Pair<Int, Int>> {
    val out = ArrayList<Pair<Int, Int>>()
    for (i in 0 until 60) {
        val u = noise(i, 1, 801) + 0.5f
        val v = noise(i, 2, 802) + 0.5f
        when (lay.place) {
            Place.LAKE -> {
                val x = (u * lay.w).toInt()
                val top = lay.horizon + (16 * lay.k).toInt()
                val y = top + (v * (lay.shore(x) - top - 2)).toInt()
                if (x < lay.x(128) || y < lay.y(52)) out += x to y
            }
            Place.SEA -> {
                val x = (u * lay.w).toInt()
                val y = lay.horizon + 2 + (v * (lay.tide(x) - lay.horizon - 4)).toInt()
                out += x to y
            }
            else -> {
                val y = lay.meadow + (v * (lay.h - lay.meadow)).toInt()
                val (cx, hw) = lay.stream(y)
                out += (cx + (u - 0.5f) * hw * 1.4f).toInt() to y
            }
        }
    }
    return out
}

/** Snow, falling leaves, pollen in the light: the season in the air. */
private fun weather(lay: Layout, spec: SceneSpec, t: Float, dot: (Int, Int, Int, Float) -> Unit) {
    when (spec.season) {
        Season.WINTER -> for (i in 0 until 50) {
            val sx = (noise(i, 1, 901) + 0.5f) * lay.w
            val speed = 6f + (noise(i, 2, 902) + 0.5f) * 8f
            val y = ((t * speed + (noise(i, 3, 903) + 0.5f) * lay.h) % lay.h).toInt()
            val x = (sx + sin(t * 0.8f + i) * 4f).toInt()
            dot(x, y, 0xFFF4F8FF.toInt(), if (i % 3 == 0) 1f else 0.7f)
        }
        Season.AUTUMN -> for (i in 0 until 9) {
            val sx = (noise(i, 1, 911) + 0.5f) * lay.w
            val y = ((t * 7f + (noise(i, 3, 913) + 0.5f) * lay.h) % lay.h).toInt()
            val x = (sx + sin(t * 1.2f + i) * 10f).toInt()
            val c = if (i % 2 == 0) 0xFFE0A040.toInt() else 0xFFC8642C.toInt()
            dot(x, y, c, 1f); if ((t * 3 + i).toInt() % 2 == 0) dot(x + 1, y, c, 1f) else dot(x, y + 1, c, 1f)
        }
        else -> if (spec.part == Daypart.DAY || spec.part == Daypart.DAWN) for (i in 0 until 12) {
            val x = ((noise(i, 1, 921) + 0.5f) * lay.w + sin(t * 0.4f + i) * 12f).toInt()
            val y = (lay.horizon + (noise(i, 2, 922) + 0.5f) * (lay.h - lay.horizon) + sin(t * 0.6f + i * 2) * 6f).toInt()
            dot(x, y, 0xFFFFF6C0.toInt(), 0.5f + 0.5f * sin(t * 2 + i))
        }
    }
}

package app.hushwood.ui.pix

import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.domain.Spot
import app.hushwood.domain.Trust

/** Someone in the picture: where their feet are (pixels), which way they face, awake or not. [eyes]: only a glint in the bushes. */
data class Actor(val beast: Beast, val x: Int, val y: Int, val flip: Boolean = false, val pose: Pose = Pose.IDLE, val eyes: Boolean = false, val guardian: Boolean = false)

/** Who stands where. Every animal has its favourite places; the guardian comes closer the more it trusts you. */
object Stage {
    /** A place to stand, in design units: across, and up from the bottom edge. */
    class Slot(val x: Float, val up: Float, val flip: Boolean = false)

    private fun s(x: Number, up: Number, flip: Boolean = false) = Slot(x.toFloat(), up.toFloat(), flip)

    private fun slots(b: Beast): List<Slot> = when (b) {
        Beast.TIT -> listOf(s(56, 48), s(73, 48, true), s(30, 86), s(120, 62, true))
        Beast.ROBIN -> listOf(s(86, 40, true), s(100, 64))
        Beast.WREN -> listOf(s(26, 106), s(48, 48))
        Beast.SQUIRREL -> listOf(s(88, 46, true), s(46, 64))
        Beast.HEDGEHOG -> listOf(s(42, 30), s(96, 56, true))
        Beast.HARE -> listOf(s(70, 100), s(54, 114))
        Beast.OWL -> listOf(s(27, 155))
        Beast.FOX -> listOf(s(24, 58), s(112, 102, true))
        Beast.DEER -> listOf(s(130, 100, true), s(62, 118))
        Beast.BEAR -> listOf(s(148, 112, true), s(40, 120))
        Beast.WHITE_DEER -> listOf(s(102, 126, true))
        Beast.FIREFLIES -> listOf(s(90, 60))

        Beast.DUCK -> listOf(s(70, 98), s(110, 104, true))
        Beast.SWAN -> listOf(s(40, 112), s(96, 118, true))
        Beast.FROG -> listOf(s(36, 64), s(110, 70, true))
        Beast.HERON -> listOf(s(16, 70), s(60, 82))
        Beast.BEAVER -> listOf(s(98, 62, true), s(70, 58))
        Beast.OTTER -> listOf(s(82, 66), s(150, 76, true))
        Beast.CRANE -> listOf(s(30, 40), s(120, 34, true))
        Beast.MOOSE -> listOf(s(146, 116, true))
        Beast.KINGFISHER -> listOf(s(132, 51))
        Beast.DRAGONFLY, Beast.CRANES -> listOf(s(90, 80))

        Beast.CHOUGH -> listOf(s(34, 93), s(62, 51, true), s(160, 96))
        Beast.PIKA -> listOf(s(48, 82), s(76, 40, true))
        Beast.MARMOT -> listOf(s(102, 62), s(132, 90, true))
        Beast.IBEX -> listOf(s(118, 112, true), s(24, 112))
        Beast.LYNX -> listOf(s(14, 60), s(124, 96, true))
        Beast.SNOW_LEOPARD -> listOf(s(34, 92))
        Beast.EAGLE, Beast.BUTTERFLY -> listOf(s(90, 140))

        Beast.CRAB -> listOf(s(122, 24), s(150, 20, true))
        Beast.SANDPIPER -> listOf(s(100, 42), s(140, 46, true))
        Beast.GULL -> listOf(s(150, 68), s(84, 40, true))
        Beast.PUFFIN -> listOf(s(20, 132), s(34, 119))
        Beast.SEAL -> listOf(s(168, 62, true), s(146, 68, true))
        Beast.WHALE -> listOf(s(112, 102, true))
        Beast.SPARKLE -> listOf(s(120, 50))
    }

    /** Where any guardian stands in a place that isn't its own: at the edge, then nearer. */
    private fun visiting(place: Place): List<Slot> = when (place) {
        Place.FOREST -> listOf(s(128, 104, true), s(90, 60, true))
        Place.LAKE -> listOf(s(12, 40), s(32, 26))
        Place.MOUNTAINS -> listOf(s(140, 100, true), s(82, 62, true))
        Place.SEA -> listOf(s(98, 50, true), s(60, 38))
    }

    /** The bush (or reeds, or rock, or dune grass) whose shadow the watching guardian's eyes shine out of. */
    private fun hide(place: Place) = when (place) {
        Place.FOREST -> s(20, 98)
        Place.LAKE -> s(10, 46)
        Place.MOUNTAINS -> s(26, 86)
        Place.SEA -> s(22, 26)
    }

    fun cast(lay: Layout, present: List<Beast>, guardian: Beast?, trust: Trust?, spot: Spot?, sitting: Boolean): List<Actor> {
        val out = ArrayList<Actor>()
        fun at(sl: Slot, b: Beast, pose: Pose = Pose.IDLE, g: Boolean = false, flip: Boolean = sl.flip) = Actor(b, lay.x(sl.x), lay.y(sl.up), flip, pose, guardian = g)
        if (guardian != null && trust != null) {
            val own = guardian.place == lay.place
            val list = if (own) slots(guardian) else visiting(lay.place)
            out += when (trust) {
                Trust.WATCHING -> hide(lay.place).let { Actor(guardian, lay.x(it.x), lay.y(it.up), eyes = true, guardian = true) }
                Trust.EDGE -> at(list.last(), guardian, g = true)
                Trust.NEAR -> at(list.first(), guardian, g = true)
                Trust.BESIDE -> {
                    val (sx, sy) = lay.spot(spot ?: Spot.STUMP)
                    val left = sx > lay.x(40)
                    val bx = sx + lay.x(if (left) -24f else 24f)
                    Actor(guardian, bx, sy + lay.x(4f), flip = !left, pose = if (sitting) Pose.SLEEP else Pose.IDLE, guardian = true)
                }
            }
        }
        for (b in present) {
            if (b.swarm || b.flying) { out += Actor(b, 0, 0); continue }
            val list = slots(b)
            val free = list.firstOrNull { sl -> out.none { a -> !a.eyes && kotlin.math.abs(a.x - lay.x(sl.x)) < lay.x(12) && kotlin.math.abs(a.y - lay.y(sl.up)) < lay.x(8) } } ?: list.last()
            out += at(free, b)
        }
        return out
    }

    /** Where the swarm of [b] shows, as (x, y) points in pixels, moving a little with [t] seconds. */
    fun swarm(lay: Layout, b: Beast, t: Float): List<Pair<Int, Int>> {
        val n = when (b) { Beast.FIREFLIES -> 16; Beast.SPARKLE -> 22; else -> 5 }
        return (0 until n).map { i ->
            val bx = (noise(i, 1, 301) + 0.5f)
            val by = (noise(i, 2, 302) + 0.5f)
            val drift = kotlin.math.sin(t * (0.3f + by) + i) * 6f
            when (b) {
                Beast.SPARKLE -> { val x = (bx * lay.w).toInt(); x + (drift / 3).toInt() to lay.tide(x) - lay.x(2f + by * 6f) }
                Beast.DRAGONFLY -> lay.x(40f + bx * 100f + drift * 2) to lay.y(70f + by * 30f + kotlin.math.sin(t * 2 + i) * 4)
                Beast.BUTTERFLY -> lay.x(20f + bx * 140f + drift) to lay.y(30f + by * 50f + kotlin.math.sin(t * 1.6f + i) * 5)
                else -> lay.x(bx * 180f + drift) to lay.y(20f + by * 100f + kotlin.math.cos(t * 0.7f + i) * 4)
            }
        }
    }

    /** Where a flyer is at [t] seconds: the skein crosses the sky, the eagle circles. */
    fun flight(lay: Layout, b: Beast, t: Float): Pair<Int, Int> = when (b) {
        Beast.EAGLE -> lay.x(90f + kotlin.math.cos(t * 0.25f) * 50f) to lay.horizon - lay.x(60f + kotlin.math.sin(t * 0.25f) * 12f)
        else -> (((t * 6f) % (lay.w + 80)) - 40).toInt() to lay.horizon - lay.x(64f)
    }

    /**
     * One still picture with everyone in it: for the widget, the share card and the screenshots. The live view
     * draws the same pieces itself, frame by frame.
     */
    fun still(lay: Layout, bg: Px, actors: List<Actor>, season: Season, part: Daypart, spot: Spot?, fire: Boolean, f: Int = 0, t: Float = 12f): Px {
        val out = Px(bg.w, bg.h)
        out.blit(bg, 0, 0)
        val (fx, fy) = lay.fire
        (if (fire) Things.fire(f) else Things.embers(f)).let { out.blit(Beasts.graded(it.px, part), fx - it.ax, fy - it.ay) }
        val ground = actors.filter { !it.beast.swarm && !it.beast.flying }.sortedBy { it.y }
        val sitter = spot?.let { lay.spot(it) }
        var drawn = false
        for (a in ground) {
            if (sitter != null && !drawn && a.y > sitter.second) { drawSitter(out, lay, spot, part, f); drawn = true }
            if (a.eyes) { eyes(out, a.x, a.y, f); continue }
            val s = Beasts.frame(a.beast, f, a.pose, season, a.flip, part)
            out.blit(s.px, a.x - s.ax, a.y - s.ay)
        }
        if (sitter != null && !drawn) drawSitter(out, lay, spot, part, f)
        for (a in actors.filter { it.beast.flying }) {
            val (x, y) = flight(lay, a.beast, t)
            Beasts.frame(a.beast, f, part = part).let { out.blit(it.px, x - it.ax, y - it.ay) }
        }
        for (a in actors.filter { it.beast.swarm }) for ((i, p) in swarm(lay, a.beast, t).withIndex()) {
            val s = Beasts.frame(a.beast, f + i, part = if (a.beast == Beast.FIREFLIES || a.beast == Beast.SPARKLE) null else part)
            out.blit(s.px, p.first - s.ax, p.second - s.ay)
        }
        out.blit(Beasts.graded(Things.grassFront(lay, season, f), part), 0, lay.h - Things.grassFront(lay, season, f).h)
        return out
    }

    private fun drawSitter(out: Px, lay: Layout, spot: Spot, part: Daypart, f: Int) {
        val (x, y) = lay.spot(spot)
        val s = Things.sitter(spot, 0f, f)
        out.blit(Beasts.graded(s.px, part), x - s.ax, y - s.ay)
    }

    /** Two glints in the dark of the leaves. */
    fun eyes(out: Px, x: Int, y: Int, f: Int) {
        if (f == 6) return
        val c = rgb(250, 230, 140)
        out.set(x - 2, y, c); out.set(x - 1, y, c, 140); out.set(x + 2, y, c); out.set(x + 3, y, c, 140)
    }
}

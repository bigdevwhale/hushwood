package app.hushwood.ui.pix

import app.hushwood.domain.Gift
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.domain.Spot

/** Everything small: you on the stump, the fire, the grass in front, what's left on the stump and the leaves of the herbarium. */
object Things {
    private val sweater = Ramp(rgb(52, 70, 60), rgb(76, 104, 84), rgb(104, 138, 104), rgb(138, 170, 124), rgb(176, 200, 150))
    private val denim = Ramp(rgb(30, 40, 62), rgb(46, 62, 92), rgb(66, 88, 122), rgb(96, 120, 150))
    private val hat = Ramp(rgb(110, 40, 34), rgb(170, 70, 46), rgb(214, 108, 64), rgb(240, 156, 104))
    private val hair = Ramp(rgb(46, 30, 26), rgb(84, 56, 40), rgb(124, 86, 58), rgb(160, 120, 84))
    private val skin = Ramp(rgb(150, 100, 80), rgb(200, 150, 120), rgb(230, 190, 160))
    private val boots = Ramp(rgb(40, 28, 26), rgb(72, 50, 40), rgb(104, 76, 58))

    /** A knit hat with a pompom, hair falling from under it, the nape of the neck: someone seen from behind. */
    private fun head(kit: Kit, cx: Float, cy: Float) {
        val px = kit.px
        px.shade(kit.e(cx, cy + 0.6f, 2.9f, 3f), kit.d(cx, cy + 0.4f, 2.9f, 3f), hair, texture = { x, y -> if ((x + y * 2) % 3 == 0) -0.2f else 0f })
        px.shade(kit.r(cx - 0.8f, cy + 3f, 1.6f, 1.2f), facing(), skin, outline = false, lo = 1)
        val cap = kit.p(cx - 3.1f, cy + 0.2f, cx - 2.6f, cy - 2.2f, cx, cy - 3.3f, cx + 2.6f, cy - 2.2f, cx + 3.1f, cy + 0.2f)
        px.shade(cap, kit.d(cx, cy - 1.2f, 3f, 2.4f), hat, texture = { x, _ -> if (x % 2 == 0) -0.15f else 0f })
        px.shade(kit.r(cx - 3.1f, cy - 0.4f, 6.2f, 1.1f), facing(0f, -0.3f, 1f), hat, lo = 0, hi = 2)
        px.shade(kit.e(cx, cy - 3.9f, 1.2f), kit.d(cx, cy - 4.1f, 1.2f), Ramp(rgb(200, 190, 170), rgb(236, 230, 214), rgb(252, 250, 242)), lo = 0)
    }

    /**
     * You, seen from behind: a knit hat, a green sweater, crossed legs on the seat. Sitting for most practices,
     * standing for a stretch (arms raised in a tree pose) or a walk. [breath] 0..1 lifts the shoulders a pixel.
     */
    fun sitter(spot: Spot, breath: Float, f: Int): Sprite {
        val standing = spot == Spot.STONE || spot == Spot.PATH
        val kit = Kit(14f, if (standing) 26f else 19f)
        val px = kit.px
        val up = if (breath > 0.5f) 0.5f else 0f
        val knit: (Int, Int) -> Float = { x, y -> if ((x + y) % 3 == 0) -0.14f else 0f }
        if (standing) {
            val step = spot == Spot.PATH && f % 4 >= 2
            kit.limb(5.8f, 16f, 5.4f + (if (step) -0.7f else 0f), 24.6f, 2f, denim, lo = 0, hi = 3)
            kit.limb(8.2f, 16f, 8.6f + (if (step) 0.7f else 0f), 24.6f, 2f, denim, lo = 0, hi = 3)
            px.shade(kit.e(5.4f + (if (step) -0.7f else 0f), 25.2f, 1.4f, 0.8f), facing(), boots, lo = 0)
            px.shade(kit.e(8.6f + (if (step) 0.7f else 0f), 25.2f, 1.4f, 0.8f), facing(), boots, lo = 0)
            if (spot == Spot.STONE) {
                kit.limb(4.4f, 10f, 6.2f, 2.6f, 1.5f, sweater, lo = 1)
                kit.limb(9.6f, 10f, 7.8f, 2.6f, 1.5f, sweater, lo = 1)
                kit.dot(6.6f, 1.8f, skin[2]); kit.dot(7.2f, 1.8f, skin[2])
            } else {
                kit.limb(3.6f, 10f, 3.2f, 16f, 1.5f, sweater, lo = 0, hi = 3)
                kit.limb(10.4f, 10f, 10.8f, 16f, 1.5f, sweater, lo = 0, hi = 3)
            }
            px.shade(kit.p(3.6f, 9f - up, 10.4f, 9f - up, 10f, 17f, 4f, 17f), kit.d(7f, 12f, 4f, 5f), sweater, texture = knit, rim = sweater[4])
            head(kit, 7f, 6.4f - up)
            return kit.sprite(7f, 25.6f)
        }
        // Crossed legs on the seat, knees out to the sides.
        px.shade(kit.e(7f, 16.4f, 6.6f, 2.2f), kit.d(7f, 16f, 6.6f, 2.4f), denim, texture = { x, _ -> if (x % 4 == 0) -0.12f else 0f })
        px.shade(kit.e(1.4f, 16.8f, 1.2f, 0.9f), facing(), boots, lo = 0); px.shade(kit.e(12.6f, 16.8f, 1.2f, 0.9f), facing(), boots, lo = 0)
        // Arms resting on the knees.
        kit.limb(3.4f, 9.6f - up, 2.4f, 15f, 1.6f, sweater, lo = 0, hi = 3)
        kit.limb(10.6f, 9.6f - up, 11.6f, 15f, 1.6f, sweater, lo = 0, hi = 3)
        // The back in a knit sweater, rounded at the shoulders.
        px.shade(kit.p(3.4f, 9.4f - up, 4.6f, 8f - up, 9.4f, 8f - up, 10.6f, 9.4f - up, 10.4f, 15.4f, 3.6f, 15.4f), kit.d(7f, 11.4f, 4f, 4.4f), sweater, texture = knit, rim = sweater[4])
        kit.line(7f, 9.4f - up, 7f, 14.6f, sweater[1])
        head(kit, 7f, 5f - up)
        return kit.sprite(7f, 18.4f)
    }

    private val flame = Ramp(rgb(120, 30, 30), rgb(200, 70, 40), rgb(244, 140, 50), rgb(255, 204, 100), rgb(255, 246, 200))

    /** The fire's flames, a loop of four, with a spark flying up. */
    fun fire(f: Int): Sprite {
        val kit = Kit(12f, 16f)
        val px = kit.px
        val k = f % 4
        val tips = listOf(listOf(3f to 6f, 6f to 2.4f, 8.6f to 6.6f), listOf(3f to 6.6f, 6.2f to 3f, 9f to 4.6f), listOf(4f to 4.6f, 6f to 1.6f, 8.4f to 5.6f), listOf(2.6f to 6.6f, 5.4f to 2.6f, 8.4f to 3.6f))[k]
        px.glow(kit.x(6f).toFloat(), kit.y(13f).toFloat(), 12f * kit.k, rgb(255, 160, 80), 70, 4)
        for ((tx, ty) in tips) px.shade(kit.p(tx - 2.6f, 15.4f, tx, ty, tx + 2.6f, 15.4f), facing(), flame, outline = false, lo = 1, hi = 3)
        px.shade(kit.e(6f, 13f, 2.8f, 3f), facing(), flame, outline = false, lo = 3, hi = 4)
        kit.dot(6f + (k - 1.5f), 0.6f + k * 0.5f, flame[4])
        return kit.sprite(6f, 15.6f)
    }

    /** Embers when nobody is at the fire. */
    fun embers(f: Int): Sprite {
        val kit = Kit(10f, 4f)
        for (i in 0 until 7) {
            val on = (i + f) % 3 != 0
            kit.dot(2f + i, 2f + (i % 2) * 0.7f, if (on) rgb(240, 120, 50) else rgb(140, 50, 40))
        }
        return kit.sprite(5f, 3f)
    }

    /** Tufts of tall grass (or reeds, or marram) along the bottom edge, swaying: drawn over everything else. */
    fun grassFront(lay: Layout, s: Season, f: Int): Px {
        val k = lay.k
        val w = lay.w
        val px = Px(w, (24 * k).toInt())
        val ramp = Clearing.grass(s)
        val tall = when {
            s == Season.WINTER -> Ramp(rgb(120, 100, 76), rgb(160, 136, 100), rgb(196, 176, 136))
            lay.place == Place.SEA -> Ramp(rgb(90, 110, 70), rgb(130, 150, 90), rgb(176, 186, 120))
            else -> Ramp(*IntArray(ramp.size) { mixc(ramp[it], rgb(10, 30, 20), 0.25f) })
        }
        val sway = if (f % 4 < 2) 0 else 1
        for (i in 0 until (w / 2.5f).toInt()) {
            val x = (i * 2.5f).toInt() + ((noise(i, 1, 201) + 0.5f) * 3).toInt()
            // Mostly at the two corners, thin in the middle so nobody is hidden.
            val edge = kotlin.math.min(x, w - x) / (w / 2f)
            if (noise(i, 2, 202) < edge * 0.9f - 0.3f) continue
            val h = ((6 + (noise(i, 3, 203) + 0.5f) * 14 * (1 - edge * 0.6f)) * k).toInt()
            for (j in 0 until h) {
                val lean = if (j > h / 2) sway * (if (i % 2 == 0) 1 else -1) else 0
                px.set(x + lean, px.h - 1 - j, tall[if (j > h * 0.7f) tall.size - 1 else if (j > h / 3) 1 else 0])
                if (j < h / 2) px.set(x + 1, px.h - 1 - j, tall[0], 140)
            }
            if (s == Season.SUMMER && lay.place != Place.SEA && i % 7 == 0) { px.set(x + sway, px.h - h - 1, rgb(240, 240, 230)); px.set(x + sway + 1, px.h - h - 1, rgb(250, 220, 90)) }
        }
        return px
    }

    /* ---------- Small icons, by hand ---------- */

    private val OFFER = mapOf(
        Offering.SEEDS to arrayOf("........", "..s.s...", ".s.s.s..", "..sSs.s.", ".s.s.S..", "..s.s...", "........"),
        Offering.BERRIES to arrayOf("...g....", "..gg....", ".rR.rR..", ".rr.rr..", "..rRr...", "..rrr...", "........"),
        Offering.NUTS to arrayOf("..cc....", ".cCCc...", ".nNNn...", ".nnNn...", "..nn....", "........", "........"),
        Offering.APPLE to arrayOf("...g....", "...bg...", ".rrRrr..", "rrrrRrr.", "rrrrrrr.", ".rrrrr..", "..r.r..."),
        Offering.HONEY to arrayOf(".kkkk...", "..ww....", ".yYYy...", "yyYYyy..", "yyyyyy..", ".yyyy...", "........"),
    )
    private val OFFER_PAL = mapOf(
        's' to rgb(214, 190, 130), 'S' to rgb(150, 120, 70), 'g' to rgb(90, 140, 70), 'r' to rgb(196, 50, 60), 'R' to rgb(250, 140, 140),
        'c' to rgb(120, 86, 50), 'C' to rgb(160, 120, 70), 'n' to rgb(170, 120, 60), 'N' to rgb(214, 170, 100), 'b' to rgb(100, 70, 40),
        'k' to rgb(160, 120, 90), 'w' to rgb(230, 220, 200), 'y' to rgb(220, 150, 40), 'Y' to rgb(250, 210, 110),
    )

    fun offering(o: Offering): Px = Px(8, 7).also { Beasts.stamp(it, OFFER.getValue(o), OFFER_PAL) }

    private val GIFT = mapOf(
        Gift.FEATHER to arrayOf("......w.", ".....wW.", "....wWg.", "...wWg..", "..wWg...", ".wWg....", ".g......", "g......."),
        Gift.ACORN to arrayOf("...b....", "..cCc...", ".cCCCc..", ".nNNNn..", ".nNnNn..", "..nNn...", "...n....", "........"),
        Gift.PEBBLE to arrayOf("........", "........", "..sSSs..", ".sSWSss.", ".sssssS.", "..ssss..", "........", "........"),
        Gift.PINECONE to arrayOf("...b....", "..bBb...", ".bBbBb..", ".BbBbB..", ".bBbBb..", "..bBb...", "...b....", "........"),
        Gift.FERN to arrayOf("....g...", "...gGg..", "..gGgGg.", "...gGg..", "..gGgGg.", "...gGg..", "....g...", "....g..."),
        Gift.ROWAN to arrayOf("...g....", "..g.g...", ".rR.rR..", ".rrRrr..", "..rRr...", "...rr...", "........", "........"),
        Gift.SHELL to arrayOf("........", "...wW...", "..wWwW..", ".wWwWwW.", ".WwWwWw.", "..wwww..", "........", "........"),
        Gift.MOSS to arrayOf("........", "........", "..gGg...", ".gGgGgG.", "gGgGgGgG", ".mmmmmm.", "........", "........"),
    )
    private val GIFT_PAL = mapOf(
        'w' to rgb(230, 226, 214), 'W' to rgb(160, 150, 140), 'g' to rgb(84, 130, 64), 'G' to rgb(140, 180, 90),
        'b' to rgb(100, 66, 40), 'B' to rgb(150, 104, 60), 'c' to rgb(110, 80, 50), 'C' to rgb(150, 116, 70), 'n' to rgb(170, 120, 60), 'N' to rgb(210, 166, 100),
        's' to rgb(120, 124, 130), 'S' to rgb(170, 172, 176), 'r' to rgb(214, 70, 40), 'R' to rgb(255, 160, 110), 'm' to rgb(90, 70, 50),
    )

    fun gift(g: Gift): Px = Px(8, 8).also { Beasts.stamp(it, GIFT.getValue(g), GIFT_PAL) }

    /** A pressed leaf for the herbarium, its kind and colour from the day it was picked. */
    fun leaf(kind: Int, s: Season): Px {
        val ramp = Clearing.leaves(if (s == Season.WINTER) Season.AUTUMN else s)
        val px = Px(14, 14)
        val shape = when (kind % 3) {
            // Birch: a toothed heart.
            0 -> poly(7f, 1f, 11.5f, 5f, 12f, 8.5f, 7f, 11.5f, 2f, 8.5f, 2.5f, 5f)
            // Oak: lobed.
            1 -> union(ellipse(7f, 6.5f, 2.8f, 5.2f), ellipse(4.5f, 4.5f, 2f, 1.6f), ellipse(9.5f, 4.5f, 2f, 1.6f), ellipse(4.2f, 8f, 2.2f, 1.6f), ellipse(9.8f, 8f, 2.2f, 1.6f))
            // Aspen: round, with a point.
            else -> union(ellipse(7f, 6.5f, 5f, 4.6f), poly(5.5f, 3f, 7f, 0.5f, 8.5f, 3f))
        }
        px.shade(shape, dome(7f, 6f, 6f, 6f, 0.6f), ramp, lo = 1)
        px.line(7, 3, 7, 13, ramp[0])
        return px
    }
}

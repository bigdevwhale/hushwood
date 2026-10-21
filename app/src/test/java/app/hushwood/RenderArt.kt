package app.hushwood

import android.graphics.Bitmap
import app.hushwood.domain.Daypart
import app.hushwood.domain.Season
import app.hushwood.domain.Beast
import app.hushwood.ui.pix.Beasts
import app.hushwood.ui.pix.Clearing
import app.hushwood.domain.Spot
import app.hushwood.domain.Place
import app.hushwood.domain.Trust
import app.hushwood.domain.Keeper
import app.hushwood.domain.Stroke
import app.hushwood.ui.pix.Layout
import app.hushwood.ui.pix.Portraits
import app.hushwood.ui.pix.Pose
import app.hushwood.ui.pix.Stage
import app.hushwood.ui.pix.Things
import app.hushwood.ui.pix.Px
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Paints the clearing and its animals to PNG sheets (run with `-Prender`), so they can be looked at without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    /** Lays [cells] out in a grid, each pixel blown up [scale] times. */
    private fun sheet(name: String, cells: List<List<Px?>>, scale: Int, bg: Int = 0xFF203034.toInt(), gap: Int = 4) {
        val cw = cells.flatten().filterNotNull().maxOf { it.w }
        val ch = cells.flatten().filterNotNull().maxOf { it.h }
        val cols = cells.maxOf { it.size }
        val bmp = Bitmap.createBitmap(cols * (cw * scale + gap), cells.size * (ch * scale + gap), Bitmap.Config.ARGB_8888)
        bmp.eraseColor(bg)
        cells.forEachIndexed { r, row ->
            row.forEachIndexed { c, px ->
                if (px == null) return@forEachIndexed
                val ox = c * (cw * scale + gap)
                val oy = r * (ch * scale + gap) + (ch - px.h) * scale
                for (y in 0 until px.h) for (x in 0 until px.w) {
                    val p = px.c[y * px.w + x]
                    val a = (p ushr 24) and 0xFF
                    if (a == 0) continue
                    val col = blend(bg, p, a / 255f)
                    for (j in 0 until scale) for (i in 0 until scale) bmp.setPixel(ox + x * scale + i, oy + y * scale + j, col)
                }
            }
        }
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) + ((((b shr s) and 0xFF) - ((a shr s) and 0xFF)) * t)).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    @Test
    fun clearings() {
        for (place in Place.entries) sheet("place-${place.name.lowercase()}", Season.entries.map { s -> Daypart.entries.map { Clearing.paint(270, 450, it, s, place) } }, 2)
    }

    @Test
    fun beasts() {
        sheet("beasts", Beast.entries.chunked(10).map { row -> row.map { Beasts.frame(it, 0).px } } + Beast.entries.chunked(10).map { row -> row.map { Beasts.frame(it, 3, Pose.SLEEP).px } }, 6, bg = 0xFF7FA070.toInt())
    }

    @Test
    fun scenes() {
        fun scene(place: Place, p: Daypart, s: Season, present: List<Beast>, g: Beast?, t: Trust?, spot: Spot?): Px {
            val lay = Layout(270, 450, place)
            val bg = Clearing.paint(270, 450, p, s, place)
            return Stage.still(lay, bg, Stage.cast(lay, present, g, t, spot, spot != null), s, p, spot, spot == Spot.FIRE)
        }
        sheet("scenes", listOf(listOf(
            scene(Place.FOREST, Daypart.DAWN, Season.SUMMER, listOf(Beast.TIT, Beast.ROBIN, Beast.SQUIRREL, Beast.HARE, Beast.DEER, Beast.WHITE_DEER), Beast.HERON, Trust.NEAR, Spot.STUMP),
            scene(Place.LAKE, Daypart.DAY, Season.SUMMER, listOf(Beast.DUCK, Beast.SWAN, Beast.FROG, Beast.HERON, Beast.BEAVER, Beast.MOOSE, Beast.KINGFISHER, Beast.DRAGONFLY), Beast.FOX, Trust.EDGE, Spot.STREAM),
            scene(Place.MOUNTAINS, Daypart.DUSK, Season.SUMMER, listOf(Beast.CHOUGH, Beast.PIKA, Beast.MARMOT, Beast.IBEX, Beast.LYNX, Beast.EAGLE, Beast.BUTTERFLY), Beast.DEER, Trust.BESIDE, Spot.FIRE),
            scene(Place.SEA, Daypart.DAY, Season.SUMMER, listOf(Beast.CRAB, Beast.SANDPIPER, Beast.GULL, Beast.PUFFIN, Beast.SEAL, Beast.WHALE), Beast.WREN, Trust.WATCHING, Spot.STUMP),
        ), listOf(
            scene(Place.FOREST, Daypart.NIGHT, Season.WINTER, listOf(Beast.HARE, Beast.FOX, Beast.OWL), Beast.BEAR, Trust.BESIDE, Spot.STUMP),
            scene(Place.LAKE, Daypart.DUSK, Season.AUTUMN, listOf(Beast.OTTER, Beast.CRANE, Beast.CRANES), Beast.OWL, Trust.NEAR, null),
            scene(Place.MOUNTAINS, Daypart.NIGHT, Season.WINTER, listOf(Beast.SNOW_LEOPARD, Beast.LYNX), Beast.CRANE, Trust.EDGE, Spot.STONE),
            scene(Place.SEA, Daypart.NIGHT, Season.SUMMER, listOf(Beast.SPARKLE, Beast.CRAB), Beast.HEDGEHOG, Trust.NEAR, Spot.PATH),
        )), 2)
    }

    @Test
    fun portraits() {
        val parts = Daypart.entries
        fun strokes(n: Int) = (0 until n).map { Stroke(30, parts[(it * 7 / 5) % 4]) }
        sheet("portraits", Beast.Keepers.map { b -> listOf(Portraits.paint(b, strokes(8)), Portraits.paint(b, strokes(24)), Portraits.paint(b, strokes(40)), Portraits.full(b)) }, 4)
        sheet("things", listOf(
            app.hushwood.domain.Offering.entries.map { Things.offering(it) } + app.hushwood.domain.Gift.entries.map { Things.gift(it) },
            (0 until 4).map { Things.leaf(it, Season.SUMMER) } + (0 until 4).map { Things.leaf(it, Season.AUTUMN) } + Spot.entries.map { Things.sitter(it, 0f, 0).px },
        ), 6)
    }

    /** The launcher icon: you on a stump under the moon with a sleeping fox, in a round window; written into the mipmaps and docs/logo.png. */
    @Test
    fun icon() {
        val art = app.hushwood.ui.pix.Icon.paint()
        val res = File("src/main/res")
        val bg = 0xFF1E3437.toInt()
        for ((dir, px) in listOf("mdpi" to 108, "hdpi" to 162, "xhdpi" to 216, "xxhdpi" to 324, "xxxhdpi" to 432)) {
            // The safe zone is the middle two thirds.
            val k = maxOf(1, (px * 0.62f / art.w).toInt())
            fun draw(b: Bitmap) {
                val ox = (b.width - art.w * k) / 2
                val oy = (b.height - art.h * k) / 2
                for (y in 0 until art.h) for (x in 0 until art.w) {
                    val c = art.c[y * art.w + x]
                    if ((c ushr 24) == 0) continue
                    for (j in 0 until k) for (i in 0 until k) b.setPixel(ox + x * k + i, oy + y * k + j, c)
                }
            }
            val fg = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            draw(fg)
            File(res, "mipmap-$dir").mkdirs()
            File(res, "mipmap-$dir/ic_launcher_foreground.png").outputStream().use { fg.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val legacy = Bitmap.createBitmap(px * 48 / 108, px * 48 / 108, Bitmap.Config.ARGB_8888)
            legacy.eraseColor(bg)
            val scaled = Bitmap.createScaledBitmap(fg, legacy.width * 108 / 72, legacy.height * 108 / 72, false)
            android.graphics.Canvas(legacy).drawBitmap(scaled, (legacy.width - scaled.width) / 2f, (legacy.height - scaled.height) / 2f, null)
            File(res, "mipmap-$dir/ic_launcher.png").outputStream().use { legacy.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val logo = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        logo.eraseColor(bg)
        val k = 512 * 3 / 4 / art.w
        for (y in 0 until art.h) for (x in 0 until art.w) {
            val c = art.c[y * art.w + x]
            if ((c ushr 24) == 0) continue
            for (j in 0 until k) for (i in 0 until k) logo.setPixel((512 - art.w * k) / 2 + x * k + i, (512 - art.h * k) / 2 + y * k + j, c)
        }
        File("../docs").mkdirs()
        File("../docs/logo.png").outputStream().use { logo.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

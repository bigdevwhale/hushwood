package app.hushwood.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import app.hushwood.domain.Beast
import app.hushwood.ui.pix.Beasts
import app.hushwood.ui.pix.Motion
import app.hushwood.ui.pix.Px
import app.hushwood.ui.pix.rgb
import app.hushwood.ui.scene.Art
import kotlinx.coroutines.delay

private val bitmaps = HashMap<Px, ImageBitmap>()

private fun Px.image(): ImageBitmap = bitmaps.getOrPut(this) { bitmap() }

/** A pixel picture blown up whole pixels at a time, centred in [size], resting on the bottom edge. */
@Composable
fun PixImage(px: Px, size: Dp, modifier: Modifier = Modifier, bottom: Boolean = true) {
    Canvas(modifier.size(size)) {
        val scale = maxOf(1, (minOf(this.size.width / px.w, this.size.height / px.h)).toInt())
        val w = px.w * scale
        val h = px.h * scale
        val x = ((this.size.width - w) / 2).toInt()
        val y = if (bottom) (this.size.height - h).toInt() else ((this.size.height - h) / 2).toInt()
        drawImage(px.image(), IntOffset.Zero, IntSize(px.w, px.h), IntOffset(x, y), IntSize(w, h), filterQuality = FilterQuality.None)
    }
}

/** An animal in its idle loop; or only its dark outline while it's still unknown. */
@Composable
fun BeastIcon(b: Beast, size: Dp, modifier: Modifier = Modifier, animate: Boolean = true, unknown: Boolean = false, flip: Boolean = false) {
    var f by remember { mutableIntStateOf(0) }
    if (animate && !unknown && !Art.still) LaunchedEffect(b) {
        while (true) {
            delay(100)
            f = Motion.frame(b.ordinal * 13, System.currentTimeMillis())
        }
    }
    val px = remember(b, f, unknown, flip) {
        val s = Beasts.frame(b, f, flip = flip).px
        if (unknown) silhouette(s) else s
    }
    PixImage(px, size, modifier)
}

private val shadows = HashMap<Px, Px>()

fun silhouette(p: Px): Px = shadows.getOrPut(p) {
    val o = Px(p.w, p.h)
    val c = rgb(44, 56, 56)
    for (i in p.c.indices) if ((p.c[i] ushr 24) > 0) o.c[i] = c
    o
}

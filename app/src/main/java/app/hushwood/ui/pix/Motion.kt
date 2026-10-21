package app.hushwood.ui.pix

/**
 * Calm idle life. Most of the time an animal only breathes, slowly; now and then, at moments of its own, it
 * blinks, flicks an ear or lowers its head, and goes still again. Frames 0 and 7 are the breath; the others are
 * the small actions the sprites draw (1 a hop, 2 an ear or a sniff, 3–4 a peck or a nod, 4–5 grazing, 6 a blink).
 */
object Motion {
    /** One breath, in and out. */
    private const val BREATH_MS = 3_200L

    /** Each animal gets one chance at an action in a window this long. */
    private const val WINDOW_MS = 6_500L

    fun frame(salt: Int, now: Long): Int {
        val shifted = now + salt * 977L
        val window = shifted / WINDOW_MS
        val t = shifted % WINDOW_MS
        val r = noise(window.toInt(), salt, 71) + 0.5f
        val at = (noise(window.toInt(), salt, 72) + 0.5f) * (WINDOW_MS - 1_600)
        val since = t - at.toLong()
        if (since in 0 until 1_400) {
            when {
                // A blink: the commonest thing.
                r < 0.45f -> if (since < 160) return 6
                // A peck, a nod, a sniff at the ground.
                r < 0.7f -> return if (since < 600) 3 else if (since < 1_100) 4 else 0
                // An ear turned, a whisker twitched.
                r < 0.85f -> if (since < 500) return 2
                // Grazing, a little longer.
                r < 0.93f -> return if (since < 700) 4 else 5
                // A small hop.
                else -> if (since < 260) return 1
            }
        }
        return if ((shifted % BREATH_MS) < BREATH_MS / 2) 0 else 7
    }

    /** Grass and reeds sway on a slower wind. */
    fun sway(now: Long): Int = if ((now / 1_300) % 2 == 0L) 0 else 2
}

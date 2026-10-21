package app.hushwood.domain

import java.time.LocalDate

/** Everything about this sitting that decides who may come: the place, the light, the season and what's on the stump. */
data class Setting(
    val place: Place,
    val spot: Spot,
    val part: Daypart,
    val date: LocalDate,
    val offering: Offering? = null,
    /** Breathing by the stream 7 days of the last 10. */
    val kingfisher: Boolean = false,
    /** The guardian sits close: the others are bolder. */
    val bold: Boolean = false,
    /** Mixed into each animal's shyness so no two sittings go the same. */
    val seed: Long = 0,
) {
    val season: Season get() = Season.of(date)
}

/** An animal that may come out, once [at] minutes of unbroken quiet have passed. */
data class Arrival(val beast: Beast, val at: Float)

/**
 * Who comes out, and when. Quiet builds up minute by minute; a movement sends them back to the trees and
 * takes most of it away, but not all, so the small birds are back soon after.
 */
object Visits {
    /** No more than this many at once, besides the guardian: a clearing, not a zoo. */
    const val MAX = 6

    /** What's left of the quiet after a startle. */
    const val AFTER_STARTLE = 0.4f

    /** Practices in motion (a walk, a stretch) gather quiet at this rate instead. */
    const val MOVING_RATE = 0.5f

    fun arrivals(s: Setting, guardian: Beast? = null): List<Arrival> = Beast.entries
        .filter { it != guardian && it.place == s.place && it.hours.contains(s.part) && it.seasons.contains(s.season) }
        .filter { b ->
            when (b) {
                Beast.KINGFISHER -> s.kingfisher && s.spot == Spot.STREAM
                Beast.CRANES -> Season.equinox(s.date)
                else -> true
            }
        }
        .map { b ->
            var at = b.calm
            if (s.spot in b.near) at *= 0.6f
            if (s.offering != null && s.offering in b.likes) at *= 0.6f
            if (s.bold && !b.rare) at *= 0.85f
            // Each sitting a little different: 0.8x .. 1.25x, the rare ones keep their word.
            if (!b.rare) at *= 0.8f + 0.45f * unit(s.seed, b.ordinal)
            Arrival(b, at)
        }
        .sortedBy { it.at }

    /** Out in the clearing after [calm] minutes of quiet. */
    fun present(arrivals: List<Arrival>, calm: Float): List<Beast> = arrivals.filter { it.at <= calm }.take(MAX).map { it.beast }

    /** Who came to a practice written down afterwards: as if half the time had been quiet, and only a few. */
    fun afterwards(s: Setting, minutes: Int, guardian: Beast? = null): List<Beast> =
        present(arrivals(s, guardian), minutes * MOVING_RATE).take(3)

    /** 0..1, stable for a seed and a salt. */
    fun unit(seed: Long, salt: Int): Float {
        var h = seed xor (salt.toLong() * -0x61c8864680b583ebL)
        h = (h xor (h ushr 33)) * -0xae502812aa7333L
        h = h xor (h ushr 33)
        return ((h ushr 11) and 0xFFFFFF).toFloat() / 0xFFFFFF
    }
}

/**
 * The quiet of one sitting as it goes. Fed with the clock and with startles from the phone's movement
 * sensor; remembers who has been out at all, which is what the journal keeps.
 */
class Quiet(private val arrivals: List<Arrival>, private val moving: Boolean) {
    /** Minutes of quiet now. */
    var calm = 0f
        private set

    /** The longest stretch, minutes. */
    var longest = 0f
        private set
    var startles = 0
        private set
    private var unbroken = 0f
    val seen = LinkedHashSet<Beast>()
    var here: List<Beast> = emptyList()
        private set

    fun tick(minutes: Float) {
        calm += minutes * if (moving) Visits.MOVING_RATE else 1f
        unbroken += minutes
        longest = maxOf(longest, unbroken)
        here = Visits.present(arrivals, calm)
        seen += here
    }

    fun startle() {
        if (moving) return
        calm *= Visits.AFTER_STARTLE
        unbroken = 0f
        startles++
        here = Visits.present(arrivals, calm)
    }

    /** The next one to come, and how many more minutes of quiet it needs. */
    fun next(): Arrival? = arrivals.firstOrNull { it.at > calm && it.beast !in here }
}

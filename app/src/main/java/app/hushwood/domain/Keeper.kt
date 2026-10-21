package app.hushwood.domain

import app.hushwood.data.Session
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt

/** How close the guardian dares to come. It grows with the days you come, and never punishes the days you don't. */
enum class Trust {
    /** Eyes in the bushes. */
    WATCHING,

    /** At the edge of the clearing. */
    EDGE,

    /** Halfway to you. */
    NEAR,

    /** Asleep beside you while you sit. */
    BESIDE;

    companion object {
        /** From the days with a practice among the last fourteen. */
        fun of(days: Int) = when {
            days >= 9 -> BESIDE
            days >= 5 -> NEAR
            days >= 2 -> EDGE
            else -> WATCHING
        }
    }
}

/** A sitting's share of a portrait: how much of it, and in which light, which tints it. */
data class Stroke(val units: Int, val part: Daypart)

/**
 * One portrait of a guardian, filled in pixel by pixel by the sittings since it was begun. When it's
 * whole it goes up on the shelf and the next one begins: the guardian may be someone new by then.
 */
data class Portrait(val beast: Beast, val begun: Long, val strokes: List<Stroke>, val finished: Long? = null) {
    val painted: Int get() = strokes.sumOf { it.units }.coerceAtMost(Keeper.WHOLE)
    val share: Float get() = painted / Keeper.WHOLE.toFloat()
    val whole: Boolean get() = finished != null
}

/** What the animals make of how you practise. Everything here is worked out from the sittings, so nothing can drift. */
object Keeper {
    /** Before a guardian steps out: two weeks since the first sitting, and six sittings. */
    const val REVEAL_DAYS = 14
    const val REVEAL_SITS = 6

    /** The guardian is read from this many days back. */
    const val WINDOW_DAYS = 60

    /** A portrait, in units; a quarter of an hour adds 30, so about forty sittings fill one. */
    const val WHOLE = 1200

    fun units(minutes: Int) = minutes.coerceIn(3, 60) * 2

    /** The sitting that brings the guardian out, or null while it's still watching. */
    fun revealedBy(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): Session? {
        val first = sessions.firstOrNull() ?: return null
        val from = first.day(zone)
        return sessions.withIndex().firstOrNull { (i, s) ->
            i + 1 >= REVEAL_SITS && ChronoUnit.DAYS.between(from, s.day(zone)) >= REVEAL_DAYS
        }?.value
    }

    /** How far along the watching is: days and sittings still to go. */
    fun toReveal(sessions: List<Session>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Pair<Int, Int> {
        val first = sessions.firstOrNull()?.day(zone) ?: today
        val days = (REVEAL_DAYS - ChronoUnit.DAYS.between(first, today)).toInt().coerceAtLeast(0)
        val sits = (REVEAL_SITS - sessions.size).coerceAtLeast(0)
        return if (sessions.isEmpty()) REVEAL_DAYS to REVEAL_SITS else days to sits
    }

    /** Whose way of practising this is, read from [sessions] (the window up to now). */
    fun read(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): Beast {
        if (sessions.isEmpty()) return Beast.WREN
        val n = sessions.size.toFloat()
        val days = sessions.map { it.day(zone) }.toSortedSet()
        val span = (ChronoUnit.DAYS.between(days.first(), days.last()) + 1).coerceIn(1, WINDOW_DAYS.toLong()).toFloat()
        val regular = (days.size / span).coerceAtMost(1f)
        val avg = sessions.sumOf { it.minutes } / n
        fun share(p: (Session) -> Boolean) = sessions.count(p) / n
        val dawn = share { it.part(zone) == Daypart.DAWN }
        val dusk = share { it.part(zone) == Daypart.DUSK }
        val night = share { it.part(zone) == Daypart.NIGHT }
        val breath = share { it.kindOf == Kind.BREATH }
        val move = share { it.kindOf == Kind.MOVE || it.kindOf == Kind.WALK }
        val write = share { it.kindOf == Kind.WRITE }
        val stillness = sessions.filter { it.minutes > 0 && it.kindOf.still }.let { s -> if (s.isEmpty()) 0f else s.sumOf { it.still }.toFloat() / s.sumOf { it.minutes } }
        val perDay = n / days.size
        val variety = sessions.map { it.kind }.distinct().size
        // Hours counted from 4 in the morning so a night owl's 23:00 and 01:00 are close.
        val hours = sessions.map { ((it.at(zone).hour + 20) % 24).toFloat() }
        val mean = hours.average().toFloat()
        val spread = sqrt(hours.sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / n)

        val scores = mapOf(
            Beast.WREN to regular * 1.2f + dawn + (if (avg <= 15) 0.5f else 0f),
            Beast.OWL to night * 2f + (if (avg >= 20) 0.4f else 0f),
            Beast.HERON to breath * 2f + stillness * 0.5f,
            Beast.CRANE to move * 2f,
            Beast.BEAR to (if (regular < 0.4f) 1f else 0f) + (if (avg >= 25) 1f else 0f),
            Beast.FOX to (spread / 6f).coerceAtMost(1f) * 1.6f,
            Beast.HEDGEHOG to dusk * 1.2f + write * 1.2f,
            Beast.DEER to ((perDay - 1f) * 1.5f).coerceIn(0f, 1f) + (if (variety >= 3) 0.6f else 0f),
        )
        return Beast.Keepers.maxBy { scores.getValue(it) }
    }

    /** The window of sittings that reads the guardian at moment [at]. */
    private fun window(sessions: List<Session>, at: Long): List<Session> {
        val from = at - WINDOW_DAYS * 86_400_000L
        return sessions.filter { it.start in from..at }
    }

    /** Every portrait so far, the last one still being filled; empty while the guardian hasn't come out. */
    fun portraits(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): List<Portrait> {
        val reveal = revealedBy(sessions, zone) ?: return emptyList()
        val out = ArrayList<Portrait>()
        var beast = read(window(sessions, reveal.start), zone)
        var begun = reveal.start
        var strokes = ArrayList<Stroke>()
        var sum = 0
        for (s in sessions) {
            if (s.start < reveal.start) continue
            strokes += Stroke(units(s.minutes), s.part(zone))
            sum += units(s.minutes)
            if (sum >= WHOLE) {
                out += Portrait(beast, begun, strokes, finished = s.start)
                // A whole portrait: the forest looks again at how you've been practising.
                beast = read(window(sessions, s.start), zone)
                begun = s.end
                strokes = ArrayList()
                sum = 0
            }
        }
        out += Portrait(beast, begun, strokes)
        return out
    }

    fun guardian(sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()): Beast? = portraits(sessions, zone).lastOrNull()?.beast

    fun trust(sessions: List<Session>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Trust {
        val from = today.minusDays(13)
        return Trust.of(sessions.map { it.day(zone) }.filter { !it.isBefore(from) && !it.isAfter(today) }.distinct().size)
    }

    /** Breathing by the stream on 7 of the last 10 days: the kingfisher may come. */
    fun kingfisher(sessions: List<Session>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val from = today.minusDays(9)
        return sessions.filter { it.kindOf == Kind.BREATH }.map { it.day(zone) }.filter { !it.isBefore(from) }.distinct().size >= 7
    }
}

/** Small things the guardian leaves by the stump once it trusts you enough to sleep there. */
enum class Gift {
    FEATHER, ACORN, PEBBLE, PINECONE, FERN, ROWAN, SHELL, MOSS;

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name }

        /** One sitting in five, give or take, once the guardian sleeps beside you. */
        fun after(trust: Trust, seed: Long): Gift? {
            if (trust != Trust.BESIDE) return null
            if (Visits.unit(seed, 77) > 0.22f) return null
            return entries[(Visits.unit(seed, 78) * entries.size).toInt().coerceAtMost(entries.size - 1)]
        }
    }
}

package app.hushwood.domain

import app.hushwood.data.Session
import java.time.LocalDate
import java.time.ZoneId

/** A breathing rhythm, seconds per phase. The reeds sway with it and the fireflies gather and spread. */
enum class Breath(val inhale: Int, val hold: Int, val exhale: Int, val rest: Int) {
    CALM(4, 0, 6, 0),
    EVEN(5, 0, 5, 0),
    BOX(4, 4, 4, 4),
    RELAX(4, 7, 8, 0);

    val cycle: Int get() = inhale + hold + exhale + rest

    enum class Phase { IN, HOLD, OUT, REST }

    /** Where in the rhythm [seconds] falls: the phase, 0..1 through it, and the lungs 0 (empty) .. 1 (full). */
    data class At(val phase: Phase, val through: Float, val fill: Float, val left: Int)

    fun at(seconds: Float): At {
        val t = seconds.mod(cycle.toFloat())
        fun ease(x: Float) = (1 - kotlin.math.cos(x * Math.PI.toFloat())) / 2
        return when {
            t < inhale -> (t / inhale).let { At(Phase.IN, it, ease(it), (inhale - t).toInt() + 1) }
            t < inhale + hold -> ((t - inhale) / hold).let { At(Phase.HOLD, it, 1f, (inhale + hold - t).toInt() + 1) }
            t < inhale + hold + exhale -> ((t - inhale - hold) / exhale).let { At(Phase.OUT, it, 1 - ease(it), (inhale + hold + exhale - t).toInt() + 1) }
            else -> ((t - inhale - hold - exhale) / rest).let { At(Phase.REST, it, 0f, (cycle - t).toInt() + 1) }
        }
    }

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: CALM
    }
}

/** A year of sittings: minutes per day, for the rings of the stump. */
class Year(val year: Int, sessions: List<Session>, zone: ZoneId = ZoneId.systemDefault()) {
    val minutes: Map<LocalDate, Int> = sessions.filter { it.day(zone).year == year }.groupBy { it.day(zone) }.mapValues { (_, s) -> s.sumOf { it.minutes } }
    val total: Int = minutes.values.sum()
    val days: Int = minutes.size
    val sittings: Int = sessions.count { it.day(zone).year == year }

    /** The fullest day, for scaling the rings. */
    val most: Int = minutes.values.maxOrNull() ?: 0

    fun month(m: Int): Int = minutes.filterKeys { it.monthValue == m }.values.sum()
}

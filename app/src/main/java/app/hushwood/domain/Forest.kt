package app.hushwood.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

/** The four lights of a day in the clearing. The animals keep their own hours. */
enum class Daypart {
    DAWN, DAY, DUSK, NIGHT;

    companion object {
        fun of(hour: Int): Daypart = when (hour) {
            in 5..8 -> DAWN
            in 9..16 -> DAY
            in 17..20 -> DUSK
            else -> NIGHT
        }

        fun of(t: LocalDateTime): Daypart = of(t.hour)
    }
}

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    companion object {
        fun of(d: LocalDate): Season = when (d.month) {
            Month.MARCH, Month.APRIL, Month.MAY -> SPRING
            Month.JUNE, Month.JULY, Month.AUGUST -> SUMMER
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> AUTUMN
            else -> WINTER
        }

        /** Within two days of an equinox: the cranes pass over. */
        fun equinox(d: LocalDate): Boolean {
            fun near(m: Int, day: Int) = kotlin.math.abs(d.toEpochDay() - LocalDate.of(d.year, m, day).toEpochDay()) <= 2
            return near(3, 20) || near(9, 22)
        }
    }
}

/** A place in the clearing. Each practice has its own, and each place has its own visitors. */
enum class Spot { STUMP, STREAM, STONE, FIRE, PATH }

/** What a practice is, which decides where in the clearing it's done and whether stillness counts. */
enum class Kind(val spot: Spot, val still: Boolean) {
    SIT(Spot.STUMP, true),
    BREATH(Spot.STREAM, true),
    MOVE(Spot.STONE, false),
    WRITE(Spot.FIRE, true),
    WALK(Spot.PATH, false),
    OTHER(Spot.STUMP, true);

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** Left on the stump after a sitting; it brings those who like it sooner next time. */
enum class Offering(val minutes: Int) {
    SEEDS(0), BERRIES(60), NUTS(180), APPLE(400), HONEY(900);

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name }

        /** What the minutes so far have gathered. */
        fun gathered(totalMinutes: Int) = entries.filter { totalMinutes >= it.minutes }
    }
}

/**
 * Where you sit. The forest is open from the first day; the others open as the minutes add up, each with
 * animals of its own. The guardian follows you to all of them.
 */
enum class Place(val minutes: Int) {
    FOREST(0), LAKE(90), MOUNTAINS(300), SEA(700);

    companion object {
        fun of(name: String?) = entries.firstOrNull { it.name == name } ?: FOREST
        fun open(totalMinutes: Int) = entries.filter { totalMinutes >= it.minutes }
    }
}

private val ALL_YEAR = Season.entries.toSet()
private val WARM = setOf(Season.SPRING, Season.SUMMER, Season.AUTUMN)
private val DAWN_TO_DUSK = setOf(Daypart.DAWN, Daypart.DAY, Daypart.DUSK)
private val TWILIGHT = setOf(Daypart.DAWN, Daypart.DUSK, Daypart.NIGHT)

/**
 * Everyone who may come out, and where. [calm]: minutes of unbroken quiet before it dares, at its own place.
 * [rare] ones have a condition of their own on top of the hour and the season.
 */
enum class Beast(
    val place: Place,
    val calm: Float,
    val hours: Set<Daypart>,
    val seasons: Set<Season>,
    val likes: Set<Offering> = emptySet(),
    /** Comes sooner to these places. */
    val near: Set<Spot> = emptySet(),
    val rare: Boolean = false,
) {
    // The forest.
    TIT(Place.FOREST, 0.4f, DAWN_TO_DUSK, ALL_YEAR, setOf(Offering.SEEDS), setOf(Spot.STUMP)),
    ROBIN(Place.FOREST, 1f, setOf(Daypart.DAWN, Daypart.DUSK), WARM, setOf(Offering.SEEDS, Offering.BERRIES), setOf(Spot.STUMP, Spot.FIRE)),
    WREN(Place.FOREST, 1.5f, setOf(Daypart.DAWN, Daypart.DAY), ALL_YEAR, setOf(Offering.SEEDS), setOf(Spot.STUMP)),
    SQUIRREL(Place.FOREST, 2.5f, setOf(Daypart.DAWN, Daypart.DAY), ALL_YEAR, setOf(Offering.NUTS), setOf(Spot.STUMP)),
    HEDGEHOG(Place.FOREST, 3f, setOf(Daypart.DUSK, Daypart.NIGHT), WARM, setOf(Offering.APPLE), setOf(Spot.FIRE)),
    HARE(Place.FOREST, 5f, TWILIGHT, ALL_YEAR, setOf(Offering.APPLE), setOf(Spot.STONE, Spot.PATH)),
    OWL(Place.FOREST, 6f, setOf(Daypart.NIGHT), ALL_YEAR, near = setOf(Spot.FIRE)),
    FOX(Place.FOREST, 8f, TWILIGHT, ALL_YEAR, setOf(Offering.BERRIES), setOf(Spot.FIRE)),
    DEER(Place.FOREST, 12f, setOf(Daypart.DAWN, Daypart.DUSK), ALL_YEAR, setOf(Offering.APPLE), setOf(Spot.PATH)),
    BEAR(Place.FOREST, 20f, setOf(Daypart.DAY, Daypart.DUSK), WARM, setOf(Offering.BERRIES, Offering.HONEY)),
    FIREFLIES(Place.FOREST, 1f, setOf(Daypart.DUSK, Daypart.NIGHT), setOf(Season.SUMMER)),

    /** At dawn, after half an hour of quiet. */
    WHITE_DEER(Place.FOREST, 30f, setOf(Daypart.DAWN), ALL_YEAR, rare = true),

    // The lake.
    DUCK(Place.LAKE, 0.6f, DAWN_TO_DUSK, WARM, setOf(Offering.SEEDS), setOf(Spot.STREAM)),
    FROG(Place.LAKE, 1.5f, setOf(Daypart.DAY, Daypart.DUSK, Daypart.NIGHT), setOf(Season.SPRING, Season.SUMMER), near = setOf(Spot.STREAM)),
    DRAGONFLY(Place.LAKE, 0.8f, setOf(Daypart.DAY), setOf(Season.SUMMER)),
    HERON(Place.LAKE, 5f, DAWN_TO_DUSK, WARM, near = setOf(Spot.STREAM)),
    BEAVER(Place.LAKE, 6f, TWILIGHT, WARM, setOf(Offering.APPLE), setOf(Spot.STREAM)),
    SWAN(Place.LAKE, 8f, DAWN_TO_DUSK, WARM, setOf(Offering.SEEDS)),
    OTTER(Place.LAKE, 9f, TWILIGHT, ALL_YEAR, near = setOf(Spot.STREAM)),
    CRANE(Place.LAKE, 10f, setOf(Daypart.DAWN, Daypart.DAY), WARM, near = setOf(Spot.STONE)),
    MOOSE(Place.LAKE, 16f, setOf(Daypart.DAWN, Daypart.DUSK), ALL_YEAR, setOf(Offering.APPLE)),

    /** By the water, only for someone who has been breathing there 7 days of the last 10. */
    KINGFISHER(Place.LAKE, 4f, setOf(Daypart.DAWN, Daypart.DAY), WARM, near = setOf(Spot.STREAM), rare = true),

    /** A skein passing over, in the days around an equinox. */
    CRANES(Place.LAKE, 3f, DAWN_TO_DUSK, setOf(Season.SPRING, Season.AUTUMN), rare = true),

    // The mountains.
    CHOUGH(Place.MOUNTAINS, 0.5f, DAWN_TO_DUSK, ALL_YEAR, setOf(Offering.SEEDS, Offering.BERRIES), setOf(Spot.STONE)),
    PIKA(Place.MOUNTAINS, 1.5f, setOf(Daypart.DAWN, Daypart.DAY), ALL_YEAR, setOf(Offering.SEEDS), setOf(Spot.STONE)),
    BUTTERFLY(Place.MOUNTAINS, 1f, setOf(Daypart.DAY), setOf(Season.SUMMER)),
    MARMOT(Place.MOUNTAINS, 3f, setOf(Daypart.DAWN, Daypart.DAY), WARM, setOf(Offering.APPLE), setOf(Spot.PATH)),
    EAGLE(Place.MOUNTAINS, 6f, setOf(Daypart.DAY), ALL_YEAR),
    IBEX(Place.MOUNTAINS, 9f, DAWN_TO_DUSK, ALL_YEAR, setOf(Offering.APPLE), setOf(Spot.STONE)),
    LYNX(Place.MOUNTAINS, 14f, TWILIGHT, ALL_YEAR, near = setOf(Spot.FIRE)),

    /** Only in winter, at dusk or night, after long quiet. */
    SNOW_LEOPARD(Place.MOUNTAINS, 24f, setOf(Daypart.DUSK, Daypart.NIGHT), setOf(Season.WINTER), rare = true),

    // The sea.
    CRAB(Place.SEA, 0.5f, setOf(Daypart.DAY, Daypart.DUSK, Daypart.NIGHT), WARM, near = setOf(Spot.STREAM)),
    SANDPIPER(Place.SEA, 1f, DAWN_TO_DUSK, WARM, setOf(Offering.SEEDS), setOf(Spot.STREAM)),
    GULL(Place.SEA, 1.5f, DAWN_TO_DUSK, ALL_YEAR, setOf(Offering.SEEDS, Offering.BERRIES), setOf(Spot.FIRE)),
    PUFFIN(Place.SEA, 4f, setOf(Daypart.DAWN, Daypart.DAY), setOf(Season.SPRING, Season.SUMMER), near = setOf(Spot.STONE)),
    SEAL(Place.SEA, 8f, DAWN_TO_DUSK, ALL_YEAR, near = setOf(Spot.STONE)),

    /** The sea glowing where the waves break, on late-summer nights. */
    SPARKLE(Place.SEA, 2f, setOf(Daypart.NIGHT), setOf(Season.SUMMER, Season.AUTUMN), near = setOf(Spot.STREAM)),

    /** A spout far out, after twenty minutes of looking at the sea. */
    WHALE(Place.SEA, 20f, setOf(Daypart.DAWN, Daypart.DAY), setOf(Season.SUMMER, Season.AUTUMN), rare = true);

    /** Can be a guardian. */
    val keeper: Boolean get() = this in Keepers

    /** Seen as a swarm of lights or wings over the place, not as one animal. */
    val swarm: Boolean get() = this == FIREFLIES || this == DRAGONFLY || this == BUTTERFLY || this == SPARKLE

    /** In the sky, not on the ground. */
    val flying: Boolean get() = this == CRANES || this == EAGLE

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name }

        /** The animals that can choose you, in the order ties are broken. They follow you everywhere. */
        val Keepers = listOf(WREN, OWL, HERON, CRANE, BEAR, FOX, HEDGEHOG, DEER)

        fun list(csv: String): List<Beast> = csv.split(',').mapNotNull { of(it.trim()) }
        fun csv(b: Collection<Beast>) = b.joinToString(",") { it.name }
    }
}

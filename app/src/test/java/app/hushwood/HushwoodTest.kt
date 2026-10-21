package app.hushwood

import app.hushwood.data.Session
import app.hushwood.domain.Beast
import app.hushwood.domain.Breath
import app.hushwood.domain.Daypart
import app.hushwood.domain.Keeper
import app.hushwood.domain.Kind
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import app.hushwood.domain.Quiet
import app.hushwood.domain.Season
import app.hushwood.domain.Setting
import app.hushwood.domain.Spot
import app.hushwood.domain.Trust
import app.hushwood.domain.Visits
import app.hushwood.domain.Year
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HushwoodTest {
    private val zone = ZoneOffset.UTC
    private val day0 = LocalDate.of(2026, 6, 1)

    private fun sit(day: Int, hour: Int, minutes: Int, kind: Kind = Kind.SIT, still: Int = minutes / 2) = Session(
        practiceId = 1, kind = kind.name, start = day0.plusDays(day.toLong()).atTime(hour, 0).toInstant(zone).toEpochMilli(),
        end = day0.plusDays(day.toLong()).atTime(hour, 0).toInstant(zone).toEpochMilli() + minutes * 60_000L, minutes = minutes, still = still,
    )

    @Test fun dayparts() {
        assertEquals(Daypart.NIGHT, Daypart.of(4))
        assertEquals(Daypart.DAWN, Daypart.of(5))
        assertEquals(Daypart.DAY, Daypart.of(12))
        assertEquals(Daypart.DUSK, Daypart.of(19))
        assertEquals(Daypart.NIGHT, Daypart.of(23))
        assertTrue(Season.equinox(LocalDate.of(2026, 9, 21)))
        assertFalse(Season.equinox(LocalDate.of(2026, 7, 1)))
    }

    @Test fun onlyTheirPlaceTheirHourTheirSeason() {
        val s = Setting(Place.FOREST, Spot.STUMP, Daypart.NIGHT, LocalDate.of(2026, 1, 10))
        val who = Visits.arrivals(s).map { it.beast }
        assertTrue(Beast.OWL in who)
        assertFalse(Beast.TIT in who) // asleep at night
        assertFalse(Beast.HEDGEHOG in who) // asleep all winter
        assertFalse(Beast.DUCK in who) // another place
        assertTrue(who.all { it.place == Place.FOREST })
    }

    @Test fun anOfferingBringsThemSooner() {
        val plain = Setting(Place.FOREST, Spot.STUMP, Daypart.DAY, day0, seed = 3)
        val nuts = plain.copy(offering = Offering.NUTS)
        val a = Visits.arrivals(plain).first { it.beast == Beast.SQUIRREL }.at
        val b = Visits.arrivals(nuts).first { it.beast == Beast.SQUIRREL }.at
        assertEquals(a * 0.6f, b, 0.001f)
    }

    @Test fun rareOnesKeepTheirConditions() {
        val dawn = Setting(Place.FOREST, Spot.STUMP, Daypart.DAWN, day0)
        val white = Visits.arrivals(dawn).first { it.beast == Beast.WHITE_DEER }
        assertEquals(30f, white.at, 0.001f)
        val lake = Setting(Place.LAKE, Spot.STREAM, Daypart.DAY, day0)
        assertFalse(Visits.arrivals(lake).any { it.beast == Beast.KINGFISHER })
        assertTrue(Visits.arrivals(lake.copy(kingfisher = true)).any { it.beast == Beast.KINGFISHER })
    }

    @Test fun quietBuildsAndAStartleTakesMostOfIt() {
        val q = Quiet(Visits.arrivals(Setting(Place.FOREST, Spot.STUMP, Daypart.DAY, day0, seed = 1)), moving = false)
        repeat(10) { q.tick(1f) }
        assertEquals(10f, q.calm, 0.01f)
        val before = q.here.size
        assertTrue(before > 0)
        q.startle()
        assertEquals(4f, q.calm, 0.01f)
        assertEquals(1, q.startles)
        assertTrue(q.here.size <= before)
        assertTrue(q.seen.size >= before)
        assertTrue(q.here.size <= Visits.MAX)
    }

    @Test fun movingPracticesAreNotStartled() {
        val q = Quiet(Visits.arrivals(Setting(Place.FOREST, Spot.PATH, Daypart.DAY, day0)), moving = true)
        q.tick(4f)
        q.startle()
        assertEquals(2f, q.calm, 0.01f)
        assertEquals(0, q.startles)
    }

    @Test fun theGuardianWaitsTwoWeeksAndSixSittings() {
        val few = (0 until 5).map { sit(it * 4, 7, 10) }
        assertNull(Keeper.revealedBy(few, zone))
        val quick = (0 until 10).map { sit(it, 7, 10) }
        assertNull(Keeper.revealedBy(quick, zone))
        val enough = (0 until 16).map { sit(it, 7, 10) }
        assertEquals(enough[14], Keeper.revealedBy(enough, zone))
    }

    @Test fun theGuardianIsReadFromThePractice() {
        assertEquals(Beast.HERON, Keeper.read((0 until 20).map { sit(it, 12, 15, Kind.BREATH, still = 12) }, zone))
        assertEquals(Beast.OWL, Keeper.read((0 until 20).map { sit(it, 23, 30) }, zone))
        assertEquals(Beast.CRANE, Keeper.read((0 until 20).map { sit(it, 12, 20, Kind.MOVE, still = 0) }, zone))
        assertEquals(Beast.BEAR, Keeper.read((0 until 8).map { sit(it * 7, 13, 40) }, zone))
        assertEquals(Beast.WREN, Keeper.read((0 until 20).map { sit(it, 6, 8) }, zone))
        assertEquals(Beast.HEDGEHOG, Keeper.read((0 until 20).map { sit(it, 19, 15, Kind.WRITE) }, zone))
    }

    @Test fun portraitsFillAndTheNextBegins() {
        // 30 days of 30 minutes = 60 units a sitting: the first portrait (1200) is whole after 20 sittings from the reveal.
        val all = (0 until 40).map { sit(it, 7, 30) }
        val p = Keeper.portraits(all, zone)
        assertEquals(2, p.size)
        assertTrue(p[0].whole)
        assertEquals(Keeper.WHOLE, p[0].painted)
        assertFalse(p[1].whole)
        assertEquals(Keeper.units(30) * (40 - 14 - 20), p[1].painted)
    }

    @Test fun trustGrowsWithTheDaysAndForgives() {
        val today = day0.plusDays(30)
        assertEquals(Trust.WATCHING, Keeper.trust(emptyList(), today, zone))
        assertEquals(Trust.EDGE, Keeper.trust((27..30).filter { it % 2 == 0 }.map { sit(it, 7, 10) }, today, zone))
        assertEquals(Trust.BESIDE, Keeper.trust((17..30).map { sit(it, 7, 10) }, today, zone))
        // A long gap: it grows shy again, but the past isn't lost.
        assertEquals(Trust.WATCHING, Keeper.trust((0..10).map { sit(it, 7, 10) }, today, zone))
    }

    @Test fun breathTurnsThroughItsPhases() {
        val b = Breath.BOX
        assertEquals(Breath.Phase.IN, b.at(1f).phase)
        assertEquals(Breath.Phase.HOLD, b.at(5f).phase)
        assertEquals(Breath.Phase.OUT, b.at(9f).phase)
        assertEquals(Breath.Phase.REST, b.at(13f).phase)
        assertEquals(Breath.Phase.IN, b.at(17f).phase)
        assertEquals(1f, b.at(5f).fill, 0.001f)
        assertEquals(0f, Breath.CALM.at(0f).fill, 0.001f)
    }

    @Test fun placesOpenWithTheMinutes() {
        assertEquals(listOf(Place.FOREST), Place.open(0))
        assertEquals(listOf(Place.FOREST, Place.LAKE), Place.open(120))
        assertEquals(Place.entries, Place.open(10_000))
    }

    @Test fun yearCountsDays() {
        val y = Year(2026, listOf(sit(0, 7, 10), sit(0, 20, 15), sit(3, 7, 5)), zone)
        assertEquals(30, y.total)
        assertEquals(2, y.days)
        assertEquals(3, y.sittings)
        assertEquals(25, y.most)
    }
}

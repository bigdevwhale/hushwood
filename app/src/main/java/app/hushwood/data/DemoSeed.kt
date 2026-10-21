package app.hushwood.data

import app.hushwood.AppContainer
import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Gift
import app.hushwood.domain.Keeper
import app.hushwood.domain.Kind
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import app.hushwood.domain.Setting
import app.hushwood.domain.Visits
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * The demo build's made-up half year: someone who breathes by the water most mornings, sits now and then, keeps a
 * journal some evenings. Enough for every place to be open, a heron for a guardian and a couple of portraits done.
 */
object DemoSeed {
    private val notesRu = listOf(
        "Утро тихое, и я тоже.", "Долго не получалось усесться, потом отпустило.", "Синица прилетела почти сразу.",
        "Мысли о маме, тёплые.", "Шумно в голове, но дыхание ровное.", "Ветер в камышах, как будто дышит вместе со мной.",
        "Не хотелось, но пришлось — и хорошо.", "Свет на воде. Больше ничего не надо.", "Усталость, но хотя бы пять минут тишины.",
        "Цапля стояла дольше меня.", "Снег, и такая тишина.", "Сегодня благодарность за чай и за солнце.",
    )
    private val notesEn = listOf(
        "A quiet morning, and so am I.", "Took a while to settle, then it let go.", "A tit came almost at once.",
        "Thought of my mother, warmly.", "Noisy head, steady breath.", "Wind in the reeds, breathing with me.",
        "Didn't want to, did it anyway, glad.", "Light on the water. Nothing else needed.", "Tired, but sat for five minutes at least.",
        "The heron stood longer than I did.", "Snow, and such quiet.", "Grateful today for tea and for the sun.",
    )

    suspend fun seedIfEmpty(c: AppContainer, ru: Boolean) {
        val dao = c.db.dao()
        val lang = if (ru) "ru" else "en"
        val s = c.settings.now()
        if (dao.sessionsNow().isNotEmpty() && s.demoLang == lang) return
        dao.clearSessions()
        dao.clearPractices()
        val names = if (ru) listOf("Медитация", "Дыхание", "Йога", "Дневник", "Прогулка") else listOf("Meditation", "Breathing", "Yoga", "Journal", "Mindful walk")
        val kinds = listOf(Kind.SIT, Kind.BREATH, Kind.MOVE, Kind.WRITE, Kind.WALK)
        dao.insertPractices(names.mapIndexed { i, n -> Practice(name = n, kind = kinds[i].name, order = i) })
        val practices = dao.practicesNow()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val rnd = Random(42)
        val out = ArrayList<Session>()
        var total = 0
        val notes = if (ru) notesRu else notesEn
        for (back in 170 downTo 0) {
            val day = today.minusDays(back.toLong())
            if (rnd.nextFloat() > 0.8f && back > 0) continue
            val count = if (rnd.nextFloat() < 0.2f) 2 else 1
            for (n in 0 until count) {
                val r = rnd.nextFloat()
                val p = when {
                    n == 1 && r < 0.6f -> practices[3]
                    r < 0.55f -> practices[1]
                    r < 0.75f -> practices[0]
                    r < 0.88f -> practices[2]
                    else -> practices[4]
                }
                val hour = when (p.kindOf) {
                    Kind.WRITE -> 20 + rnd.nextInt(2)
                    Kind.WALK -> 17 + rnd.nextInt(2)
                    else -> if (rnd.nextFloat() < 0.75f) 6 + rnd.nextInt(3) else 12 + rnd.nextInt(6)
                }
                // Today's sitting is earlier this morning, so the demo always has one.
                val start = day.atTime(if (back == 0) 7 else hour, rnd.nextInt(60)).atZone(zone)
                val minutes = when (p.kindOf) { Kind.BREATH -> 8 + rnd.nextInt(14); Kind.WRITE -> 10 + rnd.nextInt(10); Kind.WALK -> 25 + rnd.nextInt(30); else -> 12 + rnd.nextInt(22) }
                val open = Place.open(total)
                val place = if (p.kindOf == Kind.BREATH && Place.LAKE in open) Place.LAKE else open[rnd.nextInt(open.size)]
                val startMs = start.toInstant().toEpochMilli()
                val setting = Setting(place, p.kindOf.spot, Daypart.of(start.toLocalDateTime()), day, Offering.entries[rnd.nextInt(3)], seed = startMs, kingfisher = back < 40)
                val came = Visits.present(Visits.arrivals(setting), minutes * (if (p.kindOf.still) 0.9f else Visits.MOVING_RATE))
                val still = if (p.kindOf.still) (minutes * (0.5f + rnd.nextFloat() * 0.5f)).toInt() else 0
                val trustNow = Keeper.trust(out, day, zone)
                out += Session(
                    practiceId = p.id, kind = p.kind, place = place.name, start = startMs, end = startMs + minutes * 60_000L, minutes = minutes,
                    still = still, startles = if (p.kindOf.still) rnd.nextInt(3) else 0, beasts = Beast.csv(came),
                    note = if (rnd.nextFloat() < 0.3f) notes[rnd.nextInt(notes.size)] else "",
                    gift = Gift.after(trustNow, startMs)?.name ?: "",
                )
                total += minutes
            }
        }
        dao.insertSessions(out)
        c.settings.update {
            it.copy(onboarded = true, place = Place.LAKE, offering = Offering.BERRIES, remind = false, shownGuardian = true, demoLang = lang, sitting = null, lastPractice = practices[1].id)
        }
    }
}

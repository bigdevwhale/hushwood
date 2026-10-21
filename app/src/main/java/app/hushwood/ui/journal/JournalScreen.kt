package app.hushwood.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.hushwood.R
import app.hushwood.domain.Beast
import app.hushwood.domain.Kind
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.domain.Year
import app.hushwood.ui.T
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.BeastIcon
import app.hushwood.ui.components.PixImage
import app.hushwood.ui.components.Segmented
import app.hushwood.ui.components.UCard
import app.hushwood.ui.ctx
import app.hushwood.ui.pix.Rings
import app.hushwood.ui.pix.Things
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType

private enum class Page { MEETINGS, HERBARIUM, RINGS }

/** The field journal: who has come so far and who hasn't yet, the pressed lines of the herbarium, the rings of the year. */
@Composable
fun JournalScreen(woods: Woods, first: Int = 0) {
    var page by rememberSaveable { mutableStateOf(Page.entries[first]) }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        Text(stringResource(R.string.journal_title), style = UType.Display, modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp))
        Spacer(Modifier.height(12.dp))
        Segmented(
            Page.entries, page,
            { stringResource(when (it) { Page.MEETINGS -> R.string.journal_meetings; Page.HERBARIUM -> R.string.journal_herbarium; Page.RINGS -> R.string.journal_rings }) },
            { page = it },
            Modifier.padding(horizontal = 16.dp),
        )
        when (page) {
            Page.MEETINGS -> Meetings(woods)
            Page.HERBARIUM -> Herbarium(woods)
            Page.RINGS -> RingsPage(woods)
        }
    }
}

@Composable
private fun Meetings(woods: Woods) {
    val ru = woods.ru
    var open by remember { mutableStateOf<Beast?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            val met = woods.seen.size
            Text(stringResource(R.string.journal_met, met, Beast.entries.size), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
        }
        for (place in Place.entries) {
            val beasts = Beast.entries.filter { it.place == place }
            val opened = place in woods.open
            item(key = place.name) {
                Row(Modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Words.place(place).of(ru).uppercase(), style = UType.Eyebrow)
                    if (!opened) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Lock, null, Modifier.size(13.dp), tint = U.c.faint)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.place_opens, place.minutes - woods.totalMinutes), style = UType.Small)
                    }
                }
            }
            items(beasts.chunked(3), key = { row -> row.first().name }) { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { b ->
                        val n = woods.seen[b] ?: 0
                        Column(
                            Modifier.weight(1f).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { open = b }.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BeastIcon(b, 64.dp, unknown = n == 0, animate = false)
                            Spacer(Modifier.height(4.dp))
                            Text(if (n > 0) Words.name(b).of(ru) else "???", style = UType.Small.copy(color = if (n > 0) U.c.ink else U.c.faint), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (n > 0) "× $n" else if (b.rare) stringResource(R.string.rare) else " ", style = UType.Small.copy(color = if (b.rare && n == 0) U.c.amber else U.c.faint))
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    open?.let { b -> BeastCard(b, woods) { open = null } }
}

/** One animal, close up: what it is, when it comes, what it likes, how often it came to you. */
@Composable
private fun BeastCard(b: Beast, woods: Woods, onDismiss: () -> Unit) {
    val ru = woods.ru
    val n = woods.seen[b] ?: 0
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RXl).background(U.c.surface).border(1.dp, U.c.line, U.RXl).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(130.dp).clip(U.RLg).background(U.c.surface2), contentAlignment = Alignment.BottomCenter) {
                BeastIcon(b, 120.dp, unknown = n == 0, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(if (n > 0) Words.name(b).of(ru) else stringResource(R.string.unknown), style = UType.Title, textAlign = TextAlign.Center)
            Text(Words.place(b.place).of(ru), style = UType.Muted)
            Spacer(Modifier.height(10.dp))
            if (n > 0) Text(Words.lore(b).of(ru), style = UType.Line.copy(fontSize = 16.sp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            val hours = b.hours.joinToString(", ") { Words.part(it).of(ru) }
            val seasons = if (b.seasons.size == Season.entries.size) stringResource(R.string.all_year) else b.seasons.joinToString(", ") { Words.season(it).of(ru) }
            Text(stringResource(R.string.beast_when, hours, seasons), style = UType.Muted, textAlign = TextAlign.Center)
            if (b.likes.isNotEmpty()) Text(stringResource(R.string.beast_likes, b.likes.joinToString(", ") { Words.offering(it).of(ru).lowercase() }), style = UType.Muted, textAlign = TextAlign.Center)
            if (n == 0 && b.rare) Text(Words.lore(b).of(ru), style = UType.Muted.copy(color = U.c.amber), textAlign = TextAlign.Center)
            if (n > 0) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.beast_seen, n, T.date(woods.firstSeen[b] ?: woods.today, ru)), style = UType.Strong, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Every line written after a sitting, pressed as a leaf of its season. */
@Composable
private fun Herbarium(woods: Woods) {
    val c = ctx()
    val ru = woods.ru
    val notes = woods.sessions.filter { it.note.isNotBlank() }.reversed()
    if (notes.isEmpty()) {
        Text(stringResource(R.string.herbarium_empty), style = UType.Line, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(notes, key = { it.id }) { s ->
            val day = s.day(woods.zone)
            UCard {
                Row(verticalAlignment = Alignment.Top) {
                    PixImage(Things.leaf((s.start / 86_400_000L).toInt(), Season.of(day)), 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.note, style = UType.Line.copy(fontSize = 17.sp))
                        Spacer(Modifier.height(4.dp))
                        Text(T.date(day, ru) + " · " + (woods.practice(s.practiceId)?.name ?: "") + " · " + T.minutes(c, s.minutes), style = UType.Small)
                    }
                }
            }
        }
    }
}

/** The year as a stump cut across: a ring for each month, thicker on the days you sat longer. */
@Composable
private fun RingsPage(woods: Woods) {
    val c = ctx()
    val ru = woods.ru
    val years = (woods.sessions.map { it.day(woods.zone).year } + woods.today.year).distinct().sorted()
    var yi by remember { mutableIntStateOf(years.size - 1) }
    val year = remember(woods.sessions, yi) { Year(years[yi], woods.sessions, woods.zone) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                years.forEachIndexed { i, y ->
                    Text(
                        y.toString(),
                        style = UType.Strong.copy(color = if (i == yi) U.c.moss else U.c.muted),
                        modifier = Modifier.clip(U.RSm).clickable { yi = i }.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val px = remember(year) { Rings.paint(year) }
            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                PixImage(px, 320.dp, bottom = false)
            }
            Text(stringResource(R.string.rings_sub), style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(T.minutes(c, year.total), stringResource(R.string.stat_total), Modifier.weight(1f))
                Stat(year.days.toString(), stringResource(R.string.stat_days), Modifier.weight(1f))
                Stat(year.sittings.toString(), stringResource(R.string.stat_sittings), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            val byPractice = woods.sessions.filter { it.day(woods.zone).year == year.year }.groupBy { it.practiceId }.mapValues { (_, l) -> l.sumOf { it.minutes } }.toList().sortedByDescending { it.second }
            val most = byPractice.maxOfOrNull { it.second } ?: 1
            if (byPractice.isNotEmpty()) UCard {
                Text(stringResource(R.string.by_practice).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(8.dp))
                byPractice.forEach { (pid, m) ->
                    val p = woods.practice(pid)
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p?.name ?: "", style = UType.Body, modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.weight(1f).height(10.dp).clip(U.RSm).background(U.c.surface2)) {
                            Box(Modifier.fillMaxWidth(m / most.toFloat()).height(10.dp).background(if (p?.kindOf == Kind.BREATH) U.c.amber else U.c.moss))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(T.minutes(c, m), style = UType.Small, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).padding(12.dp)) {
        Text(value, style = UType.Number.copy(fontSize = 18.sp), maxLines = 1)
        Text(label, style = UType.Small)
    }
}

package app.hushwood.ui.keeper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hushwood.R
import app.hushwood.domain.Beast
import app.hushwood.domain.Gift
import app.hushwood.domain.Keeper
import app.hushwood.domain.Trust
import app.hushwood.ui.T
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.BeastIcon
import app.hushwood.ui.components.PixImage
import app.hushwood.ui.components.SectionTitle
import app.hushwood.ui.components.UCard
import app.hushwood.ui.pix.Portraits
import app.hushwood.ui.pix.Things
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType
import java.time.Instant
import java.time.LocalDate

/**
 * The guardian: before it shows itself, the forest watching you; after, its portrait coming out of the paper one
 * sitting at a time, how close it dares to come, a small suggestion, what it has left you, the finished portraits.
 */
@Composable
fun KeeperScreen(woods: Woods) {
    val ru = woods.ru
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.keeper_title), style = UType.Display)
        val g = woods.guardian
        val portrait = woods.portrait
        if (g == null || portrait == null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.keeper_watching), style = UType.Line)
            Spacer(Modifier.height(16.dp))
            val (days, sits) = woods.toReveal
            UCard {
                Text(stringResource(R.string.keeper_soon), style = UType.Strong)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.keeper_soon_sub, days, sits), style = UType.Muted)
            }
            Ways(woods)
            Spacer(Modifier.height(24.dp))
            return@Column
        }

        Spacer(Modifier.height(16.dp))
        val px = remember(portrait) { Portraits.paint(portrait.beast, portrait.strokes) }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PixImage(px, 260.dp, bottom = false)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.keeper_painted, (portrait.share * 100).toInt()), style = UType.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Box(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 6.dp).height(6.dp).clip(U.RSm).background(U.c.surface2)) {
            Box(Modifier.fillMaxWidth(portrait.share.coerceAtLeast(0.01f)).height(6.dp).background(U.c.moss))
        }
        Spacer(Modifier.height(14.dp))
        Text(Words.name(g).of(ru), style = UType.Hero, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(Words.quality(g).of(ru), style = UType.Strong.copy(color = U.c.moss), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(Words.why(g).of(ru), style = UType.Line.copy(fontSize = 17.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        SectionTitle(stringResource(R.string.keeper_trust))
        val trust = woods.trust ?: Trust.WATCHING
        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Trust.entries.forEach { t ->
                    Box(Modifier.size(12.dp).clip(CircleShape).background(if (t <= trust) U.c.moss else U.c.surface2))
                    if (t != Trust.BESIDE) Box(Modifier.weight(1f).height(2.dp).background(if (t < trust) U.c.moss else U.c.surface2))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(Words.name(g).of(ru) + " " + Words.trust(trust).of(ru) + ".", style = UType.Strong)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.keeper_trust_sub), style = UType.Muted)
        }

        SectionTitle(stringResource(R.string.keeper_hint))
        UCard { Text(Words.hint(g).of(ru), style = UType.Line.copy(fontSize = 16.sp)) }

        val gifts = woods.gifts.mapNotNull { Gift.of(it.gift) }.groupingBy { it }.eachCount()
        SectionTitle(stringResource(R.string.keeper_gifts))
        if (gifts.isEmpty()) Text(stringResource(R.string.keeper_gifts_empty), style = UType.Muted)
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            gifts.forEach { (gift, n) ->
                Column(Modifier.clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PixImage(Things.gift(gift), 36.dp)
                    Text("× $n", style = UType.Small)
                }
            }
        }

        val shelf = woods.portraits.filter { it.whole }
        if (shelf.isNotEmpty()) {
            SectionTitle(stringResource(R.string.keeper_shelf))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                shelf.forEach { p ->
                    Column(Modifier.width(110.dp).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val img = remember(p) { Portraits.paint(p.beast, p.strokes) }
                        PixImage(img, 92.dp, bottom = false)
                        Text(Words.name(p.beast).of(ru), style = UType.Small.copy(color = U.c.ink), maxLines = 1)
                        Text(T.short(day(p.finished ?: 0, woods), ru), style = UType.Small)
                    }
                }
            }
        }
        Ways(woods)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.keeper_about, Keeper.WINDOW_DAYS), style = UType.Small, modifier = Modifier.padding(vertical = 16.dp))
    }
}

/** Every guardian there is, and the way of practising that brings each one out; known ones in colour. */
@Composable
private fun Ways(woods: Woods) {
    val ru = woods.ru
    val known = woods.portraits.map { it.beast }.toSet()
    SectionTitle(stringResource(R.string.keeper_ways))
    Text(stringResource(R.string.keeper_ways_sub), style = UType.Muted)
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Beast.Keepers.forEach { b ->
            val here = b == woods.guardian
            Row(
                Modifier.fillMaxWidth().clip(U.RMd).background(if (here) U.c.mossSoft else U.c.surface).border(1.dp, if (here) U.c.moss.copy(alpha = 0.5f) else U.c.line, U.RMd).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { BeastIcon(b, 52.dp, unknown = b !in known, animate = here) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(Words.name(b).of(ru) + " · " + Words.quality(b).of(ru), style = UType.Strong)
                    Text(Words.way(b).of(ru), style = UType.Muted)
                }
            }
        }
    }
}

private fun day(ms: Long, woods: Woods): LocalDate = Instant.ofEpochMilli(ms).atZone(woods.zone).toLocalDate()

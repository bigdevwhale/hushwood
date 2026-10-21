package app.hushwood.ui.sit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hushwood.R
import app.hushwood.container
import app.hushwood.domain.Gift
import app.hushwood.domain.Keeper
import app.hushwood.domain.Offering
import app.hushwood.ui.Go
import app.hushwood.ui.T
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.BeastIcon
import app.hushwood.ui.components.PixImage
import app.hushwood.ui.components.PrimaryButton
import app.hushwood.ui.components.SectionTitle
import app.hushwood.ui.components.UCard
import app.hushwood.ui.ctx
import app.hushwood.ui.pix.Portraits
import app.hushwood.ui.pix.Things
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType

/**
 * After a sitting: who came out, anything the guardian left, a line for the herbarium, and what to leave on
 * the stump for next time. If the guardian stepped out today, or a portrait was finished, it's told here.
 */
@Composable
fun AfterScreen(id: Long, woods: Woods, go: Go) {
    val c = ctx()
    val ru = woods.ru
    val session = woods.sessions.firstOrNull { it.id == id }
    if (session == null) {
        Box(Modifier.fillMaxSize().background(U.c.bg))
        return
    }
    var note by remember(id) { mutableStateOf(session.note) }
    var offering by remember { mutableStateOf(woods.settings.offering) }
    val earlier = woods.sessions.filter { it.start < session.start }
    val firsts = session.seen.filter { b -> earlier.none { b in it.seen } }
    val revealed = Keeper.revealedBy(woods.sessions, woods.zone)?.id == session.id
    val finished = woods.portraits.firstOrNull { it.finished == session.start }

    fun done() {
        c.container.write {
            if (note.trim() != session.note) db.dao().update(session.copy(note = note.trim()))
            settings.update { it.copy(offering = offering) }
        }
        go.home()
    }
    BackHandler { done() }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.after_title), style = UType.Eyebrow)
            Spacer(Modifier.height(4.dp))
            Text(T.minutes(c, session.minutes) + " · " + Words.place(session.placeOf).of(ru), style = UType.Display)
            if (session.kindOf.still) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.after_still, session.still, session.startles), style = UType.Muted)
            }

            if (revealed && woods.guardian != null) {
                Spacer(Modifier.height(16.dp))
                val g = woods.portraits.first().beast
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BeastIcon(g, 72.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.after_guardian), style = UType.Eyebrow)
                            Text(Words.name(g).of(ru), style = UType.Title)
                            Text(Words.quality(g).of(ru), style = UType.Strong.copy(color = U.c.moss))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(Words.why(g).of(ru), style = UType.Line.copy(fontSize = 16.sp))
                }
            }
            if (finished != null) {
                Spacer(Modifier.height(16.dp))
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixImage(Portraits.paint(finished.beast, finished.strokes), 84.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.after_portrait), style = UType.Strong)
                            Text(stringResource(R.string.after_portrait_sub), style = UType.Muted)
                        }
                    }
                }
            }

            SectionTitle(stringResource(if (session.seen.isEmpty()) R.string.after_nobody else R.string.after_who))
            if (session.seen.isEmpty()) {
                Text(stringResource(R.string.after_nobody_sub), style = UType.Muted)
            } else FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                session.seen.forEach { b ->
                    Column(Modifier.width(96.dp).clip(U.RMd).background(U.c.surface).border(1.dp, if (b in firsts) U.c.moss.copy(alpha = 0.6f) else U.c.line, U.RMd).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BeastIcon(b, 64.dp)
                        Text(Words.name(b).of(ru), style = UType.Small.copy(color = U.c.ink), textAlign = TextAlign.Center, maxLines = 2)
                        if (b in firsts) Text(stringResource(R.string.after_first), style = UType.Small.copy(color = U.c.moss))
                    }
                }
            }

            Gift.of(session.gift)?.let { g ->
                Spacer(Modifier.height(14.dp))
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixImage(Things.gift(g), 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.after_gift, woods.guardian?.let { Words.name(it).of(ru) } ?: "", Words.gift(g).of(ru)), style = UType.Body, modifier = Modifier.weight(1f))
                    }
                }
            }

            SectionTitle(stringResource(R.string.after_note))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 140) note = it.replace('\n', ' ') },
                placeholder = { Text(stringResource(R.string.after_note_hint), style = UType.Body, color = U.c.faint) },
                textStyle = UType.Body,
                shape = U.RMd,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = U.c.moss, unfocusedBorderColor = U.c.line, cursorColor = U.c.moss),
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(R.string.after_offering))
            Text(stringResource(R.string.after_offering_sub), style = UType.Muted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Offering.entries.forEach { o ->
                    val have = o in woods.gathered
                    val on = o == offering
                    Column(
                        Modifier.weight(1f).clip(U.RMd).background(if (on) U.c.mossSoft else U.c.surface).border(1.dp, if (on) U.c.moss.copy(alpha = 0.6f) else U.c.line, U.RMd)
                            .then(if (have) Modifier.clickable { offering = if (on) null else o } else Modifier).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            if (have) PixImage(Things.offering(o), 30.dp) else Text("?", style = UType.Strong.copy(color = U.c.faint))
                        }
                        Text(if (have) Words.offering(o).of(ru) else stringResource(R.string.n_min, o.minutes), style = UType.Small.copy(color = if (have) U.c.ink else U.c.faint), maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(stringResource(R.string.done), ::done, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))
    }
}

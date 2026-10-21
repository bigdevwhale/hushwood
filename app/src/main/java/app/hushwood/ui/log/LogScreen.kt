package app.hushwood.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.hushwood.R
import app.hushwood.container
import app.hushwood.data.Session
import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Setting
import app.hushwood.domain.Visits
import app.hushwood.ui.Go
import app.hushwood.ui.Woods
import app.hushwood.ui.components.Chip
import app.hushwood.ui.components.PrimaryButton
import app.hushwood.ui.components.SectionTitle
import app.hushwood.ui.components.TopBar
import app.hushwood.ui.ctx
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * A practice done away from the phone (a class, a walk, a page in a notebook), written down afterwards.
 * Fewer animals come to these: nobody was there to sit still, but the place remembers.
 */
@Composable
fun LogScreen(woods: Woods, go: Go) {
    val c = ctx()
    var pid by remember { mutableLongStateOf(woods.active.firstOrNull { it.id == woods.settings.lastPractice }?.id ?: woods.active.firstOrNull()?.id ?: 0L) }
    var minutes by remember { mutableIntStateOf(20) }
    var daysAgo by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }

    fun save() {
        val practice = woods.practice(pid) ?: return
        val end = LocalDateTime.now(woods.zone).minusDays(daysAgo.toLong())
        val start = end.minusMinutes(minutes.toLong())
        val startMs = start.atZone(woods.zone).toInstant().toEpochMilli()
        val came = Visits.afterwards(Setting(woods.place, practice.kindOf.spot, Daypart.of(start), start.toLocalDate(), woods.settings.offering, seed = startMs), minutes, woods.guardian)
        val session = Session(
            practiceId = pid, kind = practice.kind, place = woods.place.name, start = startMs, end = end.atZone(woods.zone).toInstant().toEpochMilli(),
            minutes = minutes, beasts = Beast.csv(came), note = note.trim(), manual = true,
        )
        val container = c.container
        container.scope.launch {
            val id = container.db.dao().insert(session)
            container.settings.update { it.copy(lastPractice = pid) }
            container.changed()
            withContext(Dispatchers.Main) { go.after(id) }
        }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.log_title), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.log_sub), style = UType.Muted)
            SectionTitle(stringResource(R.string.setup_practice))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                woods.active.forEach { p -> Chip(p.name, p.id == pid, { pid = p.id }) }
            }
            SectionTitle(stringResource(R.string.setup_length))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20, 30, 45, 60, 90).forEach { m -> Chip(stringResource(R.string.n_min, m), m == minutes, { minutes = m }) }
            }
            SectionTitle(stringResource(R.string.log_when))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.today, R.string.yesterday, R.string.two_days_ago).forEachIndexed { i, r -> Chip(stringResource(r), i == daysAgo, { daysAgo = i }) }
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
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(stringResource(R.string.log_save), ::save, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), enabled = pid != 0L)
    }
}

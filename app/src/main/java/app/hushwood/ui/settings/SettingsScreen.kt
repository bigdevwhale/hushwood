package app.hushwood.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsNone
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.hushwood.BuildConfig
import app.hushwood.R
import app.hushwood.container
import app.hushwood.data.Practice
import app.hushwood.data.Settings
import app.hushwood.domain.Breath
import app.hushwood.domain.Kind
import app.hushwood.ui.Go
import app.hushwood.ui.Language
import app.hushwood.ui.T
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.Chip
import app.hushwood.ui.components.InfoRow
import app.hushwood.ui.components.SectionTitle
import app.hushwood.ui.components.SoftButton
import app.hushwood.ui.components.Toggle
import app.hushwood.ui.components.TopBar
import app.hushwood.ui.components.UCard
import app.hushwood.ui.ctx
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType

@Composable
fun SettingsScreen(woods: Woods, go: Go) {
    val c = ctx()
    val ru = woods.ru
    val s = woods.settings
    fun set(change: (Settings) -> Settings) = c.container.write { settings.update(change) }
    var editing by remember { mutableStateOf<Practice?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SectionTitle(stringResource(R.string.set_practices))
            UCard {
                woods.active.forEach { p ->
                    Row(Modifier.fillMaxWidth().clickable { editing = p }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = UType.Strong)
                            Text(Words.kind(p.kindOf).of(ru) + " · " + Words.spot(woods.place, p.kindOf.spot).of(ru), style = UType.Muted)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                SoftButton(stringResource(R.string.set_add_practice), { adding = true }, icon = Icons.Rounded.Add)
            }

            SectionTitle(stringResource(R.string.set_sitting))
            UCard {
                Text(stringResource(R.string.setup_rhythm), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Breath.entries.forEach { b -> Chip(Words.breath(b).of(ru), b == s.breath, { set { it.copy(breath = b) } }) }
                }
                Spacer(Modifier.height(10.dp))
                Toggle(stringResource(R.string.set_sound), stringResource(R.string.set_sound_sub), s.sound) { v -> set { it.copy(sound = v) } }
                Toggle(stringResource(R.string.set_bell), stringResource(R.string.set_bell_sub), s.bell) { v -> set { it.copy(bell = v) } }
                Toggle(stringResource(R.string.set_dim), stringResource(R.string.set_dim_sub), s.dim) { v -> set { it.copy(dim = v) } }
                Toggle(stringResource(R.string.set_haptics), "", s.haptics) { v -> set { it.copy(haptics = v) } }
            }

            SectionTitle(stringResource(R.string.set_remind))
            UCard {
                Toggle(stringResource(R.string.set_remind_on), stringResource(R.string.set_remind_sub), s.remind) { v -> set { it.copy(remind = v) } }
                if (s.remind) InfoRow(Icons.Rounded.NotificationsNone, stringResource(R.string.set_remind_at), T.time(s.remindAt)) {
                    TimePickerDialog(c, { _, h, m -> set { it.copy(remindAt = h * 60 + m) } }, s.remindAt / 60, s.remindAt % 60, true).show()
                }
            }

            SectionTitle(stringResource(R.string.set_language))
            UCard {
                val cur = Language.current()
                InfoRow(Icons.Rounded.Language, stringResource(R.string.set_language), when (cur) { Language.RU -> "Русский"; Language.EN -> "English"; else -> stringResource(R.string.lang_system) })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                    Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                    Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
                }
            }

            SectionTitle(stringResource(R.string.set_about))
            Text(stringResource(R.string.set_about_text), style = UType.Muted)
            Spacer(Modifier.height(6.dp))
            Text("Hushwood ${BuildConfig.VERSION_NAME} · Stillcraft", style = UType.Small)
            Spacer(Modifier.height(28.dp))
        }
    }

    editing?.let { p ->
        PracticeSheet(p.name, p.kindOf, ru, canArchive = woods.active.size > 1, onDismiss = { editing = null }, onArchive = {
            editing = null
            c.container.write { db.dao().update(p.copy(archived = true)) }
        }) { name, kind ->
            editing = null
            c.container.write { db.dao().update(p.copy(name = name, kind = kind.name)) }
        }
    }
    if (adding) PracticeSheet("", Kind.SIT, ru, canArchive = false, onDismiss = { adding = false }, onArchive = {}) { name, kind ->
        adding = false
        c.container.write { db.dao().insert(Practice(name = name, kind = kind.name, order = woods.practices.size)) }
    }
}

/** A practice's name and what kind it is (which decides where in the place it's done). */
@Composable
private fun PracticeSheet(name0: String, kind0: Kind, ru: Boolean, canArchive: Boolean, onDismiss: () -> Unit, onArchive: () -> Unit, onSave: (String, Kind) -> Unit) {
    var name by remember { mutableStateOf(name0) }
    var kind by remember { mutableStateOf(kind0) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RXl).background(U.c.surface).border(1.dp, U.c.line, U.RXl).padding(20.dp)) {
            Text(stringResource(if (name0.isEmpty()) R.string.set_add_practice else R.string.set_edit_practice), style = UType.Title)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 28) name = it.replace('\n', ' ') },
                placeholder = { Text(stringResource(R.string.set_practice_hint), style = UType.Body, color = U.c.faint) },
                singleLine = true,
                textStyle = UType.Body,
                shape = U.RMd,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = U.c.moss, unfocusedBorderColor = U.c.line, cursorColor = U.c.moss),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Kind.entries.forEach { k -> Chip(Words.kind(k).of(ru), k == kind, { kind = k }) }
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(if (kind.still) R.string.kind_still else R.string.kind_moving), style = UType.Muted)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (canArchive) SoftButton(stringResource(R.string.set_archive), onArchive, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2)
                SoftButton(stringResource(R.string.done), { if (name.isNotBlank()) onSave(name.trim(), kind) }, Modifier.weight(1f), enabled = name.isNotBlank())
            }
        }
    }
}

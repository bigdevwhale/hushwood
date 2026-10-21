package app.hushwood.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.hushwood.R
import app.hushwood.ui.theme.Sans
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType
import kotlinx.coroutines.delay

@Composable
fun UCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(U.c.surface)
            .border(1.dp, U.c.line, U.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = UType.Eyebrow, modifier = modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp))
}

/** The bright one, for the thing to do next. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = U.c.moss, icon: ImageVector? = null, content: Color = U.c.onMoss) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = U.RLg,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = content, disabledContainerColor = U.c.line, disabledContentColor = U.c.faint),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button.copy(fontSize = 17.sp))
    }
}

/** Quiet, for the second thing. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.moss, soft: Color = U.c.mossSoft, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier
            .height(50.dp)
            .clip(U.RMd)
            .background(if (enabled) soft else U.c.surface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) color else U.c.faint
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = U.c.moss) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp) },
        shape = U.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = U.c.surface,
            labelColor = U.c.muted,
            selectedContainerColor = color.copy(alpha = 0.18f),
            selectedLabelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = U.c.line, selectedBorderColor = color.copy(alpha = 0.55f)),
    )
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(U.RSm).background(U.c.surface2).border(1.dp, U.c.line, U.RSm).padding(4.dp)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(U.RSm)
                    .background(if (on) U.c.mossSoft else Color.Transparent)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) { Text(label(o), style = UType.Button.copy(fontSize = 15.sp), color = if (on) U.c.moss else U.c.muted, maxLines = 1) }
        }
    }
}

@Composable
fun IconDisc(icon: ImageVector, size: Dp = 44.dp, color: Color = U.c.moss, soft: Color = U.c.mossSoft) {
    Box(Modifier.size(size).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = color)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = U.c.ink) }
        } else Spacer(Modifier.width(14.dp))
        Text(title, style = UType.Title.copy(fontSize = 21.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = UType.Muted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = UType.Strong)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

@Composable
fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            if (sub.isNotEmpty()) Text(sub, style = UType.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = U.c.moss, checkedThumbColor = U.c.onMoss, uncheckedTrackColor = U.c.surface2, uncheckedBorderColor = U.c.line, uncheckedThumbColor = U.c.faint))
    }
}

/** Wall clock that ticks every [stepMs] while the screen is visible. */
@Composable
fun rememberNow(stepMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { visible = true; now = System.currentTimeMillis() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { visible = false }
    LaunchedEffect(visible, stepMs) {
        while (visible) {
            now = System.currentTimeMillis()
            delay(stepMs - now % stepMs)
        }
    }
    return now
}

/** A small card with one question, a line of text, and "Done". */
@Composable
fun NameSheet(title: String, hint: String, onDone: (String) -> Unit, onDismiss: () -> Unit, initial: String = "", max: Int = 40) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RXl).background(U.c.surface).border(1.dp, U.c.line, U.RXl).padding(20.dp)) {
            Text(title, style = UType.Title)
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= max) text = it.replace('\n', ' ') },
                placeholder = { Text(hint, style = UType.Body, color = U.c.faint) },
                singleLine = true,
                textStyle = UType.Body,
                shape = U.RMd,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { if (text.isNotBlank()) onDone(text.trim()) }),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = U.c.moss, unfocusedBorderColor = U.c.line, cursorColor = U.c.moss),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2)
                SoftButton(stringResource(R.string.done), { if (text.isNotBlank()) onDone(text.trim()) }, Modifier.weight(1f), enabled = text.isNotBlank())
            }
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** "Delete?" for a long press, quiet and small. */
@Composable
fun ConfirmSheet(title: String, sub: String, yes: String, onYes: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RXl).background(U.c.surface).border(1.dp, U.c.line, U.RXl).padding(20.dp)) {
            Text(title, style = UType.Title)
            Spacer(Modifier.height(6.dp))
            Text(sub, style = UType.Muted)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), color = U.c.muted, soft = U.c.surface2)
                SoftButton(yes, onYes, Modifier.weight(1f))
            }
        }
    }
}

/** A round button with a label under it. */
@Composable
fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, lit: Boolean = false) {
    Column(modifier.clip(U.RMd).clickable(onClick = onClick).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(50.dp).clip(CircleShape).background(if (lit) U.c.mossSoft else U.c.surface.copy(alpha = 0.85f)).border(1.dp, if (lit) U.c.moss.copy(alpha = 0.5f) else U.c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(22.dp), tint = if (lit) U.c.moss else U.c.ink) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = UType.Small.copy(color = U.c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The three tabs along the bottom: the place, the journal, the guardian. */
@Composable
fun TabBar(tab: app.hushwood.ui.Tab, onSelect: (app.hushwood.ui.Tab) -> Unit) {
    val items = listOf(
        Triple(app.hushwood.ui.Tab.PLACE, Icons.Rounded.Forest, R.string.tab_place),
        Triple(app.hushwood.ui.Tab.JOURNAL, Icons.AutoMirrored.Rounded.MenuBook, R.string.tab_journal),
        Triple(app.hushwood.ui.Tab.KEEPER, Icons.Rounded.Pets, R.string.tab_keeper),
    )
    Column(Modifier.fillMaxWidth().background(U.c.surface)) {
        androidx.compose.material3.HorizontalDivider(color = U.c.line)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((t, icon, label) in items) {
                val on = t == tab
                Column(
                    Modifier.weight(1f).clip(U.RMd).clickable { onSelect(t) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(width = 52.dp, height = 28.dp).clip(U.RSm).background(if (on) U.c.mossSoft else Color.Transparent), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(20.dp), tint = if (on) U.c.moss else U.c.muted)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(stringResource(label), style = UType.Small.copy(color = if (on) U.c.ink else U.c.muted))
                }
            }
        }
    }
}


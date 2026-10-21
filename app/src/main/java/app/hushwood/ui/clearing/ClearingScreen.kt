package app.hushwood.ui.clearing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.hushwood.R
import app.hushwood.container
import app.hushwood.data.Sitting
import app.hushwood.domain.Breath
import app.hushwood.domain.Kind
import app.hushwood.domain.Place
import app.hushwood.ui.Go
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.BeastIcon
import app.hushwood.ui.components.Chip
import app.hushwood.ui.components.PixImage
import app.hushwood.ui.components.PrimaryButton
import app.hushwood.ui.components.SoftButton
import app.hushwood.ui.ctx
import app.hushwood.ui.pix.Things
import app.hushwood.ui.scene.SceneSpec
import app.hushwood.ui.scene.SceneView
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType

/** The place as it is right now, who's out in it, and the way to sit down. */
@Composable
fun ClearingScreen(woods: Woods, go: Go) {
    val c = ctx()
    val ru = woods.ru
    var setup by remember { mutableStateOf(false) }
    var places by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            SceneView(
                SceneSpec(woods.place, woods.part, woods.season, woods.idle, woods.guardian, woods.trust, offering = woods.settings.offering),
                Modifier.fillMaxSize(),
            )
            // A soft fade from the picture into the panel below.
            Box(Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, U.c.bg))))
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(U.RMd).background(U.c.bg.copy(alpha = 0.72f)).clickable { places = true }.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Words.place(woods.place).of(ru), style = UType.Strong)
                    Icon(Icons.Rounded.ExpandMore, null, tint = U.c.muted)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(CircleShape).background(U.c.bg.copy(alpha = 0.72f))) {
                    IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.ink) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp)) {
            val line = woods.guardian?.let { g -> Words.name(g).of(ru) + " " + Words.trust(woods.trust!!).of(ru) + "." }
                ?: Words.idle[(woods.today.dayOfYear + woods.part.ordinal) % Words.idle.size].of(ru)
            Text(line, style = UType.Line)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(woods.todayMinutes.toString(), style = UType.Number)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.min_today), style = UType.Muted, modifier = Modifier.padding(bottom = 5.dp))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    woods.todayBeasts.take(5).forEach { BeastIcon(it, 30.dp, animate = false) }
                }
                woods.settings.offering?.let {
                    Spacer(Modifier.width(8.dp))
                    PixImage(Things.offering(it), 26.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (woods.settings.sitting != null) {
                    PrimaryButton(stringResource(R.string.back_to_sitting), go::sit, Modifier.weight(1f), icon = Icons.Rounded.SelfImprovement)
                } else {
                    PrimaryButton(stringResource(R.string.sit_quietly), { setup = true }, Modifier.weight(1f), icon = Icons.Rounded.SelfImprovement)
                    SoftButton(stringResource(R.string.write_down), go::log, icon = Icons.Rounded.EditNote)
                }
            }
        }
    }
    if (setup) SetupSheet(woods, onDismiss = { setup = false }) { practiceId, length, breath ->
        setup = false
        val container = c.container
        container.write {
            settings.update { it.copy(sitting = Sitting(practiceId, System.currentTimeMillis(), length), length = length, breath = breath, lastPractice = practiceId) }
        }
        go.sit()
    }
    if (places) PlaceSheet(woods, onDismiss = { places = false }) { p ->
        places = false
        c.container.write { settings.update { it.copy(place = p) } }
    }
}

/** Before sitting down: which practice, for how long, and (for breathing) which rhythm. */
@Composable
private fun SetupSheet(woods: Woods, onDismiss: () -> Unit, onStart: (Long, Int, Breath) -> Unit) {
    val ru = woods.ru
    val list = woods.active
    var pid by remember { mutableStateOf(list.firstOrNull { it.id == woods.settings.lastPractice }?.id ?: list.firstOrNull()?.id ?: 0L) }
    var length by remember { mutableStateOf(woods.settings.length) }
    var breath by remember { mutableStateOf(woods.settings.breath) }
    val kind = woods.practice(pid)?.kindOf ?: Kind.SIT
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RXl).background(U.c.surface).border(1.dp, U.c.line, U.RXl).padding(20.dp)) {
            Text(stringResource(R.string.setup_title), style = UType.Title)
            Spacer(Modifier.height(4.dp))
            Text(Words.place(woods.place).of(ru) + ", " + Words.spot(woods.place, kind.spot).of(ru), style = UType.Muted)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.setup_practice).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { p -> Chip(p.name, p.id == pid, { pid = p.id }) }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.setup_length).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 5, 10, 15, 20, 30, 45, 60).forEach { m ->
                    Chip(if (m == 0) "∞" else stringResource(R.string.n_min, m), m == length, { length = m })
                }
            }
            if (kind == Kind.BREATH) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.setup_rhythm).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Breath.entries.forEach { b -> Chip(Words.breath(b).of(ru), b == breath, { breath = b }) }
                }
            }
            if (!kind.still) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.setup_moving), style = UType.Muted)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(stringResource(R.string.setup_start), { if (pid != 0L) onStart(pid, length, breath) }, Modifier.fillMaxWidth(), enabled = pid != 0L)
        }
    }
}

/** Where to sit: the places open so far, and how many minutes until the next one opens. */
@Composable
private fun PlaceSheet(woods: Woods, onDismiss: () -> Unit, onPick: (Place) -> Unit) {
    val ru = woods.ru
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RXl).background(U.c.surface).border(1.dp, U.c.line, U.RXl).padding(16.dp)) {
            Text(stringResource(R.string.places_title), style = UType.Title, modifier = Modifier.padding(4.dp))
            Spacer(Modifier.height(8.dp))
            Place.entries.forEach { p ->
                val open = p in woods.open
                val here = p == woods.place
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(U.RMd)
                        .background(if (here) U.c.mossSoft else U.c.surface2)
                        .then(if (open) Modifier.clickable { onPick(p) } else Modifier)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(width = 74.dp, height = 56.dp).clip(U.RSm)) {
                        SceneView(SceneSpec(p, woods.part, woods.season), Modifier.fillMaxSize())
                        if (!open) Box(Modifier.fillMaxSize().background(U.c.bg.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Lock, null, tint = U.c.muted)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(Words.place(p).of(ru), style = UType.Strong.copy(color = if (open) U.c.ink else U.c.muted))
                        Text(
                            if (open) Words.placeLine(p).of(ru) else stringResource(R.string.place_opens, p.minutes - woods.totalMinutes),
                            style = UType.Muted.copy(fontSize = 13.sp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

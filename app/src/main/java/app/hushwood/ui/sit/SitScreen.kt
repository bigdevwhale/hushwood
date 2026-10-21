package app.hushwood.ui.sit

import android.app.Activity
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hushwood.R
import app.hushwood.container
import app.hushwood.data.Session
import app.hushwood.domain.Beast
import app.hushwood.domain.Breath
import app.hushwood.domain.Daypart
import app.hushwood.domain.Gift
import app.hushwood.domain.Kind
import app.hushwood.domain.Quiet
import app.hushwood.domain.Setting
import app.hushwood.domain.Trust
import app.hushwood.domain.Visits
import app.hushwood.sit.Sound
import app.hushwood.sit.rememberFeel
import app.hushwood.ui.Go
import app.hushwood.ui.T
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.ConfirmSheet
import app.hushwood.ui.components.SoftButton
import app.hushwood.ui.ctx
import app.hushwood.ui.scene.Art
import app.hushwood.ui.scene.SceneSpec
import app.hushwood.ui.scene.SceneView
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType
import app.hushwood.ui.tick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * The sitting itself: the place fills the screen, you sit at your spot, and the animals come out as the quiet
 * builds. A jolt of the phone sends them back into the trees. The bell rings when the time is up.
 */
@Composable
fun SitScreen(woods: Woods, go: Go) {
    val c = ctx()
    val view = LocalView.current
    val ru = woods.ru
    val s = woods.settings
    val sitting = s.sitting
    if (sitting == null) {
        LaunchedEffect(Unit) { go.home() }
        return
    }
    val practice = woods.practice(sitting.practiceId)
    val kind = practice?.kindOf ?: Kind.SIT
    val spot = kind.spot
    val begun = LocalDateTime.ofInstant(Instant.ofEpochMilli(sitting.start), woods.zone)
    val trust = woods.trust
    val setting = remember(sitting.start, woods.place) {
        Setting(
            place = woods.place, spot = spot, part = Daypart.of(begun), date = begun.toLocalDate(), offering = s.offering,
            kingfisher = woods.kingfisher, bold = trust != null && trust >= Trust.NEAR, seed = sitting.start,
        )
    }
    val quiet = remember(sitting.start, woods.place) {
        // A picture of a sitting already under way, for the screenshots.
        Quiet(Visits.arrivals(setting, woods.guardian), moving = !kind.still).also { if (Art.still) it.tick(9f) }
    }
    var here by remember { mutableStateOf(quiet.here) }
    var calm by remember { mutableStateOf(quiet.calm) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var done by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    val feel = rememberFeel {
        quiet.startle()
        here = quiet.here
        calm = quiet.calm
    }

    fun finish(save: Boolean) {
        if (done) return
        done = true
        Sound.stop()
        val container = c.container
        if (!save) {
            container.write { settings.update { it.copy(sitting = null) } }
            go.home()
            return
        }
        val end = System.currentTimeMillis()
        val minutes = ((end - sitting.start) / 60_000f).roundToInt().coerceAtLeast(1)
        if (s.bell) Sound.bell()
        if (s.haptics) runCatching {
            c.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 120, 60), -1))
        }
        val session = Session(
            practiceId = sitting.practiceId, kind = kind.name, place = woods.place.name, start = sitting.start, end = end, minutes = minutes,
            still = quiet.longest.toInt(), startles = quiet.startles, beasts = Beast.csv(quiet.seen),
            gift = Gift.after(trust ?: Trust.WATCHING, sitting.start)?.name ?: "",
        )
        container.scope.launch {
            val id = container.db.dao().insert(session)
            container.settings.update { it.copy(sitting = null) }
            container.changed()
            withContext(Dispatchers.Main) { go.after(id) }
        }
    }

    // The clock: quiet gathers second by second, and the bell rings when the time is up.
    LaunchedEffect(sitting.start) {
        var last = System.currentTimeMillis()
        while (!done) {
            delay(1000)
            val n = System.currentTimeMillis()
            quiet.tick((n - last) / 60_000f)
            last = n
            now = n
            here = quiet.here
            calm = quiet.calm
            if (sitting.length > 0 && n - sitting.start >= sitting.length * 60_000L) finish(true)
        }
    }

    // The forest's sounds while you sit.
    LaunchedEffect(s.sound, woods.place) {
        if (s.sound && !Art.still) Sound.ambience(c, Sound.Mood(woods.place, woods.part, woods.season, spot == app.hushwood.domain.Spot.FIRE))
        else Sound.stop()
    }
    DisposableEffect(Unit) { onDispose { Sound.stop() } }

    // Keep the screen awake, and almost dark while the phone lies face down.
    val activity = c as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
        }
    }
    val dark = s.dim && feel.faceDown
    LaunchedEffect(dark) {
        activity?.window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = if (dark) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
    }

    // Breathing: the rhythm, with a tick under the finger as each phase turns.
    val rhythm = if (kind == Kind.BREATH) s.breath else null
    val fill: (() -> Float)? = rhythm?.let { b -> { b.at((System.currentTimeMillis() - sitting.start) / 1000f).fill } }
    val phase = rhythm?.at((now - sitting.start) / 1000f)
    LaunchedEffect(phase?.phase) { if (phase != null && now - sitting.start > 1500) view.tick(s.haptics) }

    BackHandler { go.home() }

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
      Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
        SceneView(
            SceneSpec(woods.place, woods.part, woods.season, here, woods.guardian, trust, spot, sitting = true, breath = fill, offering = s.offering),
            Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxWidth().height(160.dp).background(Brush.verticalGradient(listOf(U.c.bg.copy(alpha = 0.7f), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, U.c.bg))))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = go::home) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = U.c.ink) }
                Column(Modifier.weight(1f)) {
                    Text(practice?.name ?: "", style = UType.Strong)
                    Text(Words.place(woods.place).of(ru) + ", " + Words.spot(woods.place, spot).of(ru), style = UType.Muted)
                }
                // The place's sounds, on and off right here.
                IconButton(onClick = { c.container.write { settings.update { it.copy(sound = !it.sound) } } }) {
                    Icon(
                        if (s.sound) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                        stringResource(if (s.sound) R.string.sound_off else R.string.sound_on),
                        tint = if (s.sound) U.c.ink else U.c.muted,
                    )
                }
            }
            val secs = (now - sitting.start) / 1000
            val shown = if (sitting.length > 0) (sitting.length * 60L - secs).coerceAtLeast(0) else secs
            Text(T.clock(shown), style = UType.Number.copy(fontSize = 44.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (phase != null && rhythm != null) {
                Text(Words.phase(phase.phase).of(ru), style = UType.Title.copy(fontSize = 26.sp))
                Text(phase.left.toString(), style = UType.Number.copy(fontSize = 20.sp, color = U.c.muted))
                Spacer(Modifier.height(8.dp))
            }
            val moved = now - feel.movedAt < 6_000 && feel.movedAt > 0
            // Always two lines tall, so the picture above never changes size when the line does.
            Text(Words.calm(calm, moved && kind.still).of(ru), style = UType.Line, textAlign = TextAlign.Center, minLines = 2, maxLines = 2)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.stand_up), {
                    if (now - sitting.start < 60_000) leaving = true else finish(true)
                }, icon = Icons.Rounded.Check)
            }
        }
      }
        // Almost dark while the phone lies face down; the picture lives on underneath.
        if (dark) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)))
    }
    if (leaving) ConfirmSheet(
        stringResource(R.string.leave_title), stringResource(R.string.leave_sub), stringResource(R.string.leave_yes),
        onYes = { leaving = false; finish(false) }, onDismiss = { leaving = false },
    )
}

package app.hushwood.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hushwood.R
import app.hushwood.container
import app.hushwood.data.Practice
import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Place
import app.hushwood.ui.Words
import app.hushwood.ui.Woods
import app.hushwood.ui.components.Chip
import app.hushwood.ui.components.PrimaryButton
import app.hushwood.ui.ctx
import app.hushwood.ui.scene.SceneSpec
import app.hushwood.ui.scene.SceneView
import app.hushwood.ui.theme.U
import app.hushwood.ui.theme.UType

/** The first visit: the clearing at dawn, what happens here, and which practices to bring. */
@Composable
fun WelcomeScreen(woods: Woods) {
    val c = ctx()
    val ru = woods.ru
    var step by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf(setOf(0, 1)) }
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.fillMaxWidth().weight(if (step == 0) 1f else 0.6f)) {
            SceneView(SceneSpec(Place.FOREST, if (step == 0) Daypart.DAWN else woods.part, woods.season, listOf(Beast.TIT, Beast.ROBIN, Beast.HARE, Beast.DEER)), Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, U.c.bg))))
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 24.dp)) {
            if (step == 0) {
                Text("Hushwood", style = UType.Hero)
                Spacer(Modifier.height(4.dp))
                Text(Words.idle[0].of(ru), style = UType.Line)
                Spacer(Modifier.height(16.dp))
                Point(Icons.Rounded.SelfImprovement, stringResource(R.string.welcome_1))
                Point(Icons.Rounded.Pets, stringResource(R.string.welcome_2))
                Point(Icons.Rounded.Forest, stringResource(R.string.welcome_3))
                Spacer(Modifier.height(20.dp))
                PrimaryButton(stringResource(R.string.next), { step = 1 }, Modifier.fillMaxWidth())
            } else {
                Text(stringResource(R.string.welcome_pick), style = UType.Display)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.welcome_pick_sub), style = UType.Muted)
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Words.starters.forEachIndexed { i, (w, _) ->
                        Chip(w.of(ru), i in picked, { picked = if (i in picked) picked - i else picked + i })
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton(stringResource(R.string.welcome_enter), {
                    val chosen = Words.starters.filterIndexed { i, _ -> i in picked }
                    c.container.write {
                        db.dao().insertPractices(chosen.mapIndexed { i, (w, kind) -> Practice(name = w.of(ru), kind = kind.name, order = i) })
                        settings.update { it.copy(onboarded = true) }
                    }
                }, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(22.dp), tint = U.c.moss)
        Spacer(Modifier.width(12.dp))
        Text(text, style = UType.Body, modifier = Modifier.weight(1f))
    }
}

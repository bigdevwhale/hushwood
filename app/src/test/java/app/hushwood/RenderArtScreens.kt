package app.hushwood

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.hushwood.data.DemoSeed
import app.hushwood.data.Sitting
import app.hushwood.domain.Daypart
import app.hushwood.domain.Kind
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.ui.Go
import app.hushwood.ui.Tab
import app.hushwood.ui.Woods
import app.hushwood.ui.clearing.ClearingScreen
import app.hushwood.ui.components.TabBar
import app.hushwood.ui.journal.JournalScreen
import app.hushwood.ui.keeper.KeeperScreen
import app.hushwood.ui.log.LogScreen
import app.hushwood.ui.onboarding.WelcomeScreen
import app.hushwood.ui.scene.Art
import app.hushwood.ui.settings.SettingsScreen
import app.hushwood.ui.sit.AfterScreen
import app.hushwood.ui.sit.SitScreen
import app.hushwood.ui.theme.HushTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo months, drawn to PNGs (run with `-Prender`), English and Russian: a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
        Art.fixedPart = Daypart.DAWN
        Art.fixedSeason = Season.SUMMER
    }

    private fun woods(): Woods = runBlocking {
        val app = ApplicationProvider.getApplicationContext<HushwoodApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c, app.ru)
        val dao = c.db.dao()
        Woods(dao.practices().first(), dao.sessions().first(), c.settings.now(), app.ru)
    }

    private fun shot(name: String, w: Woods, settle: Long = 3_000, content: @Composable (Woods, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            HushTheme { content(w, Go(nav)) }
        }
        rule.mainClock.advanceTimeBy(settle)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(settle)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Composable
    private fun Tabbed(tab: Tab, body: @Composable () -> Unit) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { body() }
            TabBar(tab) {}
        }
    }

    private fun Woods.at(place: Place) = Woods(practices, sessions, settings.copy(place = place), ru)

    @Test fun welcome() = woods().let { shot("0-welcome", it) { w, _ -> WelcomeScreen(w) } }
    @Test fun lake() = woods().let { shot("1-lake", it.at(Place.LAKE)) { w, go -> Tabbed(Tab.PLACE) { ClearingScreen(w, go) } } }
    @Test fun sit() = woods().let { w0 ->
        val breath = w0.practices.first { it.kindOf == Kind.BREATH }
        val w = Woods(w0.practices, w0.sessions, w0.settings.copy(place = Place.FOREST, sitting = Sitting(breath.id, System.currentTimeMillis() - 9 * 60_000, 15)), w0.ru)
        shot("2-sit", w, settle = 6_000) { ww, go -> SitScreen(ww, go) }
    }
    @Test fun after() = woods().let { w -> shot("3-after", w) { ww, go -> AfterScreen(ww.sessions.filter { it.seen.size >= 3 }.maxBy { it.start }.id, ww, go) } }
    @Test fun meetings() = woods().let { shot("4-meetings", it) { w, _ -> Tabbed(Tab.JOURNAL) { JournalScreen(w, 0) } } }
    @Test fun keeper() = woods().let { shot("5-keeper", it) { w, _ -> Tabbed(Tab.KEEPER) { KeeperScreen(w) } } }
    @Test fun rings() = woods().let { shot("6-rings", it) { w, _ -> Tabbed(Tab.JOURNAL) { JournalScreen(w, 2) } } }
    @Test fun herbarium() = woods().let { shot("7-herbarium", it) { w, _ -> Tabbed(Tab.JOURNAL) { JournalScreen(w, 1) } } }
    @Test fun mountains() = woods().let { shot("8-mountains", it.at(Place.MOUNTAINS)) { w, go -> Tabbed(Tab.PLACE) { ClearingScreen(w, go) } } }
    @Test fun sea() = woods().let { shot("9-sea", it.at(Place.SEA)) { w, go -> Tabbed(Tab.PLACE) { ClearingScreen(w, go) } } }
    @Test fun log() = woods().let { shot("10-log", it) { w, go -> LogScreen(w, go) } }
    @Test fun settings() = woods().let { shot("11-settings", it) { w, go -> SettingsScreen(w, go) } }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}

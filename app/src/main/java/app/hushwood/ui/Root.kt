package app.hushwood.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.hushwood.ui.clearing.ClearingScreen
import app.hushwood.ui.components.TabBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.hushwood.BuildConfig
import app.hushwood.container
import app.hushwood.data.DemoSeed
import app.hushwood.data.Practice
import app.hushwood.data.Session
import app.hushwood.data.Settings
import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Keeper
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import app.hushwood.domain.Portrait
import app.hushwood.domain.Season
import app.hushwood.domain.Trust
import app.hushwood.domain.Setting
import app.hushwood.domain.Spot
import app.hushwood.domain.Visits
import app.hushwood.ru
import app.hushwood.ui.components.rememberNow
import app.hushwood.ui.journal.JournalScreen
import app.hushwood.ui.keeper.KeeperScreen
import app.hushwood.ui.log.LogScreen
import app.hushwood.ui.onboarding.WelcomeScreen
import app.hushwood.ui.scene.Art
import app.hushwood.ui.settings.SettingsScreen
import app.hushwood.ui.sit.AfterScreen
import app.hushwood.ui.sit.SitScreen
import app.hushwood.ui.theme.U
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Everything stored, plus what is worked out from it. Screens read through here. */
@Immutable
class Woods(
    val practices: List<Practice>,
    val sessions: List<Session>,
    val settings: Settings,
    val ru: Boolean,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)
    val part: Daypart = Art.fixedPart ?: Daypart.of(LocalDateTime.now(zone))
    val season: Season = Art.fixedSeason ?: Season.of(today)

    val active: List<Practice> = practices.filter { !it.archived }
    fun practice(id: Long) = practices.firstOrNull { it.id == id }

    val totalMinutes: Int = sessions.sumOf { it.minutes }
    val open: List<Place> = Place.open(totalMinutes)
    val place: Place = settings.place.takeIf { it in open } ?: Place.FOREST

    val portraits: List<Portrait> = Keeper.portraits(sessions, zone)
    val portrait: Portrait? = portraits.lastOrNull()
    val guardian: Beast? = portrait?.beast
    val trust: Trust? = guardian?.let { Keeper.trust(sessions, today, zone) }
    val toReveal: Pair<Int, Int> = Keeper.toReveal(sessions, today, zone)
    val kingfisher: Boolean = Keeper.kingfisher(sessions, today, zone)

    /** How many sittings each animal came to, and the first day it did. */
    val seen: Map<Beast, Int> = sessions.flatMap { it.seen }.groupingBy { it }.eachCount()
    val firstSeen: Map<Beast, LocalDate> = buildMap { for (s in sessions) for (b in s.seen) if (b !in this) put(b, s.day(zone)) }

    val todays: List<Session> = sessions.filter { it.day(zone) == today }
    val todayMinutes: Int = todays.sumOf { it.minutes }
    val todayBeasts: List<Beast> = todays.flatMap { it.seen }.distinct()

    val gathered: List<Offering> = Offering.gathered(totalMinutes)
    val gifts = sessions.filter { it.gift.isNotEmpty() }

    /** Everyone out when nobody sits: the boldest one or two who are always about, and those who came today, if it's their hour. */
    val idle: List<Beast> = (
        Visits.arrivals(Setting(place, Spot.STUMP, part, today, settings.offering, seed = today.toEpochDay())).filter { !it.beast.rare && it.at < 1.2f }.take(2).map { it.beast } +
            todayBeasts.filter { it.place == place && part in it.hours && season in it.seasons }
        ).distinct().take(4)
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun sit() = nav.navigate("sit") { launchSingleTop = true }
    fun after(id: Long) = nav.navigate("after/$id") { popUpTo("main") }
    fun log() = nav.navigate("log") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun back() { nav.popBackStack() }
    fun home() { nav.popBackStack("main", false) }
}

@Composable
fun HushRoot(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val dao = container.db.dao()
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val practices by dao.practices().collectAsStateWithLifecycle(initialValue = null)
    val sessions by dao.sessions().collectAsStateWithLifecycle(initialValue = null)
    // The hour rolls on: dawn becomes day while the app is open.
    val tick = rememberNow(60_000) / 600_000
    val ru = context.ru
    val woods = remember(practices, sessions, settings, tick, ru) { Woods(practices.orEmpty(), sessions.orEmpty(), settings, ru) }
    val go = remember(nav) { Go(nav) }

    LaunchedEffect(ru) { if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container, ru) }

    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }
    val open by container.open.collectAsStateWithLifecycle()
    LaunchedEffect(open, settings.onboarded) {
        if (open == null || !settings.onboarded) return@LaunchedEffect
        container.open.value = null
        nav.popBackStack("main", false)
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.remind) {
        if (settings.remind && Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (practices == null || sessions == null) {
        Box(Modifier.fillMaxSize().background(U.c.bg))
        return
    }
    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(320)) },
            popExitTransition = { fadeOut(tween(200)) },
        ) {
            composable("welcome") { WelcomeScreen(woods) }
            composable("main") { MainScreen(woods, go) }
            composable("sit") { SitScreen(woods, go) }
            composable("after/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                AfterScreen(e.arguments?.getLong("id") ?: 0, woods, go)
            }
            composable("log") { LogScreen(woods, go) }
            composable("settings") { SettingsScreen(woods, go) }
        }
        // A sitting left running comes back to the front when the app opens (once the graph is in place).
        LaunchedEffect(Unit) { if (settings.onboarded && settings.sitting != null) go.sit() }
    }
}

enum class Tab { PLACE, JOURNAL, KEEPER }

@Composable
fun MainScreen(woods: Woods, go: Go) {
    var tab by rememberSaveable { mutableStateOf(Tab.PLACE) }
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.PLACE -> ClearingScreen(woods, go)
                Tab.JOURNAL -> JournalScreen(woods)
                Tab.KEEPER -> KeeperScreen(woods)
            }
        }
        TabBar(tab) { tab = it }
    }
}

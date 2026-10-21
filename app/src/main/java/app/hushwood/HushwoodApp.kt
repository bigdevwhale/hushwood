package app.hushwood

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.hushwood.data.DemoSeed
import app.hushwood.data.HushDatabase
import app.hushwood.data.SettingsStore
import app.hushwood.notify.Remind
import app.hushwood.widget.ClearingWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = HushDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where a tap from outside wants to go ("sit" from the reminder) and when. */
    val open = MutableStateFlow<Pair<String, Long>?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch {
            block()
            changed()
        }
    }

    /** Re-arm the reminder and redraw the widget. */
    suspend fun changed() {
        Remind.reschedule(context)
        ClearingWidget.refresh(context)
    }
}

class HushwoodApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Remind.ensureChannel(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container, ru)
            // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as HushwoodApp).container

/** Resources in the in-app language, for notifications and the widget, which AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"

package app.hushwood

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hushwood.data.Settings
import app.hushwood.ui.HushRoot
import app.hushwood.ui.theme.HushTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always dark bars: the pictures carry the colour.
        val style = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (savedInstanceState == null) opened(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            HushTheme {
                // Wait for settings so the welcome screens don't flash for someone already here.
                settings?.let { HushRoot(it) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened(intent)
    }

    /** A tap on the reminder or the widget: back to the place. */
    private fun opened(intent: Intent?) {
        val open = intent?.getStringExtra(EXTRA_OPEN) ?: return
        container.open.value = open to System.currentTimeMillis()
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_PLACE = "place"
    }
}

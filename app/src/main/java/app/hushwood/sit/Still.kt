package app.hushwood.sit

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

/** What the phone feels: whether it lies face down, and whether it was just moved. */
class Feel {
    var faceDown by mutableStateOf(false)
        internal set

    /** When the last movement was felt (epoch ms), 0 if never. */
    var movedAt by mutableStateOf(0L)
        internal set
}

/**
 * Listens to the accelerometer while a sitting is on. A jolt (the phone picked up, bumped, carried) calls
 * [onStartle], at most once every few seconds; the slow drift of a breathing body doesn't.
 */
@Composable
fun rememberFeel(onStartle: () -> Unit): Feel {
    val context = LocalContext.current
    val feel = remember { Feel() }
    val startle by rememberUpdatedState(onStartle)
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            private var px = 0f
            private var py = 0f
            private var pz = 0f
            private var primed = false
            private var motion = 0f
            private var over = 0

            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]
                val y = e.values[1]
                val z = e.values[2]
                // Face down, with a little hysteresis so a wobble doesn't flicker it.
                feel.faceDown = if (feel.faceDown) z < -6.5f else z < -8.3f
                if (primed) {
                    val dx = x - px
                    val dy = y - py
                    val dz = z - pz
                    motion = motion * 0.8f + sqrt(dx * dx + dy * dy + dz * dz) * 0.2f
                    // A real jolt (picked up, bumped, carried), not the sensor's own jitter or a table's hum.
                    over = if (motion > 1.1f) over + 1 else 0
                    val now = System.currentTimeMillis()
                    if (over >= 4 && now - feel.movedAt > 4_000) {
                        feel.movedAt = now
                        startle()
                    }
                }
                px = x; py = y; pz = z; primed = true
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return feel
}

package app.hushwood.sit

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import app.hushwood.R
import app.hushwood.domain.Daypart
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The places' sounds: real field recordings (birds, wind, a stream, waves, crickets, a fire; see CREDITS.md)
 * looped and layered in a mix of its own for each place and hour, faded in and out. The bell is synthesised.
 */
object Sound {
    private const val RATE = 22_050
    private const val FADE_MS = 2_500L

    /** What's playing: the place, the hour, the season and whether the fire is lit. */
    data class Mood(val place: Place, val part: Daypart, val season: Season, val fire: Boolean)

    private class Layer(val player: MediaPlayer, val volume: Float)

    @Volatile private var playing: Mood? = null
    private val layers = ArrayList<Layer>()
    private var fader: Thread? = null

    /** The recordings for [m], each with its volume. */
    fun mix(m: Mood): List<Pair<Int, Float>> {
        val night = m.part == Daypart.NIGHT
        val winter = m.season == Season.WINTER
        val warm = m.season == Season.SPRING || m.season == Season.SUMMER
        val out = ArrayList<Pair<Int, Float>>()
        val wind = when (m.place) { Place.MOUNTAINS -> 0.6f; Place.SEA -> 0.25f; else -> 0.22f } + if (winter) 0.2f else 0f
        out += R.raw.ambient_wind to wind
        if (!night && !winter) when (m.place) {
            Place.FOREST -> out += R.raw.ambient_birds to 0.55f
            Place.LAKE -> out += R.raw.ambient_birds to 0.4f
            Place.MOUNTAINS -> out += R.raw.ambient_birds to 0.22f
            Place.SEA -> {}
        }
        when (m.place) {
            Place.FOREST -> out += R.raw.ambient_stream to 0.16f
            Place.LAKE -> out += R.raw.ambient_stream to 0.12f
            Place.MOUNTAINS -> out += R.raw.ambient_stream to 0.38f
            Place.SEA -> out += R.raw.ambient_waves to 0.75f
        }
        if (warm && (night || m.part == Daypart.DUSK)) out += R.raw.ambient_night to if (m.place == Place.SEA) 0.2f else 0.45f
        if (m.fire) out += R.raw.ambient_fire to 0.6f
        return out
    }

    @Synchronized
    fun ambience(context: Context, mood: Mood?) {
        if (mood == playing && layers.isNotEmpty()) return
        stop()
        if (mood == null) return
        playing = mood
        for ((res, volume) in mix(mood)) {
            val p = runCatching { MediaPlayer.create(context.applicationContext, res) }.getOrNull() ?: continue
            p.isLooping = true
            p.setVolume(0f, 0f)
            p.start()
            layers += Layer(p, volume)
        }
        val mine = ArrayList(layers)
        fader = Thread({
            val start = System.currentTimeMillis()
            while (playing == mood) {
                val k = ((System.currentTimeMillis() - start) / FADE_MS.toFloat()).coerceAtMost(1f)
                mine.forEach { l -> runCatching { l.player.setVolume(l.volume * k, l.volume * k) } }
                if (k >= 1f) break
                Thread.sleep(60)
            }
        }, "ambience-fade").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        playing = null
        val old = ArrayList(layers)
        layers.clear()
        if (old.isEmpty()) return
        // A short fade out, then let go.
        Thread({
            for (i in 10 downTo 0) {
                old.forEach { l -> runCatching { l.player.setVolume(l.volume * i / 10f, l.volume * i / 10f) } }
                Thread.sleep(40)
            }
            old.forEach { l -> runCatching { l.player.stop() }; runCatching { l.player.release() } }
        }, "ambience-stop").apply { isDaemon = true; start() }
    }

    private fun attrs() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /** A tingsha bell: two close partials beating, a bright overtone, a long fade. */
    fun bell() {
        Thread {
            val n = (5.5 * RATE).toInt()
            val pcm = ShortArray(n) { i ->
                val t = i.toDouble() / RATE
                val env = exp(-t * 0.9) * (1 - exp(-t * 400))
                val v = sin(2 * PI * 1046.5 * t) * 0.5 + sin(2 * PI * 1049.8 * t) * 0.4 + sin(2 * PI * 2793.0 * t) * 0.18 * exp(-t * 2) + sin(2 * PI * 4186.0 * t) * 0.06 * exp(-t * 4)
                (v * env * 0.5 * Short.MAX_VALUE).toInt().toShort()
            }
            val t = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(attrs())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: return@Thread
            t.write(pcm, 0, pcm.size)
            t.play()
            Thread.sleep(5_800)
            t.release()
        }.start()
    }
}

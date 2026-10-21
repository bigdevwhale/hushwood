package app.hushwood.sit

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import app.hushwood.domain.Daypart
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The places' sounds, synthesised on the fly so the app ships no audio: wind in the spruces, birds at dawn,
 * the lake lapping, the sea breathing, crickets on summer nights, a fire when you sit at one, and the bell.
 */
object Sound {
    private const val RATE = 22_050

    /** What's playing: the place, the hour, the season and whether the fire is lit. */
    data class Mood(val place: Place, val part: Daypart, val season: Season, val fire: Boolean)

    @Volatile private var playing: Mood? = null
    private var thread: Thread? = null

    fun ambience(mood: Mood?) {
        if (mood == playing && thread?.isAlive == true) return
        stop()
        if (mood == null) return
        playing = mood
        thread = Thread({ loop(mood) }, "ambience").apply { isDaemon = true; start() }
    }

    fun stop() {
        playing = null
        thread?.join(400)
        thread = null
    }

    private fun attrs() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private fun track(bytes: Int, mode: Int) = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(attrs())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(bytes)
            .setTransferMode(mode)
            .build()
    }.getOrNull()

    private fun loop(mood: Mood) {
        val chunk = 2048
        val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = track(maxOf(min, chunk * 4), AudioTrack.MODE_STREAM) ?: return
        val gen = Forest(mood)
        val buf = ShortArray(chunk)
        var n = 0L
        try {
            t.play()
            while (playing == mood) {
                for (i in 0 until chunk) {
                    // Fade in over the first three seconds.
                    val fade = (n / (RATE * 3.0)).coerceAtMost(1.0)
                    buf[i] = (gen.next() * fade * 0.6 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
                    n++
                }
                t.write(buf, 0, chunk)
            }
        } finally {
            runCatching { t.stop() }
            t.release()
        }
    }

    /** Everything layered for one place and hour. */
    private class Forest(m: Mood) {
        private val rnd = Random(7)
        private var time = 0.0
        private val dt = 1.0 / RATE
        private val night = m.part == Daypart.NIGHT
        private val winter = m.season == Season.WINTER

        // Wind: brown noise, swelling slowly.
        private var brown = 0.0
        private val windK = when (m.place) { Place.MOUNTAINS -> 1.6; Place.SEA -> 0.6; else -> 1.0 }

        // Water: a stream's hiss, the lake's lapping, the sea's long breath.
        private var low = 0.0
        private var low2 = 0.0
        private var prev = 0.0
        private val water = m.place

        // Birds: chirps, quick falling sweeps.
        private val birds = !night && !winter && m.place != Place.SEA
        private val gulls = !night && m.place == Place.SEA
        private var chirpT = 10.0
        private var chirpF = 0.0
        private var chirpA = 0.0
        private var chirpPh = 0.0

        // Crickets on summer evenings and nights.
        private val crickets = m.season == Season.SUMMER && (night || m.part == Daypart.DUSK) && m.place != Place.SEA
        private var cricketPh = 0.0

        // An owl now and then.
        private val owl = night && m.place == Place.FOREST
        private var hootT = 6.0
        private var hootPh = 0.0

        private val fire = m.fire
        private var crack = 0.0
        private var crackDecay = 0.0

        fun next(): Double {
            time += dt
            val white = rnd.nextDouble(-1.0, 1.0)
            brown = (brown + white * 0.02) * 0.998
            val swell = 0.6 + 0.4 * sin(time * 0.21) * sin(time * 0.07 + 1.0)
            var out = brown * 1.4 * windK * swell

            low += (white - low) * 0.08
            low2 += (low - low2) * 0.02
            when (water) {
                Place.FOREST, Place.MOUNTAINS -> {
                    val hiss = white - prev
                    out += hiss * 0.05 * (0.8 + 0.2 * sin(time * 1.7))
                }
                Place.LAKE -> out += low * 0.35 * (0.5 + 0.5 * sin(time * 2 * PI / 3.4).let { it * it })
                Place.SEA -> {
                    // A wave every eight seconds or so: a rising wash, then the hiss of it drawing back.
                    val ph = (time / 8.3) % 1.0
                    val env = if (ph < 0.35) ph / 0.35 else exp(-(ph - 0.35) * 5)
                    out += (low * 0.6 + (white - prev) * 0.08) * env + low2 * 1.4
                }
            }
            prev = white

            if (birds || gulls) {
                chirpT -= dt
                if (chirpT <= 0) {
                    chirpT = if (gulls) 3.0 + rnd.nextDouble() * 9 else 0.15 + rnd.nextDouble() * (if (rnd.nextDouble() < 0.3) 4.0 else 0.4)
                    chirpF = if (gulls) 900.0 + rnd.nextDouble() * 300 else 2800.0 + rnd.nextDouble() * 2200
                    chirpA = if (gulls) 0.08 else 0.05 + rnd.nextDouble() * 0.05
                }
                chirpA *= if (gulls) 0.99994 else 0.9993
                chirpF *= if (gulls) 0.99996 else 0.99993
                chirpPh += 2 * PI * chirpF * dt
                out += sin(chirpPh) * chirpA * (if (gulls) (1 + 0.5 * sin(chirpPh * 0.5)) else 1.0)
            }
            if (crickets) {
                cricketPh += 2 * PI * 4400 * dt
                val pulse = if ((time * 28) % 1.0 < 0.4 && (time * 0.7) % 1.0 < 0.6) 1.0 else 0.0
                out += sin(cricketPh) * 0.018 * pulse
            }
            if (owl) {
                hootT -= dt
                if (hootT <= 0) hootT = 9.0 + rnd.nextDouble() * 14
                val h = 9.0 - hootT
                if (hootT > 7.6) {
                    hootPh += 2 * PI * 380 * dt
                    val env = sin(PI * ((9.0 - hootT) / 1.4)).coerceAtLeast(0.0)
                    if (h < 0.5 || h in 0.7..1.4) out += sin(hootPh) * 0.09 * env
                }
            }
            if (fire) {
                if (rnd.nextDouble() < 8.0 / RATE) {
                    crack = 0.4 + rnd.nextDouble() * 0.6
                    crackDecay = 0.9 + rnd.nextDouble() * 0.08
                }
                out += (white - prev) * crack * 0.3 + brown * 0.6
                crack *= crackDecay
            }
            return out
        }
    }

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
            val t = track(pcm.size * 2, AudioTrack.MODE_STATIC) ?: return@Thread
            t.write(pcm, 0, pcm.size)
            t.play()
            Thread.sleep(5_800)
            t.release()
        }.start()
    }
}

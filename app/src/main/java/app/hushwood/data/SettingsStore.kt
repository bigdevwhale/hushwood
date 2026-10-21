package app.hushwood.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.hushwood.domain.Breath
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** A sitting under way; kept in settings so it survives the app being closed. [length]: minutes, 0 for open-ended. */
data class Sitting(val practiceId: Long, val start: Long, val length: Int)

data class Settings(
    val onboarded: Boolean = false,
    /** Minutes the bell waits by default; 0 sits until you stand up. */
    val length: Int = 15,
    val breath: Breath = Breath.CALM,
    /** The forest's own sounds while sitting. */
    val sound: Boolean = true,
    /** A bell at the end. */
    val bell: Boolean = true,
    val haptics: Boolean = true,
    /** Near-dark screen while the phone lies face down. */
    val dim: Boolean = true,
    val remind: Boolean = false,
    /** Minutes after midnight. */
    val remindAt: Int = 8 * 60,
    /** What's been left on the stump for next time. */
    val offering: Offering? = null,
    /** Where you sit now. */
    val place: Place = Place.FOREST,
    val sitting: Sitting? = null,
    /** The practice picked last time, offered first next time. */
    val lastPractice: Long = 0,
    /** Animals already welcomed with a "first time" card. */
    val met: Set<String> = emptySet(),
    /** Portraits already celebrated (their start times), and whether the guardian's coming out was shown. */
    val shownPortraits: Set<String> = emptySet(),
    val shownGuardian: Boolean = false,
    /** The demo build only: the language its made-up months were written in. */
    val demoLang: String = "",
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        length = this[LENGTH] ?: 15,
        breath = Breath.of(this[BREATH] ?: ""),
        sound = this[SOUND] ?: true,
        bell = this[BELL] ?: true,
        haptics = this[HAPTICS] ?: true,
        dim = this[DIM] ?: true,
        remind = this[REMIND] ?: false,
        remindAt = this[REMIND_AT] ?: (8 * 60),
        offering = Offering.of(this[OFFERING]),
        place = Place.of(this[PLACE]),
        sitting = this[SIT_PRACTICE]?.let { Sitting(it, this[SIT_START] ?: 0, this[SIT_LENGTH] ?: 0) },
        lastPractice = this[LAST_PRACTICE] ?: 0,
        met = this[MET] ?: emptySet(),
        shownPortraits = this[SHOWN_PORTRAITS] ?: emptySet(),
        shownGuardian = this[SHOWN_GUARDIAN] ?: false,
        demoLang = this[DEMO_LANG] ?: "",
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[LENGTH] = s.length
            it[BREATH] = s.breath.name
            it[SOUND] = s.sound
            it[BELL] = s.bell
            it[HAPTICS] = s.haptics
            it[DIM] = s.dim
            it[REMIND] = s.remind
            it[REMIND_AT] = s.remindAt
            it[PLACE] = s.place.name
            if (s.offering != null) it[OFFERING] = s.offering.name else it.remove(OFFERING)
            if (s.sitting != null) {
                it[SIT_PRACTICE] = s.sitting.practiceId
                it[SIT_START] = s.sitting.start
                it[SIT_LENGTH] = s.sitting.length
            } else {
                it.remove(SIT_PRACTICE); it.remove(SIT_START); it.remove(SIT_LENGTH)
            }
            it[LAST_PRACTICE] = s.lastPractice
            it[MET] = s.met
            it[SHOWN_PORTRAITS] = s.shownPortraits
            it[SHOWN_GUARDIAN] = s.shownGuardian
            it[DEMO_LANG] = s.demoLang
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LENGTH = intPreferencesKey("length")
        val BREATH = stringPreferencesKey("breath")
        val SOUND = booleanPreferencesKey("sound")
        val BELL = booleanPreferencesKey("bell")
        val HAPTICS = booleanPreferencesKey("haptics")
        val DIM = booleanPreferencesKey("dim")
        val REMIND = booleanPreferencesKey("remind")
        val REMIND_AT = intPreferencesKey("remind_at")
        val OFFERING = stringPreferencesKey("offering")
        val PLACE = stringPreferencesKey("place")
        val SIT_PRACTICE = longPreferencesKey("sit_practice")
        val SIT_START = longPreferencesKey("sit_start")
        val SIT_LENGTH = intPreferencesKey("sit_length")
        val LAST_PRACTICE = longPreferencesKey("last_practice")
        val MET = stringSetPreferencesKey("met")
        val SHOWN_PORTRAITS = stringSetPreferencesKey("shown_portraits")
        val SHOWN_GUARDIAN = booleanPreferencesKey("shown_guardian")
        val DEMO_LANG = stringPreferencesKey("demo_lang")
    }
}

package app.hushwood.ui

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import app.hushwood.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Words in the app's language. Plain functions over a [Context] so notifications can use them too. */
object T {
    /** "19:30". */
    fun time(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    /** "12:05" for a running timer, "1:02:05" past an hour. */
    fun clock(seconds: Long): String {
        val h = seconds / 3600
        val m = seconds / 60 % 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** "3 h 20 min" or "45 min". */
    fun minutes(c: Context, m: Int): String = if (m >= 60) c.getString(R.string.h_min, m / 60, m % 60) else c.getString(R.string.n_min, m)

    fun date(d: LocalDate, ru: Boolean): String =
        d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH))

    /** "12 Oct". */
    fun short(d: LocalDate, ru: Boolean): String = d.format(DateTimeFormatter.ofPattern("d MMM", if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH))
}

/** In-app language switching (persisted by AppCompat; shows up in Android 13+ per-app language settings). */
object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}

/** A soft tick under the finger. */
fun View.tick(haptics: Boolean) {
    if (haptics) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

@Composable
fun ctx(): Context = LocalContext.current

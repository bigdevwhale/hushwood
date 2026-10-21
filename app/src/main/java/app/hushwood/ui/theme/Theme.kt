package app.hushwood.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.hushwood.R

/**
 * The wood after dusk: deep spruce-green surfaces so the pixel clearing is the brightest thing on screen,
 * letters the colour of birch bark, moss for the one thing to do and lantern amber for the fire.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val moss: Color,
    val mossSoft: Color,
    val onMoss: Color,
    val amber: Color,
    val amberSoft: Color,
)

val Dusk = Palette(
    bg = Color(0xFF111819),
    surface = Color(0xFF182224),
    surface2 = Color(0xFF1F2B2D),
    line = Color(0xFF2C3A3C),
    ink = Color(0xFFEDE8D8),
    muted = Color(0xFFA3A894),
    faint = Color(0xFF66705F),
    moss = Color(0xFFB5CF8E),
    mossSoft = Color(0xFF253423),
    onMoss = Color(0xFF14200F),
    amber = Color(0xFFF0B866),
    amberSoft = Color(0xFF3A2C18),
)

val LocalPalette = staticCompositionLocalOf { Dusk }

/** Lora: the journal's hand, for titles and the forest's lines. */
val Serif = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

/** Nunito: soft and round, for everything else. */
val Sans = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(26.dp)
    val RLg = RoundedCornerShape(20.dp)
    val RMd = RoundedCornerShape(16.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 40.sp, color = U.c.ink)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 27.sp, lineHeight = 33.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 27.sp, color = U.c.ink)
    /** What the forest says. */
    val Line: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 26.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 15.5.sp, lineHeight = 23.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.16.em, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = U.c.ink)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = U.c.faint)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
fun HushTheme(content: @Composable () -> Unit) {
    val p = Dusk
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = p.moss,
                onPrimary = p.onMoss,
                primaryContainer = p.mossSoft,
                onPrimaryContainer = p.ink,
                secondary = p.amber,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface2,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 15.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}

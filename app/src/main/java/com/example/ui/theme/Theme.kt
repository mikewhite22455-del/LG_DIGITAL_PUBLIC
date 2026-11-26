package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

enum class LgThemePreset(val id: String, val label: String) {
    TURQUOISE_GOLD("turquoise_gold", "Turquoise & Gold"),
    OBSIDIAN_CYAN("obsidian_cyan", "Obsidian Cyan")
}

object LgThemeState {
    var activeTheme by mutableStateOf(LgThemePreset.TURQUOISE_GOLD)
}

object AppThemePrefs {
    private const val PREFS_NAME = "lg_theme_prefs"
    private const val KEY_PRESET = "active_theme_preset"

    fun load(context: Context): LgThemePreset {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRESET, LgThemePreset.TURQUOISE_GOLD.id)
        return LgThemePreset.entries.firstOrNull { it.id == id } ?: LgThemePreset.TURQUOISE_GOLD
    }

    fun save(context: Context, preset: LgThemePreset) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRESET, preset.id)
            .apply()
    }
}

object LgColors {
    val ObsidianBg = Color(0xFF040F14)
    val PanelSlate = Color(0xFF091B24)
    val ElevatedSlate = Color(0xFF102835)
    val TealAmbient = Color(0xFF0A3846)
    val Turquoise = Color(0xFF1CE8D0)
    val CyanGlow = Color(0xFF42F5F5)
    val Gold = Color(0xFFE5B84B)
    val GoldBright = Color(0xFFFFD86B)
    val GoldDark = Color(0xFF9C721D)
    val Crimson = Color(0xFFFF5A6E)
    val AmberWarn = Color(0xFFFFB74D)
    val InkPrimary = Color(0xFFF2FAFC)
    val InkSecondary = Color(0xFFB3D2DC)
    val InkMuted = Color(0xFF6E93A1)
    val BorderHairline = Color(0xFF1D4454)
}

val LgBrandFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Bold),
    Font(R.font.orbitron, FontWeight.ExtraBold)
)

val RussoOneFontFamily = FontFamily(
    Font(R.font.russo_one, FontWeight.Normal),
    Font(R.font.russo_one, FontWeight.Bold)
)

val TradingViewFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

private val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

private val DisplayFontFamily = FontFamily(
    Font(R.font.rajdhani, FontWeight.SemiBold),
    Font(R.font.rajdhani, FontWeight.Bold)
)

val TabLabelTextStyle = TextStyle(
    fontFamily = RussoOneFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 0.8.sp
)

val DropdownTabTextStyle = TextStyle(
    fontFamily = RussoOneFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    letterSpacing = 0.9.sp
)

private val LgTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

private val LgDarkColorScheme = darkColorScheme(
    primary = LgColors.Turquoise,
    onPrimary = LgColors.ObsidianBg,
    primaryContainer = LgColors.TealAmbient,
    onPrimaryContainer = LgColors.CyanGlow,
    secondary = LgColors.Gold,
    onSecondary = LgColors.ObsidianBg,
    secondaryContainer = LgColors.GoldDark,
    onSecondaryContainer = LgColors.GoldBright,
    tertiary = LgColors.CyanGlow,
    onTertiary = LgColors.ObsidianBg,
    background = LgColors.ObsidianBg,
    onBackground = LgColors.InkPrimary,
    surface = LgColors.PanelSlate,
    onSurface = LgColors.InkPrimary,
    surfaceVariant = LgColors.ElevatedSlate,
    onSurfaceVariant = LgColors.InkSecondary,
    outline = LgColors.BorderHairline,
    error = LgColors.Crimson,
    onError = LgColors.ObsidianBg
)

@Composable
fun LgDigitalTheme(
    theme: LgThemePreset = LgThemeState.activeTheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LgDarkColorScheme,
        typography = LgTypography,
        content = content
    )
}

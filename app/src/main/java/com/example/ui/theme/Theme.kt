package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

object LgColors {
    val ObsidianBg = Color(0xFF040B11)
    val PanelSlate = Color(0xFF07151E)
    val ElevatedSlate = Color(0xFF0C202B)
    val BorderHairline = Color(0xFF1A4352)
    val Turquoise = Color(0xFF3CE6C5)
    val CyanGlow = Color(0xFF39D8F0)
    val Gold = Color(0xFFEAC569)
    val GoldDark = Color(0xFFB5822D)
    val Crimson = Color(0xFFFF6E83)
    val AmberWarn = Color(0xFFF6B756)
    val InkPrimary = Color(0xFFF3F7FA)
    val InkSecondary = Color(0xFFA0B6C7)
    val InkMuted = Color(0xFF678396)
}

val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Bold)
)

val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold)
)

val DmMonoFontFamily = FontFamily(
    Font(R.font.dm_mono, FontWeight.Normal),
    Font(R.font.dm_mono, FontWeight.Medium)
)

private val LgDarkColorScheme = darkColorScheme(
    primary = LgColors.Turquoise,
    onPrimary = LgColors.ObsidianBg,
    primaryContainer = Color(0xFF113830),
    onPrimaryContainer = LgColors.Turquoise,
    secondary = LgColors.Gold,
    onSecondary = LgColors.ObsidianBg,
    secondaryContainer = Color(0xFF2E2614),
    onSecondaryContainer = LgColors.Gold,
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
    onError = Color(0xFF2A070D)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.6.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.4.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun LgDigitalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LgDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

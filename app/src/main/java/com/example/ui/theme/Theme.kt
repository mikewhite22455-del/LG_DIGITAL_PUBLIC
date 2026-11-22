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
    // Deep Oceanic Teal / Obsidian matching the LG Coin & Old Layout / Welcome Screen
    val ObsidianBg = Color(0xFF031117)
    val TealAmbient = Color(0xFF092A33)
    val PanelSlate = Color(0xFF061B23)
    val ElevatedSlate = Color(0xFF0B2732)
    val BorderHairline = Color(0xFF174F5E)

    // Exact LG Coin Turquoise & Gold palette
    val Turquoise = Color(0xFF28E7D2)
    val TurquoiseDeep = Color(0xFF0D4D52)
    val CyanGlow = Color(0xFF35E3F2)
    val Gold = Color(0xFFF0C96A)
    val GoldBright = Color(0xFFFBE294)
    val GoldDark = Color(0xFFB5822D)

    val Crimson = Color(0xFFFF6E83)
    val AmberWarn = Color(0xFFF6B756)
    val InkPrimary = Color(0xFFF3F8FA)
    val InkSecondary = Color(0xFFA5C2CE)
    val InkMuted = Color(0xFF6B8E9C)
}

val ChakraPetchFontFamily = FontFamily(
    Font(R.font.chakra_petch, FontWeight.Normal),
    Font(R.font.chakra_petch, FontWeight.SemiBold),
    Font(R.font.chakra_petch, FontWeight.Bold),
    Font(R.font.chakra_petch, FontWeight.ExtraBold)
)

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

val TabLabelTextStyle = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.0.sp
)

val DropdownTabTextStyle = TextStyle(
    fontFamily = ChakraPetchFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.9.sp
)

private val LgDarkColorScheme = darkColorScheme(
    primary = LgColors.Turquoise,
    onPrimary = LgColors.ObsidianBg,
    primaryContainer = Color(0xFF0C3A3C),
    onPrimaryContainer = LgColors.Turquoise,
    secondary = LgColors.Gold,
    onSecondary = LgColors.ObsidianBg,
    secondaryContainer = Color(0xFF332812),
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
    onError = Color(0xFF2A070D)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.8.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.6.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.35.sp
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
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.6.sp
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

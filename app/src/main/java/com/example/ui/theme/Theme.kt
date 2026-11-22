package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

data class LgPalette(
    val obsidianBg: Color,
    val tealAmbient: Color,
    val panelSlate: Color,
    val elevatedSlate: Color,
    val borderHairline: Color,
    val turquoise: Color,
    val turquoiseDeep: Color,
    val cyanGlow: Color,
    val gold: Color,
    val goldBright: Color,
    val goldDark: Color,
    val crimson: Color,
    val amberWarn: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color
)

enum class AppColorTheme(
    val id: String,
    val number: Int,
    val displayName: String,
    val tagline: String,
    val palette: LgPalette
) {
    COBALT_AMBER(
        id = "cobalt_amber",
        number = 1,
        displayName = "Midnight Cobalt & Gold",
        tagline = "Royal sapphire depth with electric azure & molten gold",
        palette = LgPalette(
            obsidianBg = Color(0xFF050C1D),
            tealAmbient = Color(0xFF0E2148),
            panelSlate = Color(0xFF091630),
            elevatedSlate = Color(0xFF10254D),
            borderHairline = Color(0xFF234B8E),
            turquoise = Color(0xFF38B6FF),
            turquoiseDeep = Color(0xFF10427A),
            cyanGlow = Color(0xFF66D1FF),
            gold = Color(0xFFFFBC42),
            goldBright = Color(0xFFFFDF8E),
            goldDark = Color(0xFFB87914),
            crimson = Color(0xFFFF5C7A),
            amberWarn = Color(0xFFFFA93A),
            inkPrimary = Color(0xFFF3F8FF),
            inkSecondary = Color(0xFFA9C3E8),
            inkMuted = Color(0xFF6E8DB8),
            primaryContainer = Color(0xFF103266),
            secondaryContainer = Color(0xFF3F2B0D)
        )
    ),
    ROYAL_VIOLET(
        id = "royal_violet",
        number = 2,
        displayName = "Royal Amethyst & Gold",
        tagline = "Midnight ultraviolet with luminous purple & champagne",
        palette = LgPalette(
            obsidianBg = Color(0xFF0B0618),
            tealAmbient = Color(0xFF231147),
            panelSlate = Color(0xFF130B29),
            elevatedSlate = Color(0xFF1E1340),
            borderHairline = Color(0xFF493085),
            turquoise = Color(0xFFC07BFF),
            turquoiseDeep = Color(0xFF4D2287),
            cyanGlow = Color(0xFFD9A7FF),
            gold = Color(0xFFF5CE6E),
            goldBright = Color(0xFFFFE8A6),
            goldDark = Color(0xFFA87B22),
            crimson = Color(0xFFFF5E8A),
            amberWarn = Color(0xFFF7B24F),
            inkPrimary = Color(0xFFF8F4FF),
            inkSecondary = Color(0xFFC5B5E3),
            inkMuted = Color(0xFF8B79B0),
            primaryContainer = Color(0xFF32175E),
            secondaryContainer = Color(0xFF3B2C12)
        )
    ),
    EMERALD_MATRIX(
        id = "emerald_matrix",
        number = 3,
        displayName = "Emerald Quant & Platinum",
        tagline = "Deep forest obsidian with neon emerald & brushed silver",
        palette = LgPalette(
            obsidianBg = Color(0xFF04120C),
            tealAmbient = Color(0xFF0D3322),
            panelSlate = Color(0xFF081F15),
            elevatedSlate = Color(0xFF103021),
            borderHairline = Color(0xFF1E6343),
            turquoise = Color(0xFF14F195),
            turquoiseDeep = Color(0xFF0C5939),
            cyanGlow = Color(0xFF5CFFB8),
            gold = Color(0xFFD8E6DF),
            goldBright = Color(0xFFF4FBF7),
            goldDark = Color(0xFF829E91),
            crimson = Color(0xFFFF637B),
            amberWarn = Color(0xFFF5B841),
            inkPrimary = Color(0xFFF2FBF6),
            inkSecondary = Color(0xFFA4CCB8),
            inkMuted = Color(0xFF69967F),
            primaryContainer = Color(0xFF0C402A),
            secondaryContainer = Color(0xFF243830)
        )
    ),
    CRIMSON_CARBON(
        id = "crimson_carbon",
        number = 4,
        displayName = "Crimson Carbon & Champagne",
        tagline = "Stealth obsidian carbon with ruby flare & warm gold",
        palette = LgPalette(
            obsidianBg = Color(0xFF12060A),
            tealAmbient = Color(0xFF36101B),
            panelSlate = Color(0xFF1C0A10),
            elevatedSlate = Color(0xFF2B1119),
            borderHairline = Color(0xFF6E253A),
            turquoise = Color(0xFFFF5277),
            turquoiseDeep = Color(0xFF7A1731),
            cyanGlow = Color(0xFFFF85A1),
            gold = Color(0xFFFFCE73),
            goldBright = Color(0xFFFFE6B0),
            goldDark = Color(0xFFB37D24),
            crimson = Color(0xFFFF3B5C),
            amberWarn = Color(0xFFFFAC4B),
            inkPrimary = Color(0xFFFFF5F7),
            inkSecondary = Color(0xFFDDB2BE),
            inkMuted = Color(0xFFA37280),
            primaryContainer = Color(0xFF4F1324),
            secondaryContainer = Color(0xFF402C12)
        )
    ),
    SOLAR_COPPER(
        id = "solar_copper",
        number = 5,
        displayName = "Solar Copper & Bronze Noir",
        tagline = "Espresso obsidian with glowing molten orange & gold",
        palette = LgPalette(
            obsidianBg = Color(0xFF130C06),
            tealAmbient = Color(0xFF38200D),
            panelSlate = Color(0xFF1E130A),
            elevatedSlate = Color(0xFF2E1E10),
            borderHairline = Color(0xFF704620),
            turquoise = Color(0xFFFF9233),
            turquoiseDeep = Color(0xFF7D3D0A),
            cyanGlow = Color(0xFFFFB56B),
            gold = Color(0xFFFFD56B),
            goldBright = Color(0xFFFFEBA8),
            goldDark = Color(0xFFB5801C),
            crimson = Color(0xFFFF5E6C),
            amberWarn = Color(0xFFFFB74D),
            inkPrimary = Color(0xFFFFF8F0),
            inkSecondary = Color(0xFFDCC0A6),
            inkMuted = Color(0xFFA38267),
            primaryContainer = Color(0xFF4D290D),
            secondaryContainer = Color(0xFF423010)
        )
    ),
    ARCTIC_FROST(
        id = "arctic_frost",
        number = 6,
        displayName = "Arctic Slate & Ice Cyan",
        tagline = "Crisp graphite slate with glacier cyan & frost silver",
        palette = LgPalette(
            obsidianBg = Color(0xFF0A1017),
            tealAmbient = Color(0xFF18293A),
            panelSlate = Color(0xFF101A26),
            elevatedSlate = Color(0xFF192838),
            borderHairline = Color(0xFF335270),
            turquoise = Color(0xFF5CE1FF),
            turquoiseDeep = Color(0xFF195870),
            cyanGlow = Color(0xFF9AF0FF),
            gold = Color(0xFFDCEBFF),
            goldBright = Color(0xFFF5FAFF),
            goldDark = Color(0xFF8AA4C2),
            crimson = Color(0xFFFF6B81),
            amberWarn = Color(0xFFFFC15E),
            inkPrimary = Color(0xFFF5FAFF),
            inkSecondary = Color(0xFFB4C9DF),
            inkMuted = Color(0xFF7A94B0),
            primaryContainer = Color(0xFF163C52),
            secondaryContainer = Color(0xFF25354A)
        )
    ),
    CYBER_MAGENTA(
        id = "cyber_magenta",
        number = 7,
        displayName = "Cyber Magenta & Neon Cyan",
        tagline = "Ultraviolet neon pulse with electric pink & cyber teal",
        palette = LgPalette(
            obsidianBg = Color(0xFF0E0517),
            tealAmbient = Color(0xFF2E0D42),
            panelSlate = Color(0xFF170924),
            elevatedSlate = Color(0xFF26103B),
            borderHairline = Color(0xFF6E258A),
            turquoise = Color(0xFF2CF6FF),
            turquoiseDeep = Color(0xFF105D70),
            cyanGlow = Color(0xFF8CF9FF),
            gold = Color(0xFFFF52D9),
            goldBright = Color(0xFFFF9CEB),
            goldDark = Color(0xFFA61D8A),
            crimson = Color(0xFFFF4D6D),
            amberWarn = Color(0xFFFFBA42),
            inkPrimary = Color(0xFFFDF5FF),
            inkSecondary = Color(0xFFD6B5E8),
            inkMuted = Color(0xFF9B75B3),
            primaryContainer = Color(0xFF113F52),
            secondaryContainer = Color(0xFF4D1347)
        )
    ),
    OCEANIC_GOLD(
        id = "oceanic_gold",
        number = 8,
        displayName = "Bright Turquoise & Gold",
        tagline = "Signature LG Digital bright glowing turquoise lines & luminous gold",
        palette = LgPalette(
            obsidianBg = Color(0xFF03141B),
            tealAmbient = Color(0xFF0C3B48),
            panelSlate = Color(0xFF07232E),
            elevatedSlate = Color(0xFF0E3442),
            borderHairline = Color(0xFF1E8499),
            turquoise = Color(0xFF00FFF0),
            turquoiseDeep = Color(0xFF0E6E78),
            cyanGlow = Color(0xFF52FFFF),
            gold = Color(0xFFFFD447),
            goldBright = Color(0xFFFFEE88),
            goldDark = Color(0xFFC98F1E),
            crimson = Color(0xFFFF6B81),
            amberWarn = Color(0xFFFFC154),
            inkPrimary = Color(0xFFF6FCFF),
            inkSecondary = Color(0xFFB5DCE8),
            inkMuted = Color(0xFF78AAB8),
            primaryContainer = Color(0xFF0C4C52),
            secondaryContainer = Color(0xFF423210)
        )
    );

    companion object {
        fun fromId(id: String?): AppColorTheme =
            entries.firstOrNull { it.id == id } ?: OCEANIC_GOLD
    }
}

object AppThemePrefs {
    private const val PREFS_NAME = "lg_digital_theme_prefs"
    private const val KEY_THEME_ID = "selected_theme_id_v2"

    fun load(context: Context): AppColorTheme {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_ID, AppColorTheme.OCEANIC_GOLD.id)
        return AppColorTheme.fromId(id)
    }

    fun save(context: Context, theme: AppColorTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME_ID, theme.id)
            .apply()
    }
}

object LgThemeState {
    var activeTheme by mutableStateOf(AppColorTheme.OCEANIC_GOLD)
}

/**
 * Dynamic Compose-observable palette used across all LG Digital screens and components.
 * Switching LgThemeState.activeTheme updates the entire UI instantly while preserving all images.
 */
object LgColors {
    private val p: LgPalette get() = LgThemeState.activeTheme.palette

    val ObsidianBg: Color get() = p.obsidianBg
    val TealAmbient: Color get() = p.tealAmbient
    val PanelSlate: Color get() = p.panelSlate
    val ElevatedSlate: Color get() = p.elevatedSlate
    val BorderHairline: Color get() = p.borderHairline

    val Turquoise: Color get() = p.turquoise
    val TurquoiseDeep: Color get() = p.turquoiseDeep
    val CyanGlow: Color get() = p.cyanGlow
    val Gold: Color get() = p.gold
    val GoldBright: Color get() = p.goldBright
    val GoldDark: Color get() = p.goldDark

    val Crimson: Color get() = p.crimson
    val AmberWarn: Color get() = p.amberWarn
    val InkPrimary: Color get() = p.inkPrimary
    val InkSecondary: Color get() = p.inkSecondary
    val InkMuted: Color get() = p.inkMuted
}

/**
 * Original LG DIGITAL signature futuristic geometric brand typeface (Orbitron).
 */
val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron, FontWeight.Normal),
    Font(R.font.orbitron, FontWeight.Medium),
    Font(R.font.orbitron, FontWeight.SemiBold),
    Font(R.font.orbitron, FontWeight.Bold),
    Font(R.font.orbitron, FontWeight.ExtraBold),
    Font(R.font.orbitron, FontWeight.Black)
)

/**
 * Primary brand wordmark font for "LG DIGITAL" headers across all screens.
 */
val LgBrandFontFamily = OrbitronFontFamily

/**
 * TradingView-matched bold heavy geometric sans-serif logo typeface (Archivo Black).
 */
val TradingViewFontFamily = FontFamily(
    Font(R.font.archivo_black, FontWeight.Normal),
    Font(R.font.archivo_black, FontWeight.SemiBold),
    Font(R.font.archivo_black, FontWeight.Bold),
    Font(R.font.archivo_black, FontWeight.ExtraBold)
)

/**
 * Original bold tech display typeface (Russo One) from the classic LG Digital app.
 */
val RussoOneFontFamily = FontFamily(
    Font(R.font.russo_one, FontWeight.Normal),
    Font(R.font.russo_one, FontWeight.Medium),
    Font(R.font.russo_one, FontWeight.SemiBold),
    Font(R.font.russo_one, FontWeight.Bold),
    Font(R.font.russo_one, FontWeight.ExtraBold)
)

val ChakraPetchFontFamily = FontFamily(
    Font(R.font.chakra_petch, FontWeight.Normal),
    Font(R.font.chakra_petch, FontWeight.SemiBold),
    Font(R.font.chakra_petch, FontWeight.Bold),
    Font(R.font.chakra_petch, FontWeight.ExtraBold)
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
    fontFamily = TradingViewFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 10.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.25.sp
)

val DropdownTabTextStyle = TextStyle(
    fontFamily = TradingViewFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
)

private fun buildColorScheme(p: LgPalette) = darkColorScheme(
    primary = p.turquoise,
    onPrimary = p.obsidianBg,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.turquoise,
    secondary = p.gold,
    onSecondary = p.obsidianBg,
    secondaryContainer = p.secondaryContainer,
    onSecondaryContainer = p.goldBright,
    tertiary = p.cyanGlow,
    onTertiary = p.obsidianBg,
    background = p.obsidianBg,
    onBackground = p.inkPrimary,
    surface = p.panelSlate,
    onSurface = p.inkPrimary,
    surfaceVariant = p.elevatedSlate,
    onSurfaceVariant = p.inkSecondary,
    outline = p.borderHairline,
    error = p.crimson,
    onError = Color(0xFF2A070D)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 35.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 29.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 27.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.5.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ChakraPetchFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = TradingViewFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmMonoFontFamily,
        fontWeight = FontWeight.Medium,
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
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun LgDigitalTheme(
    theme: AppColorTheme = LgThemeState.activeTheme,
    content: @Composable () -> Unit
) {
    if (LgThemeState.activeTheme != theme) {
        LgThemeState.activeTheme = theme
    }
    MaterialTheme(
        colorScheme = buildColorScheme(theme.palette),
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

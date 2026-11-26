package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.SignalAction
import com.example.ui.theme.LgColors
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TradingViewFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared deep oceanic-teal + bright glowing turquoise-lined circuit-board backdrop
 * rendered continuously behind every screen, card, and pop-up across the app.
 */
@Composable
fun LgCircuitBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        LgColors.TealAmbient,
                        LgColors.ObsidianBg,
                        LgColors.PanelSlate,
                        LgColors.ObsidianBg
                    )
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w <= 1f || h <= 1f) return@drawBehind
                val topRadius = (maxOf(w, h) * 0.62f).coerceAtLeast(1f)
                val bottomRadius = (maxOf(w, h) * 0.48f).coerceAtLeast(1f)

                // Ambient bright turquoise radial glow near top center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LgColors.Turquoise.copy(alpha = 0.24f),
                            LgColors.TealAmbient.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.14f),
                        radius = topRadius
                    ),
                    center = Offset(w * 0.5f, h * 0.14f),
                    radius = topRadius
                )

                // Warm bright gold radial shimmer near bottom right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LgColors.GoldBright.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.84f),
                        radius = bottomRadius
                    ),
                    center = Offset(w * 0.85f, h * 0.84f),
                    radius = bottomRadius
                )

                // Full-screen glowing turquoise lined matrix grid (horizontal + vertical lines)
                val gridSpacing = 54f
                val gridLineColor = LgColors.Turquoise.copy(alpha = 0.16f)
                val majorLineColor = LgColors.Turquoise.copy(alpha = 0.30f)

                var gx = 0f
                var colIdx = 0
                while (gx <= w) {
                    val isMajor = colIdx % 3 == 0
                    drawLine(
                        color = if (isMajor) majorLineColor else gridLineColor,
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = if (isMajor) 1.5f else 0.9f
                    )
                    gx += gridSpacing
                    colIdx++
                }

                var gy = 0f
                var rowIdx = 0
                while (gy <= h) {
                    val isMajor = rowIdx % 3 == 0
                    drawLine(
                        color = if (isMajor) majorLineColor else gridLineColor,
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = if (isMajor) 1.5f else 0.9f
                    )
                    gy += gridSpacing
                    rowIdx++
                }

                // Bright turquoise circuit buses & gold accent traces spanning the entire background
                val turqTraceBright = LgColors.Turquoise.copy(alpha = 0.48f)
                val turqTraceMid = LgColors.CyanGlow.copy(alpha = 0.34f)
                val goldTrace = LgColors.GoldBright.copy(alpha = 0.30f)

                // Left rail circuit traces
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.02f, h * 0.06f),
                        Offset(w * 0.14f, h * 0.12f),
                        Offset(w * 0.14f, h * 0.28f),
                        Offset(w * 0.32f, h * 0.34f),
                        Offset(w * 0.32f, h * 0.44f)
                    ),
                    color = turqTraceBright,
                    strokeWidth = 2.6f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(0f, h * 0.40f),
                        Offset(w * 0.10f, h * 0.47f),
                        Offset(w * 0.10f, h * 0.68f),
                        Offset(w * 0.24f, h * 0.74f),
                        Offset(w * 0.52f, h * 0.74f)
                    ),
                    color = goldTrace,
                    strokeWidth = 2.2f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.03f, h * 0.79f),
                        Offset(w * 0.16f, h * 0.86f),
                        Offset(w * 0.16f, h * 0.96f)
                    ),
                    color = turqTraceBright,
                    strokeWidth = 2.5f
                )

                // Center cross-screen turquoise data buses
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.22f, h * 0.18f),
                        Offset(w * 0.48f, h * 0.18f),
                        Offset(w * 0.56f, h * 0.24f),
                        Offset(w * 0.78f, h * 0.24f)
                    ),
                    color = turqTraceMid,
                    strokeWidth = 2.2f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.18f, h * 0.56f),
                        Offset(w * 0.44f, h * 0.56f),
                        Offset(w * 0.52f, h * 0.50f),
                        Offset(w * 0.82f, h * 0.50f)
                    ),
                    color = turqTraceBright,
                    strokeWidth = 2.3f
                )

                // Right rail circuit traces
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.98f, h * 0.09f),
                        Offset(w * 0.86f, h * 0.15f),
                        Offset(w * 0.86f, h * 0.32f),
                        Offset(w * 0.70f, h * 0.38f)
                    ),
                    color = goldTrace,
                    strokeWidth = 2.2f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w, h * 0.47f),
                        Offset(w * 0.88f, h * 0.53f),
                        Offset(w * 0.88f, h * 0.72f),
                        Offset(w * 0.74f, h * 0.79f)
                    ),
                    color = turqTraceBright,
                    strokeWidth = 2.6f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.96f, h * 0.84f),
                        Offset(w * 0.82f, h * 0.90f),
                        Offset(w * 0.58f, h * 0.90f),
                        Offset(w * 0.50f, h * 0.96f)
                    ),
                    color = turqTraceMid,
                    strokeWidth = 2.2f
                )
            },
        content = content
    )
}

private fun DrawScope.drawCircuitPath(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawCircle(color = color.copy(alpha = (color.alpha * 1.9f).coerceAtMost(0.75f)), radius = 4.5f, center = points.first())
    drawCircle(color = color.copy(alpha = (color.alpha * 1.9f).coerceAtMost(0.75f)), radius = 4.5f, center = points.last())
}

@Composable
fun GlassPanelCard(
    modifier: Modifier = Modifier,
    highlightBorder: Color = LgColors.Turquoise.copy(alpha = 0.52f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .circuitTextFieldBackdrop(cornerRadius = 12.dp, drawBorder = false),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = LgColors.PanelSlate.copy(alpha = 0.74f)
        ),
        border = BorderStroke(1.1.dp, highlightBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
fun CyberFrameCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = LgColors.Turquoise,
    content: @Composable ColumnScope.() -> Unit
) {
    val gold = LgColors.GoldBright
    val cardTopBg = LgColors.TealAmbient.copy(alpha = 0.76f)
    val cardMidBg = LgColors.PanelSlate.copy(alpha = 0.82f)
    val cardBotBg = LgColors.ObsidianBg.copy(alpha = 0.88f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(cardTopBg, cardMidBg, cardBotBg)
                )
            )
            .drawBehind {
                val stroke = 2.8f
                val cornerLen = 30f
                val w = size.width
                val h = size.height
                if (w <= 1f || h <= 1f) return@drawBehind
                val cardGlowRadius = (maxOf(w, h) * 0.55f).coerceAtLeast(1f)

                // Bright turquoise radial + lined circuit traces behind card content
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LgColors.Turquoise.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.18f, h * 0.16f),
                        radius = cardGlowRadius
                    ),
                    center = Offset(w * 0.18f, h * 0.16f),
                    radius = cardGlowRadius
                )
                // Subtle internal turquoise blueprint lines
                val step = 46f
                var lx = step
                while (lx < w) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.10f),
                        start = Offset(lx, 0f),
                        end = Offset(lx, h),
                        strokeWidth = 1f
                    )
                    lx += step
                }
                var ly = step
                while (ly < h) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.10f),
                        start = Offset(0f, ly),
                        end = Offset(w, ly),
                        strokeWidth = 1f
                    )
                    ly += step
                }
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.03f, h * 0.14f),
                        Offset(w * 0.12f, h * 0.22f),
                        Offset(w * 0.12f, h * 0.58f)
                    ),
                    color = accentColor.copy(alpha = 0.28f),
                    strokeWidth = 1.8f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.97f, h * 0.82f),
                        Offset(w * 0.86f, h * 0.72f),
                        Offset(w * 0.86f, h * 0.36f)
                    ),
                    color = gold.copy(alpha = 0.24f),
                    strokeWidth = 1.7f
                )

                // Outer crisp turquoise frame
                drawRect(
                    color = accentColor.copy(alpha = 0.46f),
                    topLeft = Offset(1f, 1f),
                    size = Size(w - 2f, h - 2f),
                    style = Stroke(width = 1.6f)
                )

                // Bright gold corner brackets
                drawLine(gold, Offset(2f, 2f), Offset(cornerLen, 2f), strokeWidth = stroke)
                drawLine(gold, Offset(2f, 2f), Offset(2f, cornerLen), strokeWidth = stroke)
                drawLine(gold, Offset(w - cornerLen, 2f), Offset(w - 2f, 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - 2f, 2f), Offset(w - 2f, cornerLen), strokeWidth = stroke)
                drawLine(gold, Offset(2f, h - 2f), Offset(cornerLen, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(2f, h - cornerLen), Offset(2f, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - cornerLen, h - 2f), Offset(w - 2f, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - 2f, h - cornerLen), Offset(w - 2f, h - 2f), strokeWidth = stroke)

                // Mid-rail bright turquoise circuit glow strips
                drawLine(
                    color = accentColor.copy(alpha = 0.92f),
                    start = Offset(2f, h * 0.26f),
                    end = Offset(2f, h * 0.74f),
                    strokeWidth = 3.2f
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.92f),
                    start = Offset(w - 2f, h * 0.26f),
                    end = Offset(w - 2f, h * 0.74f),
                    strokeWidth = 3.2f
                )
            }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = LgColors.Gold.copy(alpha = 0.62f)
                    )
                    Text(
                        text = title.uppercase(),
                        fontFamily = RussoOneFontFamily,
                        fontSize = 14.sp,
                        letterSpacing = 1.1.sp,
                        color = LgColors.GoldBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = LgColors.Gold.copy(alpha = 0.62f)
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun GoldMetallicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) {
                    Brush.verticalGradient(
                        listOf(LgColors.GoldBright, LgColors.Gold, LgColors.GoldDark)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(LgColors.ElevatedSlate, LgColors.PanelSlate)
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) Color(0xFFFFEEBD) else LgColors.BorderHairline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            fontFamily = RussoOneFontFamily,
            fontSize = 14.sp,
            letterSpacing = 0.9.sp,
            color = if (enabled) Color(0xFF051319) else LgColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SentimentArcGauges(
    bullishPct: Float,
    bearishPct: Float,
    neutralPct: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SingleSentimentGauge("Bullish", bullishPct, LgColors.Turquoise, Modifier.weight(1f))
        SingleSentimentGauge("Bearish", bearishPct, LgColors.Crimson, Modifier.weight(1f))
        SingleSentimentGauge("Neutral", neutralPct, LgColors.Gold, Modifier.weight(1f))
    }
}

@Composable
private fun SingleSentimentGauge(
    label: String,
    pct: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clamped = pct.coerceIn(0f, 100f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ElevatedSlate.copy(alpha = 0.72f))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 10f
                val d = minOf(size.width * 0.85f, size.height * 1.65f)
                val left = (size.width - d) / 2f
                val top = size.height - d / 2f - 4f
                drawArc(
                    color = LgColors.BorderHairline,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(d, d),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f * (clamped / 100f),
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(d, d),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${clamped.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = LgColors.InkSecondary)
    }
}

/**
 * Displays the authentic coin logo for every cryptocurrency across the app:
 * bundled coin drawables when available, plus crisp custom vector emblems for
 * stablecoins (USDT, USDC) and PoW/staking/DEX tokens (KAS, ETC, XMR, BNB, DASH, etc.).
 */
@Composable
fun CoinAvatar(
    symbol: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
    logoUrl: String? = null
) {
    val cleanSym = symbol.trim().uppercase()
        .removeSuffix("/USD")
        .removeSuffix("-USD")
        .removeSuffix("/USDT")
        .removeSuffix("-USDT")
        .let {
            when (it) {
                "XBT", "XXBT" -> "BTC"
                "XDG", "XXDG" -> "DOGE"
                else -> it
            }
        }
    val iconRes = CoinAssets.iconFor(cleanSym)
    if (iconRes != null) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "$cleanSym coin logo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.2.dp, LgColors.Gold.copy(alpha = 0.65f), CircleShape)
        )
    } else {
        val resolvedUrl = logoUrl?.takeIf { it.isNotBlank() }
            ?: com.example.core.CoinLogoCache.urlFor(cleanSym)
        val context = LocalContext.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(LgColors.PanelSlate)
                .border(1.2.dp, LgColors.Gold.copy(alpha = 0.62f), CircleShape)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$cleanSym coin logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.5.dp)
                    .clip(CircleShape),
                loading = {
                    CustomCoinLogoBadge(
                        symbol = cleanSym,
                        size = size
                    )
                },
                error = {
                    CustomCoinLogoBadge(
                        symbol = cleanSym,
                        size = size
                    )
                }
            )
        }
    }
}

@Composable
private fun CustomCoinLogoBadge(
    symbol: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val palette = when (symbol) {
        "BTC" -> Pair(Color(0xFFF7931A), Color(0xFF7A4407))
        "ETH" -> Pair(Color(0xFF627EEA), Color(0xFF28377A))
        "SOL" -> Pair(Color(0xFF14F195), Color(0xFF9945FF))
        "XRP" -> Pair(Color(0xFF38BDF8), Color(0xFF0C4A6E))
        "ADA" -> Pair(Color(0xFF3B82F6), Color(0xFF1E3A8A))
        "DOGE" -> Pair(Color(0xFFC2A633), Color(0xFF5C4D11))
        "AVAX" -> Pair(Color(0xFFE84142), Color(0xFF6B1516))
        "LINK" -> Pair(Color(0xFF2A5ADA), Color(0xFF152C6E))
        "DOT" -> Pair(Color(0xFFE6007A), Color(0xFF6E003B))
        "LTC" -> Pair(Color(0xFF94A3B8), Color(0xFF334155))
        "UNI" -> Pair(Color(0xFFFF007A), Color(0xFF6E0035))
        "USDT" -> Pair(Color(0xFF1BA27A), Color(0xFF0C5C45))
        "USDC" -> Pair(Color(0xFF2775CA), Color(0xFF14427A))
        "KAS" -> Pair(Color(0xFF49EACB), Color(0xFF0E534A))
        "ETC" -> Pair(Color(0xFF3AB83A), Color(0xFF144D23))
        "XMR" -> Pair(Color(0xFFFF6600), Color(0xFF282B30))
        "BNB" -> Pair(Color(0xFFF3BA2F), Color(0xFF6B4E09))
        "DASH" -> Pair(Color(0xFF008CE7), Color(0xFF0A3866))
        else -> {
            val hash = kotlin.math.abs(symbol.hashCode())
            val accents = listOf(
                Color(0xFF1CE8D0),
                Color(0xFFFFD86B),
                Color(0xFF60A5FA),
                Color(0xFFA78BFA),
                Color(0xFF34D399),
                Color(0xFFF472B6),
                Color(0xFFFB923C)
            )
            Pair(accents[hash % accents.size], LgColors.Gold)
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(palette.first.copy(alpha = 0.35f), LgColors.PanelSlate)
                )
            )
            .border(1.3.dp, palette.first.copy(alpha = 0.85f), CircleShape)
    ) {
        when (symbol) {
            "USDT" -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawCircle(color = Color(0xFF26A17B), radius = minOf(w, h) * 0.48f)
                    // White T-crossbar + oval ring
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.25f, h * 0.30f),
                        end = Offset(w * 0.75f, h * 0.30f),
                        strokeWidth = maxOf(2f, w * 0.11f),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.50f, h * 0.30f),
                        end = Offset(w * 0.50f, h * 0.80f),
                        strokeWidth = maxOf(2f, w * 0.11f),
                        cap = StrokeCap.Round
                    )
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(w * 0.16f, h * 0.45f),
                        size = Size(w * 0.68f, h * 0.20f),
                        style = Stroke(width = maxOf(1.6f, w * 0.08f))
                    )
                }
            }
            "USDC" -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.12f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawCircle(color = Color(0xFF2775CA), radius = minOf(w, h) * 0.48f)
                    drawArc(
                        color = Color.White,
                        startAngle = 125f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(w * 0.14f, h * 0.14f),
                        size = Size(w * 0.72f, h * 0.72f),
                        style = Stroke(width = maxOf(1.6f, w * 0.08f), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -55f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(w * 0.14f, h * 0.14f),
                        size = Size(w * 0.72f, h * 0.72f),
                        style = Stroke(width = maxOf(1.6f, w * 0.08f), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.45f).sp,
                    color = Color.White
                )
            }
            "ETC" -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.2f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val top = Path().apply {
                        moveTo(w * 0.5f, 0f)
                        lineTo(w * 0.84f, h * 0.46f)
                        lineTo(w * 0.5f, h * 0.36f)
                        lineTo(w * 0.16f, h * 0.46f)
                        close()
                    }
                    val bottom = Path().apply {
                        moveTo(w * 0.5f, h)
                        lineTo(w * 0.84f, h * 0.54f)
                        lineTo(w * 0.5f, h * 0.64f)
                        lineTo(w * 0.16f, h * 0.54f)
                        close()
                    }
                    drawPath(top, color = Color(0xFF3AB83A))
                    drawPath(bottom, color = Color(0xFF7CE87C))
                }
            }
            "BNB" -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.22f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val diamond = Path().apply {
                        moveTo(w * 0.5f, h * 0.12f)
                        lineTo(w * 0.88f, h * 0.5f)
                        lineTo(w * 0.5f, h * 0.88f)
                        lineTo(w * 0.12f, h * 0.5f)
                        close()
                    }
                    drawPath(diamond, color = Color(0xFFF3BA2F), style = Stroke(width = maxOf(2f, w * 0.14f)))
                    drawCircle(color = Color(0xFFF3BA2F), radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.5f))
                }
            }
            else -> {
                val initials = symbol.take(3).ifBlank { "?" }
                val fontSp = when {
                    initials.length >= 3 -> (size.value * 0.31f).sp
                    else -> (size.value * 0.38f).sp
                }
                Text(
                    text = initials,
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSp,
                    color = LgColors.GoldBright,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Unified Gold & Turquoise Coin Dropdown Selector with authentic CoinAvatar logos
 * in both the collapsed selector button and every dropdown menu row.
 */
@Composable
fun CoinDropdownSelector(
    selectedSymbol: String,
    symbols: List<String>,
    onSelectSymbol: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select Coin",
    displayLabelFor: (String) -> String = { sym ->
        val entry = CoinAssets.find(sym)
        if (entry != null) "${entry.symbol} · ${entry.name}" else sym
    },
    testTag: String = "coin_dropdown_selector"
) {
    var expanded by remember { mutableStateOf(false) }
    val catalog = com.example.core.ExchangeCoinCatalogStore.snapshot
    val totalCount = remember(catalog.coins, symbols) {
        maxOf(catalog.coins.size, symbols.size)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LgColors.PanelSlate.copy(alpha = 0.96f),
                            LgColors.ElevatedSlate.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.4.dp,
                    color = if (expanded) LgColors.Turquoise else LgColors.Gold.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                CoinAvatar(symbol = selectedSymbol, size = 30.dp)
                Column(modifier = Modifier.weight(1f)) {
                    if (label.isNotBlank()) {
                        Text(
                            text = "${label.uppercase()} · ${catalog.activeScopeLabel.uppercase()} ($totalCount)",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = displayLabelFor(selectedSymbol),
                        fontFamily = RussoOneFontFamily,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp,
                        color = LgColors.GoldBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = "Open full exchange coin dropdown",
                tint = LgColors.GoldBright,
                modifier = Modifier.size(24.dp)
            )
        }

        if (expanded) {
            ExchangeCoinPickerDialog(
                selectedSymbol = selectedSymbol,
                extraSymbols = symbols,
                title = label,
                displayLabelFor = displayLabelFor,
                onSelectSymbol = onSelectSymbol,
                onDismiss = { expanded = false }
            )
        }
    }
}

/**
 * Official TradingView geometric TV monogram + wordmark rendered in the bold TradingView logo font.
 */
@Composable
fun TradingViewBrandMark(
    iconSize: Dp = 20.dp,
    showWordmark: Boolean = true,
    tint: Color = LgColors.InkPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(width = iconSize * 1.45f, height = iconSize)) {
            val w = size.width
            val h = size.height
            // Left T-block of the official TradingView TV mark
            val tPath = Path().apply {
                moveTo(w * 0.04f, h * 0.18f)
                lineTo(w * 0.50f, h * 0.18f)
                lineTo(w * 0.50f, h * 0.84f)
                lineTo(w * 0.30f, h * 0.84f)
                lineTo(w * 0.30f, h * 0.40f)
                lineTo(w * 0.04f, h * 0.40f)
                close()
            }
            drawPath(tPath, color = tint)
            // Signature circular dot
            drawCircle(
                color = LgColors.Turquoise,
                radius = h * 0.15f,
                center = Offset(w * 0.62f, h * 0.32f)
            )
            // Signature angled V slash
            val slashPath = Path().apply {
                moveTo(w * 0.78f, h * 0.18f)
                lineTo(w * 0.96f, h * 0.18f)
                lineTo(w * 0.74f, h * 0.84f)
                lineTo(w * 0.56f, h * 0.84f)
                close()
            }
            drawPath(slashPath, color = LgColors.GoldBright)
        }
        if (showWordmark) {
            Text(
                text = "TradingView",
                fontFamily = TradingViewFontFamily,
                fontSize = (iconSize.value * 0.72f).sp,
                letterSpacing = (-0.5).sp,
                color = tint,
                maxLines = 1
            )
        }
    }
}

/**
 * Loads the actual official high-resolution Sports League logos (NFL, NBA, NHL, MLB, EPL, MLS, UFC)
 * from ESPN's public league CDN via Coil, falling back cleanly to custom vector crests if offline.
 */
@Composable
fun SportsLeagueLogo(
    league: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val code = league.trim().uppercase()
    val officialLogoUrl = when (code) {
        "NFL" -> "https://a.espncdn.com/i/teamlogos/leagues/500/nfl.png"
        "NBA" -> "https://a.espncdn.com/i/teamlogos/leagues/500/nba.png"
        "NHL" -> "https://a.espncdn.com/i/teamlogos/leagues/500/nhl.png"
        "MLB" -> "https://a.espncdn.com/i/teamlogos/leagues/500/mlb.png"
        "EPL" -> "https://a.espncdn.com/combiner/i?img=/i/leaguelogos/soccer/500/23.png"
        "MLS" -> "https://a.espncdn.com/i/teamlogos/leagues/500/mls.png"
        "UFC", "MMA" -> "https://a.espncdn.com/i/teamlogos/leagues/500/ufc.png"
        "BOXING", "WBC", "WBA" -> null // Use crisp custom Championship Boxing Crest
        else -> null
    }

    if (officialLogoUrl != null) {
        val context = LocalContext.current
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, LgColors.Gold.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(officialLogoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$code official league logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    SportsLeagueFallbackCrest(code = code, size = size)
                },
                error = {
                    SportsLeagueFallbackCrest(code = code, size = size)
                }
            )
        }
    } else {
        SportsLeagueFallbackCrest(code = code, size = size, modifier = modifier)
    }
}

@Composable
private fun SportsLeagueFallbackCrest(
    code: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (code) {
            "NFL" -> {
                // Classic Shield Crest (Navy top, Crimson lower stripes, Gold border)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val shield = Path().apply {
                        moveTo(w * 0.14f, h * 0.14f)
                        quadraticTo(w * 0.50f, h * 0.03f, w * 0.86f, h * 0.14f)
                        lineTo(w * 0.86f, h * 0.56f)
                        quadraticTo(w * 0.86f, h * 0.82f, w * 0.50f, h * 0.96f)
                        quadraticTo(w * 0.14f, h * 0.82f, w * 0.14f, h * 0.56f)
                        close()
                    }
                    drawPath(shield, color = Color(0xFF0D2558))
                    val redLower = Path().apply {
                        moveTo(w * 0.16f, h * 0.45f)
                        lineTo(w * 0.84f, h * 0.45f)
                        lineTo(w * 0.84f, h * 0.56f)
                        quadraticTo(w * 0.84f, h * 0.80f, w * 0.50f, h * 0.93f)
                        quadraticTo(w * 0.16f, h * 0.80f, w * 0.16f, h * 0.56f)
                        close()
                    }
                    drawPath(redLower, color = Color(0xFFD50A0A))
                    drawPath(shield, color = LgColors.GoldBright, style = Stroke(width = 2.2f))
                    // 4 league stars in navy crown
                    for (i in 0..3) {
                        drawCircle(
                            color = Color.White,
                            radius = w * 0.036f,
                            center = Offset(w * (0.29f + i * 0.14f), h * 0.25f)
                        )
                    }
                }
                Text(
                    text = "NFL",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.30f).sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = size * 0.22f)
                )
            }
            "NBA" -> {
                // Classic Split Navy/Crimson Rounded Pill + White Silhouette Curve
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color(0xFF1D428A),
                        topLeft = Offset(w * 0.16f, h * 0.06f),
                        size = Size(w * 0.34f, h * 0.88f),
                        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
                    )
                    drawRoundRect(
                        color = Color(0xFFC8102E),
                        topLeft = Offset(w * 0.50f, h * 0.06f),
                        size = Size(w * 0.34f, h * 0.88f),
                        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.16f, h * 0.06f),
                        size = Size(w * 0.68f, h * 0.88f),
                        cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                        style = Stroke(width = 2f)
                    )
                    // Basketball & player silhouette arc
                    drawCircle(color = Color.White, radius = w * 0.07f, center = Offset(w * 0.52f, h * 0.22f))
                    drawCircle(color = LgColors.GoldBright, radius = w * 0.06f, center = Offset(w * 0.36f, h * 0.42f))
                }
                Text(
                    text = "NBA",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.27f).sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = size * 0.44f)
                )
            }
            "NHL" -> {
                // Classic Black & Metallic Silver-Gold Shield Crest
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val shield = Path().apply {
                        moveTo(w * 0.16f, h * 0.12f)
                        lineTo(w * 0.84f, h * 0.12f)
                        lineTo(w * 0.84f, h * 0.58f)
                        quadraticTo(w * 0.84f, h * 0.84f, w * 0.50f, h * 0.95f)
                        quadraticTo(w * 0.16f, h * 0.84f, w * 0.16f, h * 0.58f)
                        close()
                    }
                    drawPath(shield, color = Color(0xFF101418))
                    drawLine(
                        color = Color(0xFF3E4956),
                        start = Offset(w * 0.22f, h * 0.72f),
                        end = Offset(w * 0.78f, h * 0.22f),
                        strokeWidth = maxOf(4f, w * 0.16f)
                    )
                    drawPath(shield, color = Color(0xFFD5DCE4), style = Stroke(width = 2.4f))
                }
                Text(
                    text = "NHL",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.29f).sp,
                    color = Color.White
                )
            }
            "MLB" -> {
                // Classic Horizontal Split Navy/Red Rounded Frame + Batter Emblem
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color(0xFF002D72),
                        topLeft = Offset(w * 0.06f, h * 0.20f),
                        size = Size(w * 0.44f, h * 0.60f),
                        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                    )
                    drawRoundRect(
                        color = Color(0xFFD50032),
                        topLeft = Offset(w * 0.50f, h * 0.20f),
                        size = Size(w * 0.44f, h * 0.60f),
                        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.06f, h * 0.20f),
                        size = Size(w * 0.88f, h * 0.60f),
                        cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(color = Color.White, radius = w * 0.065f, center = Offset(w * 0.24f, h * 0.50f))
                }
                Text(
                    text = "MLB",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.27f).sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = size * 0.16f)
                )
            }
            "EPL", "MLS" -> {
                // Premier / Major Soccer Crest (Turquoise & Royal Purple/Navy + Gold Crown Ring)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val crest = Path().apply {
                        moveTo(w * 0.16f, h * 0.14f)
                        lineTo(w * 0.84f, h * 0.14f)
                        lineTo(w * 0.82f, h * 0.60f)
                        quadraticTo(w * 0.78f, h * 0.86f, w * 0.50f, h * 0.95f)
                        quadraticTo(w * 0.22f, h * 0.86f, w * 0.18f, h * 0.60f)
                        close()
                    }
                    drawPath(crest, color = Color(0xFF38003C))
                    drawPath(crest, color = LgColors.Turquoise, style = Stroke(width = 2.4f))
                    // Golden soccer starball ring at top
                    val cx = w * 0.50f
                    val cy = h * 0.34f
                    val r = w * 0.12f
                    drawCircle(color = LgColors.GoldBright, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
                    for (k in 0..4) {
                        val ang = (k * 72 - 90) * PI / 180.0
                        drawCircle(
                            color = LgColors.Turquoise,
                            radius = w * 0.03f,
                            center = Offset(cx + (r * cos(ang)).toFloat(), cy + (r * sin(ang)).toFloat())
                        )
                    }
                }
                Text(
                    text = code,
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.28f).sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = size * 0.36f)
                )
            }
            "BOXING", "WBC", "WBA" -> {
                // Championship Boxing Ring & Golden Belt Crest
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    // Deep crimson/obsidian championship hexagon ring medallion
                    val medal = Path().apply {
                        moveTo(w * 0.50f, h * 0.05f)
                        lineTo(w * 0.90f, h * 0.24f)
                        lineTo(w * 0.90f, h * 0.76f)
                        lineTo(w * 0.50f, h * 0.95f)
                        lineTo(w * 0.10f, h * 0.76f)
                        lineTo(w * 0.10f, h * 0.24f)
                        close()
                    }
                    drawPath(medal, color = Color(0xFF1F0A0F))
                    // 3 boxing ring ropes across upper canvas
                    for (r in 0..2) {
                        val ry = h * (0.26f + r * 0.12f)
                        drawLine(
                            color = if (r == 1) Color(0xFFD50032) else Color.White.copy(alpha = 0.65f),
                            start = Offset(w * 0.16f, ry),
                            end = Offset(w * 0.84f, ry),
                            strokeWidth = 1.6f
                        )
                    }
                    // Stylized paired crimson/gold boxing gloves at crown
                    drawCircle(
                        color = Color(0xFFD50032),
                        radius = w * 0.12f,
                        center = Offset(w * 0.39f, h * 0.34f)
                    )
                    drawCircle(
                        color = Color(0xFFE53935),
                        radius = w * 0.12f,
                        center = Offset(w * 0.61f, h * 0.34f)
                    )
                    drawCircle(
                        color = LgColors.GoldBright,
                        radius = w * 0.12f,
                        center = Offset(w * 0.39f, h * 0.34f),
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(
                        color = LgColors.GoldBright,
                        radius = w * 0.12f,
                        center = Offset(w * 0.61f, h * 0.34f),
                        style = Stroke(width = 1.5f)
                    )
                    // Championship gold outer trim
                    drawPath(medal, color = Color(0xFFD50032), style = Stroke(width = 3.0f))
                    drawPath(medal, color = LgColors.GoldBright, style = Stroke(width = 1.4f))
                }
                Text(
                    text = "BOX",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.26f).sp,
                    color = LgColors.GoldBright,
                    modifier = Modifier.padding(top = size * 0.34f)
                )
            }
            else -> {
                // UFC Octagon Crest (Crimson & Gold Championship Octagon)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = this.size.width
                    val h = this.size.height
                    val d = minOf(w, h) * 0.90f
                    val ox = (w - d) / 2f
                    val oy = (h - d) / 2f
                    val c = 0.29f
                    val oct = Path().apply {
                        moveTo(ox + d * c, oy)
                        lineTo(ox + d * (1f - c), oy)
                        lineTo(ox + d, oy + d * c)
                        lineTo(ox + d, oy + d * (1f - c))
                        lineTo(ox + d * (1f - c), oy + d)
                        lineTo(ox + d * c, oy + d)
                        lineTo(ox, oy + d * (1f - c))
                        lineTo(ox, oy + d * c)
                        close()
                    }
                    drawPath(oct, color = Color(0xFF1A070B))
                    drawPath(oct, color = Color(0xFFD20A0A), style = Stroke(width = 3.2f))
                    drawPath(oct, color = LgColors.GoldBright, style = Stroke(width = 1.4f))
                }
                Text(
                    text = "UFC",
                    fontFamily = RussoOneFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.28f).sp,
                    color = LgColors.GoldBright
                )
            }
        }
    }
}

@Composable
fun SignalBadge(
    action: SignalAction,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when (action) {
        SignalAction.BUY -> Triple(LgColors.Turquoise.copy(alpha = 0.16f), LgColors.Turquoise, "BUY")
        SignalAction.SELL -> Triple(LgColors.Crimson.copy(alpha = 0.16f), LgColors.Crimson, "SELL")
        SignalAction.HOLD -> Triple(LgColors.Gold.copy(alpha = 0.16f), LgColors.Gold, "HOLD")
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.55f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Compact 24H price movement pill pairing a clear directional up/down arrow icon
 * with the formatted percentage delta for rapid scanning and color-independent accessibility.
 */
@Composable
fun TrendChangeBadge(
    changePct: Double,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val isUp = changePct >= 0.0
    val color = if (isUp) LgColors.Turquoise else LgColors.Crimson
    val formatted = Formatters.formatPct(changePct)
    val arrowDesc = if (isUp) "Up $formatted in 24 hours" else "Down $formatted in 24 hours"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 2.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = arrowDesc,
                tint = color,
                modifier = Modifier.size(if (compact) 11.dp else 13.dp)
            )
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    valueColor: Color = LgColors.InkPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ElevatedSlate.copy(alpha = 0.78f))
            .border(1.dp, LgColors.BorderHairline.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LgColors.InkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = LgColors.Gold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LgColors.InkSecondary
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Inserts the website / old-app deep oceanic-teal + glowing turquoise & gold circuit-board
 * background directly behind text boxes and input panels.
 */
fun Modifier.circuitTextFieldBackdrop(
    cornerRadius: Dp = 12.dp,
    drawBorder: Boolean = true
): Modifier {
    val bgA = LgColors.TealAmbient.copy(alpha = 0.92f)
    val bgB = LgColors.PanelSlate.copy(alpha = 0.96f)
    val bgC = LgColors.ElevatedSlate.copy(alpha = 0.92f)
    val glowPrimary = LgColors.Turquoise
    val glowSecondary = LgColors.Gold
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.horizontalGradient(listOf(bgA, bgB, bgC))
        )
        .drawBehind {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@drawBehind
            val leftRadius = maxOf(w * 0.42f, h * 1.2f).coerceAtLeast(1f)
            val rightRadius = maxOf(w * 0.32f, h * 1.1f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowPrimary.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.5f),
                    radius = leftRadius
                ),
                center = Offset(w * 0.15f, h * 0.5f),
                radius = leftRadius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowSecondary.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.5f),
                    radius = rightRadius
                ),
                center = Offset(w * 0.88f, h * 0.5f),
                radius = rightRadius
            )
            drawCircuitPath(
                points = listOf(
                    Offset(w * 0.02f, h * 0.25f),
                    Offset(w * 0.14f, h * 0.25f),
                    Offset(w * 0.20f, h * 0.72f)
                ),
                color = glowPrimary.copy(alpha = 0.26f),
                strokeWidth = 1.6f
            )
            drawCircuitPath(
                points = listOf(
                    Offset(w * 0.98f, h * 0.75f),
                    Offset(w * 0.85f, h * 0.75f),
                    Offset(w * 0.78f, h * 0.28f)
                ),
                color = glowSecondary.copy(alpha = 0.22f),
                strokeWidth = 1.5f
            )
            if (drawBorder) {
                val r = cornerRadius.toPx()
                drawRoundRect(
                    color = glowSecondary.copy(alpha = 0.35f),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = 1.1f)
                )
            }
        }
}

/**
 * Displays an authentic official Exchange Logo beside every exchange title across the app
 * (Kraken, KuCoin, Coinbase, Bitstamp, Crypto.com, CoinGecko, Binance, All Connected).
 * Loads crisp high-res official exchange icons via Coil with color-matched vector fallback.
 */
@Composable
fun ExchangeLogo(
    exchangeName: String,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val norm = exchangeName.trim().lowercase()
    val logoUrl = when {
        norm.contains("kraken") -> "https://www.google.com/s2/favicons?domain=kraken.com&sz=128"
        norm.contains("kucoin") -> "https://www.google.com/s2/favicons?domain=kucoin.com&sz=128"
        norm.contains("coinbase") -> "https://www.google.com/s2/favicons?domain=coinbase.com&sz=128"
        norm.contains("bitstamp") -> "https://www.google.com/s2/favicons?domain=bitstamp.net&sz=128"
        norm.contains("crypto.com") || norm.contains("cryptocom") -> "https://www.google.com/s2/favicons?domain=crypto.com&sz=128"
        norm.contains("coingecko") -> "https://www.google.com/s2/favicons?domain=coingecko.com&sz=128"
        norm.contains("binance") -> "https://www.google.com/s2/favicons?domain=binance.com&sz=128"
        norm.contains("okx") -> "https://www.google.com/s2/favicons?domain=okx.com&sz=128"
        norm.contains("bybit") -> "https://www.google.com/s2/favicons?domain=bybit.com&sz=128"
        else -> null
    }

    val ringColor = when {
        norm.contains("kraken") -> Color(0xFF7132F5)
        norm.contains("kucoin") -> Color(0xFF23AF91)
        norm.contains("coinbase") -> Color(0xFF0052FF)
        norm.contains("bitstamp") -> Color(0xFF16A34A)
        norm.contains("crypto.com") || norm.contains("cryptocom") -> Color(0xFF1199FA)
        norm.contains("coingecko") -> Color(0xFF8DC63F)
        norm.contains("binance") -> Color(0xFFF3BA2F)
        else -> LgColors.Gold
    }

    if (logoUrl != null) {
        val context = LocalContext.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF0B1F27))
                .border(1.1.dp, ringColor.copy(alpha = 0.85f), CircleShape)
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$exchangeName exchange logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.5.dp)
                    .clip(CircleShape),
                loading = { ExchangeLogoFallback(exchangeName, size) },
                error = { ExchangeLogoFallback(exchangeName, size) }
            )
        }
    } else {
        ExchangeLogoFallback(exchangeName, size, modifier)
    }
}

@Composable
private fun ExchangeLogoFallback(
    exchangeName: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val norm = exchangeName.trim().lowercase()
    val (bg, accent, label) = when {
        norm.contains("kraken") -> Triple(Color(0xFF3E189C), Color(0xFFA78BFA), "K")
        norm.contains("kucoin") -> Triple(Color(0xFF0B4F43), Color(0xFF23AF91), "KU")
        norm.contains("coinbase") -> Triple(Color(0xFF0052FF), Color.White, "C")
        norm.contains("bitstamp") -> Triple(Color(0xFF0C5A2C), Color(0xFF4ADE80), "B")
        norm.contains("crypto.com") || norm.contains("cryptocom") -> Triple(Color(0xFF0B2B5C), Color(0xFF38BDF8), "CR")
        norm.contains("coingecko") -> Triple(Color(0xFF2F5E17), Color(0xFFA3E635), "CG")
        norm.contains("binance") -> Triple(Color(0xFF5A4206), Color(0xFFF3BA2F), "BN")
        else -> Triple(Color(0xFF0C3542), LgColors.GoldBright, "ALL")
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, accent.copy(alpha = 0.85f), CircleShape)
    ) {
        Text(
            text = label,
            fontFamily = TradingViewFontFamily,
            fontSize = (size.value * if (label.length >= 3) 0.28f else if (label.length == 2) 0.36f else 0.46f).sp,
            color = accent,
            maxLines = 1
        )
    }
}

enum class SocialPlatform(
    val label: String,
    val url: String,
    val brandBg: Color,
    val brandAccent: Color
) {
    FACEBOOK("Facebook", "https://www.facebook.com/lgdigitalcrypto", Color(0xFF1877F2), Color.White),
    TELEGRAM("Telegram", "https://t.me/lgdigitalcrypto", Color(0xFF229ED9), Color.White),
    GOOGLE("Google", "https://accounts.google.com", Color.White, Color(0xFF4285F4)),
    INSTAGRAM("Instagram", "https://www.instagram.com/lgdigitalcrypto", Color(0xFFE1306C), Color.White),
    YOUTUBE("YouTube", "https://www.youtube.com/@lgdigitalcrypto", Color(0xFFFF0000), Color.White),
    WEBSITE("Website", "https://lgdigital.ca", Color(0xFF0B323E), Color(0xFFFFD86B))
}

@Composable
fun SocialBrandBadge(
    platform: SocialPlatform,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(platform.brandBg)
            .border(
                width = 1.dp,
                color = if (platform == SocialPlatform.GOOGLE) Color(0xFFDADCE0) else platform.brandAccent.copy(alpha = 0.55f),
                shape = CircleShape
            )
    ) {
        when (platform) {
            SocialPlatform.FACEBOOK -> {
                Text(
                    text = "f",
                    fontFamily = TradingViewFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.62f).sp,
                    color = Color.White
                )
            }
            SocialPlatform.GOOGLE -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.16f)) {
                    val strokeW = maxOf(2.4f, this.size.width * 0.22f)
                    val inset = strokeW / 2f
                    val arcSize = Size(this.size.width - strokeW, this.size.height - strokeW)
                    val topLeft = Offset(inset, inset)
                    // Red top arc
                    drawArc(Color(0xFFEA4335), -145f, 100f, false, topLeft, arcSize, style = Stroke(strokeW, cap = StrokeCap.Butt))
                    // Yellow left arc
                    drawArc(Color(0xFFFBBC05), 145f, 70f, false, topLeft, arcSize, style = Stroke(strokeW, cap = StrokeCap.Butt))
                    // Green bottom arc
                    drawArc(Color(0xFF34A853), 45f, 100f, false, topLeft, arcSize, style = Stroke(strokeW, cap = StrokeCap.Butt))
                    // Blue right arc + crossbar
                    drawArc(Color(0xFF4285F4), -10f, 60f, false, topLeft, arcSize, style = Stroke(strokeW, cap = StrokeCap.Butt))
                    drawLine(
                        color = Color(0xFF4285F4),
                        start = Offset(this.size.width * 0.52f, this.size.height * 0.50f),
                        end = Offset(this.size.width * 0.94f, this.size.height * 0.50f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Butt
                    )
                }
            }
            SocialPlatform.TELEGRAM -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.20f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val plane = Path().apply {
                        moveTo(w * 0.95f, h * 0.12f)
                        lineTo(w * 0.05f, h * 0.48f)
                        lineTo(w * 0.38f, h * 0.60f)
                        lineTo(w * 0.78f, h * 0.28f)
                        lineTo(w * 0.46f, h * 0.66f)
                        lineTo(w * 0.74f, h * 0.88f)
                        close()
                    }
                    drawPath(plane, color = Color.White)
                }
            }
            SocialPlatform.INSTAGRAM -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.22f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawRoundRect(
                        color = Color.White,
                        cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
                        style = Stroke(width = maxOf(1.8f, w * 0.13f))
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.22f,
                        style = Stroke(width = maxOf(1.6f, w * 0.12f))
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.07f,
                        center = Offset(w * 0.76f, h * 0.24f)
                    )
                }
            }
            SocialPlatform.YOUTUBE -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.26f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val play = Path().apply {
                        moveTo(w * 0.24f, h * 0.16f)
                        lineTo(w * 0.86f, h * 0.50f)
                        lineTo(w * 0.24f, h * 0.84f)
                        close()
                    }
                    drawPath(play, color = Color.White)
                }
            }
            SocialPlatform.WEBSITE -> {
                Text(
                    text = "LG",
                    fontFamily = TradingViewFontFamily,
                    fontSize = (size.value * 0.38f).sp,
                    color = LgColors.GoldBright
                )
            }
        }
    }
}

/**
 * Reusable in-app Social Media Links Bar (Facebook, Telegram, Instagram, YouTube, Website)
 * usable on the Welcome/Sign-In screens, Home Dashboard, Vault, and Walkthrough sheet.
 */
@Composable
fun SocialMediaLinksBar(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val channels = remember {
        listOf(
            SocialPlatform.FACEBOOK,
            SocialPlatform.TELEGRAM,
            SocialPlatform.INSTAGRAM,
            SocialPlatform.YOUTUBE,
            SocialPlatform.WEBSITE
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        channels.forEach { platform ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (compact) 52.dp else 64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .circuitTextFieldBackdrop(cornerRadius = 12.dp)
                    .clickable {
                        runCatching { uriHandler.openUri(platform.url) }
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("social_link_${platform.name.lowercase()}")
            ) {
                SocialBrandBadge(platform = platform, size = if (compact) 24.dp else 28.dp)
                Text(
                    text = platform.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


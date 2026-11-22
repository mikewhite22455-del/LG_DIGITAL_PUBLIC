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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.CoinAssets
import com.example.domain.models.SignalAction
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.LgColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared deep oceanic-teal + glowing turquoise & gold circuit-board backdrop
 * matching the Old Layout & Welcome Screen aesthetic and LG coin palette.
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
                        Color(0xFF07232B),
                        LgColors.ObsidianBg,
                        Color(0xFF04171F),
                        Color(0xFF020B10)
                    )
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height

                // Ambient coin-matched turquoise radial glow near top center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LgColors.Turquoise.copy(alpha = 0.14f),
                            LgColors.TealAmbient.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.14f),
                        radius = maxOf(w, h) * 0.55f
                    ),
                    center = Offset(w * 0.5f, h * 0.14f),
                    radius = maxOf(w, h) * 0.55f
                )

                // Warm subtle gold radial shimmer near bottom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            LgColors.Gold.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.82f),
                        radius = maxOf(w, h) * 0.42f
                    ),
                    center = Offset(w * 0.85f, h * 0.82f),
                    radius = maxOf(w, h) * 0.42f
                )

                val turqTrace = LgColors.Turquoise.copy(alpha = 0.16f)
                val goldTrace = LgColors.Gold.copy(alpha = 0.14f)

                // Left rail circuit traces
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.02f, h * 0.08f),
                        Offset(w * 0.08f, h * 0.13f),
                        Offset(w * 0.08f, h * 0.29f),
                        Offset(w * 0.03f, h * 0.34f)
                    ),
                    color = turqTrace,
                    strokeWidth = 2.2f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(0f, h * 0.42f),
                        Offset(w * 0.06f, h * 0.47f),
                        Offset(w * 0.06f, h * 0.68f),
                        Offset(w * 0.12f, h * 0.74f)
                    ),
                    color = goldTrace,
                    strokeWidth = 1.8f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.03f, h * 0.80f),
                        Offset(w * 0.11f, h * 0.86f),
                        Offset(w * 0.11f, h * 0.95f)
                    ),
                    color = turqTrace,
                    strokeWidth = 2.0f
                )

                // Right rail circuit traces
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.98f, h * 0.10f),
                        Offset(w * 0.91f, h * 0.16f),
                        Offset(w * 0.91f, h * 0.33f),
                        Offset(w * 0.97f, h * 0.39f)
                    ),
                    color = goldTrace,
                    strokeWidth = 1.8f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w, h * 0.48f),
                        Offset(w * 0.93f, h * 0.54f),
                        Offset(w * 0.93f, h * 0.72f),
                        Offset(w * 0.87f, h * 0.78f)
                    ),
                    color = turqTrace,
                    strokeWidth = 2.2f
                )
                drawCircuitPath(
                    points = listOf(
                        Offset(w * 0.96f, h * 0.84f),
                        Offset(w * 0.88f, h * 0.90f),
                        Offset(w * 0.88f, h * 0.97f)
                    ),
                    color = goldTrace,
                    strokeWidth = 1.8f
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
    drawCircle(color = color.copy(alpha = (color.alpha * 1.9f).coerceAtMost(0.5f)), radius = 4f, center = points.first())
    drawCircle(color = color.copy(alpha = (color.alpha * 1.9f).coerceAtMost(0.5f)), radius = 4f, center = points.last())
}

@Composable
fun GlassPanelCard(
    modifier: Modifier = Modifier,
    highlightBorder: Color = LgColors.BorderHairline,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = LgColors.PanelSlate.copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.dp, highlightBorder),
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
    val gold = LgColors.Gold
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(LgColors.PanelSlate.copy(alpha = 0.95f), Color(0xFF041319))
                )
            )
            .drawBehind {
                val stroke = 2.4f
                val cornerLen = 28f
                val w = size.width
                val h = size.height

                // Outer subtle turquoise frame
                drawRect(
                    color = accentColor.copy(alpha = 0.30f),
                    topLeft = Offset(1f, 1f),
                    size = Size(w - 2f, h - 2f),
                    style = Stroke(width = 1.5f)
                )

                // Gold corner brackets
                drawLine(gold, Offset(2f, 2f), Offset(cornerLen, 2f), strokeWidth = stroke)
                drawLine(gold, Offset(2f, 2f), Offset(2f, cornerLen), strokeWidth = stroke)
                drawLine(gold, Offset(w - cornerLen, 2f), Offset(w - 2f, 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - 2f, 2f), Offset(w - 2f, cornerLen), strokeWidth = stroke)
                drawLine(gold, Offset(2f, h - 2f), Offset(cornerLen, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(2f, h - cornerLen), Offset(2f, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - cornerLen, h - 2f), Offset(w - 2f, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - 2f, h - cornerLen), Offset(w - 2f, h - 2f), strokeWidth = stroke)

                // Mid-rail turquoise circuit glow strips
                drawLine(
                    color = accentColor.copy(alpha = 0.78f),
                    start = Offset(2f, h * 0.28f),
                    end = Offset(2f, h * 0.72f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.78f),
                    start = Offset(w - 2f, h * 0.28f),
                    end = Offset(w - 2f, h * 0.72f),
                    strokeWidth = 3f
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
                        color = LgColors.Gold.copy(alpha = 0.5f)
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = LgColors.Gold,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = LgColors.Gold.copy(alpha = 0.5f)
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
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Color(0xFF071318) else LgColors.InkMuted,
            fontWeight = FontWeight.ExtraBold,
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
 * stablecoins (USDT, USDC) and PoW/staking/DEX tokens (ALEO, KAS, ETC, XMR, BNB, etc.).
 */
@Composable
fun CoinAvatar(
    symbol: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val cleanSym = symbol.trim().uppercase()
        .removeSuffix("USD")
        .removeSuffix("-USD")
        .removeSuffix("/USD")
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
        CustomCoinLogoBadge(
            symbol = cleanSym,
            size = size,
            modifier = modifier
        )
    }
}

@Composable
private fun CustomCoinLogoBadge(
    symbol: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val palette = when (symbol) {
        "USDT" -> Pair(Color(0xFF1BA27A), Color(0xFF0C5C45))
        "USDC" -> Pair(Color(0xFF2775CA), Color(0xFF14427A))
        "ALEO" -> Pair(LgColors.Turquoise, LgColors.Gold)
        "KAS" -> Pair(Color(0xFF49EACB), Color(0xFF0E534A))
        "ETC" -> Pair(Color(0xFF3AB83A), Color(0xFF144D23))
        "XMR" -> Pair(Color(0xFFFF6600), Color(0xFF282B30))
        "BNB" -> Pair(Color(0xFFF3BA2F), Color(0xFF6B4E09))
        "DASH" -> Pair(Color(0xFF008CE7), Color(0xFF0A3866))
        else -> Pair(LgColors.Turquoise, LgColors.Gold)
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
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.45f).sp,
                    color = Color.White
                )
            }
            "ALEO" -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val p = Path().apply {
                        moveTo(w * 0.50f, h * 0.12f)
                        lineTo(w * 0.85f, h * 0.85f)
                        lineTo(w * 0.67f, h * 0.85f)
                        lineTo(w * 0.50f, h * 0.44f)
                        lineTo(w * 0.33f, h * 0.85f)
                        lineTo(w * 0.15f, h * 0.85f)
                        close()
                    }
                    drawPath(p, color = LgColors.Gold)
                    drawCircle(color = LgColors.Turquoise, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.70f))
                }
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
                    fontFamily = ChakraPetchFontFamily,
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
 * Custom vector/Canvas Sports League Logos for NFL, NBA, NHL, MLB, EPL (Soccer), and UFC (MMA).
 */
@Composable
fun SportsLeagueLogo(
    league: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val code = league.trim().uppercase()
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
                    fontFamily = ChakraPetchFontFamily,
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
                    fontFamily = ChakraPetchFontFamily,
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
                    fontFamily = ChakraPetchFontFamily,
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
                    fontFamily = ChakraPetchFontFamily,
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
                    fontFamily = ChakraPetchFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.28f).sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = size * 0.36f)
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
                    fontFamily = ChakraPetchFontFamily,
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

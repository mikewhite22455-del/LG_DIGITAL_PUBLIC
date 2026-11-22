package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.CoinAssets
import com.example.domain.models.SignalAction
import com.example.ui.theme.LgColors

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
            containerColor = LgColors.PanelSlate
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
    accentColor: Color = LgColors.CyanGlow,
    content: @Composable ColumnScope.() -> Unit
) {
    val gold = LgColors.Gold
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(LgColors.PanelSlate, Color(0xFF050F16))
                )
            )
            .drawBehind {
                val stroke = 2.2f
                val cornerLen = 28f
                val w = size.width
                val h = size.height

                // Outer subtle cyan frame
                drawRect(
                    color = accentColor.copy(alpha = 0.28f),
                    topLeft = Offset(1f, 1f),
                    size = Size(w - 2f, h - 2f),
                    style = Stroke(width = 1.5f)
                )

                // Gold corner brackets
                // Top-left
                drawLine(gold, Offset(2f, 2f), Offset(cornerLen, 2f), strokeWidth = stroke)
                drawLine(gold, Offset(2f, 2f), Offset(2f, cornerLen), strokeWidth = stroke)
                // Top-right
                drawLine(gold, Offset(w - cornerLen, 2f), Offset(w - 2f, 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - 2f, 2f), Offset(w - 2f, cornerLen), strokeWidth = stroke)
                // Bottom-left
                drawLine(gold, Offset(2f, h - 2f), Offset(cornerLen, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(2f, h - cornerLen), Offset(2f, h - 2f), strokeWidth = stroke)
                // Bottom-right
                drawLine(gold, Offset(w - cornerLen, h - 2f), Offset(w - 2f, h - 2f), strokeWidth = stroke)
                drawLine(gold, Offset(w - 2f, h - cornerLen), Offset(w - 2f, h - 2f), strokeWidth = stroke)

                // Mid-rail cyan circuit glow strips
                drawLine(
                    color = accentColor.copy(alpha = 0.75f),
                    start = Offset(2f, h * 0.28f),
                    end = Offset(2f, h * 0.72f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = accentColor.copy(alpha = 0.75f),
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
                        color = LgColors.Gold.copy(alpha = 0.45f)
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = LgColors.Gold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = LgColors.Gold.copy(alpha = 0.45f)
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
                        listOf(Color(0xFFF7DA84), LgColors.Gold, LgColors.GoldDark)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(LgColors.ElevatedSlate, LgColors.PanelSlate)
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) Color(0xFFFFEAB0) else LgColors.BorderHairline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Color(0xFF0A1219) else LgColors.InkMuted,
            fontWeight = FontWeight.Bold,
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
            .background(LgColors.ElevatedSlate.copy(alpha = 0.65f))
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

@Composable
fun CoinAvatar(
    symbol: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val iconRes = CoinAssets.iconFor(symbol)
    if (iconRes != null) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "$symbol icon",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, LgColors.BorderHairline, CircleShape)
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(LgColors.ElevatedSlate, Color(0xFF1E3250))
                    )
                )
                .border(1.dp, LgColors.Gold.copy(alpha = 0.55f), CircleShape)
        ) {
            Text(
                text = symbol.take(3).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.Gold,
                fontWeight = FontWeight.Bold
            )
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
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f))
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
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
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
            .background(LgColors.ElevatedSlate.copy(alpha = 0.72f))
            .border(1.dp, LgColors.BorderHairline.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
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

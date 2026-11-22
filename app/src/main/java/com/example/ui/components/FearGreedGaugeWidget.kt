package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.Formatters
import com.example.domain.models.FearGreedIndexData
import com.example.domain.models.FearGreedPoint
import com.example.domain.models.UiState
import com.example.ui.theme.LgColors
import com.example.ui.theme.RussoOneFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ExtremeFearColor = Color(0xFFFF3B5C)
private val FearColor = Color(0xFFFF8C3B)
private val ExtremeGreedColor = Color(0xFF00F5D4)

private fun fearGreedAccentFor(score: Int): Color = when {
    score < 25 -> ExtremeFearColor
    score < 45 -> FearColor
    score <= 55 -> LgColors.GoldBright
    score < 75 -> LgColors.Turquoise
    else -> ExtremeGreedColor
}

/**
 * Live-updating Crypto Fear & Greed Index speedometer gauge widget powered by
 * the external Alternative.me FNG API + real-time market momentum blending.
 */
@Composable
fun FearGreedGaugeWidget(
    fearGreedState: UiState<FearGreedIndexData>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(
        title = "CRYPTO FEAR & GREED INDEX",
        modifier = modifier
            .fillMaxWidth()
            .testTag("fear_greed_gauge_widget")
    ) {
        // Live status + manual refresh button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveAutoFeedBadge(label = "AUTO")
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("refresh_fear_greed_button")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LgColors.GoldBright
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Crypto Fear & Greed Index",
                        tint = LgColors.GoldBright
                    )
                }
            }
        }

        when (fearGreedState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = LgColors.Turquoise)
                        Text(
                            text = "Fetching live Fear & Greed Index...",
                            style = MaterialTheme.typography.labelMedium,
                            color = LgColors.InkSecondary
                        )
                    }
                }
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LgColors.ElevatedSlate.copy(alpha = 0.7f))
                        .border(1.dp, LgColors.Crimson.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = fearGreedState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.Crimson
                    )
                    GoldMetallicButton(
                        text = "Retry Live Feed",
                        onClick = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("retry_fear_greed_button")
                    )
                }
            }

            is UiState.Success -> {
                FearGreedLoadedContent(data = fearGreedState.data)
            }
        }
    }
}

@Composable
private fun FearGreedLoadedContent(data: FearGreedIndexData) {
    val accent = fearGreedAccentFor(data.liveValue)
    val animatedScore by animateFloatAsState(
        targetValue = data.liveValue.toFloat().coerceIn(0f, 100f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fear_greed_needle_score"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Semicircle 5-zone segmented gauge + animated metallic Gold needle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LgColors.ObsidianBg.copy(alpha = 0.94f),
                            LgColors.PanelSlate.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(1.dp, LgColors.Gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 18.dp)
            ) {
                val strokeWidth = 22f
                val maxDiameter = minOf(size.width * 0.86f, size.height * 1.66f)
                val radius = maxDiameter / 2f
                val centerX = size.width / 2f
                val centerY = size.height - 8f
                val topLeft = Offset(centerX - radius, centerY - radius)
                val arcSize = Size(maxDiameter, maxDiameter)

                // Ambient glow behind active regime
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY - radius * 0.25f),
                        radius = radius * 0.95f
                    ),
                    center = Offset(centerX, centerY - radius * 0.25f),
                    radius = radius * 0.95f
                )

                // Dark recessed track
                drawArc(
                    color = Color(0xFF0A212B),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth + 6f, cap = StrokeCap.Round)
                )

                // 5 color-coded Fear & Greed zones with crisp gaps
                val zones = listOf(
                    Triple(180f, 41.5f, ExtremeFearColor), // 0..24
                    Triple(224f, 34f, FearColor),          // 25..44
                    Triple(260.5f, 19f, LgColors.GoldBright),     // 45..55
                    Triple(282f, 33.5f, LgColors.Turquoise),      // 56..74
                    Triple(318f, 42f, ExtremeGreedColor)   // 75..100
                )
                zones.forEach { (startDeg, sweepDeg, zoneColor) ->
                    drawArc(
                        color = zoneColor.copy(alpha = 0.22f),
                        startAngle = startDeg,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth + 8f, cap = StrokeCap.Butt)
                    )
                    drawArc(
                        color = zoneColor,
                        startAngle = startDeg,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                }

                // Inner precision ticks (0, 25, 50, 75, 100 + minor ticks)
                for (i in 0..20) {
                    val deg = 180.0 + (i / 20.0) * 180.0
                    val rad = deg * PI / 180.0
                    val isMajor = i % 5 == 0
                    val outerR = radius - strokeWidth * 0.72f
                    val innerR = outerR - if (isMajor) 14f else 7f
                    drawLine(
                        color = if (isMajor) LgColors.GoldBright.copy(alpha = 0.85f)
                        else LgColors.InkMuted.copy(alpha = 0.45f),
                        start = Offset(
                            centerX + (outerR * cos(rad)).toFloat(),
                            centerY + (outerR * sin(rad)).toFloat()
                        ),
                        end = Offset(
                            centerX + (innerR * cos(rad)).toFloat(),
                            centerY + (innerR * sin(rad)).toFloat()
                        ),
                        strokeWidth = if (isMajor) 2.6f else 1.4f,
                        cap = StrokeCap.Round
                    )
                }

                // Animated metallic Gold needle
                val needleDeg = 180.0 + (animatedScore / 100.0) * 180.0
                val needleRad = needleDeg * PI / 180.0
                val needleLen = radius - strokeWidth * 0.45f
                val tip = Offset(
                    centerX + (needleLen * cos(needleRad)).toFloat(),
                    centerY + (needleLen * sin(needleRad)).toFloat()
                )
                val pivot = Offset(centerX, centerY)

                // Subtle needle glow
                drawLine(
                    color = accent.copy(alpha = 0.42f),
                    start = pivot,
                    end = tip,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(LgColors.GoldDark, LgColors.GoldBright, Color.White),
                        start = pivot,
                        end = tip
                    ),
                    start = pivot,
                    end = tip,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                // Glowing tip dot + Gold pivot hub
                drawCircle(color = accent, radius = 6.5f, center = tip)
                drawCircle(color = LgColors.GoldBright, radius = 12f, center = pivot)
                drawCircle(color = LgColors.ObsidianBg, radius = 5.5f, center = pivot)
            }

            // Left / Right scale markers + Centered Hero Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "0 FEAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExtremeFearColor
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${data.liveValue}",
                        fontFamily = RussoOneFontFamily,
                        fontSize = 34.sp,
                        color = accent,
                        modifier = Modifier.testTag("fear_greed_score_value")
                    )
                    StatusPill(
                        text = data.liveClassification.uppercase(),
                        color = accent
                    )
                }
                Text(
                    text = "100 GREED",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExtremeGreedColor
                )
            }
        }

        // Breakdown tiles: Official External API vs Yesterday vs 7D Ago
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pulseLabel = when {
                data.liveMomentumAdjustment > 0 -> "${data.apiValue} (+${data.liveMomentumAdjustment} live)"
                data.liveMomentumAdjustment < 0 -> "${data.apiValue} (${data.liveMomentumAdjustment} live)"
                else -> "${data.apiValue} · ${data.apiClassification}"
            }
            MetricTile(
                label = "Index Base",
                value = pulseLabel,
                valueColor = LgColors.GoldBright,
                modifier = Modifier.weight(1.2f)
            )
            val yestText = data.yesterdayValue?.let { y ->
                val diff = data.liveValue - y
                val sign = if (diff >= 0) "+" else ""
                "$y ($sign$diff)"
            } ?: "--"
            MetricTile(
                label = "Yesterday",
                value = yestText,
                valueColor = data.yesterdayValue?.let { fearGreedAccentFor(it) } ?: LgColors.InkMuted,
                modifier = Modifier.weight(1f)
            )
            val weekText = data.lastWeekValue?.let { w ->
                val diff = data.liveValue - w
                val sign = if (diff >= 0) "+" else ""
                "$w ($sign$diff)"
            } ?: "--"
            MetricTile(
                label = "7D Ago",
                value = weekText,
                valueColor = data.lastWeekValue?.let { fearGreedAccentFor(it) } ?: LgColors.InkMuted,
                modifier = Modifier.weight(1f)
            )
        }

        // 7-Day Sentiment Trend Strip
        if (data.history7d.size >= 2) {
            FearGreed7DayStrip(points = data.history7d)
        }

        // Attribution + Live Timestamp Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Source: ${data.source}",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.InkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            val updatedStamp = Formatters.formatHourMinute(data.updatedAtMs)
            Text(
                text = "Updated $updatedStamp",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.Turquoise
            )
        }
    }
}

@Composable
private fun FearGreed7DayStrip(points: List<FearGreedPoint>) {
    val dayFmt = SimpleDateFormat("EEE", Locale.US)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ElevatedSlate.copy(alpha = 0.62f))
            .border(1.dp, LgColors.BorderHairline.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "7-DAY FEAR & GREED TREND",
            style = MaterialTheme.typography.labelSmall,
            color = LgColors.Gold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            points.takeLast(7).forEachIndexed { idx, pt ->
                val barColor = fearGreedAccentFor(pt.value)
                val barHeightDp = (12f + (pt.value / 100f) * 32f).dp
                val isLatest = idx == points.takeLast(7).lastIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "${pt.value}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLatest) LgColors.GoldBright else barColor,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeightDp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(barColor, barColor.copy(alpha = 0.35f))
                                )
                            )
                            .border(
                                width = if (isLatest) 1.2.dp else 0.5.dp,
                                color = if (isLatest) LgColors.GoldBright else barColor.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                    )
                    Text(
                        text = if (isLatest) "NOW" else dayFmt.format(Date(pt.timestampSec * 1000L)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLatest) LgColors.Turquoise else LgColors.InkMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

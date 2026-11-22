package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.engine.TechnicalEngine
import com.example.domain.models.Candle
import com.example.domain.models.ChartOverlay
import com.example.ui.theme.LgColors
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    overlay: ChartOverlay,
    modifier: Modifier = Modifier
) {
    val visible = remember(candles) { candles.takeLast(80) }
    var inspectedIndex by remember(visible) { mutableStateOf<Int?>(null) }

    if (visible.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(LgColors.ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No OHLC candles loaded", color = LgColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val closes = remember(visible) { visible.map { it.close } }
    val sma9 = remember(closes, overlay) {
        if (overlay == ChartOverlay.SMA_CROSS) TechnicalEngine.smaSeries(closes, 9) else emptyList()
    }
    val sma21 = remember(closes, overlay) {
        if (overlay == ChartOverlay.SMA_CROSS) TechnicalEngine.smaSeries(closes, 21) else emptyList()
    }
    val ema12 = remember(closes, overlay) {
        if (overlay == ChartOverlay.EMA_CROSS) TechnicalEngine.emaSeries(closes, 12) else emptyList()
    }
    val ema26 = remember(closes, overlay) {
        if (overlay == ChartOverlay.EMA_CROSS) TechnicalEngine.emaSeries(closes, 26) else emptyList()
    }
    val bollinger = remember(closes, overlay) {
        if (overlay == ChartOverlay.BOLLINGER) TechnicalEngine.bollingerSeries(closes, 20, 2.0) else emptyList()
    }
    val supertrend = remember(visible, overlay) {
        if (overlay == ChartOverlay.SUPERTREND) TechnicalEngine.supertrendSeries(visible, 10, 3.0) else emptyList()
    }

    val activeCandle = visible[inspectedIndex?.coerceIn(0, visible.lastIndex) ?: visible.lastIndex]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.92f))
            .border(1.2.dp, LgColors.Gold.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "O ${Formatters.formatUsd(activeCandle.open)}  H ${Formatters.formatUsd(activeCandle.high)}  L ${Formatters.formatUsd(activeCandle.low)}  C ${Formatters.formatUsd(activeCandle.close)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (activeCandle.close >= activeCandle.open) LgColors.Turquoise else LgColors.Crimson
            )
            Text(
                text = Formatters.formatTimeShort(activeCandle.timestampSec * 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.InkMuted
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .pointerInput(visible) {
                    detectTapGestures { offset ->
                        val step = size.width.toFloat() / max(visible.size, 1)
                        val idx = (offset.x / step).toInt().coerceIn(0, visible.lastIndex)
                        inspectedIndex = idx
                    }
                }
                .pointerInput(visible) {
                    detectHorizontalDragGestures(
                        onDragEnd = { inspectedIndex = null },
                        onDragCancel = { inspectedIndex = null }
                    ) { change, _ ->
                        val step = size.width.toFloat() / max(visible.size, 1)
                        val idx = (change.position.x / step).toInt().coerceIn(0, visible.lastIndex)
                        inspectedIndex = idx
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val priceHeight = h * 0.80f
            val volTop = h * 0.84f
            val volHeight = h * 0.16f

            var minPrice = visible.minOf { it.low }
            var maxPrice = visible.maxOf { it.high }
            if (overlay == ChartOverlay.BOLLINGER) {
                bollinger.forEach { b ->
                    b.upper?.let { maxPrice = max(maxPrice, it) }
                    b.lower?.let { minPrice = min(minPrice, it) }
                }
            }
            val pad = (maxPrice - minPrice).coerceAtLeast(maxPrice * 0.004) * 0.08
            minPrice -= pad
            maxPrice += pad
            val priceSpan = max(maxPrice - minPrice, 1e-9)
            val maxVol = visible.maxOf { it.volume }.coerceAtLeast(1e-9)

            fun yForPrice(p: Double): Float {
                return (priceHeight - ((p - minPrice) / priceSpan * priceHeight)).toFloat()
            }

            // Grid lines
            for (g in 0..3) {
                val y = priceHeight * (g / 3f)
                drawLine(
                    color = LgColors.BorderHairline.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            val candleStep = w / visible.size
            val bodyWidth = max(candleStep * 0.62f, 2f)

            visible.forEachIndexed { i, c ->
                val centerX = (i + 0.5f) * candleStep
                val isBull = c.close >= c.open
                val color = if (isBull) LgColors.Turquoise else LgColors.Crimson

                // Wick
                drawLine(
                    color = color.copy(alpha = 0.85f),
                    start = Offset(centerX, yForPrice(c.high)),
                    end = Offset(centerX, yForPrice(c.low)),
                    strokeWidth = max(1.2f, bodyWidth * 0.16f)
                )

                // Body
                val yOpen = yForPrice(c.open)
                val yClose = yForPrice(c.close)
                val top = min(yOpen, yClose)
                val bodyH = max(kotlin.math.abs(yClose - yOpen), 2f)
                drawRect(
                    color = color,
                    topLeft = Offset(centerX - bodyWidth / 2f, top),
                    size = Size(bodyWidth, bodyH)
                )

                // Volume bar
                val vH = ((c.volume / maxVol) * volHeight).toFloat().coerceAtLeast(1.5f)
                drawRect(
                    color = color.copy(alpha = 0.28f),
                    topLeft = Offset(centerX - bodyWidth / 2f, volTop + (volHeight - vH)),
                    size = Size(bodyWidth, vH)
                )
            }

            fun drawLineOverlay(series: List<Double?>, color: Color, strokePx: Float = 2.4f) {
                val path = Path()
                var started = false
                series.forEachIndexed { idx, v ->
                    if (v != null) {
                        val x = (idx + 0.5f) * candleStep
                        val y = yForPrice(v)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }
                if (started) {
                    drawPath(path = path, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round))
                }
            }

            when (overlay) {
                ChartOverlay.NONE -> Unit
                ChartOverlay.SMA_CROSS -> {
                    drawLineOverlay(sma9, LgColors.CyanGlow)
                    drawLineOverlay(sma21, LgColors.Gold)
                }
                ChartOverlay.EMA_CROSS -> {
                    drawLineOverlay(ema12, LgColors.Turquoise)
                    drawLineOverlay(ema26, LgColors.AmberWarn)
                }
                ChartOverlay.BOLLINGER -> {
                    drawLineOverlay(bollinger.map { it.upper }, LgColors.CyanGlow.copy(alpha = 0.8f), 1.8f)
                    drawLineOverlay(bollinger.map { it.mid }, LgColors.Gold.copy(alpha = 0.85f), 2.0f)
                    drawLineOverlay(bollinger.map { it.lower }, LgColors.CyanGlow.copy(alpha = 0.8f), 1.8f)
                }
                ChartOverlay.SUPERTREND -> {
                    for (i in 1 until supertrend.size) {
                        val prev = supertrend[i - 1]
                        val curr = supertrend[i]
                        if (prev.value != null && curr.value != null) {
                            val c = if (curr.bullish) LgColors.Turquoise else LgColors.Crimson
                            drawLine(
                                color = c,
                                start = Offset((i - 0.5f) * candleStep, yForPrice(prev.value)),
                                end = Offset((i + 0.5f) * candleStep, yForPrice(curr.value)),
                                strokeWidth = 2.8f
                            )
                        }
                    }
                }
            }

            inspectedIndex?.let { idx ->
                val x = (idx.coerceIn(0, visible.lastIndex) + 0.5f) * candleStep
                drawLine(
                    color = LgColors.Gold.copy(alpha = 0.65f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

@Composable
fun EquitySparklineChart(
    curve: List<Double>,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    if (curve.size < 2) return
    val strokeColor = if (positive) LgColors.Turquoise else LgColors.Crimson
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.92f))
            .border(1.dp, LgColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minV = curve.minOrNull() ?: 0.0
            val maxV = curve.maxOrNull() ?: 1.0
            val span = max(maxV - minV, 1e-6)
            val w = size.width
            val h = size.height
            val step = w / max(curve.size - 1, 1)

            val linePath = Path()
            val fillPath = Path()
            curve.forEachIndexed { idx, v ->
                val x = idx * step
                val y = (h - ((v - minV) / span * h)).toFloat()
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo((curve.size - 1) * step, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(strokeColor.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(width = 2.6f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun HourlyTrendSparkline(
    prices: List<Double>,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (positive) LgColors.Turquoise else LgColors.Crimson
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        if (prices.size < 2) {
            drawLine(
                color = LgColors.BorderHairline,
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val minV = prices.minOrNull() ?: 0.0
        val maxV = prices.maxOrNull() ?: 1.0
        val span = max(maxV - minV, maxV * 1e-6)
        val padY = h * 0.12f
        val usableH = (h - padY * 2f).coerceAtLeast(1f)
        val step = w / max(prices.size - 1, 1)

        fun pointAt(idx: Int): Offset {
            val x = idx * step
            val norm = ((prices[idx] - minV) / span).toFloat().coerceIn(0f, 1f)
            val y = padY + usableH * (1f - norm)
            return Offset(x, y)
        }

        val points = prices.indices.map { pointAt(it) }

        // Subtle baseline reference at 24h-ago opening price
        val startY = points.first().y
        drawLine(
            color = LgColors.BorderHairline.copy(alpha = 0.55f),
            start = Offset(0f, startY),
            end = Offset(w, startY),
            strokeWidth = 1.2f
        )

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val cx = (p0.x + p1.x) * 0.5f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, h)
            lineTo(points.first().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(strokeColor.copy(alpha = 0.30f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )

        val endPt = points.last()
        drawCircle(
            color = strokeColor.copy(alpha = 0.35f),
            radius = 6f,
            center = endPt
        )
        drawCircle(
            color = strokeColor,
            radius = 3.2f,
            center = endPt
        )
    }
}

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.models.EquityCurvePoint
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.SignalAction
import com.example.domain.models.SimulatedSignalMarker
import com.example.domain.models.TechnicalIndicatorType
import com.example.ui.theme.LgColors
import kotlin.math.abs
import kotlin.math.max

/**
 * Interactive historical Price & Signals chart that overlays active Moving Averages (SMA/EMA),
 * Bollinger Bands, winning/losing trade spans, and directional BUY (▲) / SELL (▼) signal markers.
 */
@Composable
fun BacktestPriceSignalsCanvas(
    points: List<EquityCurvePoint>,
    markers: List<SimulatedSignalMarker>,
    config: IndicatorBacktestConfig,
    inspectedIndex: Int?,
    onInspectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val showSma = TechnicalIndicatorType.SMA_CROSS in config.selectedIndicators
    val showEma = TechnicalIndicatorType.EMA_CROSS in config.selectedIndicators
    val showBollinger = TechnicalIndicatorType.BOLLINGER in config.selectedIndicators

    val prices = remember(points, showSma, showEma, showBollinger) {
        buildList {
            points.forEach { pt ->
                add(pt.priceUsd)
                if (showSma) {
                    pt.smaFast?.let { add(it) }
                    pt.smaSlow?.let { add(it) }
                }
                if (showEma) {
                    pt.emaFast?.let { add(it) }
                    pt.emaSlow?.let { add(it) }
                }
                if (showBollinger) {
                    pt.bbUpper?.let { add(it) }
                    pt.bbLower?.let { add(it) }
                }
            }
        }
    }
    val minP = remember(prices) { prices.minOrNull() ?: 0.0 }
    val maxP = remember(prices) { prices.maxOrNull() ?: 1.0 }
    val midP = remember(minP, maxP) { (minP + maxP) * 0.5 }

    // Pair consecutive BUY -> SELL markers to shade trade holding periods
    val tradeSpans = remember(markers) {
        val spans = mutableListOf<Triple<Int, Int, Boolean>>()
        var openBuyIdx: Int? = null
        markers.forEach { m ->
            if (m.action == SignalAction.BUY) {
                openBuyIdx = m.candleIndex
            } else if (m.action == SignalAction.SELL && openBuyIdx != null) {
                val win = (m.pnlUsd ?: 0.0) >= 0.0
                spans += Triple(openBuyIdx!!, m.candleIndex, win)
                openBuyIdx = null
            }
        }
        spans
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LgColors.ObsidianBg)
            .border(1.2.dp, LgColors.Turquoise.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("backtest_signals_canvas")
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val step = size.width.toFloat() / max(points.size - 1, 1)
                        val idx = ((offset.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                        onInspectIndex(if (inspectedIndex == idx) null else idx)
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragEnd = { },
                        onDragCancel = { }
                    ) { change, _ ->
                        val step = size.width.toFloat() / max(points.size - 1, 1)
                        val idx = ((change.position.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                        onInspectIndex(idx)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || points.size < 2) return@Canvas

            val span = max(maxP - minP, maxP * 0.004)
            val lo = minP - span * 0.10
            val hi = maxP + span * 0.10
            val fullSpan = max(hi - lo, 1e-9)

            fun xFor(i: Int): Float = (i.toFloat() / (points.size - 1)) * w
            fun yFor(v: Double): Float {
                val n = ((v - lo) / fullSpan).toFloat().coerceIn(0f, 1f)
                return h - n * h
            }

            // Subtle horizontal gridlines
            listOf(0.25f, 0.50f, 0.75f).forEach { frac ->
                val y = h * frac
                drawLine(
                    color = LgColors.BorderHairline.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Trade hold span shading (turquoise for winning trades, crimson for losses)
            tradeSpans.forEach { (startIdx, endIdx, isWin) ->
                val sx = xFor(startIdx.coerceIn(0, points.lastIndex))
                val ex = xFor(endIdx.coerceIn(0, points.lastIndex))
                if (ex > sx) {
                    drawRect(
                        color = (if (isWin) LgColors.Turquoise else LgColors.Crimson).copy(alpha = 0.09f),
                        topLeft = Offset(sx, 0f),
                        size = Size(ex - sx, h)
                    )
                }
            }

            // Bollinger Bands shaded envelope
            if (showBollinger) {
                val upperPts = mutableListOf<Offset>()
                val lowerPts = mutableListOf<Offset>()
                points.forEachIndexed { idx, pt ->
                    val u = pt.bbUpper
                    val l = pt.bbLower
                    if (u != null && l != null) {
                        val x = xFor(idx)
                        upperPts += Offset(x, yFor(u))
                        lowerPts += Offset(x, yFor(l))
                    }
                }
                if (upperPts.size >= 2) {
                    val upperPath = Path().apply {
                        moveTo(upperPts.first().x, upperPts.first().y)
                        upperPts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    val lowerPath = Path().apply {
                        moveTo(lowerPts.first().x, lowerPts.first().y)
                        lowerPts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    val channelPath = Path().apply {
                        addPath(upperPath)
                        lowerPts.reversed().forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(channelPath, color = LgColors.Gold.copy(alpha = 0.08f))
                    drawPath(upperPath, color = LgColors.Gold.copy(alpha = 0.55f), style = Stroke(width = 1.6f))
                    drawPath(lowerPath, color = LgColors.Gold.copy(alpha = 0.55f), style = Stroke(width = 1.6f))
                }
            }

            fun drawIndicatorSeries(
                selector: (EquityCurvePoint) -> Double?,
                color: Color,
                strokeWidth: Float,
                dashed: Boolean = false
            ) {
                val path = Path()
                var started = false
                points.forEachIndexed { idx, pt ->
                    val v = selector(pt)
                    if (v != null) {
                        val x = xFor(idx)
                        val y = yFor(v)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }
                if (started) {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f) else null
                        )
                    )
                }
            }

            // Moving Average overlays
            if (showSma) {
                drawIndicatorSeries({ it.smaFast }, LgColors.GoldBright, 2.4f, dashed = false)
                drawIndicatorSeries({ it.smaSlow }, Color(0xFFFF8A3D), 2.1f, dashed = true)
            }
            if (showEma) {
                drawIndicatorSeries({ it.emaFast }, Color(0xFF42F5E9), 2.4f, dashed = false)
                drawIndicatorSeries({ it.emaSlow }, Color(0xFFD06BFF), 2.1f, dashed = true)
            }

            // Historical Close Price line + subtle depth gradient
            val pricePath = Path().apply {
                points.forEachIndexed { idx, pt ->
                    val x = xFor(idx)
                    val y = yFor(pt.priceUsd)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            val areaPath = Path().apply {
                addPath(pricePath)
                lineTo(xFor(points.lastIndex), h)
                lineTo(xFor(0), h)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    listOf(LgColors.CyanGlow.copy(alpha = 0.16f), Color.Transparent)
                )
            )
            drawPath(
                path = pricePath,
                color = LgColors.CyanGlow,
                style = Stroke(width = 3.0f, cap = StrokeCap.Round)
            )

            // Directional BUY (▲) and SELL (▼) signal markers
            markers.forEach { m ->
                val idx = m.candleIndex.coerceIn(0, points.lastIndex)
                val cx = xFor(idx)
                val cy = yFor(m.priceUsd)
                val isBuy = m.action == SignalAction.BUY
                val markerColor = if (isBuy) LgColors.Turquoise else LgColors.Crimson

                // Glow halo on the price line
                drawCircle(color = markerColor.copy(alpha = 0.28f), radius = 12f, center = Offset(cx, cy))
                drawCircle(color = LgColors.ObsidianBg, radius = 5.5f, center = Offset(cx, cy))
                drawCircle(color = markerColor, radius = 3.8f, center = Offset(cx, cy))

                // Crisp triangle arrow slightly below BUY / above SELL
                val triPath = Path().apply {
                    if (isBuy) {
                        val tipY = (cy + 8f).coerceAtMost(h - 14f)
                        moveTo(cx, tipY)
                        lineTo(cx - 8f, tipY + 11f)
                        lineTo(cx + 8f, tipY + 11f)
                        close()
                    } else {
                        val tipY = (cy - 8f).coerceAtLeast(14f)
                        moveTo(cx, tipY)
                        lineTo(cx - 8f, tipY - 11f)
                        lineTo(cx + 8f, tipY - 11f)
                        close()
                    }
                }
                drawPath(path = triPath, color = markerColor)
            }

            // Scrubbing crosshair
            inspectedIndex?.let { rawIdx ->
                val idx = rawIdx.coerceIn(0, points.lastIndex)
                val x = xFor(idx)
                val y = yFor(points[idx].priceUsd)
                drawLine(
                    color = LgColors.GoldBright.copy(alpha = 0.78f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                )
                drawCircle(color = LgColors.GoldBright, radius = 6.5f, center = Offset(x, y))
                drawCircle(color = LgColors.ObsidianBg, radius = 3f, center = Offset(x, y))
            }
        }

        // Right-aligned price axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(Formatters.formatUsd(maxP), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
            Text(Formatters.formatUsd(midP), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted.copy(alpha = 0.7f))
            Text(Formatters.formatUsd(minP), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
        }
    }
}

/**
 * Synchronized Oscillator Sub-Chart (RSI overbought/oversold bands and/or MACD Histogram)
 * aligned horizontally with BacktestPriceSignalsCanvas.
 */
@Composable
fun BacktestOscillatorSubChart(
    points: List<EquityCurvePoint>,
    config: IndicatorBacktestConfig,
    inspectedIndex: Int?,
    onInspectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val showRsi = TechnicalIndicatorType.RSI in config.selectedIndicators
    val showMacd = TechnicalIndicatorType.MACD in config.selectedIndicators
    if (!showRsi && !showMacd) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showRsi) {
            val focusRsi = inspectedIndex?.let { points.getOrNull(it)?.rsiValue } ?: points.lastOrNull()?.rsiValue
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LgColors.ObsidianBg)
                    .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RSI (${config.rsiPeriod}) · Buy <= ${config.rsiOversold.toInt()} / Sell >= ${config.rsiOverbought.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LgColors.Gold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = focusRsi?.let { "%.1f".format(it) } ?: "--",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            focusRsi == null -> LgColors.InkMuted
                            focusRsi <= config.rsiOversold -> LgColors.Turquoise
                            focusRsi >= config.rsiOverbought -> LgColors.Crimson
                            else -> LgColors.CyanGlow
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .testTag("backtest_rsi_subchart")
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val step = size.width.toFloat() / max(points.size - 1, 1)
                                val idx = ((offset.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                                onInspectIndex(if (inspectedIndex == idx) null else idx)
                            }
                        }
                        .pointerInput(points) {
                            detectHorizontalDragGestures { change, _ ->
                                val step = size.width.toFloat() / max(points.size - 1, 1)
                                val idx = ((change.position.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                                onInspectIndex(idx)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f || points.size < 2) return@Canvas

                    fun xFor(i: Int): Float = (i.toFloat() / (points.size - 1)) * w
                    fun yForRsi(v: Double): Float = h - ((v.coerceIn(0.0, 100.0) / 100.0).toFloat() * h)

                    val obY = yForRsi(config.rsiOverbought)
                    val osY = yForRsi(config.rsiOversold)

                    // Shaded overbought & oversold threshold zones
                    drawRect(
                        color = LgColors.Crimson.copy(alpha = 0.12f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, obY)
                    )
                    drawRect(
                        color = LgColors.Turquoise.copy(alpha = 0.12f),
                        topLeft = Offset(0f, osY),
                        size = Size(w, h - osY)
                    )
                    drawLine(
                        color = LgColors.Crimson.copy(alpha = 0.65f),
                        start = Offset(0f, obY),
                        end = Offset(w, obY),
                        strokeWidth = 1.4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                    drawLine(
                        color = LgColors.Turquoise.copy(alpha = 0.65f),
                        start = Offset(0f, osY),
                        end = Offset(w, osY),
                        strokeWidth = 1.4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    val rsiPath = Path()
                    var started = false
                    points.forEachIndexed { idx, pt ->
                        val v = pt.rsiValue
                        if (v != null) {
                            val x = xFor(idx)
                            val y = yForRsi(v)
                            if (!started) {
                                rsiPath.moveTo(x, y)
                                started = true
                            } else {
                                rsiPath.lineTo(x, y)
                            }
                        }
                    }
                    if (started) {
                        drawPath(rsiPath, color = LgColors.Turquoise, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
                    }

                    inspectedIndex?.let { rawIdx ->
                        val idx = rawIdx.coerceIn(0, points.lastIndex)
                        val x = xFor(idx)
                        drawLine(
                            color = LgColors.GoldBright.copy(alpha = 0.7f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.4f
                        )
                        points[idx].rsiValue?.let { v ->
                            drawCircle(color = LgColors.GoldBright, radius = 4.8f, center = Offset(x, yForRsi(v)))
                        }
                    }
                }
            }
        }

        if (showMacd) {
            val focusHist = inspectedIndex?.let { points.getOrNull(it)?.macdHistogram } ?: points.lastOrNull()?.macdHistogram
            val maxAbsHist = remember(points) {
                (points.mapNotNull { it.macdHistogram?.let(::abs) }.maxOrNull() ?: 1.0).coerceAtLeast(1e-6)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LgColors.ObsidianBg)
                    .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MACD Histogram (${config.macdFast}/${config.macdSlow}/${config.macdSignal})",
                        style = MaterialTheme.typography.labelSmall,
                        color = LgColors.Gold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = focusHist?.let { "%+.2f".format(it) } ?: "--",
                        style = MaterialTheme.typography.labelSmall,
                        color = if ((focusHist ?: 0.0) >= 0.0) LgColors.Turquoise else LgColors.Crimson,
                        fontWeight = FontWeight.Bold
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .testTag("backtest_macd_subchart")
                ) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f || points.size < 2) return@Canvas
                    val zeroY = h * 0.5f
                    drawLine(
                        color = LgColors.BorderHairline.copy(alpha = 0.65f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.2f
                    )
                    val barStep = w / points.size.toFloat()
                    val barW = (barStep * 0.72f).coerceIn(1.5f, 8f)
                    points.forEachIndexed { idx, pt ->
                        val hist = pt.macdHistogram ?: return@forEachIndexed
                        val norm = (hist / maxAbsHist).toFloat().coerceIn(-1f, 1f)
                        val barH = abs(norm) * (h * 0.45f)
                        val x = idx * barStep
                        val topY = if (norm >= 0f) zeroY - barH else zeroY
                        drawRect(
                            color = (if (norm >= 0f) LgColors.Turquoise else LgColors.Crimson).copy(alpha = 0.80f),
                            topLeft = Offset(x, topY),
                            size = Size(barW, barH.coerceAtLeast(1f))
                        )
                    }

                    inspectedIndex?.let { rawIdx ->
                        val idx = rawIdx.coerceIn(0, points.lastIndex)
                        val x = (idx.toFloat() / (points.size - 1)) * w
                        drawLine(
                            color = LgColors.GoldBright.copy(alpha = 0.7f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.4f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Equity Curve vs Buy & Hold Benchmark canvas with gradient area fill,
 * initial capital reference line, and BUY/SELL inflection markers.
 */
@Composable
fun BacktestEquityComparisonCanvas(
    points: List<EquityCurvePoint>,
    markers: List<SimulatedSignalMarker>,
    initialCapital: Double,
    strategyColor: Color,
    inspectedIndex: Int?,
    onInspectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allVals = remember(points, initialCapital) {
        points.flatMap { listOf(it.strategyEquityUsd, it.buyHoldEquityUsd) } + initialCapital
    }
    val minVal = remember(allVals) { allVals.minOrNull() ?: initialCapital }
    val maxVal = remember(allVals) { allVals.maxOrNull() ?: (initialCapital + 1.0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LgColors.ObsidianBg)
            .border(1.2.dp, LgColors.Gold.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("backtest_equity_canvas")
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val step = size.width.toFloat() / max(points.size - 1, 1)
                        val idx = ((offset.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                        onInspectIndex(if (inspectedIndex == idx) null else idx)
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        val step = size.width.toFloat() / max(points.size - 1, 1)
                        val idx = ((change.position.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                        onInspectIndex(idx)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || points.size < 2) return@Canvas

            val span = max(maxVal - minVal, initialCapital * 0.01)
            val lo = minVal - span * 0.08
            val hi = maxVal + span * 0.08
            val fullSpan = max(hi - lo, 1e-9)

            fun xFor(i: Int): Float = (i.toFloat() / (points.size - 1)) * w
            fun yFor(v: Double): Float {
                val n = ((v - lo) / fullSpan).toFloat().coerceIn(0f, 1f)
                return h - n * h
            }

            // Initial capital dashed baseline
            val baseY = yFor(initialCapital)
            drawLine(
                color = LgColors.BorderHairline.copy(alpha = 0.7f),
                start = Offset(0f, baseY),
                end = Offset(w, baseY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            // Buy & Hold Benchmark Path
            val bhPath = Path().apply {
                points.forEachIndexed { idx, pt ->
                    val x = xFor(idx)
                    val y = yFor(pt.buyHoldEquityUsd)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = bhPath,
                color = LgColors.Gold.copy(alpha = 0.85f),
                style = Stroke(
                    width = 2.6f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Strategy Curve Path + Gradient Fill
            val mapped = points.indices.map { idx -> Offset(xFor(idx), yFor(points[idx].strategyEquityUsd)) }
            val stratPath = Path().apply {
                moveTo(mapped.first().x, mapped.first().y)
                for (i in 1 until mapped.size) {
                    val p0 = mapped[i - 1]
                    val p1 = mapped[i]
                    val cx = (p0.x + p1.x) * 0.5f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            val fillPath = Path().apply {
                addPath(stratPath)
                lineTo(mapped.last().x, h)
                lineTo(mapped.first().x, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(strategyColor.copy(alpha = 0.30f), Color.Transparent))
            )
            drawPath(
                path = stratPath,
                color = strategyColor,
                style = Stroke(width = 3.8f, cap = StrokeCap.Round)
            )

            // Trade execution dots along the equity curve
            markers.forEach { m ->
                val idx = m.candleIndex.coerceIn(0, mapped.lastIndex)
                val c = if (m.action == SignalAction.BUY) LgColors.Turquoise else LgColors.Crimson
                drawCircle(color = LgColors.ObsidianBg, radius = 5f, center = mapped[idx])
                drawCircle(color = c, radius = 3.2f, center = mapped[idx])
            }

            // Crosshair
            inspectedIndex?.let { rawIdx ->
                val idx = rawIdx.coerceIn(0, points.lastIndex)
                val x = xFor(idx)
                drawLine(
                    color = LgColors.CyanGlow.copy(alpha = 0.75f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.6f
                )
                drawCircle(color = strategyColor, radius = 6.5f, center = mapped[idx])
                drawCircle(
                    color = LgColors.GoldBright,
                    radius = 5f,
                    center = Offset(x, yFor(points[idx].buyHoldEquityUsd))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(Formatters.formatCompactUsd(maxVal), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
            Text(Formatters.formatCompactUsd(minVal), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
        }
    }
}

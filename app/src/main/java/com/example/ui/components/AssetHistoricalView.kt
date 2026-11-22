package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.models.HistoryWindow
import com.example.domain.models.MarketTicker
import com.example.domain.models.PriceHistoryPoint
import com.example.domain.models.UiState
import com.example.ui.theme.LgColors
import kotlin.math.max

@Composable
fun AssetHistoricalView(
    symbol: String,
    ticker: MarketTicker?,
    selectedWindow: HistoryWindow,
    historyState: UiState<List<PriceHistoryPoint>>,
    quickSymbols: List<String>,
    onSelectSymbol: (String) -> Unit,
    onSelectWindow: (HistoryWindow) -> Unit,
    onRetry: () -> Unit,
    onOpenInTrade: ((String) -> Unit)? = null,
    showAssetSelector: Boolean = true,
    modifier: Modifier = Modifier
) {
    val points = (historyState as? UiState.Success)?.data.orEmpty()
    var inspectedIndex by remember(points, symbol, selectedWindow) { mutableStateOf<Int?>(null) }

    val openPrice = points.firstOrNull()?.priceUsd
    val activePoint = inspectedIndex?.let { idx -> points.getOrNull(idx) } ?: points.lastOrNull()
    val displayPrice = activePoint?.priceUsd ?: ticker?.priceUsd ?: 0.0
    val periodDeltaUsd = if (openPrice != null && activePoint != null) activePoint.priceUsd - openPrice else 0.0
    val periodDeltaPct = if (openPrice != null && openPrice > 0.0 && activePoint != null) {
        ((activePoint.priceUsd - openPrice) / openPrice) * 100.0
    } else {
        ticker?.change24hPct ?: 0.0
    }
    val isPositive = periodDeltaPct >= 0.0
    val trendColor = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val selectableSymbols = remember(quickSymbols, symbol) {
        (listOf(symbol) + quickSymbols).distinct()
    }

    CyberFrameCard(
        title = "HISTORICAL PERFORMANCE · $symbol",
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_historical_view_$symbol")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Asset Identity + Active/Scrubbed Price & Window Delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinAvatar(symbol = symbol, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${ticker?.name ?: symbol} ($symbol)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (inspectedIndex != null && activePoint != null) {
                            val tsMs = activePoint.timestampSec * 1000L
                            val stamp = if (selectedWindow == HistoryWindow.D7) {
                                Formatters.formatTimeShort(tsMs)
                            } else {
                                Formatters.formatHourMinute(tsMs)
                            }
                            "${selectedWindow.subtitle} · $stamp"
                        } else {
                            "${selectedWindow.subtitle} Performance · Scrub to inspect"
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (inspectedIndex != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (displayPrice > 0.0) Formatters.formatUsd(displayPrice) else "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        text = Formatters.formatPct(periodDeltaPct),
                        color = trendColor
                    )
                }
            }

            // Asset selector dropdown with coin logos
            if (showAssetSelector && selectableSymbols.size > 1) {
                CoinDropdownSelector(
                    selectedSymbol = symbol,
                    symbols = selectableSymbols,
                    onSelectSymbol = onSelectSymbol,
                    label = "Chart Coin",
                    testTag = "history_asset_dropdown"
                )
            }

            // 1H / 24H / 7D Material 3 Segmented Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_window_selector")
            ) {
                HistoryWindow.entries.forEachIndexed { index, window ->
                    SegmentedButton(
                        selected = selectedWindow == window,
                        onClick = { onSelectWindow(window) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = HistoryWindow.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("history_window_${window.label.lowercase()}")
                    ) {
                        Text(window.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }

            // Chart Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LgColors.ObsidianBg.copy(alpha = 0.92f))
                    .border(1.2.dp, LgColors.Gold.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                when (historyState) {
                    is UiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Loading ${selectedWindow.subtitle.lowercase()} $symbol chart...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is UiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = historyState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = onRetry,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("history_retry_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(18.dp))
                                Text("Retry ${selectedWindow.label} History")
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (points.size < 2) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Insufficient data points for $symbol (${selectedWindow.label})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text("Reload Chart")
                                }
                            }
                        } else {
                            HistoricalAreaChartCanvas(
                                points = points,
                                window = selectedWindow,
                                trendColor = trendColor,
                                inspectedIndex = inspectedIndex,
                                onInspectIndex = { inspectedIndex = it }
                            )
                        }
                    }
                }
            }

            // 4-Metric Period Summary Strip
            if (points.size >= 2) {
                val periodHigh = remember(points) { points.maxOf { it.priceUsd } }
                val periodLow = remember(points) { points.minOf { it.priceUsd } }
                val openVal = points.first().priceUsd
                val closeVal = activePoint?.priceUsd ?: points.last().priceUsd
                val absDiff = closeVal - openVal
                val diffPrefix = if (absDiff >= 0.0) "+" else ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        label = "Open",
                        value = Formatters.formatUsd(openVal),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "${selectedWindow.label} High",
                        value = Formatters.formatUsd(periodHigh),
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "${selectedWindow.label} Low",
                        value = Formatters.formatUsd(periodLow),
                        valueColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Delta",
                        value = "$diffPrefix${Formatters.formatUsd(absDiff)}",
                        valueColor = trendColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (onOpenInTrade != null) {
                GoldMetallicButton(
                    text = "Open $symbol in Trade & Backtest",
                    onClick = { onOpenInTrade(symbol) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_open_trade_button")
                )
            }
        }
    }
}

@Composable
private fun HistoricalAreaChartCanvas(
    points: List<PriceHistoryPoint>,
    window: HistoryWindow,
    trendColor: Color,
    inspectedIndex: Int?,
    onInspectIndex: (Int?) -> Unit
) {
    val minPrice = remember(points) { points.minOf { it.priceUsd } }
    val maxPrice = remember(points) { points.maxOf { it.priceUsd } }
    val midPrice = (minPrice + maxPrice) * 0.5

    val formatAxisTime: (Long) -> String = { sec ->
        val ms = sec * 1000L
        if (window == HistoryWindow.D7) Formatters.formatTimeShort(ms) else Formatters.formatHourMinute(ms)
    }

    val startLabel = formatAxisTime(points.first().timestampSec)
    val midLabel = formatAxisTime(points[points.size / 2].timestampSec)
    val endLabel = formatAxisTime(points.last().timestampSec)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("historical_area_canvas")
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val step = size.width.toFloat() / max(points.size - 1, 1)
                            val idx = ((offset.x / step) + 0.5f).toInt().coerceIn(0, points.lastIndex)
                            onInspectIndex(if (inspectedIndex == idx) null else idx)
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures(
                            onDragEnd = { onInspectIndex(null) },
                            onDragCancel = { onInspectIndex(null) }
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

                val rawSpan = max(maxPrice - minPrice, maxPrice * 0.002)
                val padVal = rawSpan * 0.10
                val lowBound = minPrice - padVal
                val highBound = maxPrice + padVal
                val totalSpan = max(highBound - lowBound, 1e-9)

                fun xFor(idx: Int): Float = (idx.toFloat() / (points.size - 1)) * w
                fun yFor(price: Double): Float {
                    val norm = ((price - lowBound) / totalSpan).toFloat().coerceIn(0f, 1f)
                    return h - (norm * h)
                }

                // Subtle horizontal grid lines
                val gridColor = LgColors.BorderHairline.copy(alpha = 0.35f)
                listOf(0.15f, 0.50f, 0.85f).forEach { frac ->
                    val y = h * frac
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Dashed opening reference baseline
                val openY = yFor(points.first().priceUsd)
                drawLine(
                    color = LgColors.Gold.copy(alpha = 0.45f),
                    start = Offset(0f, openY),
                    end = Offset(w, openY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )

                val mapped = points.indices.map { idx ->
                    Offset(xFor(idx), yFor(points[idx].priceUsd))
                }

                val linePath = Path().apply {
                    moveTo(mapped.first().x, mapped.first().y)
                    for (i in 1 until mapped.size) {
                        val p0 = mapped[i - 1]
                        val p1 = mapped[i]
                        val cx = (p0.x + p1.x) * 0.5f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(mapped.last().x, h)
                    lineTo(mapped.first().x, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            trendColor.copy(alpha = 0.34f),
                            trendColor.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = linePath,
                    color = trendColor,
                    style = Stroke(width = 4.2f, cap = StrokeCap.Round)
                )

                val focusIdx = inspectedIndex?.coerceIn(0, mapped.lastIndex) ?: mapped.lastIndex
                val focusPt = mapped[focusIdx]

                if (inspectedIndex != null) {
                    drawLine(
                        color = LgColors.Gold.copy(alpha = 0.75f),
                        start = Offset(focusPt.x, 0f),
                        end = Offset(focusPt.x, h),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                drawCircle(
                    color = trendColor.copy(alpha = 0.28f),
                    radius = if (inspectedIndex != null) 14f else 10f,
                    center = focusPt
                )
                drawCircle(
                    color = LgColors.ObsidianBg,
                    radius = 6f,
                    center = focusPt
                )
                drawCircle(
                    color = if (inspectedIndex != null) LgColors.Gold else trendColor,
                    radius = 4.5f,
                    center = focusPt
                )
            }

            // Right-aligned Y-axis reference labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = Formatters.formatUsd(maxPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Formatters.formatUsd(midPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = Formatters.formatUsd(minPrice),
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted
                )
            }
        }

        // Bottom X-axis time ticks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
            Text(midLabel, style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
        }
    }
}

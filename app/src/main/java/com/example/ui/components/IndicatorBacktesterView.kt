package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConfluenceRule
import com.example.domain.models.EquityCurvePoint
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.IndicatorBacktestResult
import com.example.domain.models.SignalAction
import com.example.domain.models.SimulatedSignalMarker
import com.example.domain.models.TechnicalIndicatorType
import com.example.ui.theme.LgColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class BacktestChartTab(val label: String) {
    EQUITY("Equity vs Hold"),
    SIGNALS("Price & Signals")
}

@Composable
fun IndicatorBacktesterView(
    symbol: String,
    timeframe: ChartTimeframe,
    config: IndicatorBacktestConfig,
    result: IndicatorBacktestResult?,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onUpdateConfig: (IndicatorBacktestConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showParameters by remember { mutableStateOf(false) }
    var activeChartTab by remember { mutableStateOf(BacktestChartTab.EQUITY) }
    var inspectedIdx by remember(result) { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("indicator_backtester_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Strategy Builder & Indicator Config Card
        CyberFrameCard(title = "INDICATOR BACKTEST STUDIO") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select Technical Indicators",
                        style = MaterialTheme.typography.titleMedium,
                        color = LgColors.InkPrimary
                    )
                    Text(
                        text = "$symbol/USD · ${timeframe.label} historical candles",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                }
                TextButton(
                    onClick = { onUpdateConfig(IndicatorBacktestConfig()) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = "Reset indicators",
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text("Reset")
                }
            }

            // Indicator Multi-Select Chips: RSI, MACD, Bollinger Bands
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechnicalIndicatorType.entries.forEach { indicator ->
                    val selected = indicator in config.selectedIndicators
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (selected) {
                                if (config.selectedIndicators.size > 1) {
                                    config.selectedIndicators - indicator
                                } else {
                                    config.selectedIndicators
                                }
                            } else {
                                config.selectedIndicators + indicator
                            }
                            onUpdateConfig(config.copy(selectedIndicators = next))
                        },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = if (indicator == TechnicalIndicatorType.BOLLINGER) "Bollinger" else indicator.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.Turquoise,
                            selectedLeadingIconColor = LgColors.Turquoise
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backtest_indicator_${indicator.id}")
                    )
                }
            }

            // Timeframe selector inside Backtest Studio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartTimeframe.entries.forEach { tf ->
                    FilterChip(
                        selected = tf == timeframe,
                        onClick = { onSelectTimeframe(tf) },
                        label = { Text(tf.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.CyanGlow.copy(alpha = 0.20f),
                            selectedLabelColor = LgColors.CyanGlow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backtest_tf_${tf.label}")
                    )
                }
            }

            // Combination rule when 2+ indicators selected
            if (config.selectedIndicators.size > 1) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ConfluenceRule.entries.forEachIndexed { index, rule ->
                        SegmentedButton(
                            selected = config.confluenceRule == rule,
                            onClick = { onUpdateConfig(config.copy(confluenceRule = rule)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ConfluenceRule.entries.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = LgColors.Gold.copy(alpha = 0.22f),
                                activeContentColor = LgColors.Gold,
                                inactiveContainerColor = LgColors.ElevatedSlate,
                                inactiveContentColor = LgColors.InkSecondary
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("backtest_rule_${rule.name.lowercase()}")
                        ) {
                            Text(rule.label, maxLines = 1)
                        }
                    }
                }
            }

            // Capital & Slippage Presets
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(1000.0, 5000.0, 10000.0, 25000.0)) { cap ->
                    FilterChip(
                        selected = config.initialCapitalUsd == cap,
                        onClick = { onUpdateConfig(config.copy(initialCapitalUsd = cap)) },
                        label = { Text("Cap ${Formatters.formatCompactUsd(cap)}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Gold.copy(alpha = 0.18f),
                            selectedLabelColor = LgColors.Gold
                        )
                    )
                }
                items(listOf(0.0 to 0.0, 10.0 to 5.0, 20.0 to 10.0)) { (fee, slip) ->
                    val active = config.feeBps == fee && config.slippageBps == slip
                    FilterChip(
                        selected = active,
                        onClick = { onUpdateConfig(config.copy(feeBps = fee, slippageBps = slip)) },
                        label = { Text("${(fee + slip).toInt()} bps cost") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.CyanGlow.copy(alpha = 0.18f),
                            selectedLabelColor = LgColors.CyanGlow
                        )
                    )
                }
            }

            // Expandable Indicator Parameter Tuning
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LgColors.ElevatedSlate.copy(alpha = 0.55f))
                    .clickable { showParameters = !showParameters }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Tune Indicator Parameters",
                        style = MaterialTheme.typography.labelLarge,
                        color = LgColors.InkPrimary
                    )
                }
                Icon(
                    imageVector = if (showParameters) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (showParameters) "Collapse parameters" else "Expand parameters",
                    tint = LgColors.InkSecondary
                )
            }

            AnimatedVisibility(visible = showParameters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LgColors.ObsidianBg.copy(alpha = 0.8f))
                        .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (TechnicalIndicatorType.RSI in config.selectedIndicators) {
                        ParameterSliderRow(
                            label = "RSI Period: ${config.rsiPeriod}",
                            value = config.rsiPeriod.toFloat(),
                            range = 6f..28f,
                            onValueChange = { onUpdateConfig(config.copy(rsiPeriod = it.roundToInt())) }
                        )
                        ParameterSliderRow(
                            label = "RSI Oversold Buy <= ${config.rsiOversold.toInt()}",
                            value = config.rsiOversold.toFloat(),
                            range = 18f..42f,
                            onValueChange = { onUpdateConfig(config.copy(rsiOversold = it.roundToInt().toDouble())) }
                        )
                        ParameterSliderRow(
                            label = "RSI Overbought Sell >= ${config.rsiOverbought.toInt()}",
                            value = config.rsiOverbought.toFloat(),
                            range = 58f..85f,
                            onValueChange = { onUpdateConfig(config.copy(rsiOverbought = it.roundToInt().toDouble())) }
                        )
                    }
                    if (TechnicalIndicatorType.MACD in config.selectedIndicators) {
                        ParameterSliderRow(
                            label = "MACD Fast EMA: ${config.macdFast}",
                            value = config.macdFast.toFloat(),
                            range = 5f..18f,
                            onValueChange = {
                                val nextFast = it.roundToInt()
                                val safeSlow = max(config.macdSlow, nextFast + 4)
                                onUpdateConfig(config.copy(macdFast = nextFast, macdSlow = safeSlow))
                            }
                        )
                        ParameterSliderRow(
                            label = "MACD Slow EMA: ${config.macdSlow}",
                            value = config.macdSlow.toFloat(),
                            range = 16f..40f,
                            onValueChange = {
                                val nextSlow = max(it.roundToInt(), config.macdFast + 4)
                                onUpdateConfig(config.copy(macdSlow = nextSlow))
                            }
                        )
                        ParameterSliderRow(
                            label = "MACD Signal Line: ${config.macdSignal}",
                            value = config.macdSignal.toFloat(),
                            range = 4f..16f,
                            onValueChange = { onUpdateConfig(config.copy(macdSignal = it.roundToInt())) }
                        )
                    }
                    if (TechnicalIndicatorType.BOLLINGER in config.selectedIndicators) {
                        ParameterSliderRow(
                            label = "Bollinger Period: ${config.bbPeriod}",
                            value = config.bbPeriod.toFloat(),
                            range = 10f..35f,
                            onValueChange = { onUpdateConfig(config.copy(bbPeriod = it.roundToInt())) }
                        )
                        ParameterSliderRow(
                            label = "Bollinger StdDev: ${"%.1f".format(config.bbStdDev)}σ",
                            value = config.bbStdDev.toFloat(),
                            range = 1.2f..3.0f,
                            onValueChange = {
                                val rounded = (it * 10f).roundToInt() / 10.0
                                onUpdateConfig(config.copy(bbStdDev = rounded))
                            }
                        )
                    }
                }
            }

            GoldMetallicButton(
                text = "Simulate ${config.selectedIndicators.joinToString(" + ") { it.label }}",
                onClick = {
                    inspectedIdx = null
                    onUpdateConfig(config.copy())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_indicator_backtest_button")
            )
        }

        // 2. Results & Visualization
        if (result == null || result.equityPoints.size < 2) {
            OutlinedCard(
                border = BorderStroke(1.dp, LgColors.BorderHairline),
                colors = CardDefaults.outlinedCardColors(containerColor = LgColors.PanelSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.ShowChart, contentDescription = null, tint = LgColors.CyanGlow)
                    Text(
                        text = "Load historical candles for $symbol to simulate strategy performance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkSecondary
                    )
                }
            }
        } else {
            val stratUp = result.totalReturnPct >= 0.0
            val stratColor = if (stratUp) LgColors.Turquoise else LgColors.Crimson
            val alphaUp = result.alphaPct >= 0.0

            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header: Simulated Equity + Outperformance Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SIMULATED STRATEGY EQUITY",
                                style = MaterialTheme.typography.labelMedium,
                                color = LgColors.Gold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.formatUsd(result.finalEquityUsd),
                                style = MaterialTheme.typography.headlineSmall,
                                color = LgColors.InkPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("backtest_final_equity")
                            )
                            Text(
                                text = "Initial ${Formatters.formatUsd(result.initialCapitalUsd)} · ${result.equityPoints.size} bars",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkMuted
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatusPill(
                                text = "Strat ${Formatters.formatPct(result.totalReturnPct)}",
                                color = stratColor
                            )
                            StatusPill(
                                text = "Alpha ${Formatters.formatPct(result.alphaPct)}",
                                color = if (alphaUp) LgColors.CyanGlow else LgColors.Gold
                            )
                        }
                    }

                    // Dual Chart Mode Toggle (Equity vs Hold | Price & Signals)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        BacktestChartTab.entries.forEachIndexed { index, tab ->
                            SegmentedButton(
                                selected = activeChartTab == tab,
                                onClick = { activeChartTab = tab },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = BacktestChartTab.entries.size
                                ),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("backtest_chart_tab_${tab.name.lowercase()}")
                            ) {
                                Text(tab.label, maxLines = 1)
                            }
                        }
                    }

                    // Scrub Readout Bar
                    val focusPt = inspectedIdx?.let { result.equityPoints.getOrNull(it) } ?: result.equityPoints.last()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF070E1A))
                            .border(1.dp, LgColors.BorderHairline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formatters.formatTimeShort(focusPt.timestampSec * 1000L),
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkMuted
                            )
                            if (activeChartTab == BacktestChartTab.EQUITY) {
                                Text(
                                    text = "Strat ${Formatters.formatUsd(focusPt.strategyEquityUsd)}  vs  Hold ${Formatters.formatUsd(focusPt.buyHoldEquityUsd)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = stratColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                val rsiTxt = focusPt.rsiValue?.let { " · RSI ${"%.1f".format(it)}" } ?: ""
                                val macdTxt = focusPt.macdHistogram?.let { " · MACD ${"%.2f".format(it)}" } ?: ""
                                Text(
                                    text = "${Formatters.formatUsd(focusPt.priceUsd)}$rsiTxt$macdTxt",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LgColors.CyanGlow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (inspectedIdx != null) {
                            Text(
                                text = "SCRUB",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Gold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Visualization Canvas
                    if (activeChartTab == BacktestChartTab.EQUITY) {
                        EquityComparisonCanvas(
                            points = result.equityPoints,
                            initialCapital = result.initialCapitalUsd,
                            strategyColor = stratColor,
                            inspectedIndex = inspectedIdx,
                            onInspectIndex = { inspectedIdx = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LegendDot("Strategy Curve", stratColor)
                            LegendDot("Buy & Hold Benchmark", LgColors.Gold)
                        }
                    } else {
                        PriceAndSignalsCanvas(
                            points = result.equityPoints,
                            markers = result.markers,
                            showBollinger = TechnicalIndicatorType.BOLLINGER in result.config.selectedIndicators,
                            inspectedIndex = inspectedIdx,
                            onInspectIndex = { inspectedIdx = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LegendDot("BUY (${result.markers.count { it.action == SignalAction.BUY }})", LgColors.Turquoise)
                            LegendDot("SELL (${result.markers.count { it.action == SignalAction.SELL }})", LgColors.Crimson)
                            if (TechnicalIndicatorType.BOLLINGER in result.config.selectedIndicators) {
                                LegendDot("BB Envelope", LgColors.Gold)
                            }
                        }
                    }

                    // 6 KPI Performance Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Strategy Ret",
                                value = Formatters.formatPct(result.totalReturnPct),
                                valueColor = stratColor,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Buy & Hold",
                                value = Formatters.formatPct(result.buyHoldReturnPct),
                                valueColor = if (result.buyHoldReturnPct >= 0) LgColors.Gold else LgColors.Crimson,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Alpha",
                                value = Formatters.formatPct(result.alphaPct),
                                valueColor = if (alphaUp) LgColors.CyanGlow else LgColors.InkSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Win Rate",
                                value = "${"%.1f".format(result.winRatePct)}% (${result.winningTrades}/${result.totalTrades})",
                                valueColor = if (result.winRatePct >= 50.0) LgColors.Turquoise else LgColors.InkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Max Drawdown",
                                value = "-${"%.2f".format(result.maxDrawdownPct)}%",
                                valueColor = LgColors.Crimson,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Profit Factor",
                                value = if (result.profitFactor >= 99.0) "MAX" else "${"%.2f".format(result.profitFactor)}x",
                                valueColor = if (result.profitFactor >= 1.2) LgColors.Turquoise else LgColors.Gold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Simulated Historical Trade Ledger
                    HorizontalDivider(color = LgColors.BorderHairline.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Analytics, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Simulated Executions (${result.closedTrades.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = LgColors.Gold
                            )
                        }
                        Text(
                            text = "${result.config.feeBps.toInt()}+${result.config.slippageBps.toInt()} bps/side",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.InkMuted
                        )
                    }

                    if (result.closedTrades.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = LgColors.ElevatedSlate.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, LgColors.BorderHairline)
                        ) {
                            Text(
                                text = "No completed round-trip trades triggered under current settings. Try switching to \"Any Indicator (OR)\" or tuning RSI/Bollinger thresholds.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.closedTrades.take(10).forEach { trade ->
                                val ret = trade.returnPct ?: 0.0
                                val pnl = trade.pnlUsd ?: 0.0
                                val win = ret >= 0.0
                                val c = if (win) LgColors.Turquoise else LgColors.Crimson

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LgColors.ElevatedSlate.copy(alpha = 0.5f))
                                        .border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "${trade.entryPriceUsd?.let { Formatters.formatUsd(it) } ?: "--"} -> ${Formatters.formatUsd(trade.priceUsd)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = LgColors.InkPrimary
                                        )
                                        Text(
                                            text = "${trade.reason} · ${Formatters.formatTimeShort(trade.timestampSec * 1000L)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LgColors.InkSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatPct(ret),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = c,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val pnlPrefix = if (pnl > 0) "+" else ""
                                        Text(
                                            text = "$pnlPrefix${Formatters.formatUsd(pnl)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = c
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LgColors.CyanGlow
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = LgColors.Gold,
                activeTrackColor = LgColors.Turquoise,
                inactiveTrackColor = LgColors.ElevatedSlate
            )
        )
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = LgColors.InkSecondary)
    }
}

@Composable
private fun EquityComparisonCanvas(
    points: List<EquityCurvePoint>,
    initialCapital: Double,
    strategyColor: Color,
    inspectedIndex: Int?,
    onInspectIndex: (Int?) -> Unit
) {
    val allVals = remember(points, initialCapital) {
        points.flatMap { listOf(it.strategyEquityUsd, it.buyHoldEquityUsd) } + initialCapital
    }
    val minVal = remember(allVals) { allVals.minOrNull() ?: initialCapital }
    val maxVal = remember(allVals) { allVals.maxOrNull() ?: (initialCapital + 1.0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070E1A))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
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

            val span = max(maxVal - minVal, initialCapital * 0.01)
            val lo = minVal - span * 0.08
            val hi = maxVal + span * 0.08
            val fullSpan = max(hi - lo, 1e-9)

            fun xFor(i: Int): Float = (i.toFloat() / (points.size - 1)) * w
            fun yFor(v: Double): Float {
                val n = ((v - lo) / fullSpan).toFloat().coerceIn(0f, 1f)
                return h - n * h
            }

            // Initial capital baseline
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
                color = LgColors.Gold.copy(alpha = 0.8f),
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

            // Crosshair
            inspectedIndex?.let { idx ->
                val x = xFor(idx.coerceIn(0, points.lastIndex))
                drawLine(
                    color = LgColors.CyanGlow.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.6f
                )
                drawCircle(color = strategyColor, radius = 6f, center = mapped[idx.coerceIn(0, mapped.lastIndex)])
                drawCircle(
                    color = LgColors.Gold,
                    radius = 5f,
                    center = Offset(x, yFor(points[idx.coerceIn(0, points.lastIndex)].buyHoldEquityUsd))
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

@Composable
private fun PriceAndSignalsCanvas(
    points: List<EquityCurvePoint>,
    markers: List<SimulatedSignalMarker>,
    showBollinger: Boolean,
    inspectedIndex: Int?,
    onInspectIndex: (Int?) -> Unit
) {
    val prices = remember(points, showBollinger) {
        buildList {
            points.forEach { pt ->
                add(pt.priceUsd)
                if (showBollinger) {
                    pt.bbUpper?.let { add(it) }
                    pt.bbLower?.let { add(it) }
                }
            }
        }
    }
    val minP = remember(prices) { prices.minOrNull() ?: 0.0 }
    val maxP = remember(prices) { prices.maxOrNull() ?: 1.0 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070E1A))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
            .padding(10.dp)
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

            val span = max(maxP - minP, maxP * 0.004)
            val lo = minP - span * 0.08
            val hi = maxP + span * 0.08
            val fullSpan = max(hi - lo, 1e-9)

            fun xFor(i: Int): Float = (i.toFloat() / (points.size - 1)) * w
            fun yFor(v: Double): Float {
                val n = ((v - lo) / fullSpan).toFloat().coerceIn(0f, 1f)
                return h - n * h
            }

            if (showBollinger) {
                val upperPath = Path()
                val lowerPath = Path()
                var started = false
                points.forEachIndexed { idx, pt ->
                    val u = pt.bbUpper
                    val l = pt.bbLower
                    if (u != null && l != null) {
                        val x = xFor(idx)
                        if (!started) {
                            upperPath.moveTo(x, yFor(u))
                            lowerPath.moveTo(x, yFor(l))
                            started = true
                        } else {
                            upperPath.lineTo(x, yFor(u))
                            lowerPath.lineTo(x, yFor(l))
                        }
                    }
                }
                if (started) {
                    drawPath(upperPath, color = LgColors.Gold.copy(alpha = 0.55f), style = Stroke(width = 1.8f))
                    drawPath(lowerPath, color = LgColors.Gold.copy(alpha = 0.55f), style = Stroke(width = 1.8f))
                }
            }

            val pricePath = Path().apply {
                points.forEachIndexed { idx, pt ->
                    val x = xFor(idx)
                    val y = yFor(pt.priceUsd)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = pricePath,
                color = LgColors.CyanGlow,
                style = Stroke(width = 3.0f, cap = StrokeCap.Round)
            )

            markers.forEach { m ->
                val idx = m.candleIndex.coerceIn(0, points.lastIndex)
                val cx = xFor(idx)
                val cy = yFor(m.priceUsd)
                val isBuy = m.action == SignalAction.BUY
                val markerColor = if (isBuy) LgColors.Turquoise else LgColors.Crimson

                drawCircle(color = markerColor.copy(alpha = 0.30f), radius = 11f, center = Offset(cx, cy))
                drawCircle(color = Color(0xFF070E1A), radius = 6.5f, center = Offset(cx, cy))
                drawCircle(color = markerColor, radius = 4.8f, center = Offset(cx, cy))
            }

            inspectedIndex?.let { idx ->
                val x = xFor(idx.coerceIn(0, points.lastIndex))
                drawLine(
                    color = LgColors.Gold.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.5f
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
            Text(Formatters.formatUsd(maxP), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
            Text(Formatters.formatUsd(minP), style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
        }
    }
}

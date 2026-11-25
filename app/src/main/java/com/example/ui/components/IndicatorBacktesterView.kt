package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConfluenceRule
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.IndicatorBacktestResult
import com.example.domain.models.SignalAction
import com.example.domain.models.TechnicalIndicatorType
import com.example.ui.theme.LgColors
import kotlin.math.max
import kotlin.math.roundToInt

private enum class BacktestChartTab(val label: String) {
    SIGNALS("Signals & Price"),
    EQUITY("Equity vs Hold")
}

private data class StrategyBacktestPreset(
    val id: String,
    val label: String,
    val config: IndicatorBacktestConfig
)

private val BacktestPresets = listOf(
    StrategyBacktestPreset(
        id = "rsi_sma",
        label = "RSI + SMA Cross",
        config = IndicatorBacktestConfig(
            selectedIndicators = setOf(TechnicalIndicatorType.RSI, TechnicalIndicatorType.SMA_CROSS),
            confluenceRule = ConfluenceRule.ANY,
            rsiPeriod = 14,
            rsiOversold = 32.0,
            rsiOverbought = 68.0,
            smaFastPeriod = 9,
            smaSlowPeriod = 21
        )
    ),
    StrategyBacktestPreset(
        id = "sma_golden",
        label = "SMA Golden Cross (9/21)",
        config = IndicatorBacktestConfig(
            selectedIndicators = setOf(TechnicalIndicatorType.SMA_CROSS),
            smaFastPeriod = 9,
            smaSlowPeriod = 21
        )
    ),
    StrategyBacktestPreset(
        id = "ema_momentum",
        label = "EMA Momentum (12/26)",
        config = IndicatorBacktestConfig(
            selectedIndicators = setOf(TechnicalIndicatorType.EMA_CROSS),
            emaFastPeriod = 12,
            emaSlowPeriod = 26
        )
    ),
    StrategyBacktestPreset(
        id = "rsi_reversion",
        label = "RSI Reversal (14 · 30/70)",
        config = IndicatorBacktestConfig(
            selectedIndicators = setOf(TechnicalIndicatorType.RSI),
            rsiPeriod = 14,
            rsiOversold = 30.0,
            rsiOverbought = 70.0
        )
    ),
    StrategyBacktestPreset(
        id = "bollinger_rsi",
        label = "Bollinger + RSI",
        config = IndicatorBacktestConfig(
            selectedIndicators = setOf(TechnicalIndicatorType.BOLLINGER, TechnicalIndicatorType.RSI),
            confluenceRule = ConfluenceRule.ANY,
            bbPeriod = 20,
            bbStdDev = 2.0,
            rsiPeriod = 14,
            rsiOversold = 30.0,
            rsiOverbought = 70.0
        )
    ),
    StrategyBacktestPreset(
        id = "macd_ema",
        label = "MACD + EMA Confluence",
        config = IndicatorBacktestConfig(
            selectedIndicators = setOf(TechnicalIndicatorType.MACD, TechnicalIndicatorType.EMA_CROSS),
            confluenceRule = ConfluenceRule.ALL,
            emaFastPeriod = 10,
            emaSlowPeriod = 24,
            macdFast = 12,
            macdSlow = 26,
            macdSignal = 9
        )
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IndicatorBacktesterView(
    symbol: String,
    timeframe: ChartTimeframe,
    config: IndicatorBacktestConfig,
    result: IndicatorBacktestResult?,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onUpdateConfig: (IndicatorBacktestConfig) -> Unit,
    onSelectSymbol: ((String) -> Unit)? = null,
    isLoadingCandles: Boolean = false,
    candlesError: String? = null,
    onPullHistoricalData: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showParameters by remember { mutableStateOf(true) }
    var activeChartTab by remember { mutableStateOf(BacktestChartTab.SIGNALS) }
    var inspectedIdx by remember(result) { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("indicator_backtester_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Strategy Builder & Technical Indicator Inputs
        CyberFrameCard(title = "STRATEGY BACKTEST STUDIO") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Configure Indicators & Pull History",
                        style = MaterialTheme.typography.titleMedium,
                        color = LgColors.InkPrimary
                    )
                    Text(
                        text = if (result != null && result.equityPoints.isNotEmpty()) {
                            "$symbol/USD · ${timeframe.label} · ${result.equityPoints.size} historical bars loaded"
                        } else {
                            "$symbol/USD · ${timeframe.label} live OHLC historical candles"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.Turquoise
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

            // Asset Dropdown + Pull Historical Data Button
            if (onSelectSymbol != null) {
                CoinDropdownSelector(
                    selectedSymbol = symbol,
                    symbols = CoinAssets.catalog.map { it.symbol },
                    onSelectSymbol = onSelectSymbol,
                    label = "Historical Asset",
                    testTag = "backtest_coin_dropdown"
                )
            }

            // Timeframe selector + Refresh Historical Candles button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartTimeframe.entries.forEach { tf ->
                    FilterChip(
                        selected = tf == timeframe,
                        onClick = { onSelectTimeframe(tf) },
                        label = { Text(tf.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.CyanGlow.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.CyanGlow
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backtest_tf_${tf.label}")
                    )
                }
                if (onPullHistoricalData != null) {
                    OutlinedButton(
                        onClick = onPullHistoricalData,
                        enabled = !isLoadingCandles,
                        border = BorderStroke(1.dp, LgColors.Gold.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("backtest_pull_history_button")
                    ) {
                        if (isLoadingCandles) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = LgColors.GoldBright
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Pull historical candles",
                                tint = LgColors.GoldBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (candlesError != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LgColors.Crimson.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, LgColors.Crimson.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = candlesError,
                            style = MaterialTheme.typography.bodySmall,
                            color = LgColors.InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (onPullHistoricalData != null) {
                            TextButton(onClick = onPullHistoricalData) {
                                Text("Retry", color = LgColors.GoldBright)
                            }
                        }
                    }
                }
            }

            // One-Tap Strategy Presets
            Text(
                text = "QUICK STRATEGY TEMPLATES",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.Gold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BacktestPresets, key = { it.id }) { preset ->
                    val isMatching = config.selectedIndicators == preset.config.selectedIndicators
                    FilterChip(
                        selected = isMatching,
                        onClick = {
                            inspectedIdx = null
                            onUpdateConfig(
                                preset.config.copy(
                                    initialCapitalUsd = config.initialCapitalUsd,
                                    feeBps = config.feeBps,
                                    slippageBps = config.slippageBps,
                                    stopLossPct = config.stopLossPct,
                                    takeProfitPct = config.takeProfitPct
                                )
                            )
                        },
                        label = { Text(preset.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Gold.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.GoldBright
                        ),
                        modifier = Modifier.testTag("backtest_preset_${preset.id}")
                    )
                }
            }

            // Multi-Select Technical Indicators: RSI, SMA Cross, EMA Cross, MACD, Bollinger
            Text(
                text = "TECHNICAL INDICATORS (MULTI-SELECT)",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.Gold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        label = { Text(indicator.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.Turquoise,
                            selectedLeadingIconColor = LgColors.Turquoise
                        ),
                        modifier = Modifier.testTag("backtest_indicator_${indicator.id}")
                    )
                }
            }

            // Confluence rule when 2+ indicators are active
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
                                activeContentColor = LgColors.GoldBright,
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

            // Expandable Indicator Parameter Inputs (Direct Numeric Fields + Sliders)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LgColors.ElevatedSlate.copy(alpha = 0.55f))
                    .clickable { showParameters = !showParameters }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("backtest_toggle_parameters"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Indicator Inputs & Risk Controls",
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
                        .background(LgColors.ObsidianBg.copy(alpha = 0.85f))
                        .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (TechnicalIndicatorType.RSI in config.selectedIndicators) {
                        Text("RSI OSCILLATOR", style = MaterialTheme.typography.labelSmall, color = LgColors.Turquoise)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumericParamInput(
                                label = "Period",
                                value = config.rsiPeriod.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { onUpdateConfig(config.copy(rsiPeriod = it.coerceIn(4, 50))) }
                                },
                                modifier = Modifier.weight(1f).testTag("input_rsi_period")
                            )
                            NumericParamInput(
                                label = "Buy <=",
                                value = config.rsiOversold.toInt().toString(),
                                onCommit = { v ->
                                    v.toDoubleOrNull()?.let {
                                        onUpdateConfig(config.copy(rsiOversold = it.coerceIn(10.0, 48.0)))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_rsi_oversold")
                            )
                            NumericParamInput(
                                label = "Sell >=",
                                value = config.rsiOverbought.toInt().toString(),
                                onCommit = { v ->
                                    v.toDoubleOrNull()?.let {
                                        onUpdateConfig(config.copy(rsiOverbought = it.coerceIn(52.0, 92.0)))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_rsi_overbought")
                            )
                        }
                    }

                    if (TechnicalIndicatorType.SMA_CROSS in config.selectedIndicators) {
                        Text("SIMPLE MOVING AVERAGE (SMA CROSS)", style = MaterialTheme.typography.labelSmall, color = LgColors.GoldBright)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumericParamInput(
                                label = "Fast SMA",
                                value = config.smaFastPeriod.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { f ->
                                        val safeF = f.coerceIn(2, 60)
                                        val safeS = max(config.smaSlowPeriod, safeF + 2)
                                        onUpdateConfig(config.copy(smaFastPeriod = safeF, smaSlowPeriod = safeS))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_sma_fast")
                            )
                            NumericParamInput(
                                label = "Slow SMA",
                                value = config.smaSlowPeriod.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { s ->
                                        val safeS = max(s.coerceIn(4, 150), config.smaFastPeriod + 2)
                                        onUpdateConfig(config.copy(smaSlowPeriod = safeS))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_sma_slow")
                            )
                        }
                    }

                    if (TechnicalIndicatorType.EMA_CROSS in config.selectedIndicators) {
                        Text("EXPONENTIAL MOVING AVERAGE (EMA CROSS)", style = MaterialTheme.typography.labelSmall, color = LgColors.CyanGlow)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumericParamInput(
                                label = "Fast EMA",
                                value = config.emaFastPeriod.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { f ->
                                        val safeF = f.coerceIn(2, 60)
                                        val safeS = max(config.emaSlowPeriod, safeF + 2)
                                        onUpdateConfig(config.copy(emaFastPeriod = safeF, emaSlowPeriod = safeS))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_ema_fast")
                            )
                            NumericParamInput(
                                label = "Slow EMA",
                                value = config.emaSlowPeriod.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { s ->
                                        val safeS = max(s.coerceIn(4, 150), config.emaFastPeriod + 2)
                                        onUpdateConfig(config.copy(emaSlowPeriod = safeS))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_ema_slow")
                            )
                        }
                    }

                    if (TechnicalIndicatorType.MACD in config.selectedIndicators) {
                        Text("MACD MOMENTUM", style = MaterialTheme.typography.labelSmall, color = LgColors.Turquoise)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumericParamInput(
                                label = "Fast",
                                value = config.macdFast.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { f ->
                                        val safeF = f.coerceIn(3, 30)
                                        val safeS = max(config.macdSlow, safeF + 4)
                                        onUpdateConfig(config.copy(macdFast = safeF, macdSlow = safeS))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_macd_fast")
                            )
                            NumericParamInput(
                                label = "Slow",
                                value = config.macdSlow.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { s ->
                                        val safeS = max(s.coerceIn(8, 60), config.macdFast + 4)
                                        onUpdateConfig(config.copy(macdSlow = safeS))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_macd_slow")
                            )
                            NumericParamInput(
                                label = "Signal",
                                value = config.macdSignal.toString(),
                                onCommit = { v ->
                                    v.toIntOrNull()?.let { sig ->
                                        onUpdateConfig(config.copy(macdSignal = sig.coerceIn(3, 25)))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("input_macd_signal")
                            )
                        }
                    }

                    if (TechnicalIndicatorType.BOLLINGER in config.selectedIndicators) {
                        Text("BOLLINGER BANDS", style = MaterialTheme.typography.labelSmall, color = LgColors.Gold)
                        ParameterSliderRow(
                            label = "Period: ${config.bbPeriod} · StdDev: ${"%.1f".format(config.bbStdDev)}σ",
                            value = config.bbPeriod.toFloat(),
                            range = 10f..35f,
                            onValueChange = { onUpdateConfig(config.copy(bbPeriod = it.roundToInt())) }
                        )
                        ParameterSliderRow(
                            label = "Band Width: ${"%.1f".format(config.bbStdDev)}σ",
                            value = config.bbStdDev.toFloat(),
                            range = 1.2f..3.0f,
                            onValueChange = {
                                val rounded = (it * 10f).roundToInt() / 10.0
                                onUpdateConfig(config.copy(bbStdDev = rounded))
                            }
                        )
                    }

                    HorizontalDivider(color = LgColors.BorderHairline.copy(alpha = 0.5f))

                    // Capital, Stop Loss, Take Profit, and Execution Costs
                    Text("CAPITAL & RISK OVERLAYS", style = MaterialTheme.typography.labelSmall, color = LgColors.Gold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumericParamInput(
                            label = "Capital ($)",
                            value = config.initialCapitalUsd.toInt().toString(),
                            onCommit = { v ->
                                v.toDoubleOrNull()?.let {
                                    onUpdateConfig(config.copy(initialCapitalUsd = it.coerceIn(100.0, 1_000_000.0)))
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("input_backtest_capital")
                        )
                        NumericParamInput(
                            label = "SL % (0=Off)",
                            value = if (config.stopLossPct == 0.0) "0" else "%.1f".format(config.stopLossPct),
                            onCommit = { v ->
                                v.toDoubleOrNull()?.let {
                                    onUpdateConfig(config.copy(stopLossPct = it.coerceIn(0.0, 35.0)))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("input_backtest_sl")
                        )
                        NumericParamInput(
                            label = "TP % (0=Off)",
                            value = if (config.takeProfitPct == 0.0) "0" else "%.1f".format(config.takeProfitPct),
                            onCommit = { v ->
                                v.toDoubleOrNull()?.let {
                                    onUpdateConfig(config.copy(takeProfitPct = it.coerceIn(0.0, 80.0)))
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("input_backtest_tp")
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0.0 to 0.0, 10.0 to 5.0, 20.0 to 10.0)) { (fee, slip) ->
                            val active = config.feeBps == fee && config.slippageBps == slip
                            FilterChip(
                                selected = active,
                                onClick = { onUpdateConfig(config.copy(feeBps = fee, slippageBps = slip)) },
                                label = { Text("${(fee + slip).toInt()} bps fee+slip") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LgColors.CyanGlow.copy(alpha = 0.18f),
                                    selectedLabelColor = LgColors.CyanGlow
                                )
                            )
                        }
                    }
                }
            }

            GoldMetallicButton(
                text = if (isLoadingCandles) {
                    "PULLING $symbol HISTORICAL DATA..."
                } else {
                    "RUN BACKTEST · ${config.selectedIndicators.joinToString(" + ") { it.label }}"
                },
                onClick = {
                    inspectedIdx = null
                    if (result == null || result.equityPoints.size < 2) {
                        onPullHistoricalData?.invoke() ?: onUpdateConfig(config.copy())
                    } else {
                        onUpdateConfig(config.copy())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("run_indicator_backtest_button")
            )
        }

        // 2. Results & Interactive Historical Performance Charts
        if (isLoadingCandles && (result == null || result.equityPoints.size < 2)) {
            OutlinedCard(
                border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.5f)),
                colors = CardDefaults.outlinedCardColors(containerColor = LgColors.PanelSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LgColors.Turquoise
                    )
                    Text(
                        text = "Pulling live historical $symbol (${timeframe.label}) candles & computing indicator signals...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkPrimary
                    )
                }
            }
        } else if (result == null || result.equityPoints.size < 2) {
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
                        text = "Tap Run Backtest or Pull Data to fetch historical $symbol candles and visualize signal performance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkSecondary
                    )
                }
            }
        } else {
            val stratUp = result.totalReturnPct >= 0.0
            val stratColor = if (stratUp) LgColors.Turquoise else LgColors.Crimson
            val alphaUp = result.alphaPct >= 0.0

            CyberFrameCard(
                title = "BACKTEST PERFORMANCE · $symbol ${timeframe.label}",
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Simulated Equity + Strategy Return & Alpha Badges
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
                                text = "Initial ${Formatters.formatUsd(result.initialCapitalUsd)} · ${result.equityPoints.size} bars · ${result.totalTrades} trades",
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

                    // Chart View Toggle (Signals & Price | Equity vs Hold)
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

                    // Interactive Crosshair Readout Bar
                    val focusPt = inspectedIdx?.let { result.equityPoints.getOrNull(it) } ?: result.equityPoints.last()
                    val focusMarker = inspectedIdx?.let { idx -> result.markers.firstOrNull { it.candleIndex == idx } }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LgColors.ObsidianBg)
                            .border(1.dp, LgColors.BorderHairline.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = Formatters.formatTimeShort(focusPt.timestampSec * 1000L) +
                                    if (focusMarker != null) " · ${focusMarker.action.name}: ${focusMarker.reason}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (focusMarker != null) {
                                    if (focusMarker.action == SignalAction.BUY) LgColors.Turquoise else LgColors.Crimson
                                } else LgColors.InkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                val parts = buildList {
                                    add(Formatters.formatUsd(focusPt.priceUsd))
                                    if (TechnicalIndicatorType.SMA_CROSS in result.config.selectedIndicators) {
                                        focusPt.smaFast?.let { f ->
                                            focusPt.smaSlow?.let { s ->
                                                add("SMA ${Formatters.formatUsd(f)}/${Formatters.formatUsd(s)}")
                                            }
                                        }
                                    }
                                    if (TechnicalIndicatorType.EMA_CROSS in result.config.selectedIndicators) {
                                        focusPt.emaFast?.let { f ->
                                            focusPt.emaSlow?.let { s ->
                                                add("EMA ${Formatters.formatUsd(f)}/${Formatters.formatUsd(s)}")
                                            }
                                        }
                                    }
                                    if (TechnicalIndicatorType.RSI in result.config.selectedIndicators) {
                                        focusPt.rsiValue?.let { add("RSI ${"%.1f".format(it)}") }
                                    }
                                }
                                Text(
                                    text = parts.joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LgColors.CyanGlow,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (inspectedIdx != null) {
                            TextButton(onClick = { inspectedIdx = null }) {
                                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = LgColors.GoldBright)
                            }
                        }
                    }

                    // Primary Visualization Canvases
                    if (activeChartTab == BacktestChartTab.SIGNALS) {
                        BacktestPriceSignalsCanvas(
                            points = result.equityPoints,
                            markers = result.markers,
                            config = result.config,
                            inspectedIndex = inspectedIdx,
                            onInspectIndex = { inspectedIdx = it }
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LegendDot("▲ BUY (${result.markers.count { it.action == SignalAction.BUY }})", LgColors.Turquoise)
                            LegendDot("▼ SELL (${result.markers.count { it.action == SignalAction.SELL }})", LgColors.Crimson)
                            if (TechnicalIndicatorType.SMA_CROSS in result.config.selectedIndicators) {
                                LegendDot("SMA ${result.config.smaFastPeriod}", LgColors.GoldBright)
                                LegendDot("SMA ${result.config.smaSlowPeriod}", Color(0xFFFF8A3D))
                            }
                            if (TechnicalIndicatorType.EMA_CROSS in result.config.selectedIndicators) {
                                LegendDot("EMA ${result.config.emaFastPeriod}", Color(0xFF42F5E9))
                                LegendDot("EMA ${result.config.emaSlowPeriod}", Color(0xFFD06BFF))
                            }
                            if (TechnicalIndicatorType.BOLLINGER in result.config.selectedIndicators) {
                                LegendDot("BB Channel", LgColors.Gold)
                            }
                        }

                        // Synchronized RSI / MACD Sub-Charts directly beneath the Price & Signals Chart
                        BacktestOscillatorSubChart(
                            points = result.equityPoints,
                            config = result.config,
                            inspectedIndex = inspectedIdx,
                            onInspectIndex = { inspectedIdx = it }
                        )
                    } else {
                        BacktestEquityComparisonCanvas(
                            points = result.equityPoints,
                            markers = result.markers,
                            initialCapital = result.initialCapitalUsd,
                            strategyColor = stratColor,
                            inspectedIndex = inspectedIdx,
                            onInspectIndex = { inspectedIdx = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LegendDot("Strategy Equity", stratColor)
                            LegendDot("Buy & Hold Benchmark", LgColors.Gold)
                        }
                    }

                    // 9-Metric Institutional Performance Grid (3x3)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Avg Trade",
                                value = Formatters.formatPct(result.avgTradeReturnPct),
                                valueColor = if (result.avgTradeReturnPct >= 0.0) LgColors.Turquoise else LgColors.Crimson,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Sharpe Est.",
                                value = "%.2f".format(result.sharpeEstimate),
                                valueColor = if (result.sharpeEstimate >= 1.0) LgColors.Turquoise else LgColors.Gold,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Exec Cost",
                                value = "${(result.config.feeBps + result.config.slippageBps).toInt()} bps",
                                valueColor = LgColors.InkSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Simulated Historical Trade Ledger (tap any row to jump crosshair on chart)
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
                                text = "Simulated Signals & Trades (${result.closedTrades.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = LgColors.Gold
                            )
                        }
                        Text(
                            text = "Tap to inspect",
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
                                text = "No completed round-trip trades triggered on this window. Try a faster Moving Average (e.g. 5/13), wider RSI thresholds (35/65), or switch to \"Any Indicator (OR)\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.closedTrades.take(12).forEach { trade ->
                                val ret = trade.returnPct ?: 0.0
                                val pnl = trade.pnlUsd ?: 0.0
                                val win = ret >= 0.0
                                val c = if (win) LgColors.Turquoise else LgColors.Crimson
                                val isFocused = inspectedIdx == trade.candleIndex

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isFocused) c.copy(alpha = 0.16f)
                                            else LgColors.ElevatedSlate.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            width = if (isFocused) 1.5.dp else 1.dp,
                                            color = if (isFocused) c else c.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            activeChartTab = BacktestChartTab.SIGNALS
                                            inspectedIdx = trade.candleIndex
                                        }
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
private fun NumericParamInput(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = draft,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || it == '.' }
            draft = filtered
            if (filtered.isNotBlank()) {
                onCommit(filtered)
            }
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LgColors.Turquoise,
            unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = modifier
    )
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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

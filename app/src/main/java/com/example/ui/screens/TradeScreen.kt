package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.RiskProfile
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import com.example.ui.components.CandlestickChart
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.EquitySparklineChart
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.IndicatorBacktesterView
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SignalBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

enum class TradeSubWorkspace(val label: String) {
    ORDER("Paper Ticket"),
    BACKTEST("Backtest Lab"),
    STRATEGIES("11 Strategies")
}

@Composable
fun TradeScreen(
    state: TradeUiState,
    vaultState: VaultUiState,
    isWide: Boolean = false,
    activeWorkspace: TradeSubWorkspace = TradeSubWorkspace.ORDER,
    onSelectWorkspace: ((TradeSubWorkspace) -> Unit)? = null,
    onSelectSymbol: (String) -> Unit,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onSelectOverlay: (ChartOverlay) -> Unit,
    onSelectStrategy: (StrategyType) -> Unit,
    onSubmitPaperOrder: (Boolean, Double) -> Unit,
    onToggleAutopilot: (Boolean) -> Unit,
    onUpdateRiskConfig: (RiskProfile, String, Double, Boolean) -> Unit,
    onRunAutopilotTick: () -> Unit,
    onUpdateIndicatorBacktestConfig: (IndicatorBacktestConfig) -> Unit = {}
) {
    var localSubPanel by remember { mutableStateOf(activeWorkspace) }
    val activeSubPanel = if (onSelectWorkspace != null) activeWorkspace else localSubPanel
    fun chooseSubPanel(target: TradeSubWorkspace) {
        if (onSelectWorkspace != null) onSelectWorkspace(target) else localSubPanel = target
    }
    var orderAmountText by remember { mutableStateOf("250") }
    var scheduleText by remember(vaultState.account.stopSchedule) { mutableStateOf(vaultState.account.stopSchedule) }
    var stopScheduleEnabled by remember(vaultState.account.stopSchedule) {
        mutableStateOf(vaultState.account.stopSchedule.isNotBlank())
    }
    var maxDdPct by remember(vaultState.account.maxDrawdownPct) {
        mutableDoubleStateOf(vaultState.account.maxDrawdownPct)
    }
    var selectedRisk by remember(vaultState.account.riskProfile) {
        mutableStateOf(RiskProfile.entries.firstOrNull { it.label == vaultState.account.riskProfile } ?: RiskProfile.MEDIUM)
    }
    var htfConfirm by remember(vaultState.account.htfConfirmation) { mutableStateOf(vaultState.account.htfConfirmation) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LgColors.ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Coin Watch Selector Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CoinAvatar(state.symbol, size = 36.dp)
                        Column {
                            val livePrice = (state.candlesState as? UiState.Success)?.data?.lastOrNull()?.close
                            Text(
                                text = if (livePrice != null) "${state.symbol}/USD · ${Formatters.formatUsd(livePrice)}" else "${state.symbol} / USD",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = state.strategy.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.Gold
                            )
                        }
                    }
                    state.liveSignal?.let { SignalBadge(it.action) }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CoinAssets.catalog.take(12), key = { it.symbol }) { entry ->
                        FilterChip(
                            selected = entry.symbol == state.symbol,
                            onClick = { onSelectSymbol(entry.symbol) },
                            label = { Text(entry.symbol) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.2f),
                                selectedLabelColor = LgColors.Turquoise
                            ),
                            modifier = Modifier.testTag("trade_coin_${entry.symbol}")
                        )
                    }
                }
            }
        }

        // Primary Adaptive Trader + Market Snapshot (side-by-side on wide tablets, stacked on phones)
        item {
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AdaptiveTraderCard(
                        vaultState = vaultState,
                        selectedRisk = selectedRisk,
                        onSelectRisk = { selectedRisk = it },
                        stopScheduleEnabled = stopScheduleEnabled,
                        onToggleStopSchedule = { stopScheduleEnabled = it },
                        scheduleText = scheduleText,
                        onScheduleChange = { scheduleText = it },
                        maxDdPct = maxDdPct,
                        onStepMaxDd = { delta -> maxDdPct = (maxDdPct + delta).coerceIn(1.0, 25.0) },
                        htfConfirm = htfConfirm,
                        onToggleHtf = { htfConfirm = it },
                        onSaveGuard = {
                            onUpdateRiskConfig(
                                selectedRisk,
                                if (stopScheduleEnabled) scheduleText.ifBlank { "22:00-06:00" } else "",
                                maxDdPct,
                                htfConfirm
                            )
                        },
                        onToggleAutopilot = onToggleAutopilot,
                        onRunTick = onRunAutopilotTick,
                        modifier = Modifier.weight(1f)
                    )
                    MarketSnapshotCard(
                        state = state,
                        onSelectTimeframe = onSelectTimeframe,
                        onSelectOverlay = onSelectOverlay,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AdaptiveTraderCard(
                        vaultState = vaultState,
                        selectedRisk = selectedRisk,
                        onSelectRisk = { selectedRisk = it },
                        stopScheduleEnabled = stopScheduleEnabled,
                        onToggleStopSchedule = { stopScheduleEnabled = it },
                        scheduleText = scheduleText,
                        onScheduleChange = { scheduleText = it },
                        maxDdPct = maxDdPct,
                        onStepMaxDd = { delta -> maxDdPct = (maxDdPct + delta).coerceIn(1.0, 25.0) },
                        htfConfirm = htfConfirm,
                        onToggleHtf = { htfConfirm = it },
                        onSaveGuard = {
                            onUpdateRiskConfig(
                                selectedRisk,
                                if (stopScheduleEnabled) scheduleText.ifBlank { "22:00-06:00" } else "",
                                maxDdPct,
                                htfConfirm
                            )
                        },
                        onToggleAutopilot = onToggleAutopilot,
                        onRunTick = onRunAutopilotTick
                    )
                    MarketSnapshotCard(
                        state = state,
                        onSelectTimeframe = onSelectTimeframe,
                        onSelectOverlay = onSelectOverlay
                    )
                }
            }
        }

        state.statusBanner?.let { banner ->
            item {
                GlassPanelCard(highlightBorder = LgColors.Gold.copy(alpha = 0.55f)) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.Gold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Desk Sub-Tabs: Paper Ticket | Backtest Lab | 11 Strategies
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TradeSubWorkspace.entries.forEach { panel ->
                    FilterChip(
                        selected = panel == activeSubPanel,
                        onClick = { chooseSubPanel(panel) },
                        label = { Text(panel.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Gold.copy(alpha = 0.2f),
                            selectedLabelColor = LgColors.Gold
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trade_subtab_${panel.name.lowercase()}")
                    )
                }
            }
        }

        when (activeSubPanel) {
            TradeSubWorkspace.ORDER -> {
                item {
                    val currentHolding = vaultState.holdings.firstOrNull { it.symbol == state.symbol }
                    CyberFrameCard(title = "PAPER ORDER TICKET") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Available Cash: ${Formatters.formatUsd(vaultState.account.usdBalance)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkSecondary
                            )
                            if (currentHolding != null) {
                                StatusPill(
                                    text = "${Formatters.formatQty(currentHolding.quantity)} ${state.symbol}",
                                    color = LgColors.CyanGlow
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("100", "250", "500", "1000").forEach { preset ->
                                FilterChip(
                                    selected = orderAmountText == preset,
                                    onClick = { orderAmountText = preset },
                                    label = { Text("$$preset") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = orderAmountText,
                            onValueChange = { orderAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Order Notional (USD)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paper_order_amount_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val parsed = orderAmountText.toDoubleOrNull() ?: 0.0
                            Button(
                                onClick = { onSubmitPaperOrder(true, parsed) },
                                colors = ButtonDefaults.buttonColors(containerColor = LgColors.Turquoise, contentColor = LgColors.ObsidianBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("paper_buy_button")
                            ) {
                                Text("Buy ${state.symbol}", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onSubmitPaperOrder(false, parsed) },
                                colors = ButtonDefaults.buttonColors(containerColor = LgColors.Crimson, contentColor = LgColors.ObsidianBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("paper_sell_button")
                            ) {
                                Text("Sell ${state.symbol}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            TradeSubWorkspace.STRATEGIES -> {
                items(StrategyType.entries, key = { it.id }) { strat ->
                    val selected = strat == state.strategy
                    GlassPanelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStrategy(strat) },
                        highlightBorder = if (selected) LgColors.Turquoise.copy(alpha = 0.65f) else LgColors.BorderHairline
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(strat.title, style = MaterialTheme.typography.titleMedium, color = if (selected) LgColors.Turquoise else LgColors.InkPrimary)
                                Text(strat.summary, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                            }
                            if (selected) {
                                state.liveSignal?.let { SignalBadge(it.action) }
                            }
                        }
                    }
                }
            }

            TradeSubWorkspace.BACKTEST -> {
                item {
                    IndicatorBacktesterView(
                        symbol = state.symbol,
                        timeframe = state.timeframe,
                        config = state.indicatorBacktestConfig,
                        result = state.indicatorBacktestResult,
                        onSelectTimeframe = onSelectTimeframe,
                        onUpdateConfig = onUpdateIndicatorBacktestConfig
                    )
                }
                state.backtestResult?.let { bt ->
                    item {
                        CyberFrameCard(title = "ACTIVE STRATEGY 70/30 SPLIT") {
                            Text(
                                text = "${bt.strategy.title} · Fee ${bt.feeBps.toInt()} bps + Slip ${bt.slippageBps.toInt()} bps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkSecondary
                            )
                            EquitySparklineChart(
                                curve = bt.equityCurve,
                                positive = bt.totalReturnPct >= 0.0
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    label = "Total Ret",
                                    value = Formatters.formatPct(bt.totalReturnPct),
                                    valueColor = if (bt.totalReturnPct >= 0) LgColors.Turquoise else LgColors.Crimson,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Val (30%)",
                                    value = Formatters.formatPct(bt.validationReturnPct),
                                    valueColor = if (bt.validationReturnPct >= 0) LgColors.Turquoise else LgColors.Crimson,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Win Rate",
                                    value = "${"%.1f".format(bt.winRatePct)}%",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    label = "Train (70%)",
                                    value = Formatters.formatPct(bt.trainReturnPct),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Max DD",
                                    value = "-${"%.2f".format(bt.maxDrawdownPct)}%",
                                    valueColor = LgColors.Crimson,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Trades",
                                    value = bt.totalTrades.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveTraderCard(
    vaultState: VaultUiState,
    selectedRisk: RiskProfile,
    onSelectRisk: (RiskProfile) -> Unit,
    stopScheduleEnabled: Boolean,
    onToggleStopSchedule: (Boolean) -> Unit,
    scheduleText: String,
    onScheduleChange: (String) -> Unit,
    maxDdPct: Double,
    onStepMaxDd: (Double) -> Unit,
    htfConfirm: Boolean,
    onToggleHtf: (Boolean) -> Unit,
    onSaveGuard: () -> Unit,
    onToggleAutopilot: (Boolean) -> Unit,
    onRunTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val running = vaultState.account.autopilotRunning
    CyberFrameCard(title = "ADAPTIVE TRADER", modifier = modifier) {
        // Center Budget Readout (mirrors $300 USD / Live Budget visual anchor)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = LgColors.Gold)
                Text("Live Paper Budget", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                StatusPill(
                    text = if (running) "ARMED" else "PAUSED",
                    color = if (running) LgColors.Turquoise else LgColors.Gold
                )
            }
            Text(
                text = Formatters.formatUsd(selectedRisk.maxDailyNotionalUsd),
                style = MaterialTheme.typography.displaySmall,
                color = LgColors.CyanGlow,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Max Order ${Formatters.formatUsd(selectedRisk.maxOrderUsd)} · Equity ${Formatters.formatUsd(vaultState.totalPortfolioEquityUsd)}",
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.InkMuted
            )
        }

        // Risk Level LOW / MEDIUM / HIGH pills
        Text(
            text = "Risk Level",
            style = MaterialTheme.typography.labelLarge,
            color = LgColors.Gold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RiskProfile.entries.forEach { rp ->
                val isSel = rp == selectedRisk
                val pillColor = when (rp) {
                    RiskProfile.LOW -> LgColors.InkSecondary
                    RiskProfile.MEDIUM -> LgColors.Turquoise
                    RiskProfile.HIGH -> LgColors.Gold
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) pillColor.copy(alpha = 0.2f) else LgColors.ObsidianBg)
                        .border(
                            width = if (isSel) 1.5.dp else 1.dp,
                            color = if (isSel) pillColor else LgColors.BorderHairline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectRisk(rp) }
                        .testTag("risk_pill_${rp.label}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rp.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSel) pillColor else LgColors.InkMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Stop Schedule & Auto-Stop Max Drawdown Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Stop Schedule", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkPrimary)
            Switch(
                checked = stopScheduleEnabled,
                onCheckedChange = onToggleStopSchedule,
                colors = SwitchDefaults.colors(checkedThumbColor = LgColors.ObsidianBg, checkedTrackColor = LgColors.Turquoise)
            )
        }
        if (stopScheduleEnabled) {
            OutlinedTextField(
                value = scheduleText,
                onValueChange = onScheduleChange,
                label = { Text("Quiet Window (UTC HH:MM-HH:MM)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto Stop Drawdown", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkPrimary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onStepMaxDd(-0.5) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LgColors.ElevatedSlate)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease max drawdown", tint = LgColors.CyanGlow)
                }
                Text(
                    text = "${"%.1f".format(maxDdPct)}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = LgColors.Gold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onStepMaxDd(0.5) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LgColors.ElevatedSlate)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase max drawdown", tint = LgColors.CyanGlow)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleHtf(!htfConfirm) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("4h Higher-Timeframe Filter", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
            Switch(checked = htfConfirm, onCheckedChange = onToggleHtf)
        }

        // Primary Gold START / PAUSE CTA + Run Tick + Save Guard
        GoldMetallicButton(
            text = if (running) "PAUSE AUTOPILOT" else "START AUTOPILOT",
            onClick = { onToggleAutopilot(!running) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("autopilot_switch")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSaveGuard,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text("Save Guard")
            }
            Button(
                onClick = onRunTick,
                colors = ButtonDefaults.buttonColors(containerColor = LgColors.CyanGlow, contentColor = LgColors.ObsidianBg),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("run_autopilot_tick_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Run Tick", fontWeight = FontWeight.Bold)
            }
        }

        // 3 Mini Bottom Stat Tiles (matches Current Balance / Daily Used / Max Loss Limit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "Cash Bal",
                value = Formatters.formatUsd(vaultState.account.usdBalance),
                valueColor = LgColors.CyanGlow,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Daily Used",
                value = Formatters.formatUsd(vaultState.account.dailyNotionalUsedUsd),
                valueColor = LgColors.Turquoise,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Drawdown",
                value = "-${"%.2f".format(vaultState.currentDrawdownPct)}%",
                valueColor = LgColors.Crimson,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MarketSnapshotCard(
    state: TradeUiState,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onSelectOverlay: (ChartOverlay) -> Unit,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(title = "MARKET SNAPSHOT", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChartTimeframe.entries.forEach { tf ->
                FilterChip(
                    selected = tf == state.timeframe,
                    onClick = { onSelectTimeframe(tf) },
                    label = { Text(tf.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ChartOverlay.entries) { ov ->
                FilterChip(
                    selected = ov == state.overlay,
                    onClick = { onSelectOverlay(ov) },
                    label = { Text(ov.label) }
                )
            }
        }

        when (val cs = state.candlesState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LgColors.Turquoise)
                }
            }
            is UiState.Error -> {
                Text(
                    text = "Chart error: ${cs.message}",
                    color = LgColors.Crimson,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is UiState.Success -> {
                CandlestickChart(candles = cs.data, overlay = state.overlay)
            }
        }

        state.liveSignal?.let { sig ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sig.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LgColors.InkSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(sig.confidence * 100).toInt()}% conf",
                    style = MaterialTheme.typography.labelMedium,
                    color = LgColors.CyanGlow,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

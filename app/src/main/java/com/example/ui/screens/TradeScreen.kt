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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.models.AdaptiveOpportunity
import com.example.domain.models.BaseFiatCurrency
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConnectedExchange
import com.example.domain.models.ExecutionMode
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.LiveExecutionRoute
import com.example.domain.models.RiskProfile
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import com.example.ui.components.CandlestickChart
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CoinDropdownSelector
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.EquitySparklineChart
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.IndicatorBacktesterView
import com.example.ui.components.LiveAutoFeedBadge
import com.example.ui.components.LiveServerAndExecutionCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SignalBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.StrategiesHeroChartButton
import com.example.ui.components.StrategyChartButtonCard
import com.example.ui.components.TradingViewFullChartDialog
import com.example.ui.components.TradingViewLiveChartWebView
import com.example.ui.components.circuitTextFieldBackdrop
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

enum class TradeSubWorkspace(val label: String) {
    ORDER("Order Ticket"),
    BACKTEST("Backtest Lab"),
    STRATEGIES("Strategies")
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
    onUpdateIndicatorBacktestConfig: (IndicatorBacktestConfig) -> Unit = {},
    onSelectBaseCurrency: (BaseFiatCurrency) -> Unit = {},
    onSelectConnectedExchange: (ConnectedExchange) -> Unit = {},
    onScanExchangeMarket: () -> Unit = {},
    onApplyAdaptiveOpportunity: (AdaptiveOpportunity) -> Unit = {},
    onSetExecutionMode: (ExecutionMode) -> Unit = {},
    onSelectLiveRoute: (LiveExecutionRoute) -> Unit = {},
    onOpenExchangeConfig: () -> Unit = {}
) {
    var localSubPanel by remember { mutableStateOf(activeWorkspace) }
    val activeSubPanel = if (onSelectWorkspace != null) activeWorkspace else localSubPanel
    fun chooseSubPanel(target: TradeSubWorkspace) {
        if (onSelectWorkspace != null) onSelectWorkspace(target) else localSubPanel = target
    }
    var showConfirmLiveDialog by remember { mutableStateOf(false) }
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

    if (showConfirmLiveDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmLiveDialog = false },
            containerColor = LgColors.PanelSlate,
            titleContentColor = LgColors.GoldBright,
            textContentColor = LgColors.InkPrimary,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = LgColors.GoldBright
                )
            },
            title = { Text("Arm Live Trading Mode?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Orders and Adaptive Autopilot ticks will route LIVE through ${state.liveExecutionRoute.label}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Active Risk Cap: ${selectedRisk.label} (max $${selectedRisk.maxOrderUsd.toInt()} per live order).",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.Turquoise
                    )
                    Text(
                        text = "Connect and manage your exchange APIs in Exchange Configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmLiveDialog = false
                        onSetExecutionMode(ExecutionMode.LIVE)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgColors.Turquoise,
                        contentColor = LgColors.ObsidianBg
                    ),
                    modifier = Modifier.testTag("confirm_arm_live_mode_button")
                ) {
                    Text("ARM LIVE MODE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmLiveDialog = false },
                    modifier = Modifier.testTag("cancel_arm_live_mode_button")
                ) {
                    Text("Stay Paper", color = LgColors.Gold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
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

                CoinDropdownSelector(
                    selectedSymbol = state.symbol,
                    symbols = CoinAssets.catalog.map { it.symbol },
                    onSelectSymbol = onSelectSymbol,
                    label = "Active Trading Coin",
                    testTag = "trade_coin_dropdown"
                )
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
                        tradeState = state,
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
                        onSelectBaseCurrency = onSelectBaseCurrency,
                        onSelectConnectedExchange = onSelectConnectedExchange,
                        onScanExchangeMarket = onScanExchangeMarket,
                        onApplyAdaptiveOpportunity = onApplyAdaptiveOpportunity,
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
                        tradeState = state,
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
                        onSelectBaseCurrency = onSelectBaseCurrency,
                        onSelectConnectedExchange = onSelectConnectedExchange,
                        onScanExchangeMarket = onScanExchangeMarket,
                        onApplyAdaptiveOpportunity = onApplyAdaptiveOpportunity
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

        // Chart-Image Button for Strategies + Desk Sub-Tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StrategiesHeroChartButton(
                    activeStrategyTitle = state.strategy.title,
                    isActiveTab = activeSubPanel == TradeSubWorkspace.STRATEGIES,
                    onClick = { chooseSubPanel(TradeSubWorkspace.STRATEGIES) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                TradeSubWorkspace.entries.forEach { panel ->
                    FilterChip(
                        selected = panel == activeSubPanel,
                        onClick = { chooseSubPanel(panel) },
                        label = {
                            Text(
                                text = panel.label.uppercase(),
                                style = TabLabelTextStyle,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.Gold,
                            containerColor = LgColors.PanelSlate.copy(alpha = 0.8f),
                            labelColor = LgColors.InkSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trade_subtab_${panel.name.lowercase()}")
                    )
                }
                }
            }
        }

        when (activeSubPanel) {
            TradeSubWorkspace.ORDER -> {
                item {
                    LiveServerAndExecutionCard(
                        state = state,
                        selectedRisk = selectedRisk,
                        onRequestArmLive = { showConfirmLiveDialog = true },
                        onSwitchToPaper = { onSetExecutionMode(ExecutionMode.PAPER) },
                        onSelectLiveRoute = onSelectLiveRoute,
                        onOpenExchangeConfig = onOpenExchangeConfig
                    )
                }
                item {
                    val isLive = state.executionMode == ExecutionMode.LIVE
                    val currentHolding = vaultState.holdings.firstOrNull { it.symbol == state.symbol }
                    CyberFrameCard(
                        title = if (isLive) {
                            "LIVE ORDER TICKET · ${state.liveExecutionRoute.shortBadge}"
                        } else {
                            "PAPER ORDER TICKET"
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isLive) {
                                    "Live Cap: $${selectedRisk.maxOrderUsd.toInt()}/order (${selectedRisk.label})"
                                } else {
                                    "Available Cash: ${Formatters.formatUsd(vaultState.account.usdBalance)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isLive) LgColors.Turquoise else LgColors.InkSecondary
                            )
                            if (isLive) {
                                StatusPill(
                                    text = "LIVE ARMED",
                                    color = LgColors.Turquoise
                                )
                            } else if (currentHolding != null) {
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
                            label = { Text(if (isLive) "Live Order Notional (USD)" else "Order Notional (USD)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LgColors.Turquoise,
                                unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .circuitTextFieldBackdrop(drawBorder = false)
                                .testTag("paper_order_amount_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val parsed = orderAmountText.toDoubleOrNull() ?: 0.0
                            Button(
                                onClick = { onSubmitPaperOrder(true, parsed) },
                                enabled = !state.isSubmittingLiveOrder,
                                colors = ButtonDefaults.buttonColors(containerColor = LgColors.Turquoise, contentColor = LgColors.ObsidianBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("paper_buy_button")
                            ) {
                                Text(
                                    if (isLive) "LIVE BUY ${state.symbol}" else "Buy ${state.symbol}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { onSubmitPaperOrder(false, parsed) },
                                enabled = !state.isSubmittingLiveOrder,
                                colors = ButtonDefaults.buttonColors(containerColor = LgColors.Crimson, contentColor = LgColors.ObsidianBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("paper_sell_button")
                            ) {
                                Text(
                                    if (isLive) "LIVE SELL ${state.symbol}" else "Sell ${state.symbol}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (state.recentLiveReceipts.isNotEmpty()) {
                            Text(
                                text = "RECENT LIVE DISPATCH RECEIPTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Gold
                            )
                            state.recentLiveReceipts.take(4).forEach { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
                                        .border(1.dp, LgColors.Turquoise.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${r.side} ${r.symbol} · ${Formatters.formatUsd(r.notionalUsd)} (${r.route.shortBadge})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = LgColors.GoldBright
                                        )
                                        Text(
                                            text = "ID: ${r.exchangeOrderId} · ${r.statusText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LgColors.InkSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            TradeSubWorkspace.STRATEGIES -> {
                items(StrategyType.entries, key = { it.id }) { strat ->
                    val selected = strat == state.strategy
                    StrategyChartButtonCard(
                        strategy = strat,
                        selected = selected,
                        liveAction = if (selected) state.liveSignal?.action else null,
                        onClick = { onSelectStrategy(strat) }
                    )
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
    tradeState: TradeUiState,
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
    onSelectBaseCurrency: (BaseFiatCurrency) -> Unit,
    onSelectConnectedExchange: (ConnectedExchange) -> Unit,
    onScanExchangeMarket: () -> Unit,
    onApplyAdaptiveOpportunity: (AdaptiveOpportunity) -> Unit,
    modifier: Modifier = Modifier
) {
    val running = vaultState.account.autopilotRunning
    val fiat = tradeState.baseCurrency
    val fx = tradeState.usdToCadRate

    CyberFrameCard(title = "ADAPTIVE TRADER · ${fiat.code}", modifier = modifier) {
        // 1. Base Fiat Currency Switcher (USD / CAD) + Live FX Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BASE FUNDING CURRENCY",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.Gold
            )
            StatusPill(
                text = "1 USD = ${"%.3f".format(fx)} CAD",
                color = LgColors.Turquoise
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BaseFiatCurrency.entries.forEach { currency ->
                val isSelected = currency == fiat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) LgColors.Turquoise.copy(alpha = 0.22f) else LgColors.ObsidianBg)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) LgColors.GoldBright else LgColors.BorderHairline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectBaseCurrency(currency) }
                        .testTag("adaptive_currency_${currency.code.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currency.displayLabel,
                        style = TabLabelTextStyle,
                        color = if (isSelected) LgColors.GoldBright else LgColors.InkMuted
                    )
                }
            }
        }

        // 2. Connected Exchange Selector (Whole-Exchange Market Scan Source)
        Text(
            text = "CONNECTED EXCHANGE SCANNER",
            style = MaterialTheme.typography.labelSmall,
            color = LgColors.Gold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ConnectedExchange.entries, key = { it.id }) { ex ->
                val selected = ex == tradeState.connectedExchange
                FilterChip(
                    selected = selected,
                    onClick = { onSelectConnectedExchange(ex) },
                    leadingIcon = {
                        ExchangeLogo(
                            exchangeName = ex.shortLabel,
                            size = 18.dp
                        )
                    },
                    label = {
                        Text(
                            text = ex.shortLabel,
                            style = TabLabelTextStyle
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.24f),
                        selectedLabelColor = LgColors.GoldBright
                    ),
                    modifier = Modifier.testTag("adaptive_exchange_${ex.id}")
                )
            }
        }

        // 3. Center Budget Readout in USD or CAD
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
                Text("Live ${fiat.code} Budget", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                StatusPill(
                    text = if (running) "ARMED" else "PAUSED",
                    color = if (running) LgColors.Turquoise else LgColors.Gold
                )
            }
            Text(
                text = Formatters.formatFiat(selectedRisk.maxDailyNotionalUsd, fiat, fx),
                style = MaterialTheme.typography.displaySmall,
                color = LgColors.CyanGlow,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Max Order ${Formatters.formatFiat(selectedRisk.maxOrderUsd, fiat, fx)} · Equity ${Formatters.formatFiat(vaultState.totalPortfolioEquityUsd, fiat, fx)}",
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.InkMuted
            )
        }

        // 4. Whole-Exchange Profit-Optimizing Scanner Results
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LgColors.ObsidianBg.copy(alpha = 0.90f))
                .border(1.dp, LgColors.Turquoise.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EXCHANGE PROFIT OPTIMIZER",
                        style = MaterialTheme.typography.labelMedium,
                        color = LgColors.GoldBright
                    )
                    Text(
                        text = if (tradeState.totalExchangeMarketsScanned > 0) {
                            "Scanned ${tradeState.totalExchangeMarketsScanned} live markets · ${tradeState.venuesScannedLabel}"
                        } else {
                            "Scans all pairs on ${tradeState.connectedExchange.displayName}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LgColors.Turquoise
                    )
                }
                OutlinedButton(
                    onClick = onScanExchangeMarket,
                    enabled = !tradeState.isScanningExchange,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("scan_entire_exchange_button")
                ) {
                    if (tradeState.isScanningExchange) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 6.dp),
                            strokeWidth = 2.dp,
                            color = LgColors.GoldBright
                        )
                    }
                    Text(
                        text = if (tradeState.isScanningExchange) "Scanning..." else "Scan All",
                        style = TabLabelTextStyle,
                        color = LgColors.GoldBright
                    )
                }
            }

            if (tradeState.adaptiveOpportunities.isEmpty() && !tradeState.isScanningExchange) {
                Text(
                    text = "Tap Scan All to analyze every coin across ${tradeState.connectedExchange.displayName} across all quant strategies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.InkSecondary
                )
            } else {
                tradeState.adaptiveOpportunities.take(4).forEachIndexed { idx, opp ->
                    val isCurrent = opp.symbol == tradeState.symbol && opp.recommendedStrategy == tradeState.strategy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCurrent) LgColors.Turquoise.copy(alpha = 0.16f)
                                else LgColors.PanelSlate.copy(alpha = 0.85f)
                            )
                            .border(
                                width = if (isCurrent) 1.4.dp else 1.dp,
                                color = if (isCurrent) LgColors.GoldBright else LgColors.BorderHairline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onApplyAdaptiveOpportunity(opp) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("adaptive_opp_${opp.symbol}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CoinAvatar(opp.symbol, size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "#${idx + 1} ${opp.symbol} ·",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (idx == 0) LgColors.GoldBright else LgColors.InkPrimary
                                    )
                                    ExchangeLogo(opp.exchangeName, size = 16.dp)
                                    Text(
                                        text = opp.exchangeName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (idx == 0) LgColors.GoldBright else LgColors.InkPrimary
                                    )
                                }
                                Text(
                                    text = "${opp.recommendedStrategy.title} · ${Formatters.formatFiat(opp.priceUsd, fiat, fx)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.InkSecondary
                                )
                                Text(
                                    text = "BT ${Formatters.formatPct(opp.backtestReturnPct)} · Win ${opp.winRatePct.toInt()}% · Score ${opp.profitScore.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (opp.backtestReturnPct >= 0) LgColors.Turquoise else LgColors.Gold
                                )
                            }
                        }
                        SignalBadge(opp.signalAction)
                    }
                }
            }
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LgColors.Turquoise,
                    unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .circuitTextFieldBackdrop(drawBorder = false)
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

        // 3 Mini Bottom Stat Tiles in active USD or CAD funding currency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "${fiat.code} Cash",
                value = Formatters.formatFiat(vaultState.account.usdBalance, fiat, fx),
                valueColor = LgColors.CyanGlow,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Daily Used",
                value = Formatters.formatFiat(vaultState.account.dailyNotionalUsedUsd, fiat, fx),
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
    var useTradingViewLive by remember { mutableStateOf(false) }
    var showFullTv by remember { mutableStateOf(false) }

    if (showFullTv) {
        TradingViewFullChartDialog(
            symbol = state.symbol,
            onDismiss = { showFullTv = false }
        )
    }

    CyberFrameCard(title = "MARKET SNAPSHOT · LIVE CHART", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveAutoFeedBadge(label = "LIVE FEED")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !useTradingViewLive,
                    onClick = { useTradingViewLive = false },
                    label = { Text("OHLC", style = TabLabelTextStyle) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                        selectedLabelColor = LgColors.Gold
                    )
                )
                FilterChip(
                    selected = useTradingViewLive,
                    onClick = { useTradingViewLive = true },
                    label = { Text("TRADINGVIEW", style = TabLabelTextStyle) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                        selectedLabelColor = LgColors.Gold
                    )
                )
                FilterChip(
                    selected = false,
                    onClick = { showFullTv = true },
                    label = { Text("FULL", style = TabLabelTextStyle, color = LgColors.Gold) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChartTimeframe.entries.forEach { tf ->
                FilterChip(
                    selected = tf == state.timeframe,
                    onClick = { onSelectTimeframe(tf) },
                    label = { Text(tf.label, style = TabLabelTextStyle) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!useTradingViewLive) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ChartOverlay.entries) { ov ->
                    FilterChip(
                        selected = ov == state.overlay,
                        onClick = { onSelectOverlay(ov) },
                        label = { Text(ov.label) }
                    )
                }
            }
        }

        if (useTradingViewLive) {
            val tvInterval = when (state.timeframe) {
                ChartTimeframe.M15 -> "15"
                ChartTimeframe.H1 -> "60"
                ChartTimeframe.H4 -> "240"
                ChartTimeframe.D1 -> "D"
            }
            TradingViewLiveChartWebView(
                symbol = state.symbol,
                interval = tvInterval,
                height = 290.dp
            )
        } else {
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

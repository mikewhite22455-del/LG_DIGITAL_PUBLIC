package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CoinDropdownSelector
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.SentimentArcGauges
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import com.example.viewmodel.AdvisorUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

@Composable
fun AdvisorScreen(
    advisorState: AdvisorUiState,
    marketsState: MarketsUiState,
    tradeState: TradeUiState,
    vaultState: VaultUiState,
    isWide: Boolean = false,
    onSendAdvisorPrompt: (String) -> Unit,
    onSelectStrategy: (StrategyType) -> Unit,
    onSelectCoin: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }

    val quickPrompts = remember {
        listOf(
            "Analyze active setup & signal",
            "Review paper drawdown risk",
            "How do fees affect spot arb?",
            "Supertrend vs Bollinger here"
        )
    }

    val tickers = (marketsState.tickersState as? UiState.Success)?.data ?: emptyList()
    val contextCoins = remember(tickers) {
        listOf("BTC", "ETH", "SOL", "XRP").mapNotNull { sym ->
            tickers.firstOrNull { it.symbol == sym }
        }
    }

    // Compute live market sentiment breakdown dynamically from MarketBreadth
    val (bullishPct, bearishPct, neutralPct) = remember(marketsState.breadth, tickers) {
        val b = marketsState.breadth
        if (b == null || b.totalAssets == 0) {
            Triple(45f, 25f, 30f)
        } else {
            val neutralCount = tickers.count { kotlin.math.abs(it.change24hPct) < 1.25 }
            val strongBull = tickers.count { it.change24hPct >= 1.25 }
            val strongBear = tickers.count { it.change24hPct <= -1.25 }
            val total = (strongBull + strongBear + neutralCount).coerceAtLeast(1)
            Triple(
                (strongBull * 100f) / total,
                (strongBear * 100f) / total,
                (neutralCount * 100f) / total
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Advisor Hero Portrait + Streaming Chat (clean robot portrait, no descriptive blurb beside robot head)
        item {
            CyberFrameCard(title = "TRADING ADVISOR ANALYSIS") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_advisor_robot),
                        contentDescription = "Trading Advisor",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.2.dp, LgColors.Gold.copy(alpha = 0.85f), CircleShape)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("${tradeState.symbol} ${tradeState.timeframe.label}", LgColors.Turquoise)
                        StatusPill(tradeState.strategy.title, LgColors.Gold)
                    }
                    CoinDropdownSelector(
                        selectedSymbol = tradeState.symbol,
                        symbols = CoinAssets.catalog.map { it.symbol },
                        onSelectSymbol = onSelectCoin,
                        label = "Active Advisor Coin",
                        testTag = "advisor_coin_dropdown"
                    )
                }

                // Conversation Thread
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    advisorState.messages.takeLast(8).forEach { msg ->
                        val bg = when {
                            msg.isError -> LgColors.Crimson.copy(alpha = 0.14f)
                            msg.isUser -> LgColors.Gold.copy(alpha = 0.18f)
                            else -> LgColors.ElevatedSlate.copy(alpha = 0.85f)
                        }
                        val borderColor = when {
                            msg.isError -> LgColors.Crimson.copy(alpha = 0.6f)
                            msg.isUser -> LgColors.Gold.copy(alpha = 0.55f)
                            else -> LgColors.CyanGlow.copy(alpha = 0.45f)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (msg.isUser) "OPERATOR" else if (msg.isError) "SYSTEM ERROR" else "AI ADVISOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (msg.isUser) LgColors.Gold else if (msg.isError) LgColors.Crimson else LgColors.CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkPrimary
                            )
                        }
                    }

                    if (advisorState.isGenerating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LgColors.ElevatedSlate)
                                .border(1.dp, LgColors.Turquoise.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LgColors.Turquoise)
                                Text("Streaming analysis...", style = MaterialTheme.typography.labelSmall, color = LgColors.Turquoise)
                            }
                            if (advisorState.streamingReply.isNotBlank()) {
                                Text(advisorState.streamingReply, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkPrimary)
                            }
                        }
                    }
                }

                // Quick Prompt Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { qp ->
                        FilterChip(
                            selected = false,
                            onClick = { onSendAdvisorPrompt(qp) },
                            label = { Text(qp) }
                        )
                    }
                }

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Type a question for Quant Copilot...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LgColors.Turquoise,
                            unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .circuitTextFieldBackdrop(drawBorder = false)
                            .testTag("advisor_prompt_input")
                    )
                    Button(
                        onClick = {
                            val text = promptInput
                            promptInput = ""
                            onSendAdvisorPrompt(text)
                        },
                        enabled = !advisorState.isGenerating && promptInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = LgColors.CyanGlow, contentColor = LgColors.ObsidianBg),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("advisor_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt")
                    }
                }
            }
        }

        // Strategy & Diagnostics + Risk Guards & Sentiment (matches Image 4 left & right panes)
        item {
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    StrategyDiagnosticsCard(
                        tradeState = tradeState,
                        contextCoins = contextCoins,
                        onSelectStrategy = onSelectStrategy,
                        onSelectCoin = onSelectCoin,
                        modifier = Modifier.weight(1f)
                    )
                    RiskGuardsSentimentCard(
                        vaultState = vaultState,
                        bullishPct = bullishPct,
                        bearishPct = bearishPct,
                        neutralPct = neutralPct,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StrategyDiagnosticsCard(
                        tradeState = tradeState,
                        contextCoins = contextCoins,
                        onSelectStrategy = onSelectStrategy,
                        onSelectCoin = onSelectCoin
                    )
                    RiskGuardsSentimentCard(
                        vaultState = vaultState,
                        bullishPct = bullishPct,
                        bearishPct = bearishPct,
                        neutralPct = neutralPct
                    )
                }
            }
        }
    }
}

@Composable
private fun StrategyDiagnosticsCard(
    tradeState: TradeUiState,
    contextCoins: List<com.example.domain.models.MarketTicker>,
    onSelectStrategy: (StrategyType) -> Unit,
    onSelectCoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            "Spot Bot Single-Pair" to StrategyType.TREND_CROSS,
            "Supertrend Momentum" to StrategyType.SUPERTREND,
            "Multi-Pair Grid" to StrategyType.MULTI_PAIR_GRID,
            "Bollinger Reversion" to StrategyType.BOLLINGER_REVERSION
        )
    }

    CyberFrameCard(title = "STRATEGY & DIAGNOSTICS", modifier = modifier) {
        Text("ACTIVE STRATEGY PRESETS", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
        presets.forEach { (label, strat) ->
            val selected = tradeState.strategy == strat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) LgColors.CyanGlow.copy(alpha = 0.14f) else LgColors.ElevatedSlate.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = if (selected) LgColors.CyanGlow else LgColors.BorderHairline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectStrategy(strat) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) LgColors.CyanGlow else LgColors.InkPrimary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                StatusPill(
                    text = if (selected) "ACTIVE" else "SELECT",
                    color = if (selected) LgColors.Turquoise else LgColors.InkMuted
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LgColors.ElevatedSlate.copy(alpha = 0.55f))
                .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExchangeLogo("Kraken", size = 18.dp)
                    ExchangeLogo("KuCoin", size = 18.dp)
                    Text("Feed: Kraken / KuCoin Spot", style = MaterialTheme.typography.labelMedium, color = LgColors.Turquoise)
                }
                Text(
                    text = "Signal: ${tradeState.liveSignal?.action?.name ?: "LOADING"} · Backtest ${tradeState.backtestResult?.let { Formatters.formatPct(it.totalReturnPct) } ?: "--"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkSecondary
                )
            }
            StatusPill("CONNECTED", LgColors.Turquoise)
        }

        if (contextCoins.isNotEmpty()) {
            Text("COIN CONTEXT", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
            contextCoins.forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LgColors.ElevatedSlate.copy(alpha = 0.55f))
                        .border(1.dp, if (c.symbol == tradeState.symbol) LgColors.Gold else LgColors.BorderHairline, RoundedCornerShape(10.dp))
                        .clickable { onSelectCoin(c.symbol) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CoinAvatar(c.symbol, size = 24.dp)
                        Text(c.symbol, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Formatters.formatPct(c.change24hPct),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (c.change24hPct >= 0) LgColors.Turquoise else LgColors.Crimson
                        )
                        Text(Formatters.formatUsd(c.priceUsd), style = MaterialTheme.typography.labelLarge, color = LgColors.Gold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskGuardsSentimentCard(
    vaultState: VaultUiState,
    bullishPct: Float,
    bearishPct: Float,
    neutralPct: Float,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(title = "RISK GUARDS & SENTIMENT", modifier = modifier) {
        Text("LIVE RISK GUARDS", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
        GlassPanelCard(highlightBorder = LgColors.BorderHairline) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = LgColors.Turquoise, modifier = Modifier.size(16.dp))
                        Text("Autopilot Engine", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = if (vaultState.account.autopilotRunning) "Active" else "Standby",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (vaultState.account.autopilotRunning) LgColors.Turquoise else LgColors.Gold
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Drawdown Guard", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                    Text(
                        text = "${"%.2f".format(vaultState.currentDrawdownPct)}% / ${vaultState.account.maxDrawdownPct}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (vaultState.currentDrawdownPct < vaultState.account.maxDrawdownPct) LgColors.Turquoise else LgColors.Crimson
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Stop Window", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                    Text(
                        text = vaultState.account.stopSchedule.ifBlank { "Off" },
                        style = MaterialTheme.typography.labelLarge,
                        color = LgColors.CyanGlow
                    )
                }
            }
        }

        Text("MARKET SENTIMENT", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
        SentimentArcGauges(
            bullishPct = bullishPct,
            bearishPct = bearishPct,
            neutralPct = neutralPct
        )

        Text("RECENT AGENT ALERTS", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
        if (vaultState.agentMemory.isEmpty()) {
            Text(
                text = "No risk guard triggers logged yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = LgColors.InkMuted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                vaultState.agentMemory.take(3).forEach { mem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LgColors.ElevatedSlate.copy(alpha = 0.65f))
                            .border(1.dp, LgColors.Gold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${mem.eventType} · ${mem.symbol}", style = MaterialTheme.typography.labelMedium, color = LgColors.Gold)
                            Text(
                                text = mem.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.SignalAction
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CoinDropdownSelector
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SentimentArcGauges
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import com.example.ui.theme.TradingViewFontFamily
import com.example.viewmodel.AdvisorUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdvisorScreen(
    advisorState: AdvisorUiState,
    marketsState: MarketsUiState,
    tradeState: TradeUiState,
    vaultState: VaultUiState,
    isWide: Boolean = false,
    onSendAdvisorPrompt: (String) -> Unit,
    onRetryLastPrompt: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onRefreshContext: () -> Unit = {},
    onSelectStrategy: (StrategyType) -> Unit,
    onSelectCoin: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    var showContextDetails by remember { mutableStateOf(false) }
    val chatScrollState = rememberLazyListState()

    val tickers = (marketsState.tickersState as? UiState.Success)?.data ?: emptyList()
    val activeTicker = remember(tickers, tradeState.symbol) {
        tickers.firstOrNull { it.symbol.equals(tradeState.symbol, ignoreCase = true) }
    }
    val fearGreed = (marketsState.fearGreedState as? UiState.Success)?.data
    val contextCoins = remember(tickers) {
        listOf("BTC", "ETH", "SOL", "XRP").mapNotNull { sym ->
            tickers.firstOrNull { it.symbol == sym }
        }
    }

    val syncedTimeLabel = remember(advisorState.contextUpdatedAtMs) {
        if (advisorState.contextUpdatedAtMs <= 0L) "Syncing..."
        else SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(advisorState.contextUpdatedAtMs))
    }

    // Dynamic context-aware quick prompts reflecting live symbol, price, strategy, and market regime
    val quickPrompts = remember(
        tradeState.symbol,
        tradeState.strategy,
        tradeState.isLiveMode,
        activeTicker?.priceUsd,
        fearGreed?.liveValue,
        tradeState.indicatorBacktestResult != null
    ) {
        buildList {
            val pxLabel = activeTicker?.let { " at ${Formatters.formatUsd(it.priceUsd)}" }.orEmpty()
            add("Analyze ${tradeState.symbol}$pxLabel (${tradeState.strategy.title})")
            add(
                if (tradeState.isLiveMode) "Review my Live ${tradeState.liveExecutionRoute.shortBadge} balance & risk"
                else "Audit my Paper portfolio drawdown & sizing"
            )
            if (fearGreed != null) {
                add("What does Fear & Greed ${fearGreed.liveValue} (${fearGreed.liveClassification}) mean for ${tradeState.symbol}?")
            }
            if (tradeState.indicatorBacktestResult != null) {
                add("Critique my ${tradeState.symbol} backtest performance & alpha")
            } else {
                add("Best entry & stop-loss structure for ${tradeState.symbol} ${tradeState.timeframe.label}")
            }
            add("Top market movers & spot arbitrage edge right now")
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

    LaunchedEffect(
        advisorState.messages.size,
        advisorState.isGenerating,
        advisorState.streamingReply.length
    ) {
        val total = advisorState.messages.size + if (advisorState.isGenerating) 1 else 0
        if (total > 0) {
            chatScrollState.animateScrollToItem(total - 1)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Trading Advisor Chat Desk with Live Gemini Market Context
        item {
            CyberFrameCard(title = "GEMINI AI TRADING ADVISOR") {
                // Header Row with Avatar, Status Badges, Sync & Clear Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_advisor_robot),
                            contentDescription = "AI Trading Advisor",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(2.dp, LgColors.GoldBright, CircleShape)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = LgColors.Turquoise,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "GEMINI 3.8 FLASH",
                                    fontFamily = TradingViewFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LgColors.GoldBright
                                )
                            }
                            Text(
                                text = "Live market telemetry synced · $syncedTimeLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Turquoise,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onRefreshContext,
                            enabled = !advisorState.isRefreshingContext,
                            modifier = Modifier.testTag("advisor_refresh_context_button")
                        ) {
                            if (advisorState.isRefreshingContext) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LgColors.Turquoise
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "Sync live market data context",
                                    tint = LgColors.Turquoise
                                )
                            }
                        }
                        IconButton(
                            onClick = onClearChat,
                            enabled = !advisorState.isGenerating,
                            modifier = Modifier.testTag("advisor_clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Reset advisor conversation",
                                tint = LgColors.Gold
                            )
                        }
                    }
                }

                // Active Asset Selector (auto-updates Gemini market context on selection)
                CoinDropdownSelector(
                    selectedSymbol = tradeState.symbol,
                    symbols = CoinAssets.catalog.map { it.symbol },
                    onSelectSymbol = onSelectCoin,
                    label = "Active Market Context Coin",
                    testTag = "advisor_coin_dropdown"
                )

                // Live Market Context Telemetry Strip (updates automatically with market data)
                val sigAction = tradeState.liveSignal?.action
                val sigColor = when (sigAction) {
                    SignalAction.BUY -> LgColors.Turquoise
                    SignalAction.SELL -> LgColors.Crimson
                    else -> LgColors.GoldBright
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        label = "${tradeState.symbol} Spot",
                        value = activeTicker?.let { Formatters.formatUsd(it.priceUsd) } ?: "--",
                        valueColor = LgColors.GoldBright,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "24h Move",
                        value = activeTicker?.let { Formatters.formatPct(it.change24hPct) } ?: "--",
                        valueColor = if ((activeTicker?.change24hPct ?: 0.0) >= 0.0) LgColors.Turquoise else LgColors.Crimson,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Signal",
                        value = tradeState.liveSignal?.let { "${it.action.name} ${(it.confidence * 100).toInt()}%" } ?: "WAIT",
                        valueColor = sigColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expandable Live Market Context Feed Accordion (proves live grounding payload sent to Gemini)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
                        .border(1.dp, LgColors.Turquoise.copy(alpha = 0.42f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { showContextDetails = !showContextDetails }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("advisor_context_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            StatusPill(
                                text = if (tradeState.isLiveMode) "LIVE · ${tradeState.liveExecutionRoute.shortBadge}" else "PAPER",
                                color = if (tradeState.isLiveMode) LgColors.Turquoise else LgColors.Gold
                            )
                            if (fearGreed != null) {
                                StatusPill(
                                    text = "F&G ${fearGreed.liveValue}",
                                    color = if (fearGreed.liveValue >= 50) LgColors.Turquoise else LgColors.AmberWarn
                                )
                            }
                            Text(
                                text = "Live Context Feed",
                                style = MaterialTheme.typography.labelMedium,
                                color = LgColors.InkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = if (showContextDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (showContextDetails) "Hide live market context" else "Inspect live market context",
                            tint = LgColors.Turquoise
                        )
                    }

                    AnimatedVisibility(visible = showContextDetails) {
                        Text(
                            text = advisorState.liveContextPreview.ifBlank { "Syncing live market telemetry..." },
                            fontFamily = TradingViewFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = LgColors.InkSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                                .testTag("advisor_live_context_text")
                        )
                    }
                }

                // Auto-Scrolling Chat Conversation Transcript
                LazyColumn(
                    state = chatScrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 210.dp, max = 360.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LgColors.ObsidianBg.copy(alpha = 0.65f))
                        .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .testTag("advisor_chat_transcript"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(advisorState.messages, key = { it.id }) { msg ->
                        val isUser = msg.isUser
                        val bg = when {
                            msg.isError -> LgColors.Crimson.copy(alpha = 0.14f)
                            isUser -> LgColors.Gold.copy(alpha = 0.18f)
                            else -> LgColors.ElevatedSlate.copy(alpha = 0.9f)
                        }
                        val borderColor = when {
                            msg.isError -> LgColors.Crimson.copy(alpha = 0.65f)
                            isUser -> LgColors.GoldBright.copy(alpha = 0.6f)
                            else -> LgColors.Turquoise.copy(alpha = 0.5f)
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .fillMaxWidth(if (isUser) 0.9f else 0.96f)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 14.dp
                                        )
                                    )
                                    .background(bg)
                                    .border(
                                        1.dp,
                                        borderColor,
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 14.dp
                                        )
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            isUser -> "OPERATOR"
                                            msg.isError -> "SYSTEM ERROR"
                                            else -> "GEMINI ADVISOR"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            isUser -> LgColors.GoldBright
                                            msg.isError -> LgColors.Crimson
                                            else -> LgColors.Turquoise
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.US).format(Date(msg.timestampMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.InkMuted
                                    )
                                }
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LgColors.InkPrimary
                                )
                                if (msg.isError && !advisorState.isGenerating) {
                                    TextButton(
                                        onClick = onRetryLastPrompt,
                                        modifier = Modifier.testTag("advisor_retry_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            tint = LgColors.GoldBright,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(end = 4.dp)
                                        )
                                        Text("Retry Request", color = LgColors.GoldBright)
                                    }
                                }
                            }
                        }
                    }

                    if (advisorState.isGenerating) {
                        item(key = "streaming_reply") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.96f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(LgColors.ElevatedSlate)
                                    .border(1.dp, LgColors.Turquoise.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp,
                                        color = LgColors.Turquoise
                                    )
                                    Text(
                                        text = "Analyzing live ${tradeState.symbol} telemetry...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.Turquoise
                                    )
                                }
                                if (advisorState.streamingReply.isNotBlank()) {
                                    Text(
                                        text = advisorState.streamingReply,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LgColors.InkPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Dynamic Context-Aware Quick Prompt Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { qp ->
                        FilterChip(
                            selected = false,
                            enabled = !advisorState.isGenerating,
                            onClick = { onSendAdvisorPrompt(qp) },
                            label = { Text(qp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = LgColors.ElevatedSlate.copy(alpha = 0.85f),
                                labelColor = LgColors.InkPrimary
                            )
                        )
                    }
                }

                // Message Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = { Text("Ask Gemini about ${tradeState.symbol}, risk, or strategy...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val text = promptInput.trim()
                                if (!advisorState.isGenerating && text.isNotBlank()) {
                                    promptInput = ""
                                    onSendAdvisorPrompt(text)
                                }
                            }
                        ),
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
                            val text = promptInput.trim()
                            if (text.isNotBlank()) {
                                promptInput = ""
                                onSendAdvisorPrompt(text)
                            }
                        },
                        enabled = !advisorState.isGenerating && promptInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LgColors.Turquoise,
                            contentColor = LgColors.ObsidianBg
                        ),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("advisor_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt")
                    }
                }
            }
        }

        // Strategy & Diagnostics + Risk Guards & Sentiment
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

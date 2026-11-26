package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.Formatters
import com.example.domain.models.UiState
import com.example.ui.theme.LgColors
import com.example.ui.theme.TradingViewFontFamily
import com.example.viewmodel.AdvisorUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Floating bottom-right AI Chat Box launcher button displayed across every screen.
 */
@Composable
fun FloatingAiChatFab(
    isGenerating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(62.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        LgColors.TealAmbient,
                        LgColors.ObsidianBg
                    )
                )
            )
            .border(2.2.dp, LgColors.GoldBright, CircleShape)
            .clickable(onClick = onClick)
            .testTag("floating_ai_chat_fab"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(1.5.dp, LgColors.Turquoise, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_advisor_robot),
                contentDescription = "Open floating AI chat box",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(22.dp)
                .clip(CircleShape)
                .background(LgColors.Turquoise)
                .border(1.4.dp, LgColors.ObsidianBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = LgColors.ObsidianBg
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ChatBubble,
                    contentDescription = null,
                    tint = LgColors.ObsidianBg,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Pop-up Floating AI Chat Box Sheet accessible from any page via the bottom-right FAB,
 * grounded live in the current market tickers, Fear & Greed index, signal, and portfolio state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingAiChatBoxSheet(
    advisorState: AdvisorUiState,
    activeSymbol: String,
    marketsState: MarketsUiState? = null,
    tradeState: TradeUiState? = null,
    onSendPrompt: (String) -> Unit,
    onRetryLastPrompt: () -> Unit = {},
    onRefreshContext: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onOpenFullAdvisor: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showLiveContext by remember { mutableStateOf(false) }
    val chatListState = rememberLazyListState()
    val visibleMessages = remember(advisorState.messages) { advisorState.messages.takeLast(14) }

    val tickers = (marketsState?.tickersState as? UiState.Success)?.data.orEmpty()
    val activeTicker = remember(tickers, activeSymbol) {
        tickers.firstOrNull { it.symbol.equals(activeSymbol, ignoreCase = true) }
    }
    val fearGreed = (marketsState?.fearGreedState as? UiState.Success)?.data
    val updatedLabel = remember(advisorState.contextUpdatedAtMs) {
        if (advisorState.contextUpdatedAtMs <= 0L) "Live"
        else SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(advisorState.contextUpdatedAtMs))
    }

    val quickPrompts = remember(activeSymbol, activeTicker?.priceUsd, fearGreed?.liveValue) {
        listOf(
            "Quick $activeSymbol${activeTicker?.let { " (${Formatters.formatUsd(it.priceUsd)})" }.orEmpty()} outlook",
            "Best strategy right now?",
            "Check my risk & drawdown",
            if (fearGreed != null) "Fear & Greed (${fearGreed.liveValue}) read" else "Top market movers today"
        )
    }

    LaunchedEffect(visibleMessages.size, advisorState.isGenerating, advisorState.streamingReply.length) {
        val totalItems = visibleMessages.size + if (advisorState.isGenerating) 1 else 0
        if (totalItems > 0) {
            chatListState.animateScrollToItem(totalItems - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LgColors.PanelSlate,
        modifier = Modifier.testTag("floating_ai_chat_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.8.dp, LgColors.GoldBright, CircleShape)
                    )
                    Column {
                        Text(
                            text = "LG AI ADVISOR · GEMINI",
                            fontFamily = TradingViewFontFamily,
                            fontSize = 14.sp,
                            color = LgColors.GoldBright
                        )
                        Text(
                            text = buildString {
                                append(activeSymbol)
                                activeTicker?.let {
                                    append(" · ${Formatters.formatUsd(it.priceUsd)} (${Formatters.formatPct(it.change24hPct)})")
                                }
                                append(" · $updatedLabel")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshContext,
                    enabled = !advisorState.isRefreshingContext,
                    modifier = Modifier.testTag("floating_ai_refresh_context")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = "Sync live market data",
                        tint = LgColors.Turquoise
                    )
                }
                IconButton(
                    onClick = onClearChat,
                    enabled = !advisorState.isGenerating,
                    modifier = Modifier.testTag("floating_ai_clear_chat")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear AI chat",
                        tint = LgColors.Gold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close AI Chat",
                        tint = LgColors.GoldBright
                    )
                }
            }

            // Compact Live Market Context Status Strip (auto-updates from live market telemetry)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
                    .border(1.dp, LgColors.Turquoise.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLiveContext = !showLiveContext }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        StatusPill(
                            text = if (tradeState?.isLiveMode == true) "LIVE" else "PAPER",
                            color = if (tradeState?.isLiveMode == true) LgColors.Turquoise else LgColors.Gold
                        )
                        tradeState?.liveSignal?.let { sig ->
                            StatusPill(
                                text = "${sig.action.name} ${(sig.confidence * 100).toInt()}%",
                                color = LgColors.CyanGlow
                            )
                        }
                        fearGreed?.let { fg ->
                            StatusPill(
                                text = "F&G ${fg.liveValue}",
                                color = if (fg.liveValue >= 50) LgColors.Turquoise else LgColors.AmberWarn
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenFullAdvisor()
                            },
                            border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.6f)),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = LgColors.Turquoise,
                                modifier = Modifier.size(13.dp).padding(end = 3.dp)
                            )
                            Text("Desk", color = LgColors.Turquoise, fontSize = 11.sp)
                        }
                    }
                    Icon(
                        imageVector = if (showLiveContext) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle live market context",
                        tint = LgColors.Turquoise,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showLiveContext) {
                    Text(
                        text = advisorState.liveContextPreview.ifBlank { "Syncing live market context..." },
                        fontFamily = TradingViewFontFamily,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = LgColors.InkSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                    )
                }
            }

            // Recent Messages
            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 270.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .circuitTextFieldBackdrop(cornerRadius = 12.dp)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (visibleMessages.isEmpty() && !advisorState.isGenerating) {
                    item {
                        Text(
                            text = "Ask Gemini about $activeSymbol, live strategies, arbitrage spreads, or portfolio risk.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LgColors.InkSecondary
                        )
                    }
                }
                items(visibleMessages, key = { it.id }) { msg ->
                    val bg = when {
                        msg.isError -> LgColors.Crimson.copy(alpha = 0.16f)
                        msg.isUser -> LgColors.Gold.copy(alpha = 0.18f)
                        else -> LgColors.ElevatedSlate.copy(alpha = 0.85f)
                    }
                    val border = when {
                        msg.isError -> LgColors.Crimson.copy(alpha = 0.65f)
                        msg.isUser -> LgColors.GoldBright.copy(alpha = 0.6f)
                        else -> LgColors.Turquoise.copy(alpha = 0.55f)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bg)
                            .border(1.dp, border, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (msg.isUser) "YOU" else if (msg.isError) "SYSTEM" else "GEMINI ADVISOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isUser) LgColors.GoldBright else if (msg.isError) LgColors.Crimson else LgColors.Turquoise,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = LgColors.InkPrimary
                        )
                        if (msg.isError && !advisorState.isGenerating) {
                            TextButton(
                                onClick = onRetryLastPrompt,
                                modifier = Modifier.testTag("floating_ai_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = LgColors.GoldBright,
                                    modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                )
                                Text("Retry", color = LgColors.GoldBright, fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (advisorState.isGenerating) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LgColors.ElevatedSlate)
                                .border(1.dp, LgColors.Turquoise.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = LgColors.Turquoise
                                )
                                Text(
                                    text = "Analyzing live $activeSymbol market data...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise
                                )
                            }
                            if (advisorState.streamingReply.isNotBlank()) {
                                Text(
                                    text = advisorState.streamingReply,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Quick prompts
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPrompts) { prompt ->
                    FilterChip(
                        selected = false,
                        enabled = !advisorState.isGenerating,
                        onClick = { onSendPrompt(prompt) },
                        label = { Text(prompt, fontSize = 11.sp) }
                    )
                }
            }

            // Message input bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Gemini AI Advisor...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val clean = inputText.trim()
                            if (!advisorState.isGenerating && clean.isNotBlank()) {
                                inputText = ""
                                onSendPrompt(clean)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LgColors.Turquoise,
                        unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.65f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .circuitTextFieldBackdrop(cornerRadius = 12.dp, drawBorder = false)
                        .testTag("floating_ai_chat_input")
                )
                Button(
                    onClick = {
                        val clean = inputText.trim()
                        if (clean.isNotBlank()) {
                            inputText = ""
                            onSendPrompt(clean)
                        }
                    },
                    enabled = !advisorState.isGenerating && inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgColors.Turquoise,
                        contentColor = LgColors.ObsidianBg
                    ),
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("floating_ai_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send AI message"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LgColors
import com.example.ui.theme.TradingViewFontFamily
import com.example.viewmodel.AdvisorUiState

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

        // Crisp bright turquoise & gold AI chat bubble badge in bottom-right of FAB
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
 * Pop-up Floating AI Chat Box Sheet accessible from any page via the bottom-right FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingAiChatBoxSheet(
    advisorState: AdvisorUiState,
    activeSymbol: String,
    onSendPrompt: (String) -> Unit,
    onOpenFullAdvisor: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val quickPrompts = remember(activeSymbol) {
        listOf(
            "Quick $activeSymbol outlook",
            "Best strategy right now?",
            "Check my risk & drawdown",
            "Explain Fear & Greed signal"
        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "LG AI CHAT",
                            fontFamily = TradingViewFontFamily,
                            fontSize = 15.sp,
                            color = LgColors.GoldBright
                        )
                        Text(
                            text = "Live Quant Copilot · $activeSymbol",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenFullAdvisor()
                    },
                    border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.7f)),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = LgColors.Turquoise,
                        modifier = Modifier.size(15.dp).padding(end = 4.dp)
                    )
                    Text("Full Desk", color = LgColors.Turquoise, fontSize = 11.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close AI Chat",
                        tint = LgColors.GoldBright
                    )
                }
            }

            // Recent Messages
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .circuitTextFieldBackdrop(cornerRadius = 12.dp)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (advisorState.messages.isEmpty() && !advisorState.isGenerating) {
                    item {
                        Text(
                            text = "Ask me anything about $activeSymbol, live strategies, arbitrage spreads, sports odds, or mining yields.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LgColors.InkSecondary
                        )
                    }
                }
                items(advisorState.messages.takeLast(8), key = { it.id }) { msg ->
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
                            text = if (msg.isUser) "YOU" else if (msg.isError) "SYSTEM" else "AI ADVISOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isUser) LgColors.GoldBright else if (msg.isError) LgColors.Crimson else LgColors.Turquoise,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = LgColors.InkPrimary
                        )
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
                                    text = "Thinking...",
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
                    placeholder = { Text("Ask LG AI Advisor...") },
                    singleLine = true,
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

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.models.ExecutionMode
import com.example.domain.models.LiveExecutionRoute
import com.example.domain.models.RiskProfile
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.TradeUiState

@Composable
fun LiveServerAndExecutionCard(
    state: TradeUiState,
    selectedRisk: RiskProfile,
    onRequestArmLive: () -> Unit,
    onSwitchToPaper: () -> Unit,
    onSelectLiveRoute: (LiveExecutionRoute) -> Unit,
    onOpenExchangeConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLive = state.executionMode == ExecutionMode.LIVE

    CyberFrameCard(
        title = if (isLive) "LIVE EXECUTION ENGINE · ARMED" else "LIVE LG SERVERS & EXECUTION MODE",
        modifier = modifier
    ) {
        // 1. PAPER vs LIVE mode switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (!isLive) LgColors.Gold.copy(alpha = 0.22f) else LgColors.ObsidianBg.copy(alpha = 0.72f))
                    .border(
                        width = if (!isLive) 1.5.dp else 1.dp,
                        color = if (!isLive) LgColors.GoldBright else LgColors.BorderHairline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSwitchToPaper() }
                    .testTag("mode_switch_paper"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAPER MODE",
                    style = TabLabelTextStyle,
                    color = if (!isLive) LgColors.GoldBright else LgColors.InkMuted
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLive) LgColors.Turquoise.copy(alpha = 0.24f) else LgColors.ObsidianBg.copy(alpha = 0.72f))
                    .border(
                        width = if (isLive) 1.5.dp else 1.dp,
                        color = if (isLive) LgColors.Turquoise else LgColors.BorderHairline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { if (!isLive) onRequestArmLive() }
                    .testTag("mode_switch_live"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (isLive) LgColors.Turquoise else LgColors.Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isLive) "LIVE ARMED" else "SWITCH TO LIVE",
                        style = TabLabelTextStyle,
                        color = if (isLive) LgColors.Turquoise else LgColors.GoldBright
                    )
                }
            }
        }

        // 2. Execution Route Selector (Kraken Pro, Coinbase, KuCoin, Bitstamp)
        Text(
            text = "LIVE EXECUTION ROUTE (CAP $${selectedRisk.maxOrderUsd.toInt()}/ORDER)",
            style = MaterialTheme.typography.labelSmall,
            color = LgColors.Gold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LiveExecutionRoute.entries, key = { it.id }) { route ->
                val selected = route == state.liveExecutionRoute
                val status = state.liveConnectors.firstOrNull { it.route == route }
                val ready = status?.isConfigured == true
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) LgColors.Turquoise.copy(alpha = 0.22f) else LgColors.ObsidianBg.copy(alpha = 0.72f))
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) LgColors.GoldBright else LgColors.BorderHairline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectLiveRoute(route) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("live_route_${route.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = route.shortBadge,
                        style = TabLabelTextStyle,
                        color = if (selected) LgColors.GoldBright else LgColors.InkPrimary
                    )
                    StatusPill(
                        text = if (ready) "READY" else "SECRETS",
                        color = if (ready) LgColors.Turquoise else LgColors.Gold
                    )
                }
            }
        }

        val activeStatus = state.liveConnectors.firstOrNull { it.route == state.liveExecutionRoute }
        if (activeStatus != null) {
            Text(
                text = activeStatus.detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (activeStatus.isConfigured) LgColors.Turquoise else LgColors.InkSecondary
            )
        }

        GoldMetallicButton(
            text = "EXCHANGE CONFIGURATION (ENCRYPTED KEYS)",
            onClick = onOpenExchangeConfig,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trade_open_exchange_config_button")
        )
    }
}

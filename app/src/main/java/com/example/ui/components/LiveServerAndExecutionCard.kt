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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.models.ExecutionMode
import com.example.domain.models.LiveExecutionRoute
import com.example.domain.models.RiskProfile
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.TradeUiState

private val MobileDirectRoutes = listOf(
    LiveExecutionRoute.KRAKEN_DIRECT,
    LiveExecutionRoute.COINBASE_DIRECT,
    LiveExecutionRoute.KUCOIN_DIRECT,
    LiveExecutionRoute.BITSTAMP_DIRECT
)

@Composable
fun LiveServerAndExecutionCard(
    state: TradeUiState,
    selectedRisk: RiskProfile,
    onRequestArmLive: () -> Unit,
    onSwitchToPaper: () -> Unit,
    onSelectLiveRoute: (LiveExecutionRoute) -> Unit,
    onRefreshLiveBalance: () -> Unit = {},
    onOpenExchangeConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLive = state.executionMode == ExecutionMode.LIVE
    val liveSnap = state.liveAccountSnapshot
    val fiat = state.baseCurrency
    val fx = state.usdToCadRate

    CyberFrameCard(
        title = if (isLive) "ANDROID COMMAND · LIVE ARMED" else "ANDROID COMMAND · EXECUTION MODE",
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

        // Immediate Live Exchange Balance & Budget Readout when LIVE is armed
        if (isLive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LgColors.ObsidianBg.copy(alpha = 0.90f))
                    .border(1.2.dp, LgColors.Turquoise.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE ${state.liveExecutionRoute.shortBadge} BALANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                state.isSyncingLiveBalance -> "Syncing live account balances..."
                                liveSnap?.isConfigured == false -> liveSnap.statusMessage
                                state.liveBalanceError != null -> state.liveBalanceError
                                liveSnap?.isSynced == true -> liveSnap.statusMessage
                                else -> "Tap refresh to sync live balance"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.liveBalanceError != null || liveSnap?.isConfigured == false) LgColors.GoldBright else LgColors.InkSecondary
                        )
                    }
                    IconButton(
                        onClick = onRefreshLiveBalance,
                        enabled = !state.isSyncingLiveBalance,
                        modifier = Modifier.testTag("live_balance_sync_button")
                    ) {
                        if (state.isSyncingLiveBalance) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LgColors.Turquoise
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sync live exchange balance",
                                tint = LgColors.Turquoise
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(
                        label = "Live Portfolio",
                        value = Formatters.formatFiat(state.liveEquityUsd, fiat, fx),
                        valueColor = LgColors.CyanGlow,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Live Trading Budget",
                        value = Formatters.formatFiat(state.liveAvailableCashUsd, fiat, fx),
                        valueColor = LgColors.GoldBright,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Native Android Direct Exchange Execution Routes
        Text(
            text = "DIRECT EXCHANGE ROUTE (CAP $${selectedRisk.maxOrderUsd.toInt()}/ORDER)",
            style = MaterialTheme.typography.labelSmall,
            color = LgColors.Gold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MobileDirectRoutes, key = { it.id }) { route ->
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
                        text = if (ready) "READY" else "KEYS",
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

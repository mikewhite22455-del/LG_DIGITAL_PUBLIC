package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.core.Formatters
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.VaultUiState

@Composable
fun ArbitrageScreen(
    state: ArbitrageUiState,
    vaultState: VaultUiState,
    onUpdateParams: (String?, ArbitrageMode?, Double?, Double?, Boolean?) -> Unit,
    onScanNow: () -> Unit,
    onExecuteOpportunity: (ArbitrageOpportunity) -> Unit,
    onResetInventory: () -> Unit
) {
    var inspectedOpp by remember { mutableStateOf<ArbitrageOpportunity?>(null) }

    val arbCoins = remember { listOf("BTC", "ETH", "SOL", "XRP", "LINK", "AVAX", "ADA", "LTC", "DOGE") }
    val shownOpps = remember(state.opportunities, state.onlyQualified) {
        if (state.onlyQualified) state.opportunities.filter { it.qualified } else state.opportunities
    }

    inspectedOpp?.let { opp ->
        AlertDialog(
            onDismissRequest = { inspectedOpp = null },
            containerColor = LgColors.PanelSlate,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoinAvatar(opp.symbol, size = 28.dp)
                    Text("${opp.buyVenue} -> ${opp.sellVenue} (${opp.symbol})", style = MaterialTheme.typography.titleLarge, color = LgColors.Gold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (opp.isSynthetic) "Synthetic Demo Order Book" else "Live Public Exchange Order Book",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (opp.isSynthetic) LgColors.AmberWarn else LgColors.Turquoise
                    )
                    Text("Notional Size: $${opp.notionalUsd} (${opp.baseQuantity} ${opp.symbol})")
                    Text("Buy VWAP (${opp.buyVenue}): $${opp.buyVwap}")
                    Text("Sell VWAP (${opp.sellVenue}): $${opp.sellVwap}")
                    Text("Gross Spread: ${Formatters.formatBps(opp.grossSpreadBps)}")
                    Text("Taker Fees (16 bps x 2): -$${opp.takerFeesUsd}", color = LgColors.Crimson)
                    Text("Slippage Buffer (8 bps x 2): -$${opp.slippageBufferUsd}", color = LgColors.Crimson)
                    Text("Rebalance Reserve (5 bps): -$${opp.rebalanceReserveUsd}", color = LgColors.Crimson)
                    Text(
                        text = "Worst-Case Net: $${opp.worstCaseNetUsd} (${Formatters.formatBps(opp.worstCaseNetBps)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (opp.worstCaseNetUsd.toDouble() > 0) LgColors.Turquoise else LgColors.Crimson
                    )
                    if (opp.rejectionReasons.isNotEmpty()) {
                        Text("Blocked Reasons:", style = MaterialTheme.typography.labelMedium, color = LgColors.Crimson)
                        opp.rejectionReasons.forEach { r ->
                            Text("• $r", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = opp.qualified,
                    onClick = {
                        onExecuteOpportunity(opp)
                        inspectedOpp = null
                    }
                ) {
                    Text("Execute Paper Arb")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedOpp = null }) { Text("Close") }
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
        // Top KPI Summary Bar
        item {
            val qualifiedCount = state.opportunities.count { it.qualified }
            val realizedArbNet = vaultState.arbitrageTrades.sumOf { it.worstCaseNetUsd }
            CyberFrameCard(title = "CROSS-EXCHANGE ARBITRAGE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BigDecimal depth walk & worst-case net",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkSecondary
                    )
                    StatusPill(
                        text = if (state.mode == ArbitrageMode.DEMO) "DEMO" else "LIVE BOOKS",
                        color = if (state.mode == ArbitrageMode.DEMO) LgColors.AmberWarn else LgColors.Turquoise
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(
                        label = "Qualified",
                        value = "$qualifiedCount / ${state.opportunities.size}",
                        valueColor = if (qualifiedCount > 0) LgColors.Turquoise else LgColors.InkSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Venues",
                        value = "${state.activeVenuesCount}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Arb Net",
                        value = Formatters.formatUsd(realizedArbNet),
                        valueColor = if (realizedArbNet >= 0) LgColors.Turquoise else LgColors.Crimson,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Mode + Coin + Size Controls
        item {
            CyberFrameCard(title = "SCANNER PARAMETERS") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArbitrageMode.entries.forEach { m ->
                        FilterChip(
                            selected = m == state.mode,
                            onClick = { onUpdateParams(null, m, null, null, null) },
                            label = { Text(m.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(arbCoins) { coin ->
                        FilterChip(
                            selected = coin == state.symbol,
                            onClick = { onUpdateParams(coin, null, null, null, null) },
                            leadingIcon = { CoinAvatar(coin, size = 18.dp) },
                            label = { Text(coin) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100.0, 250.0, 500.0, 1000.0).forEach { sz ->
                        FilterChip(
                            selected = state.tradeNotionalUsd == sz,
                            onClick = { onUpdateParams(null, null, sz, null, null) },
                            label = { Text("$${sz.toInt()}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = state.onlyQualified,
                        onClick = { onUpdateParams(null, null, null, null, !state.onlyQualified) },
                        label = { Text("Qualified Only") }
                    )
                    GoldMetallicButton(
                        text = if (state.isScanning) "Scanning..." else "Scan ${state.symbol} Books",
                        onClick = onScanNow,
                        enabled = !state.isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_arbitrage_button")
                    )
                }
            }
        }

        state.bannerMessage?.let { msg ->
            item {
                GlassPanelCard(highlightBorder = LgColors.Turquoise.copy(alpha = 0.5f)) {
                    Text(msg, color = LgColors.Turquoise, modifier = Modifier.padding(12.dp))
                }
            }
        }

        state.error?.let { err ->
            item {
                GlassPanelCard(highlightBorder = LgColors.Crimson.copy(alpha = 0.5f)) {
                    Text(err, color = LgColors.Crimson, modifier = Modifier.padding(12.dp))
                }
            }
        }

        if (state.isScanning) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = LgColors.Turquoise)
                }
            }
        }

        // Opportunities List
        items(shownOpps.take(12), key = { it.id }) { opp ->
            GlassPanelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { inspectedOpp = opp },
                highlightBorder = if (opp.qualified) LgColors.Turquoise.copy(alpha = 0.5f) else LgColors.BorderHairline
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoinAvatar(opp.symbol, size = 28.dp)
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = LgColors.Gold)
                            Text("${opp.buyVenue} -> ${opp.sellVenue}", style = MaterialTheme.typography.titleMedium)
                        }
                        if (opp.qualified) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Qualified", tint = LgColors.Turquoise)
                        } else {
                            Icon(Icons.Filled.Warning, contentDescription = "Blocked", tint = LgColors.AmberWarn)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(
                            label = "Gross Spread",
                            value = Formatters.formatBps(opp.grossSpreadBps),
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Worst Net",
                            value = "$${opp.worstCaseNetUsd}",
                            valueColor = if (opp.worstCaseNetUsd.toDouble() > 0) LgColors.Turquoise else LgColors.Crimson,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Net bps",
                            value = Formatters.formatBps(opp.worstCaseNetBps),
                            valueColor = if (opp.worstCaseNetBps > 0) LgColors.Turquoise else LgColors.Crimson,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!opp.qualified && opp.rejectionReasons.isNotEmpty()) {
                        Text(
                            text = opp.rejectionReasons.first(),
                            style = MaterialTheme.typography.labelMedium,
                            color = LgColors.AmberWarn
                        )
                    } else if (opp.qualified) {
                        Button(
                            onClick = { onExecuteOpportunity(opp) },
                            colors = ButtonDefaults.buttonColors(containerColor = LgColors.Turquoise, contentColor = LgColors.ObsidianBg),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Execute Paper Arb (+$${opp.worstCaseNetUsd})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Venue Paper Wallets & Journal
        item {
            val usdVenues = vaultState.arbitrageInventory.filter { it.asset == "USD" }
            CyberFrameCard(title = "VENUE PAPER INVENTORY") {
                SectionHeader(
                    title = "${usdVenues.size} Active Exchanges",
                    subtitle = "${vaultState.arbitrageTrades.size} executed paper routes",
                    trailing = {
                        OutlinedButton(onClick = onResetInventory) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset venue balances")
                        }
                    }
                )
                usdVenues.forEach { item ->
                    val coinBal = vaultState.arbitrageInventory
                        .firstOrNull { it.venue == item.venue && it.asset == state.symbol }?.balance ?: 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.venue, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${Formatters.formatUsd(item.balance)} · ${Formatters.formatQty(coinBal)} ${state.symbol}",
                            style = MaterialTheme.typography.labelLarge,
                            color = LgColors.CyanGlow
                        )
                    }
                }
            }
        }

        if (vaultState.arbitrageTrades.isNotEmpty()) {
            item {
                SectionHeader("Arbitrage Paper Journal", "Recent cross-exchange route executions")
            }
            items(vaultState.arbitrageTrades.take(10), key = { "arb_${it.id}" }) { tr ->
                GlassPanelCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CoinAvatar(tr.symbol, size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${tr.symbol}: ${tr.buyVenue} -> ${tr.sellVenue}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "$${"%.0f".format(tr.notionalUsd)} · ${Formatters.formatBps(tr.grossSpreadBps)} · ${Formatters.formatTimeShort(tr.timestampMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.InkMuted
                                )
                            }
                        }
                        StatusPill(
                            text = "+${Formatters.formatUsd(tr.worstCaseNetUsd)}",
                            color = LgColors.Turquoise
                        )
                    }
                }
            }
        }
    }
}

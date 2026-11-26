package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.ui.components.CoinDropdownSelector
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
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
    var notionalInput by remember(state.tradeNotionalUsd) {
        mutableStateOf(state.tradeNotionalUsd.toInt().toString())
    }
    var minSpreadInput by remember(state.minSpreadBps) {
        mutableStateOf(state.minSpreadBps.toInt().toString())
    }

    val visibleOpportunities = remember(state.opportunities, state.onlyQualified) {
        if (state.onlyQualified) state.opportunities.filter { it.qualified } else state.opportunities
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberFrameCard(title = "CROSS-EXCHANGE ARBITRAGE DESK") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${state.symbol}/USD Venue Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "VWAP order-book depth · taker fees · slippage & rebalance reserves",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.InkMuted
                        )
                    }
                    StatusPill(
                        text = "${state.activeVenuesCount} VENUES",
                        color = if (state.activeVenuesCount >= 2) LgColors.Turquoise else LgColors.AmberWarn
                    )
                }

                CoinDropdownSelector(
                    selectedSymbol = state.symbol,
                    symbols = CoinAssets.catalog.map { it.symbol },
                    onSelectSymbol = { sym -> onUpdateParams(sym, null, null, null, null) },
                    label = "Arbitrage Asset",
                    testTag = "arb_coin_dropdown"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArbitrageMode.entries.forEach { mode ->
                        val selected = mode == state.mode
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateParams(null, mode, null, null, null) },
                            label = { Text(mode.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                                selectedLabelColor = LgColors.GoldBright,
                                containerColor = LgColors.PanelSlate,
                                labelColor = LgColors.InkSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("arb_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = notionalInput,
                        onValueChange = { raw ->
                            notionalInput = raw
                            raw.toDoubleOrNull()?.takeIf { it >= 25.0 }?.let {
                                onUpdateParams(null, null, it, null, null)
                            }
                        },
                        label = { Text("Order Size ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LgColors.Turquoise,
                            unfocusedBorderColor = LgColors.BorderHairline,
                            focusedTextColor = LgColors.InkPrimary,
                            unfocusedTextColor = LgColors.InkPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .circuitTextFieldBackdrop()
                            .testTag("arb_notional_input")
                    )

                    OutlinedTextField(
                        value = minSpreadInput,
                        onValueChange = { raw ->
                            minSpreadInput = raw
                            raw.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let {
                                onUpdateParams(null, null, null, it, null)
                            }
                        },
                        label = { Text("Min Net (bps)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LgColors.Turquoise,
                            unfocusedBorderColor = LgColors.BorderHairline,
                            focusedTextColor = LgColors.InkPrimary,
                            unfocusedTextColor = LgColors.InkPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .circuitTextFieldBackdrop()
                            .testTag("arb_min_spread_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show qualified spreads only",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                    Switch(
                        checked = state.onlyQualified,
                        onCheckedChange = { checked -> onUpdateParams(null, null, null, null, checked) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LgColors.ObsidianBg,
                            checkedTrackColor = LgColors.Turquoise
                        ),
                        modifier = Modifier.testTag("arb_qualified_only_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldMetallicButton(
                        text = if (state.isScanning) "SCANNING..." else "SCAN ORDER BOOKS",
                        onClick = onScanNow,
                        enabled = !state.isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arb_scan_button")
                    )
                    OutlinedButton(
                        onClick = onResetInventory,
                        modifier = Modifier.testTag("arb_reset_inventory_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Reset inventory")
                    }
                }

                if (!state.bannerMessage.isNullOrBlank()) {
                    Text(
                        text = state.bannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.Turquoise
                    )
                }
                if (!state.error.isNullOrBlank()) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.AmberWarn
                    )
                }
            }
        }

        if (state.isScanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LgColors.Turquoise)
                }
            }
        } else if (visibleOpportunities.isEmpty()) {
            item {
                GlassPanelCard {
                    Text(
                        text = "No arbitrage routes matched your filters. Tap SCAN ORDER BOOKS or relax the minimum net spread threshold.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                }
            }
        } else {
            items(visibleOpportunities, key = { it.id }) { opp ->
                ArbitrageOpportunityCard(
                    opp = opp,
                    onExecute = { onExecuteOpportunity(opp) }
                )
            }
        }

        if (vaultState.arbitrageInventory.isNotEmpty()) {
            item {
                GlassPanelCard {
                    Text(
                        text = "PRE-FUNDED VENUE PAPER INVENTORY",
                        style = MaterialTheme.typography.titleSmall,
                        color = LgColors.GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                    val grouped = vaultState.arbitrageInventory.groupBy { it.venue }
                    grouped.forEach { (venue, itemsList) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ExchangeLogo(exchangeName = venue, size = 22.dp)
                                Text(
                                    text = venue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            val summary = itemsList.joinToString(" · ") { item ->
                                if (item.asset == "USD") Formatters.formatUsd(item.balance)
                                else "${String.format("%.3f", item.balance)} ${item.asset}"
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkSecondary
                            )
                        }
                    }
                }
            }
        }

        if (vaultState.arbitrageTrades.isNotEmpty()) {
            item {
                GlassPanelCard {
                    Text(
                        text = "RECENT PAPER ARBITRAGE EXECUTIONS",
                        style = MaterialTheme.typography.titleSmall,
                        color = LgColors.Turquoise,
                        fontWeight = FontWeight.Bold
                    )
                    vaultState.arbitrageTrades.take(6).forEach { trade ->
                        HorizontalDivider(color = LgColors.BorderHairline.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${trade.symbol}: ${trade.buyVenue} → ${trade.sellVenue}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Notional ${Formatters.formatUsd(trade.notionalUsd)} · Gross ${String.format("%.1f", trade.grossSpreadBps)} bps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.InkMuted
                                )
                            }
                            Text(
                                text = "${if (trade.worstCaseNetUsd >= 0) "+" else ""}${Formatters.formatUsd(trade.worstCaseNetUsd)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (trade.worstCaseNetUsd >= 0) LgColors.Turquoise else LgColors.Crimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArbitrageOpportunityCard(
    opp: ArbitrageOpportunity,
    onExecute: () -> Unit
) {
    val netDouble = opp.worstCaseNetUsd.toDouble()
    GlassPanelCard(
        highlightBorder = if (opp.qualified) LgColors.Turquoise.copy(alpha = 0.7f) else LgColors.BorderHairline
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                ExchangeLogo(exchangeName = opp.buyVenue, size = 24.dp)
                Text(
                    text = "${opp.buyVenue} → ${opp.sellVenue}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            StatusPill(
                text = if (opp.qualified) "QUALIFIED" else "BLOCKED",
                color = if (opp.qualified) LgColors.Turquoise else LgColors.AmberWarn
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                label = "Buy VWAP",
                value = Formatters.formatUsd(opp.buyVwap.toDouble()),
                valueColor = LgColors.InkPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Sell VWAP",
                value = Formatters.formatUsd(opp.sellVwap.toDouble()),
                valueColor = LgColors.GoldBright,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "Worst Net",
                value = "${if (netDouble >= 0) "+" else ""}${Formatters.formatUsd(netDouble)}",
                valueColor = if (netDouble >= 0) LgColors.Turquoise else LgColors.Crimson,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Gross ${String.format("%.1f", opp.grossSpreadBps)} bps · Net ${String.format("%.1f", opp.worstCaseNetBps)} bps · Fees ${Formatters.formatUsd(opp.takerFeesUsd.toDouble())} · Slip ${Formatters.formatUsd(opp.slippageBufferUsd.toDouble())}",
            style = MaterialTheme.typography.labelSmall,
            color = LgColors.InkSecondary
        )

        if (opp.rejectionReasons.isNotEmpty()) {
            Text(
                text = opp.rejectionReasons.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.AmberWarn
            )
        }

        Button(
            onClick = onExecute,
            enabled = opp.qualified,
            colors = ButtonDefaults.buttonColors(
                containerColor = LgColors.Turquoise,
                contentColor = LgColors.ObsidianBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("execute_arb_${opp.id}")
        ) {
            Text(
                text = if (opp.qualified) "SIMULATE PAPER ARBITRAGE (${Formatters.formatUsd(opp.notionalUsd.toDouble())})" else "SPREAD BELOW SAFETY FLOOR",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

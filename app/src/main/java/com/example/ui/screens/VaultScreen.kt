package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.BiometricAuthManager
import com.example.core.BiometricCapability
import com.example.core.Formatters
import com.example.data.local.PayoutRuleEntity
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CoinDropdownSelector
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.SocialMediaLinksBar
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import com.example.ui.theme.LgThemeState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

@Composable
fun VaultScreen(
    vaultState: VaultUiState,
    onSavePriceTrigger: (String, Double, Boolean) -> Unit,
    onDeletePriceTrigger: (String) -> Unit,
    onSavePayoutRule: (PayoutRuleEntity) -> Unit,
    onResetPortfolio: () -> Unit,
    onRemoveWatchlist: (String) -> Unit,
    onAddWatchWallet: (String, String, String) -> Unit,
    onRemoveWatchWallet: (Long) -> Unit,
    onOpenHelpSheet: () -> Unit,
    biometricLockEnabled: Boolean = true,
    autoLockOnBackground: Boolean = true,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleAutoLockBackground: (Boolean) -> Unit = {},
    onLockDashboardNow: () -> Unit = {},
    memberSession: MemberSession? = null,
    onOpenWelcomeSignIn: (showSignIn: Boolean) -> Unit = {},
    membershipProfile: MembershipBillingProfile = MembershipBillingProfile(),
    onOpenMembershipBilling: () -> Unit = {},
    onOpenExchangeConfig: () -> Unit = {},
    tradeState: TradeUiState? = null,
    onRefreshLiveBalance: () -> Unit = {}
) {
    val context = LocalContext.current
    val biometricCapability = remember { BiometricAuthManager.checkCapability(context) }
    val rule = vaultState.payoutRule
    val circuitFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LgColors.Turquoise,
        unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )
    var autoSweepText by remember(rule.autoSweepUsd) { mutableStateOf(rule.autoSweepUsd.toInt().toString()) }
    var targetCoin by remember(rule.targetCoin) { mutableStateOf(rule.targetCoin) }
    var minPayoutText by remember(rule.minPayoutThresholdUsd) { mutableStateOf(rule.minPayoutThresholdUsd.toInt().toString()) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    var walletLabel by remember { mutableStateOf("") }
    var walletNetwork by remember { mutableStateOf("Ethereum") }
    var walletAddress by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        statusBanner?.let { msg ->
            item {
                GlassPanelCard(highlightBorder = LgColors.Turquoise.copy(alpha = 0.6f)) {
                    Text(msg, color = LgColors.Turquoise, modifier = Modifier.padding(12.dp))
                }
            }
        }

        // 1. Push Alerts & Triggers (matches Image 6 left pane)
        item {
            CyberFrameCard(title = "PUSH ALERTS & TRIGGERS") {
                Text("ALERT CHANNELS", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
                ChannelToggleRow(
                    icon = Icons.Filled.NotificationsActive,
                    label = "Push Alerts",
                    checked = rule.pushAlertsEnabled,
                    onCheckedChange = { onSavePayoutRule(rule.copy(pushAlertsEnabled = it)) }
                )
                ChannelToggleRow(
                    icon = Icons.Filled.Email,
                    label = "Email Alerts",
                    checked = rule.emailAlertsEnabled,
                    onCheckedChange = { onSavePayoutRule(rule.copy(emailAlertsEnabled = it)) }
                )
                ChannelToggleRow(
                    icon = Icons.Filled.Webhook,
                    label = "Webhook API",
                    checked = rule.webhookEnabled,
                    onCheckedChange = { onSavePayoutRule(rule.copy(webhookEnabled = it)) }
                )

                Text("ACTIVE PRICE TRIGGERS", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
                vaultState.priceTriggers.forEach { trig ->
                    var priceInput by remember(trig.symbol, trig.targetPriceUsd) {
                        mutableStateOf(if (trig.targetPriceUsd > 0) trig.targetPriceUsd.toInt().toString() else "")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LgColors.ElevatedSlate.copy(alpha = 0.65f))
                            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CoinAvatar(trig.symbol, size = 26.dp)
                                Text("${trig.symbol} Trigger", style = MaterialTheme.typography.titleMedium)
                            }
                            StatusPill(
                                text = if (trig.enabled) "Active" else "Paused",
                                color = if (trig.enabled) LgColors.Turquoise else LgColors.InkMuted
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Target USD") },
                                singleLine = true,
                                colors = circuitFieldColors,
                                modifier = Modifier
                                    .weight(1f)
                                    .circuitTextFieldBackdrop(drawBorder = false)
                            )
                            IconButton(
                                onClick = {
                                    val px = priceInput.toDoubleOrNull() ?: trig.targetPriceUsd
                                    onSavePriceTrigger(trig.symbol, px, true)
                                    statusBanner = "Saved ${trig.symbol} alert at ${Formatters.formatUsd(px)}"
                                },
                                modifier = Modifier.testTag("save_trigger_${trig.symbol}")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Save trigger", tint = LgColors.Turquoise)
                            }
                            IconButton(
                                onClick = { onDeletePriceTrigger(trig.symbol) },
                                modifier = Modifier.testTag("delete_trigger_${trig.symbol}")
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove trigger", tint = LgColors.Crimson)
                            }
                        }
                    }
                }
            }
        }

        // 2. Payouts & Settlement (matches Image 6 middle pane)
        item {
            CyberFrameCard(title = "PAYOUTS & SETTLEMENT") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = autoSweepText,
                        onValueChange = { autoSweepText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Auto-Sweep $") },
                        singleLine = true,
                        colors = circuitFieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .circuitTextFieldBackdrop(drawBorder = false)
                    )
                    OutlinedTextField(
                        value = minPayoutText,
                        onValueChange = { minPayoutText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Min Payout $") },
                        singleLine = true,
                        colors = circuitFieldColors,
                        modifier = Modifier
                            .weight(1f)
                            .circuitTextFieldBackdrop(drawBorder = false)
                    )
                }

                CoinDropdownSelector(
                    selectedSymbol = targetCoin,
                    symbols = listOf("BTC", "ETH", "SOL", "USDC", "USDT", "XRP", "LTC"),
                    onSelectSymbol = { targetCoin = it },
                    label = "Target Settlement Coin",
                    testTag = "vault_settlement_coin_dropdown"
                )

                GoldMetallicButton(
                    text = "SAVE PAYOUT RULE",
                    onClick = {
                        val updated = rule.copy(
                            autoSweepUsd = autoSweepText.toDoubleOrNull() ?: rule.autoSweepUsd,
                            targetCoin = targetCoin,
                            minPayoutThresholdUsd = minPayoutText.toDoubleOrNull() ?: rule.minPayoutThresholdUsd
                        )
                        onSavePayoutRule(updated)
                        statusBanner = "Payout sweep saved: ${Formatters.formatUsd(updated.autoSweepUsd)} -> ${updated.targetCoin}"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_payout_rule_button")
                )

                Text("EXCHANGE API CONNECTIONS", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LgColors.ElevatedSlate.copy(alpha = 0.65f))
                        .border(1.dp, LgColors.Turquoise.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExchangeLogo("Kraken", size = 18.dp)
                            Text("Kraken", style = MaterialTheme.typography.labelLarge, color = LgColors.InkPrimary)
                            ExchangeLogo("Coinbase", size = 18.dp)
                            Text("Coinbase", style = MaterialTheme.typography.labelLarge, color = LgColors.InkPrimary)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExchangeLogo("Bitstamp", size = 18.dp)
                            Text("Bitstamp", style = MaterialTheme.typography.labelLarge, color = LgColors.InkPrimary)
                            ExchangeLogo("KuCoin", size = 18.dp)
                            Text("KuCoin", style = MaterialTheme.typography.labelLarge, color = LgColors.InkPrimary)
                        }
                        Text("Read-Only Public Order Books & Feeds", style = MaterialTheme.typography.labelSmall, color = LgColors.InkSecondary)
                    }
                    StatusPill("Active", LgColors.Turquoise)
                }
            }
        }

        // 3. Portfolio Equity, Audit & Security (switches immediately between Live & Paper mode)
        item {
            val isLiveMode = tradeState?.isLiveMode == true
            CyberFrameCard(
                title = if (isLiveMode) {
                    "LIVE PORTFOLIO · ${tradeState?.liveExecutionRoute?.shortBadge ?: "EXCHANGE"}"
                } else {
                    "AUDIT, RECORDS & SECURITY"
                }
            ) {
                if (isLiveMode && tradeState != null) {
                    SectionHeader(
                        title = "Live ${tradeState.liveExecutionRoute.shortBadge} Equity",
                        subtitle = when {
                            tradeState.isSyncingLiveBalance -> "Syncing live balances..."
                            tradeState.liveAccountSnapshot?.isConfigured == false -> "Connect API keys to sync live balance"
                            tradeState.liveBalanceError != null -> tradeState.liveBalanceError
                            else -> tradeState.liveAccountSnapshot?.statusMessage?.ifBlank { "Live exchange balance active" } ?: "Live exchange balance active"
                        },
                        trailing = {
                            OutlinedButton(
                                onClick = onRefreshLiveBalance,
                                modifier = Modifier.testTag("vault_sync_live_balance_button")
                            ) {
                                Text("Sync", color = LgColors.Turquoise)
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile("Live Equity", Formatters.formatUsd(tradeState.liveEquityUsd), LgColors.Turquoise, Modifier.weight(1f))
                        MetricTile("Live Budget", Formatters.formatUsd(tradeState.liveAvailableCashUsd), LgColors.GoldBright, Modifier.weight(1f))
                        MetricTile("Spot Assets", "${tradeState.liveAccountSnapshot?.holdings?.size ?: 0}", LgColors.CyanGlow, Modifier.weight(1f))
                    }
                } else {
                    SectionHeader(
                        title = "Paper Vault Equity",
                        subtitle = "Local Room SQLite ledger",
                        trailing = {
                            OutlinedButton(
                                onClick = onResetPortfolio,
                                modifier = Modifier.testTag("reset_portfolio_button")
                            ) {
                                Icon(Icons.Filled.RestartAlt, contentDescription = "Reset paper portfolio")
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile("Total Equity", Formatters.formatUsd(vaultState.totalPortfolioEquityUsd), LgColors.Turquoise, Modifier.weight(1f))
                        MetricTile("USD Cash", Formatters.formatUsd(vaultState.account.usdBalance), LgColors.Gold, Modifier.weight(1f))
                        MetricTile("Drawdown", "${"%.2f".format(vaultState.currentDrawdownPct)}%", LgColors.Crimson, Modifier.weight(1f))
                    }
                }

                Text("ACCOUNT SECURITY", style = MaterialTheme.typography.labelMedium, color = LgColors.InkMuted)
                GoldMetallicButton(
                    text = "EXCHANGE CONFIGURATION (AES-256 VAULT)",
                    onClick = onOpenExchangeConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_open_exchange_config_button")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = LgColors.Turquoise)
                        Column {
                            Text("Biometric Dashboard Lock", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = biometricCapability.statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (biometricCapability == BiometricCapability.READY) LgColors.Turquoise else LgColors.Gold
                            )
                        }
                    }
                    Switch(
                        checked = biometricLockEnabled,
                        onCheckedChange = onToggleBiometricLock,
                        colors = SwitchDefaults.colors(checkedThumbColor = LgColors.ObsidianBg, checkedTrackColor = LgColors.Turquoise),
                        modifier = Modifier.testTag("vault_biometric_lock_switch")
                    )
                }
                if (biometricLockEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Lock After Background (15s)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = autoLockOnBackground,
                            onCheckedChange = onToggleAutoLockBackground,
                            colors = SwitchDefaults.colors(checkedThumbColor = LgColors.ObsidianBg, checkedTrackColor = LgColors.Turquoise),
                            modifier = Modifier.testTag("vault_autolock_switch")
                        )
                    }
                    OutlinedButton(
                        onClick = onLockDashboardNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("vault_lock_now_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                        Text("Lock Trading Dashboard Now")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("API Key Rotation Reminders", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = rule.apiKeyRotationReminder,
                        onCheckedChange = { onSavePayoutRule(rule.copy(apiKeyRotationReminder = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = LgColors.ObsidianBg, checkedTrackColor = LgColors.Turquoise)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IP Whitelisting Guard", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = rule.ipWhitelistingEnabled,
                        onCheckedChange = { onSavePayoutRule(rule.copy(ipWhitelistingEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = LgColors.ObsidianBg, checkedTrackColor = LgColors.Turquoise)
                    )
                }
            }
        }

        if (vaultState.holdings.isNotEmpty()) {
            item { SectionHeader("Open Paper Holdings") }
            items(vaultState.holdings, key = { it.symbol }) { h ->
                GlassPanelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CoinAvatar(h.symbol, size = 34.dp)
                            Column {
                                Text(h.symbol, style = MaterialTheme.typography.titleMedium)
                                Text("Avg ${Formatters.formatUsd(h.avgEntryPrice)}", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                            }
                        }
                        Text("${Formatters.formatQty(h.quantity)} ${h.symbol}", style = MaterialTheme.typography.labelLarge, color = LgColors.CyanGlow)
                    }
                }
            }
        }

        if (vaultState.watchlist.isNotEmpty()) {
            item { SectionHeader("Research Watchlist") }
            items(vaultState.watchlist, key = { it.symbol }) { w ->
                GlassPanelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CoinAvatar(w.symbol, size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${w.symbol} · ${w.regimeLabel}", style = MaterialTheme.typography.titleMedium)
                                Text("24h Proj ${Formatters.formatUsd(w.projected24hUsd)} · ${w.notes}", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                            }
                        }
                        IconButton(onClick = { onRemoveWatchlist(w.symbol) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove watchlist item", tint = LgColors.InkMuted)
                        }
                    }
                }
            }
        }

        if (vaultState.tradeLog.isNotEmpty()) {
            item { SectionHeader("Recent Trade & Payout Audit Log") }
            items(vaultState.tradeLog.take(10), key = { "tl_${it.id}" }) { tl ->
                GlassPanelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CoinAvatar(tl.symbol, size = 28.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${tl.side} ${tl.symbol} · ${Formatters.formatUsd(tl.notionalUsd)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${tl.strategyId} · ${Formatters.formatTimeShort(tl.timestampMs)}", style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
                            }
                        }
                        if (tl.side == "SELL") {
                            StatusPill(
                                text = Formatters.formatUsd(tl.pnlUsd),
                                color = if (tl.pnlUsd >= 0) LgColors.Turquoise else LgColors.Crimson
                            )
                        } else {
                            StatusPill("FILLED", LgColors.CyanGlow)
                        }
                    }
                }
            }
        }

        // 4. Watch-Only Cold Wallet Tracker
        item {
            CyberFrameCard(title = "WATCH-ONLY COLD WALLETS") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = LgColors.Gold)
                    Text(
                        text = "Public addresses only — private keys are never stored on device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkSecondary
                    )
                }
                OutlinedTextField(
                    value = walletLabel,
                    onValueChange = { walletLabel = it },
                    label = { Text("Label (e.g. Ledger Main)") },
                    singleLine = true,
                    colors = circuitFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .circuitTextFieldBackdrop(drawBorder = false)
                )
                val networkSym = when (walletNetwork) {
                    "Bitcoin" -> "BTC"
                    "Ethereum" -> "ETH"
                    "Solana" -> "SOL"
                    else -> "ETH"
                }
                CoinDropdownSelector(
                    selectedSymbol = networkSym,
                    symbols = listOf("BTC", "ETH", "SOL"),
                    onSelectSymbol = { sym ->
                        walletNetwork = when (sym) {
                            "BTC" -> "Bitcoin"
                            "SOL" -> "Solana"
                            else -> "Ethereum"
                        }
                    },
                    label = "Cold Wallet Coin / Network",
                    displayLabelFor = { sym ->
                        when (sym) {
                            "BTC" -> "BTC · Bitcoin Network"
                            "ETH" -> "ETH · Ethereum Network"
                            "SOL" -> "SOL · Solana Network"
                            else -> sym
                        }
                    },
                    testTag = "vault_wallet_network_dropdown"
                )
                OutlinedTextField(
                    value = walletAddress,
                    onValueChange = { walletAddress = it },
                    label = { Text("Public Address") },
                    singleLine = true,
                    colors = circuitFieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .circuitTextFieldBackdrop(drawBorder = false)
                )
                Button(
                    onClick = {
                        onAddWatchWallet(walletLabel, walletNetwork, walletAddress)
                        walletLabel = ""
                        walletAddress = ""
                    },
                    enabled = walletLabel.isNotBlank() && walletAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text("Add Watch Address")
                }
            }
        }

        items(vaultState.watchWallets, key = { it.id }) { w ->
            val netSym = when (w.network.lowercase()) {
                "bitcoin" -> "BTC"
                "ethereum" -> "ETH"
                "solana" -> "SOL"
                else -> w.network.take(3).uppercase()
            }
            GlassPanelCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CoinAvatar(netSym, size = 28.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${w.label} (${w.network})", style = MaterialTheme.typography.titleMedium)
                            Text(w.address, style = MaterialTheme.typography.labelSmall, color = LgColors.CyanGlow, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { onRemoveWatchWallet(w.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove watch address", tint = LgColors.InkMuted)
                    }
                }
            }
        }

        // 5. Support & Walkthrough Launch Card (matches Image 2)
        item {
            CyberFrameCard(title = "SUPPORT & WALKTHROUGH") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Need help navigating the desk?", style = MaterialTheme.typography.titleMedium)
                        Text("Open the interactive 3-card Support & Walkthrough hub.", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                    }
                }
                GoldMetallicButton(
                    text = "OPEN HELP & WALKTHROUGH",
                    onClick = onOpenHelpSheet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_help_hub_button")
                )
            }
        }

        // 6. Member Session, Membership & Billing, Welcome Screen & Social Media Links
        item {
            CyberFrameCard(title = "MEMBERSHIP, BILLING & SOCIAL") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memberSession?.let { "${it.displayName} (${it.provider})" } ?: "LG Digital Member",
                            style = MaterialTheme.typography.titleMedium,
                            color = LgColors.GoldBright
                        )
                        Text(
                            text = if (membershipProfile.isEnrolled) {
                                "Plan: ${membershipProfile.tier.title} (${membershipProfile.billingCycle.label}) · ID ${membershipProfile.memberId}"
                            } else {
                                memberSession?.handleOrEmail?.ifBlank { "Sign up for Pro or Elite membership" }
                                    ?: "Sign in with Google, Facebook, or Telegram"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.InkSecondary
                        )
                    }
                    StatusPill(
                        text = membershipProfile.tier.title.uppercase(),
                        color = if (membershipProfile.tier == MembershipTier.STARTER) LgColors.Turquoise else LgColors.GoldBright
                    )
                }
                GoldMetallicButton(
                    text = if (membershipProfile.isEnrolled) "MANAGE MEMBERSHIP & BILLING" else "MEMBERSHIP SIGN-UP & BILLING",
                    onClick = onOpenMembershipBilling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_open_membership_billing_button")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenWelcomeSignIn(false) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("vault_open_welcome_button")
                    ) {
                        Text("Welcome Screen", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onOpenWelcomeSignIn(true) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("vault_open_signin_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                        Text("Sign In / Switch", maxLines = 1)
                    }
                }
                Text("CONNECT ON SOCIAL MEDIA", style = MaterialTheme.typography.labelMedium, color = LgColors.Gold)
                SocialMediaLinksBar(compact = false)
            }
        }
    }
}

@Composable
private fun ChannelToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LgColors.ElevatedSlate.copy(alpha = 0.6f))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = LgColors.CyanGlow, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkPrimary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = LgColors.ObsidianBg, checkedTrackColor = LgColors.Turquoise)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpWalkthroughSheet(
    onDismiss: () -> Unit,
    onAskAiHelp: (String) -> Unit,
    onOpenFullWalkthrough: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LgColors.PanelSlate
    ) {
        HelpWalkthroughContent(
            onAskAiHelp = { prompt ->
                onDismiss()
                onAskAiHelp(prompt)
            },
            onOpenFullWalkthrough = {
                onDismiss()
                onOpenFullWalkthrough()
            }
        )
    }
}

@Composable
fun HelpWalkthroughContent(
    onAskAiHelp: (String) -> Unit,
    onOpenFullWalkthrough: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var walkthroughStep by remember { mutableStateOf(0) }
    val steps = remember {
        listOf(
            "1. Home & Live Market Overview" to "Inspect the Live Crypto Fear & Greed Index, TradingView feeds, 24h range, and scan cross-exchange arbitrage spreads.",
            "2. Adaptive USD/CAD Trader" to "Select USD or CAD funding and your connected exchange (Kraken, KuCoin, Coinbase, Bitstamp). Adaptive Trader scans the entire exchange to optimize profit.",
            "3. AI Advisor & Universal Quant Labs" to "Use Quant Labs for Official Sports Leagues Edge, Multi-Coin Staking & Earn yields, Universal ASIC/GPU Miner telemetry for any coin, and AI Copilot."
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: CONTACT
        CyberFrameCard(title = "CONTACT") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = "Support contact email",
                    tint = LgColors.Gold,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("LG Digital Desk Support", style = MaterialTheme.typography.titleMedium, color = LgColors.InkPrimary)
                    Text("Public Read-Only Exchange Connectors · Universal Miner & Coin Desk", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                }
            }
            SocialMediaLinksBar(compact = true)
        }

        // Card 2: DIGITAL WALKTHROUGH (with robot portrait + website walkthrough link)
        CyberFrameCard(title = "DIGITAL WALKTHROUGH") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_walkthrough_bot),
                    contentDescription = "LG Digital interactive walkthrough guide",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, LgColors.Gold.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(steps[walkthroughStep].first, style = MaterialTheme.typography.titleMedium, color = LgColors.CyanGlow)
                    Text(steps[walkthroughStep].second, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.indices.forEach { idx ->
                    FilterChip(
                        selected = walkthroughStep == idx,
                        onClick = { walkthroughStep = idx },
                        label = { Text("Step ${idx + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenFullWalkthrough,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("walkthrough_launch_full_native_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = LgColors.GoldBright,
                    modifier = Modifier.size(20.dp).padding(end = 6.dp)
                )
                Text("Launch Full Digital Walkthrough (7 Steps)", color = LgColors.GoldBright, fontWeight = FontWeight.Bold)
            }
        }

        // Card 3: SUPPORT (matches Image 2 right with gold GET HELP CTA)
        CyberFrameCard(title = "SUPPORT") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = LgColors.CyanGlow)
                Text(
                    text = "Ask AI Copilot for a live system check and strategy walkthrough.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LgColors.InkSecondary
                )
            }
            GoldMetallicButton(
                text = "GET HELP",
                onClick = {
                    onAskAiHelp("Give me a quick guided walkthrough of my active LG Digital desk, current risk guard settings, and how to test a paper trade safely.")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("help_get_help_button")
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

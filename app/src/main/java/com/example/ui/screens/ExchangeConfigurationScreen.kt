package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.ParsedExchangeQrCredentials
import com.example.core.QrExchangeTarget
import com.example.data.local.EncryptedExchangeCredentialStore
import com.example.data.local.EncryptedExchangeCredentials
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.QrCredentialScannerOverlay
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.ExchangeConnectionStatus
import com.example.viewmodel.ExchangeHealthState
import com.example.viewmodel.ExchangePingLogEntry
import kotlinx.coroutines.delay

@Composable
fun ExchangeConfigurationScreen(
    credentials: EncryptedExchangeCredentials,
    testingExchangeId: String?,
    isVerifyingAll: Boolean,
    isPingingKrakenCoinbase: Boolean,
    autoVerifyEnabled: Boolean,
    connectionStatuses: Map<String, ExchangeConnectionStatus>,
    pingHistory: List<ExchangePingLogEntry>,
    statusMessage: String?,
    onSaveKraken: (apiKey: String, apiSecret: String) -> Unit,
    onSaveCoinbase: (apiKey: String, apiSecret: String) -> Unit,
    onSaveKuCoin: (apiKey: String, apiSecret: String, passphrase: String) -> Unit,
    onSaveBitstamp: (apiKey: String, apiSecret: String) -> Unit,
    onTestConnection: (exchangeId: String) -> Unit,
    onPingKrakenAndCoinbase: () -> Unit,
    onVerifyAllConnections: () -> Unit,
    onToggleAutoVerify: (Boolean) -> Unit,
    onClearExchange: (exchangeId: String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    // Immediately verify stored keys on open, and poll every 30 seconds while Live Monitor is enabled
    LaunchedEffect(autoVerifyEnabled) {
        onVerifyAllConnections()
        while (autoVerifyEnabled) {
            delay(30_000L)
            onVerifyAllConnections()
        }
    }

    // Live 1-second clock tick for "Checked Xs ago" indicators
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    // Draft form state — prefilled from decrypted in-memory credentials when available
    var krakenKey by remember(credentials.krakenApiKey) { mutableStateOf(credentials.krakenApiKey) }
    var krakenSecret by remember(credentials.krakenApiSecret) { mutableStateOf(credentials.krakenApiSecret) }

    var coinbaseKey by remember(credentials.coinbaseApiKey) { mutableStateOf(credentials.coinbaseApiKey) }
    var coinbaseSecret by remember(credentials.coinbaseApiSecret) { mutableStateOf(credentials.coinbaseApiSecret) }

    var kucoinKey by remember(credentials.kucoinApiKey) { mutableStateOf(credentials.kucoinApiKey) }
    var kucoinSecret by remember(credentials.kucoinApiSecret) { mutableStateOf(credentials.kucoinApiSecret) }
    var kucoinPassphrase by remember(credentials.kucoinPassphrase) { mutableStateOf(credentials.kucoinPassphrase) }

    var bitstampKey by remember(credentials.bitstampApiKey) { mutableStateOf(credentials.bitstampApiKey) }
    var bitstampSecret by remember(credentials.bitstampApiSecret) { mutableStateOf(credentials.bitstampApiSecret) }

    var showClearAllDialog by remember { mutableStateOf(false) }
    var activeQrScannerTarget by remember { mutableStateOf<QrExchangeTarget?>(null) }
    var localQrImportNotice by remember { mutableStateOf<String?>(null) }

    fun applyParsedQrCredentials(
        parsed: ParsedExchangeQrCredentials,
        saveImmediately: Boolean
    ) {
        when (parsed.exchangeId) {
            "kraken" -> {
                if (parsed.apiKey.isNotBlank()) krakenKey = parsed.apiKey
                if (parsed.apiSecret.isNotBlank()) krakenSecret = parsed.apiSecret
                if (saveImmediately) {
                    onSaveKraken(
                        parsed.apiKey.ifBlank { krakenKey },
                        parsed.apiSecret.ifBlank { krakenSecret }
                    )
                }
            }
            "coinbase" -> {
                if (parsed.apiKey.isNotBlank()) coinbaseKey = parsed.apiKey
                if (parsed.apiSecret.isNotBlank()) coinbaseSecret = parsed.apiSecret
                if (saveImmediately) {
                    onSaveCoinbase(
                        parsed.apiKey.ifBlank { coinbaseKey },
                        parsed.apiSecret.ifBlank { coinbaseSecret }
                    )
                }
            }
            "kucoin" -> {
                if (parsed.apiKey.isNotBlank()) kucoinKey = parsed.apiKey
                if (parsed.apiSecret.isNotBlank()) kucoinSecret = parsed.apiSecret
                if (parsed.passphrase.isNotBlank()) kucoinPassphrase = parsed.passphrase
                if (saveImmediately) {
                    onSaveKuCoin(
                        parsed.apiKey.ifBlank { kucoinKey },
                        parsed.apiSecret.ifBlank { kucoinSecret },
                        parsed.passphrase.ifBlank { kucoinPassphrase }
                    )
                }
            }
            "bitstamp" -> {
                if (parsed.apiKey.isNotBlank()) bitstampKey = parsed.apiKey
                if (parsed.apiSecret.isNotBlank()) bitstampSecret = parsed.apiSecret
                if (saveImmediately) {
                    onSaveBitstamp(
                        parsed.apiKey.ifBlank { bitstampKey },
                        parsed.apiSecret.ifBlank { bitstampSecret }
                    )
                }
            }
        }
        localQrImportNotice = if (saveImmediately) {
            "QR imported (${parsed.formatLabel}) — encrypted & verifying ${parsed.exchangeDisplayName}..."
        } else {
            "${parsed.exchangeDisplayName} fields populated via QR (${parsed.formatLabel}). Review and tap SAVE & VERIFY."
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = LgColors.PanelSlate,
            titleContentColor = LgColors.GoldBright,
            textContentColor = LgColors.InkPrimary,
            title = { Text("Wipe All Encrypted API Keys?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This removes all saved Kraken, Coinbase, KuCoin, and Bitstamp credentials from your local AES-256-GCM encrypted DataStore.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgColors.Crimson,
                        contentColor = LgColors.ObsidianBg
                    ),
                    modifier = Modifier.testTag("confirm_wipe_all_keys_button")
                ) {
                    Text("WIPE ALL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false },
                    modifier = Modifier.testTag("cancel_wipe_all_keys_button")
                ) {
                    Text("Cancel", color = LgColors.Gold)
                }
            }
        )
    }

    val krakenStatus = connectionStatuses["kraken"] ?: defaultStatus("kraken", credentials.hasKraken)
    val coinbaseStatus = connectionStatuses["coinbase"] ?: defaultStatus("coinbase", credentials.hasCoinbase)
    val kucoinStatus = connectionStatuses["kucoin"] ?: defaultStatus("kucoin", credentials.hasKuCoin)
    val bitstampStatus = connectionStatuses["bitstamp"] ?: defaultStatus("bitstamp", credentials.hasBitstamp)

    val liveOnlineCount = listOf(krakenStatus, coinbaseStatus, kucoinStatus, bitstampStatus)
        .count { it.state == ExchangeHealthState.CONNECTED }

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Brand Top Bar
            Surface(
                color = LgColors.PanelSlate.copy(alpha = 0.96f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.lg_digital_emblem),
                                contentDescription = "LG Digital encrypted vault emblem",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, LgColors.Gold, CircleShape)
                            )
                            Column {
                                Text(
                                    text = "LG DIGITAL",
                                    fontFamily = LgBrandFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.9.sp,
                                    color = LgColors.GoldBright,
                                    maxLines = 1
                                )
                                Text(
                                    text = "EXCHANGE CONFIGURATION · AES-256-GCM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { activeQrScannerTarget = QrExchangeTarget.AUTO },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("exchange_config_top_scan_qr_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan exchange API QR code",
                                    tint = LgColors.Turquoise
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("exchange_config_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close exchange configuration",
                                    tint = LgColors.GoldBright
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.5f), thickness = 1.dp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .testTag("exchange_config_scroll_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. Real-Time Exchange Connectivity Matrix
                    item {
                        CyberFrameCard(title = "REAL-TIME API CONNECTIVITY") {
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
                                    Icon(
                                        imageVector = Icons.Filled.WifiTethering,
                                        contentDescription = null,
                                        tint = if (liveOnlineCount > 0) LgColors.Turquoise else LgColors.Gold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "$liveOnlineCount / 4 EXCHANGES LIVE VERIFIED",
                                            style = TabLabelTextStyle,
                                            color = if (liveOnlineCount > 0) LgColors.Turquoise else LgColors.GoldBright
                                        )
                                        Text(
                                            text = if (autoVerifyEnabled) "Live auto-check active (every 30s)" else "Manual verification mode",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LgColors.InkSecondary
                                        )
                                    }
                                }
                                Button(
                                    onClick = onVerifyAllConnections,
                                    enabled = !isVerifyingAll && !isPingingKrakenCoinbase,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LgColors.Turquoise,
                                        contentColor = LgColors.ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("verify_all_exchanges_button")
                                ) {
                                    if (isVerifyingAll) {
                                        CircularProgressIndicator(
                                            color = LgColors.ObsidianBg,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(" VERIFY ALL", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Dedicated Kraken + Coinbase Ping Service Trigger
                            GoldMetallicButton(
                                text = if (isPingingKrakenCoinbase) {
                                    "PINGING KRAKEN & COINBASE..."
                                } else {
                                    "PING KRAKEN & COINBASE NOW"
                                },
                                onClick = onPingKrakenAndCoinbase,
                                enabled = !isPingingKrakenCoinbase && !isVerifyingAll,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ping_kraken_coinbase_service_button")
                            )

                            // 2x2 Real-Time Status Matrix Tiles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiveExchangeStatusTile(
                                    label = "Kraken Pro",
                                    status = krakenStatus,
                                    nowMs = nowMs,
                                    onClick = { onTestConnection("kraken") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("live_status_tile_kraken")
                                )
                                LiveExchangeStatusTile(
                                    label = "Coinbase",
                                    status = coinbaseStatus,
                                    nowMs = nowMs,
                                    onClick = { onTestConnection("coinbase") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("live_status_tile_coinbase")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiveExchangeStatusTile(
                                    label = "KuCoin",
                                    status = kucoinStatus,
                                    nowMs = nowMs,
                                    onClick = { onTestConnection("kucoin") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("live_status_tile_kucoin")
                                )
                                LiveExchangeStatusTile(
                                    label = "Bitstamp",
                                    status = bitstampStatus,
                                    nowMs = nowMs,
                                    onClick = { onTestConnection("bitstamp") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("live_status_tile_bitstamp")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Auto-Verify Stored Keys (30s heartbeat)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkSecondary
                                )
                                Switch(
                                    checked = autoVerifyEnabled,
                                    onCheckedChange = onToggleAutoVerify,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LgColors.ObsidianBg,
                                        checkedTrackColor = LgColors.Turquoise,
                                        uncheckedThumbColor = LgColors.Gold,
                                        uncheckedTrackColor = LgColors.ObsidianBg
                                    ),
                                    modifier = Modifier.testTag("auto_verify_exchanges_switch")
                                )
                            }

                            // Recent Exchange Ping Service Telemetry Log (Kraken, Coinbase, KuCoin, Bitstamp)
                            if (pingHistory.isNotEmpty()) {
                                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.3f), thickness = 1.dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("exchange_ping_service_log"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "PING SERVICE TELEMETRY (RECENT PROBES)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.GoldBright,
                                        fontWeight = FontWeight.Bold
                                    )
                                    pingHistory.take(4).forEach { entry ->
                                        val entryColor = when (entry.state) {
                                            ExchangeHealthState.CONNECTED -> LgColors.Turquoise
                                            ExchangeHealthState.CHECKING -> LgColors.CyanGlow
                                            ExchangeHealthState.ERROR -> LgColors.Crimson
                                            ExchangeHealthState.NOT_CONFIGURED -> LgColors.Gold
                                        }
                                        val httpBadge = entry.httpStatusCode?.let { "HTTP $it · " } ?: ""
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
                                                .border(1.dp, entryColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LiveBeaconDot(state = entry.state)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "${entry.exchangeName} · ${httpBadge}${entry.latencyMs}ms",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = entryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = formatRelativeChecked(entry.timestampMs, nowMs),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = LgColors.InkMuted,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                                Text(
                                                    text = "${entry.endpoint} — ${entry.summary}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = LgColors.InkSecondary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Vault Security Summary
                    item {
                        GlassPanelCard(highlightBorder = LgColors.Turquoise.copy(alpha = 0.65f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = LgColors.Turquoise,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "HARDWARE-BACKED ENCRYPTED DATASTORE",
                                        style = TabLabelTextStyle,
                                        color = LgColors.GoldBright
                                    )
                                }
                                Text(
                                    text = "Every API key, secret, and passphrase is encrypted locally with an AndroidKeyStore AES-256-GCM key before saving to Jetpack DataStore. Scan a QR code directly from Kraken Pro, Coinbase Advanced, KuCoin, or Bitstamp portals — or paste manually.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkSecondary
                                )
                                Button(
                                    onClick = { activeQrScannerTarget = QrExchangeTarget.AUTO },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LgColors.Turquoise,
                                        contentColor = LgColors.ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .heightIn(min = 48.dp)
                                        .testTag("scan_exchange_portal_qr_hero_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = stringResource(R.string.qr_scanner_scan_button) + " (AUTO-DETECT EXCHANGE)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Live feedback banner (QR import or ViewModel save/verify status)
                    val activeBannerMessage = statusMessage ?: localQrImportNotice
                    activeBannerMessage?.let { msg ->
                        item {
                            GlassPanelCard(highlightBorder = LgColors.Gold.copy(alpha = 0.75f)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = LgColors.Turquoise,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LgColors.GoldBright
                                    )
                                }
                            }
                        }
                    }

                    // 1. KRAKEN PRO
                    item {
                        ExchangeCredentialCard(
                            title = "KRAKEN PRO API",
                            venueName = "Kraken",
                            subtitle = "HMAC-SHA512 Spot Trading · Key + Base64 Private Key",
                            isConfigured = credentials.hasKraken,
                            maskedPreview = EncryptedExchangeCredentialStore.maskSecret(credentials.krakenApiKey),
                            liveStatus = krakenStatus,
                            nowMs = nowMs,
                            isTesting = testingExchangeId == "kraken" || krakenStatus.state == ExchangeHealthState.CHECKING,
                            testTagPrefix = "kraken",
                            onScanQr = { activeQrScannerTarget = QrExchangeTarget.KRAKEN },
                            onSave = { onSaveKraken(krakenKey, krakenSecret) },
                            onTest = { onTestConnection("kraken") },
                            onClear = {
                                krakenKey = ""
                                krakenSecret = ""
                                onClearExchange("kraken")
                            }
                        ) {
                            SecretCredentialField(
                                value = krakenKey,
                                onValueChange = { krakenKey = it },
                                label = "Kraken API Key",
                                testTag = "kraken_api_key_input"
                            )
                            SecretCredentialField(
                                value = krakenSecret,
                                onValueChange = { krakenSecret = it },
                                label = "Kraken API Secret (Base64)",
                                testTag = "kraken_api_secret_input"
                            )
                        }
                    }

                    // 2. COINBASE ADVANCED TRADE
                    item {
                        ExchangeCredentialCard(
                            title = "COINBASE ADVANCED API",
                            venueName = "Coinbase",
                            subtitle = "Supports Legacy HMAC-SHA256 Secret or CDP Bearer JWT Token",
                            isConfigured = credentials.hasCoinbase,
                            maskedPreview = EncryptedExchangeCredentialStore.maskSecret(credentials.coinbaseApiKey),
                            liveStatus = coinbaseStatus,
                            nowMs = nowMs,
                            isTesting = testingExchangeId == "coinbase" || coinbaseStatus.state == ExchangeHealthState.CHECKING,
                            testTagPrefix = "coinbase",
                            onScanQr = { activeQrScannerTarget = QrExchangeTarget.COINBASE },
                            onSave = { onSaveCoinbase(coinbaseKey, coinbaseSecret) },
                            onTest = { onTestConnection("coinbase") },
                            onClear = {
                                coinbaseKey = ""
                                coinbaseSecret = ""
                                onClearExchange("coinbase")
                            }
                        ) {
                            SecretCredentialField(
                                value = coinbaseKey,
                                onValueChange = { coinbaseKey = it },
                                label = "Coinbase API Key Name / ID",
                                testTag = "coinbase_api_key_input"
                            )
                            SecretCredentialField(
                                value = coinbaseSecret,
                                onValueChange = { coinbaseSecret = it },
                                label = "Coinbase API Secret or CDP JWT",
                                testTag = "coinbase_api_secret_input"
                            )
                        }
                    }

                    // 3. KUCOIN SPOT
                    item {
                        ExchangeCredentialCard(
                            title = "KUCOIN SPOT API",
                            venueName = "KuCoin",
                            subtitle = "HMAC-SHA256 v2 Spot Trading · Key + Secret + Passphrase",
                            isConfigured = credentials.hasKuCoin,
                            maskedPreview = EncryptedExchangeCredentialStore.maskSecret(credentials.kucoinApiKey),
                            liveStatus = kucoinStatus,
                            nowMs = nowMs,
                            isTesting = testingExchangeId == "kucoin" || kucoinStatus.state == ExchangeHealthState.CHECKING,
                            testTagPrefix = "kucoin",
                            onScanQr = { activeQrScannerTarget = QrExchangeTarget.KUCOIN },
                            onSave = { onSaveKuCoin(kucoinKey, kucoinSecret, kucoinPassphrase) },
                            onTest = { onTestConnection("kucoin") },
                            onClear = {
                                kucoinKey = ""
                                kucoinSecret = ""
                                kucoinPassphrase = ""
                                onClearExchange("kucoin")
                            }
                        ) {
                            SecretCredentialField(
                                value = kucoinKey,
                                onValueChange = { kucoinKey = it },
                                label = "KuCoin API Key",
                                testTag = "kucoin_api_key_input"
                            )
                            SecretCredentialField(
                                value = kucoinSecret,
                                onValueChange = { kucoinSecret = it },
                                label = "KuCoin API Secret",
                                testTag = "kucoin_api_secret_input"
                            )
                            SecretCredentialField(
                                value = kucoinPassphrase,
                                onValueChange = { kucoinPassphrase = it },
                                label = "KuCoin API Passphrase",
                                testTag = "kucoin_api_passphrase_input"
                            )
                        }
                    }

                    // 4. OPTIONAL: BITSTAMP
                    item {
                        ExchangeCredentialCard(
                            title = "BITSTAMP API (OPTIONAL)",
                            venueName = "Bitstamp",
                            subtitle = "Bitstamp v2 HMAC-SHA256 Spot Trading",
                            isConfigured = credentials.hasBitstamp,
                            maskedPreview = EncryptedExchangeCredentialStore.maskSecret(credentials.bitstampApiKey),
                            liveStatus = bitstampStatus,
                            nowMs = nowMs,
                            isTesting = testingExchangeId == "bitstamp" || bitstampStatus.state == ExchangeHealthState.CHECKING,
                            testTagPrefix = "bitstamp",
                            onScanQr = { activeQrScannerTarget = QrExchangeTarget.BITSTAMP },
                            onSave = { onSaveBitstamp(bitstampKey, bitstampSecret) },
                            onTest = { onTestConnection("bitstamp") },
                            onClear = {
                                bitstampKey = ""
                                bitstampSecret = ""
                                onClearExchange("bitstamp")
                            }
                        ) {
                            SecretCredentialField(
                                value = bitstampKey,
                                onValueChange = { bitstampKey = it },
                                label = "Bitstamp API Key",
                                testTag = "bitstamp_api_key_input"
                            )
                            SecretCredentialField(
                                value = bitstampSecret,
                                onValueChange = { bitstampSecret = it },
                                label = "Bitstamp API Secret",
                                testTag = "bitstamp_api_secret_input"
                            )
                        }
                    }

                    // Wipe All Credentials Action
                    item {
                        OutlinedButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("wipe_all_encrypted_keys_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteForever,
                                contentDescription = null,
                                tint = LgColors.Crimson,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "WIPE ALL LOCAL ENCRYPTED API KEYS",
                                color = LgColors.Crimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // CameraX + ML Kit QR Code Scanner Overlay
        activeQrScannerTarget?.let { target ->
            QrCredentialScannerOverlay(
                initialTarget = target,
                onFillFieldsOnly = { parsed ->
                    applyParsedQrCredentials(parsed, saveImmediately = false)
                },
                onSaveAndVerify = { parsed ->
                    applyParsedQrCredentials(parsed, saveImmediately = true)
                },
                onDismiss = { activeQrScannerTarget = null }
            )
        }
    }
}

private fun defaultStatus(exchangeId: String, hasLocalKeys: Boolean): ExchangeConnectionStatus =
    ExchangeConnectionStatus(
        exchangeId = exchangeId,
        state = ExchangeHealthState.NOT_CONFIGURED,
        detail = if (hasLocalKeys) "Keys stored — tap VERIFY ALL" else "Awaiting encrypted API keys"
    )

private fun formatRelativeChecked(lastCheckedMs: Long, nowMs: Long): String {
    if (lastCheckedMs <= 0L) return "Not checked yet"
    val diffSec = ((nowMs - lastCheckedMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 3L -> "Verified just now"
        diffSec < 60L -> "Verified ${diffSec}s ago"
        else -> "Verified ${diffSec / 60L}m ago"
    }
}

@Composable
private fun LiveBeaconDot(
    state: ExchangeHealthState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        ExchangeHealthState.CONNECTED -> LgColors.Turquoise
        ExchangeHealthState.CHECKING -> LgColors.CyanGlow
        ExchangeHealthState.ERROR -> LgColors.Crimson
        ExchangeHealthState.NOT_CONFIGURED -> LgColors.Gold
    }
    val shouldPulse = state == ExchangeHealthState.CONNECTED || state == ExchangeHealthState.CHECKING

    if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "exchange_beacon_pulse")
        val haloScale by transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (state == ExchangeHealthState.CHECKING) 650 else 1300),
                repeatMode = RepeatMode.Restart
            ),
            label = "beacon_scale"
        )
        val haloAlpha by transition.animateFloat(
            initialValue = 0.72f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (state == ExchangeHealthState.CHECKING) 650 else 1300),
                repeatMode = RepeatMode.Restart
            ),
            label = "beacon_alpha"
        )
        Box(
            modifier = modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(haloScale)
                    .alpha(haloAlpha)
                    .clip(CircleShape)
                    .background(color)
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    } else {
        Box(
            modifier = modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (state == ExchangeHealthState.NOT_CONFIGURED) 0.65f else 1f))
            )
        }
    }
}

@Composable
private fun LiveExchangeStatusTile(
    label: String,
    status: ExchangeConnectionStatus,
    nowMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when (status.state) {
        ExchangeHealthState.CONNECTED -> LgColors.Turquoise.copy(alpha = 0.8f)
        ExchangeHealthState.CHECKING -> LgColors.CyanGlow.copy(alpha = 0.75f)
        ExchangeHealthState.ERROR -> LgColors.Crimson.copy(alpha = 0.8f)
        ExchangeHealthState.NOT_CONFIGURED -> LgColors.BorderHairline
    }
    val statusText = when (status.state) {
        ExchangeHealthState.CONNECTED -> "ONLINE · ${status.latencyMs}ms"
        ExchangeHealthState.CHECKING -> "CHECKING..."
        ExchangeHealthState.ERROR -> "ERROR · ${status.latencyMs}ms"
        ExchangeHealthState.NOT_CONFIGURED -> "NO KEYS"
    }
    val statusColor = when (status.state) {
        ExchangeHealthState.CONNECTED -> LgColors.Turquoise
        ExchangeHealthState.CHECKING -> LgColors.CyanGlow
        ExchangeHealthState.ERROR -> LgColors.Crimson
        ExchangeHealthState.NOT_CONFIGURED -> LgColors.Gold
    }

    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.78f))
            .border(1.2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LiveBeaconDot(state = status.state)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = LgColors.InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (status.lastCheckedMs > 0L && status.state != ExchangeHealthState.NOT_CONFIGURED) {
                Text(
                    text = formatRelativeChecked(status.lastCheckedMs, nowMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ExchangeCredentialCard(
    title: String,
    venueName: String,
    subtitle: String,
    isConfigured: Boolean,
    maskedPreview: String,
    liveStatus: ExchangeConnectionStatus,
    nowMs: Long,
    isTesting: Boolean,
    testTagPrefix: String,
    onScanQr: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onClear: () -> Unit,
    fields: @Composable () -> Unit
) {
    val accentColor = when (liveStatus.state) {
        ExchangeHealthState.CONNECTED -> LgColors.Turquoise
        ExchangeHealthState.CHECKING -> LgColors.CyanGlow
        ExchangeHealthState.ERROR -> LgColors.Crimson
        ExchangeHealthState.NOT_CONFIGURED -> LgColors.Gold
    }
    val headerPillText = when (liveStatus.state) {
        ExchangeHealthState.CONNECTED -> "LIVE · ${liveStatus.latencyMs}ms"
        ExchangeHealthState.CHECKING -> "VERIFYING"
        ExchangeHealthState.ERROR -> "AUTH ERROR"
        ExchangeHealthState.NOT_CONFIGURED -> if (isConfigured) "ENCRYPTED" else "NOT SET"
    }

    CyberFrameCard(title = title) {
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
                ExchangeLogo(exchangeName = venueName, size = 30.dp)
                Column {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                    if (isConfigured) {
                        Text(
                            text = "Stored Key: $maskedPreview (AES-256-GCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise
                        )
                    }
                }
            }
            StatusPill(
                text = headerPillText,
                color = accentColor
            )
        }

        // Dedicated Real-Time Connectivity Status Indicator Bar for this exchange
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LgColors.ObsidianBg.copy(alpha = 0.8f))
                .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp)
                .testTag("${testTagPrefix}_realtime_status_indicator"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    color = LgColors.CyanGlow,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                LiveBeaconDot(state = liveStatus.state)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (liveStatus.state) {
                            ExchangeHealthState.CONNECTED -> "API CONNECTED · ${liveStatus.latencyMs}ms"
                            ExchangeHealthState.CHECKING -> "VERIFYING LIVE API..."
                            ExchangeHealthState.ERROR -> "CONNECTION ISSUE · ${liveStatus.latencyMs}ms"
                            ExchangeHealthState.NOT_CONFIGURED -> "AWAITING STORED KEYS"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (liveStatus.lastCheckedMs > 0L) {
                        Text(
                            text = formatRelativeChecked(liveStatus.lastCheckedMs, nowMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.InkMuted
                        )
                    }
                }
                Text(
                    text = liveStatus.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.InkSecondary
                )
                if (liveStatus.endpoint.isNotBlank() || liveStatus.publicApiReachable != null) {
                    val gatewayText = when (liveStatus.publicApiReachable) {
                        true -> "Gateway OK (${liveStatus.publicLatencyMs}ms)"
                        false -> "Gateway Offline"
                        null -> ""
                    }
                    val httpText = liveStatus.httpStatusCode?.let { "HTTP $it" } ?: ""
                    val metaLine = listOf(liveStatus.endpoint, httpText, gatewayText, liveStatus.credentialSource)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (metaLine.isNotBlank()) {
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.InkMuted,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        fields()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoldMetallicButton(
                text = "SAVE & VERIFY",
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_save_button")
            )
            OutlinedButton(
                onClick = onScanQr,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("${testTagPrefix}_scan_qr_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = "Scan $venueName API QR code",
                    tint = LgColors.Turquoise,
                    modifier = Modifier.size(18.dp)
                )
            }
            Button(
                onClick = onTest,
                enabled = !isTesting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LgColors.Turquoise,
                    contentColor = LgColors.ObsidianBg
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("${testTagPrefix}_test_button")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        color = LgColors.ObsidianBg,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("PING", fontWeight = FontWeight.Bold)
                }
            }
            OutlinedButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("${testTagPrefix}_clear_button")
            ) {
                Text("Clear", color = LgColors.Gold)
            }
        }
    }
}

@Composable
private fun SecretCredentialField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = if (label.contains("Key", ignoreCase = true)) Icons.Filled.Key else Icons.Filled.Lock,
                contentDescription = null,
                tint = LgColors.Turquoise
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { visible = !visible },
                modifier = Modifier.testTag("${testTag}_toggle_visibility")
            ) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide $label" else "Show $label",
                    tint = LgColors.Gold
                )
            }
        },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LgColors.Turquoise,
            unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .circuitTextFieldBackdrop(drawBorder = false)
            .testTag(testTag)
    )
}

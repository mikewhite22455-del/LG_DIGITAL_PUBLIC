package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle

enum class LegalDisclosureTab(val label: String, val icon: ImageVector) {
    PRIVACY("Privacy", Icons.Filled.PrivacyTip),
    TERMS_RISK("Terms & Risk", Icons.Filled.Gavel),
    PLAY_COMPLIANCE("Safety", Icons.Filled.VerifiedUser)
}

@Composable
fun LegalAndDisclosureDialog(
    initialTab: LegalDisclosureTab = LegalDisclosureTab.PRIVACY,
    onDismiss: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = LgColors.PanelSlate,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, LgColors.Gold.copy(alpha = 0.72f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 620.dp)
                .testTag("legal_disclosure_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LEGAL, PRIVACY & SAFETY",
                            fontFamily = LgBrandFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.4.sp,
                            color = LgColors.GoldBright
                        )
                        Text(
                            text = "LG Digital · Non-Custodial Android Command",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("legal_disclosure_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close legal disclosure",
                            tint = LgColors.GoldBright
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegalDisclosureTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        FilterChip(
                            selected = selected,
                            onClick = { activeTab = tab },
                            leadingIcon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (selected) LgColors.GoldBright else LgColors.Turquoise,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(tab.label, style = TabLabelTextStyle, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.24f),
                                selectedLabelColor = LgColors.GoldBright
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("legal_tab_${tab.name.lowercase()}")
                        )
                    }
                }

                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.35f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeTab) {
                        LegalDisclosureTab.PRIVACY -> {
                            DisclosureSection(
                                heading = "On-Device Encrypted Storage",
                                body = "Exchange API keys and live access tokens are encrypted locally on your Android device using AndroidKeyStore AES-256-GCM inside Jetpack DataStore. Cloud backup is disabled (allowBackup=false) so credentials never leave your hardware keystore.",
                                accent = LgColors.Turquoise
                            )
                            DisclosureSection(
                                heading = "Biometric Hardware Lock",
                                body = "LG Digital uses AndroidX BiometricPrompt to gate dashboard access and sensitive API key actions. Biometric templates remain strictly inside the Android OS secure enclave and are never readable by LG Digital.",
                                accent = LgColors.GoldBright
                            )
                            DisclosureSection(
                                heading = "Camera QR Scanner (100% On-Device)",
                                body = "Camera access is optional and used exclusively when you launch the QR credential scanner. QR decoding runs locally via on-device ML Kit; no camera frames, images, or video are recorded or uploaded.",
                                accent = LgColors.Turquoise
                            )
                            DisclosureSection(
                                heading = "Network & Exchange Connectivity",
                                body = "LG Digital connects over encrypted HTTPS to public market feeds (Kraken, Coinbase, KuCoin, Bitstamp, DexScreener, Fear & Greed) and, only when you configure your own keys, submits authenticated requests directly to your chosen exchange.",
                                accent = LgColors.GoldBright
                            )
                            DisclosureSection(
                                heading = "Data Deletion Control",
                                body = "You can permanently wipe stored API keys, tokens, member profile details, and paper trading records at any time from Secure Settings or Vault, or by uninstalling the app.",
                                accent = LgColors.Turquoise
                            )
                        }

                        LegalDisclosureTab.TERMS_RISK -> {
                            DisclosureSection(
                                heading = "Non-Custodial Trading Software",
                                body = "LG Digital is a self-hosted, non-custodial analytics and trade-routing tool. LG Digital does not hold, custody, transmit, or receive user fiat or digital assets. All exchange accounts remain solely under your control.",
                                accent = LgColors.GoldBright
                            )
                            DisclosureSection(
                                heading = "Market Volatility & Financial Disclaimer",
                                body = "Cryptocurrency trading, arbitrage, and automated strategies carry substantial risk of financial loss. Nothing in LG Digital—including AI Trading Advisor briefings, indicator backtests, or breakout scans—constitutes personalized financial, investment, tax, or legal advice.",
                                accent = LgColors.Crimson
                            )
                            DisclosureSection(
                                heading = "Paper Mode Default & User Authorization",
                                body = "The trade desk defaults to Paper Mode for risk-free simulation. Live execution requires explicit user opt-in, configured exchange API credentials (with withdrawal permissions disabled), and strict per-order risk caps.",
                                accent = LgColors.Turquoise
                            )
                            DisclosureSection(
                                heading = "Open Access Release",
                                body = "LG Digital Android Command provides full access to included market feeds, calculators, walkthroughs, and trade routing tools without hidden external checkout flows.",
                                accent = LgColors.GoldBright
                            )
                        }

                        LegalDisclosureTab.PLAY_COMPLIANCE -> {
                            DisclosureSection(
                                heading = "No Real-Money Gambling or Wagering",
                                body = "The Sports Leagues module provides mathematical probability, implied odds, de-vig, and Kelly Criterion stake calculators for analytical and educational use only. LG Digital does not accept bets, process wagers, or facilitate real-money gambling.",
                                accent = LgColors.Turquoise
                            )
                            DisclosureSection(
                                heading = "No On-Device Cryptocurrency Mining",
                                body = "The Universal Miner tab displays external PoW hardware profitability estimates, hashrate math, and power-cost calculators only. LG Digital performs zero background or foreground cryptocurrency mining on your Android device.",
                                accent = LgColors.GoldBright
                            )
                            DisclosureSection(
                                heading = "Least-Privilege Permissions",
                                body = "LG Digital requests only Internet, Network State, Biometric authentication, and optional Camera permission for QR API-key scanning. Zero storage, SMS, contacts, call-log, or background location permissions are requested.",
                                accent = LgColors.Turquoise
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                GoldMetallicButton(
                    text = "UNDERSTOOD · CLOSE",
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("legal_disclosure_confirm_button")
                )
            }
        }
    }
}

@Composable
private fun DisclosureSection(
    heading: String,
    body: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.78f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = heading.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = accent
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = LgColors.InkPrimary
        )
    }
}

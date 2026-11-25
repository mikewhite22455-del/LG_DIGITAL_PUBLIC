package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LegalAndDisclosureDialog
import com.example.ui.components.LegalDisclosureTab
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle

/**
 * Dedicated Android Command Secure Settings screen managing hardware BiometricPrompt lock,
 * auto-lock behavior, direct exchange API health polling, encrypted Exchange API Key access,
 * and in-app Privacy Policy & Non-Custodial Risk Disclosures.
 */
@Composable
fun SecureSettingsScreen(
    configuredExchangeCount: Int,
    statusMessage: String?,
    biometricLockEnabled: Boolean,
    autoLockOnBackground: Boolean,
    autoVerifyExchangesEnabled: Boolean,
    onToggleBiometricLock: (Boolean) -> Unit,
    onToggleAutoLockBackground: (Boolean) -> Unit,
    onToggleAutoVerifyExchanges: (Boolean) -> Unit,
    onOpenExchangeConfig: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var showLegalDisclosureDialog by remember { mutableStateOf(false) }

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("secure_settings_screen")
        ) {
            Surface(
                color = LgColors.PanelSlate.copy(alpha = 0.96f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("secure_settings_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.settings_close_content_desc),
                                    tint = LgColors.Turquoise
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_title),
                                    fontFamily = LgBrandFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.6.sp,
                                    color = LgColors.GoldBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.settings_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        StatusPill(
                            text = if (biometricLockEnabled) "LOCKED VAULT" else "ANDROID COMMAND",
                            color = if (biometricLockEnabled) LgColors.Turquoise else LgColors.GoldBright
                        )
                    }
                    HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.45f), thickness = 1.dp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!statusMessage.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LgColors.Turquoise.copy(alpha = 0.14f))
                                .border(1.dp, LgColors.Turquoise.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("secure_settings_status_banner"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = LgColors.Turquoise,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = LgColors.InkPrimary
                            )
                        }
                    }

                    // 1. Direct Exchange API Vault Card
                    GlassPanelCard(
                        highlightBorder = LgColors.Turquoise.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Key,
                                        contentDescription = null,
                                        tint = LgColors.GoldBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "DIRECT EXCHANGE API VAULT",
                                        style = TabLabelTextStyle,
                                        color = LgColors.GoldBright
                                    )
                                }
                                StatusPill(
                                    text = "$configuredExchangeCount / 4 LINKED",
                                    color = if (configuredExchangeCount > 0) LgColors.Turquoise else LgColors.GoldBright
                                )
                            }

                            Text(
                                text = "Store Kraken Pro, Coinbase Advanced, KuCoin, and Bitstamp API keys strictly on-device with AndroidKeyStore AES-256-GCM encryption.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LgColors.InkSecondary
                            )

                            GoldMetallicButton(
                                text = "MANAGE EXCHANGE API KEYS",
                                onClick = onOpenExchangeConfig,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("secure_settings_open_exchange_config_button")
                            )
                        }
                    }

                    // 2. Device Security & Automation Preferences Card
                    GlassPanelCard(
                        highlightBorder = LgColors.Gold.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = LgColors.GoldBright,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "DEVICE SECURITY & AUTOMATION",
                                    style = TabLabelTextStyle,
                                    color = LgColors.GoldBright
                                )
                            }

                            SettingsToggleRow(
                                icon = Icons.Filled.Fingerprint,
                                title = "Biometric Dashboard Lock",
                                subtitle = "Require fingerprint, face, or device PIN to open LG Digital",
                                checked = biometricLockEnabled,
                                onCheckedChange = onToggleBiometricLock,
                                tag = "secure_settings_biometric_switch"
                            )

                            HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.25f))

                            SettingsToggleRow(
                                icon = Icons.Filled.Lock,
                                title = "Auto-Lock on Background (15s)",
                                subtitle = "Re-lock dashboard automatically after switching apps",
                                checked = autoLockOnBackground,
                                enabled = biometricLockEnabled,
                                onCheckedChange = onToggleAutoLockBackground,
                                tag = "secure_settings_autolock_switch"
                            )

                            HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.25f))

                            SettingsToggleRow(
                                icon = Icons.Filled.Refresh,
                                title = "Auto-Verify Exchange APIs (30s)",
                                subtitle = "Keep Kraken, Coinbase, KuCoin & Bitstamp status fresh",
                                checked = autoVerifyExchangesEnabled,
                                onCheckedChange = onToggleAutoVerifyExchanges,
                                tag = "secure_settings_autoverify_switch"
                            )
                        }
                    }

                    // 3. Legal, Privacy & Play Policy Disclosures
                    GlassPanelCard(
                        highlightBorder = LgColors.Turquoise.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "PRIVACY & COMPLIANCE",
                                style = TabLabelTextStyle,
                                color = LgColors.Turquoise
                            )

                            OutlinedButton(
                                onClick = { showLegalDisclosureDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("secure_settings_privacy_disclosure_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = LgColors.GoldBright,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "PRIVACY POLICY & RISK DISCLOSURE",
                                    color = LgColors.GoldBright,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showLegalDisclosureDialog) {
        LegalAndDisclosureDialog(
            initialTab = LegalDisclosureTab.PRIVACY,
            onDismiss = { showLegalDisclosureDialog = false }
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) LgColors.Turquoise else LgColors.InkSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) LgColors.InkPrimary else LgColors.InkSecondary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkSecondary
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LgColors.ObsidianBg,
                checkedTrackColor = LgColors.Turquoise,
                uncheckedThumbColor = LgColors.Gold,
                uncheckedTrackColor = LgColors.ElevatedSlate
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

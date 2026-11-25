package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch

private data class WalkthroughStepData(
    val number: Int,
    val badge: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val bullets: List<String>,
    val proTip: String,
    val actionLabel: String
)

@Composable
fun DigitalWalkthroughScreen(
    onOpenExchangeConfig: () -> Unit,
    onOpenSecureSettings: () -> Unit,
    onOpenTradeDesk: () -> Unit,
    onOpenAdvisor: () -> Unit,
    onOpenLabs: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val steps = remember {
        listOf(
            WalkthroughStepData(
                number = 1,
                badge = "COMMAND CENTER",
                title = "Live Markets & Full-Exchange Coin Picker",
                subtitle = "Every coin offered by Kraken, Coinbase, KuCoin, and Bitstamp in one instant searchable dropdown.",
                icon = Icons.Filled.Home,
                accent = LgColors.Turquoise,
                bullets = listOf(
                    "Monitor live spot prices, 24h volatility, Fear & Greed sentiment, and CoinDesk news in real time.",
                    "Tap any coin dropdown selector to search 350–800+ live tradable pairs across your attached exchanges.",
                    "Filter pairs by exchange (Kraken, Coinbase, KuCoin, Bitstamp) or jump directly to charts, arbitrage, or AI analysis."
                ),
                proTip = "Pull fresh exchange pair catalogs anytime from the coin picker sheet—new exchange listings appear automatically.",
                actionLabel = "RETURN TO HOME COMMAND CENTER"
            ),
            WalkthroughStepData(
                number = 2,
                badge = "BIOMETRIC VAULT",
                title = "Hardware Lock & AES-256-GCM Settings",
                subtitle = "Zero plaintext keys on disk—every secret is encrypted with AndroidKeyStore hardware keys.",
                icon = Icons.Filled.Fingerprint,
                accent = LgColors.GoldBright,
                bullets = listOf(
                    "Enable Biometric Dashboard Lock in Secure Settings to require Fingerprint, Face Unlock, or device PIN.",
                    "API tokens stay locked by default and automatically re-lock and clear memory whenever you leave the app.",
                    "Cloud backup is disabled so your encrypted credentials stay strictly on your own phone."
                ),
                proTip = "Keep Auto-Lock on Background (15s) turned ON before enabling live exchange execution.",
                actionLabel = "OPEN SECURE SETTINGS"
            ),
            WalkthroughStepData(
                number = 3,
                badge = "EXCHANGE API KEYS",
                title = "Connect Kraken, Coinbase, KuCoin & Bitstamp",
                subtitle = "Link your own non-custodial exchange API keys manually or with the on-device QR scanner.",
                icon = Icons.Filled.Key,
                accent = LgColors.Turquoise,
                bullets = listOf(
                    "Create trade-only API keys inside your exchange account—NEVER enable Withdrawal permissions.",
                    "Use the built-in CameraX + ML Kit QR scanner to import keys locally with zero image uploads.",
                    "Tap Verify All Connections to confirm live latency and authentication status before trading."
                ),
                proTip = "Paper Mode works immediately with zero API keys so you can test strategies risk-free first.",
                actionLabel = "OPEN EXCHANGE CONFIGURATION"
            ),
            WalkthroughStepData(
                number = 4,
                badge = "TRADE & AUTOPILOT",
                title = "Android Command Trade Desk (Paper & Live)",
                subtitle = "Execute spot orders, run indicator backtests, and configure Adaptive USD/CAD Autopilot with hard risk caps.",
                icon = Icons.Filled.CandlestickChart,
                accent = LgColors.GoldBright,
                bullets = listOf(
                    "Switch cleanly between Order Desk, Backtest Lab, and Quant Strategies from the top menu or workspace bar.",
                    "Test every strategy in Paper Mode first with simulated fills, equity tracking, and drawdown protection.",
                    "When ready for Live Mode, confirm your risk profile cap (Conservative / Medium / Aggressive) before arming."
                ),
                proTip = "Always start live testing with Conservative sizing ($25 max per order) to verify exchange fills.",
                actionLabel = "OPEN TRADE DESK"
            ),
            WalkthroughStepData(
                number = 5,
                badge = "AI QUANT COPILOT",
                title = "AI Trading Advisor & Live Market Briefings",
                subtitle = "Ask natural-language questions about any coin, risk setup, indicator signal, or portfolio rebalance.",
                icon = Icons.Filled.AutoAwesome,
                accent = LgColors.Turquoise,
                bullets = listOf(
                    "Tap the floating AI sparkle button from any tab for quick market briefings without leaving your screen.",
                    "Ask for technical levels, volatility checks, stop-loss placement ideas, or strategy comparisons.",
                    "All AI insights are informational analysis tools—you always stay in final control of every trade."
                ),
                proTip = "From any coin row on Home, tap the AI Copilot icon to pre-load live price context automatically.",
                actionLabel = "OPEN AI TRADING ADVISOR"
            ),
            WalkthroughStepData(
                number = 6,
                badge = "QUANT LABS SUITE",
                title = "Arbitrage, Breakouts, DEX, Miner & Math Labs",
                subtitle = "Six specialized institutional analytics tools built directly into your mobile terminal.",
                icon = Icons.Filled.Analytics,
                accent = LgColors.GoldBright,
                bullets = listOf(
                    "Arbitrage & Breakouts: Scan cross-exchange price spreads and momentum breakouts in real time.",
                    "DEX & Research: Search on-chain liquidity pairs and token metrics using the full coin picker.",
                    "Universal Miner & Sports Math: Estimate external PoW rig profitability (zero phone mining) and run De-Vig / Kelly math calculators (no real-money wagering)."
                ),
                proTip = "Use the top-right dropdown menu to jump straight into any Quant Labs tool in a single tap.",
                actionLabel = "OPEN QUANT LABS"
            ),
            WalkthroughStepData(
                number = 7,
                badge = "PRE-LIVE READINESS",
                title = "Non-Custodial Safety & Pre-Live Checklist",
                subtitle = "Complete these four safety checks before arming live orders on your phone.",
                icon = Icons.Filled.VerifiedUser,
                accent = LgColors.Turquoise,
                bullets = listOf(
                    "LG Digital never custodies your funds—orders execute directly via your encrypted exchange credentials.",
                    "Review Privacy Policy, Terms & Risk Disclosure anytime from Secure Settings or below."
                ),
                proTip = "You can re-open this Digital Walkthrough anytime from Home, Vault, or the top-right menu.",
                actionLabel = "FINISH WALKTHROUGH"
            )
        )
    }

    val checklistItems = remember {
        listOf(
            "Biometric Dashboard Lock enabled in Secure Settings",
            "Exchange API keys created with Withdrawals DISABLED",
            "Verified strategy behavior in Paper Mode first",
            "Reviewed Non-Custodial Risk & Privacy Disclosure"
        )
    }
    val checkedStates = remember { mutableStateListOf(false, false, false, false) }
    var showLegalDialog by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { steps.size })
    val coroutineScope = rememberCoroutineScope()
    val currentStep = steps[pagerState.currentPage]
    val progress = (pagerState.currentPage + 1f) / steps.size.toFloat()

    Surface(
        color = LgColors.ObsidianBg,
        modifier = Modifier
            .fillMaxSize()
            .testTag("digital_walkthrough_screen")
    ) {
        LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Header Bar
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
                                        .testTag("walkthrough_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close Digital Walkthrough",
                                        tint = LgColors.GoldBright
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.img_walkthrough_bot),
                                    contentDescription = "Digital Walkthrough guide avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.4.dp, LgColors.GoldBright, CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DIGITAL WALKTHROUGH",
                                        fontFamily = LgBrandFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.5.sp,
                                        color = LgColors.GoldBright,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Interactive Android Command Guide",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.Turquoise,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            StatusPill(
                                text = "STEP ${pagerState.currentPage + 1} / ${steps.size}",
                                color = LgColors.Turquoise
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            color = LgColors.GoldBright,
                            trackColor = LgColors.Turquoise.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                }

                // Step Quick-Jump Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, step ->
                        val active = pagerState.currentPage == index
                        val completed = index < pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        active -> LgColors.GoldBright
                                        completed -> LgColors.Turquoise.copy(alpha = 0.25f)
                                        else -> LgColors.ElevatedSlate.copy(alpha = 0.72f)
                                    }
                                )
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = if (active) LgColors.GoldBright else LgColors.Turquoise.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                                .testTag("walkthrough_step_pill_${step.number}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${step.number}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (active) LgColors.ObsidianBg else LgColors.InkPrimary
                            )
                        }
                    }
                }

                // Pager Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 14.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                    ) { pageIndex ->
                        val step = steps[pageIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassPanelCard(
                                highlightBorder = step.accent.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth()
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
                                        StatusPill(text = step.badge, color = step.accent)
                                        Icon(
                                            imageVector = step.icon,
                                            contentDescription = null,
                                            tint = step.accent,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    if (step.number == 1) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_play_store_feature_graphic),
                                            contentDescription = "LG Digital Android Command banner",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(132.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.2.dp, LgColors.Gold.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                        )
                                    }

                                    Text(
                                        text = step.title,
                                        fontFamily = LgBrandFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = LgColors.GoldBright
                                    )

                                    Text(
                                        text = step.subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LgColors.InkPrimary
                                    )

                                    HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.3f))

                                    step.bullets.forEach { bullet ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = step.accent,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(18.dp)
                                            )
                                            Text(
                                                text = bullet,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LgColors.InkSecondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Interactive Pre-Live Safety Checklist on final step
                                    if (step.number == steps.size) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(LgColors.ObsidianBg.copy(alpha = 0.78f))
                                                .border(1.dp, LgColors.Turquoise.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "PRE-LIVE LAUNCH CHECKLIST (${checkedStates.count { it }}/${checklistItems.size})",
                                                style = TabLabelTextStyle,
                                                color = LgColors.GoldBright
                                            )
                                            checklistItems.forEachIndexed { idx, label ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { checkedStates[idx] = !checkedStates[idx] }
                                                        .testTag("walkthrough_checklist_$idx"),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = checkedStates[idx],
                                                        onCheckedChange = { checkedStates[idx] = it },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = LgColors.Turquoise,
                                                            checkmarkColor = LgColors.ObsidianBg
                                                        )
                                                    )
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (checkedStates[idx]) LgColors.Turquoise else LgColors.InkPrimary
                                                    )
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { showLegalDialog = true },
                                                border = BorderStroke(1.dp, LgColors.Gold.copy(alpha = 0.7f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .heightIn(min = 48.dp)
                                                    .testTag("walkthrough_open_legal_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Shield,
                                                    contentDescription = null,
                                                    tint = LgColors.GoldBright,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Text("VIEW PRIVACY POLICY & RISK DISCLOSURE", color = LgColors.GoldBright, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Pro Tip Callout
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LgColors.Turquoise.copy(alpha = 0.12f))
                                            .border(1.dp, LgColors.Turquoise.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shield,
                                            contentDescription = null,
                                            tint = LgColors.GoldBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Tip: ${step.proTip}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = LgColors.InkPrimary
                                        )
                                    }

                                    // Step Direct Action CTA
                                    OutlinedButton(
                                        onClick = {
                                            when (step.number) {
                                                1 -> onClose()
                                                2 -> onOpenSecureSettings()
                                                3 -> onOpenExchangeConfig()
                                                4 -> onOpenTradeDesk()
                                                5 -> onOpenAdvisor()
                                                6 -> onOpenLabs()
                                                else -> onClose()
                                            }
                                        },
                                        border = BorderStroke(1.2.dp, step.accent.copy(alpha = 0.8f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("walkthrough_step_action_${step.number}")
                                    ) {
                                        Text(
                                            text = step.actionLabel,
                                            color = step.accent,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Previous / Next Bar
                Surface(
                    color = LgColors.PanelSlate.copy(alpha = 0.96f),
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.45f), thickness = 1.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    } else {
                                        onClose()
                                    }
                                },
                                border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("walkthrough_prev_button")
                            ) {
                                Text(
                                    text = if (pagerState.currentPage == 0) "CLOSE" else "PREVIOUS",
                                    color = LgColors.Turquoise,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (pagerState.currentPage < steps.lastIndex) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = LgColors.Turquoise,
                                        contentColor = LgColors.ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .heightIn(min = 50.dp)
                                        .testTag("walkthrough_next_button")
                                ) {
                                    Text("NEXT STEP", fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                GoldMetallicButton(
                                    text = "DONE · START TRADING",
                                    onClick = onClose,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .heightIn(min = 50.dp)
                                        .testTag("walkthrough_finish_button")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLegalDialog) {
        LegalAndDisclosureDialog(
            initialTab = LegalDisclosureTab.PRIVACY,
            onDismiss = { showLegalDialog = false }
        )
    }
}

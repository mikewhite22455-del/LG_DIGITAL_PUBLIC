package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.PayoutRuleEntity
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.HistoryWindow
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.MarketTicker
import com.example.domain.models.RiskProfile
import com.example.domain.models.StrategyType
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors
import com.example.viewmodel.AdvisorUiState
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.LabsUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState
import kotlinx.coroutines.launch

enum class PreviewDestination {
    HOME,
    TRADE,
    ADVISOR,
    LABS_ARBITRAGE,
    LABS_SPORTS,
    LABS_EARN_RISK,
    LABS_MINING,
    VAULT,
    HELP
}

private data class PagePreviewDescriptor(
    val id: PreviewDestination,
    val badge: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val heroDrawable: Int? = null
)

private val previewPages = listOf(
    PagePreviewDescriptor(
        id = PreviewDestination.HOME,
        badge = "PAGE 1 / 9",
        title = "Home",
        subtitle = "Portfolio summary, 24h Top Gainers & Losers, Market Pulse, Price Feeds & News Wire",
        icon = Icons.Filled.Home,
        heroDrawable = R.drawable.img_user_lg_coin
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.TRADE,
        badge = "PAGE 2 / 9",
        title = "Trade · Adaptive Trader",
        subtitle = "Live Paper Budget, Risk Level pills, Auto Stop DD, Candlestick Chart & 11 Strategies",
        icon = Icons.Filled.CandlestickChart,
        heroDrawable = R.drawable.img_hero_banner
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.ADVISOR,
        badge = "PAGE 3 / 9",
        title = "Advisor · AI Terminal",
        subtitle = "Streaming Trading Advisor chat, Strategy Presets, Risk Guards & Sentiment Arc Gauges",
        icon = Icons.Filled.AutoAwesome,
        heroDrawable = R.drawable.img_user_advisor_robot
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.LABS_ARBITRAGE,
        badge = "PAGE 4 / 9",
        title = "Labs · Arbitrage",
        subtitle = "Cross-exchange BigDecimal depth scanner, synthetic demo & live public books",
        icon = Icons.AutoMirrored.Filled.CompareArrows
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.LABS_SPORTS,
        badge = "PAGE 5 / 9",
        title = "Labs · Sports Edge",
        subtitle = "6 Sports League Logos, Two-Way Arb & De-Vig, Kelly Sizing & Poisson Matrix",
        icon = Icons.Filled.SportsScore
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.LABS_EARN_RISK,
        badge = "PAGE 6 / 9",
        title = "Labs · Earn & Risk",
        subtitle = "ALEO 12.4% & USDT 8.2% Staking Pools, Position Risk Sizer & 50/50 LP Impermanent Loss",
        icon = Icons.Filled.Savings
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.LABS_MINING,
        badge = "PAGE 7 / 9",
        title = "Labs · Miner",
        subtitle = "Miner Hardware Status (720 MH/s · 1300W) & live CoinSpectrum PoW feed",
        icon = Icons.Filled.Memory,
        heroDrawable = R.drawable.img_miner_rig
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.VAULT,
        badge = "PAGE 8 / 9",
        title = "Vault · Alerts & Payouts",
        subtitle = "Push/Email/Webhook triggers, Auto-Sweep Payout Rules, Audit Log & Watch Wallets",
        icon = Icons.Filled.AccountBalanceWallet
    ),
    PagePreviewDescriptor(
        id = PreviewDestination.HELP,
        badge = "PAGE 9 / 9",
        title = "Support & Walkthrough",
        subtitle = "3-card Contact, Robot Walkthrough & Get Help AI Diagnostics",
        icon = Icons.Filled.SupportAgent,
        heroDrawable = R.drawable.img_user_walkthrough_bot
    )
)

@Composable
fun AllPagesPreviewOverlay(
    marketsState: MarketsUiState,
    tradeState: TradeUiState,
    advisorState: AdvisorUiState,
    labsState: LabsUiState,
    arbitrageState: ArbitrageUiState,
    vaultState: VaultUiState,
    isWide: Boolean,
    onDismiss: () -> Unit,
    onJumpToDestination: (PreviewDestination) -> Unit,
    onRefreshMarkets: () -> Unit,
    onRefreshNews: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectCoinForTrade: (String) -> Unit,
    onSelectCoinForArbitrage: (String) -> Unit,
    onAskCopilotForCoin: (MarketTicker) -> Unit,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onSelectOverlay: (ChartOverlay) -> Unit,
    onSelectStrategy: (StrategyType) -> Unit,
    onSubmitPaperOrder: (Boolean, Double) -> Unit,
    onToggleAutopilot: (Boolean) -> Unit,
    onUpdateRiskConfig: (RiskProfile, String, Double, Boolean) -> Unit,
    onRunAutopilotTick: () -> Unit,
    onSendAdvisorPrompt: (String) -> Unit,
    onUpdateArbParams: (String?, ArbitrageMode?, Double?, Double?, Boolean?) -> Unit,
    onScanArbitrage: () -> Unit,
    onExecuteOpportunity: (ArbitrageOpportunity) -> Unit,
    onResetInventory: () -> Unit,
    onScanBreakouts: () -> Unit,
    onSearchDex: (String) -> Unit,
    onAnalyzeResearch: (String) -> Unit,
    onSaveWatchlist: (String, Double, String, String) -> Unit,
    onRefreshMining: () -> Unit,
    onSavePriceTrigger: (String, Double, Boolean) -> Unit,
    onDeletePriceTrigger: (String) -> Unit,
    onSavePayoutRule: (PayoutRuleEntity) -> Unit,
    onResetPortfolio: () -> Unit,
    onRemoveWatchlist: (String) -> Unit,
    onAddWatchWallet: (String, String, String) -> Unit,
    onRemoveWatchWallet: (Long) -> Unit,
    onSelectHistoryAsset: (String) -> Unit = {},
    onSelectHistoryWindow: (HistoryWindow) -> Unit = {},
    onRefreshHistory: () -> Unit = {},
    onUpdateIndicatorBacktestConfig: (IndicatorBacktestConfig) -> Unit = {}
) {
    var showGridOverview by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { previewPages.size })
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        Surface(
            color = LgColors.ObsidianBg,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LgColors.PanelSlate)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ALL PAGES VISUAL PREVIEW",
                            style = MaterialTheme.typography.titleMedium,
                            color = LgColors.Gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showGridOverview) "9 Live Screens · Tap any card to inspect" else "${previewPages[pagerState.currentPage].badge} · Swipe or tap pills",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.InkSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = showGridOverview,
                            onClick = { showGridOverview = !showGridOverview },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showGridOverview) Icons.Filled.ViewCarousel else Icons.Filled.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(if (showGridOverview) "Deck" else "Grid") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LgColors.CyanGlow.copy(alpha = 0.2f),
                                selectedLabelColor = LgColors.CyanGlow
                            ),
                            modifier = Modifier.testTag("preview_toggle_mode_button")
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("preview_close_button")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close all-pages preview", tint = LgColors.InkPrimary)
                        }
                    }
                }

                if (showGridOverview) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(previewPages, key = { _, item -> item.id.name }) { idx, page ->
                            CyberFrameCard(title = page.badge) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showGridOverview = false
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (page.heroDrawable != null) {
                                        Image(
                                            painter = painterResource(id = page.heroDrawable),
                                            contentDescription = page.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, LgColors.CyanGlow.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(LgColors.ElevatedSlate)
                                                .border(1.dp, LgColors.Gold.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(page.icon, contentDescription = page.title, tint = LgColors.Gold, modifier = Modifier.size(30.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(page.title, style = MaterialTheme.typography.titleMedium, color = LgColors.InkPrimary, fontWeight = FontWeight.Bold)
                                        Text(page.subtitle, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            showGridOverview = false
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) { Text("Live Preview") }
                                    Button(
                                        onClick = { onJumpToDestination(page.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LgColors.Turquoise, contentColor = LgColors.ObsidianBg),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) { Text("Open Tab", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                } else {
                    // Page Quick-Select Pills
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LgColors.PanelSlate.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(previewPages) { idx, page ->
                            val selected = pagerState.currentPage == idx
                            FilterChip(
                                selected = selected,
                                onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                                label = { Text("${idx + 1}. ${page.title.substringAfter("· ").trim()}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LgColors.Gold.copy(alpha = 0.22f),
                                    selectedLabelColor = LgColors.Gold
                                )
                            )
                        }
                    }

                    // Active Page Info Strip
                    val activePage = previewPages[pagerState.currentPage]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activePage.title, style = MaterialTheme.typography.titleMedium, color = LgColors.CyanGlow)
                            Text(activePage.subtitle, style = MaterialTheme.typography.labelSmall, color = LgColors.InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        StatusPill(activePage.badge, LgColors.Gold)
                    }

                    // Live Framed Interactive Pager
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, LgColors.Gold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .background(LgColors.ObsidianBg)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { pageIndex ->
                            val page = previewPages[pageIndex]
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (page.id) {
                                    PreviewDestination.HOME -> DashboardScreen(
                                        marketsState = marketsState,
                                        arbitrageState = arbitrageState,
                                        vaultState = vaultState,
                                        isWide = isWide,
                                        onRefreshAll = {
                                            onRefreshMarkets()
                                            onScanArbitrage()
                                        },
                                        onRefreshNews = onRefreshNews,
                                        onSearchChange = onSearchChange,
                                        onToggleArbMode = { mode -> onUpdateArbParams(null, mode, null, null, null) },
                                        onScanArbitrage = onScanArbitrage,
                                        onExecuteOpportunity = onExecuteOpportunity,
                                        onOpenArbitrageDesk = { scope.launch { pagerState.animateScrollToPage(3) } },
                                        onSelectCoinForTrade = { sym ->
                                            onSelectCoinForTrade(sym)
                                            onJumpToDestination(PreviewDestination.TRADE)
                                        },
                                        onSelectCoinForArbitrage = { sym ->
                                            onSelectCoinForArbitrage(sym)
                                            onJumpToDestination(PreviewDestination.LABS_ARBITRAGE)
                                        },
                                        onAskCopilotForCoin = { coin ->
                                            onAskCopilotForCoin(coin)
                                            onJumpToDestination(PreviewDestination.ADVISOR)
                                        },
                                        onNavigateToAdvisor = { scope.launch { pagerState.animateScrollToPage(2) } },
                                        onNavigateToMining = { scope.launch { pagerState.animateScrollToPage(6) } },
                                        onNavigateToTrade = { scope.launch { pagerState.animateScrollToPage(1) } },
                                        onOpenAllPagesPreview = { showGridOverview = true },
                                        onSelectHistoryAsset = onSelectHistoryAsset,
                                        onSelectHistoryWindow = onSelectHistoryWindow,
                                        onRefreshHistory = onRefreshHistory,
                                        onOpenBacktest = { sym ->
                                            onSelectCoinForTrade(sym)
                                            scope.launch { pagerState.animateScrollToPage(1) }
                                        }
                                    )
                                    PreviewDestination.TRADE -> TradeScreen(
                                        state = tradeState,
                                        vaultState = vaultState,
                                        isWide = isWide,
                                        onSelectSymbol = onSelectCoinForTrade,
                                        onSelectTimeframe = onSelectTimeframe,
                                        onSelectOverlay = onSelectOverlay,
                                        onSelectStrategy = onSelectStrategy,
                                        onSubmitPaperOrder = onSubmitPaperOrder,
                                        onToggleAutopilot = onToggleAutopilot,
                                        onUpdateRiskConfig = onUpdateRiskConfig,
                                        onRunAutopilotTick = onRunAutopilotTick,
                                        onUpdateIndicatorBacktestConfig = onUpdateIndicatorBacktestConfig
                                    )
                                    PreviewDestination.ADVISOR -> AdvisorScreen(
                                        advisorState = advisorState,
                                        marketsState = marketsState,
                                        tradeState = tradeState,
                                        vaultState = vaultState,
                                        isWide = isWide,
                                        onSendAdvisorPrompt = onSendAdvisorPrompt,
                                        onSelectStrategy = onSelectStrategy,
                                        onSelectCoin = onSelectCoinForTrade
                                    )
                                    PreviewDestination.LABS_ARBITRAGE -> LabsScreen(
                                        activeTool = LabsTool.ARBITRAGE,
                                        onSelectTool = { tool ->
                                            val targetIdx = when (tool) {
                                                LabsTool.ARBITRAGE -> 3
                                                LabsTool.SPORTS -> 4
                                                LabsTool.EARN_RISK -> 5
                                                LabsTool.MINING -> 6
                                                else -> 3
                                            }
                                            scope.launch { pagerState.animateScrollToPage(targetIdx) }
                                        },
                                        state = labsState,
                                        arbitrageState = arbitrageState,
                                        vaultState = vaultState,
                                        onUpdateArbParams = onUpdateArbParams,
                                        onScanArbitrage = onScanArbitrage,
                                        onExecuteOpportunity = onExecuteOpportunity,
                                        onResetInventory = onResetInventory,
                                        onScanBreakouts = onScanBreakouts,
                                        onSearchDex = onSearchDex,
                                        onAnalyzeResearch = onAnalyzeResearch,
                                        onSaveWatchlist = onSaveWatchlist,
                                        onRefreshMining = onRefreshMining,
                                        onOpenCoinInTrade = { sym ->
                                            onSelectCoinForTrade(sym)
                                            onJumpToDestination(PreviewDestination.TRADE)
                                        }
                                    )
                                    PreviewDestination.LABS_SPORTS -> LabsScreen(
                                        activeTool = LabsTool.SPORTS,
                                        onSelectTool = { tool ->
                                            val targetIdx = when (tool) {
                                                LabsTool.ARBITRAGE -> 3
                                                LabsTool.SPORTS -> 4
                                                LabsTool.EARN_RISK -> 5
                                                LabsTool.MINING -> 6
                                                else -> 4
                                            }
                                            scope.launch { pagerState.animateScrollToPage(targetIdx) }
                                        },
                                        state = labsState,
                                        arbitrageState = arbitrageState,
                                        vaultState = vaultState,
                                        onUpdateArbParams = onUpdateArbParams,
                                        onScanArbitrage = onScanArbitrage,
                                        onExecuteOpportunity = onExecuteOpportunity,
                                        onResetInventory = onResetInventory,
                                        onScanBreakouts = onScanBreakouts,
                                        onSearchDex = onSearchDex,
                                        onAnalyzeResearch = onAnalyzeResearch,
                                        onSaveWatchlist = onSaveWatchlist,
                                        onRefreshMining = onRefreshMining,
                                        onOpenCoinInTrade = { sym ->
                                            onSelectCoinForTrade(sym)
                                            onJumpToDestination(PreviewDestination.TRADE)
                                        }
                                    )
                                    PreviewDestination.LABS_EARN_RISK -> LabsScreen(
                                        activeTool = LabsTool.EARN_RISK,
                                        onSelectTool = { tool ->
                                            val targetIdx = when (tool) {
                                                LabsTool.ARBITRAGE -> 3
                                                LabsTool.SPORTS -> 4
                                                LabsTool.EARN_RISK -> 5
                                                LabsTool.MINING -> 6
                                                else -> 5
                                            }
                                            scope.launch { pagerState.animateScrollToPage(targetIdx) }
                                        },
                                        state = labsState,
                                        arbitrageState = arbitrageState,
                                        vaultState = vaultState,
                                        onUpdateArbParams = onUpdateArbParams,
                                        onScanArbitrage = onScanArbitrage,
                                        onExecuteOpportunity = onExecuteOpportunity,
                                        onResetInventory = onResetInventory,
                                        onScanBreakouts = onScanBreakouts,
                                        onSearchDex = onSearchDex,
                                        onAnalyzeResearch = onAnalyzeResearch,
                                        onSaveWatchlist = onSaveWatchlist,
                                        onRefreshMining = onRefreshMining,
                                        onOpenCoinInTrade = { sym ->
                                            onSelectCoinForTrade(sym)
                                            onJumpToDestination(PreviewDestination.TRADE)
                                        }
                                    )
                                    PreviewDestination.LABS_MINING -> LabsScreen(
                                        activeTool = LabsTool.MINING,
                                        onSelectTool = { tool ->
                                            val targetIdx = when (tool) {
                                                LabsTool.ARBITRAGE -> 3
                                                LabsTool.SPORTS -> 4
                                                LabsTool.EARN_RISK -> 5
                                                LabsTool.MINING -> 6
                                                else -> 6
                                            }
                                            scope.launch { pagerState.animateScrollToPage(targetIdx) }
                                        },
                                        state = labsState,
                                        arbitrageState = arbitrageState,
                                        vaultState = vaultState,
                                        onUpdateArbParams = onUpdateArbParams,
                                        onScanArbitrage = onScanArbitrage,
                                        onExecuteOpportunity = onExecuteOpportunity,
                                        onResetInventory = onResetInventory,
                                        onScanBreakouts = onScanBreakouts,
                                        onSearchDex = onSearchDex,
                                        onAnalyzeResearch = onAnalyzeResearch,
                                        onSaveWatchlist = onSaveWatchlist,
                                        onRefreshMining = onRefreshMining,
                                        onOpenCoinInTrade = { sym ->
                                            onSelectCoinForTrade(sym)
                                            onJumpToDestination(PreviewDestination.TRADE)
                                        }
                                    )
                                    PreviewDestination.VAULT -> VaultScreen(
                                        vaultState = vaultState,
                                        onSavePriceTrigger = onSavePriceTrigger,
                                        onDeletePriceTrigger = onDeletePriceTrigger,
                                        onSavePayoutRule = onSavePayoutRule,
                                        onResetPortfolio = onResetPortfolio,
                                        onRemoveWatchlist = onRemoveWatchlist,
                                        onAddWatchWallet = onAddWatchWallet,
                                        onRemoveWatchWallet = onRemoveWatchWallet,
                                        onOpenHelpSheet = { scope.launch { pagerState.animateScrollToPage(8) } }
                                    )
                                    PreviewDestination.HELP -> Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 8.dp)
                                    ) {
                                        HelpWalkthroughContent(
                                            onAskAiHelp = { prompt ->
                                                onSendAdvisorPrompt(prompt)
                                                onJumpToDestination(PreviewDestination.ADVISOR)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Pager Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                scope.launch { pagerState.animateScrollToPage(prev) }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("preview_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous page")
                        }

                        GoldMetallicButton(
                            text = "Open This Page",
                            onClick = { onJumpToDestination(activePage.id) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preview_open_live_button")
                        )

                        OutlinedButton(
                            onClick = {
                                val next = (pagerState.currentPage + 1).coerceAtMost(previewPages.lastIndex)
                                scope.launch { pagerState.animateScrollToPage(next) }
                            },
                            enabled = pagerState.currentPage < previewPages.lastIndex,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("preview_next_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next page")
                        }
                    }
                }
            }
            }
        }
    }
}

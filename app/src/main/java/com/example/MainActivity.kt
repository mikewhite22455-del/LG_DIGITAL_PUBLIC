package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.Formatters
import com.example.core.configureAppCheck
import com.example.domain.models.IndicatorBacktestConfig
import com.example.ui.components.StatusPill
import com.example.ui.screens.AdvisorScreen
import com.example.ui.screens.AllPagesPreviewOverlay
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HelpWalkthroughSheet
import com.example.ui.screens.LabsScreen
import com.example.ui.screens.LabsTool
import com.example.ui.screens.PreviewDestination
import com.example.ui.screens.TradeScreen
import com.example.ui.screens.TradeSubWorkspace
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.LgColors
import com.example.ui.theme.LgDigitalTheme
import com.example.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
data object MarketsRoute

@Serializable
data object TradeRoute

@Serializable
data object AdvisorRoute

@Serializable
data object LabsRoute

@Serializable
data object VaultRoute

private data class TopNavItem<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            LgDigitalTheme {
                LgDigitalAppShell(widthSizeClass = widthSizeClass)
            }
        }
    }
}

@Composable
fun LgDigitalAppShell(
    widthSizeClass: WindowWidthSizeClass,
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val marketsState by viewModel.marketsState.collectAsStateWithLifecycle()
    val tradeState by viewModel.tradeState.collectAsStateWithLifecycle()
    val arbitrageState by viewModel.arbitrageState.collectAsStateWithLifecycle()
    val labsState by viewModel.labsState.collectAsStateWithLifecycle()
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val advisorState by viewModel.advisorState.collectAsStateWithLifecycle()

    var selectedLabsTool by remember { mutableStateOf(LabsTool.ARBITRAGE) }
    var selectedTradeWorkspace by remember { mutableStateOf(TradeSubWorkspace.ORDER) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showAllPagesPreview by remember { mutableStateOf(false) }

    val navItems = listOf(
        TopNavItem(MarketsRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_markets"),
        TopNavItem(TradeRoute, "Trade", Icons.Filled.CandlestickChart, Icons.Outlined.CandlestickChart, "nav_trade"),
        TopNavItem(AdvisorRoute, "Advisor", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_advisor"),
        TopNavItem(LabsRoute, "Labs", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_labs"),
        TopNavItem(VaultRoute, "Vault", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_vault")
    )

    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val isWide = widthSizeClass == WindowWidthSizeClass.Expanded

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showHelpSheet) {
        HelpWalkthroughSheet(
            onDismiss = { showHelpSheet = false },
            onAskAiHelp = { prompt ->
                viewModel.sendAdvisorMessage(prompt)
                navigateToTab(AdvisorRoute)
            }
        )
    }

    if (showAllPagesPreview) {
        AllPagesPreviewOverlay(
            marketsState = marketsState,
            tradeState = tradeState,
            advisorState = advisorState,
            labsState = labsState,
            arbitrageState = arbitrageState,
            vaultState = vaultState,
            isWide = isWide,
            onDismiss = { showAllPagesPreview = false },
            onJumpToDestination = { dest ->
                showAllPagesPreview = false
                when (dest) {
                    PreviewDestination.HOME -> navigateToTab(MarketsRoute)
                    PreviewDestination.TRADE -> navigateToTab(TradeRoute)
                    PreviewDestination.ADVISOR -> navigateToTab(AdvisorRoute)
                    PreviewDestination.LABS_ARBITRAGE -> {
                        selectedLabsTool = LabsTool.ARBITRAGE
                        navigateToTab(LabsRoute)
                    }
                    PreviewDestination.LABS_SPORTS -> {
                        selectedLabsTool = LabsTool.SPORTS
                        navigateToTab(LabsRoute)
                    }
                    PreviewDestination.LABS_EARN_RISK -> {
                        selectedLabsTool = LabsTool.EARN_RISK
                        navigateToTab(LabsRoute)
                    }
                    PreviewDestination.LABS_MINING -> {
                        selectedLabsTool = LabsTool.MINING
                        navigateToTab(LabsRoute)
                    }
                    PreviewDestination.VAULT -> navigateToTab(VaultRoute)
                    PreviewDestination.HELP -> showHelpSheet = true
                }
            },
            onRefreshMarkets = viewModel::refreshMarkets,
            onRefreshNews = viewModel::refreshNews,
            onSearchChange = viewModel::updateMarketSearch,
            onSelectCoinForTrade = viewModel::selectTradeSymbol,
            onSelectCoinForArbitrage = { sym -> viewModel.updateArbitrageParams(symbol = sym) },
            onAskCopilotForCoin = { coin ->
                viewModel.selectTradeSymbol(coin.symbol)
                viewModel.sendAdvisorMessage(
                    "Give me a concise technical and risk briefing for ${coin.name} (${coin.symbol}) currently at ${Formatters.formatUsd(coin.priceUsd)} (${Formatters.formatPct(coin.change24hPct)} 24h)."
                )
            },
            onSelectTimeframe = viewModel::selectTimeframe,
            onSelectOverlay = viewModel::selectOverlay,
            onSelectStrategy = viewModel::selectStrategy,
            onSubmitPaperOrder = viewModel::submitPaperOrder,
            onToggleAutopilot = viewModel::toggleAutopilot,
            onUpdateRiskConfig = viewModel::updateRiskConfig,
            onRunAutopilotTick = viewModel::runAutopilotTick,
            onSendAdvisorPrompt = viewModel::sendAdvisorMessage,
            onUpdateArbParams = viewModel::updateArbitrageParams,
            onScanArbitrage = viewModel::scanArbitrage,
            onExecuteOpportunity = viewModel::executeArbitrageOpportunity,
            onResetInventory = viewModel::resetArbitrageInventory,
            onScanBreakouts = viewModel::scanBreakouts,
            onSearchDex = viewModel::searchDexPairs,
            onAnalyzeResearch = viewModel::analyzeResearchCoin,
            onSaveWatchlist = viewModel::saveToWatchlist,
            onRefreshMining = viewModel::loadMiningQuotes,
            onSavePriceTrigger = viewModel::savePriceTrigger,
            onDeletePriceTrigger = viewModel::deletePriceTrigger,
            onSavePayoutRule = viewModel::savePayoutRule,
            onResetPortfolio = viewModel::resetPaperPortfolio,
            onRemoveWatchlist = viewModel::removeFromWatchlist,
            onAddWatchWallet = viewModel::addWatchWallet,
            onRemoveWatchWallet = viewModel::removeWatchWallet,
            onSelectHistoryAsset = viewModel::selectHistoryAsset,
            onSelectHistoryWindow = viewModel::selectHistoryWindow,
            onRefreshHistory = viewModel::refreshAssetHistory,
            onUpdateIndicatorBacktestConfig = viewModel::updateIndicatorBacktestConfig
        )
    }

    Scaffold(
        containerColor = LgColors.ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = LgColors.PanelSlate,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_user_lg_coin),
                            contentDescription = "LG Digital emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, LgColors.Gold.copy(alpha = 0.7f), CircleShape)
                        )
                        Column {
                            Text(
                                text = "LG DIGITAL",
                                style = MaterialTheme.typography.titleMedium,
                                color = LgColors.Gold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Equity ${Formatters.formatUsd(vaultState.totalPortfolioEquityUsd)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill(
                            text = if (vaultState.account.autopilotRunning) "BOT ON" else "PAPER",
                            color = if (vaultState.account.autopilotRunning) LgColors.Turquoise else LgColors.CyanGlow,
                            modifier = Modifier.clickable { navigateToTab(TradeRoute) }
                        )
                        IconButton(
                            onClick = { showAllPagesPreview = true },
                            modifier = Modifier.testTag("top_preview_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GridView,
                                contentDescription = "Visual preview of all pages",
                                tint = LgColors.CyanGlow
                            )
                        }
                        IconButton(
                            onClick = { showHelpSheet = true },
                            modifier = Modifier.testTag("top_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = "Open Support & Walkthrough",
                                tint = LgColors.Gold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!useRail) {
                NavigationBar(
                    containerColor = LgColors.PanelSlate,
                    contentColor = LgColors.InkPrimary,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    navItems.forEach { item ->
                        val selected = currentDest?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LgColors.Turquoise,
                                selectedTextColor = LgColors.Turquoise,
                                indicatorColor = LgColors.Turquoise.copy(alpha = 0.16f),
                                unselectedIconColor = LgColors.InkMuted,
                                unselectedTextColor = LgColors.InkMuted
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(LgColors.ObsidianBg)
                .padding(innerPadding)
        ) {
            if (useRail) {
                NavigationRail(
                    containerColor = LgColors.PanelSlate,
                    contentColor = LgColors.InkPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    navItems.forEach { item ->
                        val selected = currentDest?.hasRoute(item.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = LgColors.Turquoise,
                                selectedTextColor = LgColors.Turquoise,
                                indicatorColor = LgColors.Turquoise.copy(alpha = 0.16f),
                                unselectedIconColor = LgColors.InkMuted,
                                unselectedTextColor = LgColors.InkMuted
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MarketsRoute,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1080.dp)
                ) {
                    composable<MarketsRoute> {
                        DashboardScreen(
                            marketsState = marketsState,
                            arbitrageState = arbitrageState,
                            vaultState = vaultState,
                            isWide = isWide,
                            onRefreshAll = {
                                viewModel.refreshMarkets()
                                viewModel.scanArbitrage()
                                viewModel.refreshAssetHistory()
                            },
                            onRefreshNews = viewModel::refreshNews,
                            onSearchChange = viewModel::updateMarketSearch,
                            onToggleArbMode = { mode ->
                                viewModel.updateArbitrageParams(mode = mode)
                            },
                            onScanArbitrage = viewModel::scanArbitrage,
                            onExecuteOpportunity = viewModel::executeArbitrageOpportunity,
                            onOpenArbitrageDesk = {
                                selectedLabsTool = LabsTool.ARBITRAGE
                                navigateToTab(LabsRoute)
                            },
                            onSelectCoinForTrade = { sym ->
                                viewModel.selectTradeSymbol(sym)
                                navigateToTab(TradeRoute)
                            },
                            onSelectCoinForArbitrage = { sym ->
                                viewModel.updateArbitrageParams(symbol = sym)
                                selectedLabsTool = LabsTool.ARBITRAGE
                                navigateToTab(LabsRoute)
                            },
                            onAskCopilotForCoin = { coin ->
                                viewModel.selectTradeSymbol(coin.symbol)
                                viewModel.sendAdvisorMessage(
                                    "Give me a concise technical and risk briefing for ${coin.name} (${coin.symbol}) currently at ${Formatters.formatUsd(coin.priceUsd)} (${Formatters.formatPct(coin.change24hPct)} 24h)."
                                )
                                navigateToTab(AdvisorRoute)
                            },
                            onNavigateToAdvisor = { navigateToTab(AdvisorRoute) },
                            onNavigateToMining = {
                                selectedLabsTool = LabsTool.MINING
                                navigateToTab(LabsRoute)
                            },
                            onNavigateToTrade = { navigateToTab(TradeRoute) },
                            onOpenAllPagesPreview = { showAllPagesPreview = true },
                            onSelectHistoryAsset = viewModel::selectHistoryAsset,
                            onSelectHistoryWindow = viewModel::selectHistoryWindow,
                            onRefreshHistory = viewModel::refreshAssetHistory,
                            onOpenBacktest = { sym ->
                                viewModel.selectTradeSymbol(sym)
                                selectedTradeWorkspace = TradeSubWorkspace.BACKTEST
                                navigateToTab(TradeRoute)
                            }
                        )
                    }
                    composable<TradeRoute> {
                        TradeScreen(
                            state = tradeState,
                            vaultState = vaultState,
                            isWide = isWide,
                            activeWorkspace = selectedTradeWorkspace,
                            onSelectWorkspace = { selectedTradeWorkspace = it },
                            onSelectSymbol = viewModel::selectTradeSymbol,
                            onSelectTimeframe = viewModel::selectTimeframe,
                            onSelectOverlay = viewModel::selectOverlay,
                            onSelectStrategy = viewModel::selectStrategy,
                            onSubmitPaperOrder = viewModel::submitPaperOrder,
                            onToggleAutopilot = viewModel::toggleAutopilot,
                            onUpdateRiskConfig = viewModel::updateRiskConfig,
                            onRunAutopilotTick = viewModel::runAutopilotTick,
                            onUpdateIndicatorBacktestConfig = viewModel::updateIndicatorBacktestConfig
                        )
                    }
                    composable<AdvisorRoute> {
                        AdvisorScreen(
                            advisorState = advisorState,
                            marketsState = marketsState,
                            tradeState = tradeState,
                            vaultState = vaultState,
                            isWide = isWide,
                            onSendAdvisorPrompt = viewModel::sendAdvisorMessage,
                            onSelectStrategy = viewModel::selectStrategy,
                            onSelectCoin = viewModel::selectTradeSymbol
                        )
                    }
                    composable<LabsRoute> {
                        LabsScreen(
                            activeTool = selectedLabsTool,
                            onSelectTool = { selectedLabsTool = it },
                            state = labsState,
                            arbitrageState = arbitrageState,
                            vaultState = vaultState,
                            onUpdateArbParams = viewModel::updateArbitrageParams,
                            onScanArbitrage = viewModel::scanArbitrage,
                            onExecuteOpportunity = viewModel::executeArbitrageOpportunity,
                            onResetInventory = viewModel::resetArbitrageInventory,
                            onScanBreakouts = viewModel::scanBreakouts,
                            onSearchDex = viewModel::searchDexPairs,
                            onAnalyzeResearch = viewModel::analyzeResearchCoin,
                            onSaveWatchlist = viewModel::saveToWatchlist,
                            onRefreshMining = viewModel::loadMiningQuotes,
                            onOpenCoinInTrade = { sym ->
                                viewModel.selectTradeSymbol(sym)
                                navigateToTab(TradeRoute)
                            }
                        )
                    }
                    composable<VaultRoute> {
                        VaultScreen(
                            vaultState = vaultState,
                            onSavePriceTrigger = viewModel::savePriceTrigger,
                            onDeletePriceTrigger = viewModel::deletePriceTrigger,
                            onSavePayoutRule = viewModel::savePayoutRule,
                            onResetPortfolio = viewModel::resetPaperPortfolio,
                            onRemoveWatchlist = viewModel::removeFromWatchlist,
                            onAddWatchWallet = viewModel::addWatchWallet,
                            onRemoveWatchWallet = viewModel::removeWatchWallet,
                            onOpenHelpSheet = { showHelpSheet = true }
                        )
                    }
                }
            }
        }
    }
}

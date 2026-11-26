package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.BiometricAuthManager
import com.example.core.BiometricCapability
import com.example.core.Formatters
import com.example.core.configureAppCheck
import com.example.domain.models.ExecutionMode
import com.example.domain.models.IndicatorBacktestConfig
import com.example.ui.components.FloatingAiChatBoxSheet
import com.example.ui.components.FloatingAiChatFab
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.PriceSummaryDashboardScreen
import com.example.ui.components.StatusPill
import com.example.ui.screens.AdvisorScreen
import com.example.ui.screens.AllPagesPreviewOverlay
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExchangeConfigurationScreen
import com.example.ui.screens.HelpWalkthroughSheet
import com.example.ui.screens.LabsScreen
import com.example.ui.screens.LabsTool
import com.example.ui.screens.PreviewDestination
import com.example.ui.screens.SecureSettingsScreen
import com.example.ui.screens.TradeScreen
import com.example.ui.screens.TradeSubWorkspace
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.MemberSession
import com.example.ui.screens.MemberSessionPrefs
import com.example.ui.screens.MembershipBillingPrefs
import com.example.ui.screens.MembershipBillingScreen
import com.example.ui.screens.MembershipTier
import com.example.ui.screens.WelcomeSignInScreen
import com.example.ui.theme.AppThemePrefs
import com.example.ui.theme.DropdownTabTextStyle
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.LgDigitalTheme
import com.example.ui.theme.LgThemeState
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.MainViewModel
import androidx.compose.ui.unit.sp
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

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        // Load user-selected color scheme before rendering UI
        LgThemeState.activeTheme = AppThemePrefs.load(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            LgDigitalTheme(theme = LgThemeState.activeTheme) {
                LgDigitalAppShell(
                    widthSizeClass = widthSizeClass,
                    activity = this@MainActivity
                )
            }
        }
    }
}

@Composable
fun LgDigitalAppShell(
    widthSizeClass: WindowWidthSizeClass,
    activity: FragmentActivity? = null,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var biometricLockEnabled by remember {
        mutableStateOf(BiometricAuthManager.isBiometricLockEnabled(context))
    }
    var autoLockOnBackground by remember {
        mutableStateOf(BiometricAuthManager.isAutoLockOnBackgroundEnabled(context))
    }
    var isDashboardUnlocked by remember { mutableStateOf(!biometricLockEnabled) }
    var isBiometricPromptActive by remember { mutableStateOf(false) }
    var lastBackgroundedAtMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner, biometricLockEnabled, autoLockOnBackground) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (!isBiometricPromptActive && biometricLockEnabled && autoLockOnBackground) {
                        lastBackgroundedAtMs = System.currentTimeMillis()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (biometricLockEnabled &&
                        autoLockOnBackground &&
                        lastBackgroundedAtMs > 0L &&
                        (System.currentTimeMillis() - lastBackgroundedAtMs) > 15_000L
                    ) {
                        isDashboardUnlocked = false
                    }
                    lastBackgroundedAtMs = 0L
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var memberSession by remember { mutableStateOf(MemberSessionPrefs.load(context)) }
    var membershipProfile by remember { mutableStateOf(MembershipBillingPrefs.load(context)) }
    var showWelcomeFlow by remember { mutableStateOf(true) }
    var welcomeStartOnSignIn by remember { mutableStateOf(false) }
    var showMembershipScreen by remember { mutableStateOf(false) }
    var showPriceSummaryDashboard by remember { mutableStateOf(false) }
    var showExchangeConfig by remember { mutableStateOf(false) }
    var showSecureSettings by remember { mutableStateOf(false) }

    val marketsState by viewModel.marketsState.collectAsStateWithLifecycle()
    val tradeState by viewModel.tradeState.collectAsStateWithLifecycle()
    val exchangeConfigState by viewModel.exchangeConfigState.collectAsStateWithLifecycle()

    fun toggleBiometricLock(enable: Boolean) {
        if (enable) {
            biometricLockEnabled = true
            BiometricAuthManager.setBiometricLockEnabled(context, true)
            viewModel.logSecurityEvent("BIOMETRIC_LOCK_ENABLED", "Biometric dashboard lock enabled.")
        } else {
            val host = activity
            if (host != null && BiometricAuthManager.checkCapability(host) == BiometricCapability.READY) {
                isBiometricPromptActive = true
                BiometricAuthManager.authenticate(
                    activity = host,
                    title = "Disable Biometric Lock",
                    subtitle = "Verify identity before turning off dashboard lock",
                    onSuccess = {
                        isBiometricPromptActive = false
                        biometricLockEnabled = false
                        BiometricAuthManager.setBiometricLockEnabled(context, false)
                        viewModel.logSecurityEvent("BIOMETRIC_LOCK_DISABLED", "Biometric dashboard lock disabled after verification.")
                    },
                    onError = { _, _ -> isBiometricPromptActive = false }
                )
            } else {
                biometricLockEnabled = false
                BiometricAuthManager.setBiometricLockEnabled(context, false)
                viewModel.logSecurityEvent("BIOMETRIC_LOCK_DISABLED", "Biometric dashboard lock disabled.")
            }
        }
    }

    if (biometricLockEnabled && !isDashboardUnlocked) {
        BiometricLockScreen(
            activity = activity,
            onUnlocked = { usedFallbackWithoutLock ->
                isDashboardUnlocked = true
                viewModel.logSecurityEvent(
                    eventType = if (usedFallbackWithoutLock) "SECURITY_BYPASS_NO_LOCK" else "BIOMETRIC_UNLOCK",
                    summary = if (usedFallbackWithoutLock) {
                        "Dashboard unlocked (no device biometric or screen lock configured)."
                    } else {
                        "Dashboard authenticated via AndroidX BiometricPrompt."
                    }
                )
            },
            onPromptActiveChange = { isBiometricPromptActive = it }
        )
        return
    }

    if (showMembershipScreen) {
        MembershipBillingScreen(
            currentProfile = membershipProfile,
            usdToCadRate = tradeState.usdToCadRate,
            onProfileSaved = { saved ->
                membershipProfile = saved
                val updatedSession = MemberSession(
                    displayName = saved.fullName.ifBlank { memberSession?.displayName ?: "LG Member" },
                    provider = saved.tier.title,
                    handleOrEmail = saved.email
                )
                memberSession = updatedSession
                MemberSessionPrefs.save(context, updatedSession)
            },
            onClose = { showMembershipScreen = false }
        )
        return
    }

    if (showPriceSummaryDashboard && !showExchangeConfig) {
        PriceSummaryDashboardScreen(
            rows = marketsState.priceSummaryRows,
            configuredExchangeIds = marketsState.configuredSummaryExchanges,
            selectedExchangeFilter = marketsState.priceSummaryExchangeFilter,
            isRefreshing = marketsState.isRefreshingPriceSummary,
            lastUpdatedMs = marketsState.priceSummaryUpdatedMs,
            errorMessage = marketsState.priceSummaryError,
            onSelectExchangeFilter = viewModel::selectPriceSummaryExchangeFilter,
            onRefresh = { viewModel.refreshPriceSummaryDashboard(silent = false) },
            onOpenExchangeConfig = { showExchangeConfig = true },
            onSelectPair = { sym ->
                viewModel.selectTradeSymbol(sym)
                showPriceSummaryDashboard = false
            },
            onClose = { showPriceSummaryDashboard = false }
        )
        return
    }

    if (showExchangeConfig) {
        ExchangeConfigurationScreen(
            credentials = exchangeConfigState.credentials,
            testingExchangeId = exchangeConfigState.testingExchangeId,
            isVerifyingAll = exchangeConfigState.isVerifyingAll,
            isPingingKrakenCoinbase = exchangeConfigState.isPingingKrakenCoinbase,
            autoVerifyEnabled = exchangeConfigState.autoVerifyEnabled,
            connectionStatuses = exchangeConfigState.connectionStatuses,
            pingHistory = exchangeConfigState.pingHistory,
            statusMessage = exchangeConfigState.statusMessage,
            onSaveKraken = viewModel::saveKrakenCredentials,
            onSaveCoinbase = viewModel::saveCoinbaseCredentials,
            onSaveKuCoin = viewModel::saveKuCoinCredentials,
            onSaveBitstamp = viewModel::saveBitstampCredentials,
            onTestConnection = viewModel::testExchangeConnection,
            onPingKrakenAndCoinbase = viewModel::pingKrakenAndCoinbase,
            onVerifyAllConnections = viewModel::verifyAllExchangeConnections,
            onToggleAutoVerify = viewModel::setAutoVerifyExchanges,
            onClearExchange = viewModel::clearExchangeCredentials,
            onClearAll = viewModel::clearAllExchangeCredentials,
            onClose = { showExchangeConfig = false }
        )
        return
    }

    if (showSecureSettings) {
        val creds = exchangeConfigState.credentials
        val linkedExchanges = listOf(
            creds.hasKraken,
            creds.hasCoinbase,
            creds.hasKuCoin,
            creds.hasBitstamp
        ).count { it }
        SecureSettingsScreen(
            configuredExchangeCount = linkedExchanges,
            statusMessage = exchangeConfigState.statusMessage,
            biometricLockEnabled = biometricLockEnabled,
            autoLockOnBackground = autoLockOnBackground,
            autoVerifyExchangesEnabled = exchangeConfigState.autoVerifyEnabled,
            onToggleBiometricLock = ::toggleBiometricLock,
            onToggleAutoLockBackground = { enabled ->
                autoLockOnBackground = enabled
                BiometricAuthManager.setAutoLockOnBackgroundEnabled(context, enabled)
            },
            onToggleAutoVerifyExchanges = viewModel::setAutoVerifyExchanges,
            onOpenExchangeConfig = { showExchangeConfig = true },
            onClose = { showSecureSettings = false }
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val arbitrageState by viewModel.arbitrageState.collectAsStateWithLifecycle()
    val labsState by viewModel.labsState.collectAsStateWithLifecycle()
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val advisorState by viewModel.advisorState.collectAsStateWithLifecycle()

    var selectedLabsTool by remember { mutableStateOf(LabsTool.ARBITRAGE) }
    var selectedTradeWorkspace by remember { mutableStateOf(TradeSubWorkspace.ORDER) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showDigitalWalkthrough by remember { mutableStateOf(false) }
    var showAllPagesPreview by remember { mutableStateOf(false) }
    var showFloatingAiChat by remember { mutableStateOf(false) }
    var topTabsExpanded by remember { mutableStateOf(false) }

    val navItems = listOf(
        TopNavItem(MarketsRoute, "HOME", Icons.Filled.Home, Icons.Outlined.Home, "nav_markets"),
        TopNavItem(TradeRoute, "TRADE", Icons.Filled.CandlestickChart, Icons.Outlined.CandlestickChart, "nav_trade"),
        TopNavItem(AdvisorRoute, "ADVISOR", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_advisor"),
        TopNavItem(LabsRoute, "LABS", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_labs"),
        TopNavItem(VaultRoute, "VAULT", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_vault")
    )

    val activeTabLabel = when {
        currentDest?.hasRoute(MarketsRoute::class) == true -> "HOME"
        currentDest?.hasRoute(TradeRoute::class) == true -> when (selectedTradeWorkspace) {
            TradeSubWorkspace.ORDER -> "TRADE"
            TradeSubWorkspace.BACKTEST -> "BACKTEST"
            TradeSubWorkspace.STRATEGIES -> "STRATEGIES"
        }
        currentDest?.hasRoute(AdvisorRoute::class) == true -> "ADVISOR"
        currentDest?.hasRoute(LabsRoute::class) == true -> selectedLabsTool.label.uppercase()
        currentDest?.hasRoute(VaultRoute::class) == true -> "VAULT"
        else -> "TABS"
    }

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

    if (showDigitalWalkthrough) {
        com.example.ui.screens.DigitalWalkthroughScreen(
            onOpenExchangeConfig = {
                showDigitalWalkthrough = false
                showExchangeConfig = true
            },
            onOpenSecureSettings = {
                showDigitalWalkthrough = false
                showSecureSettings = true
            },
            onOpenTradeDesk = {
                showDigitalWalkthrough = false
                selectedTradeWorkspace = TradeSubWorkspace.ORDER
                navigateToTab(TradeRoute)
            },
            onOpenAdvisor = {
                showDigitalWalkthrough = false
                navigateToTab(AdvisorRoute)
            },
            onOpenLabs = {
                showDigitalWalkthrough = false
                navigateToTab(LabsRoute)
            },
            onClose = { showDigitalWalkthrough = false }
        )
        return
    }

    if (showHelpSheet) {
        HelpWalkthroughSheet(
            onDismiss = { showHelpSheet = false },
            onAskAiHelp = { prompt ->
                viewModel.sendAdvisorMessage(prompt)
                navigateToTab(AdvisorRoute)
            },
            onOpenFullWalkthrough = {
                showHelpSheet = false
                showDigitalWalkthrough = true
            }
        )
    }

    if (showFloatingAiChat) {
        FloatingAiChatBoxSheet(
            advisorState = advisorState,
            activeSymbol = tradeState.symbol,
            onSendPrompt = viewModel::sendAdvisorMessage,
            onRetryLastPrompt = viewModel::retryLastAdvisorMessage,
            onOpenFullAdvisor = { navigateToTab(AdvisorRoute) },
            onDismiss = { showFloatingAiChat = false }
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

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButton = {
            FloatingAiChatFab(
                isGenerating = advisorState.isGenerating,
                onClick = { showFloatingAiChat = true }
            )
        },
        topBar = {
            Surface(
                color = LgColors.PanelSlate.copy(alpha = 0.95f),
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
                            contentDescription = "LG Digital emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, LgColors.Gold.copy(alpha = 0.85f), CircleShape)
                        )
                        Column {
                            Text(
                                text = "LG DIGITAL",
                                fontFamily = LgBrandFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 1.9.sp,
                                color = LgColors.GoldBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val topIsLive = tradeState.executionMode == ExecutionMode.LIVE
                            val topEquityLabel = when {
                                topIsLive && tradeState.isSyncingLiveBalance -> "Live Equity · Syncing..."
                                topIsLive && tradeState.liveAccountSnapshot?.isConfigured == false -> "Live · Connect Keys"
                                topIsLive -> "Live ${Formatters.formatUsd(tradeState.liveEquityUsd)}"
                                else -> "Paper ${Formatters.formatUsd(vaultState.totalPortfolioEquityUsd)}"
                            }
                            Text(
                                text = topEquityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (topIsLive) LgColors.CyanGlow else LgColors.Turquoise,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("top_bar_equity_text")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StatusPill(
                            text = if (membershipProfile.tier == MembershipTier.STARTER) "MEMBER" else membershipProfile.tier.title.substringBefore(" ").uppercase(),
                            color = LgColors.GoldBright,
                            modifier = Modifier
                                .clickable { showMembershipScreen = true }
                                .testTag("top_membership_pill")
                        )
                        val isLiveMode = tradeState.executionMode == ExecutionMode.LIVE
                        StatusPill(
                            text = when {
                                isLiveMode -> "LIVE"
                                vaultState.account.autopilotRunning -> "BOT ON"
                                else -> "PAPER"
                            },
                            color = if (isLiveMode || vaultState.account.autopilotRunning) LgColors.Turquoise else LgColors.Gold,
                            modifier = Modifier
                                .clickable {
                                    selectedTradeWorkspace = TradeSubWorkspace.ORDER
                                    navigateToTab(TradeRoute)
                                }
                                .testTag("top_execution_mode_pill")
                        )
                        IconButton(
                            onClick = { showSecureSettings = true },
                            modifier = Modifier.testTag("top_secure_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings_open_content_desc),
                                tint = LgColors.GoldBright
                            )
                        }

                        if (biometricLockEnabled) {
                            IconButton(
                                onClick = { isDashboardUnlocked = false },
                                modifier = Modifier.testTag("top_lock_dashboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Lock trading dashboard",
                                    tint = LgColors.Turquoise
                                )
                            }
                        }

                        // Top-Right Dropdown List with ALL Tabs in Bold Stylish Font
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LgColors.ElevatedSlate)
                                    .border(1.3.dp, LgColors.Gold.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .clickable { topTabsExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("top_tabs_dropdown_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "All tabs menu",
                                    tint = LgColors.Turquoise,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = activeTabLabel,
                                    style = TabLabelTextStyle,
                                    color = LgColors.Gold,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Expand all tabs dropdown",
                                    tint = LgColors.Gold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = topTabsExpanded,
                                onDismissRequest = { topTabsExpanded = false },
                                modifier = Modifier
                                    .background(LgColors.PanelSlate)
                                    .border(1.5.dp, LgColors.Gold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            ) {
                                @Composable
                                fun TabDropdownItem(
                                    label: String,
                                    icon: ImageVector,
                                    selected: Boolean,
                                    tag: String,
                                    onClick: () -> Unit
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) LgColors.Gold else LgColors.Turquoise,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = label,
                                                style = DropdownTabTextStyle,
                                                color = if (selected) LgColors.Gold else LgColors.InkPrimary
                                            )
                                        },
                                        onClick = {
                                            topTabsExpanded = false
                                            onClick()
                                        },
                                        modifier = Modifier
                                            .background(if (selected) LgColors.Turquoise.copy(alpha = 0.14f) else Color.Transparent)
                                            .testTag(tag)
                                    )
                                }

                                TabDropdownItem(
                                    label = "SECURITY & SETTINGS",
                                    icon = Icons.Filled.Settings,
                                    selected = false,
                                    tag = "dropdown_tab_secure_settings"
                                ) {
                                    showSecureSettings = true
                                }

                                TabDropdownItem(
                                    label = "EXCHANGE CONFIGURATION",
                                    icon = Icons.Filled.Key,
                                    selected = false,
                                    tag = "dropdown_tab_exchange_config"
                                ) {
                                    showExchangeConfig = true
                                }

                                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.35f))

                                TabDropdownItem(
                                    label = "HOME",
                                    icon = Icons.Filled.Home,
                                    selected = currentDest?.hasRoute(MarketsRoute::class) == true,
                                    tag = "dropdown_tab_home"
                                ) { navigateToTab(MarketsRoute) }

                                TabDropdownItem(
                                    label = if (tradeState.executionMode == ExecutionMode.LIVE) "TRADE · LIVE DESK" else "TRADE · ORDER DESK",
                                    icon = Icons.Filled.CandlestickChart,
                                    selected = currentDest?.hasRoute(TradeRoute::class) == true && selectedTradeWorkspace == TradeSubWorkspace.ORDER,
                                    tag = "dropdown_tab_trade"
                                ) {
                                    selectedTradeWorkspace = TradeSubWorkspace.ORDER
                                    navigateToTab(TradeRoute)
                                }

                                TabDropdownItem(
                                    label = "BACKTEST LAB",
                                    icon = Icons.Filled.QueryStats,
                                    selected = currentDest?.hasRoute(TradeRoute::class) == true && selectedTradeWorkspace == TradeSubWorkspace.BACKTEST,
                                    tag = "dropdown_tab_backtest"
                                ) {
                                    selectedTradeWorkspace = TradeSubWorkspace.BACKTEST
                                    navigateToTab(TradeRoute)
                                }

                                TabDropdownItem(
                                    label = "STRATEGIES",
                                    icon = Icons.Filled.Tune,
                                    selected = currentDest?.hasRoute(TradeRoute::class) == true && selectedTradeWorkspace == TradeSubWorkspace.STRATEGIES,
                                    tag = "dropdown_tab_strategies"
                                ) {
                                    selectedTradeWorkspace = TradeSubWorkspace.STRATEGIES
                                    navigateToTab(TradeRoute)
                                }

                                TabDropdownItem(
                                    label = "TRADING ADVISOR",
                                    icon = Icons.Filled.AutoAwesome,
                                    selected = currentDest?.hasRoute(AdvisorRoute::class) == true,
                                    tag = "dropdown_tab_advisor"
                                ) { navigateToTab(AdvisorRoute) }

                                TabDropdownItem(
                                    label = "ARBITRAGE",
                                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                                    selected = currentDest?.hasRoute(LabsRoute::class) == true && selectedLabsTool == LabsTool.ARBITRAGE,
                                    tag = "dropdown_tab_arbitrage"
                                ) {
                                    selectedLabsTool = LabsTool.ARBITRAGE
                                    navigateToTab(LabsRoute)
                                }

                                TabDropdownItem(
                                    label = "SPORTS LEAGUES",
                                    icon = Icons.Filled.SportsScore,
                                    selected = currentDest?.hasRoute(LabsRoute::class) == true && selectedLabsTool == LabsTool.SPORTS,
                                    tag = "dropdown_tab_sports"
                                ) {
                                    selectedLabsTool = LabsTool.SPORTS
                                    navigateToTab(LabsRoute)
                                }

                                TabDropdownItem(
                                    label = "EARN & RISK",
                                    icon = Icons.Filled.Savings,
                                    selected = currentDest?.hasRoute(LabsRoute::class) == true && selectedLabsTool == LabsTool.EARN_RISK,
                                    tag = "dropdown_tab_earn_risk"
                                ) {
                                    selectedLabsTool = LabsTool.EARN_RISK
                                    navigateToTab(LabsRoute)
                                }

                                TabDropdownItem(
                                    label = "MINER",
                                    icon = Icons.Filled.Memory,
                                    selected = currentDest?.hasRoute(LabsRoute::class) == true && selectedLabsTool == LabsTool.MINING,
                                    tag = "dropdown_tab_miner"
                                ) {
                                    selectedLabsTool = LabsTool.MINING
                                    navigateToTab(LabsRoute)
                                }

                                TabDropdownItem(
                                    label = "BREAKOUTS",
                                    icon = Icons.Filled.TrendingUp,
                                    selected = currentDest?.hasRoute(LabsRoute::class) == true && selectedLabsTool == LabsTool.BREAKOUT,
                                    tag = "dropdown_tab_breakouts"
                                ) {
                                    selectedLabsTool = LabsTool.BREAKOUT
                                    navigateToTab(LabsRoute)
                                }

                                TabDropdownItem(
                                    label = "DEX & RESEARCH",
                                    icon = Icons.Filled.Search,
                                    selected = currentDest?.hasRoute(LabsRoute::class) == true && selectedLabsTool == LabsTool.DEX_RESEARCH,
                                    tag = "dropdown_tab_dex"
                                ) {
                                    selectedLabsTool = LabsTool.DEX_RESEARCH
                                    navigateToTab(LabsRoute)
                                }

                                TabDropdownItem(
                                    label = "VAULT & SECURITY",
                                    icon = Icons.Filled.AccountBalanceWallet,
                                    selected = currentDest?.hasRoute(VaultRoute::class) == true,
                                    tag = "dropdown_tab_vault"
                                ) { navigateToTab(VaultRoute) }

                                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.35f))

                                TabDropdownItem(
                                    label = "ALL PAGES PREVIEW",
                                    icon = Icons.Filled.GridView,
                                    selected = false,
                                    tag = "dropdown_tab_preview_all"
                                ) { showAllPagesPreview = true }

                                TabDropdownItem(
                                    label = "MEMBER PROFILE",
                                    icon = Icons.Filled.WorkspacePremium,
                                    selected = false,
                                    tag = "dropdown_tab_membership_billing"
                                ) {
                                    showMembershipScreen = true
                                }

                                TabDropdownItem(
                                    label = "WELCOME & SIGN IN",
                                    icon = Icons.AutoMirrored.Filled.Login,
                                    selected = false,
                                    tag = "dropdown_tab_welcome_signin"
                                ) {
                                    welcomeStartOnSignIn = false
                                    showWelcomeFlow = true
                                }

                                TabDropdownItem(
                                    label = "DIGITAL WALKTHROUGH",
                                    icon = Icons.Filled.MenuBook,
                                    selected = false,
                                    tag = "dropdown_tab_digital_walkthrough"
                                ) { showDigitalWalkthrough = true }

                                TabDropdownItem(
                                    label = "SUPPORT & HELP",
                                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                    selected = false,
                                    tag = "dropdown_tab_help"
                                ) { showHelpSheet = true }
                            }
                        }
                    }
                }
                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.45f), thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            if (!useRail) {
                Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.45f), thickness = 1.dp)
                NavigationBar(
                    containerColor = LgColors.PanelSlate.copy(alpha = 0.96f),
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
                                    style = TabLabelTextStyle,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LgColors.Gold,
                                selectedTextColor = LgColors.Gold,
                                indicatorColor = LgColors.Turquoise.copy(alpha = 0.22f),
                                unselectedIconColor = LgColors.InkSecondary,
                                unselectedTextColor = LgColors.InkSecondary
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(innerPadding)
        ) {
            if (useRail) {
                NavigationRail(
                    containerColor = LgColors.PanelSlate.copy(alpha = 0.96f),
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
                            label = {
                                Text(
                                    text = item.label,
                                    style = TabLabelTextStyle,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = LgColors.Gold,
                                selectedTextColor = LgColors.Gold,
                                indicatorColor = LgColors.Turquoise.copy(alpha = 0.22f),
                                unselectedIconColor = LgColors.InkSecondary,
                                unselectedTextColor = LgColors.InkSecondary
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
                            tradeState = tradeState,
                            isWide = isWide,
                            onRefreshAll = {
                                viewModel.refreshMarkets()
                                viewModel.refreshFearAndGreed()
                                viewModel.scanArbitrage()
                                viewModel.refreshAssetHistory()
                                if (tradeState.isLiveMode) {
                                    viewModel.refreshLiveAccountBalance(silent = false)
                                }
                            },
                            onRefreshFearGreed = viewModel::refreshFearAndGreed,
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
                            onOpenWalkthrough = { showDigitalWalkthrough = true },
                            onSelectHistoryAsset = viewModel::selectHistoryAsset,
                            onSelectHistoryWindow = viewModel::selectHistoryWindow,
                            onRefreshHistory = viewModel::refreshAssetHistory,
                            onOpenBacktest = { sym ->
                                viewModel.selectTradeSymbol(sym)
                                selectedTradeWorkspace = TradeSubWorkspace.BACKTEST
                                navigateToTab(TradeRoute)
                            },
                            onRefreshPriceSummary = { viewModel.refreshPriceSummaryDashboard(silent = false) },
                            onSelectPriceSummaryFilter = viewModel::selectPriceSummaryExchangeFilter,
                            onOpenExchangeConfig = { showExchangeConfig = true }
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
                            onUpdateIndicatorBacktestConfig = viewModel::updateIndicatorBacktestConfig,
                            onPullHistoricalData = viewModel::pullHistoricalBacktestData,
                            onSelectBaseCurrency = viewModel::selectBaseCurrency,
                            onSelectConnectedExchange = viewModel::selectConnectedExchange,
                            onScanExchangeMarket = { viewModel.scanEntireExchangeForProfit(autoApplyBest = false) },
                            onApplyAdaptiveOpportunity = viewModel::applyAdaptiveOpportunity,
                            onSetExecutionMode = viewModel::setExecutionMode,
                            onSelectLiveRoute = viewModel::selectLiveExecutionRoute,
                            onRefreshLiveBalance = { viewModel.refreshLiveAccountBalance(silent = false) },
                            onOpenExchangeConfig = { showExchangeConfig = true }
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
                            onRetryLastPrompt = viewModel::retryLastAdvisorMessage,
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
                            onOpenHelpSheet = { showHelpSheet = true },
                            biometricLockEnabled = biometricLockEnabled,
                            autoLockOnBackground = autoLockOnBackground,
                            onToggleBiometricLock = ::toggleBiometricLock,
                            onToggleAutoLockBackground = { enabled ->
                                autoLockOnBackground = enabled
                                BiometricAuthManager.setAutoLockOnBackgroundEnabled(context, enabled)
                            },
                            onLockDashboardNow = { isDashboardUnlocked = false },
                            memberSession = memberSession,
                            onOpenWelcomeSignIn = { openSignIn ->
                                welcomeStartOnSignIn = openSignIn
                                showWelcomeFlow = true
                            },
                            membershipProfile = membershipProfile,
                            onOpenMembershipBilling = { showMembershipScreen = true },
                            onOpenExchangeConfig = { showExchangeConfig = true },
                            tradeState = tradeState,
                            onRefreshLiveBalance = { viewModel.refreshLiveAccountBalance(silent = false) }
                        )
                    }
                }
            }
        }
    }

    // Pop-Up Welcome Screen floating over the live turquoise-lined dashboard
    if (showWelcomeFlow) {
        WelcomeSignInScreen(
            initialShowSignIn = welcomeStartOnSignIn,
            existingSession = memberSession,
            isPopUpOverlay = true,
            onOpenMembershipSignUp = { showMembershipScreen = true },
            onCompleted = { session ->
                memberSession = session
                MemberSessionPrefs.save(context, session)
                showWelcomeFlow = false
                welcomeStartOnSignIn = false
            }
        )
    }
    }
}

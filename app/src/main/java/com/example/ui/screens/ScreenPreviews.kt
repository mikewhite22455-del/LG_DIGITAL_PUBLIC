package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.local.PaperAccountEntity
import com.example.data.local.PaperHoldingEntity
import com.example.data.local.PayoutRuleEntity
import com.example.data.local.PriceTriggerEntity
import com.example.data.remote.AdvisorMessage
import com.example.domain.engine.TechnicalEngine
import com.example.domain.models.Candle
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.HistoryWindow
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.MiningQuote
import com.example.domain.models.PriceHistoryPoint
import com.example.domain.models.StrategySignal
import com.example.domain.models.StrategyType
import com.example.domain.models.SignalAction
import com.example.domain.models.UiState
import com.example.ui.theme.LgDigitalTheme
import com.example.viewmodel.AdvisorUiState
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.LabsUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

private val previewTickers = listOf(
    MarketTicker(
        "BTC", "Bitcoin", 91365.0, 4.2, 28_400_000_000.0, 1_800_000_000_000.0, 92100.0, 88200.0, "Preview",
        listOf(88400.0, 88900.0, 88650.0, 89200.0, 89800.0, 89550.0, 90100.0, 90750.0, 90400.0, 91050.0, 91365.0)
    ),
    MarketTicker(
        "ETH", "Ethereum", 3131.0, 2.8, 14_200_000_000.0, 376_000_000_000.0, 3190.0, 3040.0, "Preview",
        listOf(3050.0, 3065.0, 3048.0, 3075.0, 3090.0, 3082.0, 3105.0, 3118.0, 3112.0, 3124.0, 3131.0)
    ),
    MarketTicker(
        "SOL", "Solana", 240.97, -1.5, 4_800_000_000.0, 114_000_000_000.0, 248.0, 236.0, "Preview",
        listOf(245.5, 246.2, 244.8, 243.5, 244.1, 242.9, 241.8, 242.4, 241.3, 240.6, 240.97)
    ),
    MarketTicker(
        "LINK", "Chainlink", 18.42, 8.4, 920_000_000.0, 11_500_000_000.0, 18.9, 16.8, "Preview",
        listOf(16.95, 17.1, 17.05, 17.35, 17.6, 17.55, 17.85, 18.05, 18.18, 18.3, 18.42)
    )
)

private val previewHistoryPoints = (0 until 24).map { idx ->
    PriceHistoryPoint(
        timestampSec = 1760000000L + idx * 3600L,
        priceUsd = 88400.0 + idx * 125.0 + if (idx % 3 == 0) 210.0 else -95.0
    )
}

private val previewMarkets = MarketsUiState(
    tickersState = UiState.Success(previewTickers),
    breadth = MarketBreadth(4, 3, 1, 3.48, 48_320_000_000.0, previewTickers[3], previewTickers[2]),
    selectedHistorySymbol = "BTC",
    historyWindow = HistoryWindow.H24,
    historyState = UiState.Success(previewHistoryPoints)
)

private val previewCandles = (0 until 40).map { idx ->
    val base = 90000.0 + idx * 45.0
    Candle(1760000000L + idx * 3600L, base, base + 220.0, base - 180.0, base + 80.0, 420.0 + idx * 12.0)
}

private val previewVault = VaultUiState(
    account = PaperAccountEntity(usdBalance = 5620.0, peakEquityUsd = 10450.0),
    holdings = listOf(
        PaperHoldingEntity("BTC", 0.04, 88200.0),
        PaperHoldingEntity("LINK", 62.0, 17.10)
    ),
    priceTriggers = listOf(
        PriceTriggerEntity("BTC", 95000.0),
        PriceTriggerEntity("ETH", 3400.0),
        PriceTriggerEntity("SOL", 260.0)
    ),
    payoutRule = PayoutRuleEntity(),
    totalPortfolioEquityUsd = 10416.64,
    holdingsValueUsd = 4796.64,
    dailyPnlUsd = 284.50,
    dailyPnlPct = 2.81,
    unrealizedPnlUsd = 208.44,
    realizedTodayPnlUsd = 76.06,
    totalReturnUsd = 416.64,
    currentDrawdownPct = 0.32
)

@Preview(name = "1. Main M3 Dashboard", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewDashboardScreen() {
    LgDigitalTheme {
        DashboardScreen(
            marketsState = previewMarkets,
            arbitrageState = ArbitrageUiState(),
            vaultState = previewVault,
            onRefreshAll = {},
            onRefreshNews = {},
            onSearchChange = {},
            onToggleArbMode = {},
            onScanArbitrage = {},
            onExecuteOpportunity = {},
            onOpenArbitrageDesk = {},
            onSelectCoinForTrade = {},
            onSelectCoinForArbitrage = {},
            onAskCopilotForCoin = {},
            onNavigateToAdvisor = {},
            onNavigateToMining = {},
            onNavigateToTrade = {},
            onOpenAllPagesPreview = {},
            onSelectHistoryAsset = {},
            onSelectHistoryWindow = {},
            onRefreshHistory = {}
        )
    }
}

@Preview(name = "2. Trade & Backtest Lab", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewTradeScreen() {
    LgDigitalTheme {
        val btCandles = (0 until 60).map { idx ->
            val wave = kotlin.math.sin(idx * 0.35) * 950.0
            val close = 89500.0 + idx * 35.0 + wave
            Candle(1760000000L + idx * 3600L, close - 120.0, close + 240.0, close - 210.0, close, 520.0 + idx * 9.0)
        }
        val cfg = IndicatorBacktestConfig()
        TradeScreen(
            state = TradeUiState(
                candlesState = UiState.Success(btCandles),
                liveSignal = StrategySignal(StrategyType.TREND_CROSS, SignalAction.BUY, 0.82, "SMA 9 crossed above SMA 21", 91365.0),
                indicatorBacktestConfig = cfg,
                indicatorBacktestResult = TechnicalEngine.simulateIndicatorBacktest("BTC", btCandles, ChartTimeframe.H1, cfg)
            ),
            vaultState = previewVault,
            activeWorkspace = TradeSubWorkspace.BACKTEST,
            onSelectSymbol = {},
            onSelectTimeframe = {},
            onSelectOverlay = {},
            onSelectStrategy = {},
            onSubmitPaperOrder = { _, _ -> },
            onToggleAutopilot = {},
            onUpdateRiskConfig = { _, _, _, _ -> },
            onRunAutopilotTick = {},
            onUpdateIndicatorBacktestConfig = {}
        )
    }
}

@Preview(name = "3. Advisor Terminal", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewAdvisorScreen() {
    LgDigitalTheme {
        AdvisorScreen(
            advisorState = AdvisorUiState(
                messages = listOf(
                    AdvisorMessage("1", false, "LG Digital Quant Copilot online. Monitoring BTC/USD Trend Cross."),
                    AdvisorMessage("2", true, "Review my current risk guard parameters.")
                )
            ),
            marketsState = previewMarkets,
            tradeState = TradeUiState(),
            vaultState = previewVault,
            onSendAdvisorPrompt = {},
            onSelectStrategy = {},
            onSelectCoin = {}
        )
    }
}

@Preview(name = "4. Labs Mining & Sports", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewLabsMining() {
    LgDigitalTheme {
        LabsScreen(
            activeTool = LabsTool.MINING,
            onSelectTool = {},
            state = LabsUiState(
                miningQuotes = UiState.Success(
                    listOf(MiningQuote("ALEO", "Aleo", "zkSNARK", 2.45, "720 MH/s", "1300W", 1.25, 18.4))
                )
            ),
            arbitrageState = ArbitrageUiState(),
            vaultState = previewVault,
            onUpdateArbParams = { _, _, _, _, _ -> },
            onScanArbitrage = {},
            onExecuteOpportunity = {},
            onResetInventory = {},
            onScanBreakouts = {},
            onSearchDex = {},
            onAnalyzeResearch = {},
            onSaveWatchlist = { _, _, _, _ -> },
            onRefreshMining = {},
            onOpenCoinInTrade = {}
        )
    }
}

@Preview(name = "5. Vault Alerts & Payouts", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewVaultScreen() {
    LgDigitalTheme {
        VaultScreen(
            vaultState = previewVault,
            onSavePriceTrigger = { _, _, _ -> },
            onDeletePriceTrigger = {},
            onSavePayoutRule = {},
            onResetPortfolio = {},
            onRemoveWatchlist = {},
            onAddWatchWallet = { _, _, _ -> },
            onRemoveWatchWallet = {},
            onOpenHelpSheet = {}
        )
    }
}

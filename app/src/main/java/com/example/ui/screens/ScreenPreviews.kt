package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.local.PaperAccountEntity
import com.example.data.local.PayoutRuleEntity
import com.example.data.local.PriceTriggerEntity
import com.example.data.remote.AdvisorMessage
import com.example.domain.models.Candle
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.MiningQuote
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
    MarketTicker("BTC", "Bitcoin", 91365.0, 4.2, 28_400_000_000.0, 1_800_000_000_000.0, 92100.0, 88200.0, "Preview"),
    MarketTicker("ETH", "Ethereum", 3131.0, 2.8, 14_200_000_000.0, 376_000_000_000.0, 3190.0, 3040.0, "Preview"),
    MarketTicker("SOL", "Solana", 240.97, -1.5, 4_800_000_000.0, 114_000_000_000.0, 248.0, 236.0, "Preview"),
    MarketTicker("LINK", "Chainlink", 18.42, 8.4, 920_000_000.0, 11_500_000_000.0, 18.9, 16.8, "Preview")
)

private val previewMarkets = MarketsUiState(
    tickersState = UiState.Success(previewTickers),
    breadth = MarketBreadth(4, 3, 1, 3.48, 48_320_000_000.0, previewTickers[3], previewTickers[2])
)

private val previewCandles = (0 until 40).map { idx ->
    val base = 90000.0 + idx * 45.0
    Candle(1760000000L + idx * 3600L, base, base + 220.0, base - 180.0, base + 80.0, 420.0 + idx * 12.0)
}

private val previewVault = VaultUiState(
    account = PaperAccountEntity(),
    priceTriggers = listOf(
        PriceTriggerEntity("BTC", 95000.0),
        PriceTriggerEntity("ETH", 3400.0),
        PriceTriggerEntity("SOL", 260.0)
    ),
    payoutRule = PayoutRuleEntity(),
    totalPortfolioEquityUsd = 10412.50,
    currentDrawdownPct = 0.85
)

@Preview(name = "1. Home Markets", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewMarketsScreen() {
    LgDigitalTheme {
        MarketsScreen(
            state = previewMarkets,
            vaultState = previewVault,
            onRefreshMarkets = {},
            onRefreshNews = {},
            onSearchChange = {},
            onSelectCoinForTrade = {},
            onSelectCoinForArbitrage = {},
            onAskCopilotForCoin = {},
            onNavigateToAdvisor = {},
            onNavigateToMining = {},
            onNavigateToTrade = {},
            onOpenAllPagesPreview = {}
        )
    }
}

@Preview(name = "2. Trade Adaptive", showBackground = true, backgroundColor = 0xFF040B11)
@Composable
private fun PreviewTradeScreen() {
    LgDigitalTheme {
        TradeScreen(
            state = TradeUiState(
                candlesState = UiState.Success(previewCandles),
                liveSignal = StrategySignal(StrategyType.TREND_CROSS, SignalAction.BUY, 0.82, "SMA 9 crossed above SMA 21", 91365.0)
            ),
            vaultState = previewVault,
            onSelectSymbol = {},
            onSelectTimeframe = {},
            onSelectOverlay = {},
            onSelectStrategy = {},
            onSubmitPaperOrder = { _, _ -> },
            onToggleAutopilot = {},
            onUpdateRiskConfig = { _, _, _, _ -> },
            onRunAutopilotTick = {}
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

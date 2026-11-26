package com.example.viewmodel

import com.example.data.local.AgentMemoryEntity
import com.example.data.local.ArbitrageInventoryEntity
import com.example.data.local.ArbitrageTradeEntity
import com.example.data.local.PaperAccountEntity
import com.example.data.local.PaperHoldingEntity
import com.example.data.local.PayoutRuleEntity
import com.example.data.local.PriceTriggerEntity
import com.example.data.local.ResearchWatchlistEntity
import com.example.data.local.TradeLogEntity
import com.example.data.local.WatchWalletEntity
import com.example.data.remote.AdvisorMessage
import com.example.domain.models.AdaptiveOpportunity
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.BacktestResult
import com.example.domain.models.BaseFiatCurrency
import com.example.domain.models.BreakoutCandidate
import com.example.domain.models.Candle
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConnectedExchange
import com.example.domain.models.DexPairInfo
import com.example.domain.models.FearGreedIndexData
import com.example.domain.models.HistoryWindow
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.IndicatorBacktestResult
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.MiningQuote
import com.example.domain.models.NewsItem
import com.example.domain.models.PriceHistoryPoint
import com.example.domain.models.ResearchProjection
import com.example.domain.models.StrategySignal
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState

data class ConfiguredExchangePriceRow(
    val id: String,
    val exchangeId: String,
    val exchangeName: String,
    val symbol: String,
    val pairLabel: String,
    val priceUsd: Double,
    val change24hPct: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24hUsd: Double,
    val isApiConfigured: Boolean,
    val fetchedAtMs: Long
)

data class MarketsUiState(
    val tickersState: UiState<List<MarketTicker>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val breadth: MarketBreadth? = null,
    val newsState: UiState<List<NewsItem>> = UiState.Loading,
    val searchQuery: String = "",
    val lastUpdatedMs: Long = 0L,
    val selectedHistorySymbol: String = "BTC",
    val historyWindow: HistoryWindow = HistoryWindow.H24,
    val historyState: UiState<List<PriceHistoryPoint>> = UiState.Loading,
    val fearGreedState: UiState<FearGreedIndexData> = UiState.Loading,
    val isRefreshingFearGreed: Boolean = false,
    val priceSummaryRows: List<ConfiguredExchangePriceRow> = emptyList(),
    val configuredSummaryExchanges: List<String> = emptyList(),
    val isRefreshingPriceSummary: Boolean = false,
    val priceSummaryUpdatedMs: Long = 0L,
    val priceSummaryExchangeFilter: String = "ALL",
    val priceSummaryError: String? = null
)

data class TradeUiState(
    val symbol: String = "BTC",
    val timeframe: ChartTimeframe = ChartTimeframe.H1,
    val overlay: ChartOverlay = ChartOverlay.SMA_CROSS,
    val strategy: StrategyType = StrategyType.TREND_CROSS,
    val candlesState: UiState<List<Candle>> = UiState.Loading,
    val liveSignal: StrategySignal? = null,
    val backtestResult: BacktestResult? = null,
    val indicatorBacktestConfig: IndicatorBacktestConfig = IndicatorBacktestConfig(),
    val indicatorBacktestResult: IndicatorBacktestResult? = null,
    val feeBps: Double = 10.0,
    val slippageBps: Double = 5.0,
    val statusBanner: String? = null,
    val baseCurrency: BaseFiatCurrency = BaseFiatCurrency.USD,
    val usdToCadRate: Double = 1.38,
    val connectedExchange: ConnectedExchange = ConnectedExchange.ALL_CONNECTED,
    val isScanningExchange: Boolean = false,
    val totalExchangeMarketsScanned: Int = 0,
    val venuesScannedLabel: String = "Kraken · KuCoin · Bitstamp",
    val adaptiveOpportunities: List<AdaptiveOpportunity> = emptyList(),
    val exchangeScanSummary: String? = null,
    val lastExchangeScanMs: Long = 0L,
    val executionMode: com.example.domain.models.ExecutionMode = com.example.domain.models.ExecutionMode.PAPER,
    val liveExecutionRoute: com.example.domain.models.LiveExecutionRoute = com.example.domain.models.LiveExecutionRoute.KRAKEN_DIRECT,
    val liveConnectors: List<com.example.domain.models.LiveConnectorStatus> = emptyList(),
    val isSubmittingLiveOrder: Boolean = false,
    val recentLiveReceipts: List<com.example.domain.models.LiveOrderReceipt> = emptyList(),
    val liveAccountSnapshot: com.example.domain.models.LiveAccountSnapshot? = null,
    val isSyncingLiveBalance: Boolean = false,
    val liveBalanceError: String? = null
) {
    val isLiveMode: Boolean
        get() = executionMode == com.example.domain.models.ExecutionMode.LIVE

    val liveEquityUsd: Double
        get() = liveAccountSnapshot?.totalEquityUsd ?: 0.0

    val liveAvailableCashUsd: Double
        get() = liveAccountSnapshot?.availableCashUsd ?: 0.0

    val liveHoldingsValueUsd: Double
        get() = liveAccountSnapshot?.holdingsValueUsd ?: 0.0
}

data class ArbitrageUiState(
    val symbol: String = "BTC",
    val mode: ArbitrageMode = ArbitrageMode.DEMO,
    val tradeNotionalUsd: Double = 250.0,
    val minSpreadBps: Double = 35.0,
    val onlyQualified: Boolean = false,
    val isScanning: Boolean = false,
    val error: String? = null,
    val opportunities: List<ArbitrageOpportunity> = emptyList(),
    val activeVenuesCount: Int = 0,
    val bannerMessage: String? = null
)

data class LabsUiState(
    val breakoutCandidates: List<BreakoutCandidate> = emptyList(),
    val breakoutLoading: Boolean = false,
    val breakoutError: String? = null,
    val dexQuery: String = "SOL",
    val dexResults: UiState<List<DexPairInfo>> = UiState.Loading,
    val researchSymbol: String = "BTC",
    val researchProjection: UiState<ResearchProjection> = UiState.Loading,
    val miningQuotes: UiState<List<MiningQuote>> = UiState.Loading
)

data class VaultUiState(
    val account: PaperAccountEntity = PaperAccountEntity(),
    val holdings: List<PaperHoldingEntity> = emptyList(),
    val tradeLog: List<TradeLogEntity> = emptyList(),
    val agentMemory: List<AgentMemoryEntity> = emptyList(),
    val watchlist: List<ResearchWatchlistEntity> = emptyList(),
    val watchWallets: List<WatchWalletEntity> = emptyList(),
    val arbitrageInventory: List<ArbitrageInventoryEntity> = emptyList(),
    val arbitrageTrades: List<ArbitrageTradeEntity> = emptyList(),
    val priceTriggers: List<PriceTriggerEntity> = emptyList(),
    val payoutRule: PayoutRuleEntity = PayoutRuleEntity(),
    val totalPortfolioEquityUsd: Double = 10000.0,
    val holdingsValueUsd: Double = 0.0,
    val dailyPnlUsd: Double = 0.0,
    val dailyPnlPct: Double = 0.0,
    val unrealizedPnlUsd: Double = 0.0,
    val realizedTodayPnlUsd: Double = 0.0,
    val totalReturnUsd: Double = 0.0,
    val currentDrawdownPct: Double = 0.0
)

data class AdvisorUiState(
    val messages: List<AdvisorMessage> = listOf(
        AdvisorMessage(
            id = "welcome",
            isUser = false,
            text = "LG Digital AI Trading Advisor connected to Gemini with live market telemetry. Ask about the active coin's setup, technical signals, backtest metrics, risk sizing, or cross-exchange arbitrage."
        )
    ),
    val streamingReply: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val liveContextPreview: String = "",
    val contextUpdatedAtMs: Long = 0L,
    val contextSymbol: String = "BTC",
    val isRefreshingContext: Boolean = false
)

enum class ExchangeHealthState {
    NOT_CONFIGURED,
    CHECKING,
    CONNECTED,
    ERROR
}

data class ExchangeConnectionStatus(
    val exchangeId: String,
    val state: ExchangeHealthState = ExchangeHealthState.NOT_CONFIGURED,
    val latencyMs: Long = 0L,
    val detail: String = "No API keys stored",
    val lastCheckedMs: Long = 0L,
    val endpoint: String = "",
    val httpStatusCode: Int? = null,
    val publicApiReachable: Boolean? = null,
    val publicLatencyMs: Long = 0L,
    val credentialSource: String = "Not Stored"
)

data class ExchangePingLogEntry(
    val id: String,
    val exchangeId: String,
    val exchangeName: String,
    val state: ExchangeHealthState,
    val latencyMs: Long,
    val httpStatusCode: Int?,
    val endpoint: String,
    val summary: String,
    val timestampMs: Long
)

data class ExchangeConfigUiState(
    val credentials: com.example.data.local.EncryptedExchangeCredentials = com.example.data.local.EncryptedExchangeCredentials(),
    val testingExchangeId: String? = null,
    val isVerifyingAll: Boolean = false,
    val isPingingKrakenCoinbase: Boolean = false,
    val autoVerifyEnabled: Boolean = true,
    val connectionTestResults: Map<String, String> = emptyMap(),
    val connectionStatuses: Map<String, ExchangeConnectionStatus> = emptyMap(),
    val pingHistory: List<ExchangePingLogEntry> = emptyList(),
    val statusMessage: String? = null
)

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
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.BacktestResult
import com.example.domain.models.BreakoutCandidate
import com.example.domain.models.Candle
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.DexPairInfo
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.MiningQuote
import com.example.domain.models.NewsItem
import com.example.domain.models.ResearchProjection
import com.example.domain.models.StrategySignal
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState

data class MarketsUiState(
    val tickersState: UiState<List<MarketTicker>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val breadth: MarketBreadth? = null,
    val newsState: UiState<List<NewsItem>> = UiState.Loading,
    val searchQuery: String = "",
    val lastUpdatedMs: Long = 0L
)

data class TradeUiState(
    val symbol: String = "BTC",
    val timeframe: ChartTimeframe = ChartTimeframe.H1,
    val overlay: ChartOverlay = ChartOverlay.SMA_CROSS,
    val strategy: StrategyType = StrategyType.TREND_CROSS,
    val candlesState: UiState<List<Candle>> = UiState.Loading,
    val liveSignal: StrategySignal? = null,
    val backtestResult: BacktestResult? = null,
    val feeBps: Double = 10.0,
    val slippageBps: Double = 5.0,
    val statusBanner: String? = null
)

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
    val currentDrawdownPct: Double = 0.0
)

data class AdvisorUiState(
    val messages: List<AdvisorMessage> = listOf(
        AdvisorMessage(
            id = "welcome",
            isUser = false,
            text = "LG Digital Quant Copilot ready. Ask me about live market breadth, your active strategy signal, risk sizing, or cross-exchange arbitrage mechanics."
        )
    ),
    val streamingReply: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null
)

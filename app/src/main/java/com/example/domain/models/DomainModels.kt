package com.example.domain.models

import java.math.BigDecimal

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MarketTicker(
    val symbol: String,
    val name: String,
    val priceUsd: Double,
    val change24hPct: Double,
    val volume24hUsd: Double,
    val marketCapUsd: Double,
    val high24h: Double,
    val low24h: Double,
    val source: String,
    val hourlyPrices: List<Double> = emptyList()
)

data class MarketBreadth(
    val totalAssets: Int,
    val advancing: Int,
    val declining: Int,
    val avgChangePct: Double,
    val totalVolume24hUsd: Double,
    val topGainer: MarketTicker?,
    val topLoser: MarketTicker?
)

data class FearGreedPoint(
    val value: Int,
    val classification: String,
    val timestampSec: Long
)

data class FearGreedIndexData(
    val apiValue: Int,
    val apiClassification: String,
    val liveValue: Int,
    val liveClassification: String,
    val liveMomentumAdjustment: Int,
    val yesterdayValue: Int?,
    val lastWeekValue: Int?,
    val history7d: List<FearGreedPoint>,
    val nextUpdateSec: Long?,
    val updatedAtMs: Long,
    val source: String
)

data class Candle(
    val timestampSec: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class HistoryWindow(
    val label: String,
    val subtitle: String,
    val krakenInterval: Int,
    val kuType: String,
    val candleCount: Int,
    val cgDays: String,
    val lookbackSec: Long
) {
    H1("1H", "1-Hour", 1, "1min", 60, "1", 3600L),
    H24("24H", "24-Hour", 15, "15min", 96, "1", 86400L),
    D7("7D", "7-Day", 60, "1hour", 168, "7", 7L * 86400L)
}

data class PriceHistoryPoint(
    val timestampSec: Long,
    val priceUsd: Double
)

enum class ChartTimeframe(val label: String, val krakenInterval: Int, val barsPerDay: Int) {
    M15("15m", 15, 96),
    H1("1h", 60, 24),
    H4("4h", 240, 6),
    D1("1d", 1440, 1)
}

enum class ChartOverlay(val label: String) {
    NONE("Price Only"),
    SMA_CROSS("SMA 9 / 21"),
    EMA_CROSS("EMA 12 / 26"),
    BOLLINGER("Bollinger 20 ± 2σ"),
    SUPERTREND("Supertrend (10, 3x)")
}

enum class StrategyType(
    val id: String,
    val title: String,
    val summary: String
) {
    TREND_CROSS("trend_cross", "Trend Cross (SMA 9/21)", "Fast SMA 9 crossing slow SMA 21 with trend bias"),
    RSI_REVERSAL("rsi_reversal", "RSI Reversal (14)", "Mean reversion around 30 oversold / 70 overbought"),
    RANGE_BREAKOUT("range_breakout", "Range Breakout (20)", "20-candle structural high/low breakout"),
    EMA_CROSS("ema_cross", "EMA Cross (12/26)", "Exponential fast/slow crossover with momentum"),
    MACD_CROSS("macd_cross", "MACD Cross (12/26/9)", "MACD line crossing 9-period signal line"),
    BOLLINGER_REVERSION("bollinger_reversion", "Bollinger Reversion", "Mean reversion against 20-period ± 2σ bands"),
    VOLUME_BREAKOUT("volume_breakout", "Volume Breakout", "20-bar range break confirmed by 1.5x average volume"),
    VWAP_TREND("vwap_trend", "VWAP Trend (20)", "Rolling 20-bar volume-weighted average price reclaim"),
    ATR_BREAKOUT("atr_breakout", "ATR Breakout (14)", "Volatility-adjusted 0.8× ATR(14) expansion trigger"),
    SUPERTREND("supertrend", "Supertrend (10, 3.0x)", "ATR(10) × 3.0 adaptive trend flip trailing band"),
    MULTI_PAIR_GRID("multi_pair_grid", "Multi-Pair Grid", "5-level dynamic grid over 30-candle price channel")
}

enum class SignalAction { BUY, SELL, HOLD }

data class StrategySignal(
    val strategy: StrategyType,
    val action: SignalAction,
    val confidence: Double,
    val reason: String,
    val entryPrice: Double,
    val indicatorValues: Map<String, Double> = emptyMap()
)

data class BacktestTrade(
    val timestampSec: Long,
    val side: SignalAction,
    val price: Double,
    val returnPct: Double,
    val phase: String
)

data class BacktestResult(
    val symbol: String,
    val strategy: StrategyType,
    val timeframe: ChartTimeframe,
    val totalReturnPct: Double,
    val trainReturnPct: Double,
    val validationReturnPct: Double,
    val winRatePct: Double,
    val maxDrawdownPct: Double,
    val totalTrades: Int,
    val feeBps: Double,
    val slippageBps: Double,
    val equityCurve: List<Double>,
    val recentTrades: List<BacktestTrade>
)

enum class TechnicalIndicatorType(
    val id: String,
    val label: String,
    val shortDesc: String
) {
    RSI("rsi", "RSI", "Relative Strength oversold/overbought"),
    MACD("macd", "MACD", "EMA momentum & signal line crossover"),
    BOLLINGER("bollinger", "Bollinger Bands", "Rolling SMA ± σ volatility envelope")
}

enum class ConfluenceRule(val label: String) {
    ANY("Any Indicator (OR)"),
    ALL("All Selected (AND)")
}

data class IndicatorBacktestConfig(
    val selectedIndicators: Set<TechnicalIndicatorType> = setOf(
        TechnicalIndicatorType.RSI,
        TechnicalIndicatorType.MACD
    ),
    val confluenceRule: ConfluenceRule = ConfluenceRule.ANY,
    val initialCapitalUsd: Double = 10000.0,
    val rsiPeriod: Int = 14,
    val rsiOversold: Double = 32.0,
    val rsiOverbought: Double = 68.0,
    val macdFast: Int = 12,
    val macdSlow: Int = 26,
    val macdSignal: Int = 9,
    val bbPeriod: Int = 20,
    val bbStdDev: Double = 2.0,
    val feeBps: Double = 10.0,
    val slippageBps: Double = 5.0
)

data class SimulatedSignalMarker(
    val candleIndex: Int,
    val timestampSec: Long,
    val action: SignalAction,
    val priceUsd: Double,
    val entryPriceUsd: Double? = null,
    val returnPct: Double? = null,
    val pnlUsd: Double? = null,
    val reason: String
)

data class EquityCurvePoint(
    val candleIndex: Int,
    val timestampSec: Long,
    val priceUsd: Double,
    val strategyEquityUsd: Double,
    val buyHoldEquityUsd: Double,
    val rsiValue: Double?,
    val macdHistogram: Double?,
    val bbUpper: Double?,
    val bbMid: Double?,
    val bbLower: Double?
)

data class IndicatorBacktestResult(
    val symbol: String,
    val timeframe: ChartTimeframe,
    val config: IndicatorBacktestConfig,
    val initialCapitalUsd: Double,
    val finalEquityUsd: Double,
    val totalReturnPct: Double,
    val buyHoldReturnPct: Double,
    val alphaPct: Double,
    val winRatePct: Double,
    val maxDrawdownPct: Double,
    val profitFactor: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val equityPoints: List<EquityCurvePoint>,
    val markers: List<SimulatedSignalMarker>,
    val closedTrades: List<SimulatedSignalMarker>
)

data class ResearchProjection(
    val symbol: String,
    val currentPrice: Double,
    val projected24hPrice: Double,
    val projectedChangePct: Double,
    val support20: Double,
    val resistance20: Double,
    val rsi14: Double,
    val atr14: Double,
    val regimeLabel: String
)

enum class RiskProfile(
    val label: String,
    val maxOrderUsd: Double,
    val maxDailyNotionalUsd: Double,
    val defaultStopLossPct: Double,
    val description: String
) {
    LOW("LOW", 125.0, 450.0, 2.5, "Conservative sizing, strict drawdown lock"),
    MEDIUM("MEDIUM", 350.0, 1200.0, 4.0, "Balanced adaptive momentum allocation"),
    HIGH("HIGH", 750.0, 3000.0, 6.5, "Aggressive trend & breakout capture")
}

enum class BaseFiatCurrency(
    val code: String,
    val symbolPrefix: String,
    val displayLabel: String
) {
    USD("USD", "$", "USD ($)"),
    CAD("CAD", "CA$", "CAD (CA$)")
}

enum class ConnectedExchange(
    val id: String,
    val displayName: String,
    val shortLabel: String
) {
    ALL_CONNECTED("all", "All Connected Exchanges", "ALL"),
    KRAKEN("kraken", "Kraken Pro", "Kraken"),
    COINBASE("coinbase", "Coinbase Advanced", "Coinbase"),
    KUCOIN("kucoin", "KuCoin Spot", "KuCoin"),
    BITSTAMP("bitstamp", "Bitstamp Spot", "Bitstamp")
}

enum class ExecutionMode(val label: String) {
    PAPER("PAPER"),
    LIVE("LIVE")
}

enum class LiveExecutionRoute(
    val id: String,
    val label: String,
    val shortBadge: String,
    val description: String
) {
    KRAKEN_DIRECT(
        id = "kraken_direct",
        label = "Kraken Pro Direct API",
        shortBadge = "KRAKEN PRO",
        description = "Direct HMAC-SHA512 signed spot orders on Kraken Pro"
    ),
    COINBASE_DIRECT(
        id = "coinbase_direct",
        label = "Coinbase Advanced Direct API",
        shortBadge = "COINBASE",
        description = "Direct signed spot orders on Coinbase Advanced Trade"
    ),
    KUCOIN_DIRECT(
        id = "kucoin_direct",
        label = "KuCoin Spot Direct API",
        shortBadge = "KUCOIN",
        description = "Direct HMAC-SHA256 signed spot orders on KuCoin"
    ),
    BITSTAMP_DIRECT(
        id = "bitstamp_direct",
        label = "Bitstamp Direct API",
        shortBadge = "BITSTAMP",
        description = "Direct v2 HMAC-SHA256 signed spot orders on Bitstamp"
    )
}

data class LiveConnectorStatus(
    val route: LiveExecutionRoute,
    val isConfigured: Boolean,
    val detail: String
)

data class LiveOrderReceipt(
    val route: LiveExecutionRoute,
    val exchangeOrderId: String,
    val symbol: String,
    val side: String,
    val notionalUsd: Double,
    val statusText: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ExchangeMarketTicker(
    val symbol: String,
    val quoteCurrency: String,
    val exchangeName: String,
    val pairLabel: String,
    val priceUsd: Double,
    val change24hPct: Double,
    val volume24hUsd: Double,
    val high24h: Double,
    val low24h: Double,
    val spreadBps: Double
)

data class ExchangeMarketScanResult(
    val exchange: ConnectedExchange,
    val totalMarketsScanned: Int,
    val venuesScanned: List<String>,
    val candidates: List<ExchangeMarketTicker>,
    val scannedAtMs: Long
)

data class AdaptiveOpportunity(
    val symbol: String,
    val exchangeName: String,
    val pairLabel: String,
    val priceUsd: Double,
    val change24hPct: Double,
    val volume24hUsd: Double,
    val recommendedStrategy: StrategyType,
    val signalAction: SignalAction,
    val signalConfidence: Double,
    val backtestReturnPct: Double,
    val winRatePct: Double,
    val maxDrawdownPct: Double,
    val profitScore: Double,
    val reason: String
)

data class NewsItem(
    val title: String,
    val link: String,
    val publishedAt: String,
    val source: String,
    val summary: String
)

data class OrderBookLevel(
    val price: BigDecimal,
    val quantity: BigDecimal
)

data class OrderBookSnapshot(
    val venue: String,
    val pair: String,
    val timestampMs: Long,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val isSynthetic: Boolean
)

enum class ArbitrageMode(val label: String) {
    DEMO("Demo (Synthetic)"),
    PUBLIC("Public Live Books")
}

data class ArbitrageOpportunity(
    val id: String,
    val symbol: String,
    val buyVenue: String,
    val sellVenue: String,
    val buyVwap: BigDecimal,
    val sellVwap: BigDecimal,
    val baseQuantity: BigDecimal,
    val notionalUsd: BigDecimal,
    val grossSpreadBps: Double,
    val takerFeesUsd: BigDecimal,
    val slippageBufferUsd: BigDecimal,
    val rebalanceReserveUsd: BigDecimal,
    val worstCaseNetUsd: BigDecimal,
    val worstCaseNetBps: Double,
    val qualified: Boolean,
    val rejectionReasons: List<String>,
    val isSynthetic: Boolean,
    val scannedAtMs: Long
)

data class BreakoutCandidate(
    val symbol: String,
    val priceUsd: Double,
    val breakoutLevel: Double,
    val volumeRatio: Double,
    val change24hPct: Double,
    val strengthScore: Double
)

data class DexPairInfo(
    val chainId: String,
    val dexId: String,
    val baseSymbol: String,
    val quoteSymbol: String,
    val priceUsd: Double,
    val priceChange24h: Double,
    val volume24hUsd: Double,
    val liquidityUsd: Double,
    val pairUrl: String
)

data class MiningQuote(
    val symbol: String,
    val name: String,
    val algorithm: String,
    val priceUsd: Double,
    val networkHashrateLabel: String,
    val difficultyLabel: String,
    val blockReward: Double,
    val estDailyUsdPerUnit: Double
)

data class SportsArbResult(
    val impliedProbTotalPct: Double,
    val edgePct: Double,
    val isArbitrage: Boolean,
    val stakeA: Double,
    val stakeB: Double,
    val guaranteedPayout: Double,
    val guaranteedProfit: Double
)

data class KellyResult(
    val fullKellyPct: Double,
    val fractionalKellyPct: Double,
    val recommendedStakeUsd: Double,
    val expectedValuePct: Double
)

data class DevigResult(
    val fairProbA: Double,
    val fairProbB: Double,
    val fairOddsA: Double,
    val fairOddsB: Double,
    val vigPct: Double
)

data class PoissonSimResult(
    val homeWinPct: Double,
    val drawPct: Double,
    val awayWinPct: Double,
    val over25Pct: Double,
    val mostLikelyScore: String
)

data class CompoundApyResult(
    val effectiveApyPct: Double,
    val finalValueUsd: Double,
    val totalYieldUsd: Double
)

data class ImpermanentLossResult(
    val ilPct: Double,
    val hodlValueUsd: Double,
    val lpValueUsd: Double,
    val netDiffUsd: Double
)

data class RiskSizeResult(
    val riskAmountUsd: Double,
    val stopDistancePct: Double,
    val positionQuantity: Double,
    val positionNotionalUsd: Double,
    val target2RPrice: Double,
    val target3RPrice: Double
)

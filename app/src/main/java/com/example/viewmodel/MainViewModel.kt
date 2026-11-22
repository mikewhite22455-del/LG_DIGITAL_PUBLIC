package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.data.local.AppDatabase
import com.example.data.local.EncryptedExchangeCredentialStore
import com.example.data.local.PayoutRuleEntity
import com.example.data.local.PriceTriggerEntity
import com.example.data.local.TradingRepository
import com.example.data.remote.AdvisorMessage
import com.example.data.remote.AiAdvisorRepository
import com.example.data.remote.ExchangePingService
import com.example.data.remote.LiveTradingClient
import com.example.data.remote.MarketApiClient
import com.example.domain.engine.ArbitrageEngine
import com.example.domain.engine.TechnicalEngine
import com.example.domain.models.AdaptiveOpportunity
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.BaseFiatCurrency
import com.example.domain.models.Candle
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConnectedExchange
import com.example.domain.models.ExecutionMode
import com.example.domain.models.FearGreedIndexData
import com.example.domain.models.HistoryWindow
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.LiveExecutionRoute
import com.example.domain.models.LiveServerTarget
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.RiskProfile
import com.example.domain.models.SignalAction
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TradingRepository(db.appDao())
    private val credentialStore = EncryptedExchangeCredentialStore(application)
    private val apiClient = MarketApiClient()
    private val liveTradingClient = LiveTradingClient()
    private val exchangePingService = ExchangePingService(credentialStore, liveTradingClient)
    private val aiRepository = AiAdvisorRepository()

    private val _marketsState = MutableStateFlow(MarketsUiState())
    val marketsState: StateFlow<MarketsUiState> = _marketsState.asStateFlow()

    private val _tradeState = MutableStateFlow(
        TradeUiState(liveConnectors = liveTradingClient.getConnectorStatuses())
    )
    val tradeState: StateFlow<TradeUiState> = _tradeState.asStateFlow()

    private val _arbitrageState = MutableStateFlow(ArbitrageUiState())
    val arbitrageState: StateFlow<ArbitrageUiState> = _arbitrageState.asStateFlow()

    private val _labsState = MutableStateFlow(LabsUiState())
    val labsState: StateFlow<LabsUiState> = _labsState.asStateFlow()

    private val _vaultState = MutableStateFlow(VaultUiState())
    val vaultState: StateFlow<VaultUiState> = _vaultState.asStateFlow()

    private val _advisorState = MutableStateFlow(AdvisorUiState())
    val advisorState: StateFlow<AdvisorUiState> = _advisorState.asStateFlow()

    private val _exchangeConfigState = MutableStateFlow(ExchangeConfigUiState())
    val exchangeConfigState: StateFlow<ExchangeConfigUiState> = _exchangeConfigState.asStateFlow()

    private var cachedCandles: List<Candle> = emptyList()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        observeEncryptedCredentials()
        observeVaultStreams()
        refreshMarkets()
        refreshPriceSummaryDashboard()
        refreshFearAndGreed()
        refreshFxRate()
        refreshNews()
        loadAssetHistory("BTC", HistoryWindow.H24)
        loadTradeContext("BTC", ChartTimeframe.H1, StrategyType.TREND_CROSS)
        scanEntireExchangeForProfit(autoApplyBest = false)
        pingLiveServers()
        scanArbitrage()
        scanBreakouts()
        searchDexPairs("SOL")
        loadMiningQuotes()
        startAutoLiveTelemetryLoop()
    }

    private fun observeEncryptedCredentials() {
        viewModelScope.launch {
            credentialStore.credentialsFlow.collect { creds ->
                _exchangeConfigState.update { it.copy(credentials = creds) }
                _tradeState.update { it.copy(liveConnectors = liveTradingClient.getConnectorStatuses(creds)) }
                refreshPriceSummaryDashboard(silent = true)
            }
        }
    }

    fun saveKrakenCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            credentialStore.saveKrakenCredentials(apiKey, apiSecret)
            repository.logMemory("VAULT_API_SAVED", "KRAKEN", "Kraken Pro API credentials encrypted (AES-256-GCM) in local DataStore.")
            _exchangeConfigState.update {
                it.copy(statusMessage = "Kraken Pro credentials encrypted. Verifying live API connectivity...")
            }
            testExchangeConnection("kraken")
        }
    }

    fun saveCoinbaseCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            credentialStore.saveCoinbaseCredentials(apiKey, apiSecret)
            repository.logMemory("VAULT_API_SAVED", "COINBASE", "Coinbase Advanced API credentials encrypted (AES-256-GCM) in local DataStore.")
            _exchangeConfigState.update {
                it.copy(statusMessage = "Coinbase credentials encrypted. Verifying live API connectivity...")
            }
            testExchangeConnection("coinbase")
        }
    }

    fun saveKuCoinCredentials(apiKey: String, apiSecret: String, passphrase: String) {
        viewModelScope.launch {
            credentialStore.saveKuCoinCredentials(apiKey, apiSecret, passphrase)
            repository.logMemory("VAULT_API_SAVED", "KUCOIN", "KuCoin Spot API credentials encrypted (AES-256-GCM) in local DataStore.")
            _exchangeConfigState.update {
                it.copy(statusMessage = "KuCoin credentials encrypted. Verifying live API connectivity...")
            }
            testExchangeConnection("kucoin")
        }
    }

    fun saveBitstampCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            credentialStore.saveBitstampCredentials(apiKey, apiSecret)
            repository.logMemory("VAULT_API_SAVED", "BITSTAMP", "Bitstamp API credentials encrypted (AES-256-GCM) in local DataStore.")
            _exchangeConfigState.update {
                it.copy(statusMessage = "Bitstamp credentials encrypted. Verifying live API connectivity...")
            }
            testExchangeConnection("bitstamp")
        }
    }

    fun saveLgServerToken(token: String) {
        viewModelScope.launch {
            credentialStore.saveLgServerToken(token)
            repository.logMemory("VAULT_API_SAVED", "LG_SERVER", "LG Render bearer token encrypted (AES-256-GCM) in local DataStore.")
            _exchangeConfigState.update {
                it.copy(statusMessage = "LG Render server token encrypted and saved locally.")
            }
            pingLiveServers()
        }
    }

    fun setAutoVerifyExchanges(enabled: Boolean) {
        _exchangeConfigState.update { it.copy(autoVerifyEnabled = enabled) }
    }

    fun testExchangeConnection(exchangeId: String) {
        viewModelScope.launch {
            val id = exchangeId.lowercase()
            _exchangeConfigState.update { st ->
                st.copy(
                    testingExchangeId = id,
                    connectionStatuses = st.connectionStatuses + (
                        id to ExchangeConnectionStatus(
                            exchangeId = id,
                            state = ExchangeHealthState.CHECKING,
                            detail = "Pinging public gateway & signed HTTPS endpoint...",
                            lastCheckedMs = System.currentTimeMillis()
                        )
                    )
                )
            }
            val report = exchangePingService.pingExchange(id)
            val status = report.status
            _exchangeConfigState.update { st ->
                val updatedHistory = (listOf(report.logEntry) + st.pingHistory).take(12)
                st.copy(
                    testingExchangeId = if (st.testingExchangeId == id) null else st.testingExchangeId,
                    connectionStatuses = st.connectionStatuses + (id to status),
                    connectionTestResults = st.connectionTestResults + (id to status.detail),
                    pingHistory = updatedHistory,
                    statusMessage = status.detail
                )
            }
        }
    }

    /**
     * Runs the dedicated ExchangePingService concurrently against Kraken Pro and Coinbase Advanced
     * using locally stored AES-256-GCM encrypted credentials.
     */
    fun pingKrakenAndCoinbase() {
        viewModelScope.launch {
            val targets = listOf("kraken", "coinbase")
            _exchangeConfigState.update { st ->
                val checkingMap = st.connectionStatuses.toMutableMap()
                targets.forEach { id ->
                    checkingMap[id] = ExchangeConnectionStatus(
                        exchangeId = id,
                        state = ExchangeHealthState.CHECKING,
                        detail = "Pinging ${id.replaceFirstChar { it.uppercase() }} gateway & encrypted credentials...",
                        lastCheckedMs = System.currentTimeMillis()
                    )
                }
                st.copy(
                    isPingingKrakenCoinbase = true,
                    connectionStatuses = checkingMap
                )
            }

            val batch = exchangePingService.pingKrakenAndCoinbase()
            _exchangeConfigState.update { st ->
                val updatedHistory = (batch.logEntries + st.pingHistory).take(12)
                st.copy(
                    isPingingKrakenCoinbase = false,
                    connectionStatuses = st.connectionStatuses + batch.statuses,
                    connectionTestResults = st.connectionTestResults + batch.statuses
                        .filterValues { it.state != ExchangeHealthState.NOT_CONFIGURED }
                        .mapValues { it.value.detail },
                    pingHistory = updatedHistory,
                    statusMessage = batch.summaryMessage
                )
            }
        }
    }

    fun verifyAllExchangeConnections() {
        viewModelScope.launch {
            val exchangeIds = listOf("kraken", "coinbase", "kucoin", "bitstamp")
            _exchangeConfigState.update { st ->
                val checkingMap = st.connectionStatuses.toMutableMap()
                exchangeIds.forEach { id ->
                    checkingMap[id] = ExchangeConnectionStatus(
                        exchangeId = id,
                        state = ExchangeHealthState.CHECKING,
                        detail = "Pinging public gateway & signed API...",
                        lastCheckedMs = System.currentTimeMillis()
                    )
                }
                st.copy(
                    isVerifyingAll = true,
                    connectionStatuses = checkingMap
                )
            }

            val batch = exchangePingService.pingAllExchanges()
            _exchangeConfigState.update { st ->
                val updatedHistory = (batch.logEntries + st.pingHistory).take(12)
                st.copy(
                    isVerifyingAll = false,
                    connectionStatuses = st.connectionStatuses + batch.statuses,
                    connectionTestResults = st.connectionTestResults + batch.statuses
                        .filterValues { it.state != ExchangeHealthState.NOT_CONFIGURED }
                        .mapValues { it.value.detail },
                    pingHistory = updatedHistory,
                    statusMessage = batch.summaryMessage
                )
            }
        }
    }

    fun clearExchangeCredentials(exchangeId: String) {
        viewModelScope.launch {
            val id = exchangeId.lowercase()
            credentialStore.clearExchange(id)
            _exchangeConfigState.update { st ->
                st.copy(
                    connectionTestResults = st.connectionTestResults - id,
                    connectionStatuses = st.connectionStatuses + (
                        id to ExchangeConnectionStatus(
                            exchangeId = id,
                            state = ExchangeHealthState.NOT_CONFIGURED,
                            detail = "Awaiting encrypted API keys",
                            lastCheckedMs = System.currentTimeMillis()
                        )
                    ),
                    statusMessage = "Cleared ${id.uppercase()} encrypted credentials."
                )
            }
        }
    }

    fun clearAllExchangeCredentials() {
        viewModelScope.launch {
            credentialStore.clearAll()
            repository.logMemory("VAULT_API_WIPED", "SECURITY", "All locally encrypted exchange API keys wiped by operator.")
            _exchangeConfigState.update {
                it.copy(
                    connectionTestResults = emptyMap(),
                    connectionStatuses = mapOf(
                        "kraken" to ExchangeConnectionStatus("kraken", ExchangeHealthState.NOT_CONFIGURED, detail = "Awaiting encrypted API keys"),
                        "coinbase" to ExchangeConnectionStatus("coinbase", ExchangeHealthState.NOT_CONFIGURED, detail = "Awaiting encrypted API keys"),
                        "kucoin" to ExchangeConnectionStatus("kucoin", ExchangeHealthState.NOT_CONFIGURED, detail = "Awaiting encrypted API keys"),
                        "bitstamp" to ExchangeConnectionStatus("bitstamp", ExchangeHealthState.NOT_CONFIGURED, detail = "Awaiting encrypted API keys")
                    ),
                    statusMessage = "All local encrypted exchange keys wiped."
                )
            }
        }
    }

    /**
     * Continuously streams live spot quotes, active OHLC candles, historical charts,
     * Crypto Fear & Greed Index, full-exchange Adaptive Trader profit scans,
     * cross-exchange arbitrage spreads, and breakout radar telemetry automatically.
     */
    private fun startAutoLiveTelemetryLoop() {
        viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(15_000L)
                tick++
                refreshMarkets()
                refreshPriceSummaryDashboard(silent = true)
                silentRefreshTradeCandles()
                silentRefreshAssetHistory()
                if (tick % 2 == 0) {
                    scanArbitrage()
                    scanBreakouts()
                }
                if (tick % 4 == 0) {
                    refreshFearAndGreed()
                    scanEntireExchangeForProfit(autoApplyBest = _vaultState.value.account.autopilotRunning)
                }
            }
        }
    }

    fun selectPriceSummaryExchangeFilter(exchangeId: String) {
        _marketsState.update { it.copy(priceSummaryExchangeFilter = exchangeId.uppercase()) }
    }

    fun refreshPriceSummaryDashboard(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _marketsState.update { it.copy(isRefreshingPriceSummary = true, priceSummaryError = null) }
            }
            val creds = credentialStore.getCredentials()
            val configured = buildList {
                if (creds.hasKraken || isRealSecretValue(com.example.BuildConfig.KRAKEN_API_KEY, com.example.BuildConfig.KRAKEN_API_SECRET)) add("kraken")
                if (creds.hasCoinbase || isRealSecretValue(com.example.BuildConfig.COINBASE_API_KEY, com.example.BuildConfig.COINBASE_API_SECRET)) add("coinbase")
                if (creds.hasKuCoin || isRealSecretValue(com.example.BuildConfig.KUCOIN_API_KEY, com.example.BuildConfig.KUCOIN_API_SECRET, com.example.BuildConfig.KUCOIN_API_PASSPHRASE)) add("kucoin")
                if (creds.hasBitstamp || isRealSecretValue(com.example.BuildConfig.BITSTAMP_API_KEY, com.example.BuildConfig.BITSTAMP_API_SECRET)) add("bitstamp")
            }
            val targetExchanges = if (configured.isNotEmpty()) configured else listOf("kraken", "coinbase", "kucoin")
            apiClient.fetchPrimaryPairsForExchanges(
                exchangeIds = targetExchanges,
                configuredExchangeIds = configured.toSet()
            ).onSuccess { rows ->
                _marketsState.update { st ->
                    val currentFilter = st.priceSummaryExchangeFilter
                    val validFilter = if (currentFilter == "ALL" || rows.any { it.exchangeId.equals(currentFilter, ignoreCase = true) }) {
                        currentFilter
                    } else {
                        "ALL"
                    }
                    st.copy(
                        priceSummaryRows = rows,
                        configuredSummaryExchanges = configured,
                        isRefreshingPriceSummary = false,
                        priceSummaryUpdatedMs = System.currentTimeMillis(),
                        priceSummaryExchangeFilter = validFilter,
                        priceSummaryError = null
                    )
                }
            }.onFailure { err ->
                _marketsState.update { st ->
                    st.copy(
                        configuredSummaryExchanges = configured,
                        isRefreshingPriceSummary = false,
                        priceSummaryError = err.message ?: "Unable to fetch live exchange prices"
                    )
                }
            }
        }
    }

    private fun isRealSecretValue(vararg values: String): Boolean =
        values.all { v ->
            val t = v.trim()
            t.isNotBlank() && !t.endsWith("_DEFAULT_VALUE")
        }

    fun refreshFxRate() {
        viewModelScope.launch {
            apiClient.fetchUsdToCadRate().onSuccess { rate ->
                _tradeState.update { it.copy(usdToCadRate = rate) }
            }
        }
    }

    fun selectBaseCurrency(currency: BaseFiatCurrency) {
        _tradeState.update {
            it.copy(
                baseCurrency = currency,
                statusBanner = "Adaptive Trader funding currency set to ${currency.displayLabel} (1 USD = ${"%.3f".format(it.usdToCadRate)} CAD)."
            )
        }
        refreshFxRate()
    }

    fun selectConnectedExchange(exchange: ConnectedExchange) {
        _tradeState.update { it.copy(connectedExchange = exchange) }
        scanEntireExchangeForProfit(autoApplyBest = false)
    }

    fun applyAdaptiveOpportunity(opp: AdaptiveOpportunity) {
        loadTradeContext(opp.symbol, _tradeState.value.timeframe, opp.recommendedStrategy)
        _tradeState.update {
            it.copy(
                statusBanner = "Adaptive target locked: ${opp.symbol} on ${opp.exchangeName} · ${opp.recommendedStrategy.title} (Score ${opp.profitScore.toInt()}, Backtest ${Formatters.formatPct(opp.backtestReturnPct)})."
            )
        }
    }

    /**
     * Scans the entire connected exchange(s) for all liquid coins, fetches OHLC history for the
     * top movers, evaluates ALL 11 quant strategies on each coin, and ranks every coin+strategy
     * combination by profit-optimization score.
     */
    fun scanEntireExchangeForProfit(autoApplyBest: Boolean = false) {
        viewModelScope.launch {
            val currentTrade = _tradeState.value
            if (currentTrade.isScanningExchange) return@launch
            _tradeState.update { it.copy(isScanningExchange = true) }

            val scanResult = apiClient.scanEntireExchangeMarket(
                exchange = currentTrade.connectedExchange,
                usdToCadRate = currentTrade.usdToCadRate
            )

            scanResult.onSuccess { res ->
                // Evaluate top 8 discovered coins across the entire connected exchange concurrently
                val topCandidates = res.candidates.take(8)
                val evaluated = topCandidates.map { ticker ->
                    async {
                        val candles = apiClient.fetchCandles(ticker.symbol, currentTrade.timeframe)
                            .getOrNull()
                            ?.takeIf { it.size >= 35 }
                            ?: return@async null

                        // Evaluate all 11 strategies to find the profit-maximizing strategy for this coin
                        var bestStrat = StrategyType.TREND_CROSS
                        var bestSignal = TechnicalEngine.evaluateStrategy(candles, bestStrat)
                        var bestBacktest = TechnicalEngine.runBacktest(
                            symbol = ticker.symbol,
                            candles = candles,
                            strategy = bestStrat,
                            timeframe = currentTrade.timeframe,
                            feeBps = currentTrade.feeBps,
                            slippageBps = currentTrade.slippageBps
                        )
                        var bestScore = Double.NEGATIVE_INFINITY

                        StrategyType.entries.forEach { strat ->
                            val sig = TechnicalEngine.evaluateStrategy(candles, strat)
                            val bt = TechnicalEngine.runBacktest(
                                symbol = ticker.symbol,
                                candles = candles,
                                strategy = strat,
                                timeframe = currentTrade.timeframe,
                                feeBps = currentTrade.feeBps,
                                slippageBps = currentTrade.slippageBps
                            )
                            val actionBonus = when (sig.action) {
                                SignalAction.BUY -> 16.0 + sig.confidence * 14.0
                                SignalAction.SELL -> 6.0 + sig.confidence * 8.0
                                SignalAction.HOLD -> 0.0
                            }
                            val stratScore = (bt.totalReturnPct * 1.8) +
                                (bt.winRatePct * 0.35) -
                                (bt.maxDrawdownPct * 0.9) +
                                actionBonus +
                                (ticker.change24hPct.coerceIn(-10.0, 20.0) * 0.45)

                            if (stratScore > bestScore) {
                                bestScore = stratScore
                                bestStrat = strat
                                bestSignal = sig
                                bestBacktest = bt
                            }
                        }

                        val normalizedScore = (50.0 + bestScore * 0.65).coerceIn(10.0, 99.0)
                        AdaptiveOpportunity(
                            symbol = ticker.symbol,
                            exchangeName = ticker.exchangeName,
                            pairLabel = ticker.pairLabel,
                            priceUsd = ticker.priceUsd,
                            change24hPct = ticker.change24hPct,
                            volume24hUsd = ticker.volume24hUsd,
                            recommendedStrategy = bestStrat,
                            signalAction = bestSignal.action,
                            signalConfidence = bestSignal.confidence,
                            backtestReturnPct = bestBacktest.totalReturnPct,
                            winRatePct = bestBacktest.winRatePct,
                            maxDrawdownPct = bestBacktest.maxDrawdownPct,
                            profitScore = normalizedScore,
                            reason = bestSignal.reason
                        )
                    }
                }.awaitAll().filterNotNull().sortedByDescending { it.profitScore }

                val topPick = evaluated.firstOrNull()
                val venuesLabel = res.venuesScanned.joinToString(" · ")
                val summary = if (topPick != null) {
                    "Scanned ${res.totalMarketsScanned} markets across $venuesLabel -> Top profit pick: ${topPick.symbol} (${topPick.recommendedStrategy.title}, ${Formatters.formatPct(topPick.backtestReturnPct)} backtest)"
                } else {
                    "Scanned ${res.totalMarketsScanned} markets across $venuesLabel"
                }

                _tradeState.update {
                    it.copy(
                        isScanningExchange = false,
                        totalExchangeMarketsScanned = res.totalMarketsScanned,
                        venuesScannedLabel = venuesLabel,
                        adaptiveOpportunities = evaluated,
                        exchangeScanSummary = summary,
                        lastExchangeScanMs = System.currentTimeMillis()
                    )
                }

                if (autoApplyBest && topPick != null) {
                    loadTradeContext(topPick.symbol, currentTrade.timeframe, topPick.recommendedStrategy)
                }
            }.onFailure { err ->
                _tradeState.update {
                    it.copy(
                        isScanningExchange = false,
                        exchangeScanSummary = err.localizedMessage ?: "Exchange scan failed — check connection"
                    )
                }
            }
        }
    }

    private fun silentRefreshAssetHistory() {
        val current = _marketsState.value
        val sym = current.selectedHistorySymbol
        val win = current.historyWindow
        viewModelScope.launch {
            apiClient.fetchAssetHistory(sym, win).onSuccess { pts ->
                if (_marketsState.value.selectedHistorySymbol == sym &&
                    _marketsState.value.historyWindow == win
                ) {
                    _marketsState.update { it.copy(historyState = UiState.Success(pts)) }
                }
            }
        }
    }

    private fun silentRefreshTradeCandles() {
        val current = _tradeState.value
        viewModelScope.launch {
            apiClient.fetchCandles(current.symbol, current.timeframe).onSuccess { candles ->
                if (_tradeState.value.symbol == current.symbol &&
                    _tradeState.value.timeframe == current.timeframe
                ) {
                    cachedCandles = candles
                    _tradeState.update { it.copy(candlesState = UiState.Success(candles)) }
                    recomputeSignalAndBacktest(_tradeState.value.strategy)
                }
            }
        }
    }

    private fun observeVaultStreams() {
        viewModelScope.launch {
            val coreFlow = combine(
                repository.account,
                repository.holdings,
                repository.tradeLog,
                repository.agentMemory,
                repository.watchlist
            ) { acct, holdings, logs, mem, watch ->
                CoreVaultTuple(acct, holdings, logs, mem, watch)
            }
            val marketArbFlow = combine(
                repository.watchWallets,
                repository.arbitrageInventory,
                repository.arbitrageTrades,
                _marketsState
            ) { wallets, arbInv, arbTrades, mkts ->
                MarketArbTuple(wallets, arbInv, arbTrades, mkts)
            }
            val alertsFlow = combine(
                repository.priceTriggers,
                repository.payoutRule
            ) { triggers, rule ->
                Pair(triggers, rule ?: PayoutRuleEntity())
            }

            combine(coreFlow, marketArbFlow, alertsFlow) { core, marketArb, alertsPair ->
                val account = core.account ?: com.example.data.local.PaperAccountEntity()
                val (triggers, payoutRule) = alertsPair

                val tickerMap = (marketArb.markets.tickersState as? UiState.Success)?.data
                    ?.associateBy { it.symbol } ?: emptyMap()

                val nowMs = System.currentTimeMillis()
                val cutoff24hMs = nowMs - 86_400_000L

                var holdingsValue = 0.0
                var unrealizedPnl = 0.0
                var holdingsDailyDelta = 0.0

                core.holdings.forEach { h ->
                    val ticker = tickerMap[h.symbol]
                    val px = ticker?.priceUsd ?: h.avgEntryPrice
                    holdingsValue += h.quantity * px
                    unrealizedPnl += h.quantity * (px - h.avgEntryPrice)

                    holdingsDailyDelta += if (h.updatedAtMs < cutoff24hMs && ticker != null) {
                        val factor = 1.0 + (ticker.change24hPct / 100.0)
                        val px24hAgo = if (factor > 0.05) px / factor else px
                        h.quantity * (px - px24hAgo)
                    } else {
                        h.quantity * (px - h.avgEntryPrice)
                    }
                }

                val realizedTrades24h = core.tradeLog
                    .filter { it.timestampMs >= cutoff24hMs && it.side.equals("SELL", ignoreCase = true) }
                    .sumOf { it.pnlUsd }
                val realizedArb24h = marketArb.arbTrades
                    .filter { it.timestampMs >= cutoff24hMs }
                    .sumOf { it.worstCaseNetUsd }
                val realizedToday = realizedTrades24h + realizedArb24h

                val dailyPnlUsd = holdingsDailyDelta + realizedToday
                val totalEq = account.usdBalance + holdingsValue
                val priorBase = (totalEq - holdingsDailyDelta - realizedTrades24h).coerceAtLeast(1.0)
                val dailyPnlPct = (dailyPnlUsd / priorBase) * 100.0
                val totalReturnUsd = totalEq - 10_000.0

                val peak = max(account.peakEquityUsd, totalEq)
                val ddPct = if (peak > 0.0) ((peak - totalEq) / peak) * 100.0 else 0.0

                VaultUiState(
                    account = account,
                    holdings = core.holdings,
                    tradeLog = core.tradeLog,
                    agentMemory = core.agentMemory,
                    watchlist = core.watchlist,
                    watchWallets = marketArb.wallets,
                    arbitrageInventory = marketArb.arbInventory,
                    arbitrageTrades = marketArb.arbTrades,
                    priceTriggers = triggers,
                    payoutRule = payoutRule,
                    totalPortfolioEquityUsd = totalEq,
                    holdingsValueUsd = holdingsValue,
                    dailyPnlUsd = dailyPnlUsd,
                    dailyPnlPct = dailyPnlPct,
                    unrealizedPnlUsd = unrealizedPnl,
                    realizedTodayPnlUsd = realizedToday,
                    totalReturnUsd = totalReturnUsd,
                    currentDrawdownPct = ddPct
                )
            }.collect { newState ->
                _vaultState.value = newState
            }
        }
    }

    private data class CoreVaultTuple(
        val account: com.example.data.local.PaperAccountEntity?,
        val holdings: List<com.example.data.local.PaperHoldingEntity>,
        val tradeLog: List<com.example.data.local.TradeLogEntity>,
        val agentMemory: List<com.example.data.local.AgentMemoryEntity>,
        val watchlist: List<com.example.data.local.ResearchWatchlistEntity>
    )

    private data class MarketArbTuple(
        val wallets: List<com.example.data.local.WatchWalletEntity>,
        val arbInventory: List<com.example.data.local.ArbitrageInventoryEntity>,
        val arbTrades: List<com.example.data.local.ArbitrageTradeEntity>,
        val markets: MarketsUiState
    )

    fun refreshMarkets() {
        viewModelScope.launch {
            val hasData = _marketsState.value.tickersState is UiState.Success
            _marketsState.update {
                if (hasData) it.copy(isRefreshing = true)
                else it.copy(tickersState = UiState.Loading, isRefreshing = true)
            }
            val result = apiClient.fetchMarketTickers()
            result.onSuccess { list ->
                val adv = list.count { it.change24hPct >= 0.0 }
                val dec = list.size - adv
                val avg = if (list.isNotEmpty()) list.map { it.change24hPct }.average() else 0.0
                val vol = list.sumOf { it.volume24hUsd }
                val breadth = MarketBreadth(
                    totalAssets = list.size,
                    advancing = adv,
                    declining = dec,
                    avgChangePct = avg,
                    totalVolume24hUsd = vol,
                    topGainer = list.maxByOrNull { it.change24hPct },
                    topLoser = list.minByOrNull { it.change24hPct }
                )
                _marketsState.update { state ->
                    val updatedFng = (state.fearGreedState as? UiState.Success)?.data?.let { base ->
                        UiState.Success(blendFearGreedWithLivePulse(base, breadth, list))
                    } ?: state.fearGreedState
                    state.copy(
                        tickersState = UiState.Success(list),
                        isRefreshing = false,
                        breadth = breadth,
                        lastUpdatedMs = System.currentTimeMillis(),
                        fearGreedState = updatedFng
                    )
                }
                if (_labsState.value.researchProjection is UiState.Loading) {
                    analyzeResearchCoin(_labsState.value.researchSymbol)
                }
            }.onFailure { err ->
                _marketsState.update {
                    if (hasData) {
                        it.copy(isRefreshing = false)
                    } else {
                        it.copy(
                            tickersState = UiState.Error(err.localizedMessage ?: "Failed to load market feeds"),
                            isRefreshing = false
                        )
                    }
                }
            }
        }
    }

    fun refreshFearAndGreed() {
        viewModelScope.launch {
            val hasData = _marketsState.value.fearGreedState is UiState.Success
            _marketsState.update {
                if (hasData) it.copy(isRefreshingFearGreed = true)
                else it.copy(fearGreedState = UiState.Loading, isRefreshingFearGreed = true)
            }
            apiClient.fetchFearAndGreedIndex()
                .onSuccess { raw ->
                    val current = _marketsState.value
                    val tickers = (current.tickersState as? UiState.Success)?.data.orEmpty()
                    val blended = blendFearGreedWithLivePulse(raw, current.breadth, tickers)
                    _marketsState.update {
                        it.copy(
                            fearGreedState = UiState.Success(blended),
                            isRefreshingFearGreed = false
                        )
                    }
                }
                .onFailure { err ->
                    _marketsState.update {
                        if (hasData) {
                            it.copy(isRefreshingFearGreed = false)
                        } else {
                            it.copy(
                                fearGreedState = UiState.Error(
                                    err.localizedMessage ?: "Unable to fetch Crypto Fear & Greed API"
                                ),
                                isRefreshingFearGreed = false
                            )
                        }
                    }
                }
        }
    }

    /**
     * Combines the official external Fear & Greed API reading (75% weight) with real-time
     * market breadth, average 24h momentum, and intraday range volatility (25% weight)
     * so the index updates dynamically on every live market refresh.
     */
    private fun blendFearGreedWithLivePulse(
        base: FearGreedIndexData,
        breadth: MarketBreadth?,
        tickers: List<MarketTicker>
    ): FearGreedIndexData {
        if (breadth == null || breadth.totalAssets == 0 || tickers.isEmpty()) {
            return base
        }
        val advRatio = (breadth.advancing.toDouble() / breadth.totalAssets.toDouble()) * 100.0
        val momentumBoost = (breadth.avgChangePct * 4.5).coerceIn(-25.0, 25.0)
        val avgRangePct = tickers.mapNotNull { t ->
            if (t.low24h > 0.0) ((t.high24h - t.low24h) / t.low24h) * 100.0 else null
        }.average().takeIf { !it.isNaN() } ?: 4.0
        // Elevated volatility dampens greed slightly (uncertainty)
        val volatilityPenalty = ((avgRangePct - 6.0).coerceAtLeast(0.0) * 1.4).coerceAtMost(12.0)

        val livePulseScore = (advRatio * 0.65 + 20.0 + momentumBoost - volatilityPenalty).coerceIn(0.0, 100.0)
        val blended = (base.apiValue * 0.75 + livePulseScore * 0.25).toInt().coerceIn(0, 100)
        val delta = blended - base.apiValue
        return base.copy(
            liveValue = blended,
            liveClassification = MarketApiClient.classifyFearGreed(blended),
            liveMomentumAdjustment = delta,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    fun updateMarketSearch(query: String) {
        _marketsState.update { it.copy(searchQuery = query) }
    }

    fun selectHistoryAsset(symbol: String) {
        loadAssetHistory(symbol, _marketsState.value.historyWindow)
    }

    fun selectHistoryWindow(window: HistoryWindow) {
        loadAssetHistory(_marketsState.value.selectedHistorySymbol, window)
    }

    fun refreshAssetHistory() {
        val current = _marketsState.value
        loadAssetHistory(current.selectedHistorySymbol, current.historyWindow)
    }

    private fun loadAssetHistory(symbol: String, window: HistoryWindow) {
        _marketsState.update {
            it.copy(
                selectedHistorySymbol = symbol,
                historyWindow = window,
                historyState = UiState.Loading
            )
        }
        viewModelScope.launch {
            apiClient.fetchAssetHistory(symbol, window)
                .onSuccess { pts ->
                    if (_marketsState.value.selectedHistorySymbol == symbol &&
                        _marketsState.value.historyWindow == window
                    ) {
                        _marketsState.update { it.copy(historyState = UiState.Success(pts)) }
                    }
                }
                .onFailure { err ->
                    if (_marketsState.value.selectedHistorySymbol == symbol &&
                        _marketsState.value.historyWindow == window
                    ) {
                        _marketsState.update {
                            it.copy(historyState = UiState.Error(err.localizedMessage ?: "Historical feed unavailable"))
                        }
                    }
                }
        }
    }

    fun refreshNews() {
        viewModelScope.launch {
            _marketsState.update { it.copy(newsState = UiState.Loading) }
            apiClient.fetchCoinDeskNews()
                .onSuccess { items ->
                    _marketsState.update { it.copy(newsState = UiState.Success(items)) }
                }
                .onFailure { err ->
                    _marketsState.update {
                        it.copy(newsState = UiState.Error(err.localizedMessage ?: "RSS feed unavailable"))
                    }
                }
        }
    }

    fun selectTradeSymbol(symbol: String) {
        val current = _tradeState.value
        loadTradeContext(symbol, current.timeframe, current.strategy)
        viewModelScope.launch { repository.updateAccountConfig(selectedSymbol = symbol) }
    }

    fun selectTimeframe(tf: ChartTimeframe) {
        val current = _tradeState.value
        loadTradeContext(current.symbol, tf, current.strategy)
    }

    fun selectOverlay(overlay: ChartOverlay) {
        _tradeState.update { it.copy(overlay = overlay) }
    }

    fun selectStrategy(strategy: StrategyType) {
        _tradeState.update { it.copy(strategy = strategy) }
        viewModelScope.launch { repository.updateAccountConfig(selectedStrategyId = strategy.id) }
        recomputeSignalAndBacktest(strategy)
    }

    fun loadTradeContext(symbol: String, timeframe: ChartTimeframe, strategy: StrategyType) {
        viewModelScope.launch {
            _tradeState.update {
                it.copy(
                    symbol = symbol,
                    timeframe = timeframe,
                    strategy = strategy,
                    candlesState = UiState.Loading,
                    statusBanner = null
                )
            }
            apiClient.fetchCandles(symbol, timeframe)
                .onSuccess { candles ->
                    cachedCandles = candles
                    _tradeState.update { it.copy(candlesState = UiState.Success(candles)) }
                    recomputeSignalAndBacktest(strategy)
                }
                .onFailure { err ->
                    _tradeState.update {
                        it.copy(candlesState = UiState.Error(err.localizedMessage ?: "Failed to load candles"))
                    }
                }
        }
    }

    private fun recomputeSignalAndBacktest(strategy: StrategyType) {
        val state = _tradeState.value
        if (cachedCandles.isEmpty()) return
        val signal = TechnicalEngine.evaluateStrategy(cachedCandles, strategy)
        val backtest = TechnicalEngine.runBacktest(
            symbol = state.symbol,
            candles = cachedCandles,
            strategy = strategy,
            timeframe = state.timeframe,
            feeBps = state.feeBps,
            slippageBps = state.slippageBps
        )
        val indicatorBacktest = TechnicalEngine.simulateIndicatorBacktest(
            symbol = state.symbol,
            candles = cachedCandles,
            timeframe = state.timeframe,
            config = state.indicatorBacktestConfig
        )
        _tradeState.update {
            it.copy(
                liveSignal = signal,
                backtestResult = backtest,
                indicatorBacktestResult = indicatorBacktest
            )
        }
    }

    fun updateIndicatorBacktestConfig(config: IndicatorBacktestConfig) {
        _tradeState.update { it.copy(indicatorBacktestConfig = config) }
        val state = _tradeState.value
        if (cachedCandles.isNotEmpty()) {
            val indicatorBacktest = TechnicalEngine.simulateIndicatorBacktest(
                symbol = state.symbol,
                candles = cachedCandles,
                timeframe = state.timeframe,
                config = config
            )
            _tradeState.update { it.copy(indicatorBacktestResult = indicatorBacktest) }
        }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        val creds = _exchangeConfigState.value.credentials
        _tradeState.update {
            it.copy(
                executionMode = mode,
                liveConnectors = liveTradingClient.getConnectorStatuses(creds),
                statusBanner = if (mode == ExecutionMode.LIVE) {
                    "LIVE MODE ARMED · Route: ${it.liveExecutionRoute.label}"
                } else {
                    "Switched back to Safe Paper Trading Mode."
                }
            )
        }
        viewModelScope.launch {
            repository.logMemory(
                eventType = if (mode == ExecutionMode.LIVE) "LIVE_MODE_ARMED" else "PAPER_MODE_RESTORED",
                symbol = _tradeState.value.symbol,
                summary = if (mode == ExecutionMode.LIVE) {
                    "Operator armed LIVE execution via ${_tradeState.value.liveExecutionRoute.label}."
                } else {
                    "Operator switched execution desk back to PAPER mode."
                }
            )
        }
    }

    fun selectLiveExecutionRoute(route: LiveExecutionRoute) {
        val creds = _exchangeConfigState.value.credentials
        val serverTarget = when (route) {
            LiveExecutionRoute.LG_OWNER_SERVER -> LiveServerTarget.LG_OWNER
            else -> LiveServerTarget.LG_PRIVATE
        }
        _tradeState.update {
            it.copy(
                liveExecutionRoute = route,
                activeLiveServerTarget = serverTarget,
                liveConnectors = liveTradingClient.getConnectorStatuses(creds),
                statusBanner = "Live route selected: ${route.label}"
            )
        }
    }

    fun selectLiveServerTarget(target: LiveServerTarget) {
        val route = when (target) {
            LiveServerTarget.LG_PRIVATE -> LiveExecutionRoute.LG_PRIVATE_SERVER
            LiveServerTarget.LG_OWNER -> LiveExecutionRoute.LG_OWNER_SERVER
        }
        _tradeState.update {
            it.copy(
                activeLiveServerTarget = target,
                liveExecutionRoute = route
            )
        }
        pingLiveServer(target)
    }

    fun pingLiveServers() {
        pingLiveServer(LiveServerTarget.LG_PRIVATE)
        pingLiveServer(LiveServerTarget.LG_OWNER)
    }

    fun pingLiveServer(target: LiveServerTarget) {
        viewModelScope.launch {
            _tradeState.update { state ->
                when (target) {
                    LiveServerTarget.LG_PRIVATE -> state.copy(
                        privateServerHealth = state.privateServerHealth.copy(
                            isChecking = true,
                            statusDetail = "Waking & pinging lg-digital-private.onrender.com..."
                        )
                    )
                    LiveServerTarget.LG_OWNER -> state.copy(
                        ownerServerHealth = state.ownerServerHealth.copy(
                            isChecking = true,
                            statusDetail = "Waking & pinging lg-digital-owner.onrender.com..."
                        )
                    )
                }
            }
            val creds = credentialStore.getCredentials()
            val result = liveTradingClient.checkServerHealth(target, creds)
            _tradeState.update { state ->
                when (target) {
                    LiveServerTarget.LG_PRIVATE -> state.copy(
                        privateServerHealth = result,
                        liveConnectors = liveTradingClient.getConnectorStatuses(creds)
                    )
                    LiveServerTarget.LG_OWNER -> state.copy(
                        ownerServerHealth = result,
                        liveConnectors = liveTradingClient.getConnectorStatuses(creds)
                    )
                }
            }
        }
    }

    fun submitPaperOrder(isBuy: Boolean, notionalUsd: Double) {
        viewModelScope.launch {
            val state = _tradeState.value
            val price = currentPriceFor(state.symbol)
                ?: cachedCandles.lastOrNull()?.close
                ?: 0.0

            if (state.executionMode == ExecutionMode.LIVE) {
                val acct = _vaultState.value.account
                val profile = RiskProfile.entries.firstOrNull { it.label == acct.riskProfile } ?: RiskProfile.MEDIUM
                if (notionalUsd > profile.maxOrderUsd) {
                    _tradeState.update {
                        it.copy(
                            statusBanner = "LIVE SAFETY BLOCK: Order $${"%.2f".format(notionalUsd)} exceeds ${profile.label} risk cap ($${profile.maxOrderUsd.toInt()})."
                        )
                    }
                    return@launch
                }
                _tradeState.update {
                    it.copy(
                        isSubmittingLiveOrder = true,
                        statusBanner = "Dispatching LIVE ${if (isBuy) "BUY" else "SELL"} ${state.symbol} via ${state.liveExecutionRoute.shortBadge}..."
                    )
                }
                val creds = credentialStore.getCredentials()
                val liveRes = liveTradingClient.executeLiveOrder(
                    route = state.liveExecutionRoute,
                    connectedExchange = state.connectedExchange,
                    symbol = state.symbol,
                    isBuy = isBuy,
                    notionalUsd = notionalUsd,
                    priceUsd = price,
                    strategyId = "manual_${state.strategy.id}",
                    stored = creds
                )
                liveRes.fold(
                    onSuccess = { receipt ->
                        val summary = "LIVE ${receipt.side} ${receipt.symbol} ($${"%.2f".format(receipt.notionalUsd)}) via ${receipt.route.shortBadge} · ID: ${receipt.exchangeOrderId} · ${receipt.statusText}"
                        repository.logMemory("LIVE_ORDER_${receipt.side}", receipt.symbol, summary)
                        _tradeState.update {
                            it.copy(
                                isSubmittingLiveOrder = false,
                                statusBanner = summary,
                                recentLiveReceipts = (listOf(receipt) + it.recentLiveReceipts).take(12)
                            )
                        }
                    },
                    onFailure = { err ->
                        val errMsg = err.message ?: "Live order rejected"
                        repository.logMemory("LIVE_ORDER_ERROR", state.symbol, errMsg)
                        _tradeState.update {
                            it.copy(
                                isSubmittingLiveOrder = false,
                                statusBanner = errMsg
                            )
                        }
                    }
                )
                return@launch
            }

            val res = if (isBuy) {
                repository.executePaperBuy(
                    symbol = state.symbol,
                    notionalUsd = notionalUsd,
                    priceUsd = price,
                    strategyId = "manual_${state.strategy.id}",
                    reason = "Manual paper desk order"
                )
            } else {
                repository.executePaperSell(
                    symbol = state.symbol,
                    notionalUsd = notionalUsd,
                    priceUsd = price,
                    strategyId = "manual_${state.strategy.id}",
                    reason = "Manual paper desk exit"
                )
            }
            _tradeState.update {
                it.copy(statusBanner = res.fold(onSuccess = { msg -> msg }, onFailure = { err -> err.message }))
            }
        }
    }

    fun toggleAutopilot(enabled: Boolean) {
        viewModelScope.launch {
            val modeLabel = if (_tradeState.value.executionMode == ExecutionMode.LIVE) {
                "LIVE mode (${_tradeState.value.liveExecutionRoute.shortBadge})"
            } else {
                "paper mode"
            }
            repository.updateAccountConfig(autopilotRunning = enabled)
            repository.logMemory(
                eventType = if (enabled) "AUTOPILOT_START" else "AUTOPILOT_PAUSE",
                symbol = _tradeState.value.symbol,
                summary = if (enabled) "Adaptive Autopilot armed in $modeLabel." else "Adaptive Autopilot paused by operator."
            )
        }
    }

    fun updateRiskConfig(profile: RiskProfile, stopSchedule: String, maxDrawdownPct: Double, htfConfirm: Boolean) {
        viewModelScope.launch {
            repository.updateAccountConfig(
                riskProfile = profile,
                stopSchedule = stopSchedule,
                maxDrawdownPct = maxDrawdownPct,
                htfConfirmation = htfConfirm
            )
            _tradeState.update { it.copy(statusBanner = "Saved risk profile ${profile.label}, guard ${"%.1f".format(maxDrawdownPct)}% DD.") }
        }
    }

    fun runAutopilotTick() {
        viewModelScope.launch {
            val acct = _vaultState.value.account
            val tradeSnap = _tradeState.value
            val fiat = tradeSnap.baseCurrency
            val fx = tradeSnap.usdToCadRate

            if (!acct.autopilotRunning) {
                _tradeState.update { it.copy(statusBanner = "Autopilot is paused. Tap START AUTOPILOT to arm whole-exchange trading.") }
                return@launch
            }
            if (TechnicalEngine.isInsideStopSchedule(acct.stopSchedule)) {
                repository.logMemory("STOP_SCHEDULE_GUARD", tradeSnap.symbol, "Tick blocked inside stop window (${acct.stopSchedule}).")
                _tradeState.update { it.copy(statusBanner = "Blocked: Inside stop-schedule (${acct.stopSchedule}).") }
                return@launch
            }
            if (_vaultState.value.currentDrawdownPct >= acct.maxDrawdownPct) {
                repository.updateAccountConfig(autopilotRunning = false)
                repository.logMemory("DRAWDOWN_LOCK", tradeSnap.symbol, "Auto-stopped at ${"%.2f".format(_vaultState.value.currentDrawdownPct)}% DD.")
                _tradeState.update { it.copy(statusBanner = "Auto-stopped: Drawdown exceeded ${acct.maxDrawdownPct}% limit.") }
                return@launch
            }

            val profile = RiskProfile.entries.firstOrNull { it.label == acct.riskProfile } ?: RiskProfile.MEDIUM
            if (acct.dailyNotionalUsedUsd >= profile.maxDailyNotionalUsd) {
                _tradeState.update {
                    it.copy(statusBanner = "Daily cap reached (${Formatters.formatFiat(profile.maxDailyNotionalUsd, fiat, fx)}).")
                }
                return@launch
            }

            // 1. Scan entire connected exchange if cache is empty or stale (> 90s)
            var opps = _tradeState.value.adaptiveOpportunities
            if (opps.isEmpty() || (System.currentTimeMillis() - _tradeState.value.lastExchangeScanMs) > 90_000L) {
                _tradeState.update {
                    it.copy(statusBanner = "Scanning ${tradeSnap.connectedExchange.displayName} (${fiat.code}) for optimal coin & strategy...")
                }
                val res = apiClient.scanEntireExchangeMarket(tradeSnap.connectedExchange, fx).getOrNull()
                if (res != null && res.candidates.isNotEmpty()) {
                    val evaluated = res.candidates.take(6).map { ticker ->
                        async {
                            val c = apiClient.fetchCandles(ticker.symbol, tradeSnap.timeframe).getOrNull()?.takeIf { it.size >= 35 }
                                ?: return@async null
                            var bestStrat = StrategyType.TREND_CROSS
                            var bestSig = TechnicalEngine.evaluateStrategy(c, bestStrat)
                            var bestBt = TechnicalEngine.runBacktest(ticker.symbol, c, bestStrat, tradeSnap.timeframe, tradeSnap.feeBps, tradeSnap.slippageBps)
                            var bestScore = Double.NEGATIVE_INFINITY
                            StrategyType.entries.forEach { strat ->
                                val sig = TechnicalEngine.evaluateStrategy(c, strat)
                                val bt = TechnicalEngine.runBacktest(ticker.symbol, c, strat, tradeSnap.timeframe, tradeSnap.feeBps, tradeSnap.slippageBps)
                                val actBonus = when (sig.action) {
                                    SignalAction.BUY -> 16.0 + sig.confidence * 14.0
                                    SignalAction.SELL -> 6.0 + sig.confidence * 8.0
                                    SignalAction.HOLD -> 0.0
                                }
                                val score = bt.totalReturnPct * 1.8 + bt.winRatePct * 0.35 - bt.maxDrawdownPct * 0.9 + actBonus
                                if (score > bestScore) {
                                    bestScore = score
                                    bestStrat = strat
                                    bestSig = sig
                                    bestBt = bt
                                }
                            }
                            AdaptiveOpportunity(
                                symbol = ticker.symbol,
                                exchangeName = ticker.exchangeName,
                                pairLabel = ticker.pairLabel,
                                priceUsd = ticker.priceUsd,
                                change24hPct = ticker.change24hPct,
                                volume24hUsd = ticker.volume24hUsd,
                                recommendedStrategy = bestStrat,
                                signalAction = bestSig.action,
                                signalConfidence = bestSig.confidence,
                                backtestReturnPct = bestBt.totalReturnPct,
                                winRatePct = bestBt.winRatePct,
                                maxDrawdownPct = bestBt.maxDrawdownPct,
                                profitScore = (50.0 + bestScore * 0.65).coerceIn(10.0, 99.0),
                                reason = bestSig.reason
                            )
                        }
                    }.awaitAll().filterNotNull().sortedByDescending { it.profitScore }
                    if (evaluated.isNotEmpty()) {
                        opps = evaluated
                        _tradeState.update {
                            it.copy(
                                totalExchangeMarketsScanned = res.totalMarketsScanned,
                                venuesScannedLabel = res.venuesScanned.joinToString(" · "),
                                adaptiveOpportunities = evaluated,
                                lastExchangeScanMs = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }

            // 2. Prioritize exiting open holdings on SELL, otherwise take the highest-profit BUY across the whole exchange
            val heldSymbols = _vaultState.value.holdings.map { it.symbol }.toSet()
            val sellHoldingOpp = opps.firstOrNull { it.symbol in heldSymbols && it.signalAction == SignalAction.SELL }
            val bestBuyOpp = opps.firstOrNull { it.signalAction == SignalAction.BUY }
            val chosenOpp = sellHoldingOpp ?: bestBuyOpp ?: opps.firstOrNull()

            val targetSymbol = chosenOpp?.symbol ?: tradeSnap.symbol
            val targetStrategy = chosenOpp?.recommendedStrategy ?: tradeSnap.strategy
            val targetExchange = chosenOpp?.exchangeName ?: tradeSnap.connectedExchange.shortLabel
            val fallbackSignal = TechnicalEngine.evaluateStrategy(cachedCandles, targetStrategy)
            val targetAction = chosenOpp?.signalAction ?: fallbackSignal.action
            val targetPrice = chosenOpp?.priceUsd ?: currentPriceFor(targetSymbol) ?: fallbackSignal.entryPrice
            val targetReason = chosenOpp?.let {
                "[${it.exchangeName} · ${fiat.code}] ${it.recommendedStrategy.title} (Score ${it.profitScore.toInt()}): ${it.reason}"
            } ?: fallbackSignal.reason

            if (chosenOpp != null && (targetSymbol != tradeSnap.symbol || targetStrategy != tradeSnap.strategy)) {
                loadTradeContext(targetSymbol, tradeSnap.timeframe, targetStrategy)
            }

            if (acct.htfConfirmation && targetAction != SignalAction.HOLD) {
                val h4 = apiClient.fetchCandles(targetSymbol, ChartTimeframe.H4).getOrNull()
                if (h4 != null && h4.size >= 30) {
                    val htfSig = TechnicalEngine.evaluateStrategy(h4, StrategyType.TREND_CROSS)
                    if (htfSig.action != SignalAction.HOLD && htfSig.action != targetAction) {
                        repository.logMemory("HTF_FILTER", targetSymbol, "Skipped $targetAction on $targetExchange against 4h trend (${htfSig.action}).")
                        _tradeState.update {
                            it.copy(statusBanner = "Skipped $targetAction on $targetSymbol ($targetExchange): 4h trend filter disagrees (${htfSig.action}).")
                        }
                        return@launch
                    }
                }
            }

            val msg = when (targetAction) {
                SignalAction.BUY -> {
                    val orderUsd = if (tradeSnap.executionMode == ExecutionMode.LIVE) {
                        profile.maxOrderUsd
                    } else {
                        minOf(profile.maxOrderUsd, acct.usdBalance)
                    }
                    if (orderUsd < 10.0) {
                        "Autopilot HOLD: Insufficient ${fiat.code} buying power."
                    } else if (tradeSnap.executionMode == ExecutionMode.LIVE) {
                        val creds = credentialStore.getCredentials()
                        liveTradingClient.executeLiveOrder(
                            route = tradeSnap.liveExecutionRoute,
                            connectedExchange = tradeSnap.connectedExchange,
                            symbol = targetSymbol,
                            isBuy = true,
                            notionalUsd = orderUsd,
                            priceUsd = targetPrice,
                            strategyId = "adaptive_${targetStrategy.id}",
                            stored = creds
                        ).fold(
                            onSuccess = { receipt ->
                                val txt = "LIVE Adaptive BUY [${receipt.route.shortBadge}]: ${Formatters.formatFiat(orderUsd, fiat, fx)} $targetSymbol · ID ${receipt.exchangeOrderId}"
                                repository.logMemory("LIVE_AUTOPILOT_BUY", targetSymbol, txt)
                                _tradeState.update { st -> st.copy(recentLiveReceipts = (listOf(receipt) + st.recentLiveReceipts).take(12)) }
                                txt
                            },
                            onFailure = { err ->
                                val errTxt = "LIVE Autopilot BUY note: ${err.message ?: "Rejected"}"
                                repository.logMemory("LIVE_AUTOPILOT_ERR", targetSymbol, errTxt)
                                errTxt
                            }
                        )
                    } else {
                        repository.executePaperBuy(targetSymbol, orderUsd, targetPrice, "adaptive_${targetStrategy.id}", targetReason)
                            .fold(
                                { "Adaptive BUY [$targetExchange · ${fiat.code}]: ${Formatters.formatFiat(orderUsd, fiat, fx)} $targetSymbol via ${targetStrategy.title}" },
                                { it.message ?: "Buy blocked" }
                            )
                    }
                }
                SignalAction.SELL -> {
                    if (tradeSnap.executionMode == ExecutionMode.LIVE) {
                        val orderUsd = profile.maxOrderUsd
                        val creds = credentialStore.getCredentials()
                        liveTradingClient.executeLiveOrder(
                            route = tradeSnap.liveExecutionRoute,
                            connectedExchange = tradeSnap.connectedExchange,
                            symbol = targetSymbol,
                            isBuy = false,
                            notionalUsd = orderUsd,
                            priceUsd = targetPrice,
                            strategyId = "adaptive_${targetStrategy.id}",
                            stored = creds
                        ).fold(
                            onSuccess = { receipt ->
                                val txt = "LIVE Adaptive SELL [${receipt.route.shortBadge}]: ${Formatters.formatFiat(orderUsd, fiat, fx)} $targetSymbol · ID ${receipt.exchangeOrderId}"
                                repository.logMemory("LIVE_AUTOPILOT_SELL", targetSymbol, txt)
                                _tradeState.update { st -> st.copy(recentLiveReceipts = (listOf(receipt) + st.recentLiveReceipts).take(12)) }
                                txt
                            },
                            onFailure = { err ->
                                val errTxt = "LIVE Autopilot SELL note: ${err.message ?: "Rejected"}"
                                repository.logMemory("LIVE_AUTOPILOT_ERR", targetSymbol, errTxt)
                                errTxt
                            }
                        )
                    } else {
                        val holding = _vaultState.value.holdings.firstOrNull { it.symbol == targetSymbol }
                        if (holding == null) {
                            "Autopilot HOLD: SELL signal on $targetSymbol ($targetExchange), no open holding."
                        } else {
                            val orderUsd = minOf(profile.maxOrderUsd, holding.quantity * targetPrice)
                            repository.executePaperSell(targetSymbol, orderUsd, targetPrice, "adaptive_${targetStrategy.id}", targetReason)
                                .fold(
                                    { "Adaptive SELL [$targetExchange · ${fiat.code}]: ${Formatters.formatFiat(orderUsd, fiat, fx)} $targetSymbol via ${targetStrategy.title}" },
                                    { it.message ?: "Sell blocked" }
                                )
                        }
                    }
                }
                SignalAction.HOLD -> {
                    val summary = "Scanned ${_tradeState.value.totalExchangeMarketsScanned} markets ($targetExchange) · Top coin $targetSymbol (${targetStrategy.title}) is HOLD."
                    repository.logMemory("AUTOPILOT_HOLD", targetSymbol, summary)
                    summary
                }
            }
            _tradeState.update { it.copy(statusBanner = msg) }
        }
    }

    // --- Arbitrage Actions ---

    fun updateArbitrageParams(symbol: String? = null, mode: ArbitrageMode? = null, notionalUsd: Double? = null, minSpreadBps: Double? = null, onlyQualified: Boolean? = null) {
        _arbitrageState.update {
            it.copy(
                symbol = symbol ?: it.symbol,
                mode = mode ?: it.mode,
                tradeNotionalUsd = notionalUsd ?: it.tradeNotionalUsd,
                minSpreadBps = minSpreadBps ?: it.minSpreadBps,
                onlyQualified = onlyQualified ?: it.onlyQualified,
                bannerMessage = null
            )
        }
        if (symbol != null || mode != null) scanArbitrage()
    }

    fun scanArbitrage() {
        viewModelScope.launch {
            val st = _arbitrageState.value
            _arbitrageState.update { it.copy(isScanning = true, error = null) }
            try {
                val books = if (st.mode == ArbitrageMode.DEMO) {
                    val refPx = currentPriceFor(st.symbol) ?: when (st.symbol) {
                        "BTC" -> 86500.0
                        "ETH" -> 3100.0
                        "SOL" -> 195.0
                        else -> 50.0
                    }
                    ArbitrageEngine.buildSyntheticDemoBooks(st.symbol, refPx)
                } else {
                    apiClient.fetchPublicOrderBooks(st.symbol)
                }

                if (books.size < 2) {
                    _arbitrageState.update {
                        it.copy(
                            isScanning = false,
                            opportunities = emptyList(),
                            activeVenuesCount = books.size,
                            error = "Fewer than 2 exchange order books responded for ${st.symbol}/USD. Try BTC, ETH, or SOL, or switch to Demo mode."
                        )
                    }
                    return@launch
                }

                val invList = _vaultState.value.arbitrageInventory
                val usdBal = invList.filter { it.asset == "USD" }.associate { it.venue to it.balance }
                val coinBal = invList.filter { it.asset != "USD" }.associate { it.venueAssetKey to it.balance }

                val results = ArbitrageEngine.scanBooks(
                    symbol = st.symbol,
                    books = books,
                    tradeNotionalUsd = st.tradeNotionalUsd,
                    venueUsdBalances = usdBal,
                    venueCoinBalances = coinBal,
                    minSpreadBps = st.minSpreadBps
                )
                _arbitrageState.update {
                    it.copy(
                        isScanning = false,
                        opportunities = results,
                        activeVenuesCount = books.size,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _arbitrageState.update {
                    it.copy(isScanning = false, error = e.localizedMessage ?: "Arbitrage scan failed")
                }
            }
        }
    }

    fun executeArbitrageOpportunity(opp: ArbitrageOpportunity) {
        viewModelScope.launch {
            val res = repository.executePaperArbitrage(opp)
            _arbitrageState.update {
                it.copy(bannerMessage = res.fold({ ok -> ok }, { err -> err.message ?: "Blocked" }))
            }
            scanArbitrage()
        }
    }

    fun resetArbitrageInventory() {
        viewModelScope.launch {
            repository.resetArbitrageInventory()
            _arbitrageState.update { it.copy(bannerMessage = "Venue paper inventories reset to starter balances.") }
            scanArbitrage()
        }
    }

    // --- Labs & Screeners ---

    fun scanBreakouts() {
        viewModelScope.launch {
            _labsState.update { it.copy(breakoutLoading = true, breakoutError = null) }
            try {
                val topSymbols = listOf("BTC", "ETH", "SOL", "XRP", "AVAX", "LINK", "ADA", "LTC")
                val pairs = topSymbols.map { sym ->
                    async {
                        val c = apiClient.fetchCandles(sym, ChartTimeframe.H1).getOrNull()
                        if (c != null) sym to c else null
                    }
                }.awaitAll().filterNotNull().toMap()
                val candidates = TechnicalEngine.findBreakouts(pairs)
                _labsState.update {
                    it.copy(breakoutCandidates = candidates, breakoutLoading = false)
                }
            } catch (e: Exception) {
                _labsState.update {
                    it.copy(breakoutLoading = false, breakoutError = e.localizedMessage ?: "Breakout scan failed")
                }
            }
        }
    }

    fun searchDexPairs(query: String) {
        viewModelScope.launch {
            _labsState.update { it.copy(dexQuery = query, dexResults = UiState.Loading) }
            apiClient.searchDexPairs(query)
                .onSuccess { pairs ->
                    _labsState.update { it.copy(dexResults = UiState.Success(pairs)) }
                }
                .onFailure { err ->
                    _labsState.update {
                        it.copy(dexResults = UiState.Error(err.localizedMessage ?: "DEX Screener query failed"))
                    }
                }
        }
    }

    fun analyzeResearchCoin(symbol: String) {
        viewModelScope.launch {
            _labsState.update { it.copy(researchSymbol = symbol, researchProjection = UiState.Loading) }
            apiClient.fetchCandles(symbol, ChartTimeframe.H1)
                .onSuccess { candles ->
                    val proj = TechnicalEngine.project24h(symbol, candles)
                    _labsState.update { it.copy(researchProjection = UiState.Success(proj)) }
                }
                .onFailure { err ->
                    _labsState.update {
                        it.copy(researchProjection = UiState.Error(err.localizedMessage ?: "Could not project $symbol"))
                    }
                }
        }
    }

    fun saveToWatchlist(symbol: String, projected24h: Double, regime: String, notes: String) {
        viewModelScope.launch {
            repository.saveWatchlistItem(symbol, projected24h, regime, notes)
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeWatchlistItem(symbol)
        }
    }

    fun loadMiningQuotes() {
        viewModelScope.launch {
            _labsState.update { it.copy(miningQuotes = UiState.Loading) }
            apiClient.fetchMiningQuotes()
                .onSuccess { list ->
                    _labsState.update { it.copy(miningQuotes = UiState.Success(list)) }
                }
                .onFailure { err ->
                    _labsState.update {
                        it.copy(miningQuotes = UiState.Error(err.localizedMessage ?: "CoinSpectrum feed unreachable"))
                    }
                }
        }
    }

    // --- Watch-only Wallets & Vault ---

    fun addWatchWallet(label: String, network: String, address: String) {
        if (label.isBlank() || address.isBlank()) return
        viewModelScope.launch {
            repository.addWatchWallet(label.trim(), network.trim(), address.trim())
        }
    }

    fun removeWatchWallet(id: Long) {
        viewModelScope.launch {
            repository.removeWatchWallet(id)
        }
    }

    fun resetPaperPortfolio() {
        viewModelScope.launch {
            repository.resetPaperPortfolio()
        }
    }

    fun savePriceTrigger(symbol: String, targetPriceUsd: Double, enabled: Boolean) {
        viewModelScope.launch {
            repository.upsertPriceTrigger(symbol, targetPriceUsd, enabled)
        }
    }

    fun deletePriceTrigger(symbol: String) {
        viewModelScope.launch {
            repository.deletePriceTrigger(symbol)
        }
    }

    fun savePayoutRule(rule: PayoutRuleEntity) {
        viewModelScope.launch {
            repository.savePayoutRule(rule)
        }
    }

    // --- AI Trading Advisor (Firebase AI Logic) ---

    fun sendAdvisorMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank() || _advisorState.value.isGenerating) return

        val currentHistory = _advisorState.value.messages
        val userMsg = AdvisorMessage(id = "u_${System.currentTimeMillis()}", isUser = true, text = clean)

        _advisorState.update {
            it.copy(
                messages = currentHistory + userMsg,
                streamingReply = "",
                isGenerating = true,
                errorMessage = null
            )
        }

        val deskContext = buildDeskContextSummary()

        viewModelScope.launch {
            var latestReply = ""
            aiRepository.streamAdvisorReply(currentHistory, clean, deskContext)
                .catch { err ->
                    val errText = err.localizedMessage ?: "AI request failed."
                    _advisorState.update {
                        it.copy(
                            isGenerating = false,
                            streamingReply = "",
                            errorMessage = errText,
                            messages = it.messages + AdvisorMessage(
                                id = "err_${System.currentTimeMillis()}",
                                isUser = false,
                                text = "Error: $errText",
                                isError = true
                            )
                        )
                    }
                }
                .collect { accumulated ->
                    latestReply = accumulated
                    _advisorState.update { it.copy(streamingReply = accumulated) }
                }

            if (latestReply.isNotBlank()) {
                _advisorState.update {
                    it.copy(
                        isGenerating = false,
                        streamingReply = "",
                        messages = it.messages + AdvisorMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            isUser = false,
                            text = latestReply
                        )
                    )
                }
            } else if (_advisorState.value.isGenerating) {
                _advisorState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun buildDeskContextSummary(): String {
        val trade = _tradeState.value
        val vault = _vaultState.value
        val breadth = _marketsState.value.breadth
        val sig = trade.liveSignal
        val bt = trade.backtestResult
        val holdingsLine = if (vault.holdings.isEmpty()) {
            "None (100% USD)"
        } else {
            vault.holdings.joinToString { "${it.symbol}: ${Formatters.formatQty(it.quantity)} @ ${Formatters.formatUsd(it.avgEntryPrice)}" }
        }
        return buildString {
            appendLine("Active Symbol: ${trade.symbol} (${trade.timeframe.label}), Strategy: ${trade.strategy.title}")
            if (sig != null) {
                appendLine("Live Signal: ${sig.action} (${(sig.confidence * 100).toInt()}% conf) @ ${Formatters.formatUsd(sig.entryPrice)} — ${sig.reason}")
            }
            if (bt != null) {
                appendLine("Backtest: Total ${Formatters.formatPct(bt.totalReturnPct)}, Val ${Formatters.formatPct(bt.validationReturnPct)}, WinRate ${"%.1f".format(bt.winRatePct)}%, MaxDD ${"%.2f".format(bt.maxDrawdownPct)}% (${bt.totalTrades} trades)")
            }
            if (breadth != null) {
                appendLine("Market Breadth: ${breadth.advancing}/${breadth.totalAssets} advancing, Avg 24h ${Formatters.formatPct(breadth.avgChangePct)}")
            }
            appendLine("Paper Account: USD=${Formatters.formatUsd(vault.account.usdBalance)}, Equity=${Formatters.formatUsd(vault.totalPortfolioEquityUsd)}, Drawdown=${"%.2f".format(vault.currentDrawdownPct)}%, Autopilot=${vault.account.autopilotRunning}, RiskProfile=${vault.account.riskProfile}")
            appendLine("Open Holdings: $holdingsLine")
        }
    }

    fun logSecurityEvent(eventType: String, summary: String) {
        viewModelScope.launch {
            repository.logMemory(eventType, "SECURITY", summary)
        }
    }

    private fun currentPriceFor(symbol: String): Double? {
        val tickers = (_marketsState.value.tickersState as? UiState.Success)?.data ?: return null
        return tickers.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }?.priceUsd
    }
}

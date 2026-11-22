package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.data.local.AppDatabase
import com.example.data.local.PayoutRuleEntity
import com.example.data.local.PriceTriggerEntity
import com.example.data.local.TradingRepository
import com.example.data.remote.AdvisorMessage
import com.example.data.remote.AiAdvisorRepository
import com.example.data.remote.MarketApiClient
import com.example.domain.engine.ArbitrageEngine
import com.example.domain.engine.TechnicalEngine
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.Candle
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.HistoryWindow
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.RiskProfile
import com.example.domain.models.SignalAction
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = TradingRepository(db.appDao())
    private val apiClient = MarketApiClient()
    private val aiRepository = AiAdvisorRepository()

    private val _marketsState = MutableStateFlow(MarketsUiState())
    val marketsState: StateFlow<MarketsUiState> = _marketsState.asStateFlow()

    private val _tradeState = MutableStateFlow(TradeUiState())
    val tradeState: StateFlow<TradeUiState> = _tradeState.asStateFlow()

    private val _arbitrageState = MutableStateFlow(ArbitrageUiState())
    val arbitrageState: StateFlow<ArbitrageUiState> = _arbitrageState.asStateFlow()

    private val _labsState = MutableStateFlow(LabsUiState())
    val labsState: StateFlow<LabsUiState> = _labsState.asStateFlow()

    private val _vaultState = MutableStateFlow(VaultUiState())
    val vaultState: StateFlow<VaultUiState> = _vaultState.asStateFlow()

    private val _advisorState = MutableStateFlow(AdvisorUiState())
    val advisorState: StateFlow<AdvisorUiState> = _advisorState.asStateFlow()

    private var cachedCandles: List<Candle> = emptyList()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
        observeVaultStreams()
        refreshMarkets()
        refreshNews()
        loadAssetHistory("BTC", HistoryWindow.H24)
        loadTradeContext("BTC", ChartTimeframe.H1, StrategyType.TREND_CROSS)
        scanArbitrage()
        searchDexPairs("SOL")
        loadMiningQuotes()
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
                _marketsState.update {
                    it.copy(
                        tickersState = UiState.Success(list),
                        isRefreshing = false,
                        breadth = breadth,
                        lastUpdatedMs = System.currentTimeMillis()
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

    fun submitPaperOrder(isBuy: Boolean, notionalUsd: Double) {
        viewModelScope.launch {
            val state = _tradeState.value
            val price = currentPriceFor(state.symbol)
                ?: cachedCandles.lastOrNull()?.close
                ?: 0.0
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
            repository.updateAccountConfig(autopilotRunning = enabled)
            repository.logMemory(
                eventType = if (enabled) "AUTOPILOT_START" else "AUTOPILOT_PAUSE",
                symbol = _tradeState.value.symbol,
                summary = if (enabled) "Adaptive Autopilot armed in paper mode." else "Adaptive Autopilot paused by operator."
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
            val symbol = _tradeState.value.symbol
            if (!acct.autopilotRunning) {
                _tradeState.update { it.copy(statusBanner = "Autopilot is paused. Toggle Armed to run a tick.") }
                return@launch
            }
            if (TechnicalEngine.isInsideStopSchedule(acct.stopSchedule)) {
                repository.logMemory("STOP_SCHEDULE_GUARD", symbol, "Tick blocked inside stop window (${acct.stopSchedule}).")
                _tradeState.update { it.copy(statusBanner = "Blocked: Inside stop-schedule (${acct.stopSchedule}).") }
                return@launch
            }
            if (_vaultState.value.currentDrawdownPct >= acct.maxDrawdownPct) {
                repository.updateAccountConfig(autopilotRunning = false)
                repository.logMemory("DRAWDOWN_LOCK", symbol, "Auto-stopped at ${"%.2f".format(_vaultState.value.currentDrawdownPct)}% DD.")
                _tradeState.update { it.copy(statusBanner = "Auto-stopped: Drawdown exceeded ${acct.maxDrawdownPct}% limit.") }
                return@launch
            }

            val profile = RiskProfile.entries.firstOrNull { it.label == acct.riskProfile } ?: RiskProfile.MEDIUM
            if (acct.dailyNotionalUsedUsd >= profile.maxDailyNotionalUsd) {
                _tradeState.update { it.copy(statusBanner = "Daily cap reached ($${"%.0f".format(profile.maxDailyNotionalUsd)}).") }
                return@launch
            }

            val candles = cachedCandles
            val signal = TechnicalEngine.evaluateStrategy(candles, _tradeState.value.strategy)

            if (acct.htfConfirmation && signal.action != SignalAction.HOLD) {
                val h4 = apiClient.fetchCandles(symbol, ChartTimeframe.H4).getOrNull()
                if (h4 != null && h4.size >= 30) {
                    val htfSig = TechnicalEngine.evaluateStrategy(h4, StrategyType.TREND_CROSS)
                    if (htfSig.action != SignalAction.HOLD && htfSig.action != signal.action) {
                        repository.logMemory("HTF_FILTER", symbol, "Skipped ${signal.action} against 4h trend (${htfSig.action}).")
                        _tradeState.update { it.copy(statusBanner = "Skipped ${signal.action}: 4h trend filter disagrees (${htfSig.action}).") }
                        return@launch
                    }
                }
            }

            val price = signal.entryPrice
            val msg = when (signal.action) {
                SignalAction.BUY -> {
                    val orderUsd = minOf(profile.maxOrderUsd, acct.usdBalance)
                    if (orderUsd < 10.0) {
                        "Autopilot HOLD: Insufficient paper USD."
                    } else {
                        repository.executePaperBuy(symbol, orderUsd, price, _tradeState.value.strategy.id, signal.reason)
                            .fold({ "Autopilot BUY: $it" }, { it.message ?: "Buy blocked" })
                    }
                }
                SignalAction.SELL -> {
                    val holding = _vaultState.value.holdings.firstOrNull { it.symbol == symbol }
                    if (holding == null) {
                        "Autopilot HOLD: SELL signal on $symbol, no open holding."
                    } else {
                        val orderUsd = minOf(profile.maxOrderUsd, holding.quantity * price)
                        repository.executePaperSell(symbol, orderUsd, price, _tradeState.value.strategy.id, signal.reason)
                            .fold({ "Autopilot SELL: $it" }, { it.message ?: "Sell blocked" })
                    }
                }
                SignalAction.HOLD -> {
                    repository.logMemory("AUTOPILOT_HOLD", symbol, "${_tradeState.value.strategy.title}: ${signal.reason}")
                    "Autopilot HOLD: ${signal.reason}"
                }
            }
            _tradeState.update { it.copy(liveSignal = signal, statusBanner = msg) }
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

    private fun currentPriceFor(symbol: String): Double? {
        val tickers = (_marketsState.value.tickersState as? UiState.Success)?.data ?: return null
        return tickers.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }?.priceUsd
    }
}

package com.example.data.local

import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.RiskProfile
import kotlinx.coroutines.flow.Flow

class TradingRepository(private val dao: AppDao) {

    val account: Flow<PaperAccountEntity?> = dao.observeAccount()
    val holdings: Flow<List<PaperHoldingEntity>> = dao.observeHoldings()
    val tradeLog: Flow<List<TradeLogEntity>> = dao.observeTradeLog()
    val agentMemory: Flow<List<AgentMemoryEntity>> = dao.observeAgentMemory()
    val watchlist: Flow<List<ResearchWatchlistEntity>> = dao.observeWatchlist()
    val watchWallets: Flow<List<WatchWalletEntity>> = dao.observeWatchWallets()
    val arbitrageInventory: Flow<List<ArbitrageInventoryEntity>> = dao.observeArbitrageInventory()
    val arbitrageTrades: Flow<List<ArbitrageTradeEntity>> = dao.observeArbitrageTrades()
    val priceTriggers: Flow<List<PriceTriggerEntity>> = dao.observePriceTriggers()
    val payoutRule: Flow<PayoutRuleEntity?> = dao.observePayoutRule()

    suspend fun ensureSeeded() {
        if (dao.getAccountOnce() == null) {
            dao.upsertAccount(PaperAccountEntity())
            dao.insertAgentMemory(
                AgentMemoryEntity(
                    eventType = "SYSTEM_INIT",
                    symbol = "BTC",
                    summary = "Paper trading vault initialized with $10,000.00 USD allocation."
                )
            )
        }
        if (dao.getArbitrageInventoryOnce().isEmpty()) {
            resetArbitrageInventory()
        }
        if (dao.getPayoutRuleOnce() == null) {
            dao.upsertPayoutRule(PayoutRuleEntity())
        }
        if (dao.getPriceTriggersOnce().isEmpty()) {
            dao.upsertPriceTrigger(PriceTriggerEntity("BTC", 92000.0, isAbove = true, enabled = true))
            dao.upsertPriceTrigger(PriceTriggerEntity("ETH", 3400.0, isAbove = true, enabled = true))
            dao.upsertPriceTrigger(PriceTriggerEntity("SOL", 220.0, isAbove = true, enabled = true))
        }
    }

    suspend fun resetArbitrageInventory() {
        dao.clearArbitrageInventory()
        val venues = listOf("Kraken", "Coinbase", "Bitstamp", "Crypto.com", "NDAX")
        val starter = mutableListOf<ArbitrageInventoryEntity>()
        for (v in venues) {
            starter += ArbitrageInventoryEntity("$v:USD", v, "USD", 2500.0)
            starter += ArbitrageInventoryEntity("$v:BTC", v, "BTC", 0.15)
            starter += ArbitrageInventoryEntity("$v:ETH", v, "ETH", 2.5)
            starter += ArbitrageInventoryEntity("$v:SOL", v, "SOL", 30.0)
            starter += ArbitrageInventoryEntity("$v:XRP", v, "XRP", 2500.0)
            starter += ArbitrageInventoryEntity("$v:LINK", v, "LINK", 150.0)
            starter += ArbitrageInventoryEntity("$v:AVAX", v, "AVAX", 80.0)
            starter += ArbitrageInventoryEntity("$v:ADA", v, "ADA", 3500.0)
            starter += ArbitrageInventoryEntity("$v:LTC", v, "LTC", 20.0)
            starter += ArbitrageInventoryEntity("$v:DOGE", v, "DOGE", 10000.0)
        }
        dao.upsertArbitrageInventory(starter)
    }

    suspend fun executePaperBuy(
        symbol: String,
        notionalUsd: Double,
        priceUsd: Double,
        strategyId: String,
        reason: String
    ): Result<String> {
        if (notionalUsd <= 0.0 || priceUsd <= 0.0) {
            return Result.failure(IllegalArgumentException("Order size and live price must be positive."))
        }
        val acct = dao.getAccountOnce() ?: PaperAccountEntity()
        if (acct.usdBalance < notionalUsd) {
            return Result.failure(IllegalStateException("Insufficient paper USD balance ($${"%.2f".format(acct.usdBalance)})."))
        }
        val qty = notionalUsd / priceUsd
        val existing = dao.getHolding(symbol)
        val newQty = (existing?.quantity ?: 0.0) + qty
        val newAvg = if (existing == null) {
            priceUsd
        } else {
            ((existing.quantity * existing.avgEntryPrice) + notionalUsd) / newQty
        }

        dao.upsertAccount(
            acct.copy(
                usdBalance = acct.usdBalance - notionalUsd,
                dailyNotionalUsedUsd = acct.dailyNotionalUsedUsd + notionalUsd
            )
        )
        dao.upsertHolding(PaperHoldingEntity(symbol, newQty, newAvg))
        dao.insertTradeLog(
            TradeLogEntity(
                symbol = symbol,
                side = "BUY",
                quantity = qty,
                priceUsd = priceUsd,
                notionalUsd = notionalUsd,
                strategyId = strategyId,
                reason = reason
            )
        )
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = "ORDER_FILL",
                symbol = symbol,
                summary = "Paper BUY ${"%.5f".format(qty)} $symbol @ $${"%.2f".format(priceUsd)} ($strategyId)"
            )
        )
        return Result.success("Bought ${"%.5f".format(qty)} $symbol @ $${"%.2f".format(priceUsd)}")
    }

    suspend fun executePaperSell(
        symbol: String,
        notionalUsd: Double,
        priceUsd: Double,
        strategyId: String,
        reason: String
    ): Result<String> {
        if (notionalUsd <= 0.0 || priceUsd <= 0.0) {
            return Result.failure(IllegalArgumentException("Order size and live price must be positive."))
        }
        val acct = dao.getAccountOnce() ?: PaperAccountEntity()
        val existing = dao.getHolding(symbol)
            ?: return Result.failure(IllegalStateException("No paper $symbol position to sell."))
        val reqQty = notionalUsd / priceUsd
        val sellQty = minOf(reqQty, existing.quantity)
        val proceeds = sellQty * priceUsd
        val pnl = (priceUsd - existing.avgEntryPrice) * sellQty
        val remaining = existing.quantity - sellQty

        if (remaining <= 0.0000001) {
            dao.deleteHolding(symbol)
        } else {
            dao.upsertHolding(existing.copy(quantity = remaining, updatedAtMs = System.currentTimeMillis()))
        }
        dao.upsertAccount(
            acct.copy(
                usdBalance = acct.usdBalance + proceeds,
                dailyNotionalUsedUsd = acct.dailyNotionalUsedUsd + proceeds
            )
        )
        dao.insertTradeLog(
            TradeLogEntity(
                symbol = symbol,
                side = "SELL",
                quantity = sellQty,
                priceUsd = priceUsd,
                notionalUsd = proceeds,
                strategyId = strategyId,
                reason = reason,
                pnlUsd = pnl
            )
        )
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = "ORDER_FILL",
                symbol = symbol,
                summary = "Paper SELL ${"%.5f".format(sellQty)} $symbol @ $${"%.2f".format(priceUsd)} (PnL $${"%.2f".format(pnl)})"
            )
        )
        return Result.success("Sold ${"%.5f".format(sellQty)} $symbol (PnL $${"%.2f".format(pnl)})")
    }

    suspend fun executePaperArbitrage(opp: ArbitrageOpportunity): Result<String> {
        if (!opp.qualified) {
            return Result.failure(IllegalStateException(opp.rejectionReasons.firstOrNull() ?: "Route blocked"))
        }
        val inv = dao.getArbitrageInventoryOnce().associateBy { it.venueAssetKey }.toMutableMap()
        val buyUsdKey = "${opp.buyVenue}:USD"
        val sellCoinKey = "${opp.sellVenue}:${opp.symbol}"
        val buyCoinKey = "${opp.buyVenue}:${opp.symbol}"
        val sellUsdKey = "${opp.sellVenue}:USD"

        val buyUsd = inv[buyUsdKey]?.balance ?: 0.0
        val sellCoin = inv[sellCoinKey]?.balance ?: 0.0
        val reqUsd = opp.notionalUsd.toDouble()
        val qty = opp.baseQuantity.toDouble()

        if (buyUsd < reqUsd) return Result.failure(IllegalStateException("Insufficient USD on ${opp.buyVenue}"))
        if (sellCoin < qty) return Result.failure(IllegalStateException("Insufficient ${opp.symbol} on ${opp.sellVenue}"))

        val sellProceeds = qty * opp.sellVwap.toDouble() - opp.takerFeesUsd.toDouble() - opp.slippageBufferUsd.toDouble() - opp.rebalanceReserveUsd.toDouble()

        inv[buyUsdKey] = ArbitrageInventoryEntity(buyUsdKey, opp.buyVenue, "USD", buyUsd - reqUsd)
        inv[buyCoinKey] = ArbitrageInventoryEntity(buyCoinKey, opp.buyVenue, opp.symbol, (inv[buyCoinKey]?.balance ?: 0.0) + qty)
        inv[sellCoinKey] = ArbitrageInventoryEntity(sellCoinKey, opp.sellVenue, opp.symbol, sellCoin - qty)
        inv[sellUsdKey] = ArbitrageInventoryEntity(sellUsdKey, opp.sellVenue, "USD", (inv[sellUsdKey]?.balance ?: 0.0) + sellProceeds)

        dao.upsertArbitrageInventory(inv.values.toList())
        dao.insertArbitrageTrade(
            ArbitrageTradeEntity(
                symbol = opp.symbol,
                buyVenue = opp.buyVenue,
                sellVenue = opp.sellVenue,
                baseQty = qty,
                notionalUsd = reqUsd,
                worstCaseNetUsd = opp.worstCaseNetUsd.toDouble(),
                grossSpreadBps = opp.grossSpreadBps,
                isSynthetic = opp.isSynthetic
            )
        )
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = "ARBITRAGE_FILL",
                symbol = opp.symbol,
                summary = "Arb ${opp.buyVenue} -> ${opp.sellVenue} ${opp.symbol} net $${opp.worstCaseNetUsd}"
            )
        )
        return Result.success("Executed ${opp.symbol} arb: +$${opp.worstCaseNetUsd} worst-case net")
    }

    suspend fun updateAccountConfig(
        autopilotRunning: Boolean? = null,
        riskProfile: RiskProfile? = null,
        stopSchedule: String? = null,
        maxDrawdownPct: Double? = null,
        htfConfirmation: Boolean? = null,
        selectedSymbol: String? = null,
        selectedStrategyId: String? = null,
        peakEquityUsd: Double? = null
    ) {
        val current = dao.getAccountOnce() ?: PaperAccountEntity()
        dao.upsertAccount(
            current.copy(
                autopilotRunning = autopilotRunning ?: current.autopilotRunning,
                riskProfile = riskProfile?.label ?: current.riskProfile,
                stopSchedule = stopSchedule ?: current.stopSchedule,
                maxDrawdownPct = maxDrawdownPct ?: current.maxDrawdownPct,
                htfConfirmation = htfConfirmation ?: current.htfConfirmation,
                selectedSymbol = selectedSymbol ?: current.selectedSymbol,
                selectedStrategyId = selectedStrategyId ?: current.selectedStrategyId,
                peakEquityUsd = peakEquityUsd ?: current.peakEquityUsd
            )
        )
    }

    suspend fun logMemory(eventType: String, symbol: String, summary: String) {
        dao.insertAgentMemory(AgentMemoryEntity(eventType = eventType, symbol = symbol, summary = summary))
    }

    suspend fun resetPaperPortfolio() {
        dao.clearHoldings()
        dao.clearTradeLog()
        dao.upsertAccount(PaperAccountEntity())
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = "VAULT_RESET",
                symbol = "USD",
                summary = "Paper portfolio reset to $10,000.00 USD."
            )
        )
    }

    suspend fun saveWatchlistItem(symbol: String, projected24hUsd: Double, regimeLabel: String, notes: String) {
        dao.upsertWatchlist(ResearchWatchlistEntity(symbol, projected24hUsd, regimeLabel, notes))
    }

    suspend fun removeWatchlistItem(symbol: String) {
        dao.deleteWatchlist(symbol)
    }

    suspend fun addWatchWallet(label: String, network: String, address: String) {
        dao.insertWatchWallet(WatchWalletEntity(label = label, network = network, address = address))
    }

    suspend fun removeWatchWallet(id: Long) {
        dao.deleteWatchWallet(id)
    }

    suspend fun upsertPriceTrigger(symbol: String, targetPriceUsd: Double, enabled: Boolean) {
        dao.upsertPriceTrigger(PriceTriggerEntity(symbol = symbol.uppercase(), targetPriceUsd = targetPriceUsd, enabled = enabled))
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = "PRICE_TRIGGER",
                symbol = symbol.uppercase(),
                summary = "Trigger ${if (enabled) "armed" else "paused"} for ${symbol.uppercase()} @ $${"%.2f".format(targetPriceUsd)}"
            )
        )
    }

    suspend fun deletePriceTrigger(symbol: String) {
        dao.deletePriceTrigger(symbol)
    }

    suspend fun savePayoutRule(rule: PayoutRuleEntity) {
        dao.upsertPayoutRule(rule)
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = "PAYOUT_RULE",
                symbol = rule.targetCoin,
                summary = "Saved auto-sweep $${"%.0f".format(rule.autoSweepUsd)} -> ${rule.targetCoin} (min $${"%.0f".format(rule.minPayoutThresholdUsd)})"
            )
        )
    }
}

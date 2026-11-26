package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.RiskProfile
import kotlinx.coroutines.flow.Flow
import kotlin.math.max

@Entity(tableName = "paper_account")
data class PaperAccountEntity(
    @PrimaryKey val id: Int = 1,
    val usdBalance: Double = 10_000.0,
    val peakEquityUsd: Double = 10_000.0,
    val dailyNotionalUsedUsd: Double = 0.0,
    val autopilotRunning: Boolean = false,
    val riskProfile: String = "MEDIUM",
    val stopSchedule: String = "",
    val maxDrawdownPct: Double = 8.0,
    val htfConfirmation: Boolean = true,
    val selectedSymbol: String = "BTC",
    val selectedStrategyId: String = "trend_cross"
)

@Entity(tableName = "paper_holdings")
data class PaperHoldingEntity(
    @PrimaryKey val symbol: String,
    val quantity: Double,
    val avgEntryPrice: Double,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "trade_log")
data class TradeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val symbol: String,
    val side: String,
    val quantity: Double,
    val priceUsd: Double,
    val notionalUsd: Double,
    val pnlUsd: Double = 0.0,
    val strategyId: String,
    val reason: String
)

@Entity(tableName = "agent_memory")
data class AgentMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val eventType: String,
    val symbol: String,
    val summary: String
)

@Entity(tableName = "research_watchlist")
data class ResearchWatchlistEntity(
    @PrimaryKey val symbol: String,
    val projected24hUsd: Double,
    val regimeLabel: String,
    val notes: String,
    val savedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_wallets")
data class WatchWalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val network: String,
    val address: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "arbitrage_inventory")
data class ArbitrageInventoryEntity(
    @PrimaryKey val venueAssetKey: String,
    val venue: String,
    val asset: String,
    val balance: Double
)

@Entity(tableName = "arbitrage_trades")
data class ArbitrageTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val symbol: String,
    val buyVenue: String,
    val sellVenue: String,
    val notionalUsd: Double,
    val grossSpreadBps: Double,
    val worstCaseNetUsd: Double,
    val worstCaseNetBps: Double
)

@Entity(tableName = "price_triggers")
data class PriceTriggerEntity(
    @PrimaryKey val symbol: String,
    val targetPriceUsd: Double,
    val enabled: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "payout_rules")
data class PayoutRuleEntity(
    @PrimaryKey val id: Int = 1,
    val pushAlertsEnabled: Boolean = true,
    val emailAlertsEnabled: Boolean = false,
    val webhookEnabled: Boolean = false,
    val autoSweepUsd: Double = 500.0,
    val minPayoutThresholdUsd: Double = 100.0,
    val targetCoin: String = "BTC",
    val apiKeyRotationReminder: Boolean = true,
    val ipWhitelistingEnabled: Boolean = true
)

@Dao
interface AppDao {
    @Query("SELECT * FROM paper_account WHERE id = 1")
    fun observeAccount(): Flow<PaperAccountEntity?>

    @Query("SELECT * FROM paper_account WHERE id = 1")
    suspend fun getAccount(): PaperAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: PaperAccountEntity)

    @Query("SELECT * FROM paper_holdings ORDER BY symbol ASC")
    fun observeHoldings(): Flow<List<PaperHoldingEntity>>

    @Query("SELECT * FROM paper_holdings WHERE symbol = :symbol")
    suspend fun getHolding(symbol: String): PaperHoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(holding: PaperHoldingEntity)

    @Query("DELETE FROM paper_holdings WHERE symbol = :symbol")
    suspend fun deleteHolding(symbol: String)

    @Query("DELETE FROM paper_holdings")
    suspend fun clearHoldings()

    @Query("SELECT * FROM trade_log ORDER BY timestampMs DESC LIMIT 100")
    fun observeTradeLog(): Flow<List<TradeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeLog(entry: TradeLogEntity)

    @Query("DELETE FROM trade_log")
    suspend fun clearTradeLog()

    @Query("SELECT * FROM agent_memory ORDER BY timestampMs DESC LIMIT 60")
    fun observeAgentMemory(): Flow<List<AgentMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentMemory(entry: AgentMemoryEntity)

    @Query("SELECT * FROM research_watchlist ORDER BY savedAtMs DESC")
    fun observeWatchlist(): Flow<List<ResearchWatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlist(item: ResearchWatchlistEntity)

    @Query("DELETE FROM research_watchlist WHERE symbol = :symbol")
    suspend fun deleteWatchlist(symbol: String)

    @Query("SELECT * FROM watch_wallets ORDER BY createdAtMs DESC")
    fun observeWatchWallets(): Flow<List<WatchWalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchWallet(wallet: WatchWalletEntity)

    @Query("DELETE FROM watch_wallets WHERE id = :id")
    suspend fun deleteWatchWallet(id: Long)

    @Query("SELECT * FROM arbitrage_inventory ORDER BY venue ASC, asset ASC")
    fun observeArbitrageInventory(): Flow<List<ArbitrageInventoryEntity>>

    @Query("SELECT * FROM arbitrage_inventory WHERE venueAssetKey = :key")
    suspend fun getArbitrageInventoryItem(key: String): ArbitrageInventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArbitrageInventory(item: ArbitrageInventoryEntity)

    @Query("DELETE FROM arbitrage_inventory")
    suspend fun clearArbitrageInventory()

    @Query("SELECT * FROM arbitrage_trades ORDER BY timestampMs DESC LIMIT 60")
    fun observeArbitrageTrades(): Flow<List<ArbitrageTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArbitrageTrade(trade: ArbitrageTradeEntity)

    @Query("SELECT * FROM price_triggers ORDER BY symbol ASC")
    fun observePriceTriggers(): Flow<List<PriceTriggerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPriceTrigger(trigger: PriceTriggerEntity)

    @Query("DELETE FROM price_triggers WHERE symbol = :symbol")
    suspend fun deletePriceTrigger(symbol: String)

    @Query("SELECT * FROM payout_rules WHERE id = 1")
    fun observePayoutRule(): Flow<PayoutRuleEntity?>

    @Query("SELECT * FROM payout_rules WHERE id = 1")
    suspend fun getPayoutRule(): PayoutRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayoutRule(rule: PayoutRuleEntity)
}

@Database(
    entities = [
        PaperAccountEntity::class,
        PaperHoldingEntity::class,
        TradeLogEntity::class,
        AgentMemoryEntity::class,
        ResearchWatchlistEntity::class,
        WatchWalletEntity::class,
        ArbitrageInventoryEntity::class,
        ArbitrageTradeEntity::class,
        PriceTriggerEntity::class,
        PayoutRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lg_digital_desk.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

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
        if (dao.getAccount() == null) {
            dao.upsertAccount(PaperAccountEntity())
        }
        if (dao.getPayoutRule() == null) {
            dao.upsertPayoutRule(PayoutRuleEntity())
        }
        if (dao.getArbitrageInventoryItem("Kraken_USD") == null) {
            seedArbitrageInventory()
        }
        dao.upsertPriceTrigger(PriceTriggerEntity("BTC", 95_000.0, true))
        dao.upsertPriceTrigger(PriceTriggerEntity("ETH", 3_600.0, true))
        dao.upsertPriceTrigger(PriceTriggerEntity("SOL", 220.0, true))
    }

    private suspend fun seedArbitrageInventory() {
        val venues = listOf("Kraken", "Coinbase", "KuCoin", "Bitstamp")
        venues.forEach { venue ->
            dao.upsertArbitrageInventory(ArbitrageInventoryEntity("${venue}_USD", venue, "USD", 2500.0))
            dao.upsertArbitrageInventory(ArbitrageInventoryEntity("${venue}_BTC", venue, "BTC", 0.08))
            dao.upsertArbitrageInventory(ArbitrageInventoryEntity("${venue}_ETH", venue, "ETH", 1.5))
            dao.upsertArbitrageInventory(ArbitrageInventoryEntity("${venue}_SOL", venue, "SOL", 20.0))
        }
    }

    suspend fun updateAccountConfig(
        autopilotRunning: Boolean? = null,
        riskProfile: RiskProfile? = null,
        stopSchedule: String? = null,
        maxDrawdownPct: Double? = null,
        htfConfirmation: Boolean? = null,
        selectedSymbol: String? = null,
        selectedStrategyId: String? = null
    ) {
        val current = dao.getAccount() ?: PaperAccountEntity()
        dao.upsertAccount(
            current.copy(
                autopilotRunning = autopilotRunning ?: current.autopilotRunning,
                riskProfile = riskProfile?.label ?: current.riskProfile,
                stopSchedule = stopSchedule ?: current.stopSchedule,
                maxDrawdownPct = maxDrawdownPct ?: current.maxDrawdownPct,
                htfConfirmation = htfConfirmation ?: current.htfConfirmation,
                selectedSymbol = selectedSymbol ?: current.selectedSymbol,
                selectedStrategyId = selectedStrategyId ?: current.selectedStrategyId
            )
        )
    }

    suspend fun executePaperBuy(
        symbol: String,
        notionalUsd: Double,
        priceUsd: Double,
        strategyId: String,
        reason: String
    ): Result<String> = runCatching {
        require(notionalUsd >= 10.0) { "Minimum paper order is $10.00." }
        require(priceUsd > 0.0) { "Live price unavailable for $symbol." }

        val acct = dao.getAccount() ?: PaperAccountEntity()
        if (acct.usdBalance < notionalUsd) {
            throw IllegalStateException("Insufficient paper USD balance (${"%.2f".format(acct.usdBalance)}).")
        }
        val feeUsd = notionalUsd * 0.001
        val netSpend = notionalUsd - feeUsd
        val qtyBought = netSpend / priceUsd

        val sym = symbol.uppercase()
        val existing = dao.getHolding(sym)
        val newQty = (existing?.quantity ?: 0.0) + qtyBought
        val newAvg = if (existing == null || existing.quantity <= 0.0) {
            priceUsd
        } else {
            ((existing.quantity * existing.avgEntryPrice) + netSpend) / newQty
        }

        dao.upsertHolding(PaperHoldingEntity(sym, newQty, newAvg, System.currentTimeMillis()))
        val newCash = (acct.usdBalance - notionalUsd).coerceAtLeast(0.0)
        dao.upsertAccount(
            acct.copy(
                usdBalance = newCash,
                dailyNotionalUsedUsd = acct.dailyNotionalUsedUsd + notionalUsd
            )
        )
        dao.insertTradeLog(
            TradeLogEntity(
                symbol = sym,
                side = "BUY",
                quantity = qtyBought,
                priceUsd = priceUsd,
                notionalUsd = notionalUsd,
                pnlUsd = -feeUsd,
                strategyId = strategyId,
                reason = reason
            )
        )
        "Paper BUY filled: ${"%.5f".format(qtyBought)} $sym @ $${"%.2f".format(priceUsd)}"
    }

    suspend fun executePaperSell(
        symbol: String,
        notionalUsd: Double,
        priceUsd: Double,
        strategyId: String,
        reason: String
    ): Result<String> = runCatching {
        require(notionalUsd >= 10.0) { "Minimum paper exit is $10.00." }
        require(priceUsd > 0.0) { "Live price unavailable for $symbol." }

        val sym = symbol.uppercase()
        val holding = dao.getHolding(sym)
            ?: throw IllegalStateException("No open paper $sym position to sell.")
        val maxNotional = holding.quantity * priceUsd
        val actualNotional = minOf(notionalUsd, maxNotional)
        val qtySold = actualNotional / priceUsd
        val feeUsd = actualNotional * 0.001
        val proceedsUsd = (actualNotional - feeUsd).coerceAtLeast(0.0)
        val costBasis = qtySold * holding.avgEntryPrice
        val realizedPnl = proceedsUsd - costBasis

        val remQty = holding.quantity - qtySold
        if (remQty <= 1e-8) {
            dao.deleteHolding(sym)
        } else {
            dao.upsertHolding(holding.copy(quantity = remQty, updatedAtMs = System.currentTimeMillis()))
        }

        val acct = dao.getAccount() ?: PaperAccountEntity()
        val newCash = acct.usdBalance + proceedsUsd
        dao.upsertAccount(
            acct.copy(
                usdBalance = newCash,
                peakEquityUsd = max(acct.peakEquityUsd, newCash),
                dailyNotionalUsedUsd = acct.dailyNotionalUsedUsd + actualNotional
            )
        )
        dao.insertTradeLog(
            TradeLogEntity(
                symbol = sym,
                side = "SELL",
                quantity = qtySold,
                priceUsd = priceUsd,
                notionalUsd = actualNotional,
                pnlUsd = realizedPnl,
                strategyId = strategyId,
                reason = reason
            )
        )
        "Paper SELL filled: ${"%.5f".format(qtySold)} $sym (PnL ${if (realizedPnl >= 0) "+" else ""}$${"%.2f".format(realizedPnl)})"
    }

    suspend fun executePaperArbitrage(opp: ArbitrageOpportunity): Result<String> = runCatching {
        val notional = opp.notionalUsd.toDouble()
        val buyUsdKey = "${opp.buyVenue}_USD"
        val sellUsdKey = "${opp.sellVenue}_USD"
        val buyUsd = dao.getArbitrageInventoryItem(buyUsdKey)
            ?: ArbitrageInventoryEntity(buyUsdKey, opp.buyVenue, "USD", 2500.0)
        val sellUsd = dao.getArbitrageInventoryItem(sellUsdKey)
            ?: ArbitrageInventoryEntity(sellUsdKey, opp.sellVenue, "USD", 2500.0)

        val netUsd = opp.worstCaseNetUsd.toDouble()
        dao.upsertArbitrageInventory(buyUsd.copy(balance = (buyUsd.balance - notional).coerceAtLeast(0.0)))
        dao.upsertArbitrageInventory(sellUsd.copy(balance = sellUsd.balance + notional + netUsd))

        dao.insertArbitrageTrade(
            ArbitrageTradeEntity(
                symbol = opp.symbol,
                buyVenue = opp.buyVenue,
                sellVenue = opp.sellVenue,
                notionalUsd = notional,
                grossSpreadBps = opp.grossSpreadBps,
                worstCaseNetUsd = netUsd,
                worstCaseNetBps = opp.worstCaseNetBps
            )
        )
        logMemory(
            "PAPER_ARB_FILLED",
            opp.symbol,
            "Arb ${opp.buyVenue} -> ${opp.sellVenue} (${opp.symbol}) net ${if (netUsd >= 0) "+" else ""}$${"%.2f".format(netUsd)}."
        )
        "Paper arbitrage logged: ${opp.buyVenue} → ${opp.sellVenue} net ${if (netUsd >= 0) "+" else ""}$${"%.2f".format(netUsd)}"
    }

    suspend fun resetArbitrageInventory() {
        dao.clearArbitrageInventory()
        seedArbitrageInventory()
    }

    suspend fun resetPaperPortfolio() {
        dao.clearHoldings()
        dao.clearTradeLog()
        dao.upsertAccount(PaperAccountEntity())
        logMemory("PORTFOLIO_RESET", "USD", "Paper portfolio reset to $10,000.00 starter balance.")
    }

    suspend fun logMemory(eventType: String, symbol: String, summary: String) {
        dao.insertAgentMemory(
            AgentMemoryEntity(
                eventType = eventType,
                symbol = symbol,
                summary = summary
            )
        )
    }

    suspend fun saveWatchlistItem(symbol: String, projected24h: Double, regime: String, notes: String) {
        dao.upsertWatchlist(
            ResearchWatchlistEntity(
                symbol = symbol.uppercase(),
                projected24hUsd = projected24h,
                regimeLabel = regime,
                notes = notes
            )
        )
    }

    suspend fun removeWatchlistItem(symbol: String) {
        dao.deleteWatchlist(symbol.uppercase())
    }

    suspend fun addWatchWallet(label: String, network: String, address: String) {
        dao.insertWatchWallet(WatchWalletEntity(label = label, network = network, address = address))
    }

    suspend fun removeWatchWallet(id: Long) {
        dao.deleteWatchWallet(id)
    }

    suspend fun upsertPriceTrigger(symbol: String, targetPriceUsd: Double, enabled: Boolean) {
        dao.upsertPriceTrigger(PriceTriggerEntity(symbol.uppercase(), targetPriceUsd, enabled))
    }

    suspend fun deletePriceTrigger(symbol: String) {
        dao.deletePriceTrigger(symbol.uppercase())
    }

    suspend fun savePayoutRule(rule: PayoutRuleEntity) {
        dao.upsertPayoutRule(rule)
    }
}

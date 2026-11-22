package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM paper_account WHERE id = 1")
    fun observeAccount(): Flow<PaperAccountEntity?>

    @Query("SELECT * FROM paper_account WHERE id = 1")
    suspend fun getAccountOnce(): PaperAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: PaperAccountEntity)

    @Query("SELECT * FROM paper_holdings WHERE quantity > 0.0000001 ORDER BY symbol ASC")
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

    @Query("SELECT * FROM agent_memory ORDER BY timestampMs DESC LIMIT 100")
    fun observeAgentMemory(): Flow<List<AgentMemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentMemory(entry: AgentMemoryEntity)

    @Query("DELETE FROM agent_memory")
    suspend fun clearAgentMemory()

    @Query("SELECT * FROM research_watchlist ORDER BY addedAtMs DESC")
    fun observeWatchlist(): Flow<List<ResearchWatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlist(item: ResearchWatchlistEntity)

    @Query("DELETE FROM research_watchlist WHERE symbol = :symbol")
    suspend fun deleteWatchlist(symbol: String)

    @Query("SELECT * FROM watch_wallets ORDER BY addedAtMs DESC")
    fun observeWatchWallets(): Flow<List<WatchWalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchWallet(wallet: WatchWalletEntity)

    @Query("DELETE FROM watch_wallets WHERE id = :id")
    suspend fun deleteWatchWallet(id: Long)

    @Query("SELECT * FROM arbitrage_inventory ORDER BY venue ASC, asset ASC")
    fun observeArbitrageInventory(): Flow<List<ArbitrageInventoryEntity>>

    @Query("SELECT * FROM arbitrage_inventory")
    suspend fun getArbitrageInventoryOnce(): List<ArbitrageInventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArbitrageInventory(items: List<ArbitrageInventoryEntity>)

    @Query("DELETE FROM arbitrage_inventory")
    suspend fun clearArbitrageInventory()

    @Query("SELECT * FROM arbitrage_trades ORDER BY timestampMs DESC LIMIT 100")
    fun observeArbitrageTrades(): Flow<List<ArbitrageTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArbitrageTrade(trade: ArbitrageTradeEntity)

    @Query("DELETE FROM arbitrage_trades")
    suspend fun clearArbitrageTrades()

    @Query("SELECT * FROM price_triggers ORDER BY symbol ASC")
    fun observePriceTriggers(): Flow<List<PriceTriggerEntity>>

    @Query("SELECT * FROM price_triggers")
    suspend fun getPriceTriggersOnce(): List<PriceTriggerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPriceTrigger(trigger: PriceTriggerEntity)

    @Query("DELETE FROM price_triggers WHERE symbol = :symbol")
    suspend fun deletePriceTrigger(symbol: String)

    @Query("SELECT * FROM payout_rules WHERE id = 1")
    fun observePayoutRule(): Flow<PayoutRuleEntity?>

    @Query("SELECT * FROM payout_rules WHERE id = 1")
    suspend fun getPayoutRuleOnce(): PayoutRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayoutRule(rule: PayoutRuleEntity)
}

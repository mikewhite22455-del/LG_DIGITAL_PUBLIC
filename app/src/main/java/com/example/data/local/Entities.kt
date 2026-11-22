package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paper_account")
data class PaperAccountEntity(
    @PrimaryKey val id: Int = 1,
    val usdBalance: Double = 10000.0,
    val peakEquityUsd: Double = 10000.0,
    val dailyNotionalUsedUsd: Double = 0.0,
    val autopilotRunning: Boolean = false,
    val riskProfile: String = "MEDIUM",
    val stopSchedule: String = "22:00-06:00",
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
    val symbol: String,
    val side: String,
    val quantity: Double,
    val priceUsd: Double,
    val notionalUsd: Double,
    val strategyId: String,
    val reason: String,
    val pnlUsd: Double = 0.0,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_memory")
data class AgentMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val symbol: String,
    val summary: String,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "research_watchlist")
data class ResearchWatchlistEntity(
    @PrimaryKey val symbol: String,
    val projected24hUsd: Double,
    val regimeLabel: String,
    val notes: String,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "watch_wallets")
data class WatchWalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val network: String,
    val address: String,
    val addedAtMs: Long = System.currentTimeMillis()
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
    val symbol: String,
    val buyVenue: String,
    val sellVenue: String,
    val baseQty: Double,
    val notionalUsd: Double,
    val worstCaseNetUsd: Double,
    val grossSpreadBps: Double,
    val isSynthetic: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_triggers")
data class PriceTriggerEntity(
    @PrimaryKey val symbol: String,
    val targetPriceUsd: Double,
    val isAbove: Boolean = true,
    val enabled: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "payout_rules")
data class PayoutRuleEntity(
    @PrimaryKey val id: Int = 1,
    val autoSweepUsd: Double = 300.0,
    val targetCoin: String = "BTC",
    val minPayoutThresholdUsd: Double = 100.0,
    val pushAlertsEnabled: Boolean = true,
    val emailAlertsEnabled: Boolean = true,
    val webhookEnabled: Boolean = false,
    val ipWhitelistingEnabled: Boolean = true,
    val apiKeyRotationReminder: Boolean = true
)

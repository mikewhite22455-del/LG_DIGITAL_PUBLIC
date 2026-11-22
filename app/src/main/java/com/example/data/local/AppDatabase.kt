package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lg_digital_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

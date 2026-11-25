package com.example.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.domain.models.ConnectedExchange

/**
 * Represents a live tradable cryptocurrency listed on Kraken, Coinbase, KuCoin, or Bitstamp.
 */
data class ExchangeListedCoin(
    val symbol: String,
    val name: String,
    val exchanges: List<String>,
    val primaryPair: String = "$symbol/USD"
) {
    val exchangesLabel: String
        get() = exchanges.joinToString(" · ")
}

data class ExchangeCoinCatalogSnapshot(
    val activeScopeLabel: String = "Kraken",
    val selectedFilter: ConnectedExchange = ConnectedExchange.KRAKEN,
    val attachedExchangeIds: List<String> = emptyList(),
    val coins: List<ExchangeListedCoin> = CoinAssets.catalog.map { asset ->
        ExchangeListedCoin(
            symbol = asset.symbol,
            name = asset.name,
            exchanges = listOf("Kraken"),
            primaryPair = "${asset.symbol}/USD"
        )
    },
    val isLoading: Boolean = false,
    val lastUpdatedMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * App-wide Compose-observable catalog of EVERY tradable coin offered by Kraken or whichever
 * exchange(s) the user has attached in Exchange Configuration.
 *
 * Read automatically by CoinDropdownSelector and coin search bars across all screens.
 */
object ExchangeCoinCatalogStore {
    var snapshot by mutableStateOf(ExchangeCoinCatalogSnapshot())
        private set

    var onRequestRefresh: ((ConnectedExchange?) -> Unit)? = null

    fun update(transform: (ExchangeCoinCatalogSnapshot) -> ExchangeCoinCatalogSnapshot) {
        snapshot = transform(snapshot)
    }

    fun requestRefresh(overrideExchange: ConnectedExchange? = null) {
        onRequestRefresh?.invoke(overrideExchange)
    }
}

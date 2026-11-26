package com.example.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.domain.models.ConnectedExchange
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe registry of live official coin logo URLs (populated from CoinGecko,
 * Coinbase, KuCoin, and deterministic CoinCap CDN fallback) so CoinAvatar renders
 * authentic cryptocurrency logos for every supported exchange asset.
 */
object CoinLogoCache {
    private val urls = ConcurrentHashMap<String, String>()

    fun register(symbol: String, url: String?) {
        val cleanSym = normalizeSymbol(symbol)
        val cleanUrl = url?.trim().orEmpty()
        if (cleanSym.isNotBlank() && cleanUrl.startsWith("http")) {
            urls[cleanSym] = cleanUrl
        }
    }

    fun urlFor(symbol: String): String {
        val cleanSym = normalizeSymbol(symbol)
        return urls[cleanSym]
            ?: "https://assets.coincap.io/assets/icons/${cleanSym.lowercase()}@2x.png"
    }

    fun cachedOfficialUrlOrNull(symbol: String): String? = urls[normalizeSymbol(symbol)]

    private fun normalizeSymbol(raw: String): String = raw.trim().uppercase()
        .removeSuffix("/USD")
        .removeSuffix("-USD")
        .removeSuffix("/USDT")
        .removeSuffix("-USDT")
        .let {
            when (it) {
                "XBT", "XXBT" -> "BTC"
                "XDG", "XXDG" -> "DOGE"
                else -> it
            }
        }
}

/**
 * Represents a live tradable cryptocurrency listed on Kraken, Coinbase, KuCoin, or Bitstamp.
 */
data class ExchangeListedCoin(
    val symbol: String,
    val name: String,
    val exchanges: List<String>,
    val primaryPair: String = "$symbol/USD",
    val logoUrl: String? = null
) {
    val exchangesLabel: String
        get() = exchanges.joinToString(" · ")

    val resolvedLogoUrl: String
        get() = logoUrl?.takeIf { it.isNotBlank() } ?: CoinLogoCache.urlFor(symbol)
}

data class ExchangeCoinCatalogSnapshot(
    val activeScopeLabel: String = "Kraken · Coinbase · KuCoin",
    val selectedFilter: ConnectedExchange = ConnectedExchange.ALL_CONNECTED,
    val attachedExchangeIds: List<String> = emptyList(),
    val coins: List<ExchangeListedCoin> = defaultSeedCoins(),
    val triExchangeCoins: List<ExchangeListedCoin> = defaultSeedCoins(),
    val krakenCount: Int = 0,
    val coinbaseCount: Int = 0,
    val kucoinCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingTriExchange: Boolean = false,
    val lastUpdatedMs: Long = 0L,
    val triExchangeUpdatedMs: Long = 0L,
    val errorMessage: String? = null,
    val triExchangeError: String? = null
) {
    companion object {
        fun defaultSeedCoins(): List<ExchangeListedCoin> = CoinAssets.catalog.map { asset ->
            ExchangeListedCoin(
                symbol = asset.symbol,
                name = asset.name,
                exchanges = listOf("Kraken", "Coinbase", "KuCoin"),
                primaryPair = "${asset.symbol}/USD",
                logoUrl = CoinLogoCache.urlFor(asset.symbol)
            )
        }
    }
}

/**
 * App-wide Compose-observable catalog of EVERY tradable coin offered by Kraken, Coinbase,
 * KuCoin (and Bitstamp), read automatically by the Dashboard autocomplete search bar,
 * CoinDropdownSelector, and coin pickers across all screens.
 */
object ExchangeCoinCatalogStore {
    var snapshot by mutableStateOf(ExchangeCoinCatalogSnapshot())
        private set

    var onRequestRefresh: ((ConnectedExchange?) -> Unit)? = null
    var onRequestTriExchangeRefresh: (() -> Unit)? = null

    fun update(transform: (ExchangeCoinCatalogSnapshot) -> ExchangeCoinCatalogSnapshot) {
        snapshot = transform(snapshot)
    }

    fun requestRefresh(overrideExchange: ConnectedExchange? = null) {
        onRequestRefresh?.invoke(overrideExchange)
    }

    fun requestTriExchangeRefresh() {
        onRequestTriExchangeRefresh?.invoke()
    }
}

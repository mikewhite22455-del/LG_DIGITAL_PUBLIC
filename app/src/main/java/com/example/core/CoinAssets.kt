package com.example.core

import androidx.annotation.DrawableRes
import com.example.R

data class CoinCatalogEntry(
    val symbol: String,
    val name: String,
    val coingeckoId: String,
    val krakenPair: String,
    val coinbaseProduct: String,
    val bitstampPair: String,
    @param:DrawableRes val iconRes: Int?
)

object CoinAssets {
    val catalog: List<CoinCatalogEntry> = listOf(
        CoinCatalogEntry("BTC", "Bitcoin", "bitcoin", "XBTUSD", "BTC-USD", "btcusd", R.drawable.coin_btc),
        CoinCatalogEntry("ETH", "Ethereum", "ethereum", "ETHUSD", "ETH-USD", "ethusd", R.drawable.coin_eth),
        CoinCatalogEntry("SOL", "Solana", "solana", "SOLUSD", "SOL-USD", "solusd", R.drawable.coin_sol),
        CoinCatalogEntry("XRP", "XRP", "ripple", "XRPUSD", "XRP-USD", "xrpusd", R.drawable.coin_xrp),
        CoinCatalogEntry("ADA", "Cardano", "cardano", "ADAUSD", "ADA-USD", "adausd", R.drawable.coin_ada),
        CoinCatalogEntry("AVAX", "Avalanche", "avalanche-2", "AVAXUSD", "AVAX-USD", "avaxusd", R.drawable.coin_avax),
        CoinCatalogEntry("LINK", "Chainlink", "chainlink", "LINKUSD", "LINK-USD", "linkusd", R.drawable.coin_link),
        CoinCatalogEntry("DOT", "Polkadot", "polkadot", "DOTUSD", "DOT-USD", "dotusd", R.drawable.coin_dot),
        CoinCatalogEntry("DOGE", "Dogecoin", "dogecoin", "DOGEUSD", "DOGE-USD", "dogeusd", R.drawable.coin_doge),
        CoinCatalogEntry("LTC", "Litecoin", "litecoin", "LTCUSD", "LTC-USD", "ltcusd", R.drawable.coin_ltc),
        CoinCatalogEntry("BCH", "Bitcoin Cash", "bitcoin-cash", "BCHUSD", "BCH-USD", "bchusd", R.drawable.coin_bch),
        CoinCatalogEntry("PEPE", "Pepe", "pepe", "PEPEUSD", "PEPE-USD", "pepeusd", R.drawable.coin_pepe),
        CoinCatalogEntry("HYPE", "Hyperliquid", "hyperliquid", "HYPEUSD", "HYPE-USD", "hypeusd", R.drawable.coin_hype),
        CoinCatalogEntry("ZEC", "Zcash", "zcash", "ZECUSD", "ZEC-USD", "zecusd", R.drawable.coin_zec),
        CoinCatalogEntry("PUMP", "Pump.fun", "pump-fun", "PUMPUSD", "PUMP-USD", "pumpusd", R.drawable.coin_pump)
    )

    private val bySymbol = catalog.associateBy { it.symbol.uppercase() }

    fun find(symbol: String): CoinCatalogEntry? =
        bySymbol[symbol.uppercase().removeSuffix("USD").removeSuffix("-USD").removeSuffix("/USD")]

    @DrawableRes
    fun iconFor(symbol: String): Int? = find(symbol)?.iconRes
}

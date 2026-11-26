package com.example.core

import androidx.annotation.DrawableRes

data class CoinAssetMeta(
    val symbol: String,
    val name: String,
    val coingeckoId: String,
    val krakenPair: String,
    @DrawableRes val iconRes: Int? = null,
    val coinbaseProduct: String = "${symbol.uppercase()}-USD",
    val bitstampPair: String = "${symbol.lowercase()}usd"
)

object CoinAssets {

    val catalog: List<CoinAssetMeta> = listOf(
        CoinAssetMeta("BTC", "Bitcoin", "bitcoin", "XBTUSD"),
        CoinAssetMeta("ETH", "Ethereum", "ethereum", "ETHUSD"),
        CoinAssetMeta("SOL", "Solana", "solana", "SOLUSD"),
        CoinAssetMeta("XRP", "XRP", "ripple", "XRPUSD"),
        CoinAssetMeta("ADA", "Cardano", "cardano", "ADAUSD"),
        CoinAssetMeta("DOGE", "Dogecoin", "dogecoin", "XDGUSD"),
        CoinAssetMeta("AVAX", "Avalanche", "avalanche-2", "AVAXUSD"),
        CoinAssetMeta("LINK", "Chainlink", "chainlink", "LINKUSD"),
        CoinAssetMeta("DOT", "Polkadot", "polkadot", "DOTUSD"),
        CoinAssetMeta("LTC", "Litecoin", "litecoin", "LTCUSD"),
        CoinAssetMeta("BCH", "Bitcoin Cash", "bitcoin-cash", "BCHUSD"),
        CoinAssetMeta("UNI", "Uniswap", "uniswap", "UNIUSD"),
        CoinAssetMeta("ATOM", "Cosmos", "cosmos", "ATOMUSD"),
        CoinAssetMeta("NEAR", "NEAR Protocol", "near", "NEARUSD"),
        CoinAssetMeta("SUI", "Sui", "sui", "SUIUSD"),
        CoinAssetMeta("APT", "Aptos", "aptos", "APTUSD"),
        CoinAssetMeta("OP", "Optimism", "optimism", "OPUSD"),
        CoinAssetMeta("ARB", "Arbitrum", "arbitrum", "ARBUSD"),
        CoinAssetMeta("AAVE", "Aave", "aave", "AAVEUSD"),
        CoinAssetMeta("XLM", "Stellar", "stellar", "XLMUSD"),
        CoinAssetMeta("ALGO", "Algorand", "algorand", "ALGOUSD"),
        CoinAssetMeta("FIL", "Filecoin", "filecoin", "FILUSD"),
        CoinAssetMeta("INJ", "Injective", "injective-protocol", "INJUSD"),
        CoinAssetMeta("SHIB", "Shiba Inu", "shiba-inu", "SHIBUSD"),
        CoinAssetMeta("PEPE", "Pepe", "pepe", "PEPEUSD"),
        CoinAssetMeta("MATIC", "Polygon", "matic-network", "POLUSD"),
        CoinAssetMeta("POL", "Polygon", "polygon-ecosystem-token", "POLUSD"),
        CoinAssetMeta("USDC", "USD Coin", "usd-coin", "USDCUSD"),
        CoinAssetMeta("USDT", "Tether", "tether", "USDTUSD")
    )

    private val bySymbol: Map<String, CoinAssetMeta> = catalog.associateBy { it.symbol.uppercase() }

    fun find(symbol: String): CoinAssetMeta? {
        val clean = symbol.trim().uppercase()
            .removeSuffix("/USD")
            .removeSuffix("-USD")
            .removeSuffix("USD")
            .let { if (it == "XBT") "BTC" else if (it == "XDG") "DOGE" else it }
        return bySymbol[clean]
    }

    @DrawableRes
    fun iconFor(symbol: String): Int? = find(symbol)?.iconRes
}

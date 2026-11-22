package com.example.data.remote

import android.util.Xml
import com.example.core.CoinAssets
import com.example.domain.models.Candle
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.DexPairInfo
import com.example.domain.models.MarketTicker
import com.example.domain.models.MiningQuote
import com.example.domain.models.NewsItem
import com.example.domain.models.OrderBookLevel
import com.example.domain.models.OrderBookSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.math.BigDecimal
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MarketApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun fetchString(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "LGDigital-Android/1.0")
            .header("Accept", "application/json, application/xml, text/xml")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} from ${resp.request.url.host}")
            }
            return resp.body?.string() ?: throw IllegalStateException("Empty body from ${resp.request.url.host}")
        }
    }

    suspend fun fetchMarketTickers(): Result<List<MarketTicker>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                fetchFromCoinGecko()
            } catch (_: Exception) {
                fetchFromKuCoin()
            }
        }
    }

    private fun fetchFromCoinGecko(): List<MarketTicker> {
        val ids = CoinAssets.catalog.joinToString(",") { it.coingeckoId }
        val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=$ids&order=market_cap_desc&per_page=25&page=1&sparkline=false&price_change_percentage=24h"
        val body = fetchString(url)
        val arr = JSONArray(body)
        if (arr.length() == 0) throw IllegalStateException("CoinGecko returned 0 markets")
        val list = mutableListOf<MarketTicker>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val sym = obj.optString("symbol").uppercase()
            val entry = CoinAssets.find(sym)
            list += MarketTicker(
                symbol = sym,
                name = entry?.name ?: obj.optString("name", sym),
                priceUsd = obj.optDouble("current_price", 0.0),
                change24hPct = obj.optDouble("price_change_percentage_24h", 0.0),
                volume24hUsd = obj.optDouble("total_volume", 0.0),
                marketCapUsd = obj.optDouble("market_cap", 0.0),
                high24h = obj.optDouble("high_24h", 0.0),
                low24h = obj.optDouble("low_24h", 0.0),
                source = "CoinGecko"
            )
        }
        return list.sortedByDescending { it.marketCapUsd }
    }

    private fun fetchFromKuCoin(): List<MarketTicker> {
        val body = fetchString("https://api.kucoin.com/api/v1/market/allTickers")
        val root = JSONObject(body)
        val tickerArr = root.getJSONObject("data").getJSONArray("ticker")
        val targetMap = CoinAssets.catalog.associateBy { "${it.symbol}-USDT" }
        val list = mutableListOf<MarketTicker>()
        for (i in 0 until tickerArr.length()) {
            val obj = tickerArr.getJSONObject(i)
            val pair = obj.optString("symbol")
            val entry = targetMap[pair] ?: continue
            list += MarketTicker(
                symbol = entry.symbol,
                name = entry.name,
                priceUsd = obj.optString("last", "0").toDoubleOrNull() ?: 0.0,
                change24hPct = (obj.optString("changeRate", "0").toDoubleOrNull() ?: 0.0) * 100.0,
                volume24hUsd = obj.optString("volValue", "0").toDoubleOrNull() ?: 0.0,
                marketCapUsd = 0.0,
                high24h = obj.optString("high", "0").toDoubleOrNull() ?: 0.0,
                low24h = obj.optString("low", "0").toDoubleOrNull() ?: 0.0,
                source = "KuCoin"
            )
        }
        if (list.isEmpty()) throw IllegalStateException("KuCoin returned no matching USDT tickers")
        return list
    }

    suspend fun fetchCandles(
        symbol: String,
        timeframe: ChartTimeframe
    ): Result<List<Candle>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                fetchKrakenCandles(symbol, timeframe)
            } catch (_: Exception) {
                fetchKuCoinCandles(symbol, timeframe)
            }
        }
    }

    private fun fetchKrakenCandles(symbol: String, timeframe: ChartTimeframe): List<Candle> {
        val pair = CoinAssets.find(symbol)?.krakenPair ?: "${symbol.uppercase()}USD"
        val url = "https://api.kraken.com/0/public/OHLC?pair=$pair&interval=${timeframe.krakenInterval}"
        val root = JSONObject(fetchString(url))
        val errs = root.optJSONArray("error")
        if (errs != null && errs.length() > 0) {
            throw IllegalStateException("Kraken OHLC: ${errs.getString(0)}")
        }
        val resultObj = root.getJSONObject("result")
        val dataKey = resultObj.keys().asSequence().firstOrNull { it != "last" }
            ?: throw IllegalStateException("Missing Kraken series key")
        val arr = resultObj.getJSONArray(dataKey)
        val out = mutableListOf<Candle>()
        val start = maxOf(0, arr.length() - 160)
        for (i in start until arr.length()) {
            val row = arr.getJSONArray(i)
            out += Candle(
                timestampSec = row.getLong(0),
                open = row.getString(1).toDouble(),
                high = row.getString(2).toDouble(),
                low = row.getString(3).toDouble(),
                close = row.getString(4).toDouble(),
                volume = row.getString(6).toDouble()
            )
        }
        if (out.isEmpty()) throw IllegalStateException("Empty Kraken OHLC")
        return out
    }

    private fun fetchKuCoinCandles(symbol: String, timeframe: ChartTimeframe): List<Candle> {
        val kuType = when (timeframe) {
            ChartTimeframe.M15 -> "15min"
            ChartTimeframe.H1 -> "1hour"
            ChartTimeframe.H4 -> "4hour"
            ChartTimeframe.D1 -> "1day"
        }
        val url = "https://api.kucoin.com/api/v1/market/candles?type=$kuType&symbol=${symbol.uppercase()}-USDT"
        val root = JSONObject(fetchString(url))
        val arr = root.getJSONArray("data")
        val out = mutableListOf<Candle>()
        for (i in 0 until minOf(160, arr.length())) {
            val row = arr.getJSONArray(i)
            out += Candle(
                timestampSec = row.getString(0).toLong(),
                open = row.getString(1).toDouble(),
                close = row.getString(2).toDouble(),
                high = row.getString(3).toDouble(),
                low = row.getString(4).toDouble(),
                volume = row.getString(5).toDouble()
            )
        }
        if (out.isEmpty()) throw IllegalStateException("Empty KuCoin candles")
        return out.sortedBy { it.timestampSec }
    }

    suspend fun fetchPublicOrderBooks(symbol: String): List<OrderBookSnapshot> = coroutineScope {
        val entry = CoinAssets.find(symbol)
        val krakenPair = entry?.krakenPair ?: "${symbol.uppercase()}USD"
        val cbProduct = entry?.coinbaseProduct ?: "${symbol.uppercase()}-USD"
        val bsPair = entry?.bitstampPair ?: "${symbol.lowercase()}usd"
        val cdcInst = "${symbol.uppercase()}_USD"

        val tasks = listOf(
            async(Dispatchers.IO) { runCatching { fetchKrakenDepth(symbol, krakenPair) }.getOrNull() },
            async(Dispatchers.IO) { runCatching { fetchCoinbaseDepth(symbol, cbProduct) }.getOrNull() },
            async(Dispatchers.IO) { runCatching { fetchBitstampDepth(symbol, bsPair) }.getOrNull() },
            async(Dispatchers.IO) { runCatching { fetchCryptoComDepth(symbol, cdcInst) }.getOrNull() }
        )
        tasks.awaitAll().filterNotNull()
    }

    private fun fetchKrakenDepth(symbol: String, pair: String): OrderBookSnapshot {
        val root = JSONObject(fetchString("https://api.kraken.com/0/public/Depth?pair=$pair&count=12"))
        val res = root.getJSONObject("result")
        val key = res.keys().next()
        val book = res.getJSONObject(key)
        return OrderBookSnapshot(
            venue = "Kraken",
            pair = "$symbol/USD",
            timestampMs = System.currentTimeMillis(),
            bids = parseLevelArray(book.getJSONArray("bids")),
            asks = parseLevelArray(book.getJSONArray("asks")),
            isSynthetic = false
        )
    }

    private fun fetchCoinbaseDepth(symbol: String, product: String): OrderBookSnapshot {
        val root = JSONObject(fetchString("https://api.exchange.coinbase.com/products/$product/book?level=2"))
        return OrderBookSnapshot(
            venue = "Coinbase",
            pair = "$symbol/USD",
            timestampMs = System.currentTimeMillis(),
            bids = parseLevelArray(root.getJSONArray("bids"), 12),
            asks = parseLevelArray(root.getJSONArray("asks"), 12),
            isSynthetic = false
        )
    }

    private fun fetchBitstampDepth(symbol: String, pair: String): OrderBookSnapshot {
        val root = JSONObject(fetchString("https://www.bitstamp.net/api/v2/order_book/$pair/"))
        return OrderBookSnapshot(
            venue = "Bitstamp",
            pair = "$symbol/USD",
            timestampMs = System.currentTimeMillis(),
            bids = parseLevelArray(root.getJSONArray("bids"), 12),
            asks = parseLevelArray(root.getJSONArray("asks"), 12),
            isSynthetic = false
        )
    }

    private fun fetchCryptoComDepth(symbol: String, instrument: String): OrderBookSnapshot {
        val root = JSONObject(fetchString("https://api.crypto.com/exchange/v1/public/get-book?instrument_name=$instrument&depth=12"))
        val data = root.getJSONObject("result").getJSONArray("data").getJSONObject(0)
        return OrderBookSnapshot(
            venue = "Crypto.com",
            pair = "$symbol/USD",
            timestampMs = System.currentTimeMillis(),
            bids = parseLevelArray(data.getJSONArray("bids"), 12),
            asks = parseLevelArray(data.getJSONArray("asks"), 12),
            isSynthetic = false
        )
    }

    private fun parseLevelArray(arr: JSONArray, limit: Int = 12): List<OrderBookLevel> {
        val list = mutableListOf<OrderBookLevel>()
        val count = minOf(limit, arr.length())
        for (i in 0 until count) {
            val row = arr.getJSONArray(i)
            val px = row.getString(0).toBigDecimalOrNull() ?: BigDecimal.ZERO
            val qty = row.getString(1).toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (px > BigDecimal.ZERO && qty > BigDecimal.ZERO) {
                list += OrderBookLevel(px, qty)
            }
        }
        return list
    }

    suspend fun searchDexPairs(query: String): Result<List<DexPairInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query.trim().ifBlank { "SOL" }, "UTF-8")
            val root = JSONObject(fetchString("https://api.dexscreener.com/latest/dex/search?q=$encoded"))
            val arr = root.optJSONArray("pairs") ?: JSONArray()
            val out = mutableListOf<DexPairInfo>()
            for (i in 0 until minOf(25, arr.length())) {
                val p = arr.getJSONObject(i)
                val base = p.optJSONObject("baseToken")?.optString("symbol") ?: "?"
                val quote = p.optJSONObject("quoteToken")?.optString("symbol") ?: "USD"
                out += DexPairInfo(
                    chainId = p.optString("chainId", "unknown"),
                    dexId = p.optString("dexId", "dex"),
                    baseSymbol = base,
                    quoteSymbol = quote,
                    priceUsd = p.optString("priceUsd", "0").toDoubleOrNull() ?: 0.0,
                    priceChange24h = p.optJSONObject("priceChange")?.optDouble("h24", 0.0) ?: 0.0,
                    volume24hUsd = p.optJSONObject("volume")?.optDouble("h24", 0.0) ?: 0.0,
                    liquidityUsd = p.optJSONObject("liquidity")?.optDouble("usd", 0.0) ?: 0.0,
                    pairUrl = p.optString("url", "")
                )
            }
            out.sortedByDescending { it.liquidityUsd }
        }
    }

    suspend fun fetchMiningQuotes(): Result<List<MiningQuote>> = withContext(Dispatchers.IO) {
        runCatching {
            val body = fetchString("https://coin-spectrum.com/api/v1/live.json")
            val root = JSONObject(body)
            val coinsArr = root.optJSONArray("coins") ?: root.optJSONArray("data")
            val list = mutableListOf<MiningQuote>()
            if (coinsArr != null) {
                for (i in 0 until minOf(20, coinsArr.length())) {
                    val obj = coinsArr.getJSONObject(i)
                    val sym = obj.optString("symbol", obj.optString("ticker", "")).uppercase()
                    if (sym.isBlank()) continue
                    list += MiningQuote(
                        symbol = sym,
                        name = obj.optString("name", sym),
                        algorithm = obj.optString("algorithm", "PoW"),
                        priceUsd = obj.optDouble("price_usd", obj.optDouble("price", 0.0)),
                        networkHashrateLabel = obj.optString("network_hashrate_formatted", obj.optString("hashrate", "--")),
                        difficultyLabel = obj.optString("difficulty_formatted", obj.optString("difficulty", "--")),
                        blockReward = obj.optDouble("block_reward", 0.0),
                        estDailyUsdPerUnit = obj.optDouble("daily_revenue_usd", obj.optDouble("profit_24h", 0.0))
                    )
                }
            } else {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.optJSONObject(key) ?: continue
                    list += MiningQuote(
                        symbol = obj.optString("symbol", key).uppercase(),
                        name = obj.optString("name", key),
                        algorithm = obj.optString("algorithm", "PoW"),
                        priceUsd = obj.optDouble("price_usd", obj.optDouble("price", 0.0)),
                        networkHashrateLabel = obj.optString("network_hashrate", "--"),
                        difficultyLabel = obj.optString("difficulty", "--"),
                        blockReward = obj.optDouble("block_reward", 0.0),
                        estDailyUsdPerUnit = obj.optDouble("daily_usd", 0.0)
                    )
                }
            }
            if (list.isEmpty()) throw IllegalStateException("CoinSpectrum feed returned no PoW coins.")
            list
        }
    }

    suspend fun fetchCoinDeskNews(): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val xml = fetchString("https://www.coindesk.com/arc/outboundfeeds/rss/")
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            val items = mutableListOf<NewsItem>()
            var insideItem = false
            var tagName = ""
            var title = ""
            var link = ""
            var pubDate = ""
            var desc = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && items.size < 20) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        tagName = parser.name.orEmpty()
                        if (tagName.equals("item", ignoreCase = true)) {
                            insideItem = true
                            title = ""
                            link = ""
                            pubDate = ""
                            desc = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideItem) {
                            val t = parser.text.orEmpty()
                            when {
                                tagName.equals("title", true) -> title += t
                                tagName.equals("link", true) -> link += t
                                tagName.equals("pubDate", true) -> pubDate += t
                                tagName.equals("description", true) -> desc += t
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("item", ignoreCase = true) && insideItem) {
                            insideItem = false
                            val cleanDesc = desc.replace(Regex("<[^>]*>"), "").trim()
                            if (title.isNotBlank()) {
                                items += NewsItem(
                                    title = title.trim(),
                                    link = link.trim(),
                                    publishedAt = pubDate.trim().take(22),
                                    source = "CoinDesk RSS",
                                    summary = cleanDesc.take(220)
                                )
                            }
                        }
                        tagName = ""
                    }
                }
                event = parser.next()
            }
            if (items.isEmpty()) throw IllegalStateException("No headlines parsed from CoinDesk RSS.")
            items
        }
    }
}

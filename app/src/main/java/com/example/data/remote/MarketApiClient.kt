package com.example.data.remote

import android.util.Xml
import com.example.core.CoinAssets
import com.example.domain.models.Candle
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConnectedExchange
import com.example.domain.models.DexPairInfo
import com.example.domain.models.ExchangeMarketScanResult
import com.example.domain.models.ExchangeMarketTicker
import com.example.domain.models.FearGreedIndexData
import com.example.domain.models.FearGreedPoint
import com.example.domain.models.HistoryWindow
import com.example.domain.models.MarketTicker
import com.example.domain.models.MiningQuote
import com.example.domain.models.NewsItem
import com.example.domain.models.OrderBookLevel
import com.example.domain.models.OrderBookSnapshot
import com.example.domain.models.PriceHistoryPoint
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
        val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&ids=$ids&order=market_cap_desc&per_page=25&page=1&sparkline=true&price_change_percentage=24h"
        val body = fetchString(url)
        val arr = JSONArray(body)
        if (arr.length() == 0) throw IllegalStateException("CoinGecko returned 0 markets")
        val list = mutableListOf<MarketTicker>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val sym = obj.optString("symbol").uppercase()
            val entry = CoinAssets.find(sym)
            val sparkArr = obj.optJSONObject("sparkline_in_7d")?.optJSONArray("price")
            val hourly = if (sparkArr != null && sparkArr.length() > 0) {
                val startIdx = maxOf(0, sparkArr.length() - 24)
                (startIdx until sparkArr.length()).mapNotNull { idx ->
                    val v = sparkArr.optDouble(idx, Double.NaN)
                    if (v.isFinite() && v > 0.0) v else null
                }
            } else {
                emptyList()
            }
            list += MarketTicker(
                symbol = sym,
                name = entry?.name ?: obj.optString("name", sym),
                priceUsd = obj.optDouble("current_price", 0.0),
                change24hPct = obj.optDouble("price_change_percentage_24h", 0.0),
                volume24hUsd = obj.optDouble("total_volume", 0.0),
                marketCapUsd = obj.optDouble("market_cap", 0.0),
                high24h = obj.optDouble("high_24h", 0.0),
                low24h = obj.optDouble("low_24h", 0.0),
                source = "CoinGecko",
                hourlyPrices = hourly
            )
        }
        return list.sortedByDescending { it.marketCapUsd }
    }

    private suspend fun fetchFromKuCoin(): List<MarketTicker> = coroutineScope {
        val body = fetchString("https://api.kucoin.com/api/v1/market/allTickers")
        val root = JSONObject(body)
        val tickerArr = root.getJSONObject("data").getJSONArray("ticker")
        val targetMap = CoinAssets.catalog.associateBy { "${it.symbol}-USDT" }
        val baseList = mutableListOf<MarketTicker>()
        for (i in 0 until tickerArr.length()) {
            val obj = tickerArr.getJSONObject(i)
            val pair = obj.optString("symbol")
            val entry = targetMap[pair] ?: continue
            baseList += MarketTicker(
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
        if (baseList.isEmpty()) throw IllegalStateException("KuCoin returned no matching USDT tickers")
        baseList.map { ticker ->
            async(Dispatchers.IO) {
                val hourly = runCatching {
                    fetchKuCoinCandles(ticker.symbol, ChartTimeframe.H1)
                        .takeLast(24)
                        .map { it.close }
                }.getOrDefault(emptyList())
                ticker.copy(hourlyPrices = hourly)
            }
        }.awaitAll()
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

    suspend fun fetchAssetHistory(
        symbol: String,
        window: HistoryWindow
    ): Result<List<PriceHistoryPoint>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val arr = fetchKrakenCandlesInterval(symbol, window.krakenInterval, window.candleCount)
                arr.map { PriceHistoryPoint(it.timestampSec, it.close) }
            } catch (_: Exception) {
                try {
                    val ku = fetchKuCoinCandlesType(symbol, window.kuType, window.candleCount)
                    ku.map { PriceHistoryPoint(it.timestampSec, it.close) }
                } catch (_: Exception) {
                    fetchCoinGeckoMarketChart(symbol, window)
                }
            }
        }
    }

    private fun fetchKrakenCandlesInterval(symbol: String, interval: Int, maxBars: Int): List<Candle> {
        val pair = CoinAssets.find(symbol)?.krakenPair ?: "${symbol.uppercase()}USD"
        val url = "https://api.kraken.com/0/public/OHLC?pair=$pair&interval=$interval"
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
        val start = maxOf(0, arr.length() - maxBars)
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
        if (out.isEmpty()) throw IllegalStateException("Empty Kraken history")
        return out
    }

    private fun fetchKuCoinCandlesType(symbol: String, kuType: String, maxBars: Int): List<Candle> {
        val url = "https://api.kucoin.com/api/v1/market/candles?type=$kuType&symbol=${symbol.uppercase()}-USDT"
        val root = JSONObject(fetchString(url))
        val arr = root.getJSONArray("data")
        val out = mutableListOf<Candle>()
        for (i in 0 until minOf(maxBars, arr.length())) {
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
        if (out.isEmpty()) throw IllegalStateException("Empty KuCoin history")
        return out.sortedBy { it.timestampSec }
    }

    private fun fetchCoinGeckoMarketChart(symbol: String, window: HistoryWindow): List<PriceHistoryPoint> {
        val cgId = CoinAssets.find(symbol)?.coingeckoId ?: symbol.lowercase()
        val url = "https://api.coingecko.com/api/v3/coins/$cgId/market_chart?vs_currency=usd&days=${window.cgDays}"
        val root = JSONObject(fetchString(url))
        val arr = root.getJSONArray("prices")
        val all = mutableListOf<PriceHistoryPoint>()
        for (i in 0 until arr.length()) {
            val row = arr.getJSONArray(i)
            val tsSec = row.getLong(0) / 1000L
            val price = row.getDouble(1)
            if (price.isFinite() && price > 0.0) {
                all += PriceHistoryPoint(tsSec, price)
            }
        }
        if (all.isEmpty()) throw IllegalStateException("Empty CoinGecko history")
        val latestTs = all.last().timestampSec
        val cutoff = latestTs - window.lookbackSec
        val sliced = all.filter { it.timestampSec >= cutoff }
        return if (sliced.size >= 2) sliced else all.takeLast(minOf(window.candleCount, all.size))
    }

    suspend fun fetchPublicOrderBooks(symbol: String): List<OrderBookSnapshot> = coroutineScope {
        val entry = CoinAssets.find(symbol)
        val krakenPair = entry?.krakenPair ?: "${symbol.uppercase()}USD"
        val baseSymbol = entry?.symbol ?: symbol.uppercase()
        val cbProduct = "$baseSymbol-USD"
        val bsPair = "${baseSymbol.lowercase()}usd"
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

    /**
     * Fetches the official Crypto Fear & Greed Index from Alternative.me (7-day series),
     * with a secondary real-API fallback to CoinGecko Global market pulse if unreachable.
     */
    suspend fun fetchFearAndGreedIndex(): Result<FearGreedIndexData> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                fetchFearGreedFromAlternativeMe()
            } catch (_: Exception) {
                fetchFearGreedFromCoinGeckoGlobal()
            }
        }
    }

    private fun fetchFearGreedFromAlternativeMe(): FearGreedIndexData {
        val body = fetchString("https://api.alternative.me/fng/?limit=7&format=json")
        val root = JSONObject(body)
        val dataArr = root.optJSONArray("data")
            ?: throw IllegalStateException("Alternative.me FNG returned missing data array")
        if (dataArr.length() == 0) {
            throw IllegalStateException("Alternative.me FNG returned empty data")
        }

        val points = mutableListOf<FearGreedPoint>()
        var nextUpdateSec: Long? = null

        for (i in 0 until dataArr.length()) {
            val item = dataArr.getJSONObject(i)
            val value = item.optString("value").toIntOrNull()?.coerceIn(0, 100) ?: continue
            val classification = item.optString("value_classification")
                .ifBlank { classifyFearGreed(value) }
            val tsSec = item.optString("timestamp").toLongOrNull()
                ?: (System.currentTimeMillis() / 1000L - i * 86_400L)
            if (i == 0) {
                nextUpdateSec = item.optString("time_until_update").toLongOrNull()
            }
            points += FearGreedPoint(
                value = value,
                classification = classification,
                timestampSec = tsSec
            )
        }

        if (points.isEmpty()) {
            throw IllegalStateException("Could not parse Fear & Greed values")
        }

        val today = points.first()
        val yesterday = points.getOrNull(1)?.value
        val lastWeek = points.lastOrNull()?.takeIf { points.size >= 3 }?.value

        return FearGreedIndexData(
            apiValue = today.value,
            apiClassification = today.classification,
            liveValue = today.value,
            liveClassification = today.classification,
            liveMomentumAdjustment = 0,
            yesterdayValue = yesterday,
            lastWeekValue = lastWeek,
            history7d = points.reversed(),
            nextUpdateSec = nextUpdateSec,
            updatedAtMs = System.currentTimeMillis(),
            source = "Alternative.me FNG API"
        )
    }

    private fun fetchFearGreedFromCoinGeckoGlobal(): FearGreedIndexData {
        val body = fetchString("https://api.coingecko.com/api/v3/global")
        val data = JSONObject(body).getJSONObject("data")
        val mcapChange24h = data.optDouble("market_cap_change_percentage_24h_usd", 0.0)
        val btcDom = data.optJSONObject("market_cap_percentage")?.optDouble("btc", 50.0) ?: 50.0
        // Market cap momentum + BTC dominance proxy mapped onto 0..100 scale
        val momentumComponent = (50.0 + mcapChange24h * 6.5).coerceIn(5.0, 95.0)
        val dominanceComponent = (100.0 - (btcDom - 35.0) * 1.4).coerceIn(15.0, 85.0)
        val score = (momentumComponent * 0.72 + dominanceComponent * 0.28).toInt().coerceIn(0, 100)
        val label = classifyFearGreed(score)
        val nowSec = System.currentTimeMillis() / 1000L

        return FearGreedIndexData(
            apiValue = score,
            apiClassification = label,
            liveValue = score,
            liveClassification = label,
            liveMomentumAdjustment = 0,
            yesterdayValue = null,
            lastWeekValue = null,
            history7d = listOf(FearGreedPoint(score, label, nowSec)),
            nextUpdateSec = null,
            updatedAtMs = System.currentTimeMillis(),
            source = "CoinGecko Global API"
        )
    }

    /**
     * Fetches live USD -> CAD foreign exchange rate from public FX APIs
     * so Adaptive Trader accurately quotes & sizes trades in USD or CAD.
     */
    suspend fun fetchUsdToCadRate(): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val root = JSONObject(fetchString("https://api.frankfurter.app/latest?from=USD&to=CAD"))
                val cad = root.getJSONObject("rates").getDouble("CAD")
                if (cad in 0.8..2.5) cad else throw IllegalStateException("Out-of-range CAD FX")
            } catch (_: Exception) {
                val root = JSONObject(fetchString("https://open.er-api.com/v6/latest/USD"))
                val cad = root.getJSONObject("rates").getDouble("CAD")
                if (cad in 0.8..2.5) cad else 1.38
            }
        }
    }

    /**
     * Scans EVERY liquid spot market across the selected connected exchange(s)
     * (Kraken, KuCoin, Coinbase, Bitstamp, or all concurrently) to discover
     * profit-optimizing coins across the entire connected exchange universe.
     */
    suspend fun scanEntireExchangeMarket(
        exchange: ConnectedExchange,
        usdToCadRate: Double = 1.38
    ): Result<ExchangeMarketScanResult> = withContext(Dispatchers.IO) {
        runCatching {
            val venuesScanned = mutableListOf<String>()
            val allScanned = mutableListOf<ExchangeMarketTicker>()
            var totalRawMarkets = 0

            val tasks: List<Pair<String, suspend () -> Pair<Int, List<ExchangeMarketTicker>>>> = when (exchange) {
                ConnectedExchange.KRAKEN -> listOf("Kraken" to { scanKrakenFullExchange(usdToCadRate) })
                ConnectedExchange.KUCOIN -> listOf("KuCoin" to { scanKuCoinFullExchange() })
                ConnectedExchange.BITSTAMP -> listOf("Bitstamp" to { scanBitstampFullExchange() })
                ConnectedExchange.COINBASE -> listOf("Coinbase" to { scanCoinbaseFullExchange() })
                ConnectedExchange.ALL_CONNECTED -> listOf(
                    "Kraken" to { scanKrakenFullExchange(usdToCadRate) },
                    "Coinbase" to { scanCoinbaseFullExchange() },
                    "KuCoin" to { scanKuCoinFullExchange() },
                    "Bitstamp" to { scanBitstampFullExchange() }
                )
            }

            val results = coroutineScope {
                tasks.map { (venue, fn) ->
                    async(Dispatchers.IO) {
                        venue to runCatching { fn() }.getOrNull()
                    }
                }.awaitAll()
            }

            results.forEach { (venue, pair) ->
                if (pair != null && pair.second.isNotEmpty()) {
                    venuesScanned += venue
                    totalRawMarkets += pair.first
                    allScanned += pair.second
                }
            }

            // Fallback to KuCoin if selected single venue timed out
            if (allScanned.isEmpty()) {
                val fallback = scanKuCoinFullExchange()
                venuesScanned += "KuCoin"
                totalRawMarkets += fallback.first
                allScanned += fallback.second
            }

            // Merge across venues by base coin, picking the highest-volume/tightest venue per coin,
            // then rank by a momentum + intraday volatility + liquidity discovery score
            val bestPerCoin = allScanned
                .groupBy { it.symbol }
                .values
                .mapNotNull { list -> list.maxByOrNull { it.volume24hUsd } }
                .filter { it.priceUsd > 0.0 && it.volume24hUsd >= 150_000.0 }
                .sortedByDescending { t ->
                    val rangePct = if (t.low24h > 0.0) ((t.high24h - t.low24h) / t.low24h) * 100.0 else 0.0
                    val liqBonus = kotlin.math.ln(t.volume24hUsd.coerceAtLeast(1.0)) * 2.2
                    val momentumScore = t.change24hPct.coerceIn(-15.0, 35.0) * 1.4
                    val rangeBonus = rangePct.coerceIn(0.0, 28.0) * 0.9
                    val spreadPenalty = (t.spreadBps * 0.08).coerceAtMost(15.0)
                    momentumScore + rangeBonus + liqBonus - spreadPenalty
                }

            ExchangeMarketScanResult(
                exchange = exchange,
                totalMarketsScanned = maxOf(totalRawMarkets, bestPerCoin.size),
                venuesScanned = venuesScanned.ifEmpty { listOf(exchange.shortLabel) },
                candidates = bestPerCoin.take(24),
                scannedAtMs = System.currentTimeMillis()
            )
        }
    }

    private fun scanKuCoinFullExchange(): Pair<Int, List<ExchangeMarketTicker>> {
        val body = fetchString("https://api.kucoin.com/api/v1/market/allTickers")
        val tickerArr = JSONObject(body).getJSONObject("data").getJSONArray("ticker")
        val excluded = setOf("USDT", "USDC", "DAI", "TUSD", "BUSD", "FDUSD", "USDE", "EUR", "GBP", "PYUSD", "USD1", "PAXG")
        val out = mutableListOf<ExchangeMarketTicker>()
        val totalCount = tickerArr.length()

        for (i in 0 until totalCount) {
            val obj = tickerArr.getJSONObject(i)
            val pair = obj.optString("symbol")
            if (!pair.endsWith("-USDT") && !pair.endsWith("-USD")) continue
            val base = pair.substringBefore("-").uppercase()
            if (base.isBlank() || base in excluded || base.endsWith("3L") || base.endsWith("3S") || base.endsWith("UP") || base.endsWith("DOWN")) continue

            val price = obj.optString("last", "0").toDoubleOrNull() ?: 0.0
            val volUsd = obj.optString("volValue", "0").toDoubleOrNull() ?: 0.0
            if (price <= 0.0 || volUsd < 200_000.0) continue

            val changePct = (obj.optString("changeRate", "0").toDoubleOrNull() ?: 0.0) * 100.0
            val high = obj.optString("high", "0").toDoubleOrNull() ?: price
            val low = obj.optString("low", "0").toDoubleOrNull() ?: price
            val buy = obj.optString("buy", "0").toDoubleOrNull() ?: price
            val sell = obj.optString("sell", "0").toDoubleOrNull() ?: price
            val spreadBps = if (buy > 0.0 && sell >= buy) ((sell - buy) / buy) * 10_000.0 else 15.0

            out += ExchangeMarketTicker(
                symbol = base,
                quoteCurrency = pair.substringAfter("-"),
                exchangeName = "KuCoin",
                pairLabel = pair,
                priceUsd = price,
                change24hPct = changePct,
                volume24hUsd = volUsd,
                high24h = high,
                low24h = low,
                spreadBps = spreadBps
            )
        }
        return totalCount to out
    }

    private fun scanKrakenFullExchange(usdToCadRate: Double): Pair<Int, List<ExchangeMarketTicker>> {
        val root = JSONObject(fetchString("https://api.kraken.com/0/public/Ticker"))
        val res = root.getJSONObject("result")
        val excluded = setOf("USDT", "USDC", "DAI", "USD", "CAD", "EUR", "GBP", "ZUSD", "ZCAD", "ZEUR", "PYUSD", "USDG")
        val out = mutableListOf<ExchangeMarketTicker>()
        val keys = res.keys().asSequence().toList()

        keys.forEach { rawPair ->
            val isUsd = rawPair.endsWith("ZUSD") || rawPair.endsWith("USD")
            val isCad = rawPair.endsWith("ZCAD") || rawPair.endsWith("CAD")
            if (!isUsd && !isCad) return@forEach

            val quote = if (isCad) "CAD" else "USD"
            val rawBase = rawPair
                .removeSuffix("ZUSD")
                .removeSuffix("ZCAD")
                .removeSuffix("USD")
                .removeSuffix("CAD")
            val base = normalizeKrakenBase(rawBase)
            if (base.isBlank() || base in excluded || base.length > 8) return@forEach

            val obj = res.optJSONObject(rawPair) ?: return@forEach
            val lastQuote = obj.optJSONArray("c")?.optString(0)?.toDoubleOrNull() ?: 0.0
            val openQuote = obj.optString("o", "0").toDoubleOrNull() ?: 0.0
            val volBase = obj.optJSONArray("v")?.optString(1)?.toDoubleOrNull() ?: 0.0
            val highQuote = obj.optJSONArray("h")?.optString(1)?.toDoubleOrNull() ?: lastQuote
            val lowQuote = obj.optJSONArray("l")?.optString(1)?.toDoubleOrNull() ?: lastQuote
            val askQuote = obj.optJSONArray("a")?.optString(0)?.toDoubleOrNull() ?: lastQuote
            val bidQuote = obj.optJSONArray("b")?.optString(0)?.toDoubleOrNull() ?: lastQuote

            val fxDiv = if (isCad && usdToCadRate > 0.5) usdToCadRate else 1.0
            val priceUsd = lastQuote / fxDiv
            val volUsd = volBase * priceUsd
            if (priceUsd <= 0.0 || volUsd < 100_000.0) return@forEach

            val changePct = if (openQuote > 0.0) ((lastQuote - openQuote) / openQuote) * 100.0 else 0.0
            val spreadBps = if (bidQuote > 0.0 && askQuote >= bidQuote) ((askQuote - bidQuote) / bidQuote) * 10_000.0 else 18.0

            out += ExchangeMarketTicker(
                symbol = base,
                quoteCurrency = quote,
                exchangeName = "Kraken",
                pairLabel = "$base/$quote",
                priceUsd = priceUsd,
                change24hPct = changePct,
                volume24hUsd = volUsd,
                high24h = highQuote / fxDiv,
                low24h = lowQuote / fxDiv,
                spreadBps = spreadBps
            )
        }
        return keys.size to out
    }

    private fun scanBitstampFullExchange(): Pair<Int, List<ExchangeMarketTicker>> {
        val arr = JSONArray(fetchString("https://www.bitstamp.net/api/v2/ticker/"))
        val excluded = setOf("USDT", "USDC", "EUR", "GBP", "DAI", "PYUSD", "RLUSD")
        val out = mutableListOf<ExchangeMarketTicker>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val pair = obj.optString("pair")
            if (!pair.endsWith("/USD")) continue
            val base = pair.substringBefore("/").uppercase()
            if (base.isBlank() || base in excluded) continue

            val price = obj.optString("last", "0").toDoubleOrNull() ?: 0.0
            val volBase = obj.optString("volume", "0").toDoubleOrNull() ?: 0.0
            val volUsd = volBase * price
            if (price <= 0.0 || volUsd < 100_000.0) continue

            val changePct = obj.optString("percent_change_24", "0").toDoubleOrNull() ?: 0.0
            val high = obj.optString("high", "0").toDoubleOrNull() ?: price
            val low = obj.optString("low", "0").toDoubleOrNull() ?: price
            val bid = obj.optString("bid", "0").toDoubleOrNull() ?: price
            val ask = obj.optString("ask", "0").toDoubleOrNull() ?: price
            val spreadBps = if (bid > 0.0 && ask >= bid) ((ask - bid) / bid) * 10_000.0 else 18.0

            out += ExchangeMarketTicker(
                symbol = base,
                quoteCurrency = "USD",
                exchangeName = "Bitstamp",
                pairLabel = pair,
                priceUsd = price,
                change24hPct = changePct,
                volume24hUsd = volUsd,
                high24h = high,
                low24h = low,
                spreadBps = spreadBps
            )
        }
        return arr.length() to out
    }

    private suspend fun scanCoinbaseFullExchange(): Pair<Int, List<ExchangeMarketTicker>> = coroutineScope {
        val productsArr = JSONArray(fetchString("https://api.exchange.coinbase.com/products"))
        val excluded = setOf("USDT", "USDC", "DAI", "PYUSD", "EURC", "GUSD", "PAX")
        val usdProducts = mutableListOf<Pair<String, String>>()
        for (i in 0 until productsArr.length()) {
            val obj = productsArr.getJSONObject(i)
            val quote = obj.optString("quote_currency")
            val base = obj.optString("base_currency").uppercase()
            val status = obj.optString("status")
            if (quote == "USD" && status == "online" && base !in excluded && !obj.optBoolean("trading_disabled", false)) {
                usdProducts += base to obj.optString("id")
            }
        }
        val prioritized = usdProducts.sortedBy { (base, _) ->
            if (CoinAssets.find(base) != null) 0 else 1
        }.take(24)

        val tickers = prioritized.map { (base, id) ->
            async(Dispatchers.IO) {
                runCatching {
                    val st = JSONObject(fetchString("https://api.exchange.coinbase.com/products/$id/stats"))
                    val last = st.optString("last", "0").toDoubleOrNull() ?: 0.0
                    val open = st.optString("open", "0").toDoubleOrNull() ?: 0.0
                    val volBase = st.optString("volume", "0").toDoubleOrNull() ?: 0.0
                    val high = st.optString("high", "0").toDoubleOrNull() ?: last
                    val low = st.optString("low", "0").toDoubleOrNull() ?: last
                    val volUsd = volBase * last
                    if (last <= 0.0 || volUsd < 100_000.0) return@runCatching null
                    val chg = if (open > 0.0) ((last - open) / open) * 100.0 else 0.0
                    ExchangeMarketTicker(
                        symbol = base,
                        quoteCurrency = "USD",
                        exchangeName = "Coinbase",
                        pairLabel = id,
                        priceUsd = last,
                        change24hPct = chg,
                        volume24hUsd = volUsd,
                        high24h = high,
                        low24h = low,
                        spreadBps = 12.0
                    )
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()

        usdProducts.size to tickers
    }

    /**
     * Fetches real-time primary-pair spot market data concurrently from the requested exchanges
     * (Kraken, Coinbase, KuCoin, Bitstamp) for the Price Summary Dashboard list.
     */
    suspend fun fetchPrimaryPairsForExchanges(
        exchangeIds: List<String>,
        configuredExchangeIds: Set<String>,
        symbols: List<String> = DEFAULT_PRIMARY_PAIRS
    ): Result<List<com.example.viewmodel.ConfiguredExchangePriceRow>> = withContext(Dispatchers.IO) {
        runCatching {
            val targets = exchangeIds.map { it.lowercase() }.distinct()
            val symbolOrder = symbols.withIndex().associate { it.value to it.index }
            val fetched = coroutineScope {
                targets.map { exId ->
                    async(Dispatchers.IO) {
                        val isConfigured = exId in configuredExchangeIds
                        runCatching {
                            when (exId) {
                                "kraken" -> fetchKrakenPrimaryPairs(symbols, isConfigured)
                                "coinbase" -> fetchCoinbasePrimaryPairs(symbols, isConfigured)
                                "kucoin" -> fetchKuCoinPrimaryPairs(symbols, isConfigured)
                                "bitstamp" -> fetchBitstampPrimaryPairs(symbols, isConfigured)
                                else -> emptyList()
                            }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }
            if (fetched.isEmpty()) {
                throw IllegalStateException("Live exchange price feeds temporarily unavailable")
            }
            fetched.sortedWith(
                compareBy<com.example.viewmodel.ConfiguredExchangePriceRow>(
                    { symbolOrder[it.symbol] ?: 99 },
                    { targets.indexOf(it.exchangeId).let { idx -> if (idx < 0) 99 else idx } }
                )
            )
        }
    }

    private fun fetchKrakenPrimaryPairs(
        symbols: List<String>,
        isApiConfigured: Boolean
    ): List<com.example.viewmodel.ConfiguredExchangePriceRow> {
        val krakenPairQuery = symbols.joinToString(",") { sym ->
            when (sym.uppercase()) {
                "BTC" -> "XBTUSD"
                "DOGE" -> "XDGUSD"
                else -> "${sym.uppercase()}USD"
            }
        }
        val root = JSONObject(fetchString("https://api.kraken.com/0/public/Ticker?pair=$krakenPairQuery"))
        val errs = root.optJSONArray("error")
        if (errs != null && errs.length() > 0) {
            throw IllegalStateException("Kraken Ticker: ${errs.optString(0)}")
        }
        val res = root.optJSONObject("result") ?: return emptyList()
        val now = System.currentTimeMillis()
        val symbolSet = symbols.map { it.uppercase() }.toSet()
        val out = mutableListOf<com.example.viewmodel.ConfiguredExchangePriceRow>()
        val keys = res.keys()
        while (keys.hasNext()) {
            val rawPair = keys.next()
            val rawBase = rawPair.removeSuffix("ZUSD").removeSuffix("USD")
            val sym = normalizeKrakenBase(rawBase)
            if (sym !in symbolSet) continue
            val obj = res.optJSONObject(rawPair) ?: continue
            val last = obj.optJSONArray("c")?.optString(0)?.toDoubleOrNull() ?: 0.0
            if (last <= 0.0) continue
            val open = obj.optString("o", "0").toDoubleOrNull() ?: 0.0
            val volBase = obj.optJSONArray("v")?.optString(1)?.toDoubleOrNull() ?: 0.0
            val high = obj.optJSONArray("h")?.optString(1)?.toDoubleOrNull() ?: last
            val low = obj.optJSONArray("l")?.optString(1)?.toDoubleOrNull() ?: last
            val changePct = if (open > 0.0) ((last - open) / open) * 100.0 else 0.0
            out += com.example.viewmodel.ConfiguredExchangePriceRow(
                id = "kraken_$sym",
                exchangeId = "kraken",
                exchangeName = "Kraken Pro",
                symbol = sym,
                pairLabel = "$sym/USD",
                priceUsd = last,
                change24hPct = changePct,
                high24h = high,
                low24h = low,
                volume24hUsd = volBase * last,
                isApiConfigured = isApiConfigured,
                fetchedAtMs = now
            )
        }
        return out
    }

    private suspend fun fetchCoinbasePrimaryPairs(
        symbols: List<String>,
        isApiConfigured: Boolean
    ): List<com.example.viewmodel.ConfiguredExchangePriceRow> = coroutineScope {
        val now = System.currentTimeMillis()
        symbols.map { rawSym ->
            val sym = rawSym.uppercase()
            async(Dispatchers.IO) {
                runCatching {
                    val productId = "$sym-USD"
                    val st = JSONObject(fetchString("https://api.exchange.coinbase.com/products/$productId/stats"))
                    val last = st.optString("last", "0").toDoubleOrNull() ?: 0.0
                    if (last <= 0.0) return@runCatching null
                    val open = st.optString("open", "0").toDoubleOrNull() ?: 0.0
                    val volBase = st.optString("volume", "0").toDoubleOrNull() ?: 0.0
                    val high = st.optString("high", "0").toDoubleOrNull() ?: last
                    val low = st.optString("low", "0").toDoubleOrNull() ?: last
                    val changePct = if (open > 0.0) ((last - open) / open) * 100.0 else 0.0
                    com.example.viewmodel.ConfiguredExchangePriceRow(
                        id = "coinbase_$sym",
                        exchangeId = "coinbase",
                        exchangeName = "Coinbase",
                        symbol = sym,
                        pairLabel = "$sym/USD",
                        priceUsd = last,
                        change24hPct = changePct,
                        high24h = high,
                        low24h = low,
                        volume24hUsd = volBase * last,
                        isApiConfigured = isApiConfigured,
                        fetchedAtMs = now
                    )
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private fun fetchKuCoinPrimaryPairs(
        symbols: List<String>,
        isApiConfigured: Boolean
    ): List<com.example.viewmodel.ConfiguredExchangePriceRow> {
        val body = fetchString("https://api.kucoin.com/api/v1/market/allTickers")
        val arr = JSONObject(body).getJSONObject("data").getJSONArray("ticker")
        val targetPairs = symbols.associate { "${it.uppercase()}-USDT" to it.uppercase() }
        val now = System.currentTimeMillis()
        val out = mutableListOf<com.example.viewmodel.ConfiguredExchangePriceRow>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val pair = obj.optString("symbol")
            val sym = targetPairs[pair] ?: continue
            val last = obj.optString("last", "0").toDoubleOrNull() ?: 0.0
            if (last <= 0.0) continue
            val changePct = (obj.optString("changeRate", "0").toDoubleOrNull() ?: 0.0) * 100.0
            val high = obj.optString("high", "0").toDoubleOrNull() ?: last
            val low = obj.optString("low", "0").toDoubleOrNull() ?: last
            val volUsd = obj.optString("volValue", "0").toDoubleOrNull() ?: 0.0
            out += com.example.viewmodel.ConfiguredExchangePriceRow(
                id = "kucoin_$sym",
                exchangeId = "kucoin",
                exchangeName = "KuCoin",
                symbol = sym,
                pairLabel = "$sym/USDT",
                priceUsd = last,
                change24hPct = changePct,
                high24h = high,
                low24h = low,
                volume24hUsd = volUsd,
                isApiConfigured = isApiConfigured,
                fetchedAtMs = now
            )
        }
        return out
    }

    private fun fetchBitstampPrimaryPairs(
        symbols: List<String>,
        isApiConfigured: Boolean
    ): List<com.example.viewmodel.ConfiguredExchangePriceRow> {
        val arr = JSONArray(fetchString("https://www.bitstamp.net/api/v2/ticker/"))
        val targetPairs = symbols.associate { "${it.uppercase()}/USD" to it.uppercase() }
        val now = System.currentTimeMillis()
        val out = mutableListOf<com.example.viewmodel.ConfiguredExchangePriceRow>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val pair = obj.optString("pair")
            val sym = targetPairs[pair] ?: continue
            val last = obj.optString("last", "0").toDoubleOrNull() ?: 0.0
            if (last <= 0.0) continue
            val changePct = obj.optString("percent_change_24", "0").toDoubleOrNull() ?: 0.0
            val high = obj.optString("high", "0").toDoubleOrNull() ?: last
            val low = obj.optString("low", "0").toDoubleOrNull() ?: last
            val volBase = obj.optString("volume", "0").toDoubleOrNull() ?: 0.0
            out += com.example.viewmodel.ConfiguredExchangePriceRow(
                id = "bitstamp_$sym",
                exchangeId = "bitstamp",
                exchangeName = "Bitstamp",
                symbol = sym,
                pairLabel = pair,
                priceUsd = last,
                change24hPct = changePct,
                high24h = high,
                low24h = low,
                volume24hUsd = volBase * last,
                isApiConfigured = isApiConfigured,
                fetchedAtMs = now
            )
        }
        return out
    }

    /**
     * Fetches EVERY tradable cryptocurrency offered live by the specified exchanges
     * (Kraken AssetPairs, Coinbase Products, KuCoin Symbols, Bitstamp Trading Pairs)
     * for populating searchable coin dropdown lists across the entire mobile app.
     */
    suspend fun fetchFullExchangeCoinCatalog(
        exchangeIds: List<String>
    ): Result<List<com.example.core.ExchangeListedCoin>> = withContext(Dispatchers.IO) {
        runCatching {
            val targets = exchangeIds
                .map { it.trim().lowercase() }
                .filter { it in setOf("kraken", "coinbase", "kucoin", "bitstamp") }
                .distinct()
                .ifEmpty { listOf("kraken") }

            val perExchangeResults = coroutineScope {
                targets.map { exId ->
                    async(Dispatchers.IO) {
                        exId to runCatching {
                            when (exId) {
                                "kraken" -> fetchKrakenAllListedCoins()
                                "coinbase" -> fetchCoinbaseAllListedCoins()
                                "kucoin" -> fetchKuCoinAllListedCoins()
                                "bitstamp" -> fetchBitstampAllListedCoins()
                                else -> emptyList()
                            }
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll()
            }

            // Merge coins across requested exchanges, preserving preferred USD/USDT pairs
            val exchangesBySymbol = LinkedHashMap<String, MutableSet<String>>()
            val pairBySymbol = LinkedHashMap<String, String>()

            perExchangeResults.forEach { (_, list) ->
                list.forEach { (sym, pair, venue) ->
                    val cleanSym = sym.trim().uppercase()
                    if (cleanSym.isBlank()) return@forEach
                    exchangesBySymbol.getOrPut(cleanSym) { LinkedHashSet() }.add(venue)
                    val existingPair = pairBySymbol[cleanSym]
                    if (existingPair == null ||
                        (!existingPair.endsWith("/USD") && pair.endsWith("/USD"))
                    ) {
                        pairBySymbol[cleanSym] = pair
                    }
                }
            }

            val featuredOrder = CoinAssets.catalog.mapIndexed { idx, asset -> asset.symbol to idx }.toMap()

            val merged = exchangesBySymbol.entries.map { (sym, venues) ->
                val known = CoinAssets.find(sym)
                com.example.core.ExchangeListedCoin(
                    symbol = sym,
                    name = known?.name ?: sym,
                    exchanges = venues.toList(),
                    primaryPair = pairBySymbol[sym] ?: "$sym/USD"
                )
            }.sortedWith(
                compareBy<com.example.core.ExchangeListedCoin>(
                    { featuredOrder[it.symbol] ?: Int.MAX_VALUE },
                    { it.symbol }
                )
            )

            if (merged.isEmpty()) {
                throw IllegalStateException("Exchange coin directory temporarily unreachable")
            }
            merged
        }
    }

    private fun fetchKrakenAllListedCoins(): List<Triple<String, String, String>> {
        val root = JSONObject(fetchString("https://api.kraken.com/0/public/AssetPairs"))
        val res = root.optJSONObject("result") ?: return emptyList()
        val fiatBases = setOf("USD", "ZUSD", "EUR", "ZEUR", "GBP", "ZGBP", "CAD", "ZCAD", "JPY", "ZJPY", "CHF", "AUD", "ZAUD")
        val pairByCoin = LinkedHashMap<String, String>()

        res.keys().forEach { key ->
            val obj = res.optJSONObject(key) ?: return@forEach
            val status = obj.optString("status", "online")
            if (status != "online") return@forEach

            val wsname = obj.optString("wsname")
            val rawBase = if (wsname.contains("/")) wsname.substringBefore("/") else obj.optString("base")
            val rawQuote = if (wsname.contains("/")) wsname.substringAfter("/") else obj.optString("quote")
            val base = normalizeKrakenBase(rawBase).trim()
            val quote = normalizeKrakenBase(rawQuote).removePrefix("Z").trim().ifBlank { "USD" }

            if (base.isBlank() || base in fiatBases || base.contains(".")) return@forEach
            val pairLabel = "$base/$quote"
            val existing = pairByCoin[base]
            if (existing == null ||
                (!existing.endsWith("/USD") && quote == "USD") ||
                (!existing.endsWith("/USD") && !existing.endsWith("/USDT") && quote == "USDT")
            ) {
                pairByCoin[base] = pairLabel
            }
        }
        return pairByCoin.map { (sym, pair) -> Triple(sym, pair, "Kraken") }
    }

    private fun fetchCoinbaseAllListedCoins(): List<Triple<String, String, String>> {
        val arr = JSONArray(fetchString("https://api.exchange.coinbase.com/products"))
        val fiatBases = setOf("USD", "EUR", "GBP", "CAD", "JPY", "CHF", "AUD")
        val pairByCoin = LinkedHashMap<String, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("status") != "online" || obj.optBoolean("trading_disabled", false)) continue
            val base = obj.optString("base_currency").uppercase().trim()
            val quote = obj.optString("quote_currency").uppercase().trim().ifBlank { "USD" }
            if (base.isBlank() || base in fiatBases) continue
            val pairLabel = "$base/$quote"
            val existing = pairByCoin[base]
            if (existing == null || (!existing.endsWith("/USD") && quote == "USD")) {
                pairByCoin[base] = pairLabel
            }
        }
        return pairByCoin.map { (sym, pair) -> Triple(sym, pair, "Coinbase") }
    }

    private fun fetchKuCoinAllListedCoins(): List<Triple<String, String, String>> {
        val root = JSONObject(fetchString("https://api.kucoin.com/api/v2/symbols"))
        val arr = root.optJSONArray("data") ?: return emptyList()
        val fiatBases = setOf("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "BRL", "TRY")
        val pairByCoin = LinkedHashMap<String, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (!obj.optBoolean("enableTrading", true)) continue
            val base = obj.optString("baseCurrency").uppercase().trim()
            val quote = obj.optString("quoteCurrency").uppercase().trim().ifBlank { "USDT" }
            if (base.isBlank() || base in fiatBases) continue
            if (base.endsWith("3L") || base.endsWith("3S") || base.endsWith("UP") || base.endsWith("DOWN")) continue
            val pairLabel = "$base/$quote"
            val existing = pairByCoin[base]
            if (existing == null ||
                (!existing.endsWith("/USD") && quote == "USD") ||
                (!existing.endsWith("/USD") && !existing.endsWith("/USDT") && quote == "USDT")
            ) {
                pairByCoin[base] = pairLabel
            }
        }
        return pairByCoin.map { (sym, pair) -> Triple(sym, pair, "KuCoin") }
    }

    private fun fetchBitstampAllListedCoins(): List<Triple<String, String, String>> {
        val arr = JSONArray(fetchString("https://www.bitstamp.net/api/v2/trading-pairs-info/"))
        val fiatBases = setOf("USD", "EUR", "GBP", "CAD", "CHF")
        val pairByCoin = LinkedHashMap<String, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (!obj.optString("trading").equals("Enabled", ignoreCase = true)) continue
            val name = obj.optString("name")
            if (!name.contains("/")) continue
            val base = name.substringBefore("/").uppercase().trim()
            val quote = name.substringAfter("/").uppercase().trim().ifBlank { "USD" }
            if (base.isBlank() || base in fiatBases) continue
            val pairLabel = "$base/$quote"
            val existing = pairByCoin[base]
            if (existing == null || (!existing.endsWith("/USD") && quote == "USD")) {
                pairByCoin[base] = pairLabel
            }
        }
        return pairByCoin.map { (sym, pair) -> Triple(sym, pair, "Bitstamp") }
    }

    private fun normalizeKrakenBase(rawBase: String): String = when (rawBase.uppercase()) {
        "XXBT", "XBT" -> "BTC"
        "XETH" -> "ETH"
        "XXRP" -> "XRP"
        "XLTC" -> "LTC"
        "XXLM" -> "XLM"
        "XDG", "XXDG" -> "DOGE"
        "XZEC" -> "ZEC"
        "XETC" -> "ETC"
        "XXMR" -> "XMR"
        "XREP" -> "REP"
        else -> rawBase.uppercase()
    }

    companion object {
        val DEFAULT_PRIMARY_PAIRS = listOf("BTC", "ETH", "SOL", "XRP", "ADA", "DOGE")

        fun classifyFearGreed(score: Int): String = when {
            score < 25 -> "Extreme Fear"
            score < 45 -> "Fear"
            score <= 55 -> "Neutral"
            score < 75 -> "Greed"
            else -> "Extreme Greed"
        }
    }
}

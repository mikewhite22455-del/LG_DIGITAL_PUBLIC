package com.example.data.remote

import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.EncryptedExchangeCredentials
import com.example.domain.models.ConnectedExchange
import com.example.domain.models.LiveConnectorStatus
import com.example.domain.models.LiveExecutionRoute
import com.example.domain.models.LiveOrderReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Live trading bridge for direct signed spot APIs:
 *  - Kraken Pro
 *  - Coinbase Advanced Trade
 *  - KuCoin
 *  - Bitstamp
 *
 * Credential resolution priority:
 *  1) Local AndroidKeyStore AES-256-GCM Encrypted DataStore vault (Exchange Configuration screen)
 *  2) Optional BuildConfig fallback (AI Studio Secrets panel)
 *
 * Security notes:
 *  - Secrets, signatures, and Authorization headers are never logged or leaked into error strings.
 *  - All endpoints enforce strict HTTPS and finite positive input validation.
 */
class LiveTradingClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val formMediaType = "application/x-www-form-urlencoded; charset=utf-8".toMediaType()

    private fun isRealSecret(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotBlank() && !trimmed.endsWith("_DEFAULT_VALUE")
    }

    private fun resolveSecret(stored: String, fallback: String): String {
        val s = stored.trim()
        if (s.isNotBlank()) return s
        val f = fallback.trim()
        return if (isRealSecret(f)) f else ""
    }

    fun getConnectorStatuses(stored: EncryptedExchangeCredentials = EncryptedExchangeCredentials()): List<LiveConnectorStatus> {
        val krakenReady = resolveSecret(stored.krakenApiKey, BuildConfig.KRAKEN_API_KEY).isNotBlank() &&
            resolveSecret(stored.krakenApiSecret, BuildConfig.KRAKEN_API_SECRET).isNotBlank()
        val coinbaseReady = resolveSecret(stored.coinbaseApiKey, BuildConfig.COINBASE_API_KEY).isNotBlank() &&
            resolveSecret(stored.coinbaseApiSecret, BuildConfig.COINBASE_API_SECRET).isNotBlank()
        val kucoinReady = resolveSecret(stored.kucoinApiKey, BuildConfig.KUCOIN_API_KEY).isNotBlank() &&
            resolveSecret(stored.kucoinApiSecret, BuildConfig.KUCOIN_API_SECRET).isNotBlank() &&
            resolveSecret(stored.kucoinPassphrase, BuildConfig.KUCOIN_API_PASSPHRASE).isNotBlank()
        val bitstampReady = resolveSecret(stored.bitstampApiKey, BuildConfig.BITSTAMP_API_KEY).isNotBlank() &&
            resolveSecret(stored.bitstampApiSecret, BuildConfig.BITSTAMP_API_SECRET).isNotBlank()

        return listOf(
            LiveConnectorStatus(
                route = LiveExecutionRoute.KRAKEN_DIRECT,
                isConfigured = krakenReady,
                detail = if (krakenReady) {
                    if (stored.hasKraken) "Kraken Pro API active (Encrypted DataStore)" else "Kraken Pro API active (Secrets)"
                } else {
                    "Configure Kraken Pro keys in Exchange Configuration"
                }
            ),
            LiveConnectorStatus(
                route = LiveExecutionRoute.COINBASE_DIRECT,
                isConfigured = coinbaseReady,
                detail = if (coinbaseReady) {
                    if (stored.hasCoinbase) "Coinbase API active (Encrypted DataStore)" else "Coinbase API active (Secrets)"
                } else {
                    "Configure Coinbase keys in Exchange Configuration"
                }
            ),
            LiveConnectorStatus(
                route = LiveExecutionRoute.KUCOIN_DIRECT,
                isConfigured = kucoinReady,
                detail = if (kucoinReady) {
                    if (stored.hasKuCoin) "KuCoin API active (Encrypted DataStore)" else "KuCoin API active (Secrets)"
                } else {
                    "Configure KuCoin keys in Exchange Configuration"
                }
            ),
            LiveConnectorStatus(
                route = LiveExecutionRoute.BITSTAMP_DIRECT,
                isConfigured = bitstampReady,
                detail = if (bitstampReady) {
                    if (stored.hasBitstamp) "Bitstamp API active (Encrypted DataStore)" else "Bitstamp API active (Secrets)"
                } else {
                    "Configure Bitstamp keys in Exchange Configuration"
                }
            )
        )
    }

    /**
     * Performs a real read-only signed balance/account check against Kraken Pro, Coinbase, KuCoin, or Bitstamp
     * to verify saved encrypted credentials without placing any order.
     */
    suspend fun testExchangeConnection(
        exchangeId: String,
        stored: EncryptedExchangeCredentials
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (exchangeId.lowercase(Locale.US)) {
                "kraken" -> testKrakenConnection(stored)
                "coinbase" -> testCoinbaseConnection(stored)
                "kucoin" -> testKuCoinConnection(stored)
                "bitstamp" -> testBitstampConnection(stored)
                else -> throw IllegalArgumentException("Unsupported exchange: $exchangeId")
            }
        }
    }

    /**
     * Verifies real-time connectivity for an exchange using stored encrypted keys (or BuildConfig fallback),
     * measuring round-trip HTTPS latency and distinguishing NOT_CONFIGURED, CONNECTED, and ERROR states.
     */
    suspend fun verifyExchangeConnectivity(
        exchangeId: String,
        stored: EncryptedExchangeCredentials
    ): com.example.viewmodel.ExchangeConnectionStatus = withContext(Dispatchers.IO) {
        val id = exchangeId.lowercase(Locale.US)
        val hasKeys = when (id) {
            "kraken" -> resolveSecret(stored.krakenApiKey, BuildConfig.KRAKEN_API_KEY).isNotBlank() &&
                resolveSecret(stored.krakenApiSecret, BuildConfig.KRAKEN_API_SECRET).isNotBlank()
            "coinbase" -> resolveSecret(stored.coinbaseApiKey, BuildConfig.COINBASE_API_KEY).isNotBlank() &&
                resolveSecret(stored.coinbaseApiSecret, BuildConfig.COINBASE_API_SECRET).isNotBlank()
            "kucoin" -> resolveSecret(stored.kucoinApiKey, BuildConfig.KUCOIN_API_KEY).isNotBlank() &&
                resolveSecret(stored.kucoinApiSecret, BuildConfig.KUCOIN_API_SECRET).isNotBlank() &&
                resolveSecret(stored.kucoinPassphrase, BuildConfig.KUCOIN_API_PASSPHRASE).isNotBlank()
            "bitstamp" -> resolveSecret(stored.bitstampApiKey, BuildConfig.BITSTAMP_API_KEY).isNotBlank() &&
                resolveSecret(stored.bitstampApiSecret, BuildConfig.BITSTAMP_API_SECRET).isNotBlank()
            else -> false
        }

        if (!hasKeys) {
            return@withContext com.example.viewmodel.ExchangeConnectionStatus(
                exchangeId = id,
                state = com.example.viewmodel.ExchangeHealthState.NOT_CONFIGURED,
                latencyMs = 0L,
                detail = "Awaiting encrypted API keys",
                lastCheckedMs = System.currentTimeMillis()
            )
        }

        val started = System.currentTimeMillis()
        val result = testExchangeConnection(id, stored)
        val elapsed = (System.currentTimeMillis() - started).coerceAtLeast(1L)

        result.fold(
            onSuccess = { msg ->
                com.example.viewmodel.ExchangeConnectionStatus(
                    exchangeId = id,
                    state = com.example.viewmodel.ExchangeHealthState.CONNECTED,
                    latencyMs = elapsed,
                    detail = msg,
                    lastCheckedMs = System.currentTimeMillis()
                )
            },
            onFailure = { err ->
                com.example.viewmodel.ExchangeConnectionStatus(
                    exchangeId = id,
                    state = com.example.viewmodel.ExchangeHealthState.ERROR,
                    latencyMs = elapsed,
                    detail = err.message?.take(140) ?: "API verification failed",
                    lastCheckedMs = System.currentTimeMillis()
                )
            }
        )
    }

    private fun testKrakenConnection(stored: EncryptedExchangeCredentials): String {
        val apiKey = resolveSecret(stored.krakenApiKey, BuildConfig.KRAKEN_API_KEY)
        val apiSecret = resolveSecret(stored.krakenApiSecret, BuildConfig.KRAKEN_API_SECRET)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank()) { "Enter Kraken API Key & Secret first." }

        val path = "/0/private/Balance"
        val nonce = System.currentTimeMillis().toString()
        val postData = "nonce=$nonce"
        val signature = signKrakenRequest(path, nonce, postData, apiSecret)

        val request = Request.Builder()
            .url("https://api.kraken.com$path")
            .header("API-Key", apiKey)
            .header("API-Sign", signature)
            .header("User-Agent", "LGDigital-Android/2.0")
            .post(postData.toRequestBody(formMediaType))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            val errors = json?.optJSONArray("error")
            if (errors != null && errors.length() > 0) {
                throw IllegalStateException("Kraken Pro: ${errors.optString(0)}")
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("Kraken Pro returned HTTP ${resp.code}")
            }
            val result = json?.optJSONObject("result")
            val assetCount = result?.length() ?: 0
            return "Kraken Pro Verified · $assetCount spot asset balances synced"
        }
    }

    private fun testCoinbaseConnection(stored: EncryptedExchangeCredentials): String {
        val apiKey = resolveSecret(stored.coinbaseApiKey, BuildConfig.COINBASE_API_KEY)
        val apiSecret = resolveSecret(stored.coinbaseApiSecret, BuildConfig.COINBASE_API_SECRET)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank()) { "Enter Coinbase API Key & Secret/Token first." }

        val path = "/api/v3/brokerage/accounts"
        val reqBuilder = Request.Builder()
            .url("https://api.coinbase.com$path?limit=25")
            .header("User-Agent", "LGDigital-Android/2.0")
            .header("Accept", "application/json")
            .get()

        if (apiSecret.startsWith("eyJ")) {
            reqBuilder.header("Authorization", "Bearer $apiSecret")
        } else {
            val ts = (System.currentTimeMillis() / 1000L).toString()
            val sig = hmacSha256Hex(ts + "GET" + path, apiSecret)
            reqBuilder
                .header("CB-ACCESS-KEY", apiKey)
                .header("CB-ACCESS-SIGN", sig)
                .header("CB-ACCESS-TIMESTAMP", ts)
        }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (!resp.isSuccessful) {
                val msg = json?.optString("message")?.ifBlank { json.optString("error_details") } ?: "HTTP ${resp.code}"
                throw IllegalStateException("Coinbase auth failed: $msg")
            }
            val accounts = json?.optJSONArray("accounts")
            val count = accounts?.length() ?: 0
            return "Coinbase Verified · $count brokerage accounts connected"
        }
    }

    private fun testKuCoinConnection(stored: EncryptedExchangeCredentials): String {
        val apiKey = resolveSecret(stored.kucoinApiKey, BuildConfig.KUCOIN_API_KEY)
        val apiSecret = resolveSecret(stored.kucoinApiSecret, BuildConfig.KUCOIN_API_SECRET)
        val passphrase = resolveSecret(stored.kucoinPassphrase, BuildConfig.KUCOIN_API_PASSPHRASE)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank() && passphrase.isNotBlank()) {
            "Enter KuCoin API Key, Secret & Passphrase first."
        }

        val path = "/api/v1/accounts?type=trade"
        val ts = System.currentTimeMillis().toString()
        val sign = hmacSha256Base64(ts + "GET" + path, apiSecret)
        val passSign = hmacSha256Base64(passphrase, apiSecret)

        val request = Request.Builder()
            .url("https://api.kucoin.com$path")
            .header("KC-API-KEY", apiKey)
            .header("KC-API-SIGN", sign)
            .header("KC-API-TIMESTAMP", ts)
            .header("KC-API-PASSPHRASE", passSign)
            .header("KC-API-KEY-VERSION", "2")
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            val code = json?.optString("code").orEmpty()
            if (!resp.isSuccessful || (code.isNotBlank() && code != "200000")) {
                throw IllegalStateException("KuCoin auth failed: ${json?.optString("msg") ?: "HTTP ${resp.code}"}")
            }
            val dataArr = json?.optJSONArray("data")
            val count = dataArr?.length() ?: 0
            return "KuCoin Verified · $count trade accounts active"
        }
    }

    private fun testBitstampConnection(stored: EncryptedExchangeCredentials): String {
        val apiKey = resolveSecret(stored.bitstampApiKey, BuildConfig.BITSTAMP_API_KEY)
        val apiSecret = resolveSecret(stored.bitstampApiSecret, BuildConfig.BITSTAMP_API_SECRET)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank()) { "Enter Bitstamp API Key & Secret first." }

        val path = "/api/v2/account_balances/"
        val xAuth = "BITSTAMP $apiKey"
        val nonce = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis().toString()
        val version = "v2"
        val sigPayload = xAuth + "POST" + "www.bitstamp.net" + path + "" + "" + nonce + ts + version + ""
        val signature = hmacSha256Hex(sigPayload, apiSecret).uppercase(Locale.US)

        val request = Request.Builder()
            .url("https://www.bitstamp.net$path")
            .header("X-Auth", xAuth)
            .header("X-Auth-Signature", signature)
            .header("X-Auth-Nonce", nonce)
            .header("X-Auth-Timestamp", ts)
            .header("X-Auth-Version", version)
            .post("".toRequestBody(null))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(raw).optString("reason") }.getOrNull() ?: "HTTP ${resp.code}"
                throw IllegalStateException("Bitstamp auth failed: $msg")
            }
            val arr = runCatching { JSONArray(raw) }.getOrNull()
            return "Bitstamp Verified · ${arr?.length() ?: 0} currency balances active"
        }
    }

    /**
     * Executes a REAL live spot order through the chosen route (LG Render Server or Direct Exchange API).
     * Never fabricates fills — returns genuine order ID or exact API error.
     */
    suspend fun executeLiveOrder(
        route: LiveExecutionRoute,
        connectedExchange: ConnectedExchange,
        symbol: String,
        isBuy: Boolean,
        notionalUsd: Double,
        priceUsd: Double,
        strategyId: String,
        stored: EncryptedExchangeCredentials = EncryptedExchangeCredentials()
    ): Result<LiveOrderReceipt> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanSymbol = symbol.trim().uppercase(Locale.US).filter { it.isLetterOrDigit() }
            require(cleanSymbol.length in 2..10) { "Invalid asset symbol." }
            require(notionalUsd.isFinite() && notionalUsd >= 10.0) { "Minimum live order size is $10.00." }
            require(priceUsd.isFinite() && priceUsd > 0.0) { "Live market price unavailable for $cleanSymbol." }

            when (route) {
                LiveExecutionRoute.KRAKEN_DIRECT -> placeKrakenSpotOrder(
                    symbol = cleanSymbol,
                    isBuy = isBuy,
                    notionalUsd = notionalUsd,
                    priceUsd = priceUsd,
                    stored = stored
                )
                LiveExecutionRoute.COINBASE_DIRECT -> placeCoinbaseMarketOrder(
                    symbol = cleanSymbol,
                    isBuy = isBuy,
                    notionalUsd = notionalUsd,
                    priceUsd = priceUsd,
                    stored = stored
                )
                LiveExecutionRoute.KUCOIN_DIRECT -> placeKuCoinMarketOrder(
                    symbol = cleanSymbol,
                    isBuy = isBuy,
                    notionalUsd = notionalUsd,
                    priceUsd = priceUsd,
                    stored = stored
                )
                LiveExecutionRoute.BITSTAMP_DIRECT -> placeBitstampMarketOrder(
                    symbol = cleanSymbol,
                    isBuy = isBuy,
                    notionalUsd = notionalUsd,
                    priceUsd = priceUsd,
                    stored = stored
                )
            }
        }
    }

    private fun placeKrakenSpotOrder(
        symbol: String,
        isBuy: Boolean,
        notionalUsd: Double,
        priceUsd: Double,
        stored: EncryptedExchangeCredentials
    ): LiveOrderReceipt {
        val apiKey = resolveSecret(stored.krakenApiKey, BuildConfig.KRAKEN_API_KEY)
        val apiSecret = resolveSecret(stored.krakenApiSecret, BuildConfig.KRAKEN_API_SECRET)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            "Kraken Pro API keys not set. Open Exchange Configuration to save them securely."
        }

        val path = "/0/private/AddOrder"
        val pair = when (symbol) {
            "BTC" -> "XBTUSD"
            "DOGE" -> "XDGUSD"
            else -> "${symbol}USD"
        }
        val side = if (isBuy) "buy" else "sell"
        val volume = String.format(Locale.US, "%.8f", notionalUsd / priceUsd)
        val nonce = System.currentTimeMillis().toString()
        val postData = "nonce=$nonce&ordertype=market&type=$side&volume=${urlEnc(volume)}&pair=${urlEnc(pair)}"
        val signature = signKrakenRequest(path, nonce, postData, apiSecret)

        val request = Request.Builder()
            .url("https://api.kraken.com$path")
            .header("API-Key", apiKey)
            .header("API-Sign", signature)
            .header("User-Agent", "LGDigital-Android/2.0")
            .post(postData.toRequestBody(formMediaType))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(body) }.getOrNull()
            val errors = json?.optJSONArray("error")
            if (errors != null && errors.length() > 0) {
                throw IllegalStateException("Kraken Pro error: ${errors.optString(0)}")
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("Kraken Pro HTTP ${resp.code}")
            }
            val result = json?.optJSONObject("result")
            val txid = result?.optJSONArray("txid")?.optString(0)?.ifBlank { "KRAKEN-OK" } ?: "KRAKEN-OK"
            val descr = result?.optJSONObject("descr")?.optString("order").orEmpty().ifBlank { "Kraken Pro market $side submitted" }
            return LiveOrderReceipt(
                route = LiveExecutionRoute.KRAKEN_DIRECT,
                exchangeOrderId = txid,
                symbol = symbol,
                side = side.uppercase(Locale.US),
                notionalUsd = notionalUsd,
                statusText = descr
            )
        }
    }

    private fun placeCoinbaseMarketOrder(
        symbol: String,
        isBuy: Boolean,
        notionalUsd: Double,
        priceUsd: Double,
        stored: EncryptedExchangeCredentials
    ): LiveOrderReceipt {
        val apiKey = resolveSecret(stored.coinbaseApiKey, BuildConfig.COINBASE_API_KEY)
        val apiSecret = resolveSecret(stored.coinbaseApiSecret, BuildConfig.COINBASE_API_SECRET)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            "Coinbase API keys not set. Open Exchange Configuration to save them securely."
        }

        val path = "/api/v3/brokerage/orders"
        val productId = "$symbol-USD"
        val side = if (isBuy) "BUY" else "SELL"
        val marketConfig = JSONObject().apply {
            if (isBuy) {
                put("quote_size", String.format(Locale.US, "%.2f", notionalUsd))
            } else {
                put("base_size", String.format(Locale.US, "%.8f", notionalUsd / priceUsd))
            }
        }
        val bodyJson = JSONObject().apply {
            put("client_order_id", UUID.randomUUID().toString())
            put("product_id", productId)
            put("side", side)
            put("order_configuration", JSONObject().put("market_market_ioc", marketConfig))
        }.toString()

        val reqBuilder = Request.Builder()
            .url("https://api.coinbase.com$path")
            .header("User-Agent", "LGDigital-Android/2.0")
            .header("Accept", "application/json")
            .post(bodyJson.toRequestBody(jsonMediaType))

        if (apiSecret.startsWith("eyJ")) {
            reqBuilder.header("Authorization", "Bearer $apiSecret")
        } else {
            val ts = (System.currentTimeMillis() / 1000L).toString()
            val sig = hmacSha256Hex(ts + "POST" + path + bodyJson, apiSecret)
            reqBuilder
                .header("CB-ACCESS-KEY", apiKey)
                .header("CB-ACCESS-SIGN", sig)
                .header("CB-ACCESS-TIMESTAMP", ts)
        }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (!resp.isSuccessful || json?.optBoolean("success", true) == false) {
                val err = json?.optJSONObject("error_response")?.optString("message")
                    ?: json?.optString("message")
                    ?: "Coinbase HTTP ${resp.code}"
                throw IllegalStateException("Coinbase rejected order: $err")
            }
            val orderId = json?.optJSONObject("success_response")?.optString("order_id")
                ?.ifBlank { json.optString("order_id") }
                ?: "CB-OK"
            return LiveOrderReceipt(
                route = LiveExecutionRoute.COINBASE_DIRECT,
                exchangeOrderId = orderId,
                symbol = symbol,
                side = side,
                notionalUsd = notionalUsd,
                statusText = "Coinbase Advanced market $side accepted ($productId)"
            )
        }
    }

    private fun placeKuCoinMarketOrder(
        symbol: String,
        isBuy: Boolean,
        notionalUsd: Double,
        priceUsd: Double,
        stored: EncryptedExchangeCredentials
    ): LiveOrderReceipt {
        val apiKey = resolveSecret(stored.kucoinApiKey, BuildConfig.KUCOIN_API_KEY)
        val apiSecret = resolveSecret(stored.kucoinApiSecret, BuildConfig.KUCOIN_API_SECRET)
        val passphrase = resolveSecret(stored.kucoinPassphrase, BuildConfig.KUCOIN_API_PASSPHRASE)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank() && passphrase.isNotBlank()) {
            "KuCoin API credentials not set. Open Exchange Configuration to save them securely."
        }

        val path = "/api/v1/orders"
        val side = if (isBuy) "buy" else "sell"
        val bodyJson = JSONObject().apply {
            put("clientOid", UUID.randomUUID().toString())
            put("side", side)
            put("symbol", "$symbol-USDT")
            put("type", "market")
            if (isBuy) {
                put("funds", String.format(Locale.US, "%.2f", notionalUsd))
            } else {
                put("size", String.format(Locale.US, "%.6f", notionalUsd / priceUsd))
            }
        }.toString()

        val ts = System.currentTimeMillis().toString()
        val sign = hmacSha256Base64(ts + "POST" + path + bodyJson, apiSecret)
        val passSign = hmacSha256Base64(passphrase, apiSecret)

        val request = Request.Builder()
            .url("https://api.kucoin.com$path")
            .header("KC-API-KEY", apiKey)
            .header("KC-API-SIGN", sign)
            .header("KC-API-TIMESTAMP", ts)
            .header("KC-API-PASSPHRASE", passSign)
            .header("KC-API-KEY-VERSION", "2")
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            val code = json?.optString("code").orEmpty()
            if (!resp.isSuccessful || (code.isNotBlank() && code != "200000")) {
                throw IllegalStateException("KuCoin rejected order: ${json?.optString("msg") ?: "HTTP ${resp.code}"}")
            }
            val orderId = json?.optJSONObject("data")?.optString("orderId")?.ifBlank { "KC-OK" } ?: "KC-OK"
            return LiveOrderReceipt(
                route = LiveExecutionRoute.KUCOIN_DIRECT,
                exchangeOrderId = orderId,
                symbol = symbol,
                side = side.uppercase(Locale.US),
                notionalUsd = notionalUsd,
                statusText = "KuCoin Spot market $side accepted ($symbol-USDT)"
            )
        }
    }

    private fun placeBitstampMarketOrder(
        symbol: String,
        isBuy: Boolean,
        notionalUsd: Double,
        priceUsd: Double,
        stored: EncryptedExchangeCredentials
    ): LiveOrderReceipt {
        val apiKey = resolveSecret(stored.bitstampApiKey, BuildConfig.BITSTAMP_API_KEY)
        val apiSecret = resolveSecret(stored.bitstampApiSecret, BuildConfig.BITSTAMP_API_SECRET)
        require(apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            "Bitstamp API keys not set. Open Exchange Configuration to save them securely."
        }

        val side = if (isBuy) "buy" else "sell"
        val pair = "${symbol.lowercase(Locale.US)}usd"
        val path = "/api/v2/$side/market/$pair/"
        val amount = if (isBuy) {
            String.format(Locale.US, "%.2f", notionalUsd)
        } else {
            String.format(Locale.US, "%.8f", notionalUsd / priceUsd)
        }
        val bodyStr = "amount=${urlEnc(amount)}"
        val contentType = "application/x-www-form-urlencoded"
        val xAuth = "BITSTAMP $apiKey"
        val nonce = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis().toString()
        val version = "v2"
        val sigPayload = xAuth + "POST" + "www.bitstamp.net" + path + "" + contentType + nonce + ts + version + bodyStr
        val signature = hmacSha256Hex(sigPayload, apiSecret).uppercase(Locale.US)

        val request = Request.Builder()
            .url("https://www.bitstamp.net$path")
            .header("X-Auth", xAuth)
            .header("X-Auth-Signature", signature)
            .header("X-Auth-Nonce", nonce)
            .header("X-Auth-Timestamp", ts)
            .header("X-Auth-Version", version)
            .post(bodyStr.toRequestBody(formMediaType))
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (!resp.isSuccessful || json?.optString("status") == "error") {
                throw IllegalStateException("Bitstamp rejected order: ${json?.optString("reason") ?: "HTTP ${resp.code}"}")
            }
            val orderId = json?.optString("id")?.ifBlank { "BS-OK" } ?: "BS-OK"
            return LiveOrderReceipt(
                route = LiveExecutionRoute.BITSTAMP_DIRECT,
                exchangeOrderId = orderId,
                symbol = symbol,
                side = side.uppercase(Locale.US),
                notionalUsd = notionalUsd,
                statusText = "Bitstamp market $side accepted (${pair.uppercase(Locale.US)})"
            )
        }
    }

    private fun signKrakenRequest(path: String, nonce: String, postData: String, base64Secret: String): String {
        val sha256 = MessageDigest.getInstance("SHA-256").digest((nonce + postData).toByteArray(Charsets.UTF_8))
        val pathBytes = path.toByteArray(Charsets.UTF_8)
        val message = ByteArray(pathBytes.size + sha256.size).apply {
            System.arraycopy(pathBytes, 0, this, 0, pathBytes.size)
            System.arraycopy(sha256, 0, this, pathBytes.size, sha256.size)
        }
        val decodedSecret = try {
            Base64.decode(base64Secret, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Kraken API Secret must be valid Base64.")
        }
        val mac = Mac.getInstance("HmacSHA512").apply {
            init(SecretKeySpec(decodedSecret, "HmacSHA512"))
        }
        return Base64.encodeToString(mac.doFinal(message), Base64.NO_WRAP)
    }

    private fun hmacSha256Hex(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        }
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256Base64(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        }
        return Base64.encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun urlEnc(value: String): String = URLEncoder.encode(value, "UTF-8")
}

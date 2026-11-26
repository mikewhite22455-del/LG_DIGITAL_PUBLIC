package com.example.data.remote

import com.example.data.local.EncryptedExchangeCredentialStore
import com.example.viewmodel.ExchangeConnectionStatus
import com.example.viewmodel.ExchangeHealthState
import com.example.viewmodel.ExchangePingLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ExchangePingReport(
    val status: ExchangeConnectionStatus,
    val logEntry: ExchangePingLogEntry
)

data class ExchangePingBatchReport(
    val statuses: Map<String, ExchangeConnectionStatus>,
    val logEntries: List<ExchangePingLogEntry>,
    val summaryMessage: String
)

class ExchangePingService(
    private val credentialStore: EncryptedExchangeCredentialStore,
    private val liveTradingClient: LiveTradingClient
) {
    private val publicClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun displayNameFor(id: String): String = when (id.lowercase(Locale.US)) {
        "kraken" -> "Kraken Pro"
        "coinbase" -> "Coinbase Advanced"
        "kucoin" -> "KuCoin Spot"
        "bitstamp" -> "Bitstamp Spot"
        else -> id.uppercase(Locale.US)
    }

    private fun publicPingEndpointFor(id: String): String = when (id.lowercase(Locale.US)) {
        "kraken" -> "https://api.kraken.com/0/public/Time"
        "coinbase" -> "https://api.coinbase.com/v2/time"
        "kucoin" -> "https://api.kucoin.com/api/v1/timestamp"
        "bitstamp" -> "https://www.bitstamp.net/api/v2/ticker/btcusd/"
        else -> "https://api.kraken.com/0/public/Time"
    }

    suspend fun pingExchange(exchangeId: String): ExchangePingReport = withContext(Dispatchers.IO) {
        val id = exchangeId.lowercase(Locale.US)
        val name = displayNameFor(id)
        val endpoint = publicPingEndpointFor(id)
        val creds = credentialStore.getCredentials()

        val pubStart = System.currentTimeMillis()
        var pubCode: Int? = null
        val pubReachable = runCatching {
            val req = Request.Builder().url(endpoint).get().build()
            publicClient.newCall(req).execute().use { resp ->
                pubCode = resp.code
                resp.isSuccessful
            }
        }.getOrDefault(false)
        val pubLatency = (System.currentTimeMillis() - pubStart).coerceAtLeast(1L)

        val baseStatus = liveTradingClient.verifyExchangeConnectivity(id, creds)
        val sourceLabel = when (id) {
            "kraken" -> if (creds.hasKraken) "AES-256 Vault" else "BuildConfig / None"
            "coinbase" -> if (creds.hasCoinbase) "AES-256 Vault" else "BuildConfig / None"
            "kucoin" -> if (creds.hasKuCoin) "AES-256 Vault" else "BuildConfig / None"
            "bitstamp" -> if (creds.hasBitstamp) "AES-256 Vault" else "BuildConfig / None"
            else -> "Not Stored"
        }

        val enriched = baseStatus.copy(
            endpoint = endpoint,
            httpStatusCode = pubCode,
            publicApiReachable = pubReachable,
            publicLatencyMs = pubLatency,
            credentialSource = sourceLabel
        )
        val logEntry = ExchangePingLogEntry(
            id = UUID.randomUUID().toString().take(8),
            exchangeId = id,
            exchangeName = name,
            state = enriched.state,
            latencyMs = if (enriched.latencyMs > 0L) enriched.latencyMs else pubLatency,
            httpStatusCode = pubCode,
            endpoint = endpoint,
            summary = enriched.detail,
            timestampMs = System.currentTimeMillis()
        )
        ExchangePingReport(status = enriched, logEntry = logEntry)
    }

    suspend fun pingKrakenAndCoinbase(): ExchangePingBatchReport =
        pingBatch(listOf("kraken", "coinbase"), "Kraken & Coinbase")

    suspend fun pingAllExchanges(): ExchangePingBatchReport =
        pingBatch(listOf("kraken", "coinbase", "kucoin", "bitstamp"), "All 4 Exchanges")

    private suspend fun pingBatch(ids: List<String>, label: String): ExchangePingBatchReport = coroutineScope {
        val reports = ids.map { id -> async { pingExchange(id) } }.awaitAll()
        val statusMap = reports.associate { it.status.exchangeId to it.status }
        val connected = reports.count { it.status.state == ExchangeHealthState.CONNECTED }
        val reachable = reports.count { it.status.publicApiReachable == true }
        val summary = "$label check: $connected signed connected, $reachable/${ids.size} public gateways online."
        ExchangePingBatchReport(
            statuses = statusMap,
            logEntries = reports.map { it.logEntry },
            summaryMessage = summary
        )
    }
}

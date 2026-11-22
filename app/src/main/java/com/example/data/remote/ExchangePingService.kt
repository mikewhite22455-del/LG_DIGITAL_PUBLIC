package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.EncryptedExchangeCredentialStore
import com.example.data.local.EncryptedExchangeCredentials
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
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Dedicated connectivity verification service that executes real HTTPS 'ping' requests
 * against Kraken Pro and Coinbase Advanced (as well as KuCoin & Bitstamp) using
 * decrypted credentials loaded from [EncryptedExchangeCredentialStore].
 *
 * Each ping runs two non-destructive stages:
 *  1. Public exchange reachability ping (e.g., Kraken `/0/public/SystemStatus`,
 *     Coinbase `/api/v3/brokerage/time`) to separate network health from key errors.
 *  2. Authenticated read-only balance/account ping signed with the stored encrypted keys.
 *
 * Security invariants:
 *  - Never logs or includes API keys, secrets, passphrases, or signatures in reports.
 *  - Strictly read-only HTTPS endpoints — never creates, modifies, or cancels orders.
 */
class ExchangePingService(
    private val credentialStore: EncryptedExchangeCredentialStore,
    private val liveTradingClient: LiveTradingClient
) {

    private val probeClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    data class BatchPingReport(
        val statuses: Map<String, ExchangeConnectionStatus>,
        val logEntries: List<ExchangePingLogEntry>,
        val summaryMessage: String
    )

    data class SinglePingReport(
        val status: ExchangeConnectionStatus,
        val logEntry: ExchangePingLogEntry
    )

    suspend fun pingKraken(): SinglePingReport = pingExchange("kraken")

    suspend fun pingCoinbase(): SinglePingReport = pingExchange("coinbase")

    /**
     * Concurrently pings Kraken Pro and Coinbase Advanced using the stored encrypted credentials.
     */
    suspend fun pingKrakenAndCoinbase(): BatchPingReport = pingExchanges(
        exchangeIds = listOf("kraken", "coinbase"),
        batchLabel = "Kraken & Coinbase"
    )

    /**
     * Concurrently pings all supported exchange integrations (Kraken, Coinbase, KuCoin, Bitstamp).
     */
    suspend fun pingAllExchanges(): BatchPingReport = pingExchanges(
        exchangeIds = listOf("kraken", "coinbase", "kucoin", "bitstamp"),
        batchLabel = "All Exchanges"
    )

    suspend fun pingExchange(exchangeId: String): SinglePingReport = withContext(Dispatchers.IO) {
        val id = exchangeId.trim().lowercase(Locale.US)
        val creds = credentialStore.getCredentials()
        executePingForExchange(id, creds)
    }

    private suspend fun pingExchanges(
        exchangeIds: List<String>,
        batchLabel: String
    ): BatchPingReport = withContext(Dispatchers.IO) {
        val creds = credentialStore.getCredentials()
        val reports = coroutineScope {
            exchangeIds.map { id ->
                async { executePingForExchange(id.lowercase(Locale.US), creds) }
            }.awaitAll()
        }

        val statusMap = reports.associate { it.status.exchangeId to it.status }
        val logEntries = reports.map { it.logEntry }

        val onlineCount = reports.count { it.status.state == ExchangeHealthState.CONNECTED }
        val configuredCount = reports.count { it.status.state != ExchangeHealthState.NOT_CONFIGURED }

        val summary = when {
            configuredCount == 0 -> "$batchLabel ping complete: public APIs checked — store encrypted API keys to authenticate."
            onlineCount == configuredCount -> "$batchLabel ping passed: $onlineCount / $configuredCount authenticated & online."
            else -> "$batchLabel ping: $onlineCount / $configuredCount authenticated (${configuredCount - onlineCount} need attention)."
        }

        BatchPingReport(
            statuses = statusMap,
            logEntries = logEntries,
            summaryMessage = summary
        )
    }

    private suspend fun executePingForExchange(
        id: String,
        stored: EncryptedExchangeCredentials
    ): SinglePingReport {
        val displayName = exchangeDisplayName(id)
        val endpoint = authenticatedEndpointLabel(id)
        val source = credentialSourceLabel(id, stored)

        // Stage 1: Public HTTPS reachability ping
        val publicProbe = runPublicReachabilityPing(id)

        // Stage 2: Authenticated read-only ping using stored encrypted credentials
        val authStatus = liveTradingClient.verifyExchangeConnectivity(id, stored)

        val enrichedDetail = when (authStatus.state) {
            ExchangeHealthState.CONNECTED -> {
                if (publicProbe.detail.isNotBlank()) {
                    "${authStatus.detail} · ${publicProbe.detail}"
                } else {
                    authStatus.detail
                }
            }
            ExchangeHealthState.NOT_CONFIGURED -> {
                if (publicProbe.reachable) {
                    "Public API reachable (${publicProbe.latencyMs}ms) · Add encrypted keys to authenticate"
                } else {
                    "Public API unreachable (${publicProbe.detail}) · No encrypted keys stored"
                }
            }
            ExchangeHealthState.ERROR -> {
                if (publicProbe.reachable) {
                    "${authStatus.detail} (Gateway reachable · ${publicProbe.latencyMs}ms)"
                } else {
                    "Network/gateway unreachable: ${authStatus.detail}"
                }
            }
            ExchangeHealthState.CHECKING -> authStatus.detail
        }

        val resolvedHttpCode = when (authStatus.state) {
            ExchangeHealthState.CONNECTED -> 200
            ExchangeHealthState.NOT_CONFIGURED -> publicProbe.httpCode
            ExchangeHealthState.ERROR -> extractHttpCode(authStatus.detail) ?: publicProbe.httpCode
            ExchangeHealthState.CHECKING -> null
        }

        val finalStatus = authStatus.copy(
            detail = enrichedDetail,
            endpoint = endpoint,
            httpStatusCode = resolvedHttpCode,
            publicApiReachable = publicProbe.reachable,
            publicLatencyMs = publicProbe.latencyMs,
            credentialSource = source
        )

        val entry = ExchangePingLogEntry(
            id = "${id}_${finalStatus.lastCheckedMs}",
            exchangeId = id,
            exchangeName = displayName,
            state = finalStatus.state,
            latencyMs = if (finalStatus.latencyMs > 0L) finalStatus.latencyMs else publicProbe.latencyMs,
            httpStatusCode = resolvedHttpCode,
            endpoint = if (finalStatus.state == ExchangeHealthState.NOT_CONFIGURED) publicProbe.endpoint else endpoint,
            summary = finalStatus.detail,
            timestampMs = finalStatus.lastCheckedMs
        )

        return SinglePingReport(status = finalStatus, logEntry = entry)
    }

    private data class PublicProbeResult(
        val reachable: Boolean,
        val httpCode: Int?,
        val latencyMs: Long,
        val endpoint: String,
        val detail: String
    )

    private fun runPublicReachabilityPing(id: String): PublicProbeResult {
        val (url, label) = when (id) {
            "kraken" -> "https://api.kraken.com/0/public/SystemStatus" to "GET /0/public/SystemStatus"
            "coinbase" -> "https://api.coinbase.com/api/v3/brokerage/time" to "GET /api/v3/brokerage/time"
            "kucoin" -> "https://api.kucoin.com/api/v1/timestamp" to "GET /api/v1/timestamp"
            "bitstamp" -> "https://www.bitstamp.net/api/v2/ticker/btcusd/" to "GET /api/v2/ticker/btcusd/"
            else -> return PublicProbeResult(false, null, 0L, "", "Unsupported exchange")
        }

        val started = System.currentTimeMillis()
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LGDigital-Android/2.0")
                .header("Accept", "application/json")
                .get()
                .build()
            probeClient.newCall(request).execute().use { resp ->
                val elapsed = (System.currentTimeMillis() - started).coerceAtLeast(1L)
                val body = resp.body?.string().orEmpty()
                val note = when (id) {
                    "kraken" -> {
                        val status = runCatching {
                            JSONObject(body).optJSONObject("result")?.optString("status")
                        }.getOrNull()?.ifBlank { null }
                        if (status != null) "Kraken status: $status" else "HTTP ${resp.code}"
                    }
                    "coinbase" -> {
                        val hasTime = runCatching {
                            JSONObject(body).has("epochSeconds") || JSONObject(body).has("iso")
                        }.getOrDefault(false)
                        if (hasTime) "Coinbase clock synced" else "HTTP ${resp.code}"
                    }
                    else -> "HTTP ${resp.code}"
                }
                PublicProbeResult(
                    reachable = resp.isSuccessful,
                    httpCode = resp.code,
                    latencyMs = elapsed,
                    endpoint = label,
                    detail = note
                )
            }
        } catch (e: Exception) {
            val elapsed = (System.currentTimeMillis() - started).coerceAtLeast(1L)
            PublicProbeResult(
                reachable = false,
                httpCode = null,
                latencyMs = elapsed,
                endpoint = label,
                detail = e.javaClass.simpleName.ifBlank { "Network error" }
            )
        }
    }

    private fun credentialSourceLabel(id: String, stored: EncryptedExchangeCredentials): String {
        return when (id) {
            "kraken" -> when {
                stored.hasKraken -> "Encrypted DataStore"
                isRealBuildSecret(BuildConfig.KRAKEN_API_KEY) && isRealBuildSecret(BuildConfig.KRAKEN_API_SECRET) -> "App Secrets"
                else -> "Not Stored"
            }
            "coinbase" -> when {
                stored.hasCoinbase -> "Encrypted DataStore"
                isRealBuildSecret(BuildConfig.COINBASE_API_KEY) && isRealBuildSecret(BuildConfig.COINBASE_API_SECRET) -> "App Secrets"
                else -> "Not Stored"
            }
            "kucoin" -> when {
                stored.hasKuCoin -> "Encrypted DataStore"
                isRealBuildSecret(BuildConfig.KUCOIN_API_KEY) &&
                    isRealBuildSecret(BuildConfig.KUCOIN_API_SECRET) &&
                    isRealBuildSecret(BuildConfig.KUCOIN_API_PASSPHRASE) -> "App Secrets"
                else -> "Not Stored"
            }
            "bitstamp" -> when {
                stored.hasBitstamp -> "Encrypted DataStore"
                isRealBuildSecret(BuildConfig.BITSTAMP_API_KEY) && isRealBuildSecret(BuildConfig.BITSTAMP_API_SECRET) -> "App Secrets"
                else -> "Not Stored"
            }
            else -> "Not Stored"
        }
    }

    private fun isRealBuildSecret(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.isNotBlank() && !trimmed.endsWith("_DEFAULT_VALUE")
    }

    private fun authenticatedEndpointLabel(id: String): String = when (id) {
        "kraken" -> "POST /0/private/Balance"
        "coinbase" -> "GET /api/v3/brokerage/accounts"
        "kucoin" -> "GET /api/v1/accounts?type=trade"
        "bitstamp" -> "POST /api/v2/account_balances/"
        else -> ""
    }

    private fun exchangeDisplayName(id: String): String = when (id) {
        "kraken" -> "Kraken Pro"
        "coinbase" -> "Coinbase"
        "kucoin" -> "KuCoin"
        "bitstamp" -> "Bitstamp"
        else -> id.uppercase(Locale.US)
    }

    private fun extractHttpCode(message: String): Int? {
        val match = Regex("""HTTP\s+(\d{3})""").find(message) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }
}

package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.exchangeVaultDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lg_encrypted_exchange_vault"
)

data class EncryptedExchangeCredentials(
    val krakenApiKey: String = "",
    val krakenApiSecret: String = "",
    val coinbaseApiKey: String = "",
    val coinbaseApiSecret: String = "",
    val kucoinApiKey: String = "",
    val kucoinApiSecret: String = "",
    val kucoinPassphrase: String = "",
    val bitstampApiKey: String = "",
    val bitstampApiSecret: String = ""
) {
    val hasKraken: Boolean get() = krakenApiKey.isNotBlank() && krakenApiSecret.isNotBlank()
    val hasCoinbase: Boolean get() = coinbaseApiKey.isNotBlank() && coinbaseApiSecret.isNotBlank()
    val hasKuCoin: Boolean get() = kucoinApiKey.isNotBlank() && kucoinApiSecret.isNotBlank() && kucoinPassphrase.isNotBlank()
    val hasBitstamp: Boolean get() = bitstampApiKey.isNotBlank() && bitstampApiSecret.isNotBlank()
}

/**
 * Hardware-backed AndroidKeyStore (AES-256-GCM) encrypted Jetpack DataStore vault
 * for storing Kraken Pro, Coinbase, KuCoin, and Bitstamp API credentials locally.
 *
 * Plaintext secrets are never written to disk or logged; every value is encrypted with
 * a unique 96-bit GCM IV before persistence.
 */
class EncryptedExchangeCredentialStore(private val context: Context) {

    private object Keys {
        val KRAKEN_KEY = stringPreferencesKey("enc_kraken_key")
        val KRAKEN_SECRET = stringPreferencesKey("enc_kraken_secret")
        val COINBASE_KEY = stringPreferencesKey("enc_coinbase_key")
        val COINBASE_SECRET = stringPreferencesKey("enc_coinbase_secret")
        val KUCOIN_KEY = stringPreferencesKey("enc_kucoin_key")
        val KUCOIN_SECRET = stringPreferencesKey("enc_kucoin_secret")
        val KUCOIN_PASSPHRASE = stringPreferencesKey("enc_kucoin_passphrase")
        val BITSTAMP_KEY = stringPreferencesKey("enc_bitstamp_key")
        val BITSTAMP_SECRET = stringPreferencesKey("enc_bitstamp_secret")
    }

    val credentialsFlow: Flow<EncryptedExchangeCredentials> = context.exchangeVaultDataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }
        .map { prefs ->
            EncryptedExchangeCredentials(
                krakenApiKey = decryptOrEmpty(prefs[Keys.KRAKEN_KEY]),
                krakenApiSecret = decryptOrEmpty(prefs[Keys.KRAKEN_SECRET]),
                coinbaseApiKey = decryptOrEmpty(prefs[Keys.COINBASE_KEY]),
                coinbaseApiSecret = decryptOrEmpty(prefs[Keys.COINBASE_SECRET]),
                kucoinApiKey = decryptOrEmpty(prefs[Keys.KUCOIN_KEY]),
                kucoinApiSecret = decryptOrEmpty(prefs[Keys.KUCOIN_SECRET]),
                kucoinPassphrase = decryptOrEmpty(prefs[Keys.KUCOIN_PASSPHRASE]),
                bitstampApiKey = decryptOrEmpty(prefs[Keys.BITSTAMP_KEY]),
                bitstampApiSecret = decryptOrEmpty(prefs[Keys.BITSTAMP_SECRET])
            )
        }

    suspend fun getCredentials(): EncryptedExchangeCredentials = withContext(Dispatchers.IO) {
        credentialsFlow.first()
    }

    suspend fun saveKrakenCredentials(apiKey: String, apiSecret: String) = withContext(Dispatchers.IO) {
        context.exchangeVaultDataStore.edit { prefs ->
            putOrRemoveEncrypted(prefs, Keys.KRAKEN_KEY, apiKey.trim())
            putOrRemoveEncrypted(prefs, Keys.KRAKEN_SECRET, apiSecret.trim())
        }
    }

    suspend fun saveCoinbaseCredentials(apiKey: String, apiSecret: String) = withContext(Dispatchers.IO) {
        context.exchangeVaultDataStore.edit { prefs ->
            putOrRemoveEncrypted(prefs, Keys.COINBASE_KEY, apiKey.trim())
            putOrRemoveEncrypted(prefs, Keys.COINBASE_SECRET, apiSecret.trim())
        }
    }

    suspend fun saveKuCoinCredentials(apiKey: String, apiSecret: String, passphrase: String) = withContext(Dispatchers.IO) {
        context.exchangeVaultDataStore.edit { prefs ->
            putOrRemoveEncrypted(prefs, Keys.KUCOIN_KEY, apiKey.trim())
            putOrRemoveEncrypted(prefs, Keys.KUCOIN_SECRET, apiSecret.trim())
            putOrRemoveEncrypted(prefs, Keys.KUCOIN_PASSPHRASE, passphrase.trim())
        }
    }

    suspend fun saveBitstampCredentials(apiKey: String, apiSecret: String) = withContext(Dispatchers.IO) {
        context.exchangeVaultDataStore.edit { prefs ->
            putOrRemoveEncrypted(prefs, Keys.BITSTAMP_KEY, apiKey.trim())
            putOrRemoveEncrypted(prefs, Keys.BITSTAMP_SECRET, apiSecret.trim())
        }
    }

    suspend fun clearExchange(exchangeId: String) = withContext(Dispatchers.IO) {
        context.exchangeVaultDataStore.edit { prefs ->
            when (exchangeId.lowercase()) {
                "kraken" -> {
                    prefs.remove(Keys.KRAKEN_KEY)
                    prefs.remove(Keys.KRAKEN_SECRET)
                }
                "coinbase" -> {
                    prefs.remove(Keys.COINBASE_KEY)
                    prefs.remove(Keys.COINBASE_SECRET)
                }
                "kucoin" -> {
                    prefs.remove(Keys.KUCOIN_KEY)
                    prefs.remove(Keys.KUCOIN_SECRET)
                    prefs.remove(Keys.KUCOIN_PASSPHRASE)
                }
                "bitstamp" -> {
                    prefs.remove(Keys.BITSTAMP_KEY)
                    prefs.remove(Keys.BITSTAMP_SECRET)
                }
            }
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        context.exchangeVaultDataStore.edit { it.clear() }
    }

    private fun putOrRemoveEncrypted(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        key: Preferences.Key<String>,
        plaintext: String
    ) {
        if (plaintext.isBlank()) {
            prefs.remove(key)
        } else {
            prefs[key] = encrypt(plaintext)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(spec)
        }.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + ciphertext.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(ciphertext, 0, combined, 1 + iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptOrEmpty(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size < 14) return ""
            val ivLen = combined[0].toInt() and 0xFF
            if (ivLen <= 0 || combined.size <= 1 + ivLen) return ""
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val ciphertext = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrDefault("")
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "lg_digital_exchange_vault_aes256_v1"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128

        fun maskSecret(value: String): String {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return "Not set"
            if (trimmed.length <= 6) return "••••••"
            return "••••••${trimmed.takeLast(4)}"
        }
    }
}

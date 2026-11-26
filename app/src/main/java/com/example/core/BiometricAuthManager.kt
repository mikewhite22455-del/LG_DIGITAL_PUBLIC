package com.example.core

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity

enum class BiometricCapability(val statusLabel: String, val detailMessage: String) {
    READY(
        statusLabel = "Fingerprint / Face / Device Lock Ready",
        detailMessage = "Hardware biometric or screen-lock authentication is active on this device."
    ),
    NOT_ENROLLED(
        statusLabel = "No Biometrics or Screen Lock Enrolled",
        detailMessage = "Enroll a fingerprint, face unlock, or device PIN in system settings to enforce hardware lock."
    ),
    NO_HARDWARE(
        statusLabel = "Biometric Hardware Unavailable",
        detailMessage = "This device does not report a biometric sensor or secure screen lock."
    ),
    TEMPORARILY_UNAVAILABLE(
        statusLabel = "Sensor Temporarily Unavailable",
        detailMessage = "Biometric sensor is busy or temporarily unavailable. Retry in a moment."
    ),
    SECURITY_UPDATE_REQUIRED(
        statusLabel = "Security Update Required",
        detailMessage = "A system security update is required before biometric authentication can be used."
    )
}

object BiometricAuthManager {
    private const val PREFS_NAME = "lg_security_prefs"
    private const val KEY_BIOMETRIC_LOCK_ENABLED = "biometric_lock_enabled"
    private const val KEY_AUTO_LOCK_BACKGROUND = "auto_lock_on_background"

    fun isBiometricLockEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false)
    }

    fun setBiometricLockEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, enabled) }
    }

    fun isAutoLockOnBackgroundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_LOCK_BACKGROUND, false)
    }

    fun setAutoLockOnBackgroundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_AUTO_LOCK_BACKGROUND, enabled) }
    }

    fun allowedAuthenticators(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }

    fun checkCapability(context: Context): BiometricCapability {
        return runCatching {
            val manager = BiometricManager.from(context)
            when (manager.canAuthenticate(allowedAuthenticators())) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.READY
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NOT_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.TEMPORARILY_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SECURITY_UPDATE_REQUIRED
                else -> BiometricCapability.NO_HARDWARE
            }
        }.getOrDefault(BiometricCapability.NO_HARDWARE)
    }

    fun hasStrongBiometricsEnrolled(context: Context): Boolean {
        return runCatching {
            val manager = BiometricManager.from(context)
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        }.getOrDefault(false)
    }

    fun openBiometricEnrollment(context: Context) {
        val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, allowedAuthenticators())
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        enrollIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(enrollIntent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun formatPromptError(errorCode: Int, message: String): Pair<Boolean, String> {
        val isHardError = errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
        val formatted = when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "Biometric verification canceled. Authenticate to continue."
            BiometricPrompt.ERROR_LOCKOUT -> "Too many failed attempts. Wait 30 seconds and try again."
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometrics temporarily locked by Android. Use your device PIN, pattern, or password."
            else -> message.ifBlank { "Biometric authentication was interrupted. Please try again." }
        }
        return isHardError to formatted
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock LG Digital",
        subtitle: String = "Verify identity with Fingerprint, Face Unlock, or Device Lock",
        confirmationRequired: Boolean = false,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, message: String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        runCatching {
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators())
                .setConfirmationRequired(confirmationRequired)
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { err ->
            onError(
                BiometricPrompt.ERROR_HW_UNAVAILABLE,
                err.localizedMessage ?: "Biometric prompt is unavailable on this device."
            )
        }
    }
}

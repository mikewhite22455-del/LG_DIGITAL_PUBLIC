package com.example.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.core.BiometricAuthManager
import com.example.core.BiometricCapability
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors

@Composable
fun BiometricLockScreen(
    activity: FragmentActivity?,
    onUnlocked: (usedFallbackWithoutDeviceLock: Boolean) -> Unit,
    onPromptActiveChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var capability by remember { mutableStateOf(BiometricAuthManager.checkCapability(context)) }
    var strongEnrolled by remember { mutableStateOf(BiometricAuthManager.hasStrongBiometricsEnrolled(context)) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }
    var autoPromptTriggered by remember { mutableStateOf(false) }

    fun launchPrompt() {
        val host = activity
        if (host == null) {
            feedbackIsError = true
            feedbackMessage = "Unable to access FragmentActivity host for biometric prompt."
            return
        }
        val currentCap = BiometricAuthManager.checkCapability(host)
        capability = currentCap
        strongEnrolled = BiometricAuthManager.hasStrongBiometricsEnrolled(host)

        if (currentCap != BiometricCapability.READY) {
            feedbackIsError = true
            feedbackMessage = currentCap.detailMessage
            return
        }

        feedbackMessage = null
        onPromptActiveChange(true)
        BiometricAuthManager.authenticate(
            activity = host,
            title = "Unlock Trading Dashboard",
            subtitle = "Use Fingerprint, Face Unlock, or Device Lock to enter LG Digital",
            onSuccess = {
                onPromptActiveChange(false)
                onUnlocked(false)
            },
            onError = { errorCode, msg ->
                onPromptActiveChange(false)
                feedbackIsError = errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                feedbackMessage = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "Verification canceled. Tap below when ready to unlock."
                    BiometricPrompt.ERROR_LOCKOUT -> "Too many attempts. Wait 30 seconds and try again."
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometrics locked by system. Use device PIN/Pattern to unlock."
                    else -> msg.ifBlank { "Biometric verification interrupted." }
                }
            },
            onFailed = {
                feedbackIsError = true
                feedbackMessage = "Biometric not recognized. Please try again."
            }
        )
    }

    // Refresh enrollment capability if user returns from Android Security Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                capability = BiometricAuthManager.checkCapability(context)
                strongEnrolled = BiometricAuthManager.hasStrongBiometricsEnrolled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Trigger BiometricPrompt automatically once on entry when hardware/credentials are ready
    LaunchedEffect(capability) {
        if (!autoPromptTriggered && capability == BiometricCapability.READY && activity != null) {
            autoPromptTriggered = true
            launchPrompt()
        }
    }

    Surface(
        color = LgColors.ObsidianBg,
        modifier = Modifier
            .fillMaxSize()
            .testTag("biometric_lock_screen")
    ) {
        LgCircuitBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .border(1.5.dp, LgColors.Gold.copy(alpha = 0.65f), MaterialTheme.shapes.large)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusPill(
                        text = if (strongEnrolled) "BIOMETRIC GUARD ACTIVE" else "DEVICE SECURITY GATE",
                        color = if (capability == BiometricCapability.READY) LgColors.Turquoise else LgColors.Gold
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(LgColors.Turquoise.copy(alpha = 0.12f))
                            .border(2.dp, LgColors.Gold.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_user_lg_coin),
                            contentDescription = "LG Digital secure vault emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "LG Digital Vault Locked",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Authenticate with Fingerprint, Face Unlock, or Device PIN to access your trading dashboard, strategies, and vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Capability readout card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LgColors.ElevatedSlate.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.dp,
                            (if (capability == BiometricCapability.READY) LgColors.Turquoise else LgColors.Gold).copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (capability == BiometricCapability.READY) Icons.Filled.Fingerprint else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (capability == BiometricCapability.READY) LgColors.Turquoise else LgColors.Gold,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = capability.statusLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = capability.detailMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    feedbackMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = (if (feedbackIsError) LgColors.Crimson else LgColors.Gold).copy(alpha = 0.14f),
                            border = BorderStroke(
                                1.dp,
                                (if (feedbackIsError) LgColors.Crimson else LgColors.Gold).copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (feedbackIsError) LgColors.Crimson else LgColors.Gold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    GoldMetallicButton(
                        text = if (capability == BiometricCapability.READY) "Unlock with Biometrics" else "Retry Biometric Check",
                        onClick = { launchPrompt() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("biometric_unlock_button")
                    )

                    if (capability == BiometricCapability.NOT_ENROLLED) {
                        FilledTonalButton(
                            onClick = { BiometricAuthManager.openBiometricEnrollment(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("biometric_enroll_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                            Text("Set Up Fingerprint / Face Unlock")
                        }
                    }

                    // Only offer unsecured bypass if the device itself has no biometrics or screen lock enrolled
                    if (capability == BiometricCapability.NOT_ENROLLED || capability == BiometricCapability.NO_HARDWARE) {
                        OutlinedButton(
                            onClick = { onUnlocked(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("biometric_bypass_no_lock_button")
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(18.dp))
                            Text("Continue (No Device Lock Configured)")
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Protected by AndroidX BiometricPrompt",
                        style = MaterialTheme.typography.labelSmall,
                        color = LgColors.InkMuted
                    )
                }
            }
        }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.core.BiometricAuthManager
import com.example.core.BiometricCapability
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors

@Composable
fun BiometricLockScreen(
    activity: FragmentActivity?,
    onUnlocked: (usedFallbackWithoutLock: Boolean) -> Unit,
    onPromptActiveChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val capability = remember { BiometricAuthManager.checkCapability(context) }
    var statusMessage by remember { mutableStateOf(capability.detailMessage) }
    var isError by remember { mutableStateOf(false) }

    fun triggerPrompt() {
        if (activity != null && capability == BiometricCapability.READY) {
            onPromptActiveChange(true)
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock LG Digital",
                subtitle = "Verify identity with Fingerprint, Face Unlock, or Device Lock",
                onSuccess = {
                    onPromptActiveChange(false)
                    onUnlocked(false)
                },
                onError = { code, msg ->
                    onPromptActiveChange(false)
                    val (hard, formatted) = BiometricAuthManager.formatPromptError(code, msg)
                    isError = hard
                    statusMessage = formatted
                },
                onFailed = {
                    isError = true
                    statusMessage = "Biometric not recognized. Please try again."
                }
            )
        } else {
            onUnlocked(true)
        }
    }

    LaunchedEffect(Unit) {
        if (capability == BiometricCapability.READY && activity != null) {
            triggerPrompt()
        }
    }

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
                .testTag("biometric_lock_screen"),
            contentAlignment = Alignment.Center
        ) {
            CyberFrameCard(
                title = "LG DIGITAL SECURITY LOCK",
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lg_digital_emblem),
                        contentDescription = "LG Digital emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, LgColors.GoldBright, CircleShape)
                    )
                    Text(
                        text = "LG DIGITAL",
                        fontFamily = LgBrandFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 2.sp,
                        color = LgColors.GoldBright
                    )
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = LgColors.Turquoise,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) LgColors.Crimson else LgColors.InkSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (capability == BiometricCapability.READY && activity != null) {
                        GoldMetallicButton(
                            text = "UNLOCK WITH BIOMETRICS",
                            onClick = { triggerPrompt() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_unlock_button")
                        )
                        if (isError) {
                            OutlinedButton(
                                onClick = { onUnlocked(true) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("biometric_error_continue_button")
                            ) {
                                Text("Continue Without Biometrics", color = LgColors.Turquoise)
                            }
                        }
                    } else {
                        GoldMetallicButton(
                            text = "CONTINUE TO DASHBOARD",
                            onClick = { onUnlocked(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_fallback_continue_button")
                        )
                        OutlinedButton(
                            onClick = { BiometricAuthManager.openBiometricEnrollment(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Enroll Device Lock in Settings", color = LgColors.Turquoise)
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.SocialBrandBadge
import com.example.ui.components.SocialMediaLinksBar
import com.example.ui.components.SocialPlatform
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle

data class MemberSession(
    val displayName: String,
    val provider: String,
    val handleOrEmail: String
)

object MemberSessionPrefs {
    private const val PREFS_NAME = "lg_digital_member_session"
    private const val KEY_SIGNED_IN = "signed_in"
    private const val KEY_NAME = "display_name"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_HANDLE = "handle"

    fun load(context: Context): MemberSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SIGNED_IN, false)) return null
        return MemberSession(
            displayName = prefs.getString(KEY_NAME, "LG Member") ?: "LG Member",
            provider = prefs.getString(KEY_PROVIDER, "Guest") ?: "Guest",
            handleOrEmail = prefs.getString(KEY_HANDLE, "") ?: ""
        )
    }

    fun save(context: Context, session: MemberSession) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_NAME, session.displayName)
            .putString(KEY_PROVIDER, session.provider)
            .putString(KEY_HANDLE, session.handleOrEmail)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

private enum class OnboardingStep {
    WELCOME,
    SIGN_IN
}

@Composable
fun WelcomeSignInScreen(
    initialShowSignIn: Boolean = false,
    existingSession: MemberSession? = null,
    isPopUpOverlay: Boolean = true,
    onOpenMembershipSignUp: () -> Unit = {},
    onCompleted: (MemberSession) -> Unit
) {
    var step by remember(initialShowSignIn) {
        mutableStateOf(if (initialShowSignIn) OnboardingStep.SIGN_IN else OnboardingStep.WELCOME)
    }

    val dismissIntoDesk: () -> Unit = {
        onCompleted(
            existingSession ?: MemberSession(
                displayName = "LG Trader",
                provider = "Quick Access",
                handleOrEmail = "Paper Desk"
            )
        )
    }

    if (isPopUpOverlay) {
        BackHandler {
            if (step == OnboardingStep.SIGN_IN) step = OnboardingStep.WELCOME else dismissIntoDesk()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .testTag("welcome_signin_root"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, LgColors.GoldBright, RoundedCornerShape(22.dp))
                    .padding(2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.4.dp, LgColors.Turquoise, RoundedCornerShape(20.dp))
            ) {
                LgCircuitBackground(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        when (step) {
                            OnboardingStep.WELCOME -> {
                                ClassicWelcomeContent(
                                    existingSession = existingSession,
                                    onGoToSignIn = { step = OnboardingStep.SIGN_IN },
                                    onOpenMembershipSignUp = onOpenMembershipSignUp,
                                    onEnterDeskDirect = dismissIntoDesk
                                )
                            }
                            OnboardingStep.SIGN_IN -> {
                                MemberSignInContent(
                                    onBackToWelcome = { step = OnboardingStep.WELCOME },
                                    onOpenMembershipSignUp = onOpenMembershipSignUp,
                                    onSignedIn = onCompleted
                                )
                            }
                        }

                        IconButton(
                            onClick = dismissIntoDesk,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(LgColors.ObsidianBg.copy(alpha = 0.80f))
                                .border(1.2.dp, LgColors.GoldBright, CircleShape)
                                .testTag("welcome_popup_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Welcome Pop-Up",
                                tint = LgColors.GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        Surface(
            color = LgColors.ObsidianBg,
            modifier = Modifier
                .fillMaxSize()
                .testTag("welcome_signin_root")
        ) {
            LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (step) {
                        OnboardingStep.WELCOME -> {
                            ClassicWelcomeContent(
                                existingSession = existingSession,
                                onGoToSignIn = { step = OnboardingStep.SIGN_IN },
                                onOpenMembershipSignUp = onOpenMembershipSignUp,
                                onEnterDeskDirect = dismissIntoDesk
                            )
                        }
                        OnboardingStep.SIGN_IN -> {
                            MemberSignInContent(
                                onBackToWelcome = { step = OnboardingStep.WELCOME },
                                onOpenMembershipSignUp = onOpenMembershipSignUp,
                                onSignedIn = onCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassicWelcomeContent(
    existingSession: MemberSession?,
    onGoToSignIn: () -> Unit,
    onOpenMembershipSignUp: () -> Unit,
    onEnterDeskDirect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        StatusPill(
            text = "LIVE QUANT TERMINAL · TRADINGVIEW FEEDS",
            color = LgColors.Turquoise
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(134.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LgColors.Turquoise.copy(alpha = 0.28f),
                            LgColors.Gold.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.2.dp, LgColors.GoldBright, CircleShape)
                .padding(6.dp)
                .border(1.4.dp, LgColors.Turquoise.copy(alpha = 0.85f), CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_user_lg_coin),
                contentDescription = "LG Digital emblem",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "WELCOME TO",
                style = TabLabelTextStyle,
                color = LgColors.Turquoise,
                letterSpacing = 2.4.sp
            )
            Text(
                text = "LG DIGITAL",
                fontFamily = LgBrandFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                letterSpacing =

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
import androidx.compose.material.icons.filled.Palette
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
import com.example.ui.theme.TradingViewFontFamily

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
    onOpenThemePreviews: () -> Unit = {},
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
                                    onOpenThemePreviews = onOpenThemePreviews,
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

                        // Pop-up dismiss X button in top-right corner
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
                                onOpenThemePreviews = onOpenThemePreviews,
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
    onOpenThemePreviews: () -> Unit,
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

        // Exact Classic LG Digital Glowing Gold & Turquoise Coin Emblem
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
                letterSpacing = 2.6.sp,
                color = LgColors.GoldBright,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Adaptive USD/CAD Exchange Scanner, Live TradingView Feeds, Arbitrage, AI Copilot & Universal Miner Suite",
                style = MaterialTheme.typography.bodyMedium,
                color = LgColors.InkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Classic 4-Feature Matrix Card
        CyberFrameCard(
            title = "COMMAND SUITE OVERVIEW",
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WelcomeFeaturePill(
                    icon = Icons.Filled.CandlestickChart,
                    title = "Live Feeds",
                    subtitle = "TradingView & Spot",
                    accent = LgColors.Turquoise,
                    modifier = Modifier.weight(1f)
                )
                WelcomeFeaturePill(
                    icon = Icons.Filled.Bolt,
                    title = "Adaptive Bot",
                    subtitle = "USD / CAD Scanner",
                    accent = LgColors.GoldBright,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WelcomeFeaturePill(
                    icon = Icons.Filled.AutoAwesome,
                    title = "AI Advisor",
                    subtitle = "Quant Copilot",
                    accent = LgColors.CyanGlow,
                    modifier = Modifier.weight(1f)
                )
                WelcomeFeaturePill(
                    icon = Icons.Filled.Memory,
                    title = "Universal Miner",
                    subtitle = "Any Rig / Any Coin",
                    accent = LgColors.Gold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Primary CTAs
        GoldMetallicButton(
            text = if (existingSession != null) "SIGN IN / SWITCH ACCOUNT" else "SIGN IN TO LG DIGITAL",
            onClick = onGoToSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag("welcome_go_to_signin_button")
        )

        OutlinedButton(
            onClick = onOpenMembershipSignUp,
            border = BorderStroke(1.4.dp, LgColors.GoldBright.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("welcome_membership_signup_button")
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = LgColors.GoldBright,
                modifier = Modifier.padding(end = 8.dp).size(20.dp)
            )
            Text(
                text = "MEMBERSHIP SIGN-UP & BILLING PLANS",
                style = TabLabelTextStyle,
                color = LgColors.GoldBright
            )
        }

        OutlinedButton(
            onClick = onEnterDeskDirect,
            border = BorderStroke(1.3.dp, LgColors.Turquoise.copy(alpha = 0.78f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("welcome_enter_desk_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = LgColors.Turquoise,
                modifier = Modifier.padding(end = 8.dp).size(20.dp)
            )
            Text(
                text = if (existingSession != null) {
                    "ENTER DESK AS ${existingSession.displayName.uppercase()}"
                } else {
                    "ENTER LIVE DESK"
                },
                style = TabLabelTextStyle,
                color = LgColors.Turquoise
            )
        }

        OutlinedButton(
            onClick = onOpenThemePreviews,
            border = BorderStroke(1.3.dp, LgColors.CyanGlow.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("welcome_theme_previews_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = null,
                tint = LgColors.CyanGlow,
                modifier = Modifier.padding(end = 8.dp).size(20.dp)
            )
            Text(
                text = "8 COLOR SCHEME PREVIEWS",
                style = TabLabelTextStyle,
                color = LgColors.CyanGlow
            )
        }

        // Social Media Links right on Welcome Screen
        CyberFrameCard(
            title = "FOLLOW LG DIGITAL",
            modifier = Modifier.fillMaxWidth()
        ) {
            SocialMediaLinksBar(compact = true)
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WelcomeFeaturePill(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .circuitTextFieldBackdrop(cornerRadius = 10.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = LgColors.InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MemberSignInContent(
    onBackToWelcome: () -> Unit,
    onOpenMembershipSignUp: () -> Unit,
    onSignedIn: (MemberSession) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var activeSocialDialog by remember { mutableStateOf<SocialPlatform?>(null) }
    var socialHandleInput by remember { mutableStateOf("") }

    activeSocialDialog?.let { provider ->
        val promptLabel = when (provider) {
            SocialPlatform.GOOGLE -> "Google Email (e.g. trader@gmail.com)"
            SocialPlatform.FACEBOOK -> "Facebook Name or Email"
            SocialPlatform.TELEGRAM -> "Telegram @username"
            else -> "Account Handle"
        }
        val portalUrl = when (provider) {
            SocialPlatform.GOOGLE -> "https://accounts.google.com"
            SocialPlatform.FACEBOOK -> "https://www.facebook.com/login/"
            SocialPlatform.TELEGRAM -> "https://oauth.telegram.org"
            else -> provider.url
        }

        AlertDialog(
            onDismissRequest = { activeSocialDialog = null },
            containerColor = LgColors.PanelSlate,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SocialBrandBadge(platform = provider, size = 30.dp)
                    Text(
                        text = "Continue with ${provider.label}",
                        style = MaterialTheme.typography.titleLarge,
                        color = LgColors.GoldBright
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Connect your ${provider.label} member profile to unlock your LG Digital desk session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkSecondary
                    )
                    OutlinedTextField(
                        value = socialHandleInput,
                        onValueChange = { socialHandleInput = it },
                        label = { Text(promptLabel) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LgColors.Turquoise,
                            unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .circuitTextFieldBackdrop(cornerRadius = 10.dp, drawBorder = false)
                            .testTag("signin_provider_handle_input")
                    )
                    OutlinedButton(
                        onClick = { runCatching { uriHandler.openUri(portalUrl) } },
                        border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = LgColors.Turquoise,
                            modifier = Modifier.size(16.dp).padding(end = 6.dp)
                        )
                        Text("Open ${provider.label} Portal", color = LgColors.Turquoise)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = socialHandleInput.trim()
                        val display = when {
                            cleaned.isNotBlank() -> cleaned.substringBefore("@").ifBlank { cleaned }
                            else -> "${provider.label} Member"
                        }
                        val handle = cleaned.ifBlank { "${provider.label.lowercase()}@lgdigital.ca" }
                        activeSocialDialog = null
                        socialHandleInput = ""
                        onSignedIn(
                            MemberSession(
                                displayName = display,
                                provider = provider.label,
                                handleOrEmail = handle
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LgColors.GoldBright,
                        contentColor = LgColors.ObsidianBg
                    ),
                    modifier = Modifier.testTag("confirm_social_signin_button")
                ) {
                    Text("Sign In", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSocialDialog = null }) {
                    Text("Cancel", color = LgColors.InkSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToWelcome,
                modifier = Modifier.testTag("signin_back_to_welcome_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Welcome Screen",
                    tint = LgColors.GoldBright
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_user_lg_coin),
                    contentDescription = "LG Digital logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.2.dp, LgColors.Gold, CircleShape)
                )
                Text(
                    text = "LG DIGITAL",
                    fontFamily = LgBrandFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = 2.2.sp,
                    color = LgColors.GoldBright
                )
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        CyberFrameCard(
            title = "MEMBER SIGN IN",
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sign in with Google, Facebook, Telegram, or your Member Email to access your LG Digital desk.",
                style = MaterialTheme.typography.bodyMedium,
                color = LgColors.InkSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // 1. GOOGLE Sign-In Button
            Button(
                onClick = {
                    socialHandleInput = ""
                    activeSocialDialog = SocialPlatform.GOOGLE
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("signin_google_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialBrandBadge(platform = SocialPlatform.GOOGLE, size = 24.dp)
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                }
            }

            // 2. FACEBOOK Sign-In Button
            Button(
                onClick = {
                    socialHandleInput = ""
                    activeSocialDialog = SocialPlatform.FACEBOOK
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("signin_facebook_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialBrandBadge(platform = SocialPlatform.FACEBOOK, size = 24.dp)
                    Text(
                        text = "Continue with Facebook",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 3. TELEGRAM Sign-In Button
            Button(
                onClick = {
                    socialHandleInput = ""
                    activeSocialDialog = SocialPlatform.TELEGRAM
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF229ED9),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("signin_telegram_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialBrandBadge(platform = SocialPlatform.TELEGRAM, size = 24.dp)
                    Text(
                        text = "Continue with Telegram",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = LgColors.Gold.copy(alpha = 0.35f))
                Text(
                    text = "OR EMAIL SIGN IN",
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = LgColors.Gold.copy(alpha = 0.35f))
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    emailError = null
                },
                label = { Text("Member Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = LgColors.Turquoise) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LgColors.Turquoise,
                    unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .circuitTextFieldBackdrop(cornerRadius = 12.dp, drawBorder = false)
                    .testTag("signin_email_input")
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    emailError = null
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = LgColors.Gold) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LgColors.Turquoise,
                    unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .circuitTextFieldBackdrop(cornerRadius = 12.dp, drawBorder = false)
                    .testTag("signin_password_input")
            )

            emailError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.labelMedium,
                    color = LgColors.Crimson
                )
            }

            GoldMetallicButton(
                text = "SIGN IN WITH EMAIL",
                onClick = {
                    val trimmed = emailInput.trim()
                    if (trimmed.isBlank() || !trimmed.contains("@")) {
                        emailError = "Enter a valid email address or choose Google, Facebook, or Telegram above."
                    } else {
                        onSignedIn(
                            MemberSession(
                                displayName = trimmed.substringBefore("@"),
                                provider = "Email",
                                handleOrEmail = trimmed
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("signin_email_submit_button")
            )

            OutlinedButton(
                onClick = onOpenMembershipSignUp,
                border = BorderStroke(1.2.dp, LgColors.GoldBright.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("signin_membership_signup_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = LgColors.GoldBright,
                    modifier = Modifier.size(18.dp).padding(end = 6.dp)
                )
                Text("New Member? Sign Up & Billing Plans", color = LgColors.GoldBright)
            }

            TextButton(
                onClick = {
                    onSignedIn(
                        MemberSession(
                            displayName = "LG Guest",
                            provider = "Paper Desk",
                            handleOrEmail = "Local Paper Session"
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signin_guest_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = LgColors.Turquoise,
                    modifier = Modifier.size(18.dp).padding(end = 6.dp)
                )
                Text("Continue to Paper Trading Desk", color = LgColors.Turquoise)
            }
        }

        CyberFrameCard(
            title = "COMMUNITY & SOCIAL CHANNELS",
            modifier = Modifier.fillMaxWidth()
        ) {
            SocialMediaLinksBar(compact = true)
        }
    }
}

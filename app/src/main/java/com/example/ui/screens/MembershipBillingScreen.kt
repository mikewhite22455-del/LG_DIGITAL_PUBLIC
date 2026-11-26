package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.Formatters
import com.example.domain.models.BaseFiatCurrency
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LegalAndDisclosureDialog
import com.example.ui.components.LegalDisclosureTab
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.MetricTile
import com.example.ui.components.SocialMediaLinksBar
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.ui.theme.TradingViewFontFamily
import kotlin.math.abs

enum class MembershipTier(
    val id: String,
    val title: String,
    val badge: String,
    val monthlyUsd: Double,
    val annualUsd: Double,
    val features: List<String>
) {
    STARTER(
        id = "starter",
        title = "Starter Desk",
        badge = "FREE ACCESS",
        monthlyUsd = 0.0,
        annualUsd = 0.0,
        features = listOf(
            "Live Crypto Fear & Greed Index Gauge",
            "Real-Time Spot Price Feeds & CoinDesk News Wire",
            "Paper Trading Ticket & Quant Strategy Previews",
            "Universal Multi-Coin Miner PoW Quotes"
        )
    ),
    PRO(
        id = "pro",
        title = "Pro Trader",
        badge = "MOST POPULAR",
        monthlyUsd = 29.99,
        annualUsd = 287.90,
        features = listOf(
            "Full Adaptive USD/CAD Whole-Exchange Profit Scanner",
            "Unlimited Live TradingView Charts & Indicator Backtester",
            "Cross-Exchange Arbitrage Depth Scanner & Alerts",
            "Official Sports League Edge, De-Vig & Kelly Calculators",
            "AI Quant Copilot Trading Advisor"
        )
    ),
    ELITE(
        id = "elite",
        title = "Elite Desk",
        badge = "FULL ACCESS",
        monthlyUsd = 79.99,
        annualUsd = 767.90,
        features = listOf(
            "Everything in Pro Trader + Priority Execution Insights",
            "Multi-Exchange Adaptive Autopilot & Custom Stop Schedules",
            "Push, Email & Webhook Price Trigger Routing",
            "Multi-Rig Universal ASIC/GPU Miner Portfolio Tools",
            "VIP Telegram Signals & Commercial Member Support"
        )
    );

    companion object {
        fun fromId(id: String?): MembershipTier = entries.firstOrNull { it.id == id } ?: STARTER
    }
}

enum class BillingCycle(val id: String, val label: String) {
    MONTHLY("monthly", "Monthly"),
    ANNUAL("annual", "Annual (Save 20%)");

    companion object {
        fun fromId(id: String?): BillingCycle = entries.firstOrNull { it.id == id } ?: MONTHLY
    }
}

enum class BillingPaymentMethod(val id: String, val label: String, val subtitle: String) {
    GOOGLE_PLAY("google_play", "Google Play Open Access", "All Android Command features included free · Subscriptions managed exclusively via Google Play"),
    HOSTED_CARD("hosted_card", "Google Play Billing", "Managed natively through Google Play Store"),
    CRYPTO("crypto", "Google Play Billing", "Managed natively through Google Play Store");

    companion object {
        fun fromId(id: String?): BillingPaymentMethod = entries.firstOrNull { it.id == id } ?: GOOGLE_PLAY
    }
}

data class MembershipBillingProfile(
    val memberId: String = "",
    val tier: MembershipTier = MembershipTier.STARTER,
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val billingCurrency: BaseFiatCurrency = BaseFiatCurrency.USD,
    val paymentMethod: BillingPaymentMethod = BillingPaymentMethod.GOOGLE_PLAY,
    val fullName: String = "",
    val email: String = "",
    val phoneOrTelegram: String = "",
    val companyName: String = "",
    val billingAddress: String = "",
    val billingCity: String = "",
    val billingRegion: String = "",
    val billingPostalCode: String = "",
    val billingCountry: String = "Canada",
    val taxId: String = "",
    val promoCode: String = "",
    val isEnrolled: Boolean = false,
    val updatedAtMs: Long = 0L
)

object MembershipBillingPrefs {
    private const val PREFS_NAME = "lg_digital_membership_billing"

    fun load(context: Context): MembershipBillingProfile {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currencyCode = p.getString("billing_currency", "USD") ?: "USD"
        return MembershipBillingProfile(
            memberId = p.getString("member_id", "") ?: "",
            tier = MembershipTier.fromId(p.getString("tier_id", MembershipTier.STARTER.id)),
            billingCycle = BillingCycle.fromId(p.getString("billing_cycle", BillingCycle.MONTHLY.id)),
            billingCurrency = if (currencyCode == "CAD") BaseFiatCurrency.CAD else BaseFiatCurrency.USD,
            paymentMethod = BillingPaymentMethod.fromId(p.getString("payment_method", BillingPaymentMethod.GOOGLE_PLAY.id)),
            fullName = p.getString("full_name", "") ?: "",
            email = p.getString("email", "") ?: "",
            phoneOrTelegram = p.getString("phone_telegram", "") ?: "",
            companyName = p.getString("company_name", "") ?: "",
            billingAddress = p.getString("billing_address", "") ?: "",
            billingCity = p.getString("billing_city", "") ?: "",
            billingRegion = p.getString("billing_region", "") ?: "",
            billingPostalCode = p.getString("billing_postal", "") ?: "",
            billingCountry = p.getString("billing_country", "Canada") ?: "Canada",
            taxId = p.getString("tax_id", "") ?: "",
            promoCode = p.getString("promo_code", "") ?: "",
            isEnrolled = p.getBoolean("is_enrolled", false),
            updatedAtMs = p.getLong("updated_at_ms", 0L)
        )
    }

    fun save(context: Context, profile: MembershipBillingProfile): MembershipBillingProfile {
        val assignedId = profile.memberId.ifBlank {
            val hash = abs((profile.email.lowercase() + profile.fullName + System.currentTimeMillis()).hashCode()) % 900000 + 100000
            "LGD-$hash"
        }
        val finalProfile = profile.copy(
            memberId = assignedId,
            isEnrolled = true,
            updatedAtMs = System.currentTimeMillis()
        )
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("member_id", finalProfile.memberId)
            .putString("tier_id", finalProfile.tier.id)
            .putString("billing_cycle", finalProfile.billingCycle.id)
            .putString("billing_currency", finalProfile.billingCurrency.code)
            .putString("payment_method", finalProfile.paymentMethod.id)
            .putString("full_name", finalProfile.fullName)
            .putString("email", finalProfile.email)
            .putString("phone_telegram", finalProfile.phoneOrTelegram)
            .putString("company_name", finalProfile.companyName)
            .putString("billing_address", finalProfile.billingAddress)
            .putString("billing_city", finalProfile.billingCity)
            .putString("billing_region", finalProfile.billingRegion)
            .putString("billing_postal", finalProfile.billingPostalCode)
            .putString("billing_country", finalProfile.billingCountry)
            .putString("tax_id", finalProfile.taxId)
            .putString("promo_code", finalProfile.promoCode)
            .putBoolean("is_enrolled", true)
            .putLong("updated_at_ms", finalProfile.updatedAtMs)
            .apply()
        return finalProfile
    }
}

@Composable
fun MembershipBillingScreen(
    currentProfile: MembershipBillingProfile,
    usdToCadRate: Double = 1.38,
    onProfileSaved: (MembershipBillingProfile) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var selectedTier by remember(currentProfile.tier) { mutableStateOf(currentProfile.tier) }
    var billingCycle by remember(currentProfile.billingCycle) { mutableStateOf(currentProfile.billingCycle) }
    var billingCurrency by remember(currentProfile.billingCurrency) { mutableStateOf(currentProfile.billingCurrency) }
    var paymentMethod by remember(currentProfile.paymentMethod) { mutableStateOf(currentProfile.paymentMethod) }

    var fullName by remember(currentProfile.fullName) { mutableStateOf(currentProfile.fullName) }
    var email by remember(currentProfile.email) { mutableStateOf(currentProfile.email) }
    var phoneOrTelegram by remember(currentProfile.phoneOrTelegram) { mutableStateOf(currentProfile.phoneOrTelegram) }
    var companyName by remember(currentProfile.companyName) { mutableStateOf(currentProfile.companyName) }

    var billingAddress by remember(currentProfile.billingAddress) { mutableStateOf(currentProfile.billingAddress) }
    var billingCity by remember(currentProfile.billingCity) { mutableStateOf(currentProfile.billingCity) }
    var billingRegion by remember(currentProfile.billingRegion) { mutableStateOf(currentProfile.billingRegion) }
    var billingPostal by remember(currentProfile.billingPostalCode) { mutableStateOf(currentProfile.billingPostalCode) }
    var billingCountry by remember(currentProfile.billingCountry) { mutableStateOf(currentProfile.billingCountry) }
    var taxId by remember(currentProfile.taxId) { mutableStateOf(currentProfile.taxId) }
    var promoCode by remember(currentProfile.promoCode) { mutableStateOf(currentProfile.promoCode) }

    var acceptedTerms by remember(currentProfile.isEnrolled) { mutableStateOf(currentProfile.isEnrolled) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var confirmationBanner by remember { mutableStateOf<String?>(null) }
    var activeLegalDialogTab by remember { mutableStateOf<LegalDisclosureTab?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = LgColors.Turquoise,
        unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    val selectedPriceFormatted = "FREE (OPEN ACCESS)"

    Surface(
        color = LgColors.ObsidianBg,
        modifier = Modifier
            .fillMaxSize()
            .testTag("membership_billing_screen")
    ) {
        LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.testTag("membership_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = LgColors.GoldBright
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.lg_digital_emblem),
                                    contentDescription = "LG Digital membership crest",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .border(1.3.dp, LgColors.GoldBright, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = "MEMBER PROFILE & ACCESS",
                                        fontFamily = TradingViewFontFamily,
                                        fontSize = 15.sp,
                                        color = LgColors.GoldBright
                                    )
                                    Text(
                                        text = "Android Command Open Access · Google Play Compliant",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.Turquoise
                                    )
                                }
                            }
                            StatusPill(
                                text = currentProfile.tier.title.uppercase(),
                                color = if (currentProfile.tier == MembershipTier.STARTER) LgColors.Turquoise else LgColors.GoldBright
                            )
                        }
                    }

                    // Active Membership Pass (shown once enrolled)
                    if (currentProfile.isEnrolled) {
                        item {
                            CyberFrameCard(title = "ACTIVE MEMBER PASS") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.WorkspacePremium,
                                            contentDescription = null,
                                            tint = LgColors.GoldBright,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "${currentProfile.fullName.ifBlank { "LG Digital Member" }} · ${currentProfile.tier.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = LgColors.GoldBright
                                            )
                                            Text(
                                                text = "Member ID: ${currentProfile.memberId} · ${currentProfile.email}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LgColors.Turquoise
                                            )
                                        }
                                    }
                                    StatusPill("ACTIVE", LgColors.Turquoise)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MetricTile(
                                        label = "Billing Cycle",
                                        value = currentProfile.billingCycle.label,
                                        valueColor = LgColors.InkPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Currency",
                                        value = currentProfile.billingCurrency.code,
                                        valueColor = LgColors.GoldBright,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricTile(
                                        label = "Payment",
                                        value = currentProfile.paymentMethod.label.substringBefore(" "),
                                        valueColor = LgColors.Turquoise,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    confirmationBanner?.let { msg ->
                        item {
                            GlassPanelCard(highlightBorder = LgColors.Turquoise.copy(alpha = 0.7f)) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = LgColors.Turquoise)
                                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = LgColors.Turquoise)
                                }
                            }
                        }
                    }

                    // 1. Choose Plan + Billing Currency & Cycle
                    item {
                        CyberFrameCard(title = "1. SELECT MEMBERSHIP PLAN") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BillingCycle.entries.forEach { cycle ->
                                    FilterChip(
                                        selected = billingCycle == cycle,
                                        onClick = { billingCycle = cycle },
                                        label = { Text(cycle.label, style = TabLabelTextStyle, maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.24f),
                                            selectedLabelColor = LgColors.GoldBright
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("billing_cycle_${cycle.id}")
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BaseFiatCurrency.entries.forEach { cur ->
                                    FilterChip(
                                        selected = billingCurrency == cur,
                                        onClick = { billingCurrency = cur },
                                        label = { Text("Bill in ${cur.displayLabel}", style = TabLabelTextStyle) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LgColors.Gold.copy(alpha = 0.22f),
                                            selectedLabelColor = LgColors.GoldBright
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("billing_currency_${cur.code.lowercase()}")
                                    )
                                }
                            }

                            MembershipTier.entries.forEach { tier ->
                                val isSelected = tier == selectedTier
                                val priceLabel = "INCLUDED FREE"
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) LgColors.Turquoise.copy(alpha = 0.16f)
                                            else LgColors.ObsidianBg.copy(alpha = 0.72f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.6.dp else 1.dp,
                                            color = if (isSelected) LgColors.GoldBright else LgColors.BorderHairline,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedTier = tier }
                                        .padding(12.dp)
                                        .testTag("membership_tier_${tier.id}"),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = tier.title,
                                                fontFamily = TradingViewFontFamily,
                                                fontSize = 16.sp,
                                                color = if (isSelected) LgColors.GoldBright else LgColors.InkPrimary
                                            )
                                            StatusPill(
                                                text = tier.badge,
                                                color = if (tier == MembershipTier.PRO) LgColors.GoldBright else LgColors.Turquoise
                                            )
                                        }
                                        Text(
                                            text = priceLabel,
                                            fontFamily = TradingViewFontFamily,
                                            fontSize = 15.sp,
                                            color = LgColors.Turquoise
                                        )
                                    }
                                    tier.features.forEach { feat ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isSelected) LgColors.GoldBright else LgColors.Turquoise,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = feat,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = LgColors.InkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Member Sign-Up Registration Details
                    item {
                        CyberFrameCard(title = "2. MEMBER SIGN-UP PROFILE") {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it; validationError = null },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = LgColors.Turquoise) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(drawBorder = false)
                                    .testTag("membership_fullname_input")
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it; validationError = null },
                                label = { Text("Member Email Address *") },
                                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = LgColors.Turquoise) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(drawBorder = false)
                                    .testTag("membership_email_input")
                            )
                            OutlinedTextField(
                                value = phoneOrTelegram,
                                onValueChange = { phoneOrTelegram = it },
                                label = { Text("Phone or Telegram @handle") },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = LgColors.Gold) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(drawBorder = false)
                                    .testTag("membership_phone_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = companyName,
                                    onValueChange = { companyName = it },
                                    label = { Text("Desk / Company (Opt)") },
                                    leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null, tint = LgColors.Gold) },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .circuitTextFieldBackdrop(drawBorder = false)
                                )
                                OutlinedTextField(
                                    value = promoCode,
                                    onValueChange = { promoCode = it.uppercase() },
                                    label = { Text("Promo Code") },
                                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = LgColors.Turquoise) },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .circuitTextFieldBackdrop(drawBorder = false)
                                )
                            }
                        }
                    }

                    // 3. Access Channel & Profile Preferences
                    item {
                        CyberFrameCard(title = "3. GOOGLE PLAY ACCESS & PROFILE") {
                            Text(
                                text = "DISTRIBUTION & SUBSCRIPTION CHANNEL",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Gold
                            )
                            val playMethod = BillingPaymentMethod.GOOGLE_PLAY
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LgColors.Turquoise.copy(alpha = 0.16f))
                                    .border(
                                        width = 1.4.dp,
                                        color = LgColors.GoldBright,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { paymentMethod = playMethod }
                                    .padding(10.dp)
                                    .testTag("billing_method_google_play"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CreditCard,
                                    contentDescription = null,
                                    tint = LgColors.GoldBright
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playMethod.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LgColors.GoldBright
                                    )
                                    Text(
                                        text = playMethod.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.InkSecondary
                                    )
                                }
                                StatusPill("ACTIVE", LgColors.Turquoise)
                            }

                            HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.25f))

                            Text(
                                text = "BILLING ADDRESS & INVOICE DETAILS",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Gold
                            )
                            OutlinedTextField(
                                value = billingAddress,
                                onValueChange = { billingAddress = it; validationError = null },
                                label = { Text("Street Address") },
                                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = LgColors.Turquoise) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(drawBorder = false)
                                    .testTag("billing_address_input")
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = billingCity,
                                    onValueChange = { billingCity = it },
                                    label = { Text("City") },
                                    leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null, tint = LgColors.Turquoise) },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1f)
                                        .circuitTextFieldBackdrop(drawBorder = false)
                                )
                                OutlinedTextField(
                                    value = billingRegion,
                                    onValueChange = { billingRegion = it },
                                    label = { Text("Prov / State") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(0.85f)
                                        .circuitTextFieldBackdrop(drawBorder = false)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = billingPostal,
                                    onValueChange = { billingPostal = it.uppercase() },
                                    label = { Text("Postal / ZIP") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .circuitTextFieldBackdrop(drawBorder = false)
                                )
                                OutlinedTextField(
                                    value = billingCountry,
                                    onValueChange = { billingCountry = it },
                                    label = { Text("Country") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .circuitTextFieldBackdrop(drawBorder = false)
                                )
                            }
                            OutlinedTextField(
                                value = taxId,
                                onValueChange = { taxId = it },
                                label = { Text("Business Tax / HST / VAT ID (Optional)") },
                                leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = LgColors.Gold) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(drawBorder = false)
                            )

                            // Terms & Auto-Renewal Compliance Notice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { acceptedTerms = !acceptedTerms },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = acceptedTerms,
                                    onCheckedChange = { acceptedTerms = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LgColors.Turquoise,
                                        checkmarkColor = LgColors.ObsidianBg
                                    ),
                                    modifier = Modifier.testTag("membership_terms_checkbox")
                                )
                                Text(
                                    text = "I acknowledge the LG Digital Terms, Non-Custodial Trading Risk Notice, and On-Device Privacy Policy. All Android Command features are unlocked free in this release; any future optional subscriptions are managed strictly via Google Play.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.InkSecondary
                                )
                            }

                            validationError?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LgColors.Crimson
                                )
                            }

                            // Order Summary Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(cornerRadius = 10.dp)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedTier.title} · ${billingCycle.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LgColors.GoldBright
                                    )
                                    Text(
                                        text = "Via ${paymentMethod.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.InkSecondary
                                    )
                                }
                                Text(
                                    text = selectedPriceFormatted,
                                    fontFamily = TradingViewFontFamily,
                                    fontSize = 18.sp,
                                    color = LgColors.Turquoise
                                )
                            }

                            GoldMetallicButton(
                                text = "SAVE MEMBER PROFILE (OPEN ACCESS FREE)",
                                onClick = {
                                    val cleanName = fullName.trim()
                                    val cleanEmail = email.trim()
                                    when {
                                        cleanName.isBlank() -> {
                                            validationError = "Please enter your full name to register your membership."
                                        }
                                        cleanEmail.isBlank() || !cleanEmail.contains("@") -> {
                                            validationError = "Please enter a valid email address for membership receipts."
                                        }
                                        !acceptedTerms -> {
                                            validationError = "Please confirm the membership & billing terms above."
                                        }
                                        else -> {
                                            validationError = null
                                            val saved = MembershipBillingPrefs.save(
                                                context,
                                                currentProfile.copy(
                                                    tier = selectedTier,
                                                    billingCycle = billingCycle,
                                                    billingCurrency = billingCurrency,
                                                    paymentMethod = paymentMethod,
                                                    fullName = cleanName,
                                                    email = cleanEmail,
                                                    phoneOrTelegram = phoneOrTelegram.trim(),
                                                    companyName = companyName.trim(),
                                                    billingAddress = billingAddress.trim(),
                                                    billingCity = billingCity.trim(),
                                                    billingRegion = billingRegion.trim(),
                                                    billingPostalCode = billingPostal.trim(),
                                                    billingCountry = billingCountry.trim().ifBlank { "Canada" },
                                                    taxId = taxId.trim(),
                                                    promoCode = promoCode.trim()
                                                )
                                            )
                                            onProfileSaved(saved)
                                            confirmationBanner = "Membership saved! ID ${saved.memberId} · ${saved.tier.title} (${saved.billingCycle.label})."
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 54.dp)
                                    .testTag("membership_submit_button")
                            )

                            // Google Play Subscriptions & In-App Legal Disclosure (Play Policy Compliant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            uriHandler.openUri("https://play.google.com/store/account/subscriptions")
                                        }
                                    },
                                    border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("manage_google_play_subs_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = LgColors.Turquoise, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                    Text("Play Subscriptions", color = LgColors.Turquoise, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = {
                                        activeLegalDialogTab = LegalDisclosureTab.PRIVACY
                                    },
                                    border = BorderStroke(1.dp, LgColors.Gold.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("open_in_app_privacy_disclosure_button")
                                ) {
                                    Text("Privacy & Risk", color = LgColors.GoldBright, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Terms & Risk Notice",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise,
                                    modifier = Modifier
                                        .clickable { activeLegalDialogTab = LegalDisclosureTab.TERMS_RISK }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("membership_terms_link")
                                )
                                Text("·", color = LgColors.InkMuted)
                                Text(
                                    text = "Privacy Policy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise,
                                    modifier = Modifier
                                        .clickable { activeLegalDialogTab = LegalDisclosureTab.PRIVACY }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("membership_privacy_link")
                                )
                            }
                        }
                    }

                    // Community & Support Links
                    item {
                        CyberFrameCard(title = "MEMBER COMMUNITY & SUPPORT") {
                            SocialMediaLinksBar(compact = true)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    activeLegalDialogTab?.let { initialTab ->
        LegalAndDisclosureDialog(
            initialTab = initialTab,
            onDismiss = { activeLegalDialogTab = null }
        )
    }
}

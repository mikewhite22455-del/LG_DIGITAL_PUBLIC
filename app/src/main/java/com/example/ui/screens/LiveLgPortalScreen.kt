package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.domain.models.LiveServerHealth
import com.example.domain.models.LiveServerTarget
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle

private val ALLOWED_LG_HOSTS = setOf(
    "lg-digital-private.onrender.com",
    "lg-digital-owner.onrender.com",
    "lg-digital.ca",
    "www.lg-digital.ca"
)

private val ALLOWED_STRIPE_HOSTS = setOf(
    "checkout.stripe.com",
    "billing.stripe.com",
    "invoice.stripe.com",
    "connect.stripe.com",
    "pay.stripe.com",
    "hooks.stripe.com",
    "stripe.com",
    "www.stripe.com"
)

private fun isAllowedEmbeddedHost(host: String): Boolean {
    val normalized = host.lowercase()
    return normalized in ALLOWED_LG_HOSTS ||
        normalized in ALLOWED_STRIPE_HOSTS ||
        normalized.endsWith(".stripe.com")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveLgPortalScreen(
    activeTarget: LiveServerTarget,
    privateHealth: LiveServerHealth,
    ownerHealth: LiveServerHealth,
    isPrimaryLauncher: Boolean = true,
    onSelectTarget: (LiveServerTarget) -> Unit,
    onPingTarget: (LiveServerTarget) -> Unit,
    onOpenPriceSummary: (() -> Unit)? = null,
    onOpenExchangeConfig: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasFinishedFirstPaint by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var loadedTargetId by remember { mutableStateOf(activeTarget.id) }
    var currentUrlDisplay by remember { mutableStateOf(activeTarget.baseUrl) }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentHealth = if (activeTarget == LiveServerTarget.LG_PRIVATE) privateHealth else ownerHealth
    val isOnStripePage = currentUrlDisplay.contains("stripe.com", ignoreCase = true)

    BackHandler(enabled = canGoBack || !isPrimaryLauncher) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else if (!isPrimaryLauncher) {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { CookieManager.getInstance().flush() }
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Compact Brand Header so lg-digital-private.onrender.com owns ~93% of the screen
            Surface(
                color = LgColors.PanelSlate.copy(alpha = 0.96f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_user_lg_coin),
                                contentDescription = "LG Digital live emblem",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, LgColors.Gold, CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "LG DIGITAL",
                                        fontFamily = LgBrandFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        letterSpacing = 1.6.sp,
                                        color = LgColors.GoldBright,
                                        maxLines = 1
                                    )
                                    StatusPill(
                                        text = when {
                                            isOnStripePage -> "STRIPE"
                                            activeTarget == LiveServerTarget.LG_PRIVATE -> "PRIVATE LIVE"
                                            else -> "OWNER LIVE"
                                        },
                                        color = if (isOnStripePage) LgColors.GoldBright else LgColors.Turquoise
                                    )
                                }
                                Text(
                                    text = if (isOnStripePage) {
                                        "Secure Stripe Billing & Checkout"
                                    } else {
                                        "${activeTarget.host} · ${if (currentHealth.isOnline) "ONLINE" else "CONNECTING"}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (currentHealth.isOnline || isOnStripePage) LgColors.Turquoise else LgColors.Gold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canGoBack) {
                                IconButton(
                                    onClick = { webViewRef?.goBack() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("live_portal_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Navigate back",
                                        tint = LgColors.Turquoise
                                    )
                                }
                            }
                            if (onOpenPriceSummary != null) {
                                IconButton(
                                    onClick = onOpenPriceSummary,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("live_portal_price_summary_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TrendingUp,
                                        contentDescription = "Open Price Summary Dashboard",
                                        tint = LgColors.GoldBright
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    onPingTarget(activeTarget)
                                    pageError = null
                                    webViewRef?.reload()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("live_portal_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reload LG Digital Private",
                                    tint = LgColors.Turquoise
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("live_portal_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Portal options",
                                        tint = LgColors.GoldBright
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier
                                        .background(LgColors.PanelSlate)
                                        .border(1.dp, LgColors.Gold.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Home (lg-digital-private)",
                                                color = LgColors.GoldBright,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Home, contentDescription = null, tint = LgColors.Turquoise)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onSelectTarget(LiveServerTarget.LG_PRIVATE)
                                            pageError = null
                                            loadedTargetId = LiveServerTarget.LG_PRIVATE.id
                                            webViewRef?.loadUrl(LiveServerTarget.LG_PRIVATE.baseUrl)
                                        },
                                        modifier = Modifier.testTag("portal_menu_home_private")
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Switch to Owner Portal",
                                                color = LgColors.InkPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.WifiTethering, contentDescription = null, tint = LgColors.Gold)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onSelectTarget(LiveServerTarget.LG_OWNER)
                                            pageError = null
                                            loadedTargetId = LiveServerTarget.LG_OWNER.id
                                            webViewRef?.loadUrl(LiveServerTarget.LG_OWNER.baseUrl)
                                        },
                                        modifier = Modifier.testTag("portal_menu_switch_owner")
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Open Current Page in Chrome",
                                                color = LgColors.InkPrimary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = LgColors.Turquoise)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            val targetUrl = currentUrlDisplay
                                                .takeIf { it.startsWith("https://", ignoreCase = true) }
                                                ?: activeTarget.baseUrl
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                            }
                                        },
                                        modifier = Modifier.testTag("portal_menu_open_browser")
                                    )
                                    if (onOpenPriceSummary != null) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Price Summary Dashboard",
                                                    color = LgColors.Turquoise,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = LgColors.Turquoise)
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onOpenPriceSummary()
                                            },
                                            modifier = Modifier.testTag("portal_menu_price_summary")
                                        )
                                    }
                                    if (onOpenExchangeConfig != null) {
                                        HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.25f))
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Local API Keys & QR Scanner",
                                                    color = LgColors.InkPrimary
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Key, contentDescription = null, tint = LgColors.Gold)
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                onOpenExchangeConfig()
                                            },
                                            modifier = Modifier.testTag("portal_menu_exchange_config")
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isPrimaryLauncher) "Native Quant Tools" else "Back to Native App",
                                                color = LgColors.InkSecondary
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (isPrimaryLauncher) Icons.Filled.Dashboard else Icons.Filled.Close,
                                                contentDescription = null,
                                                tint = LgColors.Gold
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onClose()
                                        },
                                        modifier = Modifier.testTag("portal_menu_native_tools")
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading && loadProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { loadProgress / 100f },
                            color = LgColors.Turquoise,
                            trackColor = LgColors.ObsidianBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    } else {
                        HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.5f), thickness = 1.dp)
                    }
                }
            }

            // Full-Screen Live WebView Container (LG Private + Stripe Checkout & Billing)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("live_lg_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#03141B"))
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportMultipleWindows(true)
                                javaScriptCanOpenWindowsAutomatically = true
                                allowFileAccess = false
                                allowContentAccess = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress
                                    isLoading = newProgress < 100
                                    if (newProgress >= 70) {
                                        hasFinishedFirstPaint = true
                                    }
                                }

                                // Handle target="_blank" links (e.g., Stripe Checkout / Billing Portal buttons)
                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                    val tempWebView = WebView(ctx).apply {
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                popupView: WebView?,
                                                req: WebResourceRequest?
                                            ): Boolean {
                                                val targetUri = req?.url ?: return true
                                                val scheme = targetUri.scheme?.lowercase()
                                                if (scheme != "https") {
                                                    popupView?.destroy()
                                                    return true
                                                }
                                                val host = targetUri.host?.lowercase().orEmpty()
                                                if (isAllowedEmbeddedHost(host)) {
                                                    view?.loadUrl(targetUri.toString())
                                                } else {
                                                    runCatching {
                                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
                                                    }
                                                }
                                                popupView?.destroy()
                                                return true
                                            }
                                        }
                                    }
                                    transport.webView = tempWebView
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return true
                                    val scheme = uri.scheme?.lowercase()
                                    if (scheme != "https") {
                                        // Block http:, file:, javascript:, content: inside live portal
                                        return true
                                    }
                                    val host = uri.host?.lowercase().orEmpty()
                                    if (isAllowedEmbeddedHost(host)) {
                                        return false
                                    }
                                    // Hand off non-allowlisted external HTTPS links to system browser
                                    runCatching {
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    pageError = null
                                    if (!url.isNullOrBlank()) {
                                        currentUrlDisplay = url
                                    }
                                    canGoBack = view?.canGoBack() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    hasFinishedFirstPaint = true
                                    if (!url.isNullOrBlank()) {
                                        currentUrlDisplay = url
                                    }
                                    canGoBack = view?.canGoBack() == true
                                    CookieManager.getInstance().flush()
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasFinishedFirstPaint = true
                                        pageError = "Could not reach ${activeTarget.host}. Render servers may be waking from sleep — tap Retry below."
                                    }
                                }
                            }
                            loadUrl(activeTarget.baseUrl)
                            webViewRef = this
                        }
                    },
                    update = { wv ->
                        if (loadedTargetId != activeTarget.id) {
                            loadedTargetId = activeTarget.id
                            hasFinishedFirstPaint = false
                            pageError = null
                            wv.loadUrl(activeTarget.baseUrl)
                        }
                        canGoBack = wv.canGoBack()
                    }
                )

                // Sleek cold-start waking splash while Render spins up on initial launch
                if (isLoading && !hasFinishedFirstPaint && pageError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LgColors.ObsidianBg.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassPanelCard(
                            highlightBorder = LgColors.Turquoise,
                            modifier = Modifier
                                .padding(24.dp)
                                .widthIn(max = 380.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_user_lg_coin),
                                    contentDescription = "LG Digital",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, LgColors.GoldBright, CircleShape)
                                )
                                Text(
                                    text = "LG DIGITAL",
                                    fontFamily = LgBrandFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 2.2.sp,
                                    color = LgColors.GoldBright
                                )
                                CircularProgressIndicator(
                                    color = LgColors.Turquoise,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = "Connecting to ${activeTarget.host}...",
                                    style = TabLabelTextStyle,
                                    color = LgColors.Turquoise,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Stripe Billing · Kraken Pro · Coinbase Live",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Friendly retry panel if Render cold-starts or connection drops
                pageError?.let { err ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                            .widthIn(max = 420.dp)
                    ) {
                        GlassPanelCard(highlightBorder = LgColors.Gold) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = LgColors.GoldBright,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Waking Live Server",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LgColors.GoldBright,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LgColors.InkSecondary,
                                    textAlign = TextAlign.Center
                                )
                                GoldMetallicButton(
                                    text = "RETRY CONNECTION",
                                    onClick = {
                                        pageError = null
                                        hasFinishedFirstPaint = false
                                        onPingTarget(activeTarget)
                                        webViewRef?.loadUrl(activeTarget.baseUrl)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("live_portal_retry_button")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

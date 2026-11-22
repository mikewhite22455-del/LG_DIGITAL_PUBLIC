package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.core.Formatters
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.SignalAction
import com.example.domain.models.StrategyType
import com.example.ui.theme.LgColors
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TabLabelTextStyle
import com.example.ui.theme.TradingViewFontFamily
import com.example.viewmodel.ArbitrageUiState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Maps a crypto ticker symbol to its live TradingView chart symbol & URL.
 */
object TradingViewLinks {
    fun tvSymbolFor(symbol: String): String {
        val clean = symbol.trim().uppercase()
            .removeSuffix("USD")
            .removeSuffix("-USD")
            .removeSuffix("/USD")
        return "BINANCE:${clean}USDT"
    }

    fun chartUrlFor(symbol: String): String {
        val clean = symbol.trim().uppercase()
            .removeSuffix("USD")
            .removeSuffix("-USD")
            .removeSuffix("/USD")
        return "https://www.tradingview.com/chart/?symbol=CRYPTO%3A${clean}USD"
    }
}

/**
 * Animated live pulse badge showing automatic live feed status.
 */
@Composable
fun LiveAutoFeedBadge(
    label: String = "LIVE AUTO FEED",
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = LgColors.Turquoise.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, LgColors.Turquoise.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LgColors.Turquoise.copy(alpha = pulseAlpha))
            )
            Text(
                text = label,
                fontFamily = RussoOneFontFamily,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                color = LgColors.Turquoise,
                maxLines = 1
            )
        }
    }
}

/**
 * High-contrast cyber radial meter (speedometer arc + glowing tick marks + dynamic needle).
 */
@Composable
fun RadialTelemetryGauge(
    label: String,
    value0To100: Float,
    readoutText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val clamped = value0To100.coerceIn(0f, 100f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.86f))
            .border(1.dp, LgColors.Gold.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val d = minOf(w * 0.86f, h * 1.62f)
                val left = (w - d) / 2f
                val top = h - d / 2f - 4f
                val center = Offset(w / 2f, top + d / 2f)
                val radius = d / 2f

                // Outer faint track
                drawArc(
                    color = LgColors.BorderHairline.copy(alpha = 0.55f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(d, d),
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )

                // Active gradient sweep
                drawArc(
                    brush = Brush.horizontalGradient(
                        listOf(LgColors.Crimson, LgColors.Gold, LgColors.Turquoise)
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f * (clamped / 100f),
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(d, d),
                    style = Stroke(width = 11f, cap = StrokeCap.Round)
                )

                // Crisp degree ticks
                for (t in 0..6) {
                    val deg = 180.0 + (t * 30.0)
                    val rad = deg * PI / 180.0
                    val innerR = radius - 14f
                    val outerR = radius - 8f
                    drawLine(
                        color = LgColors.Gold.copy(alpha = 0.55f),
                        start = Offset(
                            center.x + (innerR * cos(rad)).toFloat(),
                            center.y + (innerR * sin(rad)).toFloat()
                        ),
                        end = Offset(
                            center.x + (outerR * cos(rad)).toFloat(),
                            center.y + (outerR * sin(rad)).toFloat()
                        ),
                        strokeWidth = 1.6f
                    )
                }

                // Dynamic needle
                val needleDeg = 180.0 + (180.0 * (clamped / 100.0))
                val needleRad = needleDeg * PI / 180.0
                val needleLen = radius - 12f
                drawLine(
                    color = LgColors.GoldBright,
                    start = center,
                    end = Offset(
                        center.x + (needleLen * cos(needleRad)).toFloat(),
                        center.y + (needleLen * sin(needleRad)).toFloat()
                    ),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = accentColor, radius = 5f, center = center)
            }
            Text(
                text = readoutText,
                fontFamily = RussoOneFontFamily,
                fontSize = 13.sp,
                color = accentColor,
                maxLines = 1
            )
        }
        Text(
            text = label.uppercase(),
            fontFamily = RussoOneFontFamily,
            fontSize = 10.sp,
            letterSpacing = 0.7.sp,
            color = LgColors.Gold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Animated live sonar radar scanner plotting top live market movers on concentric rings.
 */
@Composable
fun LiveRadarScannerCanvas(
    tickers: List<MarketTicker>,
    onSelectTicker: (MarketTicker) -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val blips = remember(tickers) {
        tickers.sortedByDescending { abs(it.change24hPct) }.take(6)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.9f))
            .border(1.dp, LgColors.Turquoise.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated Radar Scope
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = minOf(cx, cy) - 4f

                // Concentric rings
                listOf(0.33f, 0.66f, 1.0f).forEach { frac ->
                    drawCircle(
                        color = LgColors.Turquoise.copy(alpha = 0.25f),
                        radius = maxR * frac,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.4f)
                    )
                }

                // Crosshair axes
                drawLine(LgColors.Gold.copy(alpha = 0.22f), Offset(cx - maxR, cy), Offset(cx + maxR, cy), 1.2f)
                drawLine(LgColors.Gold.copy(alpha = 0.22f), Offset(cx, cy - maxR), Offset(cx, cy + maxR), 1.2f)

                // Rotating radar sweep cone
                rotate(degrees = sweepAngle, pivot = Offset(cx, cy)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.78f to Color.Transparent,
                            0.94f to LgColors.Turquoise.copy(alpha = 0.26f),
                            1.0f to LgColors.Turquoise.copy(alpha = 0.72f),
                            center = Offset(cx, cy)
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(cx - maxR, cy - maxR),
                        size = Size(maxR * 2f, maxR * 2f)
                    )
                    drawLine(
                        color = LgColors.Turquoise,
                        start = Offset(cx, cy),
                        end = Offset(cx + maxR, cy),
                        strokeWidth = 2.4f,
                        cap = StrokeCap.Round
                    )
                }

                // Live mover radar blips
                blips.forEachIndexed { idx, ticker ->
                    val angleRad = ((idx * 58 + 24) * PI / 180.0)
                    val distFrac = (0.32f + (abs(ticker.change24hPct).toFloat().coerceAtMost(12f) / 18f)).coerceIn(0.28f, 0.90f)
                    val bx = cx + (maxR * distFrac * cos(angleRad)).toFloat()
                    val by = cy + (maxR * distFrac * sin(angleRad)).toFloat()
                    val blipColor = if (ticker.change24hPct >= 0.0) LgColors.Turquoise else LgColors.GoldBright
                    drawCircle(color = blipColor.copy(alpha = 0.35f), radius = 8f, center = Offset(bx, by))
                    drawCircle(color = blipColor, radius = 3.8f, center = Offset(bx, by))
                }

                // Center core dot
                drawCircle(color = LgColors.Gold, radius = 4f, center = Offset(cx, cy))
            }
        }

        // Live Scanned Signals Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Radar, contentDescription = null, tint = LgColors.Turquoise, modifier = Modifier.size(16.dp))
                Text(
                    text = "LIVE SCANNER",
                    fontFamily = RussoOneFontFamily,
                    fontSize = 12.sp,
                    color = LgColors.GoldBright,
                    letterSpacing = 0.8.sp
                )
            }
            if (blips.isEmpty()) {
                Text(
                    text = "Syncing live spot feeds...",
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.InkMuted
                )
            } else {
                blips.take(4).forEach { ticker ->
                    val up = ticker.change24hPct >= 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LgColors.ElevatedSlate.copy(alpha = 0.65f))
                            .clickable { onSelectTicker(ticker) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CoinAvatar(ticker.symbol, size = 18.dp)
                            Text(
                                text = ticker.symbol,
                                fontFamily = RussoOneFontFamily,
                                fontSize = 11.sp,
                                color = LgColors.InkPrimary
                            )
                        }
                        Text(
                            text = Formatters.formatPct(ticker.change24hPct),
                            fontFamily = RussoOneFontFamily,
                            fontSize = 11.sp,
                            color = if (up) LgColors.Turquoise else LgColors.Crimson
                        )
                    }
                }
            }
        }
    }
}

/**
 * Live embedded TradingView Advanced Chart powered by TradingView's official live feed widget.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewLiveChartWebView(
    symbol: String,
    interval: String = "60",
    height: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val tvSymbol = remember(symbol) { TradingViewLinks.tvSymbolFor(symbol) }
    val html = remember(tvSymbol, interval) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" />
            <style>
                html, body { margin: 0; padding: 0; width: 100%; height: 100%; background-color: #031218; overflow: hidden; }
                .tradingview-widget-container, #tv_chart { width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div class="tradingview-widget-container">
                <div id="tv_chart"></div>
                <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
                <script type="text/javascript">
                    if (typeof TradingView !== 'undefined') {
                        new TradingView.widget({
                            "autosize": true,
                            "symbol": "$tvSymbol",
                            "interval": "$interval",
                            "timezone": "Etc/UTC",
                            "theme": "dark",
                            "style": "1",
                            "locale": "en",
                            "backgroundColor": "#031218",
                            "gridColor": "rgba(26, 90, 107, 0.25)",
                            "hide_top_toolbar": false,
                            "hide_legend": false,
                            "save_image": false,
                            "allow_symbol_change": true,
                            "studies": ["RSI@tv-basicstudies", "MACD@tv-basicstudies"],
                            "container_id": "tv_chart"
                        });
                    }
                </script>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ObsidianBg)
            .border(1.3.dp, LgColors.Gold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#031218"))
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                }
            },
            update = { webView ->
                val tagKey = "$tvSymbol|$interval"
                if (webView.tag != tagKey) {
                    webView.tag = tagKey
                    webView.loadDataWithBaseURL("https://www.tradingview.com", html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Full-screen interactive Live TradingView Chart Dialog.
 */
@Composable
fun TradingViewFullChartDialog(
    symbol: String,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var interval by remember { mutableStateOf("60") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = LgColors.ObsidianBg,
            modifier = Modifier.fillMaxSize()
        ) {
            LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            CoinAvatar(symbol, size = 30.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$symbol ·",
                                        fontFamily = RussoOneFontFamily,
                                        fontSize = 15.sp,
                                        color = LgColors.GoldBright
                                    )
                                    TradingViewBrandMark(
                                        iconSize = 16.dp,
                                        showWordmark = true,
                                        tint = LgColors.InkPrimary
                                    )
                                }
                                Text(
                                    text = TradingViewLinks.tvSymbolFor(symbol),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    runCatching { uriHandler.openUri(TradingViewLinks.chartUrlFor(symbol)) }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in TradingView browser", tint = LgColors.Gold)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Close live chart", tint = LgColors.InkPrimary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("15" to "15M", "60" to "1H", "240" to "4H", "D" to "1D").forEach { (code, label) ->
                            FilterChip(
                                selected = interval == code,
                                onClick = { interval = code },
                                label = { Text(label, style = TabLabelTextStyle) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                                    selectedLabelColor = LgColors.Gold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    TradingViewLiveChartWebView(
                        symbol = symbol,
                        interval = interval,
                        height = 520.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Live Telemetry Hub for Home: Auto-running Radial Meters + Animated Live Radar Scanner + Embedded TradingView Chart.
 */
@Composable
fun LiveTelemetryCommandCard(
    selectedSymbol: String,
    breadth: MarketBreadth?,
    tickers: List<MarketTicker>,
    arbitrageState: ArbitrageUiState,
    onSelectTicker: (MarketTicker) -> Unit,
    onSelectSymbol: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var showEmbeddedTv by remember { mutableStateOf(false) }
    var showFullTvDialog by remember { mutableStateOf(false) }

    if (showFullTvDialog) {
        TradingViewFullChartDialog(
            symbol = selectedSymbol,
            onDismiss = { showFullTvDialog = false }
        )
    }

    val momentumScore = remember(breadth) {
        if (breadth == null || breadth.totalAssets == 0) 54f
        else {
            val advRatio = (breadth.advancing.toFloat() / breadth.totalAssets.toFloat()) * 100f
            val avgBoost = (breadth.avgChangePct.toFloat() * 4f).coerceIn(-25f, 25f)
            (advRatio * 0.75f + 25f + avgBoost).coerceIn(5f, 98f)
        }
    }
    val volatilityScore = remember(tickers) {
        if (tickers.isEmpty()) 42f
        else {
            val avgRangePct = tickers.mapNotNull { t ->
                if (t.low24h > 0.0) ((t.high24h - t.low24h) / t.low24h) * 100.0 else null
            }.average().takeIf { !it.isNaN() } ?: 4.0
            (avgRangePct.toFloat() * 8.5f).coerceIn(8f, 96f)
        }
    }
    val arbHeatScore = remember(arbitrageState.opportunities) {
        val qual = arbitrageState.opportunities.count { it.qualified }
        val bestBps = arbitrageState.opportunities.maxOfOrNull { it.worstCaseNetBps } ?: 0.0
        (qual * 18f + bestBps.toFloat().coerceAtLeast(0f) * 1.4f).coerceIn(10f, 98f)
    }

    CyberFrameCard(
        title = "LIVE METERS · SCANNER · TRADINGVIEW",
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveAutoFeedBadge(label = "AUTO LIVE · 15S")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = showEmbeddedTv,
                    onClick = { showEmbeddedTv = !showEmbeddedTv },
                    leadingIcon = {
                        Icon(Icons.Filled.CandlestickChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = {
                        Text(
                            text = if (showEmbeddedTv) "HIDE TV" else "TV LIVE",
                            style = TabLabelTextStyle
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.24f),
                        selectedLabelColor = LgColors.GoldBright
                    ),
                    modifier = Modifier.testTag("toggle_embedded_tv_chip")
                )
                FilterChip(
                    selected = false,
                    onClick = { showFullTvDialog = true },
                    leadingIcon = {
                        Icon(Icons.Filled.ShowChart, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("FULL", style = TabLabelTextStyle, color = LgColors.Gold) },
                    modifier = Modifier.testTag("open_full_tv_dialog_chip")
                )
            }
        }

        if (onSelectSymbol != null) {
            val symbols = remember(tickers) {
                if (tickers.isNotEmpty()) tickers.map { it.symbol }
                else listOf("BTC", "ETH", "SOL", "XRP", "ADA", "AVAX", "LINK", "DOT", "DOGE", "LTC")
            }
            CoinDropdownSelector(
                selectedSymbol = selectedSymbol,
                symbols = symbols,
                onSelectSymbol = onSelectSymbol,
                label = "Live TradingView & Telemetry Coin",
                testTag = "home_telemetry_coin_dropdown"
            )
        }

        // 3 Live Auto-Updating Radial Telemetry Meters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadialTelemetryGauge(
                label = "Momentum",
                value0To100 = momentumScore,
                readoutText = "${momentumScore.toInt()}%",
                accentColor = if (momentumScore >= 50f) LgColors.Turquoise else LgColors.Crimson,
                modifier = Modifier.weight(1f)
            )
            RadialTelemetryGauge(
                label = "Volatility",
                value0To100 = volatilityScore,
                readoutText = "${volatilityScore.toInt()}%",
                accentColor = LgColors.GoldBright,
                modifier = Modifier.weight(1f)
            )
            RadialTelemetryGauge(
                label = "Arb Heat",
                value0To100 = arbHeatScore,
                readoutText = "${arbHeatScore.toInt()}%",
                accentColor = LgColors.CyanGlow,
                modifier = Modifier.weight(1f)
            )
        }

        // Animated Live Radar Scanner
        LiveRadarScannerCanvas(
            tickers = tickers,
            onSelectTicker = onSelectTicker
        )

        if (showEmbeddedTv) {
            TradingViewLiveChartWebView(
                symbol = selectedSymbol,
                interval = "60",
                height = 290.dp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LgColors.ElevatedSlate.copy(alpha = 0.72f))
                .border(1.dp, LgColors.Gold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .clickable {
                    runCatching { uriHandler.openUri(TradingViewLinks.chartUrlFor(selectedSymbol)) }
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoinAvatar(selectedSymbol, size = 22.dp)
                Text(
                    text = "OPEN $selectedSymbol ON",
                    fontFamily = TradingViewFontFamily,
                    fontSize = 11.5.sp,
                    color = LgColors.GoldBright
                )
                TradingViewBrandMark(
                    iconSize = 16.dp,
                    showWordmark = true,
                    tint = LgColors.InkPrimary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open TradingView chart",
                tint = LgColors.Turquoise,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Tactile Chart-Image Button for launching or selecting a Trading Strategy.
 * Features the Gold & Turquoise chart banner art + unique live technical indicator graphic per strategy.
 */
@Composable
fun StrategyChartButtonCard(
    strategy: StrategyType,
    selected: Boolean,
    liveAction: SignalAction?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) LgColors.GoldBright else LgColors.Turquoise.copy(alpha = 0.42f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(if (selected) 2.dp else 1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("strategy_chart_button_${strategy.id}")
    ) {
        // Holographic Gold & Turquoise Chart Banner Image Background
        Image(
            painter = painterResource(id = R.drawable.img_trading_banner),
            contentDescription = "${strategy.title} strategy chart button",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Dark oceanic-teal readability gradient overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LgColors.ObsidianBg.copy(alpha = 0.90f),
                            LgColors.PanelSlate.copy(alpha = 0.72f),
                            LgColors.ObsidianBg.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strategy.title.uppercase(),
                        fontFamily = RussoOneFontFamily,
                        fontSize = 14.sp,
                        letterSpacing = 0.6.sp,
                        color = if (selected) LgColors.GoldBright else LgColors.InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected) {
                        StatusPill("ACTIVE", LgColors.Turquoise)
                    }
                }
                Text(
                    text = strategy.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.InkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (selected && liveAction != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    SignalBadge(liveAction)
                }
            }

            // Custom Mini Live-Chart Graphic Preview Badge inside the Button
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LgColors.ObsidianBg.copy(alpha = 0.82f))
                    .border(
                        1.2.dp,
                        if (selected) LgColors.Gold else LgColors.Turquoise.copy(alpha = 0.55f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(6.dp)
            ) {
                StrategyPatternMiniChart(strategy = strategy, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * Prominent Chart-Image Button Banner for opening the 11 Strategies gallery directly from Trade.
 */
@Composable
fun StrategiesHeroChartButton(
    activeStrategyTitle: String,
    isActiveTab: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isActiveTab) 2.dp else 1.4.dp,
                color = if (isActiveTab) LgColors.GoldBright else LgColors.Gold.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag("strategies_chart_image_button")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_trading_banner),
            contentDescription = "Strategies chart button",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LgColors.ObsidianBg.copy(alpha = 0.86f),
                            LgColors.ObsidianBg.copy(alpha = 0.42f),
                            LgColors.TealAmbient.copy(alpha = 0.30f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "QUANT STRATEGIES",
                    fontFamily = RussoOneFontFamily,
                    fontSize = 15.sp,
                    letterSpacing = 1.0.sp,
                    color = LgColors.GoldBright
                )
                Text(
                    text = "Active: $activeStrategyTitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.Turquoise,
                    fontWeight = FontWeight.Bold
                )
            }
            StatusPill(
                text = if (isActiveTab) "OPEN" else "TAP CHART",
                color = if (isActiveTab) LgColors.Turquoise else LgColors.GoldBright
            )
        }
    }
}

@Composable
private fun StrategyPatternMiniChart(
    strategy: StrategyType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val grid = LgColors.BorderHairline.copy(alpha = 0.35f)
        drawLine(grid, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)

        val patternIndex = strategy.ordinal % 4
        when (patternIndex) {
            0 -> {
                // Dual EMA Golden Cross + Bullish Candlesticks
                val p1 = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(w * 0.35f, h * 0.65f, w * 0.65f, h * 0.35f, w, h * 0.16f)
                }
                val p2 = Path().apply {
                    moveTo(0f, h * 0.62f)
                    cubicTo(w * 0.4f, h * 0.60f, w * 0.6f, h * 0.48f, w, h * 0.30f)
                }
                drawPath(p2, color = LgColors.Gold, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                drawPath(p1, color = LgColors.Turquoise, style = Stroke(width = 2.8f, cap = StrokeCap.Round))
                drawCircle(LgColors.GoldBright, radius = 3.5f, center = Offset(w * 0.44f, h * 0.54f))
            }
            1 -> {
                // Bollinger Envelope / Mean Reversion Wave
                val upper = Path().apply {
                    moveTo(0f, h * 0.25f); lineTo(w * 0.5f, h * 0.16f); lineTo(w, h * 0.22f)
                }
                val lower = Path().apply {
                    moveTo(0f, h * 0.80f); lineTo(w * 0.5f, h * 0.86f); lineTo(w, h * 0.74f)
                }
                val wave = Path().apply {
                    moveTo(0f, h * 0.60f)
                    cubicTo(w * 0.28f, h * 0.88f, w * 0.55f, h * 0.18f, w, h * 0.35f)
                }
                drawPath(upper, color = LgColors.Gold.copy(alpha = 0.75f), style = Stroke(1.6f))
                drawPath(lower, color = LgColors.Gold.copy(alpha = 0.75f), style = Stroke(1.6f))
                drawPath(wave, color = LgColors.Turquoise, style = Stroke(2.6f, cap = StrokeCap.Round))
            }
            2 -> {
                // MACD / Volume Histogram Bars + Signal Line
                val bars = listOf(-0.35f, -0.2f, 0.15f, 0.45f, 0.72f, 0.58f, 0.82f)
                val step = w / bars.size
                bars.forEachIndexed { i, v ->
                    val cx = (i + 0.5f) * step
                    val topY = if (v >= 0f) h * (0.5f - v * 0.42f) else h * 0.5f
                    val barH = abs(v) * h * 0.42f
                    drawRect(
                        color = if (v >= 0f) LgColors.Turquoise else LgColors.Crimson,
                        topLeft = Offset(cx - step * 0.28f, topY),
                        size = Size(step * 0.56f, barH.coerceAtLeast(2f))
                    )
                }
                val sig = Path().apply {
                    moveTo(0f, h * 0.68f); lineTo(w * 0.5f, h * 0.42f); lineTo(w, h * 0.20f)
                }
                drawPath(sig, color = LgColors.GoldBright, style = Stroke(2.2f, cap = StrokeCap.Round))
            }
            else -> {
                // Breakout / Grid Rungs + Surge Line
                listOf(0.24f, 0.48f, 0.74f).forEach { frac ->
                    drawLine(LgColors.Gold.copy(alpha = 0.45f), Offset(0f, h * frac), Offset(w, h * frac), 1.3f)
                }
                val surge = Path().apply {
                    moveTo(0f, h * 0.74f)
                    lineTo(w * 0.28f, h * 0.56f)
                    lineTo(w * 0.50f, h * 0.64f)
                    lineTo(w * 0.74f, h * 0.30f)
                    lineTo(w, h * 0.14f)
                }
                drawPath(surge, color = LgColors.Turquoise, style = Stroke(2.8f, cap = StrokeCap.Round))
                drawCircle(LgColors.GoldBright, radius = 3.6f, center = Offset(w, h * 0.14f))
            }
        }
    }
}

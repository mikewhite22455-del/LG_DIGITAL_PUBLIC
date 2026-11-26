package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.Formatters
import com.example.domain.engine.TechnicalEngine
import com.example.domain.models.Candle
import com.example.domain.models.ChartOverlay
import com.example.domain.models.HistoryWindow
import com.example.domain.models.MarketTicker
import com.example.domain.models.PriceHistoryPoint
import com.example.domain.models.SignalAction
import com.example.domain.models.StrategyType
import com.example.domain.models.UiState
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    overlay: ChartOverlay,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LgColors.ObsidianBg.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text("No OHLC candles loaded yet.", color = LgColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val visible = remember(candles) { candles.takeLast(64) }
    val closes = remember(visible) { visible.map { it.close } }
    val sma9 = remember(closes) { TechnicalEngine.smaSeries(closes, 9) }
    val sma21 = remember(closes) { TechnicalEngine.smaSeries(closes, 21) }
    val ema12 = remember(closes) { TechnicalEngine.emaSeries(closes, 12) }
    val ema26 = remember(closes) { TechnicalEngine.emaSeries(closes, 26) }
    val bb = remember(closes) { TechnicalEngine.bollingerSeries(closes, 20, 2.0) }

    val minLow = remember(visible) { visible.minOf { it.low } }
    val maxHigh = remember(visible) { visible.maxOf { it.high } }
    val range = (maxHigh - minLow).coerceAtLeast(1e-6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.85f))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("High ${Formatters.formatUsd(maxHigh)}", style = MaterialTheme.typography.labelSmall, color = LgColors.Turquoise)
            Text("Last ${Formatters.formatUsd(visible.last().close)}", style = MaterialTheme.typography.labelSmall, color = LgColors.GoldBright)
            Text("Low ${Formatters.formatUsd(minLow)}", style = MaterialTheme.typography.labelSmall, color = LgColors.Crimson)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .testTag("ohlc_candlestick_canvas")
        ) {
            val w = size.width
            val h = size.height
            val padY = 8f
            val chartH = (h - padY * 2f).coerceAtLeast(1f)
            val stepX = w / visible.size.coerceAtLeast(1)
            val bodyW = (stepX * 0.62f).coerceIn(2f, 14f)

            fun yFor(price: Double): Float {
                val frac = ((price - minLow) / range).toFloat().coerceIn(0f, 1f)
                return h - padY - frac * chartH
            }

            // Grid lines
            for (g in 1..3) {
                val gy = padY + (chartH / 4f) * g
                drawLine(
                    color = LgColors.BorderHairline.copy(alpha = 0.45f),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1f
                )
            }

            // Candles
            visible.forEachIndexed { idx, c ->
                val cx = idx * stepX + stepX / 2f
                val bull = c.close >= c.open
                val color = if (bull) LgColors.Turquoise else LgColors.Crimson
                drawLine(
                    color = color,
                    start = Offset(cx, yFor(c.high)),
                    end = Offset(cx, yFor(c.low)),
                    strokeWidth = 1.6f
                )
                val top = yFor(maxOf(c.open, c.close))
                val bottom = yFor(minOf(c.open, c.close))
                val bodyH = (bottom - top).coerceAtLeast(2f)
                drawRect(
                    color = color,
                    topLeft = Offset(cx - bodyW / 2f, top),
                    size = Size(bodyW, bodyH)
                )
            }

            fun drawLineSeries(series: List<Double?>, color: Color, strokeWidth: Float = 2f) {
                val path = Path()
                var started = false
                series.forEachIndexed { idx, value ->
                    if (value != null && value.isFinite()) {
                        val x = idx * stepX + stepX / 2f
                        val y = yFor(value)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }
                if (started) {
                    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                }
            }

            when (overlay) {
                ChartOverlay.SMA_CROSS -> {
                    drawLineSeries(sma9, LgColors.GoldBright, 2.2f)
                    drawLineSeries(sma21, LgColors.CyanGlow, 2.0f)
                }
                ChartOverlay.EMA_CROSS -> {
                    drawLineSeries(ema12, LgColors.GoldBright, 2.2f)
                    drawLineSeries(ema26, LgColors.AmberWarn, 2.0f)
                }
                ChartOverlay.BOLLINGER -> {
                    drawLineSeries(bb.map { it?.first }, LgColors.CyanGlow.copy(alpha = 0.85f), 1.6f)
                    drawLineSeries(bb.map { it?.second }, LgColors.GoldBright, 1.8f)
                    drawLineSeries(bb.map { it?.third }, LgColors.CyanGlow.copy(alpha = 0.85f), 1.6f)
                }
                ChartOverlay.SUPERTREND -> {
                    drawLineSeries(ema12, LgColors.Turquoise, 2.2f)
                    drawLineSeries(sma21, LgColors.Gold, 1.8f)
                }
                ChartOverlay.NONE -> Unit
            }
        }
    }
}

@Composable
fun EquitySparklineChart(
    curve: List<Double>,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    if (curve.size < 2) return
    val strokeColor = if (positive) LgColors.Turquoise else LgColors.Crimson
    val minV = remember(curve) { curve.min() }
    val maxV = remember(curve) { curve.max() }
    val span = (maxV - minV).coerceAtLeast(1e-6)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        val w = size.width
        val h = size.height
        val linePath = Path()
        val fillPath = Path()
        curve.forEachIndexed { idx, v ->
            val x = (idx.toFloat() / (curve.lastIndex).coerceAtLeast(1)) * w
            val y = h - (((v - minV) / span).toFloat().coerceIn(0f, 1f) * h)
            if (idx == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(strokeColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HourlyTrendSparkline(
    prices: List<Double>,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (positive) LgColors.Turquoise else LgColors.Crimson
    Canvas(modifier = modifier) {
        if (prices.size < 2) {
            drawLine(
                color = strokeColor.copy(alpha = 0.5f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2f
            )
            return@Canvas
        }
        val minP = prices.min()
        val maxP = prices.max()
        val span = (maxP - minP).coerceAtLeast(1e-6)
        val path = Path()
        prices.forEachIndexed { idx, p ->
            val x = (idx.toFloat() / prices.lastIndex.coerceAtLeast(1)) * size.width
            val y = size.height - (((p - minP) / span).toFloat().coerceIn(0f, 1f) * size.height)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = strokeColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
    }
}

@Composable
fun AssetHistoricalView(
    symbol: String,
    ticker: MarketTicker?,
    selectedWindow: HistoryWindow,
    historyState: UiState<List<PriceHistoryPoint>>,
    quickSymbols: List<String>,
    onSelectSymbol: (String) -> Unit,
    onSelectWindow: (HistoryWindow) -> Unit,
    onRetry: () -> Unit,
    showAssetSelector: Boolean = true,
    onOpenInTrade: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(
        title = "${symbol.uppercase()} HISTORICAL PERFORMANCE (${selectedWindow.label})",
        modifier = modifier
    ) {
        if (showAssetSelector && quickSymbols.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickSymbols, key = { it }) { sym ->
                    FilterChip(
                        selected = sym.equals(symbol, ignoreCase = true),
                        onClick = { onSelectSymbol(sym) },
                        label = { Text(sym, style = TabLabelTextStyle) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.GoldBright
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HistoryWindow.entries.forEach { win ->
                    FilterChip(
                        selected = win == selectedWindow,
                        onClick = { onSelectWindow(win) },
                        label = { Text(win.label, style = TabLabelTextStyle) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                            selectedLabelColor = LgColors.GoldBright
                        ),
                        modifier = Modifier.testTag("history_window_${win.label.lowercase()}")
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ticker?.let {
                    Text(
                        text = Formatters.formatUsd(it.priceUsd),
                        style = MaterialTheme.typography.titleMedium,
                        color = LgColors.GoldBright,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh history", tint = LgColors.Turquoise)
                }
            }
        }

        when (historyState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LgColors.Turquoise)
                }
            }
            is UiState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(historyState.message, color = LgColors.Crimson, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = onRetry) { Text("Retry Feed") }
                }
            }
            is UiState.Success -> {
                val points = historyState.data
                if (points.size >= 2) {
                    val prices = points.map { it.priceUsd }
                    val deltaPct = ((prices.last() - prices.first()) / prices.first().coerceAtLeast(1e-9)) * 100.0
                    val up = deltaPct >= 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Period High: ${Formatters.formatUsd(prices.max())}", style = MaterialTheme.typography.labelSmall, color = LgColors.Turquoise)
                        StatusPill(text = Formatters.formatPct(deltaPct), color = if (up) LgColors.Turquoise else LgColors.Crimson)
                        Text("Low: ${Formatters.formatUsd(prices.min())}", style = MaterialTheme.typography.labelSmall, color = LgColors.InkSecondary)
                    }
                    EquitySparklineChart(
                        curve = prices,
                        positive = up,
                        modifier = Modifier.height(128.dp)
                    )
                } else {
                    Text("Waiting for historical points...", color = LgColors.InkMuted, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (onOpenInTrade != null) {
            OutlinedButton(
                onClick = { onOpenInTrade(symbol) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.ShowChart, contentDescription = null, tint = LgColors.Turquoise, modifier = Modifier.padding(end = 6.dp))
                Text("Analyze $symbol in Trade & Backtest Desk", color = LgColors.GoldBright)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewLiveChartWebView(
    symbol: String,
    interval: String = "60",
    height: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val tvSymbol = remember(symbol) { "KRAKEN:${symbol.uppercase().replace("/", "")}USD" }
    val html = remember(tvSymbol, interval) {
        """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>html,body,#tv{margin:0;padding:0;width:100%;height:100%;background:#040F14;overflow:hidden;}</style>
        </head>
        <body>
          <div id="tv"></div>
          <script src="https://s3.tradingview.com/tv.js"></script>
          <script>
            if (window.TradingView) {
              new TradingView.widget({
                autosize: true,
                symbol: "$tvSymbol",
                interval: "$interval",
                timezone: "Etc/UTC",
                theme: "dark",
                style: "1",
                locale: "en",
                toolbar_bg: "#091B24",
                enable_publishing: false,
                hide_side_toolbar: true,
                allow_symbol_change: false,
                container_id: "tv"
              });
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                setBackgroundColor(android.graphics.Color.parseColor("#040F14"))
                loadDataWithBaseURL("https://s3.tradingview.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://s3.tradingview.com", html, "text/html", "UTF-8", null)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(12.dp))
    )
}

@Composable
fun TradingViewFullChartDialog(
    symbol: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = LgColors.ObsidianBg,
            modifier = Modifier.fillMaxSize()
        ) {
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
                    Text(
                        text = "${symbol.uppercase()}/USD · FULL LIVE CHART",
                        style = MaterialTheme.typography.titleMedium,
                        color = LgColors.GoldBright
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close full chart", tint = LgColors.Turquoise)
                    }
                }
                TradingViewLiveChartWebView(
                    symbol = symbol,
                    interval = "60",
                    height = 560.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StrategiesHeroChartButton(
    activeStrategyTitle: String,
    isActiveTab: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        LgColors.ElevatedSlate,
                        LgColors.TealAmbient.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = if (isActiveTab) 1.6.dp else 1.dp,
                color = if (isActiveTab) LgColors.GoldBright else LgColors.Turquoise.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("strategies_hero_chart_button"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = LgColors.GoldBright, modifier = Modifier.size(22.dp))
            Column {
                Text(
                    text = "ACTIVE QUANT STRATEGY ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.Turquoise
                )
                Text(
                    text = activeStrategyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = LgColors.InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        StatusPill(text = if (isActiveTab) "VIEWING" else "11 MODELS", color = LgColors.GoldBright)
    }
}

@Composable
fun StrategyChartButtonCard(
    strategy: StrategyType,
    selected: Boolean,
    liveAction: SignalAction?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanelCard(
        highlightBorder = if (selected) LgColors.GoldBright else LgColors.BorderHairline,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("strategy_card_${strategy.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strategy.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) LgColors.GoldBright else LgColors.InkPrimary
                )
                Text(
                    text = strategy.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.InkSecondary
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            when {
                liveAction != null -> SignalBadge(action = liveAction)
                selected -> StatusPill(text = "ACTIVE", color = LgColors.Turquoise)
                else -> StatusPill(text = "SELECT", color = LgColors.InkMuted)
            }
        }
    }
}

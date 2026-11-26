package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.models.FearGreedIndexData
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.UiState
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.ConfiguredExchangePriceRow

@Composable
fun LiveAutoFeedBadge(
    label: String = "LIVE FEED",
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_feed_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(LgColors.Turquoise.copy(alpha = 0.14f))
            .border(1.dp, LgColors.Turquoise.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(LgColors.Turquoise.copy(alpha = pulseAlpha))
        )
        Text(text = label, style = TabLabelTextStyle, color = LgColors.Turquoise)
    }
}

@Composable
fun FearGreedGaugeWidget(
    fearGreedState: UiState<FearGreedIndexData>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(title = "CRYPTO FEAR & GREED INDEX", modifier = modifier) {
        when (fearGreedState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LgColors.Turquoise, modifier = Modifier.size(28.dp))
                }
            }
            is UiState.Error -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fearGreedState.message, color = LgColors.Crimson, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry Fear and Greed", tint = LgColors.Turquoise)
                    }
                }
            }
            is UiState.Success -> {
                val fg = fearGreedState.data
                val gaugeColor = when {
                    fg.liveValue >= 65 -> LgColors.Turquoise
                    fg.liveValue >= 45 -> LgColors.GoldBright
                    else -> LgColors.Crimson
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(92.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 10.dp.toPx()
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            val topLeft = Offset(stroke / 2f, stroke / 2f)
                            drawArc(
                                color = LgColors.ElevatedSlate,
                                startAngle = 150f,
                                sweepAngle = 240f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = gaugeColor,
                                startAngle = 150f,
                                sweepAngle = (fg.liveValue.coerceIn(0, 100) / 100f) * 240f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${fg.liveValue}", style = MaterialTheme.typography.headlineSmall, color = gaugeColor, fontWeight = FontWeight.Bold)
                            Text("/ 100", style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fg.liveClassification.uppercase(), style = MaterialTheme.typography.titleMedium, color = gaugeColor)
                            IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh sentiment", tint = LgColors.Turquoise)
                            }
                        }
                        Text(
                            text = "API Index ${fg.apiValue} (${fg.apiClassification}) · Live Pulse ${if (fg.liveMomentumAdjustment >= 0) "+" else ""}${fg.liveMomentumAdjustment}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LgColors.InkSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fg.yesterdayValue?.let { StatusPill("1D: $it", LgColors.CyanGlow) }
                            fg.lastWeekValue?.let { StatusPill("7D: $it", LgColors.Gold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveTelemetryCommandCard(
    selectedSymbol: String,
    breadth: MarketBreadth?,
    tickers: List<MarketTicker>,
    arbitrageState: ArbitrageUiState,
    onSelectTicker: (MarketTicker) -> Unit,
    onSelectSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(title = "LIVE TELEMETRY & RADAR COMMAND", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveAutoFeedBadge(label = "15S AUTO STREAM")
            StatusPill(
                text = "ARB VENUES: ${arbitrageState.activeVenuesCount}",
                color = LgColors.GoldBright
            )
        }
        if (breadth != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Advancing", "${breadth.advancing}/${breadth.totalAssets}", LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Avg 24h", Formatters.formatPct(breadth.avgChangePct), if (breadth.avgChangePct >= 0) LgColors.Turquoise else LgColors.Crimson, Modifier.weight(1f))
                MetricTile("24h Vol", Formatters.formatCompactUsd(breadth.totalVolume24hUsd), LgColors.GoldBright, Modifier.weight(1f))
            }
        }
        if (tickers.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tickers.take(8), key = { it.symbol }) { t ->
                    val selected = t.symbol.equals(selectedSymbol, ignoreCase = true)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) LgColors.TealAmbient else LgColors.ElevatedSlate.copy(alpha = 0.7f))
                            .border(1.dp, if (selected) LgColors.GoldBright else LgColors.BorderHairline, RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectSymbol(t.symbol)
                                onSelectTicker(t)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(t.symbol, style = MaterialTheme.typography.labelLarge, color = LgColors.GoldBright)
                        Text(Formatters.formatUsd(t.priceUsd), style = MaterialTheme.typography.labelMedium, color = LgColors.InkPrimary)
                        val isUp = t.change24hPct >= 0.0
                        val trendColor = if (isUp) LgColors.Turquoise else LgColors.Crimson
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = if (isUp) "Price up in 24h" else "Price down in 24h",
                                tint = trendColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                Formatters.formatPct(t.change24hPct),
                                style = MaterialTheme.typography.labelSmall,
                                color = trendColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceSummaryDashboardCard(
    rows: List<ConfiguredExchangePriceRow>,
    configuredExchangeIds: List<String>,
    selectedExchangeFilter: String,
    isRefreshing: Boolean,
    lastUpdatedMs: Long,
    errorMessage: String?,
    onSelectExchangeFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenExchangeConfig: () -> Unit,
    onSelectPair: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("ALL", "kraken", "coinbase", "kucoin", "bitstamp")
    val filtered = if (selectedExchangeFilter == "ALL") {
        rows
    } else {
        rows.filter { it.exchangeId.equals(selectedExchangeFilter, ignoreCase = true) }
    }

    CyberFrameCard(title = "EXCHANGE PRICE SUMMARY FEED", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (configuredExchangeIds.isEmpty()) "Public spot feeds (connect keys for live orders)" else "Configured: ${configuredExchangeIds.joinToString(", ") { it.uppercase() }}",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.InkSecondary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh exchange prices", tint = LgColors.Turquoise)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filters) { f ->
                FilterChip(
                    selected = selectedExchangeFilter.equals(f, ignoreCase = true),
                    onClick = { onSelectExchangeFilter(f) },
                    label = { Text(f.uppercase(), style = TabLabelTextStyle) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                        selectedLabelColor = LgColors.GoldBright
                    )
                )
            }
        }

        if (errorMessage != null) {
            Text(errorMessage, color = LgColors.Crimson, style = MaterialTheme.typography.bodySmall)
        }

        if (filtered.isEmpty() && isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LgColors.Turquoise, modifier = Modifier.size(24.dp))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PAIR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LgColors.InkMuted,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "PRICE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LgColors.InkMuted,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = "24H",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LgColors.InkMuted
                    )
                }
                filtered.take(6).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LgColors.ElevatedSlate.copy(alpha = 0.65f))
                            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(10.dp))
                            .clickable { onSelectPair(row.symbol) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            CoinAvatar(row.symbol, size = 26.dp)
                            Column {
                                Text(row.pairLabel, style = MaterialTheme.typography.titleSmall, color = LgColors.InkPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(row.exchangeName, style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(
                            text = Formatters.formatUsd(row.priceUsd),
                            style = MaterialTheme.typography.labelLarge,
                            color = LgColors.GoldBright,
                            maxLines = 1,
                            modifier = Modifier.weight(0.9f)
                        )
                        TrendChangeBadge(
                            changePct = row.change24hPct,
                            compact = true,
                            modifier = Modifier.testTag("summary_24h_trend_${row.exchangeId}_${row.symbol}")
                        )
                    }
                }
            }
        }

        if (lastUpdatedMs > 0L) {
            Text(
                text = "Synced ${Formatters.formatTimeShort(lastUpdatedMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.InkMuted
            )
        }

        if (configuredExchangeIds.isEmpty()) {
            OutlinedButton(
                onClick = onOpenExchangeConfig,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, tint = LgColors.GoldBright, modifier = Modifier.padding(end = 6.dp))
                Text("Connect Exchange API Keys", color = LgColors.GoldBright)
            }
        }
    }
}

@Composable
fun PriceSummaryDashboardScreen(
    rows: List<ConfiguredExchangePriceRow>,
    configuredExchangeIds: List<String>,
    selectedExchangeFilter: String,
    isRefreshing: Boolean,
    lastUpdatedMs: Long,
    errorMessage: String?,
    onSelectExchangeFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenExchangeConfig: () -> Unit,
    onSelectPair: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Surface(color = LgColors.PanelSlate.copy(alpha = 0.96f)) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LgColors.Turquoise)
                            }
                            Text(
                                text = "EXCHANGE PRICE SUMMARY",
                                style = MaterialTheme.typography.titleMedium,
                                color = LgColors.GoldBright,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = LgColors.Turquoise)
                        }
                    }
                    HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.45f))
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PriceSummaryDashboardCard(
                        rows = rows,
                        configuredExchangeIds = configuredExchangeIds,
                        selectedExchangeFilter = selectedExchangeFilter,
                        isRefreshing = isRefreshing,
                        lastUpdatedMs = lastUpdatedMs,
                        errorMessage = errorMessage,
                        onSelectExchangeFilter = onSelectExchangeFilter,
                        onRefresh = onRefresh,
                        onOpenExchangeConfig = onOpenExchangeConfig,
                        onSelectPair = onSelectPair
                    )
                }
            }
        }
    }
}

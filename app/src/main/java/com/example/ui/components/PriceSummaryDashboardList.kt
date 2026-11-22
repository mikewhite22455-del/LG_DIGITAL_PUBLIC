package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.Formatters
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.ConfiguredExchangePriceRow
import kotlinx.coroutines.delay

/**
 * Simple Material 3 list component displaying real-time market data for the user's primary pairs
 * (BTC, ETH, SOL, XRP, ADA, DOGE) from their configured exchanges (Kraken Pro, Coinbase, KuCoin, Bitstamp).
 */
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
    onSelectPair: (symbol: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableExchanges = remember(rows) {
        rows.map { it.exchangeId to it.exchangeName }.distinctBy { it.first }
    }
    val filteredRows = remember(rows, selectedExchangeFilter) {
        if (selectedExchangeFilter == "ALL") rows
        else rows.filter { it.exchangeId.equals(selectedExchangeFilter, ignoreCase = true) }
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    CyberFrameCard(
        title = "PRICE SUMMARY · CONFIGURED EXCHANGES",
        modifier = modifier.testTag("price_summary_dashboard_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (configuredExchangeIds.isNotEmpty()) {
                        "Live primary pairs from ${configuredExchangeIds.size} configured venue(s)"
                    } else {
                        "Live primary pairs (Kraken · Coinbase · KuCoin public feeds)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LgColors.Turquoise,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatPriceSyncAge(lastUpdatedMs, nowMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenExchangeConfig,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("price_summary_config_keys_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Configure exchange API keys",
                        tint = LgColors.Gold
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("price_summary_refresh_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = LgColors.Turquoise,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh live prices",
                            tint = LgColors.Turquoise
                        )
                    }
                }
            }
        }

        // Exchange Filter Row
        if (availableExchanges.size > 1) {
            ExchangeFilterChipRow(
                availableExchanges = availableExchanges,
                selectedFilter = selectedExchangeFilter,
                onSelectFilter = onSelectExchangeFilter
            )
        }

        when {
            filteredRows.isEmpty() && isRefreshing -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = LgColors.Turquoise,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "Loading real-time exchange tickers...",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                }
            }
            filteredRows.isEmpty() && !errorMessage.isNullOrBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.Crimson
                    )
                    TextButton(
                        onClick = onRefresh,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Retry Live Feed", color = LgColors.Turquoise, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LgColors.ObsidianBg.copy(alpha = 0.72f))
                        .border(1.dp, LgColors.Gold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .testTag("price_summary_simple_list")
                ) {
                    filteredRows.forEachIndexed { idx, row ->
                        PriceSummaryListItemRow(
                            row = row,
                            onClick = { onSelectPair(row.symbol) }
                        )
                        if (idx < filteredRows.lastIndex) {
                            HorizontalDivider(
                                color = LgColors.BorderHairline.copy(alpha = 0.55f),
                                thickness = 0.75.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-screen simple LazyColumn Price Summary Dashboard accessible directly from the Live Portal
 * top bar or Native Quant Tools.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onSelectPair: (symbol: String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val availableExchanges = remember(rows) {
        rows.map { it.exchangeId to it.exchangeName }.distinctBy { it.first }
    }
    val filteredRows = remember(rows, selectedExchangeFilter) {
        if (selectedExchangeFilter == "ALL") rows
        else rows.filter { it.exchangeId.equals(selectedExchangeFilter, ignoreCase = true) }
    }

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Top Brand Header
            Surface(
                color = LgColors.PanelSlate.copy(alpha = 0.96f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_user_lg_coin),
                                contentDescription = "LG Digital price summary emblem",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, LgColors.Gold, CircleShape)
                            )
                            Column {
                                Text(
                                    text = "LG DIGITAL",
                                    fontFamily = LgBrandFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.8.sp,
                                    color = LgColors.GoldBright,
                                    maxLines = 1
                                )
                                Text(
                                    text = "PRICE SUMMARY · ${formatPriceSyncAge(lastUpdatedMs, nowMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.Turquoise,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onOpenExchangeConfig,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("price_summary_screen_config_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = "Exchange API configuration",
                                    tint = LgColors.Gold
                                )
                            }
                            IconButton(
                                onClick = onRefresh,
                                enabled = !isRefreshing,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("price_summary_screen_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh live prices",
                                    tint = LgColors.Turquoise
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("price_summary_screen_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close price summary",
                                    tint = LgColors.GoldBright
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.5f), thickness = 1.dp)
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                            .testTag("price_summary_full_lazy_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            GlassPanelCard(highlightBorder = LgColors.Turquoise.copy(alpha = 0.65f)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (configuredExchangeIds.isNotEmpty()) {
                                                "CONFIGURED VENUES (${configuredExchangeIds.size})"
                                            } else {
                                                "LIVE EXCHANGE FEEDS"
                                            },
                                            style = TabLabelTextStyle,
                                            color = LgColors.GoldBright
                                        )
                                        StatusPill(
                                            text = if (configuredExchangeIds.isNotEmpty()) "API LINKED" else "PUBLIC LIVE",
                                            color = if (configuredExchangeIds.isNotEmpty()) LgColors.Turquoise else LgColors.Gold
                                        )
                                    }
                                    Text(
                                        text = if (configuredExchangeIds.isNotEmpty()) {
                                            "Streaming live primary pairs (BTC, ETH, SOL, XRP, ADA, DOGE) from your configured exchanges every 15 seconds."
                                        } else {
                                            "Streaming live primary pairs from Kraken Pro, Coinbase, and KuCoin. Configure exchange API keys to scope this list to your linked accounts."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LgColors.InkSecondary
                                    )
                                    if (availableExchanges.size > 1) {
                                        ExchangeFilterChipRow(
                                            availableExchanges = availableExchanges,
                                            selectedFilter = selectedExchangeFilter,
                                            onSelectFilter = onSelectExchangeFilter
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredRows.isEmpty() && isRefreshing) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = LgColors.Turquoise)
                                }
                            }
                        } else if (filteredRows.isEmpty() && !errorMessage.isNullOrBlank()) {
                            item {
                                GlassPanelCard(highlightBorder = LgColors.Crimson.copy(alpha = 0.7f)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = errorMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = LgColors.Crimson
                                        )
                                        GoldMetallicButton(
                                            text = "RETRY LIVE PRICES",
                                            onClick = onRefresh,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = filteredRows,
                                key = { _, item -> item.id }
                            ) { _, row ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LgColors.PanelSlate.copy(alpha = 0.88f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (row.isApiConfigured) LgColors.Turquoise.copy(alpha = 0.55f) else LgColors.Gold.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    PriceSummaryListItemRow(
                                        row = row,
                                        onClick = { onSelectPair(row.symbol) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeFilterChipRow(
    availableExchanges: List<Pair<String, String>>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf("ALL" to "All Venues") + availableExchanges
        options.forEach { (id, label) ->
            val selected = selectedFilter.equals(id, ignoreCase = true)
            FilterChip(
                selected = selected,
                onClick = { onSelectFilter(id) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LgColors.Turquoise,
                    selectedLabelColor = LgColors.ObsidianBg,
                    containerColor = LgColors.ObsidianBg.copy(alpha = 0.75f),
                    labelColor = LgColors.InkPrimary
                ),
                modifier = Modifier.testTag("price_summary_filter_${id.lowercase()}")
            )
        }
    }
}

@Composable
private fun PriceSummaryListItemRow(
    row: ConfiguredExchangePriceRow,
    onClick: () -> Unit
) {
    val isPositive = row.change24hPct >= 0.0
    val deltaColor = if (isPositive) LgColors.Turquoise else LgColors.Crimson

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .testTag("price_summary_row_${row.id}"),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            CoinAvatar(symbol = row.symbol, size = 36.dp)
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = row.pairLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = LgColors.InkPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                StatusPill(
                    text = row.exchangeName.uppercase(),
                    color = if (row.isApiConfigured) LgColors.Turquoise else LgColors.Gold
                )
            }
        },
        supportingContent = {
            Text(
                text = "H ${Formatters.formatUsd(row.high24h)} · L ${Formatters.formatUsd(row.low24h)} · Vol ${Formatters.formatCompactUsd(row.volume24hUsd)}",
                style = MaterialTheme.typography.labelSmall,
                color = LgColors.InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatUsd(row.priceUsd),
                    style = MaterialTheme.typography.titleSmall,
                    color = LgColors.GoldBright,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = deltaColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = Formatters.formatPct(row.change24hPct),
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

private fun formatPriceSyncAge(lastUpdatedMs: Long, nowMs: Long): String {
    if (lastUpdatedMs <= 0L) return "Syncing live feeds..."
    val diffSec = ((nowMs - lastUpdatedMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 3L -> "Live · updated just now"
        diffSec < 60L -> "Live · updated ${diffSec}s ago"
        else -> "Live · updated ${diffSec / 60L}m ago"
    }
}

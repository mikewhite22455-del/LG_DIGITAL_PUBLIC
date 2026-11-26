package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.Formatters
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.HistoryWindow
import com.example.domain.models.MarketBreadth
import com.example.domain.models.MarketTicker
import com.example.domain.models.UiState
import com.example.ui.components.AssetHistoricalView
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.FearGreedGaugeWidget
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.HourlyTrendSparkline
import com.example.ui.components.LiveTelemetryCommandCard
import com.example.ui.components.MetricTile
import com.example.ui.components.PortfolioSummaryWidget
import com.example.ui.components.PriceSummaryDashboardCard
import com.example.ui.components.SocialMediaLinksBar
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

private enum class FeedFilter(val label: String) {
    ALL("All"),
    GAINERS("Gainers"),
    LOSERS("Losers"),
    HOLDINGS("Held")
}

private enum class DashboardStreamMode(val label: String) {
    PRICES("Price Feeds"),
    NEWS("News Wire")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    marketsState: MarketsUiState,
    arbitrageState: ArbitrageUiState,
    vaultState: VaultUiState,
    tradeState: TradeUiState? = null,
    isWide: Boolean = false,
    onRefreshAll: () -> Unit,
    onRefreshFearGreed: () -> Unit = onRefreshAll,
    onRefreshNews: () -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleArbMode: (ArbitrageMode) -> Unit,
    onScanArbitrage: () -> Unit,
    onExecuteOpportunity: (ArbitrageOpportunity) -> Unit,
    onOpenArbitrageDesk: () -> Unit,
    onSelectCoinForTrade: (String) -> Unit,
    onSelectCoinForArbitrage: (String) -> Unit,
    onAskCopilotForCoin: (MarketTicker) -> Unit,
    onNavigateToAdvisor: () -> Unit,
    onNavigateToMining: () -> Unit,
    onNavigateToTrade: () -> Unit,
    onOpenAllPagesPreview: () -> Unit,
    onOpenWalkthrough: () -> Unit = onOpenAllPagesPreview,
    onSelectHistoryAsset: (String) -> Unit = {},
    onSelectHistoryWindow: (HistoryWindow) -> Unit = {},
    onRefreshHistory: () -> Unit = {},
    onOpenBacktest: (String) -> Unit = {},
    onRefreshPriceSummary: () -> Unit = onRefreshAll,
    onSelectPriceSummaryFilter: (String) -> Unit = {},
    onOpenExchangeConfig: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var selectedCoinSheet by remember { mutableStateOf<MarketTicker?>(null) }
    var activeFilter by remember { mutableStateOf(FeedFilter.ALL) }
    var streamMode by remember { mutableStateOf(DashboardStreamMode.PRICES) }

    selectedCoinSheet?.let { coin ->
        val up = coin.change24hPct >= 0
        ModalBottomSheet(
            onDismissRequest = { selectedCoinSheet = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoinAvatar(coin.symbol, size = 44.dp)
                        Column {
                            Text("${coin.name} (${coin.symbol})", style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Feed: ${coin.source} · Spot USD",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Formatters.formatUsd(coin.priceUsd), style = MaterialTheme.typography.titleLarge)
                        StatusPill(
                            text = Formatters.formatPct(coin.change24hPct),
                            color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("24h High", Formatters.formatUsd(coin.high24h), modifier = Modifier.weight(1f))
                    MetricTile("24h Low", Formatters.formatUsd(coin.low24h), modifier = Modifier.weight(1f))
                    MetricTile("24h Vol", Formatters.formatCompactUsd(coin.volume24hUsd), modifier = Modifier.weight(1f))
                }

                AssetHistoricalView(
                    symbol = coin.symbol,
                    ticker = coin,
                    selectedWindow = marketsState.historyWindow,
                    historyState = marketsState.historyState,
                    quickSymbols = emptyList(),
                    onSelectSymbol = {},
                    onSelectWindow = onSelectHistoryWindow,
                    onRetry = onRefreshHistory,
                    showAssetSelector = false
                )

                Button(
                    onClick = {
                        selectedCoinSheet = null
                        onSelectCoinForTrade(coin.symbol)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.CandlestickChart, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Open Chart & Paper Desk", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            selectedCoinSheet = null
                            onSelectCoinForArbitrage(coin.symbol)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Arb Scan")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedCoinSheet = null
                            onAskCopilotForCoin(coin)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 6.dp))
                        Text("Ask AI", color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = marketsState.isRefreshing || arbitrageState.isScanning,
        onRefresh = onRefreshAll,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Top Summary Widget: Total Portfolio Value & Daily P&L
            item {
                val loadedTickers = (marketsState.tickersState as? UiState.Success)?.data.orEmpty()
                PortfolioSummaryWidget(
                    vaultState = vaultState,
                    tickers = loadedTickers,
                    isRefreshing = marketsState.isRefreshing,
                    isWide = isWide,
                    onRefresh = onRefreshAll,
                    onOpenTrade = onNavigateToTrade,
                    onSelectHolding = { sym ->
                        onSelectHistoryAsset(sym)
                        loadedTickers.firstOrNull { it.symbol == sym }?.let { ticker ->
                            selectedCoinSheet = ticker
                        } ?: onSelectCoinForTrade(sym)
                    },
                    tradeState = tradeState,
                    onOpenExchangeConfig = onOpenExchangeConfig
                )
            }

            // 0. Real-Time Primary Pairs Price Summary List (from Configured Exchanges)
            item {
                PriceSummaryDashboardCard(
                    rows = marketsState.priceSummaryRows,
                    configuredExchangeIds = marketsState.configuredSummaryExchanges,
                    selectedExchangeFilter = marketsState.priceSummaryExchangeFilter,
                    isRefreshing = marketsState.isRefreshingPriceSummary,
                    lastUpdatedMs = marketsState.priceSummaryUpdatedMs,
                    errorMessage = marketsState.priceSummaryError,
                    onSelectExchangeFilter = onSelectPriceSummaryFilter,
                    onRefresh = onRefreshPriceSummary,
                    onOpenExchangeConfig = onOpenExchangeConfig,
                    onSelectPair = { sym -> onSelectCoinForTrade(sym) }
                )
            }

            // 0a. Live-Updating Crypto Fear & Greed Index Gauge (External API + Live Pulse)
            item {
                FearGreedGaugeWidget(
                    fearGreedState = marketsState.fearGreedState,
                    isRefreshing = marketsState.isRefreshingFearGreed,
                    onRefresh = onRefreshFearGreed
                )
            }

            // 0b. Auto-Running Live Meters, Animated Radar Scanner & Live TradingView Chart
            item {
                val loadedTickers = (marketsState.tickersState as? UiState.Success)?.data.orEmpty()
                LiveTelemetryCommandCard(
                    selectedSymbol = marketsState.selectedHistorySymbol,
                    breadth = marketsState.breadth,
                    tickers = loadedTickers,
                    arbitrageState = arbitrageState,
                    onSelectTicker = { ticker ->
                        onSelectHistoryAsset(ticker.symbol)
                        selectedCoinSheet = ticker
                    },
                    onSelectSymbol = onSelectHistoryAsset
                )
            }

            // 1. M3 Command Quick-Launch Card
            item {
                PortfolioHeroM3Card(
                    marketsState = marketsState,
                    vaultState = vaultState,
                    onRefreshAll = onRefreshAll,
                    onNavigateToTrade = onNavigateToTrade,
                    onOpenWalkthrough = onOpenWalkthrough,
                    onOpenBacktest = { onOpenBacktest(marketsState.selectedHistorySymbol) }
                )
            }

            // 2. M3 Arbitrage Summary & Market Breadth (side-by-side on wide screens)
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ArbitrageSummaryM3Card(
                            state = arbitrageState,
                            vaultState = vaultState,
                            onToggleMode = onToggleArbMode,
                            onScanNow = onScanArbitrage,
                            onExecuteOpportunity = onExecuteOpportunity,
                            onOpenArbitrageDesk = onOpenArbitrageDesk,
                            modifier = Modifier.weight(1.15f)
                        )
                        MarketPulseM3Card(
                            breadth = marketsState.breadth,
                            tickers = (marketsState.tickersState as? UiState.Success)?.data ?: emptyList(),
                            onSelectCoin = { ticker ->
                                onSelectHistoryAsset(ticker.symbol)
                                selectedCoinSheet = ticker
                            },
                            modifier = Modifier.weight(0.85f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ArbitrageSummaryM3Card(
                            state = arbitrageState,
                            vaultState = vaultState,
                            onToggleMode = onToggleArbMode,
                            onScanNow = onScanArbitrage,
                            onExecuteOpportunity = onExecuteOpportunity,
                            onOpenArbitrageDesk = onOpenArbitrageDesk
                        )
                        MarketPulseM3Card(
                            breadth = marketsState.breadth,
                            tickers = (marketsState.tickersState as? UiState.Success)?.data ?: emptyList(),
                            onSelectCoin = { ticker ->
                                onSelectHistoryAsset(ticker.symbol)
                                selectedCoinSheet = ticker
                            }
                        )
                    }
                }
            }

            // 2b. Detailed Historical Asset Performance Chart (1H / 24H / 7D)
            item {
                val loadedTickers = (marketsState.tickersState as? UiState.Success)?.data.orEmpty()
                val activeTicker = loadedTickers.firstOrNull { it.symbol == marketsState.selectedHistorySymbol }
                val quickSymbols = remember(loadedTickers) {
                    val fromFeed = loadedTickers.take(10).map { it.symbol }
                    (fromFeed + listOf("BTC", "ETH", "SOL", "XRP", "LINK", "AVAX", "ADA")).distinct().take(10)
                }
                AssetHistoricalView(
                    symbol = marketsState.selectedHistorySymbol,
                    ticker = activeTicker,
                    selectedWindow = marketsState.historyWindow,
                    historyState = marketsState.historyState,
                    quickSymbols = quickSymbols,
                    onSelectSymbol = onSelectHistoryAsset,
                    onSelectWindow = onSelectHistoryWindow,
                    onRetry = onRefreshHistory,
                    onOpenInTrade = onSelectCoinForTrade
                )
            }

            // 3. Real-Time Crypto Price Feeds & CoinDesk RSS Wire Section
            item {
                CyberFrameCard(
                    title = if (streamMode == DashboardStreamMode.PRICES) "HOME · PRICE FEEDS" else "LIVE MARKET WIRE",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = if (streamMode == DashboardStreamMode.PRICES) "Spot Market Feed" else "CoinDesk Wire",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LgColors.Turquoise
                                )
                                if (streamMode == DashboardStreamMode.PRICES) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ExchangeLogo("CoinGecko", size = 16.dp)
                                        ExchangeLogo("KuCoin", size = 16.dp)
                                        Text(
                                            text = "CoinGecko & KuCoin USD spot quotes",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "CoinDesk RSS market headlines",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (marketsState.lastUpdatedMs > 0L) {
                                SuggestionChip(
                                    onClick = onRefreshAll,
                                    label = { Text(Formatters.formatTimeShort(marketsState.lastUpdatedMs)) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            }
                        }

                        // Material 3 Segmented Button toggle between Price Feeds and News Wire
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            DashboardStreamMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = streamMode == mode,
                                    onClick = { streamMode = mode },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = DashboardStreamMode.entries.size),
                                    icon = {
                                        Icon(
                                            imageVector = if (mode == DashboardStreamMode.PRICES) Icons.Filled.CandlestickChart else Icons.Filled.Newspaper,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("dashboard_stream_${mode.name.lowercase()}")
                                ) {
                                    Text(mode.label, maxLines = 1)
                                }
                            }
                        }

                        if (streamMode == DashboardStreamMode.PRICES) {
                            com.example.ui.components.CoinDropdownSelector(
                                selectedSymbol = marketsState.searchQuery.trim().uppercase().ifBlank { marketsState.selectedHistorySymbol },
                                symbols = com.example.core.CoinAssets.catalog.map { it.symbol },
                                onSelectSymbol = { chosen ->
                                    onSearchChange(chosen)
                                    onSelectHistoryAsset(chosen)
                                    onSelectCoinForTrade(chosen)
                                },
                                label = "All Coins Dropdown",
                                testTag = "dashboard_all_coins_dropdown"
                            )

                            OutlinedTextField(
                                value = marketsState.searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Quick filter feed (BTC, ETH, SOL, KAS...)") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search crypto price feeds", tint = LgColors.Turquoise) },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LgColors.Turquoise,
                                    unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .circuitTextFieldBackdrop(cornerRadius = 12.dp, drawBorder = false)
                                    .testTag("market_search_input")
                            )

                            val heldSymbols = remember(vaultState.holdings) {
                                vaultState.holdings.map { it.symbol }.toSet()
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(FeedFilter.entries) { filter ->
                                    FilterChip(
                                        selected = activeFilter == filter,
                                        onClick = { activeFilter = filter },
                                        label = {
                                            val suffix = if (filter == FeedFilter.HOLDINGS && heldSymbols.isNotEmpty()) " (${heldSymbols.size})" else ""
                                            Text("${filter.label}$suffix")
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("feed_filter_${filter.name.lowercase()}")
                                    )
                                }
                            }

                            when (val ts = marketsState.tickersState) {
                                is UiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                is UiState.Error -> {
                                    OutlinedCard(
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Live Feed Unavailable", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                                            Text(ts.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            FilledTonalButton(onClick = onRefreshAll) {
                                                Text("Retry Live Feeds")
                                            }
                                        }
                                    }
                                }
                                is UiState.Success -> {
                                    val query = marketsState.searchQuery.trim().lowercase()
                                    val filtered = ts.data.filter { ticker ->
                                        val matchesSearch = query.isEmpty() ||
                                            ticker.symbol.lowercase().contains(query) ||
                                            ticker.name.lowercase().contains(query)
                                        val matchesCategory = when (activeFilter) {
                                            FeedFilter.ALL -> true
                                            FeedFilter.GAINERS -> ticker.change24hPct >= 0.0
                                            FeedFilter.LOSERS -> ticker.change24hPct < 0.0
                                            FeedFilter.HOLDINGS -> heldSymbols.contains(ticker.symbol)
                                        }
                                        matchesSearch && matchesCategory
                                    }

                                    if (filtered.isEmpty()) {
                                        val searchedSym = marketsState.searchQuery.trim().uppercase()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Text(
                                                text = if (searchedSym.isNotBlank()) {
                                                    "$searchedSym loaded in Chart & Trade Desk"
                                                } else {
                                                    "No matching assets for this filter"
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (searchedSym.isNotBlank()) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        onSelectHistoryAsset(searchedSym)
                                                        onSelectCoinForTrade(searchedSym)
                                                        onNavigateToTrade()
                                                    },
                                                    modifier = Modifier.testTag("dashboard_open_searched_coin_trade")
                                                ) {
                                                    Text("Open $searchedSym in Trade Desk")
                                                }
                                            }
                                            TextButton(onClick = { activeFilter = FeedFilter.ALL; onSearchChange("") }) {
                                                Text("Show All Feeds")
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            filtered.forEachIndexed { idx, ticker ->
                                                PriceFeedM3Row(
                                                    ticker = ticker,
                                                    heldQty = vaultState.holdings.firstOrNull { it.symbol == ticker.symbol }?.quantity,
                                                    onClick = {
                                                        onSelectHistoryAsset(ticker.symbol)
                                                        selectedCoinSheet = ticker
                                                    }
                                                )
                                                if (idx < filtered.lastIndex) {
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Latest Crypto Headlines", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = onRefreshNews) { Text("Refresh Wire") }
                            }
                            when (val ns = marketsState.newsState) {
                                is UiState.Loading -> Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }
                                is UiState.Error -> Text("News unavailable: ${ns.message}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                is UiState.Success -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ns.data.take(8).forEach { news ->
                                            OutlinedCard(
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                                colors = CardDefaults.outlinedCardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (news.link.startsWith("http")) {
                                                            runCatching { uriHandler.openUri(news.link) }
                                                        }
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(news.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                        if (news.summary.isNotBlank()) {
                                                            Text(news.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        Text("${news.source} · ${news.publishedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                                    }
                                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open article", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. In-App Social Media Links Card
            item {
                CyberFrameCard(
                    title = "CONNECT · SOCIAL MEDIA",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Follow LG Digital updates, live market alerts, and community channels.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary
                    )
                    SocialMediaLinksBar(compact = false)
                }
            }
        }
    }
}

@Composable
private fun PortfolioHeroM3Card(
    marketsState: MarketsUiState,
    vaultState: VaultUiState,
    onRefreshAll: () -> Unit,
    onNavigateToTrade: () -> Unit,
    onOpenWalkthrough: () -> Unit,
    onOpenBacktest: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    CyberFrameCard(
        title = "HOME",
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.lg_digital_emblem),
                            contentDescription = "LG Digital token crest",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = "COMMAND CENTER",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Digital Walkthrough & Desk Guide",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = onOpenBacktest,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CandlestickChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Backtest") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("dashboard_open_backtest_chip")
                        )
                        AssistChip(
                            onClick = onNavigateToTrade,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = if (vaultState.account.autopilotRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(if (vaultState.account.autopilotRunning) "Bot On" else "Paper")
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        FilledTonalIconButton(
                            onClick = onRefreshAll,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("refresh_markets_button")
                        ) {
                            if (marketsState.isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh dashboard feeds")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile("Risk Profile", vaultState.account.riskProfile, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    MetricTile("Drawdown", "-${"%.2f".format(vaultState.currentDrawdownPct)}%", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    MetricTile("Strategy", vaultState.account.selectedStrategyId.replace('_', ' ').uppercase(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }

                // Command Center Digital Walkthrough Feature Card (uses exact walkthrough image + opens guide + website link)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .circuitTextFieldBackdrop(cornerRadius = 12.dp)
                        .clickable(onClick = onOpenWalkthrough)
                        .padding(10.dp)
                        .testTag("command_center_digital_walkthrough_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_walkthrough_bot),
                        contentDescription = "Digital Walkthrough interactive guide",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.4.dp, LgColors.GoldBright, RoundedCornerShape(10.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "DIGITAL WALKTHROUGH",
                            style = MaterialTheme.typography.titleSmall,
                            color = LgColors.GoldBright,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tap to explore step-by-step how every module, exchange scanner & universal miner works.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LgColors.InkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onOpenWalkthrough,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = LgColors.Turquoise.copy(alpha = 0.2f),
                            contentColor = LgColors.GoldBright
                        ),
                        modifier = Modifier.testTag("command_center_walkthrough_launch_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Launch Interactive Digital Walkthrough"
                        )
                    }
                }
        }
    }
}

@Composable
private fun DashboardShortcutTile(
    title: String,
    imageRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.2.dp, LgColors.Gold.copy(alpha = 0.65f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = LgColors.ElevatedSlate.copy(alpha = 0.72f)
        ),
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$title shortcut",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArbitrageSummaryM3Card(
    state: ArbitrageUiState,
    vaultState: VaultUiState,
    onToggleMode: (ArbitrageMode) -> Unit,
    onScanNow: () -> Unit,
    onExecuteOpportunity: (ArbitrageOpportunity) -> Unit,
    onOpenArbitrageDesk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qualifiedOpps = remember(state.opportunities) { state.opportunities.filter { it.qualified } }
    val topOpps = remember(state.opportunities, qualifiedOpps) {
        (qualifiedOpps.ifEmpty { state.opportunities }).take(3)
    }
    val bestOpp = qualifiedOpps.firstOrNull() ?: state.opportunities.firstOrNull()
    val realizedArbNet = remember(vaultState.arbitrageTrades) {
        vaultState.arbitrageTrades.sumOf { it.worstCaseNetUsd }
    }

    CyberFrameCard(
        title = "ACTIVE ARBITRAGE",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Active Arbitrage Summary",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Badge(
                    containerColor = if (qualifiedOpps.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (qualifiedOpps.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text("${qualifiedOpps.size} LIVE")
                }
            }

            // Mode Switcher & Scan Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArbitrageMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { onToggleMode(mode) },
                        label = { Text(if (mode == ArbitrageMode.DEMO) "Demo Books" else "Live Books", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                FilledTonalButton(
                    onClick = onScanNow,
                    enabled = !state.isScanning,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_scan_arb_button")
                ) {
                    Text(if (state.isScanning) "Scanning" else "Scan ${state.symbol}")
                }
            }

            // 3 Arbitrage KPI Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricTile(
                    label = "Qualified",
                    value = "${qualifiedOpps.size} / ${state.opportunities.size}",
                    valueColor = if (qualifiedOpps.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Best Net",
                    value = bestOpp?.let { Formatters.formatBps(it.worstCaseNetBps) } ?: "--",
                    valueColor = if ((bestOpp?.worstCaseNetBps ?: 0.0) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Arb PnL",
                    value = Formatters.formatUsd(realizedArbNet),
                    valueColor = if (realizedArbNet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = state.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            state.error?.let { err ->
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (topOpps.isEmpty() && !state.isScanning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "Tap Scan ${state.symbol} to evaluate cross-exchange order-book routes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topOpps.forEach { opp ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (opp.qualified) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenArbitrageDesk)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                leadingContent = {
                                    CoinAvatar(opp.symbol, size = 34.dp)
                                },
                                headlineContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        ExchangeLogo(opp.buyVenue, size = 16.dp)
                                        Text(
                                            text = opp.buyVenue,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("->", style = MaterialTheme.typography.labelSmall, color = LgColors.Turquoise)
                                        ExchangeLogo(opp.sellVenue, size = 16.dp)
                                        Text(
                                            text = opp.sellVenue,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                supportingContent = {
                                    Text(
                                        text = "Gross ${Formatters.formatBps(opp.grossSpreadBps)} · Net ${Formatters.formatBps(opp.worstCaseNetBps)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (opp.worstCaseNetBps > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    if (opp.qualified) {
                                        FilledTonalButton(
                                            onClick = { onExecuteOpportunity(opp) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                            Text("+$${opp.worstCaseNetUsd}")
                                        }
                                    } else {
                                        StatusPill(
                                            text = "Blocked",
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            GoldMetallicButton(
                text = "Open Full Arbitrage Scanner",
                onClick = onOpenArbitrageDesk,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_open_arb_desk_button")
            )
        }
    }
}

@Composable
private fun MarketPulseM3Card(
    breadth: MarketBreadth?,
    tickers: List<MarketTicker>,
    onSelectCoin: (MarketTicker) -> Unit,
    modifier: Modifier = Modifier
) {
    val topMovers = remember(tickers) {
        tickers.sortedByDescending { kotlin.math.abs(it.change24hPct) }.take(4)
    }
    CyberFrameCard(
        title = "MARKET PULSE & MOVERS",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Market Breadth & Movers",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                breadth?.let { b ->
                    StatusPill(
                        text = "${b.advancing}/${b.totalAssets} UP",
                        color = if (b.advancing >= b.declining) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            breadth?.let { b ->
                val advFraction = if (b.totalAssets > 0) b.advancing.toFloat() / b.totalAssets else 0.5f
                LinearProgressIndicator(
                    progress = { advFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(
                        label = "Avg 24h",
                        value = Formatters.formatPct(b.avgChangePct),
                        valueColor = if (b.avgChangePct >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "24h Vol",
                        value = Formatters.formatCompactUsd(b.totalVolume24hUsd),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (topMovers.isNotEmpty()) {
                val rows = topMovers.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { ticker ->
                                val up = ticker.change24hPct >= 0
                                OutlinedCard(
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(
                                        1.dp,
                                        (if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error).copy(alpha = 0.45f)
                                    ),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .clickable { onSelectCoin(ticker) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            CoinAvatar(ticker.symbol, size = 24.dp)
                                            Text(ticker.symbol, style = MaterialTheme.typography.titleMedium)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(
                                                imageVector = if (up) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = Formatters.formatPct(ticker.change24hPct),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
    }
}

@Composable
private fun PriceFeedM3Row(
    ticker: MarketTicker,
    heldQty: Double?,
    onClick: () -> Unit
) {
    val up = ticker.change24hPct >= 0
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .testTag("market_row_${ticker.symbol}"),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            CoinAvatar(symbol = ticker.symbol, size = 40.dp)
        },
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${ticker.symbol} / USD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (heldQty != null && heldQty > 0.0) {
                    StatusPill(
                        text = "HELD",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        supportingContent = {
            Text(
                text = "${ticker.name} · Vol ${Formatters.formatCompactUsd(ticker.volume24hUsd)} · ${ticker.source}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HourlyTrendSparkline(
                    prices = ticker.hourlyPrices,
                    positive = up,
                    modifier = Modifier
                        .width(64.dp)
                        .height(28.dp)
                        .testTag("sparkline_${ticker.symbol}")
                )
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = Formatters.formatUsd(ticker.priceUsd),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StatusPill(
                        text = Formatters.formatPct(ticker.change24hPct),
                        color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

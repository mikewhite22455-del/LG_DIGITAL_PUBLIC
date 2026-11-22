package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.Formatters
import com.example.domain.models.MarketTicker
import com.example.domain.models.UiState
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.VaultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketsScreen(
    state: MarketsUiState,
    vaultState: VaultUiState,
    isWide: Boolean = false,
    onRefreshMarkets: () -> Unit,
    onRefreshNews: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectCoinForTrade: (String) -> Unit,
    onSelectCoinForArbitrage: (String) -> Unit,
    onAskCopilotForCoin: (MarketTicker) -> Unit,
    onNavigateToAdvisor: () -> Unit,
    onNavigateToMining: () -> Unit,
    onNavigateToTrade: () -> Unit,
    onOpenAllPagesPreview: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var selectedCoinSheet by remember { mutableStateOf<MarketTicker?>(null) }

    selectedCoinSheet?.let { coin ->
        val up = coin.change24hPct >= 0
        ModalBottomSheet(
            onDismissRequest = { selectedCoinSheet = null },
            containerColor = LgColors.PanelSlate
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            Text("Source: ${coin.source} · Spot USD", style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Formatters.formatUsd(coin.priceUsd), style = MaterialTheme.typography.titleLarge)
                        StatusPill(
                            text = Formatters.formatPct(coin.change24hPct),
                            color = if (up) LgColors.Turquoise else LgColors.Crimson
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("24h High", Formatters.formatUsd(coin.high24h), modifier = Modifier.weight(1f))
                    MetricTile("24h Low", Formatters.formatUsd(coin.low24h), modifier = Modifier.weight(1f))
                    MetricTile("24h Vol", Formatters.formatCompactUsd(coin.volume24hUsd), modifier = Modifier.weight(1f))
                }

                GoldMetallicButton(
                    text = "Open Chart, Strategies & Paper Desk",
                    onClick = {
                        selectedCoinSheet = null
                        onSelectCoinForTrade(coin.symbol)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
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
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LgColors.Gold, modifier = Modifier.padding(end = 6.dp))
                        Text("Ask AI", color = LgColors.Gold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LgColors.ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Tour Hub (matches uploaded Image 3 landing preview)
        item {
            CyberFrameCard(title = "NEW HERE? START WITH THE VISUAL TOUR") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Equity ${Formatters.formatUsd(vaultState.totalPortfolioEquityUsd)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = LgColors.InkPrimary
                        )
                        Text(
                            text = "Cash ${Formatters.formatUsd(vaultState.account.usdBalance)} · ${vaultState.holdings.size} open assets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LgColors.InkSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill(
                            text = if (vaultState.account.autopilotRunning) "BOT ARMED" else "PAPER",
                            color = if (vaultState.account.autopilotRunning) LgColors.Turquoise else LgColors.Gold
                        )
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = LgColors.CyanGlow
                            )
                        } else {
                            IconButton(
                                onClick = onRefreshMarkets,
                                modifier = Modifier.testTag("refresh_markets_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh live market quotes",
                                    tint = LgColors.CyanGlow
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VisualTourTile(
                        title = "Advisor",
                        subtitle = "AI Terminal",
                        imageRes = R.drawable.img_user_advisor_robot,
                        accent = LgColors.CyanGlow,
                        onClick = onNavigateToAdvisor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tour_advisor_card")
                    )
                    VisualTourTile(
                        title = "Mining",
                        subtitle = "AE2 Rig",
                        imageRes = R.drawable.img_user_miner_ae2,
                        accent = LgColors.Turquoise,
                        onClick = onNavigateToMining,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tour_mining_card")
                    )
                    VisualTourTile(
                        title = "Trade",
                        subtitle = "Adaptive Bot",
                        imageRes = R.drawable.img_user_lg_coin,
                        accent = LgColors.Gold,
                        onClick = onNavigateToTrade,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tour_trade_card")
                    )
                }

                GoldMetallicButton(
                    text = "PREVIEW ALL 9 PAGES",
                    onClick = onOpenAllPagesPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_all_pages_preview_button")
                )
            }
        }

        // 24h Top Gainers & Losers + Market Breadth (matches Image 5 Monitor pane)
        if (state.tickersState is UiState.Success || state.breadth != null) {
            item {
                val tickers = (state.tickersState as? UiState.Success)?.data ?: emptyList()
                val gainers = remember(tickers) { tickers.sortedByDescending { it.change24hPct }.take(2) }
                val losers = remember(tickers) { tickers.sortedBy { it.change24hPct }.take(2) }

                if (isWide && state.breadth != null && tickers.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TopGainersLosersCard(
                            gainers = gainers,
                            losers = losers,
                            onSelectTicker = { selectedCoinSheet = it },
                            modifier = Modifier.weight(1f)
                        )
                        MarketBreadthCard(
                            breadth = state.breadth,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (tickers.isNotEmpty()) {
                            TopGainersLosersCard(
                                gainers = gainers,
                                losers = losers,
                                onSelectTicker = { selectedCoinSheet = it }
                            )
                        }
                        state.breadth?.let { MarketBreadthCard(breadth = it) }
                    }
                }
            }
        }

        // Top Movers Carousel
        if (state.tickersState is UiState.Success) {
            val topMovers = state.tickersState.data.sortedByDescending { kotlin.math.abs(it.change24hPct) }.take(6)
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(topMovers, key = { "mover_${it.symbol}" }) { item ->
                        val up = item.change24hPct >= 0
                        GlassPanelCard(
                            modifier = Modifier
                                .width(156.dp)
                                .clickable { selectedCoinSheet = item },
                            highlightBorder = (if (up) LgColors.Turquoise else LgColors.Crimson).copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CoinAvatar(item.symbol, size = 26.dp)
                                    Text(item.symbol, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = Formatters.formatUsd(item.priceUsd),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (up) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (up) LgColors.Turquoise else LgColors.Crimson,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = Formatters.formatPct(item.change24hPct),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (up) LgColors.Turquoise else LgColors.Crimson
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Coin Watch & News Wire Card
        item {
            var showNewsTab by remember { mutableStateOf(false) }
            CyberFrameCard(title = if (showNewsTab) "COINDESK LIVE WIRE" else "COIN WATCH") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showNewsTab = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!showNewsTab) LgColors.Turquoise else LgColors.ElevatedSlate,
                            contentColor = if (!showNewsTab) LgColors.ObsidianBg else LgColors.InkSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.CandlestickChart, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(16.dp))
                        Text("Spot Markets", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showNewsTab = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showNewsTab) LgColors.Gold else LgColors.ElevatedSlate,
                            contentColor = if (showNewsTab) LgColors.ObsidianBg else LgColors.InkSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Newspaper, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(16.dp))
                        Text("News Wire", fontWeight = FontWeight.Bold)
                    }
                }

                if (!showNewsTab) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search BTC, ETH, SOL, LINK...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search markets") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LgColors.Turquoise,
                            unfocusedBorderColor = LgColors.BorderHairline,
                            focusedContainerColor = LgColors.ObsidianBg,
                            unfocusedContainerColor = LgColors.ObsidianBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("market_search_input")
                    )

                    when (val ts = state.tickersState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(color = LgColors.Turquoise) }
                        }
                        is UiState.Error -> {
                            GlassPanelCard(highlightBorder = LgColors.Crimson.copy(alpha = 0.5f)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Market Feed Error", style = MaterialTheme.typography.titleMedium, color = LgColors.Crimson)
                                    Text(ts.message, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary)
                                    TextButton(onClick = onRefreshMarkets) { Text("Retry Feed") }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val q = state.searchQuery.trim().lowercase()
                            val filtered = ts.data.filter {
                                q.isEmpty() || it.symbol.lowercase().contains(q) || it.name.lowercase().contains(q)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filtered.forEach { ticker ->
                                    MarketTickerRow(ticker = ticker, onClick = { selectedCoinSheet = ticker })
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
                        Text("CoinDesk Live RSS", style = MaterialTheme.typography.titleMedium, color = LgColors.Gold)
                        TextButton(onClick = onRefreshNews) { Text("Refresh Wire") }
                    }
                    when (val ns = state.newsState) {
                        is UiState.Loading -> Text("Loading CoinDesk RSS...", color = LgColors.InkMuted)
                        is UiState.Error -> Text("News unavailable: ${ns.message}", color = LgColors.InkMuted)
                        is UiState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ns.data.take(10).forEach { news ->
                                    GlassPanelCard(
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
                                                    Text(news.summary, style = MaterialTheme.typography.bodyMedium, color = LgColors.InkSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                                Text("${news.source} · ${news.publishedAt}", style = MaterialTheme.typography.labelSmall, color = LgColors.Gold)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open article", tint = LgColors.InkMuted, modifier = Modifier.size(18.dp))
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
private fun VisualTourTile(
    title: String,
    subtitle: String,
    imageRes: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LgColors.ElevatedSlate.copy(alpha = 0.72f))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LgColors.ObsidianBg)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$title feature preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, LgColors.ObsidianBg.copy(alpha = 0.45f))
                        )
                    )
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LgColors.InkPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TopGainersLosersCard(
    gainers: List<MarketTicker>,
    losers: List<MarketTicker>,
    onSelectTicker: (MarketTicker) -> Unit,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(title = "24h TOP GAINERS & LOSERS", modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                gainers.forEach { t ->
                    MoverMiniCell(ticker = t, onClick = { onSelectTicker(t) }, modifier = Modifier.weight(1f))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                losers.forEach { t ->
                    MoverMiniCell(ticker = t, onClick = { onSelectTicker(t) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoverMiniCell(
    ticker: MarketTicker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val up = ticker.change24hPct >= 0
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(LgColors.ElevatedSlate.copy(alpha = 0.72f))
            .border(1.dp, LgColors.BorderHairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CoinAvatar(ticker.symbol, size = 22.dp)
            Text(ticker.symbol, style = MaterialTheme.typography.labelLarge, color = LgColors.InkPrimary)
        }
        Text(
            text = Formatters.formatPct(ticker.change24hPct),
            style = MaterialTheme.typography.labelLarge,
            color = if (up) LgColors.Turquoise else LgColors.Crimson,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MarketBreadthCard(
    breadth: com.example.domain.models.MarketBreadth,
    modifier: Modifier = Modifier
) {
    CyberFrameCard(title = "MARKET RADAR", modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CoinGecko & KuCoin Spot Pulse",
                style = MaterialTheme.typography.bodyMedium,
                color = LgColors.InkSecondary
            )
            StatusPill(
                text = "${breadth.advancing}/${breadth.totalAssets} UP",
                color = if (breadth.advancing >= breadth.declining) LgColors.Turquoise else LgColors.Crimson
            )
        }
        val advFraction = if (breadth.totalAssets > 0) breadth.advancing.toFloat() / breadth.totalAssets else 0.5f
        LinearProgressIndicator(
            progress = { advFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = LgColors.Turquoise,
            trackColor = LgColors.Crimson.copy(alpha = 0.45f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricTile(
                label = "Avg 24h",
                value = Formatters.formatPct(breadth.avgChangePct),
                valueColor = if (breadth.avgChangePct >= 0) LgColors.Turquoise else LgColors.Crimson,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "24h Vol",
                value = Formatters.formatCompactUsd(breadth.totalVolume24hUsd),
                modifier = Modifier.weight(1f)
            )
            breadth.topGainer?.let { g ->
                MetricTile(
                    label = "Leader",
                    value = "${g.symbol} ${Formatters.formatPct(g.change24hPct)}",
                    valueColor = LgColors.Turquoise,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MarketTickerRow(
    ticker: MarketTicker,
    onClick: () -> Unit
) {
    val up = ticker.change24hPct >= 0
    GlassPanelCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("market_row_${ticker.symbol}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                CoinAvatar(symbol = ticker.symbol, size = 38.dp)
                Column {
                    Text(
                        text = "${ticker.symbol} / USD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${ticker.name} · Vol ${Formatters.formatCompactUsd(ticker.volume24hUsd)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LgColors.InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatUsd(ticker.priceUsd),
                    style = MaterialTheme.typography.labelLarge
                )
                StatusPill(
                    text = Formatters.formatPct(ticker.change24hPct),
                    color = if (up) LgColors.Turquoise else LgColors.Crimson
                )
            }
        }
    }
}

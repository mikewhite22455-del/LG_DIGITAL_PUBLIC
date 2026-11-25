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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.CoinAssets
import com.example.core.Formatters
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.UiState
import com.example.ui.components.CoinAvatar
import com.example.ui.components.CoinDropdownSelector
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.ExchangeLogo
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.GoldMetallicButton
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.TabLabelTextStyle
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.LabsUiState
import com.example.viewmodel.VaultUiState

enum class LabsTool(val label: String) {
    ARBITRAGE("Arbitrage"),
    SPORTS("Sports"),
    EARN_RISK("Earn & Risk"),
    MINING("Miner"),
    BREAKOUT("Breakouts"),
    DEX_RESEARCH("DEX & Res")
}

@Composable
fun LabsScreen(
    activeTool: LabsTool,
    onSelectTool: (LabsTool) -> Unit,
    state: LabsUiState,
    arbitrageState: ArbitrageUiState,
    vaultState: VaultUiState,
    onUpdateArbParams: (String?, ArbitrageMode?, Double?, Double?, Boolean?) -> Unit,
    onScanArbitrage: () -> Unit,
    onExecuteOpportunity: (ArbitrageOpportunity) -> Unit,
    onResetInventory: () -> Unit,
    onScanBreakouts: () -> Unit,
    onSearchDex: (String) -> Unit,
    onAnalyzeResearch: (String) -> Unit,
    onSaveWatchlist: (String, Double, String, String) -> Unit,
    onRefreshMining: () -> Unit,
    onOpenCoinInTrade: (String) -> Unit
) {
    var dexQueryInput by remember(state.dexQuery) { mutableStateOf(state.dexQuery) }
    var showDexSub by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // 3x2 Quant Labs Selector Header with bold stylish font
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = LabsTool.entries.chunked(3)
            rows.forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTools.forEach { tool ->
                        val selected = tool == activeTool
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onSelectTool(tool)
                                if (tool == LabsTool.BREAKOUT && state.breakoutCandidates.isEmpty()) {
                                    onScanBreakouts()
                                }
                            },
                            label = {
                                Text(
                                    text = tool.label.uppercase(),
                                    style = TabLabelTextStyle,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.22f),
                                selectedLabelColor = LgColors.Gold,
                                containerColor = LgColors.PanelSlate.copy(alpha = 0.8f),
                                labelColor = LgColors.InkSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("labs_tool_${tool.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (activeTool == LabsTool.ARBITRAGE) {
            ArbitrageScreen(
                state = arbitrageState,
                vaultState = vaultState,
                onUpdateParams = onUpdateArbParams,
                onScanNow = onScanArbitrage,
                onExecuteOpportunity = onExecuteOpportunity,
                onResetInventory = onResetInventory
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeTool) {
                    LabsTool.SPORTS -> {
                        item { SportsCalculatorsSection() }
                    }

                    LabsTool.EARN_RISK -> {
                        item { EarnAndRiskCalculatorsSection() }
                    }

                    LabsTool.MINING -> {
                        // Universal Multi-Coin / Multi-Rig Miner Status Card
                        item {
                            CyberFrameCard(title = "UNIVERSAL MINER HUB") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(LgColors.ObsidianBg)
                                        .border(1.dp, LgColors.Turquoise.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_miner_rig),
                                        contentDescription = "Universal Crypto Miner Hardware Rig",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MetricTile("Hardware", "Any ASIC / GPU", LgColors.Gold, Modifier.weight(1f))
                                    MetricTile("Algorithms", "Multi-Algo", LgColors.Turquoise, Modifier.weight(1f))
                                    MetricTile("Coverage", "All PoW Coins", LgColors.CyanGlow, Modifier.weight(1f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Multi-Coin PoW Live Quotes", style = MaterialTheme.typography.titleMedium)
                                        Text("Universal miner profitability & network difficulty across all supported PoW coins", style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
                                    }
                                    Button(
                                        onClick = onRefreshMining,
                                        modifier = Modifier.testTag("refresh_mining_button")
                                    ) { Text("Refresh") }
                                }
                            }
                        }
                        when (val ms = state.miningQuotes) {
                            is UiState.Loading -> item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = LgColors.Turquoise)
                                }
                            }
                            is UiState.Error -> item {
                                GlassPanelCard(highlightBorder = LgColors.AmberWarn.copy(alpha = 0.45f)) {
                                    Text(
                                        text = "Mining feed note: ${ms.message}",
                                        color = LgColors.AmberWarn,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                            is UiState.Success -> {
                                items(ms.data) { q ->
                                    GlassPanelCard {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CoinAvatar(q.symbol, size = 30.dp)
                                                    Text("${q.symbol} · ${q.name}", style = MaterialTheme.typography.titleMedium)
                                                }
                                                StatusPill(q.algorithm, LgColors.Gold)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                MetricTile("Price", Formatters.formatUsd(q.priceUsd), modifier = Modifier.weight(1f))
                                                MetricTile("Reward", "${"%.3f".format(q.blockReward)}", modifier = Modifier.weight(1f))
                                                MetricTile("Hashrate", q.networkHashrateLabel, modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LabsTool.BREAKOUT -> {
                        item {
                            CyberFrameCard(title = "BREAKOUT WATCH") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExchangeLogo("Kraken", size = 22.dp)
                                    Text(
                                        text = "Kraken Live 1h Order Books",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = LgColors.Turquoise
                                    )
                                }
                                GoldMetallicButton(
                                    text = if (state.breakoutLoading) "Scanning Kraken 1h Books..." else "Run 20-Bar Breakout Scan",
                                    onClick = onScanBreakouts,
                                    enabled = !state.breakoutLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("scan_breakouts_button")
                                )
                            }
                        }
                        if (state.breakoutLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = LgColors.Turquoise)
                                }
                            }
                        }
                        state.breakoutError?.let { err ->
                            item { Text(err, color = LgColors.Crimson) }
                        }
                        items(state.breakoutCandidates, key = { it.symbol }) { item ->
                            GlassPanelCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenCoinInTrade(item.symbol) }
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CoinAvatar(item.symbol, size = 30.dp)
                                            Text("${item.symbol} / USD", style = MaterialTheme.typography.titleMedium)
                                        }
                                        StatusPill("Score ${item.strengthScore.toInt()}", LgColors.Gold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        MetricTile("Spot", Formatters.formatUsd(item.priceUsd), modifier = Modifier.weight(1f))
                                        MetricTile("20-Bar Hi", Formatters.formatUsd(item.breakoutLevel), modifier = Modifier.weight(1f))
                                        MetricTile("Vol Mult", "${"%.2f".format(item.volumeRatio)}x", if (item.volumeRatio >= 1.5) LgColors.Turquoise else LgColors.InkSecondary, Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    LabsTool.DEX_RESEARCH -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showDexSub = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!showDexSub) LgColors.Turquoise else LgColors.PanelSlate,
                                        contentColor = if (!showDexSub) LgColors.ObsidianBg else LgColors.InkSecondary
                                    ),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) { Text("24h Coin Research", fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { showDexSub = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showDexSub) LgColors.Gold else LgColors.PanelSlate,
                                        contentColor = if (showDexSub) LgColors.ObsidianBg else LgColors.InkSecondary
                                    ),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) { Text("DEX Screener", fontWeight = FontWeight.Bold) }
                            }
                        }

                        if (!showDexSub) {
                            item {
                                CoinDropdownSelector(
                                    selectedSymbol = state.researchSymbol,
                                    symbols = CoinAssets.catalog.map { it.symbol },
                                    onSelectSymbol = onAnalyzeResearch,
                                    label = "24h Quant Research Coin",
                                    testTag = "research_coin_dropdown"
                                )
                            }
                            when (val rs = state.researchProjection) {
                                is UiState.Loading -> item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = LgColors.Turquoise)
                                    }
                                }
                                is UiState.Error -> item { Text(rs.message, color = LgColors.Crimson) }
                                is UiState.Success -> {
                                    val proj = rs.data
                                    item {
                                        CyberFrameCard(title = "${proj.symbol} 24H QUANT PROJECTION") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    CoinAvatar(proj.symbol, size = 34.dp)
                                                    Text("${proj.symbol} / USD", style = MaterialTheme.typography.titleLarge)
                                                }
                                                StatusPill(proj.regimeLabel, LgColors.CyanGlow)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                MetricTile("Spot", Formatters.formatUsd(proj.currentPrice), modifier = Modifier.weight(1f))
                                                MetricTile(
                                                    "24h Model",
                                                    Formatters.formatUsd(proj.projected24hPrice),
                                                    if (proj.projectedChangePct >= 0) LgColors.Turquoise else LgColors.Crimson,
                                                    Modifier.weight(1f)
                                                )
                                                MetricTile("Drift", Formatters.formatPct(proj.projectedChangePct), modifier = Modifier.weight(1f))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                MetricTile("Support 20", Formatters.formatUsd(proj.support20), modifier = Modifier.weight(1f))
                                                MetricTile("Resist 20", Formatters.formatUsd(proj.resistance20), modifier = Modifier.weight(1f))
                                                MetricTile("RSI(14)", "${"%.1f".format(proj.rsi14)}", modifier = Modifier.weight(1f))
                                            }
                                            Button(
                                                onClick = {
                                                    onSaveWatchlist(
                                                        proj.symbol,
                                                        proj.projected24hPrice,
                                                        proj.regimeLabel,
                                                        "RSI ${"%.1f".format(proj.rsi14)} · ATR ${Formatters.formatUsd(proj.atr14)}"
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                                Text("Save to Room Watchlist")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                CoinDropdownSelector(
                                    selectedSymbol = dexQueryInput.trim().uppercase().ifBlank { "SOL" },
                                    symbols = CoinAssets.catalog.map { it.symbol },
                                    onSelectSymbol = { chosen ->
                                        dexQueryInput = chosen
                                        onSearchDex(chosen)
                                    },
                                    label = "DEX Coin Dropdown",
                                    testTag = "dex_all_coins_dropdown"
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = dexQueryInput,
                                        onValueChange = { dexQueryInput = it },
                                        label = { Text("Token or pair (e.g. SOL, PEPE, HYPE)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = LgColors.Turquoise,
                                            unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .circuitTextFieldBackdrop(drawBorder = false)
                                    )
                                    Button(
                                        onClick = { onSearchDex(dexQueryInput) },
                                        modifier = Modifier.heightIn(min = 52.dp)
                                    ) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search DEX pairs")
                                    }
                                }
                            }
                            when (val ds = state.dexResults) {
                                is UiState.Loading -> item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = LgColors.Turquoise)
                                    }
                                }
                                is UiState.Error -> item { Text(ds.message, color = LgColors.Crimson) }
                                is UiState.Success -> {
                                    items(ds.data.take(15)) { p ->
                                        GlassPanelCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (p.pairUrl.startsWith("http")) {
                                                        runCatching { uriHandler.openUri(p.pairUrl) }
                                                    }
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        CoinAvatar(p.baseSymbol, size = 26.dp)
                                                        Text("${p.baseSymbol} / ${p.quoteSymbol}", style = MaterialTheme.typography.titleMedium)
                                                    }
                                                    StatusPill("${p.chainId} · ${p.dexId}", LgColors.CyanGlow)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    MetricTile("Price", Formatters.formatUsd(p.priceUsd), modifier = Modifier.weight(1f))
                                                    MetricTile("Liq", Formatters.formatCompactUsd(p.liquidityUsd), modifier = Modifier.weight(1f))
                                                    MetricTile(
                                                        "24h",
                                                        Formatters.formatPct(p.priceChange24h),
                                                        if (p.priceChange24h >= 0) LgColors.Turquoise else LgColors.Crimson,
                                                        Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    LabsTool.ARBITRAGE -> Unit
                }
            }
        }
    }
}

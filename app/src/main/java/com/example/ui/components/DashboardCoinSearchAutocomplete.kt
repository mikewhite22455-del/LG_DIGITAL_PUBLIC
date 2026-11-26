package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.ExchangeCoinCatalogStore
import com.example.core.ExchangeListedCoin
import com.example.core.Formatters
import com.example.domain.models.MarketTicker
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.RussoOneFontFamily

private enum class AutocompleteExchangeFilter(val label: String, val exchangeName: String?) {
    ALL("All 3", null),
    KRAKEN("Kraken", "Kraken"),
    COINBASE("Coinbase", "Coinbase"),
    KUCOIN("KuCoin", "KuCoin")
}

/**
 * Dashboard global coin search input with an anchored auto-complete dropdown containing
 * the full live list of coins supported by Kraken, Coinbase, and KuCoin, complete with
 * each coin's logo and the logos of every supporting exchange.
 */
@Composable
fun DashboardCoinSearchAutocomplete(
    query: String,
    onQueryChange: (String) -> Unit,
    liveTickers: List<MarketTicker>,
    onSelectCoin: (ExchangeListedCoin) -> Unit,
    onOpenInTrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val catalog = ExchangeCoinCatalogStore.snapshot
    val focusManager = LocalFocusManager.current

    var dropdownExpanded by remember { mutableStateOf(false) }
    var activeExchangeFilter by remember { mutableStateOf(AutocompleteExchangeFilter.ALL) }
    var lastSelectedCoin by remember { mutableStateOf<ExchangeListedCoin?>(null) }

    val liveTickerBySymbol = remember(liveTickers) {
        liveTickers.associateBy { it.symbol.uppercase() }
    }

    val allSupportedCoins by remember(catalog.triExchangeCoins, liveTickers) {
        derivedStateOf {
            val map = LinkedHashMap<String, ExchangeListedCoin>(catalog.triExchangeCoins.size + liveTickers.size)
            catalog.triExchangeCoins.forEach { coin ->
                map[coin.symbol.uppercase()] = coin
            }
            liveTickers.forEach { ticker ->
                val sym = ticker.symbol.uppercase()
                if (!map.containsKey(sym)) {
                    map[sym] = ExchangeListedCoin(
                        symbol = sym,
                        name = ticker.name,
                        exchanges = listOf("Kraken", "Coinbase", "KuCoin"),
                        primaryPair = "$sym/USD"
                    )
                }
            }
            map.values.toList()
        }
    }

    val filteredCoins by remember(allSupportedCoins, query, activeExchangeFilter) {
        derivedStateOf {
            val trimmed = query.trim().lowercase()
            val exchangeFilter = activeExchangeFilter.exchangeName
            val exchangeFiltered = if (exchangeFilter == null) {
                allSupportedCoins
            } else {
                allSupportedCoins.filter { coin ->
                    coin.exchanges.any { it.equals(exchangeFilter, ignoreCase = true) }
                }
            }

            if (trimmed.isEmpty()) {
                exchangeFiltered
            } else {
                exchangeFiltered
                    .filter { coin ->
                        coin.symbol.lowercase().contains(trimmed) ||
                            coin.name.lowercase().contains(trimmed) ||
                            coin.primaryPair.lowercase().contains(trimmed) ||
                            coin.exchanges.any { it.lowercase().contains(trimmed) }
                    }
                    .sortedWith(
                        compareBy<ExchangeListedCoin>(
                            { !it.symbol.equals(trimmed, ignoreCase = true) },
                            { !it.symbol.lowercase().startsWith(trimmed) },
                            { !it.name.lowercase().startsWith(trimmed) },
                            { -it.exchanges.size },
                            { it.symbol }
                        )
                    )
            }
        }
    }

    val krakenTotal = remember(allSupportedCoins, catalog.krakenCount) {
        if (catalog.krakenCount > 0) catalog.krakenCount
        else allSupportedCoins.count { c -> c.exchanges.any { it.equals("Kraken", ignoreCase = true) } }
    }
    val coinbaseTotal = remember(allSupportedCoins, catalog.coinbaseCount) {
        if (catalog.coinbaseCount > 0) catalog.coinbaseCount
        else allSupportedCoins.count { c -> c.exchanges.any { it.equals("Coinbase", ignoreCase = true) } }
    }
    val kucoinTotal = remember(allSupportedCoins, catalog.kucoinCount) {
        if (catalog.kucoinCount > 0) catalog.kucoinCount
        else allSupportedCoins.count { c -> c.exchanges.any { it.equals("KuCoin", ignoreCase = true) } }
    }

    fun selectAndClose(coin: ExchangeListedCoin) {
        lastSelectedCoin = coin
        dropdownExpanded = false
        focusManager.clearFocus()
        onSelectCoin(coin)
    }

    CyberFrameCard(
        title = stringResource(R.string.dashboard_coin_search_title),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Exchange branding header row with live count & refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ExchangeLogo("Kraken", size = 18.dp)
                    ExchangeLogo("Coinbase", size = 18.dp)
                    ExchangeLogo("KuCoin", size = 18.dp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Kraken · Coinbase · KuCoin",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = LgColors.InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (catalog.isLoadingTriExchange) {
                        CircularProgressIndicator(
                            color = LgColors.Turquoise,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    StatusPill(
                        text = "${allSupportedCoins.size} COINS",
                        color = LgColors.Turquoise
                    )
                    IconButton(
                        onClick = { ExchangeCoinCatalogStore.requestTriExchangeRefresh() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_coin_search_refresh")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.dashboard_coin_search_refresh_cd),
                            tint = LgColors.GoldBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Auto-complete Search Input Field
            OutlinedTextField(
                value = query,
                onValueChange = { updated ->
                    onQueryChange(updated)
                    dropdownExpanded = true
                    if (updated.isBlank()) {
                        lastSelectedCoin = null
                    }
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.dashboard_coin_search_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.dashboard_coin_search_cd),
                        tint = LgColors.Turquoise
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onQueryChange("")
                                    lastSelectedCoin = null
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dashboard_coin_search_clear")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.dashboard_coin_search_clear_cd),
                                    tint = LgColors.GoldBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                dropdownExpanded = !dropdownExpanded
                                if (!dropdownExpanded) focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dashboard_coin_search_dropdown_toggle")
                        ) {
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.dashboard_coin_search_toggle_cd),
                                tint = LgColors.Turquoise
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val topMatch = filteredCoins.firstOrNull()
                        if (topMatch != null) {
                            selectAndClose(topMatch)
                        } else {
                            dropdownExpanded = false
                            focusManager.clearFocus()
                        }
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LgColors.Turquoise,
                    unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.65f),
                    focusedTextColor = LgColors.InkPrimary,
                    unfocusedTextColor = LgColors.InkPrimary,
                    cursorColor = LgColors.GoldBright,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            dropdownExpanded = true
                        }
                    }
                    .circuitTextFieldBackdrop(cornerRadius = 12.dp, drawBorder = false)
                    .testTag("dashboard_coin_search_input")
            )

            // Anchored Auto-Complete Dropdown Menu
            AnimatedVisibility(
                visible = dropdownExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LgColors.ObsidianBg.copy(alpha = 0.96f),
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.2.dp, LgColors.Turquoise.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_coin_autocomplete_dropdown")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Exchange filter chips with logos & counts
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AutocompleteExchangeFilter.entries, key = { it.name }) { filter ->
                                val selected = activeExchangeFilter == filter
                                val count = when (filter) {
                                    AutocompleteExchangeFilter.ALL -> allSupportedCoins.size
                                    AutocompleteExchangeFilter.KRAKEN -> krakenTotal
                                    AutocompleteExchangeFilter.COINBASE -> coinbaseTotal
                                    AutocompleteExchangeFilter.KUCOIN -> kucoinTotal
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { activeExchangeFilter = filter },
                                    leadingIcon = filter.exchangeName?.let { exName ->
                                        { ExchangeLogo(exchangeName = exName, size = 16.dp) }
                                    },
                                    label = {
                                        Text(
                                            text = "${filter.label} ($count)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.24f),
                                        selectedLabelColor = LgColors.GoldBright,
                                        containerColor = LgColors.PanelSlate,
                                        labelColor = LgColors.InkSecondary
                                    ),
                                    modifier = Modifier.testTag("dashboard_autocomplete_filter_${filter.name.lowercase()}")
                                )
                            }
                        }

                        // Result summary & sync state
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    catalog.isLoadingTriExchange && allSupportedCoins.size <= 35 ->
                                        stringResource(R.string.dashboard_coin_search_syncing)
                                    query.isBlank() ->
                                        "Showing ${filteredCoins.size} supported coins"
                                    else ->
                                        "${filteredCoins.size} matches for \"${query.trim()}\""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!catalog.triExchangeError.isNullOrBlank()) {
                                Text(
                                    text = catalog.triExchangeError,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.AmberWarn,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.28f))

                        if (filteredCoins.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_coin_search_empty_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = LgColors.InkPrimary
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_coin_search_empty_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LgColors.InkSecondary
                                )
                            }
                        } else {
                            // Strictly finite height LazyColumn safely virtualizes 1,000+ coins inside Dashboard LazyColumn
                            val listHeight = ((filteredCoins.size.coerceAtMost(5) * 66) + 12).dp.coerceIn(72.dp, 320.dp)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(listHeight)
                                    .testTag("dashboard_coin_autocomplete_list"),
                                contentPadding = PaddingValues(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredCoins, key = { it.symbol }) { coin ->
                                    val liveTicker = liveTickerBySymbol[coin.symbol.uppercase()]
                                    AutocompleteCoinRow(
                                        coin = coin,
                                        liveTicker = liveTicker,
                                        isSelected = lastSelectedCoin?.symbol.equals(coin.symbol, ignoreCase = true),
                                        onClick = { selectAndClose(coin) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Action Banner when a coin is picked from the autocomplete dropdown
            val picked = lastSelectedCoin
            if (!dropdownExpanded && picked != null && query.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LgColors.PanelSlate.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, LgColors.Gold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_selected_coin_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            CoinAvatar(
                                symbol = picked.symbol,
                                size = 34.dp,
                                logoUrl = picked.resolvedLogoUrl
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${picked.symbol} · ${picked.name}",
                                    fontFamily = RussoOneFontFamily,
                                    fontSize = 13.sp,
                                    color = LgColors.GoldBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    picked.exchanges.forEach { venue ->
                                        ExchangeLogo(exchangeName = venue, size = 14.dp)
                                    }
                                    Text(
                                        text = picked.primaryPair,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.InkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = { onOpenInTrade(picked.symbol) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("dashboard_selected_coin_trade_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CandlestickChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.dashboard_coin_search_open_trade),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutocompleteCoinRow(
    coin: ExchangeListedCoin,
    liveTicker: MarketTicker?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) LgColors.Turquoise.copy(alpha = 0.16f)
                else LgColors.PanelSlate.copy(alpha = 0.84f)
            )
            .border(
                width = if (isSelected) 1.3.dp else 1.dp,
                color = if (isSelected) LgColors.GoldBright else LgColors.BorderHairline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("dashboard_autocomplete_item_${coin.symbol.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Official Coin Logo
        CoinAvatar(
            symbol = coin.symbol,
            size = 34.dp,
            logoUrl = coin.resolvedLogoUrl
        )

        // Ticker, Full Name & Supporting Exchange Logos
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = coin.symbol,
                    fontFamily = LgBrandFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (isSelected) LgColors.GoldBright else LgColors.InkPrimary,
                    maxLines = 1
                )
                if (!coin.name.equals(coin.symbol, ignoreCase = true)) {
                    Text(
                        text = coin.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = LgColors.InkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                coin.exchanges.forEach { venue ->
                    ExchangeLogo(exchangeName = venue, size = 16.dp)
                }
                Text(
                    text = "${coin.primaryPair} · ${coin.exchangesLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LgColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Live spot price & 24h directional trend badge when available
        if (liveTicker != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = Formatters.formatUsd(liveTicker.priceUsd),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = LgColors.GoldBright,
                    maxLines = 1
                )
                TrendChangeBadge(
                    changePct = liveTicker.change24hPct,
                    compact = true
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CoinAssets
import com.example.core.ExchangeCoinCatalogStore
import com.example.core.ExchangeListedCoin
import com.example.domain.models.ConnectedExchange
import com.example.ui.theme.LgBrandFontFamily
import com.example.ui.theme.LgColors
import com.example.ui.theme.RussoOneFontFamily

/**
 * Full-exchange searchable coin dropdown dialog powered by LazyColumn so hundreds of live
 * coins from Kraken, Coinbase, KuCoin, or Bitstamp scroll smoothly with instant prefix search.
 */
@Composable
fun ExchangeCoinPickerDialog(
    selectedSymbol: String,
    extraSymbols: List<String>,
    title: String,
    displayLabelFor: (String) -> String,
    onSelectSymbol: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val catalog = ExchangeCoinCatalogStore.snapshot
    var searchQuery by remember { mutableStateOf("") }

    // Merge the full live exchange catalog with any caller-provided symbols
    val allCoins by remember(catalog.coins, extraSymbols, selectedSymbol) {
        derivedStateOf {
            val bySymbol = LinkedHashMap<String, ExchangeListedCoin>()
            catalog.coins.forEach { coin ->
                bySymbol[coin.symbol.uppercase()] = coin
            }
            (extraSymbols + selectedSymbol).forEach { raw ->
                val sym = raw.trim().uppercase()
                if (sym.isNotBlank() && !bySymbol.containsKey(sym)) {
                    val asset = CoinAssets.find(sym)
                    bySymbol[sym] = ExchangeListedCoin(
                        symbol = sym,
                        name = asset?.name ?: sym,
                        exchanges = listOf(catalog.activeScopeLabel),
                        primaryPair = "$sym/USD"
                    )
                }
            }
            bySymbol.values.toList()
        }
    }

    val filteredCoins by remember(allCoins, searchQuery) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                allCoins
            } else {
                allCoins
                    .filter { coin ->
                        coin.symbol.lowercase().contains(q) ||
                            coin.name.lowercase().contains(q) ||
                            coin.primaryPair.lowercase().contains(q) ||
                            coin.exchangesLabel.lowercase().contains(q)
                    }
                    .sortedWith(
                        compareBy<ExchangeListedCoin>(
                            { !it.symbol.equals(q, ignoreCase = true) },
                            { !it.symbol.lowercase().startsWith(q) },
                            { !it.name.lowercase().startsWith(q) },
                            { it.symbol }
                        )
                    )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = LgColors.PanelSlate,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.84f)
                .border(1.6.dp, LgColors.Gold.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
                .testTag("exchange_coin_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { "SELECT COIN" }.uppercase(),
                            fontFamily = LgBrandFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.2.sp,
                            color = LgColors.GoldBright,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${catalog.activeScopeLabel} · ${allCoins.size} live coins offered",
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.Turquoise,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { ExchangeCoinCatalogStore.requestRefresh(catalog.selectedFilter) },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("coin_picker_refresh_button")
                        ) {
                            if (catalog.isLoading) {
                                CircularProgressIndicator(
                                    color = LgColors.Turquoise,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh exchange coins",
                                    tint = LgColors.Turquoise
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("coin_picker_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close coin dropdown",
                                tint = LgColors.GoldBright
                            )
                        }
                    }
                }

                // Exchange Scope Filter Row (switch between Attached / Kraken / Coinbase / KuCoin / Bitstamp)
                val filterChoices = remember(catalog.attachedExchangeIds) {
                    listOf(
                        ConnectedExchange.KRAKEN to "Kraken",
                        ConnectedExchange.COINBASE to "Coinbase",
                        ConnectedExchange.KUCOIN to "KuCoin",
                        ConnectedExchange.BITSTAMP to "Bitstamp",
                        ConnectedExchange.ALL_CONNECTED to if (catalog.attachedExchangeIds.isNotEmpty()) "Attached All" else "All 4"
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterChoices, key = { it.first.id }) { (ex, chipLabel) ->
                        val selected = catalog.selectedFilter == ex
                        FilterChip(
                            selected = selected,
                            onClick = { ExchangeCoinCatalogStore.requestRefresh(ex) },
                            label = {
                                Text(
                                    text = chipLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LgColors.Turquoise.copy(alpha = 0.24f),
                                selectedLabelColor = LgColors.GoldBright,
                                containerColor = LgColors.ObsidianBg.copy(alpha = 0.78f),
                                labelColor = LgColors.InkSecondary
                            ),
                            modifier = Modifier.testTag("coin_picker_exchange_${ex.id}")
                        )
                    }
                }

                // Instant Search Field inside Dropdown
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search all ${allCoins.size} ${catalog.activeScopeLabel} coins (BTC, KAS, PEPE...)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search exchange coins",
                            tint = LgColors.Turquoise
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = LgColors.Gold
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LgColors.Turquoise,
                        unfocusedBorderColor = LgColors.Gold.copy(alpha = 0.55f),
                        focusedTextColor = LgColors.InkPrimary,
                        unfocusedTextColor = LgColors.InkPrimary,
                        cursorColor = LgColors.GoldBright
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coin_picker_search_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "Showing all ${filteredCoins.size} coins"
                        } else {
                            "${filteredCoins.size} matches for \"${searchQuery.trim()}\""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LgColors.InkSecondary
                    )
                    if (!catalog.errorMessage.isNullOrBlank()) {
                        Text(
                            text = catalog.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = LgColors.AmberWarn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.35f))

                // Virtualized LazyColumn for all 350-800+ exchange coins
                if (filteredCoins.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No coin matched \"${searchQuery.trim()}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LgColors.InkPrimary
                            )
                            Text(
                                text = "Try switching the exchange filter above or clearing search.",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.InkSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("coin_picker_list"),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCoins, key = { it.symbol }) { coin ->
                            val isSelected = coin.symbol.equals(selectedSymbol, ignoreCase = true)
                            val displayTitle = remember(coin.symbol, coin.name) {
                                val custom = displayLabelFor(coin.symbol)
                                if (custom != coin.symbol) custom else "${coin.symbol} · ${coin.name}"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) LgColors.Turquoise.copy(alpha = 0.18f)
                                        else LgColors.ObsidianBg.copy(alpha = 0.72f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.3.dp else 1.dp,
                                        color = if (isSelected) LgColors.GoldBright else LgColors.BorderHairline,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onSelectSymbol(coin.symbol)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("coin_picker_item_${coin.symbol.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CoinAvatar(
                                    symbol = coin.symbol,
                                    size = 30.dp,
                                    logoUrl = coin.resolvedLogoUrl
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayTitle,
                                        fontFamily = RussoOneFontFamily,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.4.sp,
                                        color = if (isSelected) LgColors.GoldBright else LgColors.InkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${coin.primaryPair} · ${coin.exchangesLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LgColors.InkSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    coin.exchanges.take(3).forEach { venue ->
                                        ExchangeLogo(exchangeName = venue, size = 18.dp)
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected coin",
                                            tint = LgColors.Turquoise,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

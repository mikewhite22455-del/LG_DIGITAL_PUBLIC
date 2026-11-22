package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.models.MarketTicker
import com.example.ui.theme.LgColors
import com.example.viewmodel.VaultUiState
import kotlin.math.roundToInt

@Composable
fun PortfolioSummaryWidget(
    vaultState: VaultUiState,
    tickers: List<MarketTicker>,
    isRefreshing: Boolean,
    isWide: Boolean,
    onRefresh: () -> Unit,
    onOpenTrade: () -> Unit,
    onSelectHolding: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyPositive = vaultState.dailyPnlUsd >= 0.0
    val dailyColor = if (dailyPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val totalReturnPositive = vaultState.totalReturnUsd >= 0.0
    val unrealizedPositive = vaultState.unrealizedPnlUsd >= 0.0
    val realizedPositive = vaultState.realizedTodayPnlUsd >= 0.0

    val totalEquity = vaultState.totalPortfolioEquityUsd.coerceAtLeast(0.0)
    val investedRatio = if (totalEquity > 0.0) {
        (vaultState.holdingsValueUsd / totalEquity).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val investedPctInt = (investedRatio * 100f).roundToInt().coerceIn(0, 100)
    val cashPctInt = (100 - investedPctInt).coerceIn(0, 100)

    val priceMap = remember(tickers) { tickers.associateBy { it.symbol } }

    CyberFrameCard(
        title = "PORTFOLIO SUMMARY",
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_summary_widget")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Status & Refresh Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LgColors.ElevatedSlate,
                        border = BorderStroke(1.dp, LgColors.Gold.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Portfolio Summary",
                            tint = LgColors.Gold,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp)
                        )
                    }
                    Text(
                        text = "Live valuation & 24h performance",
                        style = MaterialTheme.typography.labelSmall,
                        color = LgColors.InkSecondary
                    )
                }

                FilledTonalIconButton(
                    onClick = onRefresh,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = LgColors.ElevatedSlate,
                        contentColor = LgColors.Turquoise
                    ),
                    modifier = Modifier.testTag("portfolio_summary_refresh_button")
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LgColors.Turquoise)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh portfolio valuation")
                    }
                }
            }

                // Primary KPI Row: Total Portfolio Value + Daily P&L Highlight Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TOTAL PORTFOLIO VALUE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatUsd(vaultState.totalPortfolioEquityUsd),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("portfolio_total_value_text")
                        )
                        val returnPrefix = if (vaultState.totalReturnUsd > 0) "+" else ""
                        Text(
                            text = "All-time: $returnPrefix${Formatters.formatUsd(vaultState.totalReturnUsd)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (totalReturnPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    // Daily P&L Badge Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = dailyColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, dailyColor.copy(alpha = 0.48f)),
                        modifier = Modifier.testTag("portfolio_daily_pnl_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (dailyPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                                    contentDescription = if (dailyPositive) "Daily gain" else "Daily loss",
                                    tint = dailyColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "DAILY P&L",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dailyColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val signedPnl = (if (vaultState.dailyPnlUsd > 0) "+" else "") + Formatters.formatUsd(vaultState.dailyPnlUsd)
                            Text(
                                text = signedPnl,
                                style = MaterialTheme.typography.titleMedium,
                                color = dailyColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("portfolio_daily_pnl_usd")
                            )
                            Text(
                                text = Formatters.formatPct(vaultState.dailyPnlPct),
                                style = MaterialTheme.typography.labelMedium,
                                color = dailyColor,
                                modifier = Modifier.testTag("portfolio_daily_pnl_pct")
                            )
                        }
                    }
                }

                // Capital Allocation Progress (Invested vs Cash)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Invested $investedPctInt% (${Formatters.formatUsd(vaultState.holdingsValueUsd)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cash $cashPctInt% (${Formatters.formatUsd(vaultState.account.usdBalance)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { investedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
                    )
                }

                // 4-Metric P&L Breakdown (4-col on wide, 2x2 grid on compact)
                val unrealizedStr = (if (vaultState.unrealizedPnlUsd > 0) "+" else "") + Formatters.formatUsd(vaultState.unrealizedPnlUsd)
                val realizedStr = (if (vaultState.realizedTodayPnlUsd > 0) "+" else "") + Formatters.formatUsd(vaultState.realizedTodayPnlUsd)

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricTile(
                            label = "Available Cash",
                            value = Formatters.formatUsd(vaultState.account.usdBalance),
                            valueColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Positions (${vaultState.holdings.size})",
                            value = Formatters.formatUsd(vaultState.holdingsValueUsd),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Unrealized P&L",
                            value = unrealizedStr,
                            valueColor = if (unrealizedPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        MetricTile(
                            label = "Realized 24h",
                            value = realizedStr,
                            valueColor = if (realizedPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Available Cash",
                                value = Formatters.formatUsd(vaultState.account.usdBalance),
                                valueColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Positions (${vaultState.holdings.size})",
                                value = Formatters.formatUsd(vaultState.holdingsValueUsd),
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricTile(
                                label = "Unrealized P&L",
                                value = unrealizedStr,
                                valueColor = if (unrealizedPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            MetricTile(
                                label = "Realized 24h",
                                value = realizedStr,
                                valueColor = if (realizedPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Active Holdings Chips (shown when user has open positions)
                if (vaultState.holdings.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(vaultState.holdings, key = { it.symbol }) { holding ->
                            val livePx = priceMap[holding.symbol]?.priceUsd ?: holding.avgEntryPrice
                            val posValue = holding.quantity * livePx
                            val posPnlPct = if (holding.avgEntryPrice > 0.0) {
                                ((livePx - holding.avgEntryPrice) / holding.avgEntryPrice) * 100.0
                            } else 0.0
                            val up = posPnlPct >= 0.0

                            AssistChip(
                                onClick = { onSelectHolding(holding.symbol) },
                                leadingIcon = { CoinAvatar(symbol = holding.symbol, size = 20.dp) },
                                label = {
                                    Text(
                                        text = "${holding.symbol} ${Formatters.formatUsd(posValue)} (${Formatters.formatPct(posPnlPct)})",
                                        color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        maxLines = 1
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.testTag("portfolio_holding_chip_${holding.symbol}")
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenTrade)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "100% cash allocation — open a paper trade to track live P&L",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(
                            onClick = onOpenTrade,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("portfolio_open_trade_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CandlestickChart,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp).size(16.dp)
                            )
                            Text("Trade")
                        }
                    }
                }
            }
        }
    }

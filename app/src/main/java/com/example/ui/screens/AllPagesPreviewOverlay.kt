package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.data.local.PayoutRuleEntity
import com.example.domain.models.ArbitrageMode
import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.ChartOverlay
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.HistoryWindow
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.MarketTicker
import com.example.domain.models.RiskProfile
import com.example.domain.models.StrategyType
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.LgCircuitBackground
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors
import com.example.viewmodel.AdvisorUiState
import com.example.viewmodel.ArbitrageUiState
import com.example.viewmodel.LabsUiState
import com.example.viewmodel.MarketsUiState
import com.example.viewmodel.TradeUiState
import com.example.viewmodel.VaultUiState

enum class PreviewDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    HOME("Home Command", "Live portfolio summary, Fear & Greed, and market feeds", Icons.Filled.Home),
    TRADE("Trade & Backtest Desk", "Paper/Live execution, 11 strategies, and Strategy Backtest Studio", Icons.Filled.CandlestickChart),
    ADVISOR("AI Trading Advisor", "Gemini quant copilot grounded in live desk telemetry", Icons.Filled.AutoAwesome),
    LABS_ARBITRAGE("Cross-Exchange Arbitrage", "Multi-venue order book VWAP spread scanner", Icons.AutoMirrored.Filled.CompareArrows),
    LABS_SPORTS("Sports Edge & Kelly", "Arbitrage, devig, Kelly sizing, and Poisson simulator", Icons.Filled.SportsScore),
    LABS_EARN_RISK("Earn & Risk Calculators", "Compound APY, impermanent loss, and position risk sizer", Icons.Filled.Savings),
    LABS_MINING("Crypto Miner Desk", "Live PoW hardware profitability and network hashrates", Icons.Filled.Memory),
    VAULT("Vault & Security", "Paper ledger, alerts, cold wallets, and AES-256 keys", Icons.Filled.AccountBalanceWallet),
    HELP("Support & Walkthrough", "Interactive guide and AI copilot onboarding", Icons.Filled.HelpOutline)
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AllPagesPreviewOverlay(
    marketsState: MarketsUiState,
    tradeState: TradeUiState,
    advisorState: AdvisorUiState,
    labsState: LabsUiState,
    arbitrageState: ArbitrageUiState,
    vaultState: VaultUiState,
    isWide: Boolean,
    onDismiss: () -> Unit,
    onJumpToDestination: (PreviewDestination) -> Unit,
    onRefreshMarkets: () -> Unit,
    onRefreshNews: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectCoinForTrade: (String) -> Unit,
    onSelectCoinForArbitrage: (String) -> Unit,
    onAskCopilotForCoin: (MarketTicker) -> Unit,
    onSelectTimeframe: (ChartTimeframe) -> Unit,
    onSelectOverlay: (ChartOverlay) -> Unit,
    onSelectStrategy: (StrategyType) -> Unit,
    onSubmitPaperOrder: (Boolean, Double) -> Unit,
    onToggleAutopilot: (Boolean) -> Unit,
    onUpdateRiskConfig: (RiskProfile, String, Double, Boolean) -> Unit,
    onRunAutopilotTick: () -> Unit,
    onSendAdvisorPrompt: (String) -> Unit,
    onUpdateArbParams: (String?, ArbitrageMode?, Double?, Double?, Boolean?) -> Unit,
    onScanArbitrage: () -> Unit,
    onExecuteOpportunity: (ArbitrageOpportunity) -> Unit,
    onResetInventory: () -> Unit,
    onScanBreakouts: () -> Unit,
    onSearchDex: (String) -> Unit,
    onAnalyzeResearch: (String) -> Unit,
    onSaveWatchlist: (String, Double, String, String) -> Unit,
    onRefreshMining: () -> Unit,
    onSavePriceTrigger: (String, Double, Boolean) -> Unit,
    onDeletePriceTrigger: (String) -> Unit,
    onSavePayoutRule: (PayoutRuleEntity) -> Unit,
    onResetPortfolio: () -> Unit,
    onRemoveWatchlist: (String) -> Unit,
    onAddWatchWallet: (String, String, String) -> Unit,
    onRemoveWatchWallet: (Long) -> Unit,
    onSelectHistoryAsset: (String) -> Unit,
    onSelectHistoryWindow: (HistoryWindow) -> Unit,
    onRefreshHistory: () -> Unit,
    onUpdateIndicatorBacktestConfig: (IndicatorBacktestConfig) -> Unit
) {
    BackHandler { onDismiss() }

    LgCircuitBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("all_pages_preview_overlay")
        ) {
            Surface(color = LgColors.PanelSlate.copy(alpha = 0.96f)) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ALL PAGES WORKSPACE MAP", style = MaterialTheme.typography.titleMedium, color = LgColors.GoldBright)
                            Text(
                                text = "Active: ${tradeState.symbol} · Equity ${Formatters.formatUsd(if (tradeState.isLiveMode) tradeState.liveEquityUsd else vaultState.totalPortfolioEquityUsd)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LgColors.Turquoise
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close all pages map", tint = LgColors.Turquoise)
                        }
                    }
                    HorizontalDivider(color = LgColors.Gold.copy(alpha = 0.45f))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PreviewDestination.entries, key = { it.name }) { dest ->
                    GlassPanelCard(
                        highlightBorder = LgColors.Turquoise.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJumpToDestination(dest) }
                            .testTag("preview_jump_${dest.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(dest.icon, contentDescription = null, tint = LgColors.GoldBright, modifier = Modifier.size(26.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(dest.title, style = MaterialTheme.typography.titleMedium, color = LgColors.InkPrimary)
                                Text(dest.subtitle, style = MaterialTheme.typography.bodySmall, color = LgColors.InkSecondary)
                            }
                            StatusPill("OPEN", LgColors.Turquoise)
                        }
                    }
                }
            }
        }
    }
}

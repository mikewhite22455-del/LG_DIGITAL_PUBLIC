package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.engine.CalculatorsEngine
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.theme.LgColors

private data class SportCategoryPreset(
    val name: String,
    val icon: ImageVector,
    val matchLabel: String,
    val oddsA: String,
    val oddsB: String,
    val winPct: String,
    val homeXg: String,
    val awayXg: String
)

private val sportCategories = listOf(
    SportCategoryPreset("Football", Icons.Filled.SportsFootball, "NE vs BUF · O/U 44.5", "2.18", "2.05", "53.5", "2.4", "2.1"),
    SportCategoryPreset("Basketball", Icons.Filled.SportsBasketball, "BOS vs NYK · Total 219.5", "2.12", "2.08", "52.0", "1.9", "1.7"),
    SportCategoryPreset("Hockey", Icons.Filled.SportsHockey, "TOR vs EDM · Puck Line", "2.25", "1.96", "51.5", "3.2", "2.7"),
    SportCategoryPreset("Baseball", Icons.Filled.SportsBaseball, "NYY vs LAD · F5 Run Line", "2.10", "2.04", "52.5", "2.6", "2.1"),
    SportCategoryPreset("Soccer", Icons.Filled.SportsSoccer, "ARS vs MCI · Asian Hcp", "2.15", "2.06", "54.0", "1.65", "1.15"),
    SportCategoryPreset("MMA", Icons.Filled.SportsMma, "Main Card · Method/ML", "2.30", "1.92", "49.5", "1.4", "1.1")
)

@Composable
fun SportsCalculatorsSection() {
    var selectedSport by remember { mutableStateOf(sportCategories.first()) }
    var arbOddsA by remember { mutableStateOf(selectedSport.oddsA) }
    var arbOddsB by remember { mutableStateOf(selectedSport.oddsB) }
    var arbStake by remember { mutableStateOf("500") }

    var kellyOdds by remember { mutableStateOf(selectedSport.oddsA) }
    var kellyWinPct by remember { mutableStateOf(selectedSport.winPct) }
    var kellyBankroll by remember { mutableStateOf("10000") }
    var kellyFrac by remember { mutableStateOf(0.5) }

    var poisHome by remember { mutableStateOf(selectedSport.homeXg) }
    var poisAway by remember { mutableStateOf(selectedSport.awayXg) }

    val arb = CalculatorsEngine.calculateSportsArb(
        arbOddsA.toDoubleOrNull() ?: 0.0,
        arbOddsB.toDoubleOrNull() ?: 0.0,
        arbStake.toDoubleOrNull() ?: 0.0
    )
    val kelly = CalculatorsEngine.calculateKelly(
        kellyOdds.toDoubleOrNull() ?: 2.0,
        kellyWinPct.toDoubleOrNull() ?: 50.0,
        kellyBankroll.toDoubleOrNull() ?: 1000.0,
        kellyFrac
    )
    val devig = CalculatorsEngine.calculateDevig(
        arbOddsA.toDoubleOrNull() ?: 2.0,
        arbOddsB.toDoubleOrNull() ?: 2.0
    )
    val pois = CalculatorsEngine.simulatePoissonMatch(
        poisHome.toDoubleOrNull() ?: 1.4,
        poisAway.toDoubleOrNull() ?: 1.1
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Sport Categories Selector (matches Image 1 left pane)
        CyberFrameCard(title = "SPORTS CATEGORIES") {
            val rows = sportCategories.chunked(2)
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { sport ->
                        val isSel = sport.name == selectedSport.name
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) LgColors.CyanGlow.copy(alpha = 0.16f) else LgColors.ElevatedSlate.copy(alpha = 0.65f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) LgColors.CyanGlow else LgColors.BorderHairline,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedSport = sport
                                    arbOddsA = sport.oddsA
                                    arbOddsB = sport.oddsB
                                    kellyOdds = sport.oddsA
                                    kellyWinPct = sport.winPct
                                    poisHome = sport.homeXg
                                    poisAway = sport.awayXg
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("sport_category_${sport.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = sport.icon,
                                contentDescription = sport.name,
                                tint = if (isSel) LgColors.CyanGlow else LgColors.Gold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = sport.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSel) LgColors.CyanGlow else LgColors.InkPrimary
                            )
                        }
                    }
                }
            }
            Text(
                text = "Loaded template: ${selectedSport.matchLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.Gold
            )
        }

        // 2. Sports Edge & Odds Scanner (Two-Way Arbitrage & De-Vig)
        CyberFrameCard(title = "SPORTS EDGE & ODDS SCANNER") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { arbOddsA = "2.18"; arbOddsB = "2.05"; arbStake = "500" },
                    label = { Text("Load +5.6% Arb") }
                )
                FilterChip(
                    selected = false,
                    onClick = { arbOddsA = "1.91"; arbOddsB = "1.91"; arbStake = "500" },
                    label = { Text("Standard -110 Vig") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = arbOddsA,
                    onValueChange = { arbOddsA = it },
                    label = { Text("Odds A") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = arbOddsB,
                    onValueChange = { arbOddsB = it },
                    label = { Text("Odds B") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = arbStake,
                    onValueChange = { arbStake = it },
                    label = { Text("Stake $") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile(
                    label = if (arb.isArbitrage) "Arb Profit" else "Expected Net",
                    value = "${Formatters.formatUsd(arb.guaranteedProfit)} (${Formatters.formatPct(arb.edgePct)})",
                    valueColor = if (arb.isArbitrage) LgColors.Turquoise else LgColors.Crimson,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Stake Split",
                    value = "${Formatters.formatUsd(arb.stakeA)} / ${Formatters.formatUsd(arb.stakeB)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "No-Vig Fair Odds: ${"%.2f".format(devig.fairOddsA)} (${"%.1f".format(devig.fairProbA)}%) vs ${"%.2f".format(devig.fairOddsB)} (${"%.1f".format(devig.fairProbB)}%) · Vig ${"%.2f".format(devig.vigPct)}%",
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.InkSecondary
            )
        }

        // 3. Bankroll & Kelly Sizing (matches Image 1 right pane)
        CyberFrameCard(title = "BANKROLL & KELLY SIZING") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = kellyOdds, onValueChange = { kellyOdds = it }, label = { Text("Odds") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = kellyWinPct, onValueChange = { kellyWinPct = it }, label = { Text("Win %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = kellyBankroll, onValueChange = { kellyBankroll = it }, label = { Text("Bankroll") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.25 to "1/4 Kelly", 0.5 to "1/2 Kelly", 1.0 to "Full Kelly").forEach { (f, lbl) ->
                    FilterChip(
                        selected = kellyFrac == f,
                        onClick = { kellyFrac = f },
                        label = { Text(lbl) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Kelly Bet", Formatters.formatUsd(kelly.recommendedStakeUsd), LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Fraction", "${"%.2f".format(kelly.fractionalKellyPct)}%", LgColors.Gold, Modifier.weight(1f))
                MetricTile("EV Edge", Formatters.formatPct(kelly.expectedValuePct), if (kelly.expectedValuePct >= 0) LgColors.Turquoise else LgColors.Crimson, Modifier.weight(1f))
            }
        }

        // 4. Poisson Match Simulator
        CyberFrameCard(title = "POISSON SCORELINE MATRIX") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = poisHome, onValueChange = { poisHome = it }, label = { Text("Home xG") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = poisAway, onValueChange = { poisAway = it }, label = { Text("Away xG") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Home Win", "${"%.1f".format(pois.homeWinPct)}%", LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Draw", "${"%.1f".format(pois.drawPct)}%", LgColors.Gold, Modifier.weight(1f))
                MetricTile("Away Win", "${"%.1f".format(pois.awayWinPct)}%", LgColors.CyanGlow, Modifier.weight(1f))
            }
            Text(
                text = "Over 2.5 Goals: ${"%.1f".format(pois.over25Pct)}% · Most Likely Score: ${pois.mostLikelyScore}",
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.InkSecondary
            )
        }
    }
}

@Composable
fun EarnAndRiskCalculatorsSection() {
    var selectedPool by remember { mutableStateOf("ALEO") }
    var principal by remember { mutableStateOf("5000") }
    var apr by remember { mutableStateOf("12.4") }
    var years by remember { mutableStateOf("2") }

    var lpInitial by remember { mutableStateOf("2000") }
    var priceRatio by remember { mutableStateOf("1.8") }

    var riskBal by remember { mutableStateOf("10000") }
    var riskPct by remember { mutableStateOf("1.5") }
    var entryPx by remember { mutableStateOf("86500") }
    var stopPx by remember { mutableStateOf("83900") }

    val apy = CalculatorsEngine.calculateCompoundApy(
        principal.toDoubleOrNull() ?: 0.0,
        apr.toDoubleOrNull() ?: 0.0,
        365,
        years.toDoubleOrNull() ?: 1.0
    )
    val il = CalculatorsEngine.calculateImpermanentLoss(
        lpInitial.toDoubleOrNull() ?: 0.0,
        priceRatio.toDoubleOrNull() ?: 1.0
    )
    val rSize = CalculatorsEngine.calculatePositionRiskSize(
        riskBal.toDoubleOrNull() ?: 0.0,
        riskPct.toDoubleOrNull() ?: 1.0,
        entryPx.toDoubleOrNull() ?: 0.0,
        stopPx.toDoubleOrNull() ?: 0.0
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Staking & Earn Yields (matches Image 1 middle-bottom ALEO Pool 12.4% / USDT Pool 8.2%)
        CyberFrameCard(title = "STAKING & EARN YIELDS") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StakingPoolCard(
                    poolName = "ALEO POOL",
                    aprText = "12.4%",
                    selected = selectedPool == "ALEO",
                    onClick = {
                        selectedPool = "ALEO"
                        apr = "12.4"
                    },
                    modifier = Modifier.weight(1f)
                )
                StakingPoolCard(
                    poolName = "USDT POOL",
                    aprText = "8.2%",
                    selected = selectedPool == "USDT",
                    onClick = {
                        selectedPool = "USDT"
                        apr = "8.2"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = principal, onValueChange = { principal = it }, label = { Text("Principal $") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = apr, onValueChange = { apr = it; selectedPool = "CUSTOM" }, label = { Text("APR %") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = years, onValueChange = { years = it }, label = { Text("Years") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Eff. APY", "${"%.2f".format(apy.effectiveApyPct)}%", LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Total Yield", Formatters.formatUsd(apy.totalYieldUsd), LgColors.Gold, Modifier.weight(1f))
                MetricTile("End Value", Formatters.formatUsd(apy.finalValueUsd), LgColors.InkPrimary, Modifier.weight(1f))
            }
        }

        // Position Risk Sizer
        CyberFrameCard(title = "POSITION RISK SIZER") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = riskBal, onValueChange = { riskBal = it }, label = { Text("Account $") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = riskPct, onValueChange = { riskPct = it }, label = { Text("Risk %") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = entryPx, onValueChange = { entryPx = it }, label = { Text("Entry $") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = stopPx, onValueChange = { stopPx = it }, label = { Text("Stop $") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Max Risk", Formatters.formatUsd(rSize.riskAmountUsd), LgColors.Crimson, Modifier.weight(1f))
                MetricTile("Size Units", Formatters.formatQty(rSize.positionQuantity), LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Notional", Formatters.formatUsd(rSize.positionNotionalUsd), LgColors.CyanGlow, Modifier.weight(1f))
            }
            Text(
                text = "Stop ${"%.2f".format(rSize.stopDistancePct)}% · 2R Target: ${Formatters.formatUsd(rSize.target2RPrice)} · 3R Target: ${Formatters.formatUsd(rSize.target3RPrice)}",
                style = MaterialTheme.typography.labelMedium,
                color = LgColors.Gold
            )
        }

        // 50/50 LP Impermanent Loss
        CyberFrameCard(title = "50/50 LP IMPERMANENT LOSS") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lpInitial, onValueChange = { lpInitial = it }, label = { Text("LP Deposit $") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = priceRatio, onValueChange = { priceRatio = it }, label = { Text("Price Mult (e.g. 2.0)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("IL Impact", "${"%.2f".format(il.ilPct)}%", LgColors.Crimson, Modifier.weight(1f))
                MetricTile("HODL Val", Formatters.formatUsd(il.hodlValueUsd), LgColors.InkPrimary, Modifier.weight(1f))
                MetricTile("LP Val", Formatters.formatUsd(il.lpValueUsd), LgColors.CyanGlow, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StakingPoolCard(
    poolName: String,
    aprText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) LgColors.Turquoise.copy(alpha = 0.14f) else LgColors.ElevatedSlate.copy(alpha = 0.68f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) LgColors.Turquoise else LgColors.BorderHairline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(poolName, style = MaterialTheme.typography.labelLarge, color = LgColors.InkPrimary, fontWeight = FontWeight.Bold)
            if (selected) StatusPill("Active", LgColors.Turquoise)
        }
        Text(aprText, style = MaterialTheme.typography.titleLarge, color = LgColors.Gold, fontWeight = FontWeight.Bold)
        Text("Reference APR", style = MaterialTheme.typography.labelSmall, color = LgColors.InkMuted)
    }
}

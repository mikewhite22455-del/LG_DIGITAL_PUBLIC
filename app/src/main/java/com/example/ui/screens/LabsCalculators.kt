package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.Formatters
import com.example.domain.models.CompoundApyResult
import com.example.domain.models.DevigResult
import com.example.domain.models.ImpermanentLossResult
import com.example.domain.models.KellyResult
import com.example.domain.models.PoissonSimResult
import com.example.domain.models.RiskSizeResult
import com.example.domain.models.SportsArbResult
import com.example.ui.components.CyberFrameCard
import com.example.ui.components.GlassPanelCard
import com.example.ui.components.MetricTile
import com.example.ui.components.StatusPill
import com.example.ui.components.circuitTextFieldBackdrop
import com.example.ui.theme.LgColors
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun SportsCalculatorsSection() {
    // 1. Two-Way Arbitrage Calculator
    var oddsAInput by remember { mutableStateOf("2.10") }
    var oddsBInput by remember { mutableStateOf("2.05") }
    var totalStakeInput by remember { mutableStateOf("500") }

    val arbResult = remember(oddsAInput, oddsBInput, totalStakeInput) {
        val oddsA = (oddsAInput.toDoubleOrNull() ?: 2.10).coerceAtLeast(1.01)
        val oddsB = (oddsBInput.toDoubleOrNull() ?: 2.05).coerceAtLeast(1.01)
        val stake = (totalStakeInput.toDoubleOrNull() ?: 500.0).coerceAtLeast(1.0)
        val invA = 1.0 / oddsA
        val invB = 1.0 / oddsB
        val invSum = invA + invB
        val stakeA = stake * (invA / invSum)
        val stakeB = stake * (invB / invSum)
        val payout = stakeA * oddsA
        val profit = payout - stake
        SportsArbResult(
            impliedProbTotalPct = invSum * 100.0,
            edgePct = (1.0 - invSum) * 100.0,
            isArbitrage = invSum < 1.0,
            stakeA = stakeA,
            stakeB = stakeB,
            guaranteedPayout = payout,
            guaranteedProfit = profit
        )
    }

    // 2. Kelly Criterion Calculator
    var kellyOddsInput by remember { mutableStateOf("2.20") }
    var winProbInput by remember { mutableStateOf("52") }
    var bankrollInput by remember { mutableStateOf("2500") }
    var kellyFractionInput by remember { mutableStateOf("0.5") }

    val kellyResult = remember(kellyOddsInput, winProbInput, bankrollInput, kellyFractionInput) {
        val decOdds = (kellyOddsInput.toDoubleOrNull() ?: 2.20).coerceAtLeast(1.01)
        val p = ((winProbInput.toDoubleOrNull() ?: 52.0) / 100.0).coerceIn(0.01, 0.99)
        val q = 1.0 - p
        val b = decOdds - 1.0
        val bankroll = (bankrollInput.toDoubleOrNull() ?: 2500.0).coerceAtLeast(1.0)
        val frac = (kellyFractionInput.toDoubleOrNull() ?: 0.5).coerceIn(0.05, 1.0)
        val fullKelly = (((b * p) - q) / b).coerceAtLeast(0.0)
        val fracKelly = fullKelly * frac
        val evPct = ((p * decOdds) - 1.0) * 100.0
        KellyResult(
            fullKellyPct = fullKelly * 100.0,
            fractionalKellyPct = fracKelly * 100.0,
            recommendedStakeUsd = bankroll * fracKelly,
            expectedValuePct = evPct
        )
    }

    // 3. No-Vig Fair Odds De-Vigger
    var devigAInput by remember { mutableStateOf("1.91") }
    var devigBInput by remember { mutableStateOf("1.91") }

    val devigResult = remember(devigAInput, devigBInput) {
        val oA = (devigAInput.toDoubleOrNull() ?: 1.91).coerceAtLeast(1.01)
        val oB = (devigBInput.toDoubleOrNull() ?: 1.91).coerceAtLeast(1.01)
        val rawA = 1.0 / oA
        val rawB = 1.0 / oB
        val sum = rawA + rawB
        val fairA = rawA / sum
        val fairB = rawB / sum
        DevigResult(
            fairProbA = fairA * 100.0,
            fairProbB = fairB * 100.0,
            fairOddsA = 1.0 / fairA,
            fairOddsB = 1.0 / fairB,
            vigPct = (sum - 1.0) * 100.0
        )
    }

    // 4. Poisson Goal / Match Simulator
    var homeXgInput by remember { mutableStateOf("1.65") }
    var awayXgInput by remember { mutableStateOf("1.15") }

    val poissonResult = remember(homeXgInput, awayXgInput) {
        computePoissonMatch(
            homeLambda = (homeXgInput.toDoubleOrNull() ?: 1.65).coerceIn(0.1, 6.0),
            awayLambda = (awayXgInput.toDoubleOrNull() ?: 1.15).coerceIn(0.1, 6.0)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CyberFrameCard(title = "2-WAY SPORTS ARBITRAGE & HEDGE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Odds A (Dec)", oddsAInput, { oddsAInput = it }, Modifier.weight(1f), "sports_arb_odds_a")
                CalcNumberField("Odds B (Dec)", oddsBInput, { oddsBInput = it }, Modifier.weight(1f), "sports_arb_odds_b")
                CalcNumberField("Stake ($)", totalStakeInput, { totalStakeInput = it }, Modifier.weight(1f), "sports_arb_stake")
            }
            StatusPill(
                text = if (arbResult.isArbitrage) "SUREBET +${String.format("%.2f", arbResult.edgePct)}%" else "OVERROUND ${String.format("%.2f", arbResult.impliedProbTotalPct)}%",
                color = if (arbResult.isArbitrage) LgColors.Turquoise else LgColors.AmberWarn
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Stake Side A", Formatters.formatUsd(arbResult.stakeA), LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("Stake Side B", Formatters.formatUsd(arbResult.stakeB), LgColors.CyanGlow, Modifier.weight(1f))
                MetricTile(
                    "Net Profit",
                    Formatters.formatUsd(arbResult.guaranteedProfit),
                    if (arbResult.guaranteedProfit >= 0) LgColors.Turquoise else LgColors.Crimson,
                    Modifier.weight(1f)
                )
            }
        }

        GlassPanelCard {
            Text(
                text = "FRACTIONAL KELLY BANKROLL SIZER",
                style = MaterialTheme.typography.titleSmall,
                color = LgColors.GoldBright,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Dec Odds", kellyOddsInput, { kellyOddsInput = it }, Modifier.weight(1f), "kelly_odds_input")
                CalcNumberField("Win Prob %", winProbInput, { winProbInput = it }, Modifier.weight(1f), "kelly_prob_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Bankroll ($)", bankrollInput, { bankrollInput = it }, Modifier.weight(1f), "kelly_bankroll_input")
                CalcNumberField("Kelly Frac", kellyFractionInput, { kellyFractionInput = it }, Modifier.weight(1f), "kelly_frac_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Expected Value", "${String.format("%+.2f", kellyResult.expectedValuePct)}%", if (kellyResult.expectedValuePct >= 0) LgColors.Turquoise else LgColors.Crimson, Modifier.weight(1f))
                MetricTile("Frac Kelly", "${String.format("%.2f", kellyResult.fractionalKellyPct)}%", LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("Rec. Stake", Formatters.formatUsd(kellyResult.recommendedStakeUsd), LgColors.CyanGlow, Modifier.weight(1f))
            }
        }

        GlassPanelCard {
            Text(
                text = "NO-VIG FAIR ODDS DE-VIGGER",
                style = MaterialTheme.typography.titleSmall,
                color = LgColors.Turquoise,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Book Odds A", devigAInput, { devigAInput = it }, Modifier.weight(1f), "devig_a_input")
                CalcNumberField("Book Odds B", devigBInput, { devigBInput = it }, Modifier.weight(1f), "devig_b_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Fair A", "${String.format("%.1f", devigResult.fairProbA)}% (${String.format("%.2f", devigResult.fairOddsA)})", LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Fair B", "${String.format("%.1f", devigResult.fairProbB)}% (${String.format("%.2f", devigResult.fairOddsB)})", LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("Book Vig", "${String.format("%.2f", devigResult.vigPct)}%", LgColors.AmberWarn, Modifier.weight(1f))
            }
        }

        GlassPanelCard {
            Text(
                text = "POISSON MATCH OUTCOME SIMULATOR",
                style = MaterialTheme.typography.titleSmall,
                color = LgColors.CyanGlow,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Home xG", homeXgInput, { homeXgInput = it }, Modifier.weight(1f), "poisson_home_xg")
                CalcNumberField("Away xG", awayXgInput, { awayXgInput = it }, Modifier.weight(1f), "poisson_away_xg")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Home Win", "${String.format("%.1f", poissonResult.homeWinPct)}%", LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Draw", "${String.format("%.1f", poissonResult.drawPct)}%", LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("Away Win", "${String.format("%.1f", poissonResult.awayWinPct)}%", LgColors.Crimson, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Over 2.5 Goals", "${String.format("%.1f", poissonResult.over25Pct)}%", LgColors.CyanGlow, Modifier.weight(1f))
                MetricTile("Top Scoreline", poissonResult.mostLikelyScore, LgColors.Gold, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun EarnAndRiskCalculatorsSection() {
    // 1. Compound APY Yield Calculator
    var principalInput by remember { mutableStateOf("5000") }
    var aprInput by remember { mutableStateOf("14.5") }
    var compoundsPerYearInput by remember { mutableStateOf("365") }
    var monthsInput by remember { mutableStateOf("12") }

    val apyResult = remember(principalInput, aprInput, compoundsPerYearInput, monthsInput) {
        val p = (principalInput.toDoubleOrNull() ?: 5000.0).coerceAtLeast(1.0)
        val apr = ((aprInput.toDoubleOrNull() ?: 14.5) / 100.0).coerceIn(0.0, 50.0)
        val n = (compoundsPerYearInput.toDoubleOrNull() ?: 365.0).coerceIn(1.0, 3650.0)
        val years = ((monthsInput.toDoubleOrNull() ?: 12.0) / 12.0).coerceIn(0.05, 50.0)
        val effApy = ((1.0 + apr / n).pow(n) - 1.0) * 100.0
        val finalVal = p * (1.0 + apr / n).pow(n * years)
        CompoundApyResult(
            effectiveApyPct = effApy,
            finalValueUsd = finalVal,
            totalYieldUsd = finalVal - p
        )
    }

    // 2. DeFi Impermanent Loss Calculator
    var lpDepositInput by remember { mutableStateOf("2000") }
    var priceRatioChangeInput by remember { mutableStateOf("50") }

    val ilResult = remember(lpDepositInput, priceRatioChangeInput) {
        val deposit = (lpDepositInput.toDoubleOrNull() ?: 2000.0).coerceAtLeast(1.0)
        val pctMove = (priceRatioChangeInput.toDoubleOrNull() ?: 50.0).coerceIn(-95.0, 2000.0)
        val k = (1.0 + pctMove / 100.0).coerceAtLeast(0.05)
        val ilFrac = (2.0 * sqrt(k) / (1.0 + k)) - 1.0
        val hodlVal = deposit * 0.5 * (1.0 + k)
        val lpVal = hodlVal * (1.0 + ilFrac)
        ImpermanentLossResult(
            ilPct = ilFrac * 100.0,
            hodlValueUsd = hodlVal,
            lpValueUsd = lpVal,
            netDiffUsd = lpVal - hodlVal
        )
    }

    // 3. Position Sizing & R-Multiple Risk Calculator
    var equityInput by remember { mutableStateOf("10000") }
    var riskPctInput by remember { mutableStateOf("1.5") }
    var entryPriceInput by remember { mutableStateOf("85000") }
    var stopPriceInput by remember { mutableStateOf("82500") }

    val riskResult = remember(equityInput, riskPctInput, entryPriceInput, stopPriceInput) {
        val equity = (equityInput.toDoubleOrNull() ?: 10000.0).coerceAtLeast(1.0)
        val riskPct = (riskPctInput.toDoubleOrNull() ?: 1.5).coerceIn(0.1, 25.0)
        val entry = (entryPriceInput.toDoubleOrNull() ?: 85000.0).coerceAtLeast(0.0001)
        val stop = (stopPriceInput.toDoubleOrNull() ?: 82500.0).coerceAtLeast(0.0001)
        val stopDist = abs(entry - stop).coerceAtLeast(entry * 0.0005)
        val stopDistPct = (stopDist / entry) * 100.0
        val riskAmount = equity * (riskPct / 100.0)
        val qty = riskAmount / stopDist
        val notional = qty * entry
        val direction = if (entry >= stop) 1.0 else -1.0
        RiskSizeResult(
            riskAmountUsd = riskAmount,
            stopDistancePct = stopDistPct,
            positionQuantity = qty,
            positionNotionalUsd = notional,
            target2RPrice = entry + direction * stopDist * 2.0,
            target3RPrice = entry + direction * stopDist * 3.0
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CyberFrameCard(title = "POSITION SIZING & R-MULTIPLE RISK") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Equity ($)", equityInput, { equityInput = it }, Modifier.weight(1f), "risk_equity_input")
                CalcNumberField("Risk %", riskPctInput, { riskPctInput = it }, Modifier.weight(1f), "risk_pct_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Entry ($)", entryPriceInput, { entryPriceInput = it }, Modifier.weight(1f), "risk_entry_input")
                CalcNumberField("Stop ($)", stopPriceInput, { stopPriceInput = it }, Modifier.weight(1f), "risk_stop_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Max Risk", Formatters.formatUsd(riskResult.riskAmountUsd), LgColors.Crimson, Modifier.weight(1f))
                MetricTile("Stop Dist", "${String.format("%.2f", riskResult.stopDistancePct)}%", LgColors.AmberWarn, Modifier.weight(1f))
                MetricTile("Pos Size", Formatters.formatUsd(riskResult.positionNotionalUsd), LgColors.Turquoise, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Units", String.format("%.5f", riskResult.positionQuantity), LgColors.InkPrimary, Modifier.weight(1f))
                MetricTile("2R Target", Formatters.formatUsd(riskResult.target2RPrice), LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("3R Target", Formatters.formatUsd(riskResult.target3RPrice), LgColors.CyanGlow, Modifier.weight(1f))
            }
        }

        GlassPanelCard {
            Text(
                text = "COMPOUND STAKING & APY SIMULATOR",
                style = MaterialTheme.typography.titleSmall,
                color = LgColors.Turquoise,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Principal ($)", principalInput, { principalInput = it }, Modifier.weight(1f), "apy_principal_input")
                CalcNumberField("APR %", aprInput, { aprInput = it }, Modifier.weight(1f), "apy_apr_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("Compounds/Yr", compoundsPerYearInput, { compoundsPerYearInput = it }, Modifier.weight(1f), "apy_compounds_input")
                CalcNumberField("Months", monthsInput, { monthsInput = it }, Modifier.weight(1f), "apy_months_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Effective APY", "${String.format("%.2f", apyResult.effectiveApyPct)}%", LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("Yield Earned", "+${Formatters.formatUsd(apyResult.totalYieldUsd)}", LgColors.Turquoise, Modifier.weight(1f))
                MetricTile("Final Value", Formatters.formatUsd(apyResult.finalValueUsd), LgColors.CyanGlow, Modifier.weight(1f))
            }
        }

        GlassPanelCard {
            Text(
                text = "DEFI LIQUIDITY POOL IMPERMANENT LOSS",
                style = MaterialTheme.typography.titleSmall,
                color = LgColors.GoldBright,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalcNumberField("LP Deposit ($)", lpDepositInput, { lpDepositInput = it }, Modifier.weight(1f), "il_deposit_input")
                CalcNumberField("Asset Move %", priceRatioChangeInput, { priceRatioChangeInput = it }, Modifier.weight(1f), "il_move_input")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("IL Impact", "${String.format("%.2f", ilResult.ilPct)}%", LgColors.Crimson, Modifier.weight(1f))
                MetricTile("HODL Value", Formatters.formatUsd(ilResult.hodlValueUsd), LgColors.GoldBright, Modifier.weight(1f))
                MetricTile("LP Value", Formatters.formatUsd(ilResult.lpValueUsd), LgColors.Turquoise, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CalcNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LgColors.Turquoise,
            unfocusedBorderColor = LgColors.BorderHairline,
            focusedTextColor = LgColors.InkPrimary,
            unfocusedTextColor = LgColors.InkPrimary
        ),
        modifier = modifier
            .circuitTextFieldBackdrop()
            .testTag(tag)
    )
}

private fun computePoissonMatch(homeLambda: Double, awayLambda: Double): PoissonSimResult {
    fun poissonPmf(lambda: Double, k: Int): Double {
        var fact = 1.0
        for (i in 2..k) fact *= i
        return exp(-lambda) * lambda.pow(k) / fact
    }

    var homeWin = 0.0
    var draw = 0.0
    var awayWin = 0.0
    var over25 = 0.0
    var bestProb = -1.0
    var bestScore = "1 - 1"

    for (h in 0..7) {
        val ph = poissonPmf(homeLambda, h)
        for (a in 0..7) {
            val prob = ph * poissonPmf(awayLambda, a)
            when {
                h > a -> homeWin += prob
                h == a -> draw += prob
                else -> awayWin += prob
            }
            if (h + a > 2) over25 += prob
            if (prob > bestProb) {
                bestProb = prob
                bestScore = "$h - $a"
            }
        }
    }
    return PoissonSimResult(
        homeWinPct = homeWin * 100.0,
        drawPct = draw * 100.0,
        awayWinPct = awayWin * 100.0,
        over25Pct = over25 * 100.0,
        mostLikelyScore = bestScore
    )
}

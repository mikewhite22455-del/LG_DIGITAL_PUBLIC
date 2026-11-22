package com.example.domain.engine

import com.example.domain.models.CompoundApyResult
import com.example.domain.models.DevigResult
import com.example.domain.models.ImpermanentLossResult
import com.example.domain.models.KellyResult
import com.example.domain.models.PoissonSimResult
import com.example.domain.models.RiskSizeResult
import com.example.domain.models.SportsArbResult
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object CalculatorsEngine {

    fun calculateSportsArb(
        oddsA: Double,
        oddsB: Double,
        totalStakeUsd: Double
    ): SportsArbResult {
        if (oddsA <= 1.0 || oddsB <= 1.0 || totalStakeUsd <= 0.0) {
            return SportsArbResult(0.0, 0.0, false, 0.0, 0.0, 0.0, 0.0)
        }
        val invA = 1.0 / oddsA
        val invB = 1.0 / oddsB
        val invSum = invA + invB
        val impliedPct = invSum * 100.0
        val edgePct = ((1.0 / invSum) - 1.0) * 100.0
        val stakeA = totalStakeUsd * (invA / invSum)
        val stakeB = totalStakeUsd * (invB / invSum)
        val payout = stakeA * oddsA
        val profit = payout - totalStakeUsd
        return SportsArbResult(
            impliedProbTotalPct = impliedPct,
            edgePct = edgePct,
            isArbitrage = invSum < 1.0,
            stakeA = stakeA,
            stakeB = stakeB,
            guaranteedPayout = payout,
            guaranteedProfit = profit
        )
    }

    fun calculateKelly(
        decimalOdds: Double,
        winProbabilityPct: Double,
        bankrollUsd: Double,
        kellyFraction: Double
    ): KellyResult {
        val p = (winProbabilityPct / 100.0).coerceIn(0.001, 0.999)
        val q = 1.0 - p
        val b = max(decimalOdds - 1.0, 0.01)
        val fullKelly = max(0.0, ((b * p) - q) / b)
        val fractional = fullKelly * kellyFraction.coerceIn(0.1, 1.0)
        val evPct = ((p * b) - q) * 100.0
        return KellyResult(
            fullKellyPct = fullKelly * 100.0,
            fractionalKellyPct = fractional * 100.0,
            recommendedStakeUsd = bankrollUsd * fractional,
            expectedValuePct = evPct
        )
    }

    fun calculateDevig(oddsA: Double, oddsB: Double): DevigResult {
        if (oddsA <= 1.0 || oddsB <= 1.0) {
            return DevigResult(50.0, 50.0, 2.0, 2.0, 0.0)
        }
        val rawA = 1.0 / oddsA
        val rawB = 1.0 / oddsB
        val total = rawA + rawB
        val vigPct = (total - 1.0) * 100.0
        val fairProbA = rawA / total
        val fairProbB = rawB / total
        return DevigResult(
            fairProbA = fairProbA * 100.0,
            fairProbB = fairProbB * 100.0,
            fairOddsA = 1.0 / fairProbA,
            fairOddsB = 1.0 / fairProbB,
            vigPct = vigPct
        )
    }

    fun simulatePoissonMatch(homeLambda: Double, awayLambda: Double): PoissonSimResult {
        val hl = homeLambda.coerceIn(0.1, 6.0)
        val al = awayLambda.coerceIn(0.1, 6.0)

        fun poissonPmf(k: Int, lambda: Double): Double {
            var fact = 1.0
            for (i in 2..k) fact *= i
            return exp(-lambda) * lambda.pow(k) / fact
        }

        var homeWin = 0.0
        var draw = 0.0
        var awayWin = 0.0
        var over25 = 0.0
        var bestScore = "1-1"
        var bestProb = -1.0

        for (h in 0..8) {
            val ph = poissonPmf(h, hl)
            for (a in 0..8) {
                val p = ph * poissonPmf(a, al)
                when {
                    h > a -> homeWin += p
                    h == a -> draw += p
                    else -> awayWin += p
                }
                if (h + a >= 3) over25 += p
                if (p > bestProb) {
                    bestProb = p
                    bestScore = "$h-$a"
                }
            }
        }
        val norm = max(homeWin + draw + awayWin, 1e-9)
        return PoissonSimResult(
            homeWinPct = (homeWin / norm) * 100.0,
            drawPct = (draw / norm) * 100.0,
            awayWinPct = (awayWin / norm) * 100.0,
            over25Pct = (over25 / norm) * 100.0,
            mostLikelyScore = bestScore
        )
    }

    fun calculateCompoundApy(
        principalUsd: Double,
        nominalAprPct: Double,
        compoundsPerYear: Int,
        years: Double
    ): CompoundApyResult {
        val r = (nominalAprPct / 100.0).coerceAtLeast(0.0)
        val n = max(compoundsPerYear, 1)
        val t = years.coerceAtLeast(0.01)
        val apy = ((1.0 + r / n).pow(n) - 1.0) * 100.0
        val finalVal = principalUsd * (1.0 + r / n).pow(n * t)
        return CompoundApyResult(
            effectiveApyPct = apy,
            finalValueUsd = finalVal,
            totalYieldUsd = finalVal - principalUsd
        )
    }

    fun calculateImpermanentLoss(
        initialInvestmentUsd: Double,
        priceRatioChange: Double
    ): ImpermanentLossResult {
        val k = priceRatioChange.coerceAtLeast(0.05)
        val lpMultiplier = (2.0 * sqrt(k)) / (1.0 + k)
        val ilPct = (lpMultiplier - 1.0) * 100.0
        val hodlValue = initialInvestmentUsd * (0.5 + 0.5 * k)
        val lpValue = hodlValue * lpMultiplier
        return ImpermanentLossResult(
            ilPct = ilPct,
            hodlValueUsd = hodlValue,
            lpValueUsd = lpValue,
            netDiffUsd = lpValue - hodlValue
        )
    }

    fun calculatePositionRiskSize(
        accountBalanceUsd: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLossPrice: Double
    ): RiskSizeResult {
        val riskAmount = accountBalanceUsd.coerceAtLeast(0.0) * (riskPercent.coerceIn(0.0, 25.0) / 100.0)
        val dist = abs(entryPrice - stopLossPrice)
        if (entryPrice <= 0.0 || dist <= 1e-9) {
            return RiskSizeResult(riskAmount, 0.0, 0.0, 0.0, entryPrice, entryPrice)
        }
        val isLong = entryPrice >= stopLossPrice
        val qty = riskAmount / dist
        val notional = qty * entryPrice
        val stopPct = (dist / entryPrice) * 100.0
        val t2r = if (isLong) entryPrice + (2.0 * dist) else max(0.0, entryPrice - 2.0 * dist)
        val t3r = if (isLong) entryPrice + (3.0 * dist) else max(0.0, entryPrice - 3.0 * dist)
        return RiskSizeResult(
            riskAmountUsd = riskAmount,
            stopDistancePct = stopPct,
            positionQuantity = qty,
            positionNotionalUsd = notional,
            target2RPrice = t2r,
            target3RPrice = t3r
        )
    }
}

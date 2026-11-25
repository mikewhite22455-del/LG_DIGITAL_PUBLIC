package com.example.domain.engine

import com.example.domain.models.BacktestResult
import com.example.domain.models.BacktestTrade
import com.example.domain.models.BreakoutCandidate
import com.example.domain.models.Candle
import com.example.domain.models.ChartTimeframe
import com.example.domain.models.ConfluenceRule
import com.example.domain.models.EquityCurvePoint
import com.example.domain.models.IndicatorBacktestConfig
import com.example.domain.models.IndicatorBacktestResult
import com.example.domain.models.ResearchProjection
import com.example.domain.models.SignalAction
import com.example.domain.models.SimulatedSignalMarker
import com.example.domain.models.StrategySignal
import com.example.domain.models.StrategyType
import com.example.domain.models.TechnicalIndicatorType
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object TechnicalEngine {

    fun smaSeries(values: List<Double>, period: Int): List<Double?> {
        if (period <= 0) return List(values.size) { null }
        val result = MutableList<Double?>(values.size) { null }
        var rollingSum = 0.0
        for (i in values.indices) {
            rollingSum += values[i]
            if (i >= period) {
                rollingSum -= values[i - period]
            }
            if (i >= period - 1) {
                result[i] = rollingSum / period
            }
        }
        return result
    }

    fun emaSeries(values: List<Double>, period: Int): List<Double?> {
        if (values.isEmpty() || period <= 0) return List(values.size) { null }
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period) return result
        val k = 2.0 / (period + 1)
        var ema = values.take(period).average()
        result[period - 1] = ema
        for (i in period until values.size) {
            ema = (values[i] - ema) * k + ema
            result[i] = ema
        }
        return result
    }

    fun rsiSeries(closes: List<Double>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(closes.size) { null }
        if (closes.size <= period) return result
        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) gainSum += diff else lossSum -= diff
        }
        var avgGain = gainSum / period
        var avgLoss = lossSum / period
        result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        for (i in (period + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period
            result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
        }
        return result
    }

    fun atrSeries(candles: List<Candle>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period) return result
        val tr = DoubleArray(candles.size)
        for (i in candles.indices) {
            val c = candles[i]
            tr[i] = if (i == 0) {
                c.high - c.low
            } else {
                val prevClose = candles[i - 1].close
                max(c.high - c.low, max(abs(c.high - prevClose), abs(c.low - prevClose)))
            }
        }
        var atr = tr.slice(1..period).average()
        result[period] = atr
        for (i in (period + 1) until candles.size) {
            atr = ((atr * (period - 1)) + tr[i]) / period
            result[i] = atr
        }
        return result
    }

    data class BollingerBand(val upper: Double?, val mid: Double?, val lower: Double?)

    fun bollingerSeries(closes: List<Double>, period: Int = 20, mult: Double = 2.0): List<BollingerBand> {
        val sma = smaSeries(closes, period)
        return closes.indices.map { i ->
            val mid = sma[i]
            if (mid == null || i < period - 1) {
                BollingerBand(null, null, null)
            } else {
                val window = closes.subList(i - period + 1, i + 1)
                val variance = window.sumOf { (it - mid) * (it - mid) } / period
                val std = sqrt(variance)
                BollingerBand(mid + mult * std, mid, mid - mult * std)
            }
        }
    }

    data class SupertrendPoint(val value: Double?, val bullish: Boolean)

    fun supertrendSeries(candles: List<Candle>, period: Int = 10, multiplier: Double = 3.0): List<SupertrendPoint> {
        val atr = atrSeries(candles, period)
        val out = MutableList(candles.size) { SupertrendPoint(null, true) }
        var finalUpper = 0.0
        var finalLower = 0.0
        var bullish = true

        for (i in candles.indices) {
            val a = atr[i] ?: continue
            val hl2 = (candles[i].high + candles[i].low) / 2.0
            val basicUpper = hl2 + multiplier * a
            val basicLower = hl2 - multiplier * a
            if (i == period) {
                finalUpper = basicUpper
                finalLower = basicLower
                bullish = candles[i].close >= hl2
            } else {
                val prevClose = candles[i - 1].close
                finalUpper = if (basicUpper < finalUpper || prevClose > finalUpper) basicUpper else finalUpper
                finalLower = if (basicLower > finalLower || prevClose < finalLower) basicLower else finalLower
                bullish = when {
                    bullish && candles[i].close < finalLower -> false
                    !bullish && candles[i].close > finalUpper -> true
                    else -> bullish
                }
            }
            out[i] = SupertrendPoint(if (bullish) finalLower else finalUpper, bullish)
        }
        return out
    }

    fun evaluateStrategy(candles: List<Candle>, strategy: StrategyType): StrategySignal {
        if (candles.size < 30) {
            val price = candles.lastOrNull()?.close ?: 0.0
            return StrategySignal(
                strategy = strategy,
                action = SignalAction.HOLD,
                confidence = 0.0,
                reason = "Insufficient candles (${candles.size}/30) for deterministic evaluation.",
                entryPrice = price
            )
        }
        return evaluateAtIndex(candles, candles.lastIndex, strategy)
    }

    private fun evaluateAtIndex(candles: List<Candle>, idx: Int, strategy: StrategyType): StrategySignal {
        val closes = candles.subList(0, idx + 1).map { it.close }
        val last = candles[idx]
        val prev = candles[idx - 1]
        val price = last.close

        return when (strategy) {
            StrategyType.TREND_CROSS -> {
                val sma9 = smaSeries(closes, 9)
                val sma21 = smaSeries(closes, 21)
                val fNow = sma9[idx] ?: price
                val sNow = sma21[idx] ?: price
                val fPrev = sma9[idx - 1] ?: fNow
                val sPrev = sma21[idx - 1] ?: sNow
                val spreadPct = ((fNow - sNow) / max(sNow, 1e-9)) * 100.0
                val action = when {
                    fPrev <= sPrev && fNow > sNow -> SignalAction.BUY
                    fPrev >= sPrev && fNow < sNow -> SignalAction.SELL
                    fNow > sNow * 1.004 -> SignalAction.BUY
                    fNow < sNow * 0.996 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, min(0.95, 0.52 + abs(spreadPct) * 0.08),
                    "SMA9 ($${"%.2f".format(fNow)}) vs SMA21 ($${"%.2f".format(sNow)}), spread ${"%.2f".format(spreadPct)}%",
                    price, mapOf("SMA9" to fNow, "SMA21" to sNow)
                )
            }

            StrategyType.RSI_REVERSAL -> {
                val rsi = rsiSeries(closes, 14)[idx] ?: 50.0
                val action = when {
                    rsi <= 32.0 -> SignalAction.BUY
                    rsi >= 68.0 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                val conf = if (action == SignalAction.HOLD) 0.4 else min(0.94, 0.65 + abs(50.0 - rsi) * 0.01)
                StrategySignal(
                    strategy, action, conf,
                    "RSI(14) = ${"%.1f".format(rsi)} (oversold <= 32, overbought >= 68)",
                    price, mapOf("RSI14" to rsi)
                )
            }

            StrategyType.RANGE_BREAKOUT -> {
                val window = candles.subList(max(0, idx - 20), idx)
                val high20 = window.maxOf { it.high }
                val low20 = window.minOf { it.low }
                val action = when {
                    price > high20 -> SignalAction.BUY
                    price < low20 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, if (action == SignalAction.HOLD) 0.45 else 0.82,
                    "Close vs 20-bar channel [${"%.2f".format(low20)} – ${"%.2f".format(high20)}]",
                    price, mapOf("High20" to high20, "Low20" to low20)
                )
            }

            StrategyType.EMA_CROSS -> {
                val ema12 = emaSeries(closes, 12)
                val ema26 = emaSeries(closes, 26)
                val fNow = ema12[idx] ?: price
                val sNow = ema26[idx] ?: price
                val fPrev = ema12[idx - 1] ?: fNow
                val sPrev = ema26[idx - 1] ?: sNow
                val diffPct = ((fNow - sNow) / max(sNow, 1e-9)) * 100.0
                val action = when {
                    fPrev <= sPrev && fNow > sNow -> SignalAction.BUY
                    fPrev >= sPrev && fNow < sNow -> SignalAction.SELL
                    fNow > sNow * 1.003 -> SignalAction.BUY
                    fNow < sNow * 0.997 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, min(0.92, 0.55 + abs(diffPct) * 0.07),
                    "EMA12 vs EMA26 delta ${"%.2f".format(diffPct)}%",
                    price, mapOf("EMA12" to fNow, "EMA26" to sNow)
                )
            }

            StrategyType.MACD_CROSS -> {
                val ema12 = emaSeries(closes, 12)
                val ema26 = emaSeries(closes, 26)
                val macdLine = closes.indices.map { i ->
                    val a = ema12[i]
                    val b = ema26[i]
                    if (a != null && b != null) a - b else 0.0
                }
                val signalLine = emaSeries(macdLine, 9)
                val mNow = macdLine[idx]
                val sNow = signalLine[idx] ?: mNow
                val mPrev = macdLine[idx - 1]
                val sPrev = signalLine[idx - 1] ?: mPrev
                val hist = mNow - sNow
                val action = when {
                    mPrev <= sPrev && mNow > sNow -> SignalAction.BUY
                    mPrev >= sPrev && mNow < sNow -> SignalAction.SELL
                    hist > 0 && mNow > 0 -> SignalAction.BUY
                    hist < 0 && mNow < 0 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, if (action == SignalAction.HOLD) 0.45 else 0.78,
                    "MACD ${"%.3f".format(mNow)} vs Signal ${"%.3f".format(sNow)} (Hist ${"%.3f".format(hist)})",
                    price, mapOf("MACD" to mNow, "Signal" to sNow, "Hist" to hist)
                )
            }

            StrategyType.BOLLINGER_REVERSION -> {
                val bb = bollingerSeries(closes, 20, 2.0)[idx]
                val upper = bb.upper ?: price
                val lower = bb.lower ?: price
                val mid = bb.mid ?: price
                val action = when {
                    price <= lower -> SignalAction.BUY
                    price >= upper -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, if (action == SignalAction.HOLD) 0.42 else 0.80,
                    "Price vs Bollinger [${"%.2f".format(lower)} / ${"%.2f".format(mid)} / ${"%.2f".format(upper)}]",
                    price, mapOf("Upper" to upper, "Mid" to mid, "Lower" to lower)
                )
            }

            StrategyType.VOLUME_BREAKOUT -> {
                val window = candles.subList(max(0, idx - 20), idx)
                val high20 = window.maxOf { it.high }
                val low20 = window.minOf { it.low }
                val avgVol = window.map { it.volume }.average().coerceAtLeast(1e-9)
                val volRatio = last.volume / avgVol
                val action = when {
                    price > high20 && volRatio >= 1.5 -> SignalAction.BUY
                    price < low20 && volRatio >= 1.5 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, if (action == SignalAction.HOLD) 0.45 else min(0.95, 0.7 + volRatio * 0.08),
                    "20-bar level with ${"%.2f".format(volRatio)}x volume (threshold 1.50x)",
                    price, mapOf("VolRatio" to volRatio, "High20" to high20, "Low20" to low20)
                )
            }

            StrategyType.VWAP_TREND -> {
                val window = candles.subList(max(0, idx - 19), idx + 1)
                val totalVol = window.sumOf { it.volume }.coerceAtLeast(1e-9)
                val vwap = window.sumOf { ((it.high + it.low + it.close) / 3.0) * it.volume } / totalVol
                val devPct = ((price - vwap) / max(vwap, 1e-9)) * 100.0
                val action = when {
                    prev.close <= vwap && price > vwap -> SignalAction.BUY
                    prev.close >= vwap && price < vwap -> SignalAction.SELL
                    devPct > 0.35 -> SignalAction.BUY
                    devPct < -0.35 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, min(0.89, 0.54 + abs(devPct) * 0.08),
                    "Rolling 20-bar VWAP $${"%.2f".format(vwap)} (dev ${"%.2f".format(devPct)}%)",
                    price, mapOf("VWAP20" to vwap, "DevPct" to devPct)
                )
            }

            StrategyType.ATR_BREAKOUT -> {
                val atr = atrSeries(candles.subList(0, idx + 1), 14)[idx] ?: (price * 0.015)
                val upperTrig = prev.close + 0.8 * atr
                val lowerTrig = prev.close - 0.8 * atr
                val action = when {
                    price > upperTrig -> SignalAction.BUY
                    price < lowerTrig -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, if (action == SignalAction.HOLD) 0.45 else 0.79,
                    "ATR(14) = $${"%.2f".format(atr)}, trigger +/- 0.8× ATR",
                    price, mapOf("ATR14" to atr, "UpperTrig" to upperTrig, "LowerTrig" to lowerTrig)
                )
            }

            StrategyType.SUPERTREND -> {
                val stSeries = supertrendSeries(candles.subList(0, idx + 1), 10, 3.0)
                val stNow = stSeries[idx]
                val stPrev = stSeries[idx - 1]
                val action = when {
                    !stPrev.bullish && stNow.bullish -> SignalAction.BUY
                    stPrev.bullish && !stNow.bullish -> SignalAction.SELL
                    stNow.bullish -> SignalAction.BUY
                    else -> SignalAction.SELL
                }
                StrategySignal(
                    strategy, action, if (stPrev.bullish != stNow.bullish) 0.88 else 0.68,
                    "Supertrend(10,3.0) is ${if (stNow.bullish) "BULLISH" else "BEARISH"} @ $${"%.2f".format(stNow.value ?: price)}",
                    price, mapOf("Supertrend" to (stNow.value ?: price))
                )
            }

            StrategyType.MULTI_PAIR_GRID -> {
                val window = candles.subList(max(0, idx - 30), idx + 1)
                val hi = window.maxOf { it.high }
                val lo = window.minOf { it.low }
                val span = max(hi - lo, price * 0.005)
                val pos = (price - lo) / span
                val action = when {
                    pos <= 0.25 -> SignalAction.BUY
                    pos >= 0.75 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                StrategySignal(
                    strategy, action, if (action == SignalAction.HOLD) 0.42 else 0.76,
                    "Grid channel position ${(pos * 100).toInt()}% inside [$${"%.2f".format(lo)} – $${"%.2f".format(hi)}]",
                    price, mapOf("GridPosPct" to pos * 100.0, "ChannelLow" to lo, "ChannelHigh" to hi)
                )
            }
        }
    }

    fun runBacktest(
        symbol: String,
        candles: List<Candle>,
        strategy: StrategyType,
        timeframe: ChartTimeframe,
        feeBps: Double = 10.0,
        slippageBps: Double = 5.0
    ): BacktestResult {
        if (candles.size < 40) {
            return BacktestResult(
                symbol = symbol,
                strategy = strategy,
                timeframe = timeframe,
                totalReturnPct = 0.0,
                trainReturnPct = 0.0,
                validationReturnPct = 0.0,
                winRatePct = 0.0,
                maxDrawdownPct = 0.0,
                totalTrades = 0,
                feeBps = feeBps,
                slippageBps = slippageBps,
                equityCurve = listOf(10000.0),
                recentTrades = emptyList()
            )
        }

        val startIdx = 28
        val splitIdx = startIdx + ((candles.size - startIdx) * 0.7).toInt()
        val roundTripCost = ((feeBps + slippageBps) * 2.0) / 10000.0

        var equity = 10000.0
        var peakEquity = equity
        var maxDd = 0.0
        var trainEquityAtSplit = equity
        var inPosition = false
        var entryPrice = 0.0
        var wins = 0
        var tradesCount = 0
        val curve = mutableListOf(equity)
        val trades = mutableListOf<BacktestTrade>()

        for (i in startIdx until candles.size) {
            if (i == splitIdx) {
                trainEquityAtSplit = equity
            }
            val sig = evaluateAtIndex(candles, i, strategy)
            val close = candles[i].close
            val phase = if (i < splitIdx) "Train (70%)" else "Validation (30%)"

            if (!inPosition && sig.action == SignalAction.BUY) {
                inPosition = true
                entryPrice = close
            } else if (inPosition && (sig.action == SignalAction.SELL || i == candles.lastIndex)) {
                val rawRet = (close - entryPrice) / max(entryPrice, 1e-9)
                val netRet = rawRet - roundTripCost
                equity *= (1.0 + netRet)
                tradesCount++
                if (netRet > 0.0) wins++
                trades += BacktestTrade(
                    timestampSec = candles[i].timestampSec,
                    side = SignalAction.SELL,
                    price = close,
                    returnPct = netRet * 100.0,
                    phase = phase
                )
                inPosition = false
            }

            val markEquity = if (inPosition) {
                equity * (1.0 + ((close - entryPrice) / max(entryPrice, 1e-9)))
            } else {
                equity
            }
            if (markEquity > peakEquity) peakEquity = markEquity
            val dd = ((peakEquity - markEquity) / max(peakEquity, 1e-9)) * 100.0
            if (dd > maxDd) maxDd = dd
            curve += markEquity
        }

        val totalReturn = ((equity - 10000.0) / 10000.0) * 100.0
        val trainReturn = ((trainEquityAtSplit - 10000.0) / 10000.0) * 100.0
        val valReturn = ((equity - trainEquityAtSplit) / max(trainEquityAtSplit, 1e-9)) * 100.0
        val winRate = if (tradesCount > 0) (wins.toDouble() / tradesCount) * 100.0 else 0.0

        return BacktestResult(
            symbol = symbol,
            strategy = strategy,
            timeframe = timeframe,
            totalReturnPct = totalReturn,
            trainReturnPct = trainReturn,
            validationReturnPct = valReturn,
            winRatePct = winRate,
            maxDrawdownPct = maxDd,
            totalTrades = tradesCount,
            feeBps = feeBps,
            slippageBps = slippageBps,
            equityCurve = curve,
            recentTrades = trades.takeLast(12).reversed()
        )
    }

    fun simulateIndicatorBacktest(
        symbol: String,
        candles: List<Candle>,
        timeframe: ChartTimeframe,
        config: IndicatorBacktestConfig
    ): IndicatorBacktestResult {
        val initialCap = config.initialCapitalUsd.coerceAtLeast(100.0)
        val activeIndicators = config.selectedIndicators
        if (candles.size < 24 || activeIndicators.isEmpty()) {
            return IndicatorBacktestResult(
                symbol = symbol,
                timeframe = timeframe,
                config = config,
                initialCapitalUsd = initialCap,
                finalEquityUsd = initialCap,
                totalReturnPct = 0.0,
                buyHoldReturnPct = 0.0,
                alphaPct = 0.0,
                winRatePct = 0.0,
                maxDrawdownPct = 0.0,
                profitFactor = 0.0,
                totalTrades = 0,
                winningTrades = 0,
                equityPoints = emptyList(),
                markers = emptyList(),
                closedTrades = emptyList()
            )
        }

        val closes = candles.map { it.close }
        val safeRsiPeriod = config.rsiPeriod.coerceIn(4, 50)
        val safeSmaFast = config.smaFastPeriod.coerceIn(2, 60)
        val safeSmaSlow = max(config.smaSlowPeriod.coerceIn(4, 150), safeSmaFast + 2)
        val safeEmaFast = config.emaFastPeriod.coerceIn(2, 60)
        val safeEmaSlow = max(config.emaSlowPeriod.coerceIn(4, 150), safeEmaFast + 2)
        val safeMacdFast = config.macdFast.coerceIn(3, 30)
        val safeMacdSlow = max(config.macdSlow.coerceIn(8, 60), safeMacdFast + 3)
        val safeMacdSignal = config.macdSignal.coerceIn(3, 25)
        val safeBbPeriod = config.bbPeriod.coerceIn(5, 50)

        val rsi = rsiSeries(closes, safeRsiPeriod)
        val smaFast = smaSeries(closes, safeSmaFast)
        val smaSlow = smaSeries(closes, safeSmaSlow)
        val emaCrossFast = emaSeries(closes, safeEmaFast)
        val emaCrossSlow = emaSeries(closes, safeEmaSlow)
        val macdFastSeries = emaSeries(closes, safeMacdFast)
        val macdSlowSeries = emaSeries(closes, safeMacdSlow)
        val macdLine = closes.indices.map { i ->
            val f = macdFastSeries[i]
            val s = macdSlowSeries[i]
            if (f != null && s != null) f - s else 0.0
        }
        val macdSig = emaSeries(macdLine, safeMacdSignal)
        val bb = bollingerSeries(closes, safeBbPeriod, config.bbStdDev.coerceIn(1.0, 3.5))

        val requiredWarmup = buildList {
            add(14)
            if (TechnicalIndicatorType.RSI in activeIndicators) add(safeRsiPeriod + 2)
            if (TechnicalIndicatorType.SMA_CROSS in activeIndicators) add(safeSmaSlow + 1)
            if (TechnicalIndicatorType.EMA_CROSS in activeIndicators) add(safeEmaSlow + 1)
            if (TechnicalIndicatorType.MACD in activeIndicators) add(safeMacdSlow + safeMacdSignal)
            if (TechnicalIndicatorType.BOLLINGER in activeIndicators) add(safeBbPeriod + 1)
        }.maxOrNull() ?: 20

        val warmup = requiredWarmup.coerceIn(8, max(8, candles.size - 10))

        val oneWayCost = ((config.feeBps + config.slippageBps).coerceAtLeast(0.0)) / 10000.0
        val startPrice = candles[warmup].close.coerceAtLeast(1e-9)

        var cashEquity = initialCap
        var inPosition = false
        var entryPrice = 0.0
        var investedCapitalBase = 0.0
        var peakEquity = initialCap
        var maxDrawdownPct = 0.0
        var wins = 0
        var tradesCount = 0
        var grossProfit = 0.0
        var grossLoss = 0.0
        var sumTradeReturnPct = 0.0
        var prevMarkEquity = initialCap
        val stepReturns = mutableListOf<Double>()

        val equityPoints = mutableListOf<EquityCurvePoint>()
        val markers = mutableListOf<SimulatedSignalMarker>()
        val closedTrades = mutableListOf<SimulatedSignalMarker>()

        val useStateMode = activeIndicators.size > 1 && config.confluenceRule == ConfluenceRule.ALL

        for (i in warmup until candles.size) {
            val c = candles[i]
            val close = c.close.coerceAtLeast(1e-9)
            val pointIdx = i - warmup

            val rsiVal = rsi[i]
            val sfNow = smaFast[i]
            val ssNow = smaSlow[i]
            val sfPrev = smaFast.getOrNull(i - 1)
            val ssPrev = smaSlow.getOrNull(i - 1)

            val efNow = emaCrossFast[i]
            val esNow = emaCrossSlow[i]
            val efPrev = emaCrossFast.getOrNull(i - 1)
            val esPrev = emaCrossSlow.getOrNull(i - 1)

            val mNow = macdLine[i]
            val sNow = macdSig[i] ?: mNow
            val mPrev = macdLine[i - 1]
            val sPrev = macdSig[i - 1] ?: mPrev
            val macdHist = mNow - sNow
            val band = bb[i]

            val rsiAction = if (TechnicalIndicatorType.RSI in activeIndicators && rsiVal != null) {
                when {
                    rsiVal <= config.rsiOversold -> SignalAction.BUY
                    rsiVal >= config.rsiOverbought -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
            } else SignalAction.HOLD

            val smaAction = if (
                TechnicalIndicatorType.SMA_CROSS in activeIndicators &&
                sfNow != null && ssNow != null && sfPrev != null && ssPrev != null
            ) {
                when {
                    useStateMode && sfNow > ssNow -> SignalAction.BUY
                    useStateMode && sfNow < ssNow -> SignalAction.SELL
                    !useStateMode && sfPrev <= ssPrev && sfNow > ssNow -> SignalAction.BUY
                    !useStateMode && sfPrev >= ssPrev && sfNow < ssNow -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
            } else SignalAction.HOLD

            val emaAction = if (
                TechnicalIndicatorType.EMA_CROSS in activeIndicators &&
                efNow != null && esNow != null && efPrev != null && esPrev != null
            ) {
                when {
                    useStateMode && efNow > esNow -> SignalAction.BUY
                    useStateMode && efNow < esNow -> SignalAction.SELL
                    !useStateMode && efPrev <= esPrev && efNow > esNow -> SignalAction.BUY
                    !useStateMode && efPrev >= esPrev && efNow < esNow -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
            } else SignalAction.HOLD

            val macdAction = if (TechnicalIndicatorType.MACD in activeIndicators) {
                when {
                    useStateMode && mNow > sNow -> SignalAction.BUY
                    useStateMode && mNow < sNow -> SignalAction.SELL
                    !useStateMode && mPrev <= sPrev && mNow > sNow -> SignalAction.BUY
                    !useStateMode && mPrev >= sPrev && mNow < sNow -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
            } else SignalAction.HOLD

            val bbAction = if (TechnicalIndicatorType.BOLLINGER in activeIndicators && band.lower != null && band.upper != null) {
                when {
                    close <= band.lower -> SignalAction.BUY
                    close >= band.upper -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
            } else SignalAction.HOLD

            val voteMap = buildList {
                if (TechnicalIndicatorType.RSI in activeIndicators) add(TechnicalIndicatorType.RSI to rsiAction)
                if (TechnicalIndicatorType.SMA_CROSS in activeIndicators) add(TechnicalIndicatorType.SMA_CROSS to smaAction)
                if (TechnicalIndicatorType.EMA_CROSS in activeIndicators) add(TechnicalIndicatorType.EMA_CROSS to emaAction)
                if (TechnicalIndicatorType.MACD in activeIndicators) add(TechnicalIndicatorType.MACD to macdAction)
                if (TechnicalIndicatorType.BOLLINGER in activeIndicators) add(TechnicalIndicatorType.BOLLINGER to bbAction)
            }

            val buyVotes = voteMap.count { it.second == SignalAction.BUY }
            val sellVotes = voteMap.count { it.second == SignalAction.SELL }

            val combinedAction = when (config.confluenceRule) {
                ConfluenceRule.ANY -> when {
                    buyVotes > 0 && sellVotes == 0 -> SignalAction.BUY
                    sellVotes > 0 && buyVotes == 0 -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
                ConfluenceRule.ALL -> when {
                    buyVotes == voteMap.size -> SignalAction.BUY
                    sellVotes == voteMap.size -> SignalAction.SELL
                    else -> SignalAction.HOLD
                }
            }

            fun buildTriggerReason(target: SignalAction): String {
                val parts = mutableListOf<String>()
                if (TechnicalIndicatorType.RSI in activeIndicators && rsiAction == target && rsiVal != null) {
                    parts += "RSI ${"%.1f".format(rsiVal)}"
                }
                if (TechnicalIndicatorType.SMA_CROSS in activeIndicators && smaAction == target) {
                    parts += if (target == SignalAction.BUY) "SMA $safeSmaFast/$safeSmaSlow Bull" else "SMA $safeSmaFast/$safeSmaSlow Bear"
                }
                if (TechnicalIndicatorType.EMA_CROSS in activeIndicators && emaAction == target) {
                    parts += if (target == SignalAction.BUY) "EMA $safeEmaFast/$safeEmaSlow Bull" else "EMA $safeEmaFast/$safeEmaSlow Bear"
                }
                if (TechnicalIndicatorType.MACD in activeIndicators && macdAction == target) {
                    parts += if (target == SignalAction.BUY) "MACD Bull (+${"%.2f".format(macdHist)})" else "MACD Bear (${"%.2f".format(macdHist)})"
                }
                if (TechnicalIndicatorType.BOLLINGER in activeIndicators && bbAction == target) {
                    parts += if (target == SignalAction.BUY) "BB Lower Touch" else "BB Upper Touch"
                }
                return parts.joinToString(" + ").ifBlank { "End of Window Close" }
            }

            // Optional Stop Loss / Take Profit check while in an active position
            val stopLossHit = inPosition && config.stopLossPct > 0.0 &&
                ((close - entryPrice) / max(entryPrice, 1e-9)) * 100.0 <= -config.stopLossPct
            val takeProfitHit = inPosition && config.takeProfitPct > 0.0 &&
                ((close - entryPrice) / max(entryPrice, 1e-9)) * 100.0 >= config.takeProfitPct

            if (!inPosition && combinedAction == SignalAction.BUY && i < candles.lastIndex) {
                inPosition = true
                entryPrice = close
                investedCapitalBase = cashEquity * (1.0 - oneWayCost)
                markers += SimulatedSignalMarker(
                    candleIndex = pointIdx,
                    timestampSec = c.timestampSec,
                    action = SignalAction.BUY,
                    priceUsd = close,
                    reason = buildTriggerReason(SignalAction.BUY)
                )
            } else if (inPosition && (stopLossHit || takeProfitHit || combinedAction == SignalAction.SELL || i == candles.lastIndex)) {
                val exitProceeds = investedCapitalBase * (close / entryPrice) * (1.0 - oneWayCost)
                val pnlUsd = exitProceeds - cashEquity
                val netRetPct = ((exitProceeds - cashEquity) / max(cashEquity, 1e-9)) * 100.0
                cashEquity = exitProceeds
                tradesCount++
                sumTradeReturnPct += netRetPct
                if (pnlUsd > 0.0) {
                    wins++
                    grossProfit += pnlUsd
                } else {
                    grossLoss += abs(pnlUsd)
                }
                val reason = when {
                    stopLossHit -> "Stop Loss (-${"%.1f".format(config.stopLossPct)}%)"
                    takeProfitHit -> "Take Profit (+${"%.1f".format(config.takeProfitPct)}%)"
                    combinedAction == SignalAction.SELL -> buildTriggerReason(SignalAction.SELL)
                    else -> "End of Window Close"
                }
                val sellMarker = SimulatedSignalMarker(
                    candleIndex = pointIdx,
                    timestampSec = c.timestampSec,
                    action = SignalAction.SELL,
                    priceUsd = close,
                    entryPriceUsd = entryPrice,
                    returnPct = netRetPct,
                    pnlUsd = pnlUsd,
                    reason = reason
                )
                markers += sellMarker
                closedTrades += sellMarker
                inPosition = false
            }

            val markEquity = if (inPosition) {
                investedCapitalBase * (close / entryPrice)
            } else {
                cashEquity
            }
            if (prevMarkEquity > 0.0) {
                stepReturns += (markEquity - prevMarkEquity) / prevMarkEquity
            }
            prevMarkEquity = markEquity

            if (markEquity > peakEquity) peakEquity = markEquity
            val dd = ((peakEquity - markEquity) / max(peakEquity, 1e-9)) * 100.0
            if (dd > maxDrawdownPct) maxDrawdownPct = dd

            val buyHoldEquity = initialCap * (close / startPrice)
            equityPoints += EquityCurvePoint(
                candleIndex = pointIdx,
                timestampSec = c.timestampSec,
                priceUsd = close,
                strategyEquityUsd = markEquity,
                buyHoldEquityUsd = buyHoldEquity,
                rsiValue = rsiVal,
                macdHistogram = macdHist,
                bbUpper = band.upper,
                bbMid = band.mid,
                bbLower = band.lower,
                smaFast = sfNow,
                smaSlow = ssNow,
                emaFast = efNow,
                emaSlow = esNow
            )
        }

        val finalEq = equityPoints.lastOrNull()?.strategyEquityUsd ?: cashEquity
        val finalBuyHoldEq = equityPoints.lastOrNull()?.buyHoldEquityUsd ?: initialCap
        val totalReturnPct = ((finalEq - initialCap) / initialCap) * 100.0
        val buyHoldReturnPct = ((finalBuyHoldEq - initialCap) / initialCap) * 100.0
        val winRatePct = if (tradesCount > 0) (wins.toDouble() / tradesCount) * 100.0 else 0.0
        val avgTradeReturnPct = if (tradesCount > 0) sumTradeReturnPct / tradesCount else 0.0
        val profitFactor = when {
            grossLoss > 1e-6 -> grossProfit / grossLoss
            grossProfit > 0.0 -> 99.9
            else -> 0.0
        }

        val sharpeEstimate = if (stepReturns.size >= 5) {
            val mean = stepReturns.average()
            val variance = stepReturns.sumOf { (it - mean) * (it - mean) } / stepReturns.size
            val std = sqrt(variance)
            val periodsPerYear = when (timeframe) {
                ChartTimeframe.M15 -> 35040.0
                ChartTimeframe.H1 -> 8760.0
                ChartTimeframe.H4 -> 2190.0
                ChartTimeframe.D1 -> 365.0
            }
            if (std > 1e-9) ((mean / std) * sqrt(periodsPerYear)).coerceIn(-9.9, 9.9) else 0.0
        } else 0.0

        return IndicatorBacktestResult(
            symbol = symbol,
            timeframe = timeframe,
            config = config,
            initialCapitalUsd = initialCap,
            finalEquityUsd = finalEq,
            totalReturnPct = totalReturnPct,
            buyHoldReturnPct = buyHoldReturnPct,
            alphaPct = totalReturnPct - buyHoldReturnPct,
            winRatePct = winRatePct,
            maxDrawdownPct = maxDrawdownPct,
            profitFactor = profitFactor,
            totalTrades = tradesCount,
            winningTrades = wins,
            equityPoints = equityPoints,
            markers = markers,
            closedTrades = closedTrades.reversed(),
            avgTradeReturnPct = avgTradeReturnPct,
            sharpeEstimate = sharpeEstimate
        )
    }

    fun project24h(symbol: String, candles: List<Candle>): ResearchProjection {
        val current = candles.lastOrNull()?.close ?: 0.0
        if (candles.size < 21 || current <= 0.0) {
            return ResearchProjection(symbol, current, current, 0.0, current, current, 50.0, 0.0, "Insufficient Data")
        }
        val closes = candles.map { it.close }
        val rsi = rsiSeries(closes, 14).lastOrNull() ?: 50.0
        val atr = atrSeries(candles, 14).lastOrNull() ?: (current * 0.02)
        val lookback = candles.takeLast(20)
        val support = lookback.minOf { it.low }
        val resistance = lookback.maxOf { it.high }
        val prev24Close = candles[max(0, candles.size - 24)].close
        val momentum24h = (current - prev24Close) / max(prev24Close, 1e-9)
        val sma9 = smaSeries(closes, 9).lastOrNull() ?: current
        val sma21 = smaSeries(closes, 21).lastOrNull() ?: current

        val dampedDrift = (momentum24h * 0.38).coerceIn(-0.08, 0.08)
        val rsiBias = ((50.0 - rsi) / 50.0) * 0.012
        val projDelta = dampedDrift + rsiBias
        val projectedPrice = current * (1.0 + projDelta)
        val regime = when {
            sma9 > sma21 * 1.005 && rsi > 52 -> "Bullish Expansion"
            sma9 < sma21 * 0.995 && rsi < 48 -> "Bearish Pressure"
            else -> "Range Consolidation"
        }
        return ResearchProjection(
            symbol = symbol,
            currentPrice = current,
            projected24hPrice = projectedPrice,
            projectedChangePct = projDelta * 100.0,
            support20 = support,
            resistance20 = resistance,
            rsi14 = rsi,
            atr14 = atr,
            regimeLabel = regime
        )
    }

    fun findBreakouts(symbolToCandles: Map<String, List<Candle>>): List<BreakoutCandidate> {
        return symbolToCandles.mapNotNull { (symbol, candles) ->
            if (candles.size < 22) return@mapNotNull null
            val last = candles.last()
            val window = candles.subList(candles.size - 21, candles.size - 1)
            val high20 = window.maxOf { it.high }
            val avgVol = window.map { it.volume }.average().coerceAtLeast(1e-9)
            val volRatio = last.volume / avgVol
            val prev24 = candles[max(0, candles.size - 24)].close
            val change24h = ((last.close - prev24) / max(prev24, 1e-9)) * 100.0
            val distFromHighPct = ((last.close - high20) / max(high20, 1e-9)) * 100.0
            val score = (volRatio * 30.0) + max(0.0, distFromHighPct * 12.0) + max(0.0, change24h * 2.0)
            BreakoutCandidate(
                symbol = symbol,
                priceUsd = last.close,
                breakoutLevel = high20,
                volumeRatio = volRatio,
                change24hPct = change24h,
                strengthScore = score
            )
        }.sortedByDescending { it.strengthScore }
    }

    fun isInsideStopSchedule(windowStr: String, calendar: Calendar = Calendar.getInstance()): Boolean {
        val parts = windowStr.trim().split("-")
        if (parts.size != 2) return false
        fun toMinutes(hhmm: String): Int? {
            val hm = hhmm.trim().split(":")
            if (hm.size != 2) return null
            val h = hm[0].toIntOrNull() ?: return null
            val m = hm[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return h * 60 + m
        }
        val start = toMinutes(parts[0]) ?: return false
        val end = toMinutes(parts[1]) ?: return false
        val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (start <= end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }
}

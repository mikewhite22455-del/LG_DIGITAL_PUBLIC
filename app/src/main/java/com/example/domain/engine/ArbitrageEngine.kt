package com.example.domain.engine

import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.OrderBookLevel
import com.example.domain.models.OrderBookSnapshot
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random

object ArbitrageEngine {

    private val MC = MathContext(12, RoundingMode.HALF_UP)
    private val BPS_DIVISOR = BigDecimal("10000")

    private data class DepthFill(
        val vwap: BigDecimal,
        val baseQty: BigDecimal,
        val filledFull: Boolean
    )

    private fun walkAskBookForNotional(asks: List<OrderBookLevel>, targetUsd: BigDecimal): DepthFill {
        var remainingUsd = targetUsd
        var totalBase = BigDecimal.ZERO
        var totalUsd = BigDecimal.ZERO

        for (level in asks.sortedBy { it.price }) {
            if (remainingUsd <= BigDecimal.ZERO) break
            val levelCapUsd = level.price.multiply(level.quantity, MC)
            if (levelCapUsd <= remainingUsd) {
                totalBase = totalBase.add(level.quantity, MC)
                totalUsd = totalUsd.add(levelCapUsd, MC)
                remainingUsd = remainingUsd.subtract(levelCapUsd, MC)
            } else {
                val partialBase = remainingUsd.divide(level.price, MC)
                totalBase = totalBase.add(partialBase, MC)
                totalUsd = totalUsd.add(remainingUsd, MC)
                remainingUsd = BigDecimal.ZERO
            }
        }
        val filled = remainingUsd <= BigDecimal("0.01") && totalBase > BigDecimal.ZERO
        val vwap = if (totalBase > BigDecimal.ZERO) totalUsd.divide(totalBase, MC) else BigDecimal.ZERO
        return DepthFill(vwap, totalBase, filled)
    }

    private fun walkBidBookForQuantity(bids: List<OrderBookLevel>, targetBase: BigDecimal): DepthFill {
        var remainingBase = targetBase
        var totalUsd = BigDecimal.ZERO
        var soldBase = BigDecimal.ZERO

        for (level in bids.sortedByDescending { it.price }) {
            if (remainingBase <= BigDecimal.ZERO) break
            val fillQty = if (level.quantity <= remainingBase) level.quantity else remainingBase
            totalUsd = totalUsd.add(fillQty.multiply(level.price, MC), MC)
            soldBase = soldBase.add(fillQty, MC)
            remainingBase = remainingBase.subtract(fillQty, MC)
        }
        val filled = remainingBase <= BigDecimal("0.000001") && soldBase > BigDecimal.ZERO
        val vwap = if (soldBase > BigDecimal.ZERO) totalUsd.divide(soldBase, MC) else BigDecimal.ZERO
        return DepthFill(vwap, soldBase, filled)
    }

    fun scanBooks(
        symbol: String,
        books: List<OrderBookSnapshot>,
        tradeNotionalUsd: Double,
        venueUsdBalances: Map<String, Double>,
        venueCoinBalances: Map<String, Double>,
        takerFeeBps: Double = 16.0,
        slippageBps: Double = 8.0,
        rebalanceBps: Double = 5.0,
        minSpreadBps: Double = 35.0,
        minNetUsd: Double = 0.50,
        maxAgeMs: Long = 30_000L
    ): List<ArbitrageOpportunity> {
        val now = System.currentTimeMillis()
        val notional = BigDecimal.valueOf(tradeNotionalUsd)
        val feeRate = BigDecimal.valueOf(takerFeeBps).divide(BPS_DIVISOR, MC)
        val slipRate = BigDecimal.valueOf(slippageBps).divide(BPS_DIVISOR, MC)
        val rebRate = BigDecimal.valueOf(rebalanceBps).divide(BPS_DIVISOR, MC)

        val opportunities = mutableListOf<ArbitrageOpportunity>()

        for (buyBook in books) {
            for (sellBook in books) {
                if (buyBook.venue == sellBook.venue) continue
                val buyFill = walkAskBookForNotional(buyBook.asks, notional)
                val sellFill = if (buyFill.baseQty > BigDecimal.ZERO) {
                    walkBidBookForQuantity(sellBook.bids, buyFill.baseQty)
                } else {
                    DepthFill(BigDecimal.ZERO, BigDecimal.ZERO, false)
                }

                val reasons = mutableListOf<String>()
                if (!buyFill.filledFull) reasons += "Insufficient ask depth on ${buyBook.venue}"
                if (!sellFill.filledFull) reasons += "Insufficient bid depth on ${sellBook.venue}"
                if (now - buyBook.timestampMs > maxAgeMs || now - sellBook.timestampMs > maxAgeMs) {
                    reasons += "Stale quote (> ${maxAgeMs / 1000}s)"
                }

                val buyUsdAvail = venueUsdBalances[buyBook.venue] ?: 0.0
                if (buyUsdAvail < tradeNotionalUsd) {
                    reasons += "Low USD on ${buyBook.venue} (${"%.0f".format(buyUsdAvail)} < ${"%.0f".format(tradeNotionalUsd)})"
                }

                val sellCoinAvail = venueCoinBalances["${sellBook.venue}:$symbol"] ?: 0.0
                if (sellCoinAvail < buyFill.baseQty.toDouble()) {
                    reasons += "Low $symbol inventory on ${sellBook.venue}"
                }

                val grossProfitUsd = if (buyFill.vwap > BigDecimal.ZERO && sellFill.vwap > BigDecimal.ZERO) {
                    sellFill.vwap.subtract(buyFill.vwap, MC).multiply(buyFill.baseQty, MC)
                } else {
                    BigDecimal.ZERO
                }

                val grossBps = if (buyFill.vwap > BigDecimal.ZERO) {
                    sellFill.vwap.subtract(buyFill.vwap, MC)
                        .divide(buyFill.vwap, MC)
                        .multiply(BPS_DIVISOR, MC)
                        .toDouble()
                } else {
                    0.0
                }

                val sellNotional = sellFill.vwap.multiply(buyFill.baseQty, MC)
                val takerFeesUsd = notional.add(sellNotional, MC).multiply(feeRate, MC)
                val slippageUsd = notional.add(sellNotional, MC).multiply(slipRate, MC)
                val rebalanceUsd = notional.multiply(rebRate, MC)
                val worstCaseNet = grossProfitUsd
                    .subtract(takerFeesUsd, MC)
                    .subtract(slippageUsd, MC)
                    .subtract(rebalanceUsd, MC)
                val netBps = if (notional > BigDecimal.ZERO) {
                    worstCaseNet.divide(notional, MC).multiply(BPS_DIVISOR, MC).toDouble()
                } else {
                    0.0
                }

                if (grossBps < minSpreadBps) {
                    reasons += "Gross spread ${"%.1f".format(grossBps)} bps < min ${"%.1f".format(minSpreadBps)} bps"
                }
                if (worstCaseNet.toDouble() < minNetUsd) {
                    reasons += "Worst-case net $${worstCaseNet.setScale(2, RoundingMode.HALF_UP)} < min $${"%.2f".format(minNetUsd)}"
                }

                opportunities += ArbitrageOpportunity(
                    id = "${symbol}_${buyBook.venue}_${sellBook.venue}",
                    symbol = symbol,
                    buyVenue = buyBook.venue,
                    sellVenue = sellBook.venue,
                    buyVwap = buyFill.vwap.setScale(4, RoundingMode.HALF_UP),
                    sellVwap = sellFill.vwap.setScale(4, RoundingMode.HALF_UP),
                    baseQuantity = buyFill.baseQty.setScale(6, RoundingMode.HALF_UP),
                    notionalUsd = notional.setScale(2, RoundingMode.HALF_UP),
                    grossSpreadBps = grossBps,
                    takerFeesUsd = takerFeesUsd.setScale(2, RoundingMode.HALF_UP),
                    slippageBufferUsd = slippageUsd.setScale(2, RoundingMode.HALF_UP),
                    rebalanceReserveUsd = rebalanceUsd.setScale(2, RoundingMode.HALF_UP),
                    worstCaseNetUsd = worstCaseNet.setScale(2, RoundingMode.HALF_UP),
                    worstCaseNetBps = netBps,
                    qualified = reasons.isEmpty(),
                    rejectionReasons = reasons,
                    isSynthetic = buyBook.isSynthetic || sellBook.isSynthetic,
                    scannedAtMs = now
                )
            }
        }
        return opportunities.sortedByDescending { it.worstCaseNetUsd }
    }

    fun buildSyntheticDemoBooks(symbol: String, referencePrice: Double, seed: Long = System.currentTimeMillis() / 5000L): List<OrderBookSnapshot> {
        val rng = Random(seed + symbol.hashCode())
        val base = referencePrice.coerceAtLeast(0.01)
        val venues = listOf("Kraken", "Coinbase", "Bitstamp", "Crypto.com", "NDAX")
        val now = System.currentTimeMillis()

        return venues.mapIndexed { idx, venue ->
            val driftBps = when (idx) {
                0 -> -28.0 + rng.nextDouble(-6.0, 4.0)
                1 -> 44.0 + rng.nextDouble(-4.0, 12.0)
                2 -> -10.0 + rng.nextDouble(-8.0, 8.0)
                3 -> 22.0 + rng.nextDouble(-5.0, 10.0)
                else -> rng.nextDouble(-12.0, 12.0)
            }
            val mid = base * (1.0 + driftBps / 10000.0)
            val bids = (0..4).map { lvl ->
                val px = BigDecimal.valueOf(mid * (1.0 - (0.0004 * (lvl + 1))))
                val qty = BigDecimal.valueOf((8000.0 * (lvl + 1)) / mid)
                OrderBookLevel(px, qty)
            }
            val asks = (0..4).map { lvl ->
                val px = BigDecimal.valueOf(mid * (1.0 + (0.0004 * (lvl + 1))))
                val qty = BigDecimal.valueOf((8000.0 * (lvl + 1)) / mid)
                OrderBookLevel(px, qty)
            }
            OrderBookSnapshot(
                venue = venue,
                pair = "$symbol/USD",
                timestampMs = now,
                bids = bids,
                asks = asks,
                isSynthetic = true
            )
        }
    }
}

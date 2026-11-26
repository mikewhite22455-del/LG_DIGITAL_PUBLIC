package com.example.domain.engine

import com.example.domain.models.ArbitrageOpportunity
import com.example.domain.models.OrderBookLevel
import com.example.domain.models.OrderBookSnapshot
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID

object ArbitrageEngine {

    private val MC = MathContext(12, RoundingMode.HALF_UP)

    fun buildSyntheticDemoBooks(symbol: String, referencePriceUsd: Double): List<OrderBookSnapshot> {
        val now = System.currentTimeMillis()
        val ref = referencePriceUsd.coerceAtLeast(0.01)
        val venues = listOf(
            "Kraken" to -0.0028,
            "Coinbase" to 0.0034,
            "KuCoin" to -0.0012,
            "Bitstamp" to 0.0021
        )
        return venues.map { (venue, offsetFrac) ->
            val mid = ref * (1.0 + offsetFrac)
            val bids = (0 until 5).map { lvl ->
                val px = BigDecimal.valueOf(mid * (1.0 - 0.0003 * (lvl + 1)))
                    .setScale(if (ref >= 100.0) 2 else 4, RoundingMode.HALF_UP)
                val qty = BigDecimal.valueOf((2500.0 * (lvl + 1)) / mid)
                    .setScale(6, RoundingMode.HALF_UP)
                OrderBookLevel(px, qty)
            }
            val asks = (0 until 5).map { lvl ->
                val px = BigDecimal.valueOf(mid * (1.0 + 0.0003 * (lvl + 1)))
                    .setScale(if (ref >= 100.0) 2 else 4, RoundingMode.HALF_UP)
                val qty = BigDecimal.valueOf((2500.0 * (lvl + 1)) / mid)
                    .setScale(6, RoundingMode.HALF_UP)
                OrderBookLevel(px, qty)
            }
            OrderBookSnapshot(
                venue = venue,
                pair = "${symbol.uppercase()}/USD",
                timestampMs = now,
                bids = bids,
                asks = asks,
                isSynthetic = true
            )
        }
    }

    fun scanBooks(
        symbol: String,
        books: List<OrderBookSnapshot>,
        tradeNotionalUsd: Double,
        venueUsdBalances: Map<String, Double>,
        venueCoinBalances: Map<String, Double>,
        minSpreadBps: Double
    ): List<ArbitrageOpportunity> {
        val now = System.currentTimeMillis()
        val notional = BigDecimal.valueOf(tradeNotionalUsd.coerceAtLeast(10.0))
        val out = mutableListOf<ArbitrageOpportunity>()

        for (buyBook in books) {
            for (sellBook in books) {
                if (buyBook.venue == sellBook.venue) continue
                val buyVwap = computeFillVwap(buyBook.asks, notional) ?: continue
                val baseQty = notional.divide(buyVwap, 8, RoundingMode.HALF_UP)
                val sellVwap = computeSellVwap(sellBook.bids, baseQty) ?: continue

                if (buyVwap <= BigDecimal.ZERO) continue
                val grossSpreadFrac = sellVwap.subtract(buyVwap, MC)
                    .divide(buyVwap, MC)
                    .toDouble()
                val grossSpreadBps = grossSpreadFrac * 10_000.0
                if (grossSpreadBps <= 0.0) continue

                val grossUsd = sellVwap.subtract(buyVwap, MC).multiply(baseQty, MC)
                val takerFeeRate = BigDecimal("0.0020") // 10 bps each leg
                val slippageRate = BigDecimal("0.0006") // 6 bps conservative slippage
                val rebalanceRate = BigDecimal("0.0004") // 4 bps transfer/rebalance reserve

                val takerFeesUsd = notional.multiply(takerFeeRate, MC).setScale(2, RoundingMode.HALF_UP)
                val slippageUsd = notional.multiply(slippageRate, MC).setScale(2, RoundingMode.HALF_UP)
                val rebalanceUsd = notional.multiply(rebalanceRate, MC).setScale(2, RoundingMode.HALF_UP)

                val netUsd = grossUsd
                    .subtract(takerFeesUsd, MC)
                    .subtract(slippageUsd, MC)
                    .subtract(rebalanceUsd, MC)
                    .setScale(2, RoundingMode.HALF_UP)
                val netBps = netUsd.divide(notional, MC).toDouble() * 10_000.0

                val rejections = mutableListOf<String>()
                if (grossSpreadBps < minSpreadBps) {
                    rejections += "Gross spread ${"%.1f".format(grossSpreadBps)} bps < min ${"%.1f".format(minSpreadBps)} bps"
                }
                if (netUsd <= BigDecimal.ZERO) {
                    rejections += "Net PnL after taker fees + slippage is negative"
                }
                val availUsd = venueUsdBalances[buyBook.venue] ?: 0.0
                if (availUsd < tradeNotionalUsd) {
                    rejections += "Low USD on ${buyBook.venue} (${"%.0f".format(availUsd)} < ${"%.0f".format(tradeNotionalUsd)})"
                }
                val coinKey = "${sellBook.venue}_${symbol.uppercase()}"
                val availCoin = venueCoinBalances[coinKey] ?: 0.0
                if (availCoin < baseQty.toDouble() && venueCoinBalances.isNotEmpty()) {
                    rejections += "Insufficient ${symbol.uppercase()} inventory on ${sellBook.venue}"
                }

                out += ArbitrageOpportunity(
                    id = "${buyBook.venue}_${sellBook.venue}_${symbol}_${UUID.randomUUID().toString().take(6)}",
                    symbol = symbol.uppercase(),
                    buyVenue = buyBook.venue,
                    sellVenue = sellBook.venue,
                    buyVwap = buyVwap.setScale(if (buyVwap >= BigDecimal.TEN) 2 else 4, RoundingMode.HALF_UP),
                    sellVwap = sellVwap.setScale(if (sellVwap >= BigDecimal.TEN) 2 else 4, RoundingMode.HALF_UP),
                    baseQuantity = baseQty.setScale(6, RoundingMode.HALF_UP),
                    notionalUsd = notional.setScale(2, RoundingMode.HALF_UP),
                    grossSpreadBps = grossSpreadBps,
                    takerFeesUsd = takerFeesUsd,
                    slippageBufferUsd = slippageUsd,
                    rebalanceReserveUsd = rebalanceUsd,
                    worstCaseNetUsd = netUsd,
                    worstCaseNetBps = netBps,
                    qualified = rejections.isEmpty(),
                    rejectionReasons = rejections,
                    isSynthetic = buyBook.isSynthetic || sellBook.isSynthetic,
                    scannedAtMs = now
                )
            }
        }

        return out.sortedByDescending { it.worstCaseNetBps }
    }

    private fun computeFillVwap(asks: List<OrderBookLevel>, targetUsd: BigDecimal): BigDecimal? {
        if (asks.isEmpty()) return null
        var remainingUsd = targetUsd
        var totalBase = BigDecimal.ZERO
        for (lvl in asks.sortedBy { it.price }) {
            if (lvl.price <= BigDecimal.ZERO || lvl.quantity <= BigDecimal.ZERO) continue
            val levelUsd = lvl.price.multiply(lvl.quantity, MC)
            if (levelUsd >= remainingUsd) {
                totalBase = totalBase.add(remainingUsd.divide(lvl.price, MC), MC)
                remainingUsd = BigDecimal.ZERO
                break
            } else {
                totalBase = totalBase.add(lvl.quantity, MC)
                remainingUsd = remainingUsd.subtract(levelUsd, MC)
            }
        }
        if (totalBase <= BigDecimal.ZERO) return null
        val filledUsd = targetUsd.subtract(remainingUsd, MC)
        return filledUsd.divide(totalBase, MC)
    }

    private fun computeSellVwap(bids: List<OrderBookLevel>, targetQty: BigDecimal): BigDecimal? {
        if (bids.isEmpty() || targetQty <= BigDecimal.ZERO) return null
        var remainingQty = targetQty
        var totalUsd = BigDecimal.ZERO
        for (lvl in bids.sortedByDescending { it.price }) {
            if (lvl.price <= BigDecimal.ZERO || lvl.quantity <= BigDecimal.ZERO) continue
            if (lvl.quantity >= remainingQty) {
                totalUsd = totalUsd.add(remainingQty.multiply(lvl.price, MC), MC)
                remainingQty = BigDecimal.ZERO
                break
            } else {
                totalUsd = totalUsd.add(lvl.quantity.multiply(lvl.price, MC), MC)
                remainingQty = remainingQty.subtract(lvl.quantity, MC)
            }
        }
        val filledQty = targetQty.subtract(remainingQty, MC)
        if (filledQty <= BigDecimal.ZERO) return null
        return totalUsd.divide(filledQty, MC)
    }
}

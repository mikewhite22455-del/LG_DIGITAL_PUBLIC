package com.example.core

import com.example.domain.models.BaseFiatCurrency
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {

    fun formatUsd(value: Double): String {
        if (!value.isFinite()) return "$0.00"
        val sign = if (value < 0) "-" else ""
        val absVal = abs(value)
        return when {
            absVal >= 1000.0 -> {
                val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                "$sign$${nf.format(absVal)}"
            }
            absVal >= 1.0 -> "$sign$${"%.2f".format(Locale.US, absVal)}"
            absVal >= 0.01 -> "$sign$${"%.4f".format(Locale.US, absVal)}"
            absVal == 0.0 -> "$0.00"
            else -> "$sign$${"%.6f".format(Locale.US, absVal)}"
        }
    }

    fun formatFiat(valueUsd: Double, currency: BaseFiatCurrency, usdToCadRate: Double): String {
        val converted = when (currency) {
            BaseFiatCurrency.USD -> valueUsd
            BaseFiatCurrency.CAD -> valueUsd * usdToCadRate.coerceAtLeast(0.5)
        }
        val sign = if (converted < 0) "-" else ""
        val absVal = abs(converted)
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = if (absVal >= 1.0 || absVal == 0.0) 2 else 4
        }
        return "$sign${currency.symbolPrefix}${nf.format(absVal)}"
    }

    fun formatCompactUsd(value: Double): String {
        if (!value.isFinite()) return "$0"
        val absVal = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000_000.0 -> "$sign$${"%.2f".format(Locale.US, absVal / 1_000_000_000_000.0)}T"
            absVal >= 1_000_000_000.0 -> "$sign$${"%.2f".format(Locale.US, absVal / 1_000_000_000.0)}B"
            absVal >= 1_000_000.0 -> "$sign$${"%.2f".format(Locale.US, absVal / 1_000_000.0)}M"
            absVal >= 1_000.0 -> "$sign$${"%.1f".format(Locale.US, absVal / 1_000.0)}K"
            else -> "$sign$${"%.0f".format(Locale.US, absVal)}"
        }
    }

    fun formatPct(value: Double): String {
        if (!value.isFinite()) return "0.00%"
        val prefix = if (value > 0) "+" else ""
        return "$prefix${"%.2f".format(Locale.US, value)}%"
    }

    fun formatBps(value: Double): String {
        if (!value.isFinite()) return "0.0 bps"
        val prefix = if (value > 0) "+" else ""
        return "$prefix${"%.1f".format(Locale.US, value)} bps"
    }

    fun formatQty(value: Double): String {
        if (!value.isFinite()) return "0"
        val absVal = abs(value)
        return when {
            absVal >= 1000.0 -> "%.2f".format(Locale.US, value)
            absVal >= 1.0 -> "%.4f".format(Locale.US, value)
            absVal == 0.0 -> "0"
            else -> "%.6f".format(Locale.US, value).trimEnd('0').trimEnd('.')
        }
    }

    fun formatTimeShort(timestampMs: Long): String {
        if (timestampMs <= 0L) return "--:--"
        val fmt = SimpleDateFormat("MMM d · HH:mm", Locale.US)
        return fmt.format(Date(timestampMs))
    }
}

package com.example.core

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object Formatters {
    fun formatUsd(value: Double): String {
        val absVal = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            absVal == 0.0 -> "$0.00"
            absVal < 0.001 -> "$sign$" + String.format(Locale.US, "%.7f", absVal)
            absVal < 1.0 -> "$sign$" + String.format(Locale.US, "%.4f", absVal)
            absVal >= 1000.0 -> {
                val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                "$sign$${nf.format(absVal)}"
            }
            else -> "$sign$" + String.format(Locale.US, "%.2f", absVal)
        }
    }

    fun formatCompactUsd(value: Double): String {
        val absVal = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000_000.0 -> "$sign$" + String.format(Locale.US, "%.2fT", absVal / 1_000_000_000_000.0)
            absVal >= 1_000_000_000.0 -> "$sign$" + String.format(Locale.US, "%.2fB", absVal / 1_000_000_000.0)
            absVal >= 1_000_000.0 -> "$sign$" + String.format(Locale.US, "%.2fM", absVal / 1_000_000.0)
            absVal >= 1_000.0 -> "$sign$" + String.format(Locale.US, "%.1fK", absVal / 1_000.0)
            else -> formatUsd(value)
        }
    }

    fun formatPct(value: Double, includeSign: Boolean = true): String {
        val sign = if (includeSign && value > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", value)}%"
    }

    fun formatQty(qty: Double): String {
        return when {
            abs(qty) >= 100.0 -> String.format(Locale.US, "%.2f", qty)
            abs(qty) >= 1.0 -> String.format(Locale.US, "%.4f", qty)
            else -> String.format(Locale.US, "%.6f", qty)
        }
    }

    fun formatBps(bps: Double): String {
        val sign = if (bps > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.1f", bps)} bps"
    }

    fun formatTimeShort(epochMillis: Long): String {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        return sdf.format(Date(epochMillis))
    }

    fun formatUtcClock(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(epochMillis))
    }
}

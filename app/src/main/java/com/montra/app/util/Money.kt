package com.montra.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** INR helpers. All amounts are Long paise internally. */
object Money {
    private fun inr(): NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    /** ₹1,23,456.00 using Indian digit grouping. */
    fun format(paise: Long): String = inr().format(BigDecimal(paise).movePointLeft(2))

    /** Whole rupees, e.g. ₹1,23,456. */
    fun formatWhole(paise: Long): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }.format(BigDecimal(paise).movePointLeft(2).setScale(0, RoundingMode.HALF_UP))

    /** Compact form: ₹950, ₹12.5K, ₹1.2L, ₹3.4Cr. */
    fun compact(paise: Long): String {
        val neg = paise < 0
        val rupees = Math.abs(paise) / 100.0
        val body = when {
            rupees >= 1_00_00_000 -> oneDecimal(rupees / 1_00_00_000) + "Cr"
            rupees >= 1_00_000 -> oneDecimal(rupees / 1_00_000) + "L"
            rupees >= 1_000 -> oneDecimal(rupees / 1_000) + "K"
            else -> String.format(Locale.ENGLISH, "%.0f", rupees)
        }
        return (if (neg) "-" else "") + "₹" + body
    }

    /** 12.5 -> "12.5", 12.0 -> "12" (always with a "." and Latin digits, whatever the phone's language). */
    private fun oneDecimal(v: Double): String {
        val s = String.format(Locale.ENGLISH, "%.1f", v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }

    /** Parses user input like "1250", "1,250.50". Returns null when empty, invalid, or not positive. */
    fun parsePaise(input: String): Long? {
        val cleaned = input.replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() <= 0) return null
        if (value.scale() > 2) return null
        val paise = value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
        if (paise > BigDecimal(Long.MAX_VALUE / 100)) return null
        return paise.toLong()
    }

    /** For pre-filling an edit field. */
    fun toInput(paise: Long): String =
        if (paise % 100 == 0L) (paise / 100).toString()
        else String.format(Locale.ROOT, "%d.%02d", paise / 100, paise % 100)
}

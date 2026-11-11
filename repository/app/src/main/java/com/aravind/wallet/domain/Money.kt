package com.aravind.wallet.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.text.NumberFormat
import java.util.Locale

/** Amounts are stored as integer paise; aggregates also remain exact beyond Long's range. */
object Money {
    fun parse(text: String, allowNegative: Boolean = false): Long {
        val value = text.trim()
        require(value.length <= 22 && value.matches(Regex("-?(?:[0-9]+(?:\\.[0-9]{0,2})?|\\.[0-9]{1,2})"))) {
            "Enter an amount with up to two decimal places (for example, 250.50)."
        }
        val minor = try {
            BigDecimal(value).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            throw IllegalArgumentException("This amount is too large.")
        }
        require(allowNegative || minor > 0) { "Enter an amount greater than zero." }
        return minor
    }

    /** Rounded calendar label only; accounting retains exact values. */
    fun compactNet(minor: BigInteger): String {
        if (minor.signum() == 0) return "₹0"
        val amount = BigDecimal(minor.abs(), 2).round(MathContext(3))
        val units = listOf(12 to "T", 9 to "B", 6 to "M", 3 to "K", 0 to "")
        val text = if (amount >= BigDecimal("1E15")) amount.stripTrailingZeros().toString()
        else {
            val (power, suffix) = units.first { amount >= BigDecimal.TEN.pow(it.first) || it.first == 0 }
            amount.movePointLeft(power).stripTrailingZeros().toPlainString() + suffix
        }
        return (if (minor.signum() > 0) "+₹" else "−₹") + text
    }

    fun input(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()

    fun format(minor: Long): String = format(BigInteger.valueOf(minor))

    fun format(minor: BigInteger): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal(minor, 2))
}

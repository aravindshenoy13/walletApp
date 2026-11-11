package com.aravind.wallet.domain

import java.math.BigDecimal
import java.math.BigInteger
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

    fun input(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()

    fun format(minor: Long): String = format(BigInteger.valueOf(minor))

    fun format(minor: BigInteger): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(BigDecimal(minor, 2))
}

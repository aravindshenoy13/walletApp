package com.aravind.wallet.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Decimal arithmetic; only the final amount is rounded to paise. */
object AmountCalculator {
    fun evaluate(input: String): String {
        require(input.length <= 160) { "Calculation is too long." }
        val text = input.filterNot { it.isWhitespace() }.replace('×', '*').replace('÷', '/').replace('−', '-')
        var index = 0
        fun number(): BigDecimal {
            var negative = false
            if (index < text.length && text[index] in "+-") negative = text[index++] == '-'
            val start = index
            while (index < text.length && (text[index].isDigit() || text[index] == '.')) index++
            val token = text.substring(start, index)
            require(token.matches(Regex("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)"))) { "Enter a valid calculation." }
            return BigDecimal(token).let { if (negative) it.negate() else it }
        }
        fun product(): BigDecimal {
            var result = number()
            while (index < text.length && text[index] in "*/") {
                val operator = text[index++]
                val value = number()
                result = if (operator == '*') result.multiply(value) else {
                    require(value.signum() != 0) { "Cannot divide by zero." }
                    result.divide(value, MathContext.DECIMAL128)
                }
            }
            return result
        }
        var result = product()
        while (index < text.length && text[index] in "+-") {
            val operator = text[index++]
            val value = product()
            result = if (operator == '+') result.add(value) else result.subtract(value)
        }
        require(index == text.length) { "Enter a valid calculation." }
        return result.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }
}

package com.aravind.wallet.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountCalculatorTest {
    @Test fun precedenceAndDecimalPrecision() {
        assertEquals("800.00", AmountCalculator.evaluate("300 + 500"))
        assertEquals("14.00", AmountCalculator.evaluate("2+3×4"))
        assertEquals("0.30", AmountCalculator.evaluate("0.1+0.2"))
        assertEquals("0.67", AmountCalculator.evaluate("2÷3"))
        assertEquals("3.00", AmountCalculator.evaluate("5+-2"))
    }
    @Test fun invalidCalculationsAreRejected() {
        listOf("", "2+", "1..2", "2/0", "2abc", "2 3+").forEach { expression ->
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { AmountCalculator.evaluate(expression) }
        }
    }
}

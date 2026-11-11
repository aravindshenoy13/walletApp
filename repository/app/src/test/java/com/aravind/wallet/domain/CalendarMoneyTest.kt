package com.aravind.wallet.domain

import java.math.BigInteger
import org.junit.Assert.*
import org.junit.Test

class CalendarMoneyTest {
    @Test fun compactNetPreservesSignAndSmallNonzeroAmounts() {
        assertEquals("₹0", Money.compactNet(BigInteger.ZERO))
        assertEquals("+₹0.01", Money.compactNet(BigInteger.ONE))
        assertEquals("−₹0.01", Money.compactNet(BigInteger.valueOf(-1)))
        assertEquals("+₹1.2K", Money.compactNet(BigInteger.valueOf(120000)))
        assertEquals("−₹25K", Money.compactNet(BigInteger.valueOf(-2500000)))
    }
    @Test fun lakhBoundariesKeepRoundingAndSigns() {
        assertEquals("+₹99.9K", Money.compactNet(BigInteger("9994900")))
        assertEquals("+₹1L", Money.compactNet(BigInteger("9995000")))
        assertEquals("+₹1L", Money.compactNet(BigInteger("10000000")))
        assertEquals("−₹1.23L", Money.compactNet(BigInteger("-12345678")))
        assertEquals("+₹9.99L", Money.compactNet(BigInteger("99949900")))
        assertEquals("+₹1M", Money.compactNet(BigInteger("99950000")))
        assertEquals("−₹1M", Money.compactNet(BigInteger("-100000000")))
    }
    @Test fun roundingPromotesUnitsAndLargeTotalsRemainShort() {
        assertEquals("+₹1K", Money.compactNet(BigInteger.valueOf(99999)))
        assertEquals("+₹1M", Money.compactNet(BigInteger.valueOf(99999999)))
        val huge = BigInteger.TEN.pow(30)
        assertTrue(Money.compactNet(huge).length <= 10)
        assertEquals("+₹1E+28", Money.compactNet(huge))
    }
}

package com.aravind.wallet.domain

import com.aravind.wallet.data.Account
import com.aravind.wallet.data.Converters
import com.aravind.wallet.data.WalletRecord
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class LedgerTest {
    private val day = LocalDate.of(2026, 11, 11)
    private fun record(type: RecordType, amount: Long, account: Long = 1, destination: Long? = null,
                       at: LocalDateTime = day.atTime(13, 30)) = WalletRecord(
        type = type, amountMinor = amount, accountId = account, destinationAccountId = destination,
        categoryId = if (type == RecordType.TRANSFER) null else "FOOD",
        occurredAt = at, description = "Test",
    )

    @Test fun decimalParsingIsExact() {
        assertEquals(30L, Money.parse("0.10") + Money.parse("0.20"))
        assertEquals(50L, Money.parse(".50"))
        assertEquals(100L, Money.parse(" 1. "))
        assertEquals(-1L, Money.parse("-0.01", allowNegative = true))
        assertEquals(Long.MAX_VALUE, Money.parse("92233720368547758.07"))
        assertEquals(Long.MIN_VALUE, Money.parse("-92233720368547758.08", allowNegative = true))
        assertEquals("-0.01", Money.input(-1))
    }

    @Test fun invalidAmountsCannotBeRoundedOrOverflowed() {
        listOf("", "0", "-1", "1.001", "1,000", "1e3", "NaN", "92233720368547758.08").forEach {
            assertThrows("Invalid input: $it", IllegalArgumentException::class.java) { Money.parse(it) }
        }
    }

    @Test fun transferAffectsBothAccountsWithoutChangingTotals() {
        val ledger = Ledger(listOf(Account(1, "One", 10_000), Account(2, "Two", -5_000)), listOf(
            record(RecordType.INCOME, 1_000), record(RecordType.EXPENSE, 20_000),
            record(RecordType.TRANSFER, 2_500, destination = 2),
        ))
        assertEquals(BigInteger.valueOf(-11_500), ledger.balances[1])
        assertEquals(BigInteger.valueOf(-2_500), ledger.balances[2])
        assertEquals(BigInteger.valueOf(-14_000), ledger.totalBalance)
        assertEquals(DailyTotals(BigInteger.valueOf(1_000), BigInteger.valueOf(20_000)), ledger.totals(day))
        assertEquals(BigInteger.valueOf(-19_000), ledger.totals(day).net)
    }

    @Test fun dailyTotalsUseTheOccurrenceDayIncludingMidnight() {
        val ledger = Ledger(listOf(Account(1, "One", 0)), listOf(
            record(RecordType.INCOME, 100, at = day.atStartOfDay()),
            record(RecordType.INCOME, 200, at = day.atTime(23, 59, 59)),
            record(RecordType.EXPENSE, 30, at = day.plusDays(1).atStartOfDay()),
            record(RecordType.INCOME, 999, at = day.minusDays(1).atTime(23, 59, 59)),
        ))
        assertEquals(DailyTotals(BigInteger.valueOf(300), BigInteger.ZERO), ledger.totals(day))
        assertEquals(DailyTotals(BigInteger.ZERO, BigInteger.valueOf(30)), ledger.totals(day.plusDays(1)))
        assertEquals(DailyTotals(BigInteger.ZERO, BigInteger.ZERO), ledger.totals(day.plusDays(2)))
    }

    @Test fun aggregatesCannotOverflowEvenWithExtremeStoredAmounts() {
        val ledger = Ledger(listOf(Account(1, "One", Long.MAX_VALUE)), listOf(
            record(RecordType.INCOME, Long.MAX_VALUE), record(RecordType.INCOME, Long.MAX_VALUE),
        ))
        val max = BigInteger.valueOf(Long.MAX_VALUE)
        assertEquals(max * BigInteger.valueOf(3), ledger.balances[1])
        assertEquals(max * BigInteger.TWO, ledger.totals(day).income)
        assertFalse(Money.format(ledger.totalBalance).contains("E+"))
    }

    @Test fun timestampsRoundTripWithoutTimeZoneConversion() {
        val converters = Converters()
        val time = LocalDateTime.of(2026, 11, 11, 0, 1, 2)
        assertEquals("2026-11-11T00:01:02", converters.timeToText(time))
        assertEquals(time, converters.textToTime(converters.timeToText(time)))
    }
}

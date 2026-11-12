package com.aravind.wallet.domain

import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.data.WalletCategory
import java.math.BigInteger
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AnalysisTest {
    private val day = LocalDate.of(2026, 11, 11)
    private val accounts = listOf(Account(1,"Cash",1000), Account(2,"Bank",-500))
    private val categories = listOf(WalletCategory("FOOD","Food","utensils","Peach",true,true))
    private fun record(id: Long, type: RecordType, amount: Long, account: Long = 1, date: LocalDate = day, description: String = "Lunch") =
        WalletRecord(id,type,amount,account,if(type == RecordType.TRANSFER) 2 else null,if(type == RecordType.TRANSFER) null else "FOOD",date.atTime(12,0),description,"")

    @Test fun uncategorisedRecordsRemainInBreakdownAndHistory() {
        val entry = record(1, RecordType.EXPENSE, 250).copy(categoryId = null)
        val ledger = Ledger(accounts, listOf(entry), emptyList())
        assertEquals(BigInteger.valueOf(250), ledger.analyze(day, day).categories.getValue("").expense)
        assertEquals(listOf(entry), ledger.categoryRecords("", day, day, null, BreakdownMode.NET))
    }

    @Test fun fullMonthIncludesFutureDatesAndRollsAtYearAndLeapMonthBoundaries() {
        val december = LocalDate.of(2026, 12, 31)
        val january = december.plusDays(1)
        val leap = LocalDate.of(2028, 2, 29)
        val ledger = Ledger(accounts, listOf(
            record(1, RecordType.INCOME, 100, date = december),
            record(2, RecordType.EXPENSE, 25, 2, date = january),
            record(3, RecordType.INCOME, 200, date = january.withDayOfMonth(31)),
            record(4, RecordType.TRANSFER, 99999, date = january),
            record(5, RecordType.EXPENSE, 75, date = leap)
        ), categories)
        fun month(date: LocalDate) = ledger.analyze(date.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth())).totals
        assertEquals(BigInteger.valueOf(100), month(december).net)
        assertEquals(BigInteger.valueOf(-25), ledger.totals(january).net)
        assertEquals(BigInteger.valueOf(175), month(january).net)
        assertEquals(BigInteger.valueOf(-75), month(leap.withDayOfMonth(1)).net)
        assertEquals(BigInteger.ZERO, month(leap.plusDays(1)).net)
        assertEquals(5, ledger.records.size)
    }
    @Test fun inclusiveRangeExcludesTransfersAndCountsReimbursementsInSameCategory() {
        val ledger = Ledger(accounts,listOf(record(1,RecordType.EXPENSE,10000),record(2,RecordType.INCOME,2500),
            record(3,RecordType.INCOME,1000,2,date=day.minusDays(1)), record(4,RecordType.TRANSFER,99999),
            record(5,RecordType.INCOME,50000,date=day.plusDays(1))),categories)
        val data = ledger.analyze(day.minusDays(1),day)
        assertEquals(BigInteger.valueOf(3500),data.totals.income)
        assertEquals(BigInteger.valueOf(10000),data.totals.expense)
        assertEquals(BigInteger.valueOf(-6500),data.categories.getValue("FOOD").value(BreakdownMode.NET))
        assertEquals(2,data.daily.size)
        assertEquals(BigInteger.valueOf(-7500),data.daily.getValue(day).net)
        assertEquals(BigInteger.valueOf(2500),ledger.analyze(day,day,1).totals.income)
        assertEquals(BigInteger.ZERO,ledger.analyze(day,day,2).totals.income)
    }
    @Test fun rangeAndAccountScopeApplyToAllViews() {
        val ledger = Ledger(accounts,listOf(record(1,RecordType.EXPENSE,100),record(2,RecordType.INCOME,300,2)),categories)
        val cash = ledger.analyze(day,day,1)
        assertEquals(cash.totals,cash.daily.getValue(day))
        assertEquals(cash.totals,cash.categories.getValue("FOOD"))
        assertEquals(BigInteger.valueOf(100),cash.totals.value(BreakdownMode.EXPENSE))
        assertEquals(BigInteger.ZERO,cash.totals.value(BreakdownMode.INCOME))
        assertEquals(BigInteger.valueOf(300),ledger.analyze(day,day,2).totals.net)
    }
    @Test fun exactLargeTotalsAndEmptyHugeRangesDoNotOverflowOrAllocateDays() {
        val ledger = Ledger(accounts,listOf(record(1,RecordType.INCOME,Long.MAX_VALUE),record(2,RecordType.INCOME,Long.MAX_VALUE)),categories)
        val result = ledger.analyze(LocalDate.of(1,1,1),LocalDate.of(9999,12,31))
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO),result.totals.income)
        assertEquals(1,result.daily.size)
        assertTrue(ledger.analyze(day.minusYears(1),day.minusDays(1)).daily.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { ledger.analyze(day,day.minusDays(1)) }
    }
    @Test fun reimbursementCanCancelNetWithoutLosingIncomeOrExpenseViews() {
        val data = Ledger(accounts,listOf(record(1,RecordType.EXPENSE,100),record(2,RecordType.INCOME,100)),categories).analyze(day,day)
        val totals = data.categories.getValue("FOOD")
        assertEquals(BigInteger.ZERO,totals.value(BreakdownMode.NET))
        assertEquals(BigInteger.valueOf(100),totals.value(BreakdownMode.INCOME))
        assertEquals(BigInteger.valueOf(100),totals.value(BreakdownMode.EXPENSE))
    }
    @Test fun searchOnlyMatchesDescriptionOrAmountWithoutChangingTotals() {
        val ledger = Ledger(accounts,listOf(record(1,RecordType.EXPENSE,123456,description="Lunch café"),
            record(2,RecordType.INCOME,2500,description="Refund")),categories)
        assertEquals(listOf(1L),ledger.searchRecords("CAFÉ").map { it.id })
        assertEquals(listOf(1L),ledger.searchRecords("1,234.56").map { it.id })
        assertEquals(listOf(2L),ledger.searchRecords("25.00").map { it.id })
        assertTrue(ledger.searchRecords("Food").isEmpty())
        assertTrue(ledger.searchRecords("Cash").isEmpty())
        assertTrue(ledger.searchRecords("no match").isEmpty())
        assertEquals(2,ledger.searchRecords("  ").size)
        assertEquals(BigInteger.valueOf(2500),ledger.totals(day).income)
    }
    @Test fun categoryHistoryMatchesRangeAccountAndBreakdown() {
        val other = WalletCategory("OTHER", "Other", "tag", "Peach", true, false)
        val ledger = Ledger(accounts, listOf(
            record(1, RecordType.EXPENSE, 100, date = day.minusDays(1)),
            record(2, RecordType.INCOME, 50),
            record(3, RecordType.EXPENSE, 80, 2),
            record(4, RecordType.INCOME, 20, date = day.plusDays(1)),
            record(5, RecordType.TRANSFER, 10),
            record(6, RecordType.EXPENSE, 30).copy(categoryId = "OTHER")
        ), categories + other)
        fun ids(account: Long?, mode: BreakdownMode) = ledger.categoryRecords("FOOD", day.minusDays(1), day, account, mode).map { it.id }
        assertEquals(listOf(1L, 2L, 3L), ids(null, BreakdownMode.NET))
        assertEquals(listOf(1L, 2L), ids(1, BreakdownMode.NET))
        assertEquals(listOf(2L), ids(null, BreakdownMode.INCOME))
        assertEquals(listOf(1L, 3L), ids(null, BreakdownMode.EXPENSE))
        assertTrue(ledger.categoryRecords("FOOD", day.plusDays(2), day.plusDays(3), null, BreakdownMode.NET).isEmpty())
    }
    @Test fun accountHistoryIncludesBothTransferDirectionsButNoOtherAccountsRecords() {
        val transfer = record(1,RecordType.TRANSFER,500)
        val expense = record(2,RecordType.EXPENSE,100)
        val ledger = Ledger(accounts,listOf(transfer,expense),categories)
        assertEquals(listOf(transfer,expense),ledger.accountRecords(1))
        assertEquals(listOf(transfer),ledger.accountRecords(2))
        assertTrue(ledger.accountRecords(99).isEmpty())
        assertEquals(BigInteger.valueOf(400),ledger.balances[1])
        assertEquals(BigInteger.ZERO,ledger.balances[2])
    }
}

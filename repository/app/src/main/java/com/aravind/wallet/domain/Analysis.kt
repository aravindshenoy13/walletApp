package com.aravind.wallet.domain

import com.aravind.wallet.data.WalletRecord
import java.math.BigInteger
import java.time.LocalDate

enum class BreakdownMode { NET, INCOME, EXPENSE }
data class PeriodAnalysis(val totals: DailyTotals, val daily: Map<LocalDate, DailyTotals>, val categories: Map<String, DailyTotals>)

fun Ledger.searchRecords(query: String): List<WalletRecord> {
    val text = query.trim()
    if (text.isEmpty()) return records
    val amount = text.replace(",", "")
    return records.filter { it.description.contains(text, ignoreCase = true) ||
        (text.any(Char::isDigit) && Money.input(it.amountMinor).contains(amount)) ||
        Money.format(it.amountMinor).contains(text, ignoreCase = true) }
}

fun Ledger.accountRecords(id: Long): List<WalletRecord> = records.filter { it.accountId == id || it.destinationAccountId == id }

fun Ledger.categoryRecords(categoryId: String, start: LocalDate, end: LocalDate, accountId: Long?, mode: BreakdownMode): List<WalletRecord> {
    require(!end.isBefore(start))
    return records.filter { record ->
        val date = record.occurredAt.toLocalDate()
        record.categoryId == categoryId && record.type != RecordType.TRANSFER && date >= start && date <= end &&
            (accountId == null || record.accountId == accountId) && when (mode) {
                BreakdownMode.NET -> true
                BreakdownMode.INCOME -> record.type == RecordType.INCOME
                BreakdownMode.EXPENSE -> record.type == RecordType.EXPENSE
            }
    }
}

fun Ledger.analyze(start: LocalDate, end: LocalDate, accountId: Long? = null): PeriodAnalysis {
    require(!end.isBefore(start)) { "End date must be on or after start date." }
    val daily = sortedMapOf<LocalDate, DailyTotals>()
    val categories = mutableMapOf<String, DailyTotals>()
    var totals = DailyTotals(BigInteger.ZERO, BigInteger.ZERO)
    fun DailyTotals.add(record: WalletRecord): DailyTotals = if (record.type == RecordType.INCOME)
        copy(income = income + BigInteger.valueOf(record.amountMinor))
        else copy(expense = expense + BigInteger.valueOf(record.amountMinor))
    records.forEach { record ->
        val date = record.occurredAt.toLocalDate()
        if (record.type == RecordType.TRANSFER || date < start || date > end ||
            (accountId != null && record.accountId != accountId)) return@forEach
        totals = totals.add(record)
        val zero = DailyTotals(BigInteger.ZERO, BigInteger.ZERO)
        daily[date] = (daily[date] ?: zero).add(record)
        record.categoryId?.let { categories[it] = (categories[it] ?: zero).add(record) }
    }
    return PeriodAnalysis(totals, daily, categories)
}

fun DailyTotals.value(mode: BreakdownMode): BigInteger = when (mode) {
    BreakdownMode.NET -> net
    BreakdownMode.INCOME -> income
    BreakdownMode.EXPENSE -> expense
}

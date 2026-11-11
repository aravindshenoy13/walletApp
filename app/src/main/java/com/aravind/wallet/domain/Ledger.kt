package com.aravind.wallet.domain

import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletRecord
import java.math.BigInteger
import java.time.LocalDate

enum class RecordType { EXPENSE, INCOME, TRANSFER }

/** One shared, fixed category set for both directions. IDs remain stable on disk. */
enum class Category(val label: String) {
    FOOD("Food"), GROCERIES("Groceries"), RENT("Rent"), TRANSPORT("Transport"),
    UTILITIES("Utilities"), HEALTH("Health"), SHOPPING("Shopping"), ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"), SALARY("Salary"), GIFTS("Gifts"), OTHER("Other");
    override fun toString() = label
}

data class DailyTotals(val income: BigInteger, val expense: BigInteger) {
    val net: BigInteger get() = income - expense
}

data class Ledger(val accounts: List<Account>, val records: List<WalletRecord>) {
    val accountsById = accounts.associateBy { it.id }
    val balances: Map<Long, BigInteger> by lazy {
        val result = accounts.associate { it.id to BigInteger.valueOf(it.openingBalanceMinor) }.toMutableMap()
        records.forEach { record ->
            val amount = BigInteger.valueOf(record.amountMinor)
            val effect = if (record.type == RecordType.INCOME) amount else -amount
            result[record.accountId] = result.getValue(record.accountId) + effect
            if (record.type == RecordType.TRANSFER) {
                val destination = requireNotNull(record.destinationAccountId)
                result[destination] = result.getValue(destination) + amount
            }
        }
        result
    }
    val totalBalance: BigInteger get() = balances.values.fold(BigInteger.ZERO, BigInteger::add)

    fun totals(date: LocalDate): DailyTotals {
        var income = BigInteger.ZERO
        var expense = BigInteger.ZERO
        records.filter { it.occurredAt.toLocalDate() == date }.forEach {
            when (it.type) {
                RecordType.INCOME -> income += BigInteger.valueOf(it.amountMinor)
                RecordType.EXPENSE -> expense += BigInteger.valueOf(it.amountMinor)
                RecordType.TRANSFER -> Unit
            }
        }
        return DailyTotals(income, expense)
    }
}

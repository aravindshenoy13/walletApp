package com.aravind.wallet.data

import androidx.room.withTransaction
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.RecordType
import kotlinx.coroutines.flow.map

class WalletRepository(private val database: WalletDatabase) {
    private val dao = database.walletDao()
    val ledger = dao.observeLedger().map(::toLedger)

    suspend fun saveAccount(account: Account): Long = database.withTransaction {
        val name = account.name.trim()
        require(name.isNotEmpty()) { "Enter an account name." }
        require(name.length <= 80) { "Use at most 80 characters for the account name." }
        val existing = dao.accounts()
        require(existing.none { it.id != account.id && it.name.equals(name, ignoreCase = true) }) {
            "An account with this name already exists."
        }
        val normalized = account.copy(name = name)
        if (account.id == 0L) dao.insertAccount(normalized) else {
            require(dao.updateAccount(normalized) == 1) { "This account no longer exists." }
            account.id
        }
    }

    suspend fun saveRecord(record: WalletRecord): Long = database.withTransaction {
        require(record.amountMinor > 0) { "Enter an amount greater than zero." }
        require(record.description.isNotBlank()) { "Enter a description or payee." }
        require(record.description.trim().length <= 120) { "Use at most 120 characters for the description." }
        require(record.note.trim().length <= 1000) { "Use at most 1,000 characters for the note." }
        require(record.occurredAt.year in 1..9999) { "Choose a valid occurrence date." }
        val accountIds = dao.accounts().map { it.id }.toSet()
        require(record.accountId in accountIds) { "Choose an existing account." }
        if (record.type == RecordType.TRANSFER) {
            require(record.destinationAccountId in accountIds) { "Choose a destination account." }
            require(record.accountId != record.destinationAccountId) { "Choose two different accounts for a transfer." }
            require(record.category == null) { "Transfers do not have a category." }
        } else {
            require(record.category != null) { "Choose a category." }
            require(record.destinationAccountId == null) { "Only transfers have a destination account." }
        }
        val normalized = record.copy(description = record.description.trim(), note = record.note.trim())
        if (record.id == 0L) dao.insertRecord(normalized) else {
            require(dao.updateRecord(normalized) == 1) { "This record no longer exists." }
            record.id
        }
    }

    suspend fun deleteRecord(id: Long) = database.withTransaction {
        require(dao.deleteRecord(id) == 1) { "This record no longer exists." }
    }

    private fun toLedger(rows: List<AccountWithRecords>) = Ledger(
        accounts = rows.map { it.account },
        records = rows.flatMap { it.records }.sortedWith(
            compareByDescending<WalletRecord> { it.occurredAt }.thenByDescending { it.id },
        ),
    )
}

package com.aravind.wallet.data

import androidx.room.withTransaction
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.RecordType
import com.aravind.wallet.domain.CategoryCatalog
import kotlinx.coroutines.flow.map

class WalletRepository(private val database: WalletDatabase) {
    private val dao = database.walletDao()
    val ledger = database.invalidationTracker.createFlow("accounts", "records", "categories", emitInitialState = true)
        .map { snapshot() }

    suspend fun snapshot(): Ledger = database.withTransaction { Ledger(dao.accounts(), dao.records(), dao.categories()) }

    suspend fun saveCategory(category: WalletCategory, isNew: Boolean = false) = database.withTransaction {
        val name = category.name.trim()
        require(name.isNotEmpty() && name.length <= 80) { "Enter a category name of 1–80 characters." }
        require(CategoryCatalog.icons.any { it.key == category.iconKey }) { "Choose an icon." }
        require(CategoryCatalog.colors.any { it.key == category.backgroundColorKey }) { "Choose a background color." }
        require(dao.categories().none { it.id != category.id && it.name.equals(name, ignoreCase = true) }) {
            "A category with this name already exists (it may be archived)."
        }
        if (isNew) {
            require(!category.isBuiltIn && !category.isArchived) { "New categories must be active custom categories." }
            dao.insertCategory(category.copy(name = name))
        } else {
            val existing = requireNotNull(dao.category(category.id)) { "This category no longer exists." }
            require(dao.updateCategory(category.copy(name = name, isBuiltIn = existing.isBuiltIn, isArchived = existing.isArchived)) == 1)
        }
    }

    suspend fun archiveCategory(id: String) = database.withTransaction {
        val category = requireNotNull(dao.category(id)) { "This category no longer exists." }
        require(!category.isArchived) { "This category is already archived." }
        require(dao.categories().count { !it.isArchived } > 1) { "Keep at least one category active." }
        dao.updateCategory(category.copy(isArchived = true))
    }

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
        require(record.description.trim().length <= 120) { "Use at most 120 characters for the description." }
        require(record.note.trim().length <= 1000) { "Use at most 1,000 characters for the note." }
        require(record.occurredAt.year in 1..9999) { "Choose a valid occurrence date." }
        val accountIds = dao.accounts().map { it.id }.toSet()
        require(record.accountId in accountIds) { "Choose an existing account." }
        if (record.type == RecordType.TRANSFER) {
            require(record.destinationAccountId in accountIds) { "Choose a destination account." }
            require(record.accountId != record.destinationAccountId) { "Choose two different accounts for a transfer." }
            require(record.categoryId == null) { "Transfers do not have a category." }
        } else {
            val category = record.categoryId?.let { dao.category(it) }
            require(category != null) { "Choose an existing category." }
            val oldCategory = if (record.id == 0L) null else dao.record(record.id)?.categoryId
            require(!category.isArchived || oldCategory == category.id) { "Choose an active category for a new assignment." }
            require(record.destinationAccountId == null) { "Only transfers have a destination account." }
        }
        val normalized = record.copy(description = record.description.trim(), note = record.note.trim())
        if (record.id == 0L) dao.insertRecord(normalized) else {
            require(dao.updateRecord(normalized) == 1) { "This record no longer exists." }
            record.id
        }
    }

    suspend fun restore(backup: Ledger) = database.withTransaction {
        // Recheck at commit time, so edits made while the picker was open cannot be overwritten.
        require(dao.accounts().isEmpty() && dao.records().isEmpty() && dao.categories().toSet() == V2CategorySeeds.rows.toSet()) {
            "Restore requires an empty wallet with untouched built-in categories."
        }
        val checked = WalletBackup.decode(WalletBackup.encode(backup))
        dao.clearCategories()
        checked.categories.forEach { dao.insertCategory(it) }
        checked.accounts.forEach { dao.insertAccount(it) }
        checked.records.forEach { dao.insertRecord(it) }
    }

    suspend fun deleteRecord(id: Long) = database.withTransaction {
        require(dao.deleteRecord(id) == 1) { "This record no longer exists." }
    }

}

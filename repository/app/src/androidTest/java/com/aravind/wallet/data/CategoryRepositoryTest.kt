package com.aravind.wallet.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.RecordType
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {
    private lateinit var database: WalletDatabase
    private lateinit var repository: WalletRepository
    @Before fun prepare() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), WalletDatabase::class.java)
            .addCallback(V2CategorySeeds.callback).build()
        repository = WalletRepository(database)
    }
    @After fun close() { database.close() }
    private fun custom(name: String) = WalletCategory(UUID.randomUUID().toString(), name, "book-open", "Sky")

    @Test fun customCategoriesAndBuiltInEditsKeepStableIdsAndOrigins() = runBlocking {
        val item = custom("  Learning  ")
        repository.saveCategory(item, true)
        assertEquals("Learning", repository.snapshot().categoriesById.getValue(item.id).name)
        val food = repository.snapshot().categoriesById.getValue("FOOD")
        repository.saveCategory(food.copy(name = "Dining", iconKey = "book-open", backgroundColorKey = "Blue", isBuiltIn = false))
        val edited = repository.snapshot().categoriesById.getValue("FOOD")
        assertTrue(edited.isBuiltIn)
        assertEquals("Dining", edited.name)
        assertEquals("Blue", edited.backgroundColorKey)
        assertEquals(13, repository.snapshot().categories.size)
    }

    @Test fun metadataOnlyEditsAndArchiveEmitToExistingObservers() = runBlocking {
        val account = repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 10_000))
        repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = 1_250, accountId = account,
            categoryId = "FOOD", occurredAt = LocalDateTime.now(), description = "Lunch"))
        val events = Channel<Ledger>(Channel.UNLIMITED)
        val observer = launch { repository.ledger.collect { events.send(it) } }
        try {
            withTimeout(10_000) { events.receive() }
            val food = repository.snapshot().categoriesById.getValue("FOOD")
            repository.saveCategory(food.copy(name = "Dining", backgroundColorKey = "Red"))
            val changed = withTimeout(10_000) {
                var value = events.receive()
                while (value.categoriesById.getValue("FOOD").name != "Dining") value = events.receive()
                value
            }
            assertEquals(BigInteger.valueOf(8_750), changed.balances[account])
            repository.archiveCategory("FOOD")
            withTimeout(10_000) {
                var value = events.receive()
                while (!value.categoriesById.getValue("FOOD").isArchived) value = events.receive()
                assertEquals("FOOD", value.records.single().categoryId)
                assertEquals(BigInteger.valueOf(8_750), value.balances[account])
            }
        } finally { observer.cancel(); events.close() }
    }

    @Test fun archivedCategoriesCanBePreservedButNotNewlyAssigned() = runBlocking {
        val account = repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 0))
        val entry = WalletRecord(type = RecordType.EXPENSE, amountMinor = 100, accountId = account,
            categoryId = "FOOD", occurredAt = LocalDateTime.now(), description = "Old", note = "Legacy")
        val id = repository.saveRecord(entry)
        repository.archiveCategory("FOOD")
        assertTrue(runCatching { repository.saveRecord(entry) }.isFailure)
        repository.saveRecord(entry.copy(id = id, amountMinor = 200, type = RecordType.INCOME))
        assertEquals("Legacy", repository.snapshot().records.single().note)
        val archived = repository.snapshot().categoriesById.getValue("FOOD")
        repository.saveCategory(archived.copy(name = "Old dining", isArchived = false))
        assertTrue(repository.snapshot().categoriesById.getValue("FOOD").isArchived)
        assertTrue(runCatching { repository.saveCategory(custom("Old DINING"), true) }.isFailure)
    }

    @Test fun lastActiveCategoryCannotBeArchivedAndInvalidAppearanceCannotBeSaved() = runBlocking {
        repository.snapshot().categories.filter { it.id != "OTHER" }.forEach { repository.archiveCategory(it.id) }
        assertTrue(runCatching { repository.archiveCategory("OTHER") }.isFailure)
        assertEquals(1, repository.snapshot().categories.count { !it.isArchived })
        listOf(custom(" "), custom("Invalid").copy(iconKey = "unknown"), custom("Invalid").copy(backgroundColorKey = "unknown"))
            .forEach { assertTrue(runCatching { repository.saveCategory(it, true) }.isFailure) }
        assertEquals(12, repository.snapshot().categories.size)
    }

    @Test fun simultaneousCategoryNamesRemainUnique() = runBlocking {
        val one = async { runCatching { repository.saveCategory(custom("Study"), true) } }
        val two = async { runCatching { repository.saveCategory(custom(" study "), true) } }
        assertEquals(1, listOf(one.await(), two.await()).count { it.isSuccess })
        assertEquals(13, repository.snapshot().categories.size)
    }
}

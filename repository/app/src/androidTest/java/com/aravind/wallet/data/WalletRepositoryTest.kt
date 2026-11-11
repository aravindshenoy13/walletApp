package com.aravind.wallet.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.RecordType
import java.math.BigInteger
import java.time.LocalDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletRepositoryTest {
    private lateinit var database: WalletDatabase
    private lateinit var repository: WalletRepository
    private val at = LocalDateTime.of(2026, 11, 11, 13, 30)
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java).addCallback(V2CategorySeeds.callback).build()
        repository = WalletRepository(database)
    }
    @After fun tearDown() { database.close() }
    private suspend fun snapshot(): Ledger = withTimeout(5_000) { repository.ledger.first() }
    private fun entry(account: Long, type: RecordType = RecordType.EXPENSE, destination: Long? = null) =
        WalletRecord(type = type, amountMinor = 2_500, accountId = account, destinationAccountId = destination,
            categoryId = if (type == RecordType.TRANSFER) null else "SALARY", occurredAt = at, description = "Payment")

    @Test fun transferEditAndDeleteRecalculateBothSides() = runBlocking {
        val source = repository.saveAccount(Account(name = "Source", openingBalanceMinor = 10_000))
        val destination = repository.saveAccount(Account(name = "Destination", openingBalanceMinor = -5_000))
        val third = repository.saveAccount(Account(name = "Third", openingBalanceMinor = 0))
        val id = repository.saveRecord(entry(source, RecordType.TRANSFER, destination))
        assertEquals(BigInteger.valueOf(7_500), snapshot().balances[source])
        assertEquals(BigInteger.valueOf(-2_500), snapshot().balances[destination])
        assertEquals(BigInteger.ZERO, snapshot().totals(at.toLocalDate()).net)

        repository.saveRecord(entry(third, RecordType.TRANSFER, source).copy(id = id, amountMinor = 1_000))
        val edited = snapshot()
        assertEquals(BigInteger.valueOf(11_000), edited.balances[source])
        assertEquals(BigInteger.valueOf(-5_000), edited.balances[destination])
        assertEquals(BigInteger.valueOf(-1_000), edited.balances[third])
        assertEquals(1, edited.records.size)

        repository.deleteRecord(id)
        val deleted = snapshot()
        assertTrue(deleted.records.isEmpty())
        assertEquals(BigInteger.valueOf(10_000), deleted.balances[source])
        assertEquals(BigInteger.ZERO, deleted.balances[third])
    }

    @Test fun changingTypeAccountDateAndCategoryReversesOldEffects() = runBlocking {
        val one = repository.saveAccount(Account(name = "One", openingBalanceMinor = 0))
        val two = repository.saveAccount(Account(name = "Two", openingBalanceMinor = 0))
        val original = entry(one)
        val id = repository.saveRecord(original)
        assertEquals(BigInteger.valueOf(-2_500), snapshot().balances[one])
        repository.saveRecord(original.copy(id = id, type = RecordType.INCOME, accountId = two,
            occurredAt = at.plusDays(1), categoryId = "SALARY", note = "Edited"))
        val edited = snapshot()
        assertEquals(BigInteger.ZERO, edited.balances[one])
        assertEquals(BigInteger.valueOf(2_500), edited.balances[two])
        assertEquals(BigInteger.ZERO, edited.totals(at.toLocalDate()).expense)
        assertEquals(BigInteger.valueOf(2_500), edited.totals(at.plusDays(1).toLocalDate()).income)
        repository.saveRecord(edited.records.single().copy(type = RecordType.TRANSFER, destinationAccountId = one, categoryId = null))
        assertEquals(BigInteger.valueOf(-2_500), snapshot().balances[two])
        assertEquals(BigInteger.valueOf(2_500), snapshot().balances[one])
        assertEquals(BigInteger.ZERO, snapshot().totals(at.plusDays(1).toLocalDate()).income)
    }

    @Test fun openingBalancesAndAccountNamesRemainEditable() = runBlocking {
        val id = repository.saveAccount(Account(name = "  Cash  ", openingBalanceMinor = -100))
        repository.saveRecord(entry(id))
        repository.saveAccount(Account(id, "Pocket", 200))
        val ledger = snapshot()
        assertEquals("Pocket", ledger.accounts.single().name)
        assertEquals(BigInteger.valueOf(-2_300), ledger.balances[id])
        assertEquals(BigInteger.valueOf(2_500), ledger.totals(at.toLocalDate()).expense)
    }

    @Test fun invalidWritesAreRejectedWithoutPartialChanges() = runBlocking {
        val id = repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 0))
        val invalid = listOf(entry(id).copy(amountMinor = 0), entry(id).copy(amountMinor = -1),
            entry(id).copy(categoryId = null), entry(id).copy(description = "x".repeat(121)), entry(999),
            entry(id, RecordType.TRANSFER, id), entry(id, RecordType.TRANSFER, 999),
            entry(id).copy(destinationAccountId = id), entry(id).copy(id = 999))
        invalid.forEach { record ->
            assertTrue(runCatching { repository.saveRecord(record) }.exceptionOrNull() is IllegalArgumentException)
        }
        assertTrue(runCatching { repository.saveAccount(Account(name = " CASH ", openingBalanceMinor = 0)) }.isFailure)
        assertTrue(runCatching { repository.saveAccount(Account(name = " ", openingBalanceMinor = 0)) }.isFailure)
        assertTrue(snapshot().records.isEmpty())
        assertEquals(1, snapshot().accounts.size)
        assertEquals(BigInteger.ZERO, snapshot().totalBalance)
    }

    @Test fun concurrentDuplicateAccountsCannotBothBeCreated() = runBlocking {
        val first = async { runCatching { repository.saveAccount(Account(name = "One", openingBalanceMinor = 0)) } }
        val second = async { runCatching { repository.saveAccount(Account(name = "one", openingBalanceMinor = 0)) } }
        assertEquals(1, listOf(first.await(), second.await()).count { it.isSuccess })
        assertEquals(1, snapshot().accounts.size)
    }

    @Test fun optionalDescriptionsAreTrimmedAndPersistForEveryRecordType() = runBlocking {
        val source = repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 10000))
        val destination = repository.saveAccount(Account(name = "Bank", openingBalanceMinor = 0))
        RecordType.entries.forEach { type ->
            val record = entry(source, type, if (type == RecordType.TRANSFER) destination else null).copy(description = "  ")
            val id = repository.saveRecord(record)
            repository.saveRecord(record.copy(id = id, description = ""))
        }
        val ledger = snapshot()
        assertEquals(3, ledger.records.size)
        assertTrue(ledger.records.all { it.description == "" })
        assertEquals(BigInteger.valueOf(7500), ledger.balances[source])
        assertEquals(BigInteger.valueOf(2500), ledger.balances[destination])
    }

    @Test fun orderingUsesOccurrenceTimeThenIdRatherThanCreationOrder() = runBlocking {
        val id = repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 0))
        repository.saveRecord(entry(id).copy(description = "Afternoon"))
        repository.saveRecord(entry(id).copy(description = "Morning", occurredAt = at.withHour(9)))
        repository.saveRecord(entry(id).copy(description = "Next day", occurredAt = at.plusDays(1)))
        repository.saveRecord(entry(id).copy(description = "Same time later"))
        assertEquals(listOf("Next day", "Same time later", "Afternoon", "Morning"), snapshot().records.map { it.description })
    }

    @Test fun dataSurvivesClosingAndReopeningTheDatabase() = runBlocking {
        val name = "wallet-persistence-test.db"
        context.deleteDatabase(name)
        try {
            val disk = Room.databaseBuilder(context, WalletDatabase::class.java, name).addCallback(V2CategorySeeds.callback).build()
            val stored = WalletRepository(disk)
            val account = stored.saveAccount(Account(name = "Saved", openingBalanceMinor = -100))
            stored.saveRecord(entry(account).copy(note = "Persisted note"))
            disk.close()
            val reopened = Room.databaseBuilder(context, WalletDatabase::class.java, name).addCallback(V2CategorySeeds.callback).build()
            try {
                val result = withTimeout(5_000) { WalletRepository(reopened).ledger.first() }
                assertEquals("Persisted note", result.records.single().note)
                assertEquals(at, result.records.single().occurredAt)
                assertEquals(BigInteger.valueOf(-2_600), result.balances[account])
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}

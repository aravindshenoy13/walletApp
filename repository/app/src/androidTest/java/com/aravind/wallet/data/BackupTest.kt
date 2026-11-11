package com.aravind.wallet.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aravind.wallet.domain.*
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupTest {
    @Test fun roundTripAndNonemptyGuard() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, WalletDatabase::class.java).addCallback(V2CategorySeeds.callback).build()
        try {
            val repo = WalletRepository(db)
            val category = V2CategorySeeds.rows.first().copy(name = "Archived custom name", iconKey = "plane", isArchived = true)
            val data = Ledger(listOf(Account(7, "Bank", Long.MIN_VALUE), Account(9, "Cash", 100, "cash"), Account(11, "Card", -100, "card")), listOf(
                WalletRecord(13, RecordType.TRANSFER, 50, 7, 9, null, LocalDateTime.of(2026,11,12,10,30), "Payment", "legacy note")),
                V2CategorySeeds.rows.map { if (it.id == category.id) category else it })
            val decoded = WalletBackup.decode(WalletBackup.encode(data))
            assertEquals(data.accounts, decoded.accounts); assertEquals(data.records, decoded.records); assertEquals(data.categories, decoded.categories)
            repo.restore(decoded)
            assertEquals(data.accounts.toSet(), repo.snapshot().accounts.toSet())
            assertEquals(data.records, repo.snapshot().records)
            try { repo.restore(decoded); fail("Nonempty restore accepted") } catch (_: IllegalArgumentException) { }
            assertEquals(data.records, repo.snapshot().records)
            val next = repo.saveAccount(Account(name = "New", openingBalanceMinor = 0))
            assertTrue(next > 11)
        } finally { db.close() }
    }
    @Test fun rejectsBrokenReferencesAndUnsupportedVersion() {
        val invalid = Ledger(listOf(Account(1, "Bank", 0)), listOf(WalletRecord(1, RecordType.TRANSFER, 5, 1, 99, null, LocalDateTime.now(), "")), V2CategorySeeds.rows)
        try { WalletBackup.decode(WalletBackup.encode(invalid)); fail("Missing destination accepted") } catch (_: IllegalArgumentException) { }
        try { WalletBackup.decode(WalletBackup.encode(Ledger(emptyList(), emptyList(), V2CategorySeeds.rows)).replace("\"version\": 2", "\"version\": 99")); fail("Version accepted") } catch (_: IllegalArgumentException) { }
    }

    @Test fun oldBackupsDefaultIconsAndNewBackupsRejectInvalidIcons() {
        val ledger = Ledger(listOf(Account(1, "Account", -100, "card")), emptyList(), V2CategorySeeds.rows)
        val old = org.json.JSONObject(WalletBackup.encode(ledger)).apply {
            put("version", 1); getJSONArray("accounts").getJSONObject(0).remove("icon")
        }
        assertEquals("bank", WalletBackup.decode(old.toString()).accounts.single().iconKey)
        val invalid = org.json.JSONObject(WalletBackup.encode(ledger)).apply {
            getJSONArray("accounts").getJSONObject(0).put("icon", "unknown")
        }
        try { WalletBackup.decode(invalid.toString()); fail("Invalid account icon accepted") }
        catch (_: IllegalArgumentException) { }
        invalid.getJSONArray("accounts").getJSONObject(0).remove("icon")
        try { WalletBackup.decode(invalid.toString()); fail("Missing V2 icon accepted") }
        catch (_: org.json.JSONException) { }
    }
}

package com.aravind.wallet.ui

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.Account
import com.aravind.wallet.data.V2CategorySeeds
import com.aravind.wallet.data.WalletDatabase
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.domain.RecordType
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportViewModelTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun clearTestData() {
        val database = WalletDatabase.create(app)
        try {
            database.clearAllTables()
            database.runInTransaction { V2CategorySeeds.insert(database.openHelper.writableDatabase) }
        } finally { database.close() }
    }

    @Test fun cancelResetsPickerAndRepeatedPickerRequestsAreIgnored() {
        val model = ExportViewModel(app)
        instrumentation.runOnMainSync {
            assertTrue(model.choose())
            assertFalse(model.choose())
            model.selected(null)
            assertEquals(ExportState.Idle, model.state.value)
        }
    }

    @Test fun failedProviderCanBeRetriedAndActualOutputIsClosedAndReadable() = runBlocking {
        val account = app.repository.saveAccount(Account(name = "Export cash", openingBalanceMinor = 1000))
        app.repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = 123, accountId = account,
            categoryId = "FOOD", occurredAt = LocalDateTime.of(2026,11,11,12,30), description = "Export test"))
        val before = app.repository.snapshot()
        val model = ExportViewModel(app)
        val store = ViewModelStore()
        store.put("export", model)
        val file = File(app.cacheDir, "v2-export-writer-test.csv")
        try {
            instrumentation.runOnMainSync { model.selected(Uri.parse("content://com.aravind.wallet.missing/file")) }
            withTimeout(10_000) { model.state.first { it == ExportState.Failed } }
            instrumentation.runOnMainSync { assertTrue(model.choose()); model.selected(Uri.fromFile(file)) }
            val saved = withTimeout(10_000) { model.state.first { it is ExportState.Saved } } as ExportState.Saved
            assertEquals(1, saved.count)
            assertTrue(file.readText().startsWith("\uFEFFrecord_id,date,time,type,amount"))
            assertTrue(file.readText().contains(",1.23,INR,Export cash,,Food,Export test\r\n"))
            val after = app.repository.snapshot()
            assertEquals(before.records, after.records)
            assertEquals(before.balances, after.balances)
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            file.delete()
        }
    }
}

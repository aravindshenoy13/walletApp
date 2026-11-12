package com.aravind.wallet.ui

import android.net.Uri
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.*
import com.aravind.wallet.domain.RecordType
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateRegressionTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun reset() {
        app.getSharedPreferences("appearance", 0).edit().putBoolean("dark", false).commit()
        instrumentation.runOnMainSync {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }
        WalletDatabase.create(app).let { db ->
            try { db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) } }
            finally { db.close() }
        }
    }

    @Test fun populatedHistoryDoesNotShowAnEmptyState(): Unit = runBlocking {
        val id = app.repository.saveAccount(Account(name = "History", openingBalanceMinor = 0))
        app.repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = 100,
            accountId = id, categoryId = "FOOD", occurredAt = LocalDateTime.now().minusDays(2), description = "Historical record"))
        ActivityScenario.launch(MainActivity::class.java).use {
            await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
            onView(withId(R.id.empty_records)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test fun analysisRetapAndRotationKeepTheSelectedMonth() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
            onView(withId(R.id.analysis_tab)).perform(click())
            onView(withId(R.id.previous_month)).perform(click())
            var month = ""
            scenario.onActivity { month = it.findViewById<TextView>(R.id.calendar_month).text.toString() }
            onView(withId(R.id.analysis_tab)).perform(click())
            onView(withId(R.id.calendar_month)).check(matches(withText(month)))
            scenario.recreate()
            await { onView(withId(R.id.calendar_month)).check(matches(withText(month))) }
        }
    }

    @Test fun restoreConfirmationSurvivesRecreationAndCancelReleasesControls() = runBlocking {
        val file = File(app.cacheDir, "state-restore.json")
        file.writeText(WalletBackup.encode(app.repository.snapshot()))
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
                scenario.onActivity {
                    val model = ViewModelProvider(it)[BackupViewModel::class.java]
                    assertTrue(model.choose(true))
                    assertFalse(model.choose(false))
                    model.selected(Uri.fromFile(file), true)
                }
                await { onView(withText("Restore backup?")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(isDisplayed())) }
                scenario.recreate()
                await { onView(withText("Restore backup?")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).check(matches(isDisplayed())) }
                onView(withText("Cancel")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click())
                await { onView(withText("Back up data")).check(matches(isEnabled())) }
                assertTrue(app.repository.snapshot().accounts.isEmpty())
            }
        } finally { file.delete() }
    }

    @Test fun backupPickerCancellationAllowsAnotherOperation() {
        instrumentation.runOnMainSync {
            val model = BackupViewModel(app)
            assertTrue(model.choose(false))
            assertFalse(model.choose(true))
            model.selected(null, false)
            assertEquals(BackupState.Idle, model.state.value)
            assertTrue(model.choose(true))
        }
    }

    @Test fun restoreAndBackupCompleteOnceAndPreserveData(): Unit = runBlocking {
        val ledger = app.repository.snapshot().copy(accounts = listOf(Account(id = 42, name = "Restored", openingBalanceMinor = 12345)))
        val input = File(app.cacheDir, "state-input.json")
        val output = File(app.cacheDir, "state-output.json")
        val model = BackupViewModel(app)
        val store = androidx.lifecycle.ViewModelStore()
        instrumentation.runOnMainSync { store.put("backup", model) }
        try {
            input.writeText(WalletBackup.encode(ledger))
            instrumentation.runOnMainSync { assertTrue(model.choose(true)); model.selected(Uri.fromFile(input), true) }
            withTimeout(10000) { model.state.first { it is BackupState.Confirm } }
            instrumentation.runOnMainSync { model.confirm(); model.confirm() }
            val restored = withTimeout(10000) { model.state.first { it is BackupState.Result } }
            assertEquals(BackupState.Result("Backup restored."), restored)
            assertEquals(ledger, app.repository.snapshot())
            instrumentation.runOnMainSync {
                model.dismiss()
                assertTrue(model.choose(false))
                model.selected(Uri.fromFile(output), false)
            }
            val saved = withTimeout(10000) { model.state.first { it is BackupState.Result } }
            assertEquals(BackupState.Result("Full backup saved."), saved)
            assertEquals(ledger, WalletBackup.decode(output.readText()))
        } finally {
            instrumentation.runOnMainSync { store.clear() }
            input.delete(); output.delete()
        }
    }

    private fun await(block: () -> Unit) {
        val deadline = System.currentTimeMillis() + 10000
        while (true) {
            try { block(); return } catch (e: Throwable) {
                if (System.currentTimeMillis() >= deadline) throw e
                Thread.sleep(100)
            }
        }
    }
}

package com.aravind.wallet.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletDatabase
import com.aravind.wallet.data.V2CategorySeeds
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import java.io.File
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletFlowTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val repository get() = (context.applicationContext as WalletApplication).repository

    // Instrumentation runs against the debug app on a disposable test device.
    @Before fun clearDebugTestData() {
        val database = WalletDatabase.create(context)
        database.clearAllTables()
        database.runInTransaction { V2CategorySeeds.insert(database.openHelper.writableDatabase) }
        database.close()
    }

    @Test fun accountExpenseEditTransferDeleteAndIncomeFlow() {
        val suffix = System.nanoTime().toString().takeLast(6)
        val cash = "Cash $suffix"
        val bank = "Bank $suffix"
        val lunch = "Lunch $suffix"
        val transfer = "Payment $suffix"
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            awaitText(context.getString(R.string.add_record))
            onView(withId(R.id.accounts_tab)).perform(click())
            addAccount(cash, "100.00")
            addAccount(bank, "-50.00")
            capture("accounts")
            onView(withId(R.id.records_tab)).perform(click())
            onView(withId(R.id.add_record_button)).perform(click())
            awaitText(context.getString(R.string.save_record))
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("10.25"), closeSoftKeyboard())
            onView(withId(R.id.description)).perform(scrollTo(), replaceText(lunch), closeSoftKeyboard())
            chooseAccount(R.id.account, cash)
            val today = LocalDate.now()
            onView(withId(R.id.date)).perform(scrollTo(), click())
            onView(isAssignableFrom(DatePicker::class.java)).perform(dateAction(today.year, today.monthValue - 1, today.dayOfMonth))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.time)).perform(scrollTo(), click())
            onView(isAssignableFrom(TimePicker::class.java)).perform(timeAction(8, 45))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.save)).perform(scrollTo(), click())
            awaitText(lunch, true)
            var ledger = awaitLedger { it.records.any { row -> row.description == lunch } }
            val record = ledger.records.single { it.description == lunch }
            assertEquals(today.atTime(8, 45), record.occurredAt)
            assertEquals("", record.note)
            val cashId = ledger.accounts.single { it.name == cash }.id
            val bankId = ledger.accounts.single { it.name == bank }.id
            assertEquals(BigInteger.valueOf(8_975), ledger.balances[cashId])

            onView(withId(R.id.expense)).check(matches(withText(Money.format(1_025))))
            capture("records")

            onView(withText(org.hamcrest.CoreMatchers.containsString(lunch))).perform(click())
            awaitText(context.getString(R.string.save_record))
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("12.50"), closeSoftKeyboard())
            onView(withId(R.id.save)).perform(scrollTo(), click())
            awaitText(lunch, true)
            ledger = awaitLedger { it.records.any { row -> row.id == record.id && row.amountMinor == 1_250L } }
            assertEquals(BigInteger.valueOf(8_750), ledger.balances[cashId])

            onView(withText(org.hamcrest.CoreMatchers.containsString(lunch))).perform(click())
            awaitText(context.getString(R.string.save_record))
            onView(withId(R.id.type_transfer)).perform(scrollTo(), click())
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("20.00"), closeSoftKeyboard())
            onView(withId(R.id.description)).perform(scrollTo(), replaceText(transfer), closeSoftKeyboard())
            chooseAccount(R.id.account, cash)
            chooseAccount(R.id.destination, bank)
            capture("transfer")
            onView(withId(R.id.save)).perform(scrollTo(), click())
            awaitText(transfer, true)
            onView(allOf(withId(R.id.amount), isDisplayed())).check { view, error ->
                if (error != null) throw error
                assertEquals(view.context.getColor(R.color.transfer_amount), (view as android.widget.TextView).currentTextColor)
            }
            onView(allOf(withId(R.id.badge), isDisplayed())).check(matches(isDisplayed()))
            capture("v2-transfer-records")
            ledger = awaitLedger { it.records.any { row -> row.description == transfer } }
            assertEquals(BigInteger.valueOf(8_000), ledger.balances[cashId])
            assertEquals(BigInteger.valueOf(-3_000), ledger.balances[bankId])
            onView(withId(R.id.expense)).check(matches(withText(Money.format(0))))

            onView(withText(org.hamcrest.CoreMatchers.containsString(transfer))).perform(click())
            awaitText(context.getString(R.string.delete_record))
            onView(withId(R.id.delete)).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click())
            awaitText(context.getString(R.string.add_record))
            ledger = awaitLedger { it.records.none { row -> row.id == record.id } }
            assertEquals(BigInteger.valueOf(10_000), ledger.balances[cashId])
            assertEquals(BigInteger.valueOf(-5_000), ledger.balances[bankId])

            onView(withId(R.id.add_record_button)).perform(click())
            awaitText(context.getString(R.string.save_record))
            onView(withId(R.id.type_income)).perform(scrollTo(), click())
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("1.25"), closeSoftKeyboard())
            onView(withId(R.id.description)).perform(scrollTo(), replaceText("Refund $suffix"), closeSoftKeyboard())
            chooseAccount(R.id.account, cash)
            onView(withId(R.id.save)).perform(scrollTo(), click())
            awaitText("Refund $suffix", true)
            ledger = awaitLedger { it.records.any { row -> row.description == "Refund $suffix" } }
            assertEquals("Food", ledger.categoriesById[ledger.records.single { it.description == "Refund $suffix" }.categoryId]?.name)
            onView(withId(R.id.income)).check(matches(withText(Money.format(125))))
        } finally { scenario.close() }
    }

    @Test fun recordDraftSurvivesActivityRecreation() {
        runBlocking { repository.saveAccount(Account(name = "Draft ${System.nanoTime()}", openingBalanceMinor = 0)) }
        val scenario = ActivityScenario.launch<RecordEditorActivity>(Intent(context, RecordEditorActivity::class.java))
        try {
            awaitText(context.getString(R.string.save_record))
            onView(withId(R.id.type_income)).perform(click())
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("123.45"), closeSoftKeyboard())
            onView(withId(R.id.description)).perform(scrollTo(), replaceText("Unsaved draft"), closeSoftKeyboard())
            scenario.recreate()
            awaitText(context.getString(R.string.save_record))
            onView(withId(R.id.type_income)).check(matches(isChecked()))
            onView(withId(R.id.amount)).check(matches(withText("123.45")))
            onView(withId(R.id.description)).check(matches(withText("Unsaved draft")))
        } finally { scenario.close() }
    }

    private fun addAccount(name: String, balance: String) {
        onView(withId(R.id.add)).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isDisplayed()
            override fun getDescription() = "Wait for the account header layout before tapping"
            override fun perform(ui: androidx.test.espresso.UiController, view: android.view.View) {
                ui.loopMainThreadForAtLeast(100)
                check(view.isEnabled)
            }
        }, click())
        awaitText(context.getString(R.string.save_account))
        onView(withId(R.id.name)).perform(replaceText(name), closeSoftKeyboard())
        onView(withId(R.id.opening_balance)).perform(replaceText(balance), closeSoftKeyboard())
        onView(withId(R.id.save)).perform(scrollTo(), click())
        awaitText(name)
    }

    private fun chooseAccount(id: Int, name: String) {
        onView(withId(id)).perform(scrollTo(), click())
        onData(equalTo(name)).perform(click())
    }

    private fun awaitLedger(predicate: (Ledger) -> Boolean): Ledger = runBlocking {
        withTimeout(10_000) { repository.ledger.first(predicate) }
    }

    private fun awaitText(text: String, record: Boolean = false) {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            try { onView(if (record) allOf(withId(R.id.meta), withText(org.hamcrest.CoreMatchers.containsString(text))) else withText(text)).check(matches(allOf(withEffectiveVisibility(Visibility.VISIBLE), isEnabled()))); return }
            catch (error: Throwable) {
                if (System.currentTimeMillis() >= deadline) throw error
                Thread.sleep(50)
            }
        }
    }

    private fun dateAction(year: Int, month: Int, day: Int) = object : ViewAction {
        override fun getConstraints() = isAssignableFrom(DatePicker::class.java)
        override fun getDescription() = "Choose occurrence date"
        override fun perform(uiController: UiController, view: android.view.View) {
            (view as DatePicker).updateDate(year, month, day)
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun timeAction(hour: Int, minute: Int) = object : ViewAction {
        override fun getConstraints() = isAssignableFrom(TimePicker::class.java)
        override fun getDescription() = "Choose occurrence time"
        override fun perform(uiController: UiController, view: android.view.View) {
            (view as TimePicker).hour = hour
            view.minute = minute
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun capture(name: String) {
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = File(context.getExternalFilesDir(null), "qa/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}

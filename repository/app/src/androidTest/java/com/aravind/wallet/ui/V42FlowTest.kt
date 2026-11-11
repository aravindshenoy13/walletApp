package com.aravind.wallet.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.*
import com.aravind.wallet.domain.RecordType
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V42FlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    @Before fun setup() {
        val db = WalletDatabase.create(app)
        try { db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) } }
        finally { db.close() }
        app.getSharedPreferences("appearance", 0).edit().putBoolean("dark", false).commit()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    @After fun cleanup() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
    @Test fun accountIconsPersistAcrossEditRecreationAndReopen() = runBlocking {
        val id = app.repository.saveAccount(Account(name = "Visual label", openingBalanceMinor = -500))
        for ((key, control) in listOf("cash" to R.id.icon_cash, "card" to R.id.icon_card, "bank" to R.id.icon_bank)) {
            val intent = Intent(app, AccountEditorActivity::class.java).putExtra("id", id)
            ActivityScenario.launch<AccountEditorActivity>(intent).use { scenario ->
                await(withId(control)); onView(withId(control)).perform(scrollTo(), click())
                scenario.recreate()
                await(withId(control)); onView(withId(control)).check(matches(isChecked()))
                capture(scenario, "account-$key")
                onView(withId(R.id.save)).perform(scrollTo(), click())
                awaitSaved { app.repository.snapshot().accounts.single().iconKey == key }
            }
            assertEquals(-500L, app.repository.snapshot().accounts.single().openingBalanceMinor)
        }
        ActivityScenario.launch<AccountHistoryActivity>(Intent(app, AccountHistoryActivity::class.java).putExtra("id", id)).use { scenario ->
            await(withText("Visual label"))
            scenario.onActivity { activity ->
                assertEquals(activity.getColor(R.color.expense_amount), activity.findViewById<TextView>(R.id.current_balance).currentTextColor)
                assertNotNull(activity.findViewById<TextView>(R.id.title).compoundDrawablesRelative[0])
            }
        }
        Unit
    }
    @Test fun searchClearBackOutsideTapAndLeavingRestoreNormalRecords() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(withId(R.id.open_search))
            onView(withId(R.id.open_search)).perform(click())
            onView(withId(R.id.record_search)).perform(typeText("999"))
            onView(withId(R.id.clear_search)).perform(click())
            onView(withId(R.id.record_search)).check(matches(withText("")))
            onView(withId(R.id.record_search)).perform(typeText("123"))
            androidx.test.espresso.Espresso.pressBack()
            onView(withId(R.id.open_search)).check(matches(isDisplayed()))
            onView(withId(R.id.record_count)).check(matches(withText(R.string.all_records)))
            onView(withId(R.id.open_search)).perform(click())
            onView(withId(R.id.record_search)).perform(typeText("456"))
            onView(withId(R.id.today_date)).perform(click())
            onView(withId(R.id.open_search)).check(matches(isDisplayed()))
            onView(withId(R.id.record_count)).check(matches(withText(R.string.all_records)))
            onView(withId(R.id.open_search)).perform(click())
            onView(withId(R.id.record_search)).perform(typeText("789"))
            onView(withId(R.id.accounts_tab)).perform(click())
            onView(withId(R.id.records_tab)).perform(click())
            onView(withId(R.id.open_search)).check(matches(isDisplayed()))
            capture(scenario, "records")
        }
    }
    @Test fun bothThemesBalanceColorsAndWheelScreens() = runBlocking {
        val id = app.repository.saveAccount(Account(name = "Bank", openingBalanceMinor = 10000, iconKey = "bank"))
        app.repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 0, iconKey = "cash"))
        app.repository.saveAccount(Account(name = "Card", openingBalanceMinor = -2000, iconKey = "card"))
        for ((category, amount) in listOf("FOOD" to 1200L, "OTHER" to 600L)) {
            app.repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = amount, accountId = id,
                categoryId = category, occurredAt = LocalDateTime.now(), description = ""))
        }
        for ((theme, mode) in listOf("light" to AppCompatDelegate.MODE_NIGHT_NO, "dark" to AppCompatDelegate.MODE_NIGHT_YES)) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync { AppCompatDelegate.setDefaultNightMode(mode) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(withId(R.id.accounts_tab)); onView(withId(R.id.accounts_tab)).perform(click())
                await(withText("Bank")); capture(scenario, "$theme-accounts")
                scenario.onActivity { activity ->
                    val v = TextView(activity)
                    for (value in listOf(0L, 10L, -10L)) {
                        v.showBalance(java.math.BigInteger.valueOf(value))
                        assertEquals(activity.getColor(if (value < 0) R.color.expense_amount else R.color.ink), v.currentTextColor)
                    }
                }
                onView(withId(R.id.analysis_tab)).perform(click())
                await(withId(R.id.analysis_view))
                onView(withId(R.id.analysis_view)).perform(click())
                await(withText("Category Breakdown"))
                onView(withText("Category Breakdown")).perform(click())
                capture(scenario, "$theme-wheel")
            }
            ActivityScenario.launch(SettingsActivity::class.java).use { scenario -> capture(scenario, "$theme-settings") }
            ActivityScenario.launch(RecordEditorActivity::class.java).use { scenario ->
                await(withId(R.id.type_income)); onView(withId(R.id.type_income)).perform(scrollTo(), click())
                onView(withId(R.id.type_income)).check(matches(isChecked()))
                onView(withId(R.id.type_expense)).check(matches(isNotChecked()))
                capture(scenario, "$theme-transaction")
            }
            ActivityScenario.launch(ExportActivity::class.java).use { scenario ->
                await(withText("Bank"))
                onView(withText("Bank")).perform(click()).check(matches(isNotChecked()))
                onView(withText("Cash")).check(matches(isChecked()))
                onView(withId(R.id.export_income)).perform(click()).check(matches(isChecked()))
                onView(withId(R.id.export_all)).check(matches(isNotChecked()))
                capture(scenario, "$theme-export")
            }
            ActivityScenario.launch(CategoryEditorActivity::class.java).use { scenario ->
                await(withId(R.id.name)); onView(withId(R.id.name)).perform(scrollTo())
                capture(scenario, "$theme-category")
            }
        }
    }
    private fun await(matcher: Matcher<View>) {
        val deadline = System.currentTimeMillis() + 10000
        while (true) {
            try { onView(matcher).check(matches(isDisplayed())); return }
            catch (error: Throwable) { if (System.currentTimeMillis() > deadline) throw error; Thread.sleep(100) }
        }
    }
    private fun awaitSaved(predicate: suspend () -> Boolean) = runBlocking {
        val deadline = System.currentTimeMillis() + 10000
        while (!predicate()) { check(System.currentTimeMillis() < deadline); kotlinx.coroutines.delay(100) }
    }
    private fun <T : android.app.Activity> capture(scenario: ActivityScenario<T>, name: String) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            if (view.width > 0 && view.height > 0) {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                File(app.getExternalFilesDir(null), "v42-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}

package com.aravind.wallet.ui

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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
import com.aravind.wallet.data.V2CategorySeeds
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.data.WalletDatabase
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.domain.RecordType
import java.io.File
import java.math.BigInteger
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryFlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private val repository get() = app.repository
    @Before fun clearData() {
        app.getSharedPreferences("appearance", 0).edit().putBoolean("dark", false).commit()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }
        val db = WalletDatabase.create(app)
        try {
            db.clearAllTables()
            db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) }
        } finally { db.close() }
    }

    @Test fun builtInRenameArchiveHistoricalEditCustomCategoryAndPickerCancellation() = runBlocking {
        val account = repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 10000))
        val record = repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = 2500,
            accountId = account, categoryId = "FOOD", occurredAt = LocalDateTime.now().withSecond(0).withNano(0),
            description = "Test lunch", note = "Legacy note"))
        val main = ActivityScenario.launch(MainActivity::class.java)
        try {
            await(withId(R.id.add_record_button))
            main.onActivity { assertEquals(Configuration.UI_MODE_NIGHT_NO, it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) }
            onView(withId(R.id.categories_tab)).perform(click())
            await(withText("Food"))
            onView(allOf(withText("Food"), isDisplayed())).perform(click())
            await(withId(R.id.save))
            onView(withId(R.id.name)).perform(scrollTo(), replaceText("Dining"), closeSoftKeyboard())
            onView(withContentDescription(startsWith("Book icon"))).perform(scrollTo(), click())
            onView(withContentDescription(startsWith("Blue background"))).perform(scrollTo(), click())
            onView(withId(R.id.selection_status)).check(matches(withText("Book · Blue")))
            capture("v2-category-editor")
            onView(withId(R.id.save)).perform(scrollTo(), click())
            withTimeout(10_000) { repository.ledger.first { it.categoriesById.getValue("FOOD").name == "Dining" } }
            onView(withId(R.id.records_tab)).perform(click())
            await(withText("Dining"))
            capture("v2-records")
            assertEquals(BigInteger.valueOf(7500), repository.snapshot().balances[account])

            onView(withId(R.id.categories_tab)).perform(click())
            onView(withId(R.id.category_list)).perform(scrollList(0))
            await(withText("Dining"))
            onView(allOf(withText("Dining"), isDisplayed())).perform(click())
            await(withId(R.id.archive))
            onView(withId(R.id.archive)).perform(scrollTo(), click())
            onView(withId(android.R.id.button2)).perform(click())
            assertFalse(repository.snapshot().categoriesById.getValue("FOOD").isArchived)
            onView(withId(R.id.archive)).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click())
            withTimeout(10_000) { repository.ledger.first { it.categoriesById.getValue("FOOD").isArchived } }
            await(withId(R.id.categories_tab))
            onView(withId(R.id.records_tab)).perform(click())
            await(withText("Dining"))
            onView(allOf(withText("Dining"), isDisplayed())).perform(click())
            await(withId(R.id.amount))
            onView(withId(R.id.category_name)).check(matches(withText("Dining · Archived")))
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("26.00"), closeSoftKeyboard())
            onView(withId(R.id.save)).perform(scrollTo(), click())
            withTimeout(10_000) { repository.ledger.first { it.records.single { row -> row.id == record }.amountMinor == 2600L } }
            await(withId(R.id.add_record_button))
            onView(withId(R.id.add_record_button)).perform(click())
            await(withId(R.id.save))
            onView(withId(R.id.category)).perform(scrollTo(), click())
            await(withId(R.id.search))
            onView(withId(R.id.search)).perform(replaceText("Dining"), closeSoftKeyboard())
            onView(withId(R.id.message)).check(matches(withText(R.string.no_categories_found)))
            onView(withId(R.id.back)).perform(click())
            onView(withId(R.id.back)).perform(click())
            await(withId(R.id.categories_tab))
            onView(withId(R.id.categories_tab)).perform(click())
            onView(withId(R.id.add)).perform(click())
            await(withId(R.id.save))
            onView(withId(R.id.name)).perform(scrollTo(), replaceText("Learning"), closeSoftKeyboard())
            onView(withContentDescription(startsWith("Book icon"))).perform(scrollTo(), click())
            onView(withContentDescription(startsWith("Mint background"))).perform(scrollTo(), click())
            onView(withId(R.id.save)).perform(scrollTo(), click())
            val result = withTimeout(10_000) { repository.ledger.first { it.categories.any { row -> row.name == "Learning" } } }
            val category = result.categories.single { it.name == "Learning" }
            assertFalse(category.isBuiltIn)
            assertEquals("book-open", category.iconKey)
            assertEquals("Mint", category.backgroundColorKey)
            await(withId(R.id.category_list))
            val position = result.categories.indexOfFirst { it.id == category.id }
            onView(withId(R.id.category_list)).perform(scrollList(position))
            await(withText("Learning"))
            capture("v2-categories")
            onView(withId(R.id.records_tab)).perform(click())
            onView(withId(R.id.settings)).perform(click())
            await(allOf(withText("Export CSV"), isDisplayed()))
            onView(allOf(withText("Export CSV"), isDisplayed())).perform(click())
            await(withText(app.getString(R.string.all_transactions)))
            capture("v2-export")
            onView(withId(R.id.save)).perform(scrollTo(), click())
            // The document picker is a system Activity; allow its launch before returning.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(750)
            InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            await(withText(app.getString(R.string.choose_save)))
            onView(withId(R.id.save)).check(matches(isEnabled()))
            onView(withId(R.id.back)).perform(click())
            onView(withText("Back")).perform(click())
            val end = repository.snapshot()
            assertEquals(BigInteger.valueOf(7400), end.balances[account])
            assertEquals("Legacy note", end.records.single { it.id == record }.note)
        } finally { main.close() }
    }

    @Test fun iconAndBackgroundDraftRemainIndependentAcrossRecreation() {
        val scenario = ActivityScenario.launch<CategoryEditorActivity>(Intent(app, CategoryEditorActivity::class.java))
        try {
            await(withId(R.id.save))
            onView(withId(R.id.name)).perform(scrollTo(), replaceText("Draft category"), closeSoftKeyboard())
            onView(withContentDescription(startsWith("Book icon"))).perform(scrollTo(), click())
            onView(withContentDescription(startsWith("Red background"))).perform(scrollTo(), click())
            onView(withContentDescription(startsWith("Home icon"))).perform(scrollTo(), click())
            onView(withId(R.id.selection_status)).check(matches(withText("Home · Red")))
            scenario.recreate()
            await(withId(R.id.save))
            onView(withId(R.id.name)).check(matches(withText("Draft category")))
            onView(withId(R.id.selection_status)).check(matches(withText("Home · Red")))
            onView(withContentDescription(startsWith("Sky background"))).perform(scrollTo(), click())
            onView(withId(R.id.selection_status)).check(matches(withText("Home · Sky")))
        } finally { scenario.close() }
    }

    @Test fun searchableCategoryPageReturnsSelectionWithoutLosingRecordDraft() = runBlocking {
        repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 0))
        val scenario = ActivityScenario.launch<RecordEditorActivity>(Intent(app, RecordEditorActivity::class.java))
        try {
            await(withId(R.id.save))
            onView(withId(R.id.amount)).perform(scrollTo(), replaceText("42.50"), closeSoftKeyboard())
            onView(withId(R.id.description)).perform(scrollTo(), replaceText("Draft payee"), closeSoftKeyboard())
            onView(withId(R.id.category)).perform(scrollTo(), click())
            await(withId(R.id.search))
            onView(withId(R.id.search)).perform(replaceText("sALa"), closeSoftKeyboard())
            onView(withId(R.id.category_results)).check { view, error ->
                if (error != null) throw error
                assertEquals(1, (view as ListView).adapter.count)
            }
            capture("v2-category-search")
            onView(withText("Salary")).perform(click())
            await(withId(R.id.save))
            onView(withId(R.id.category_name)).check(matches(withText("Salary")))
            scenario.recreate()
            await(withId(R.id.save))
            onView(withId(R.id.category_name)).check(matches(withText("Salary")))
            onView(withId(R.id.amount)).check(matches(withText("42.50")))
            onView(withId(R.id.description)).check(matches(withText("Draft payee")))
            onView(withId(R.id.category)).perform(scrollTo(), click())
            await(withId(R.id.search))
            onView(withId(R.id.search)).perform(replaceText("no such category"), closeSoftKeyboard())
            onView(withId(R.id.message)).check(matches(isDisplayed()))
            onView(withId(R.id.back)).perform(click())
            await(withId(R.id.save))
            onView(withId(R.id.category_name)).check(matches(withText("Salary")))
            onView(withId(R.id.save)).perform(scrollTo(), click())
            val ledger = withTimeout(10_000) { repository.ledger.first { it.records.any { row -> row.description == "Draft payee" } } }
            assertEquals("SALARY", ledger.records.single { it.description == "Draft payee" }.categoryId)
        } finally { scenario.close() }
    }

    @Test fun categorySearchSurvivesPageRecreation() {
        ActivityScenario.launch<CategoryPickerActivity>(Intent(app, CategoryPickerActivity::class.java)).use { scenario ->
            await(withId(R.id.search))
            onView(withId(R.id.search)).perform(replaceText("groc"), closeSoftKeyboard())
            scenario.recreate()
            await(withText("Groceries"))
            onView(withId(R.id.search)).check(matches(withText("groc")))
            onView(withId(R.id.category_results)).check { view, error ->
                if (error != null) throw error
                assertEquals(1, (view as ListView).adapter.count)
            }
        }
    }

    private fun await(matcher: Matcher<View>) {
        val deadline = System.currentTimeMillis() + 12_000
        while (true) {
            try { onView(allOf(matcher, withEffectiveVisibility(Visibility.VISIBLE))).check(matches(isEnabled())); return }
            catch (error: Throwable) { if (System.currentTimeMillis() >= deadline) throw error; Thread.sleep(50) }
        }
    }
    private fun scrollList(position: Int) = object : ViewAction {
        override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
        override fun getDescription() = "Scroll to category"
        override fun perform(ui: UiController, view: View) { (view as RecyclerView).scrollToPosition(position); ui.loopMainThreadUntilIdle() }
    }
    private fun capture(name: String) {
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = File(app.getExternalFilesDir(null), "qa/$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}

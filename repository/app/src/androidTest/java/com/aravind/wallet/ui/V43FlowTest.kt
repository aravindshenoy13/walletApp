package com.aravind.wallet.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.*
import com.aravind.wallet.domain.*
import java.io.File
import java.math.BigInteger
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V43FlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private val today get() = LocalDate.now()
    private var account = 0L
    @Before fun fixture() = runBlocking {
        WalletDatabase.create(app).let { db ->
            try { db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) } }
            finally { db.close() }
        }
        account = app.repository.saveAccount(Account(name = "Bank", openingBalanceMinor = 12345))
        val cash = app.repository.saveAccount(Account(name = "Cash", openingBalanceMinor = 0))
        suspend fun record(type: RecordType, amount: Long, id: Long, date: LocalDate, category: String? = "FOOD", destination: Long? = null) {
            app.repository.saveRecord(WalletRecord(type = type, amountMinor = amount, accountId = id, categoryId = category,
                destinationAccountId = destination, occurredAt = date.atTime(12, 0), description = ""))
        }
        record(RecordType.INCOME, 10000, account, today)
        record(RecordType.EXPENSE, 3000, cash, today)
        record(RecordType.EXPENSE, 1000, account, today, "OTHER")
        record(RecordType.INCOME, 5000, cash, today.withDayOfMonth(today.lengthOfMonth()), "SALARY")
        record(RecordType.EXPENSE, 2000, cash, today.withDayOfMonth(1), "FOOD")
        record(RecordType.INCOME, 99900, account, today.withDayOfMonth(1).minusDays(1))
        record(RecordType.TRANSFER, 200, account, today, null, cash)
    }
    @After fun cleanup() { main { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) } }

    @Test fun recordsToggleUsesFullMonthAndSurvivesRecreationWithoutFilteringList() = runBlocking {
        val ledger = app.repository.snapshot().let { Ledger(it.accounts, it.records, it.categories) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(R.id.income, Money.format(ledger.totals(today).income))
            onView(withId(R.id.today_date)).perform(click())
            val totals = ledger.analyze(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth())).totals
            await(R.id.income, Money.format(totals.income))
            onView(withId(R.id.expense)).check(matches(withText(Money.format(totals.expense))))
            onView(withId(R.id.record_count)).check(matches(withText(R.string.all_records)))
            scenario.recreate(); await(R.id.income, Money.format(totals.income))
            onView(withId(R.id.open_search)).perform(click())
            onView(withId(R.id.record_search)).perform(replaceText("999"), closeSoftKeyboard())
            onView(withId(R.id.income)).check(matches(withText(Money.format(totals.income))))
            onView(withId(R.id.today_date)).perform(click())
            await(R.id.income, Money.format(ledger.totals(today).income))
        }
    }
    @Test fun analysisUsesAllAccountsAndWheelHistoryRetainsRange() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.analysis_tab)).perform(click())
            await(R.id.analysis_view)
            onView(withId(R.id.analysis_view)).perform(click())
            onView(withText("Flow Graph")).perform(click())
            onView(withId(R.id.range_expense)).perform(scrollTo()).check(matches(withText(Money.format(6000))))
            onView(withId(R.id.breakdown_list)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withText("Detailed analysis")).check(doesNotExist())
            onView(withId(R.id.analysis_view)).perform(scrollTo(), click())
            onView(withText("Category Expense Graph")).perform(click())
            scenario.recreate()
            onView(withId(R.id.analysis_view)).check(matches(withSpinnerText("Category Expense Graph")))
            onView(withId(R.id.custom_range)).perform(scrollTo(), click())
            for (id in listOf(R.id.start_date, R.id.end_date)) {
                onView(withId(id)).perform(click())
                onView(isAssignableFrom(android.widget.DatePicker::class.java)).perform(object : androidx.test.espresso.ViewAction {
                    override fun getConstraints() = isAssignableFrom(android.widget.DatePicker::class.java)
                    override fun getDescription() = "Choose today's date"
                    override fun perform(ui: androidx.test.espresso.UiController, view: View) {
                        (view as android.widget.DatePicker).updateDate(today.year, today.monthValue - 1, today.dayOfMonth)
                        ui.loopMainThreadUntilIdle()
                    }
                })
                onView(withId(android.R.id.button1)).perform(click())
            }
            onView(withId(R.id.apply)).perform(click())
            await(R.id.analysis_view)
            val expectedExpense = if (today.dayOfMonth == 1) 6000L else 4000L
            onView(withId(R.id.range_expense)).check(matches(withText(Money.format(expectedExpense))))
            onView(withText("Other")).perform(scrollTo(), click())
            await(R.id.total, Money.format(-1000))
            onView(withId(R.id.scope)).check(matches(withText(app.getString(R.string.category_history_scope, app.getString(R.string.all_accounts), app.getString(R.string.net)))))
        }
    }
    @Test fun wheelHitTestingPercentagesReleaseCancelHoleAndEmptyInBothThemes() {
        for (mode in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            main { AppCompatDelegate.setDefaultNightMode(mode) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val wheel = CategoryWheelView(activity)
                    wheel.layout(0, 0, 600, 600)
                    val food = WalletCategory("FOOD", "Food", "utensils", "Peach")
                    val other = WalletCategory("OTHER", "Other", "tag", "Sky")
                    wheel.show(listOf(food to BigInteger.valueOf(-300), other to BigInteger.valueOf(-100)))
                    fun touch(action: Int, x: Float, y: Float) {
                        MotionEvent.obtain(0, 0, action, x, y, 0).let { event -> wheel.onTouchEvent(event); event.recycle() }
                    }
                    touch(MotionEvent.ACTION_DOWN, 500f, 300f)
                    assertTrue(ViewCompat.getStateDescription(wheel).toString().contains("75%"))
                    touch(MotionEvent.ACTION_UP, 500f, 300f)
                    assertNull(ViewCompat.getStateDescription(wheel))
                    touch(MotionEvent.ACTION_DOWN, 100f, 280f)
                    assertTrue(ViewCompat.getStateDescription(wheel).toString().contains("25%"))
                    touch(MotionEvent.ACTION_CANCEL, 100f, 280f)
                    assertNull(ViewCompat.getStateDescription(wheel))
                    touch(MotionEvent.ACTION_DOWN, 300f, 300f)
                    assertNull(ViewCompat.getStateDescription(wheel))
                    touch(MotionEvent.ACTION_DOWN, 0f, 0f)
                    assertNull(ViewCompat.getStateDescription(wheel))
                    touch(MotionEvent.ACTION_DOWN, 500f, 300f)
                    touch(MotionEvent.ACTION_MOVE, 300f, 300f)
                    assertNull(ViewCompat.getStateDescription(wheel))
                    touch(MotionEvent.ACTION_CANCEL, 300f, 300f)
                    assertTrue(wheel.contentDescription.contains("Food"))
                    wheel.show(listOf(food to BigInteger.valueOf(-99999), other to BigInteger.valueOf(-1)))
                    touch(MotionEvent.ACTION_DOWN, 299.99f, 100f)
                    assertTrue(ViewCompat.getStateDescription(wheel).toString().contains("Other"))
                    wheel.show(emptyList())
                    assertNull(ViewCompat.getStateDescription(wheel))
                    touch(MotionEvent.ACTION_DOWN, 500f, 300f)
                    assertNull(ViewCompat.getStateDescription(wheel))
                }
            }
        }
    }
    @Test fun bothThemesAndIconOnlyChoicesRemainAccessible() {
        for ((theme, mode) in listOf("light" to AppCompatDelegate.MODE_NIGHT_NO, "dark" to AppCompatDelegate.MODE_NIGHT_YES)) {
            main { AppCompatDelegate.setDefaultNightMode(mode) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(R.id.today_date); capture(scenario, "$theme-records")
                onView(withId(R.id.accounts_tab)).perform(click()); await(R.id.total_balance)
                onView(withText(app.getString(R.string.opening_balance_label))).check(doesNotExist())
                capture(scenario, "$theme-accounts")
                onView(withId(R.id.analysis_tab)).perform(click()); await(R.id.calendar_grid)
                capture(scenario, "$theme-calendar")
                onView(withId(R.id.analysis_view)).perform(click()); onView(withText("Category Expense Graph")).perform(click())
                onView(withId(R.id.wheel_panel)).perform(scrollTo()); capture(scenario, "$theme-wheel")
            }
            ActivityScenario.launch(CategoryEditorActivity::class.java).use { scenario ->
                await(R.id.icon_choices)
                scenario.onActivity { activity ->
                    val choices = activity.findViewById<android.widget.GridLayout>(R.id.icon_choices)
                    for (i in 0 until choices.childCount) {
                        val cell = choices.getChildAt(i) as LinearLayout
                        assertEquals(1, cell.childCount); assertFalse(cell.contentDescription.isNullOrBlank())
                    }
                }
                onView(withText("Additional Icons")).perform(scrollTo(), click())
                onView(withContentDescription("Car icon")).perform(scrollTo(), click())
                onView(withText("Use selected icon")).perform(click())
                onView(withId(R.id.selection_status)).check(matches(withText("Car · Sky")))
            }
        }
    }
    private fun main(block: () -> Unit) = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun await(id: Int, text: String? = null) {
        val deadline = System.currentTimeMillis() + 10000
        while (true) { try {
            onView(withId(id)).check(matches(if (text == null) withEffectiveVisibility(Visibility.VISIBLE) else withText(text))); return
        } catch (error: Throwable) { if (System.currentTimeMillis() >= deadline) throw error; Thread.sleep(100) } }
    }
    private fun capture(scenario: ActivityScenario<MainActivity>, name: String) {
        scenario.onActivity { activity ->
            val view = activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(app.getExternalFilesDir(null), "v43-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}

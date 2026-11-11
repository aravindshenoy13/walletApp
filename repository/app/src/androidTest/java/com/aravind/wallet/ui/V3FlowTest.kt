package com.aravind.wallet.ui

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.DatePicker
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.*
import com.aravind.wallet.domain.Money
import com.aravind.wallet.domain.RecordType
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V3FlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private var cash = 0L
    private var bank = 0L
    private val today get() = LocalDate.now()
    private val old get() = today.withDayOfMonth(1).minusDays(1)
    @Before fun fixture() = runBlocking {
        WalletDatabase.create(app).let { db ->
            try { db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) } }
            finally { db.close() }
        }
        cash = app.repository.saveAccount(Account(name="Cash",openingBalanceMinor=100000))
        bank = app.repository.saveAccount(Account(name="Bank",openingBalanceMinor=-1000))
        suspend fun record(type: RecordType, amount: Long, account: Long, category: String?, description: String, date: LocalDate = today, destination: Long? = null) {
            app.repository.saveRecord(WalletRecord(type=type,amountMinor=amount,accountId=account,categoryId=category,
                destinationAccountId=destination,description=description,occurredAt=date.atTime(12,0)))
        }
        record(RecordType.EXPENSE,10000,cash,"FOOD","Lunch café")
        record(RecordType.INCOME,2500,cash,"FOOD","Reimbursement")
        record(RecordType.INCOME,20000,bank,"SALARY","Payday")
        record(RecordType.TRANSFER,5000,cash,null,"Move savings",destination=bank)
        record(RecordType.INCOME,99999,cash,"SALARY","Prior month",old)
    }
    @Test fun headerSearchFiltersSamePageAndRestoresWithoutChangingTodayTotals() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(withId(R.id.record_search))
            await(allOf(withId(R.id.income),withText(Money.format(22500))))
            onView(withId(R.id.record_search)).perform(replaceText("lUnCh"),closeSoftKeyboard())
            await(withText("1 matching record"))
            awaitRecords(2)
            onView(withId(R.id.expense)).check(matches(withText(Money.format(10000))))
            scenario.recreate()
            await(withText("1 matching record"))
            awaitRecords(2)
            onView(withId(R.id.record_search)).check(matches(withText("lUnCh")))
            capture("v3-search")
            onView(withId(R.id.record_search)).perform(replaceText("Food"),closeSoftKeyboard())
            await(withText(app.getString(R.string.no_matches)))
            awaitRecords(0)
            onView(withId(R.id.record_search)).perform(replaceText("50.00"),closeSoftKeyboard())
            await(withText("1 matching record"))
            awaitRecords(2)
            onView(withId(R.id.income)).check(matches(withText(Money.format(22500))))
            onView(withId(R.id.clear_search)).perform(click())
            await(withText(app.getString(R.string.all_records)))
            awaitRecords(7)
            onView(withId(R.id.record_search)).check(matches(withText("")))
        }
    }
    @Test fun analysisUsesOneRangeAndAccountScopeAndResetsOnReopening() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await(withId(R.id.analysis_tab))
            onView(withId(R.id.analysis_tab)).perform(click())
            await(allOf(withId(R.id.period_income),withText(Money.format(22500))))
            onView(withId(R.id.period_expense)).check(matches(withText(Money.format(10000))))
            onView(withId(R.id.flow_both)).check(matches(isChecked()))
            onView(withId(R.id.breakdown_net)).perform(scrollTo()).check(matches(isChecked()))
            capture("v3-analysis")
            onView(withId(R.id.flow_income)).perform(scrollTo(),click())
            onView(withId(R.id.breakdown_expense)).perform(scrollTo(),click())
            onView(withId(R.id.analysis_account)).perform(scrollTo(),click())
            onData(equalTo("Cash")).perform(click())
            await(allOf(withId(R.id.period_income),withText(Money.format(2500))))
            onView(withId(R.id.period_net)).check(matches(withText(Money.format(-7500))))
            scenario.recreate()
            await(allOf(withId(R.id.period_income),withText(Money.format(2500))))
            onView(withId(R.id.flow_income)).check(matches(isChecked()))
            onView(withId(R.id.custom_range)).perform(scrollTo(),click())
            await(withId(R.id.start_date))
            setDate(R.id.start_date,old)
            setDate(R.id.end_date,old)
            onView(withId(R.id.apply)).perform(click())
            await(allOf(withId(R.id.period_income),withText(Money.format(99999))))
            onView(withId(R.id.period_expense)).check(matches(withText(Money.format(0))))
            onView(withId(R.id.records_tab)).perform(click())
            onView(withId(R.id.analysis_tab)).perform(click())
            await(allOf(withId(R.id.period_income),withText(Money.format(22500))))
            onView(withId(R.id.period_dates)).check(matches(withText(app.getString(R.string.date_range,today.withDayOfMonth(1).display(),today.display()))))
        }
    }
    @Test fun accountRowsOpenHistoryAndSeparateEditAction() {
        ActivityScenario.launch(MainActivity::class.java).use {
            await(withId(R.id.accounts_tab)); onView(withId(R.id.accounts_tab)).perform(click())
            await(withText("Cash")); onView(allOf(withText("Cash"),isDisplayed())).perform(click())
            await(allOf(withId(R.id.current_balance),withText(Money.format(187499))))
            await(withText("Transfer to Bank"))
            capture("v3-account-history")
            onView(withId(R.id.history_list)).perform(scrollLast())
            await(withText(app.getString(R.string.opening_balance_label)))
            onView(withId(R.id.back)).perform(click())
            await(withText("Bank")); onView(allOf(withText("Bank"),isDisplayed())).perform(click())
            await(withText("Transfer from Cash"))
            onView(allOf(withId(R.id.amount),withText(app.getString(R.string.income_amount,Money.format(5000))))).check(matches(isDisplayed()))
            onView(withId(R.id.back)).perform(click())
            onView(withContentDescription("Edit Cash")).perform(click())
            await(withId(R.id.name)); onView(withId(R.id.name)).check(matches(withText("Cash")))
            onView(withId(R.id.back)).perform(click())
        }
    }
    @Test fun reversedCustomRangeIsRejectedAndDraftSurvivesRecreation() {
        ActivityScenario.launch<DateRangeActivity>(Intent(app,DateRangeActivity::class.java)
            .putExtra("start",today.toString()).putExtra("end",today.minusDays(1).toString())).use { scenario ->
            onView(withId(R.id.apply)).perform(click())
            onView(withId(R.id.error)).check(matches(isDisplayed()))
            scenario.recreate()
            onView(withId(R.id.start_date)).check(matches(withText(today.display())))
            onView(withId(R.id.end_date)).check(matches(withText(today.minusDays(1).display())))
        }
    }
    private fun setDate(id: Int,date: LocalDate) {
        onView(withId(id)).perform(click())
        await(isAssignableFrom(DatePicker::class.java))
        onView(isAssignableFrom(DatePicker::class.java)).perform(object: ViewAction {
            override fun getConstraints() = isAssignableFrom(DatePicker::class.java)
            override fun getDescription() = "Set analysis date"
            override fun perform(ui: UiController,view: View) { (view as DatePicker).updateDate(date.year,date.monthValue-1,date.dayOfMonth); ui.loopMainThreadUntilIdle() }
        })
        onView(withId(android.R.id.button1)).perform(click())
        await(allOf(withId(id),withText(date.display())))
    }
    private fun scrollLast() = object: ViewAction {
        override fun getConstraints() = isAssignableFrom(RecyclerView::class.java)
        override fun getDescription() = "Show opening balance"
        override fun perform(ui: UiController,view: View) { val list=view as RecyclerView; list.scrollToPosition(list.adapter!!.itemCount-1); ui.loopMainThreadUntilIdle() }
    }
    private fun await(matcher: Matcher<View>) {
        val until=System.currentTimeMillis()+12000
        while(true) { try { onView(allOf(matcher,withEffectiveVisibility(Visibility.VISIBLE))).check(matches(isEnabled())); return }
            catch(error: Throwable) { if(System.currentTimeMillis()>=until) throw error; Thread.sleep(50) } }
    }
    private fun awaitRecords(count: Int) {
        val until = System.currentTimeMillis() + 12000
        while (true) {
            try {
                onView(withId(R.id.record_list)).check { view, error ->
                    if (error != null) throw error
                    val list = view as RecyclerView
                    assertEquals(count, list.adapter!!.itemCount)
                    assertFalse("Wait for list layout", list.isLayoutRequested)
                    assertFalse("Wait for list animations", list.isAnimating)
                    if (count <= 2) assertEquals(count, list.childCount)
                }
                return
            } catch (error: Throwable) { if (System.currentTimeMillis() >= until) throw error; Thread.sleep(50) }
        }
    }
    private fun capture(name:String) {
        val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file=File(app.getExternalFilesDir(null),"qa/$name.png"); file.parentFile?.mkdirs()
        file.outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }; shot.recycle()
    }
}

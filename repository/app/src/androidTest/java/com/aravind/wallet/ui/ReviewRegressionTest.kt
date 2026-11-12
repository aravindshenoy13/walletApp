package com.aravind.wallet.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.*
import com.aravind.wallet.databinding.PanelAnalysisBinding
import com.aravind.wallet.domain.*
import java.io.File
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewRegressionTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun reset() {
        WalletDatabase.create(app).let { db ->
            try { db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) } }
            finally { db.close() }
        }
        val dark = InstrumentationRegistry.getArguments().getString("reviewDark") == "true"
        app.getSharedPreferences("appearance", 0).edit().putBoolean("dark", dark).commit()
        instrumentation.runOnMainSync { androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if (dark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) }
    }
    private suspend fun record(description: String = "Target", date: LocalDateTime = LocalDateTime.now().withNano(0)): Long {
        val account = app.repository.snapshot().accounts.firstOrNull()?.id ?: app.repository.saveAccount(Account(name = "Bank", openingBalanceMinor = 0))
        return app.repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = 123, accountId = account, categoryId = "FOOD", occurredAt = date, description = description))
    }

    @Test fun recreatedExportModelKeepsSubsetBeforeAnyAccountLoad(): Unit = runBlocking {
        val included = app.repository.saveAccount(Account(name = "Included", openingBalanceMinor = 0))
        val excluded = app.repository.saveAccount(Account(name = "Excluded", openingBalanceMinor = 0))
        for (id in listOf(included, excluded)) app.repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = 123, accountId = id, categoryId = "FOOD", occurredAt = LocalDateTime.now().withNano(0), description = "scope"))
        val handle = SavedStateHandle(mapOf("accounts" to longArrayOf(included), "exportType" to "EXPENSE"))
        val model = ExportViewModel(app, handle)
        val store = ViewModelStore()
        val file = File(app.cacheDir, "review-subset.csv")
        try {
            instrumentation.runOnMainSync { store.put("export", model); model.selected(Uri.fromFile(file)) }
            assertEquals(ExportState.Saved(1), withTimeout(10000) { model.state.first { it is ExportState.Saved || it == ExportState.Failed } })
            assertTrue(file.readText().contains("Included")); assertFalse(file.readText().contains("Excluded"))
            instrumentation.runOnMainSync {
                val uninitialized = ExportViewModel(app)
                assertFalse(uninitialized.choose())
                uninitialized.accountIds = emptySet()
                assertFalse(uninitialized.choose())
            }
        } finally { instrumentation.runOnMainSync { store.clear() }; file.delete() }
    }

    @Test fun backupLimitsAndFractionalTimestampsAreExplicit(): Unit = runBlocking {
        val ledger = app.repository.snapshot()
        assertEquals(ledger, WalletBackup.decode(WalletBackup.encodeBytes(ledger).toString(Charsets.UTF_8)))
        val huge = ledger.copy(categories = listOf(ledger.categories.first().copy(name = "x".repeat(WalletBackup.MAX_BYTES))))
        try { WalletBackup.encodeBytes(huge); fail("Oversized backup accepted") } catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("20 MB")) }
        val id = record()
        val snapshot = app.repository.snapshot()
        val fractional = snapshot.copy(records = snapshot.records.map { if (it.id == id) it.copy(occurredAt = it.occurredAt.withNano(123456789)) else it })
        try { WalletBackup.decode(WalletBackup.encode(fractional)); fail("Fractional precision silently accepted") }
        catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("whole seconds")) }
    }

    @Test fun failedLedgerAndExportLoadsCanBeRetried(): Unit = runBlocking {
        record()
        val db = WalletDatabase.create(app)
        try {
            db.openHelper.writableDatabase.execSQL("UPDATE records SET occurredAt='invalid'")
            ActivityScenario.launch(MainActivity::class.java).use {
                await { onView(withId(R.id.error)).check(matches(isDisplayed())) }
                db.openHelper.writableDatabase.execSQL("UPDATE records SET occurredAt='2026-11-12T10:30:00'")
                onView(withId(R.id.error)).perform(click())
                await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
                onView(withId(R.id.error)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            }
            db.openHelper.writableDatabase.execSQL("UPDATE records SET occurredAt='invalid'")
            ActivityScenario.launch(ExportActivity::class.java).use {
                await { onView(withId(R.id.retry)).check(matches(isDisplayed())) }
                onView(withId(R.id.save)).check(matches(org.hamcrest.Matchers.not(isEnabled())))
                db.openHelper.writableDatabase.execSQL("UPDATE records SET occurredAt='2026-11-12T10:30:00'")
                onView(withId(R.id.retry)).perform(click())
                await { onView(withId(R.id.save)).check(matches(isEnabled())) }
            }
        } finally { db.close() }
    }

    @Test fun rolloverRefreshesDefaultsAndPreservesExplicitPeriods() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario -> scenario.onActivity { activity ->
            val saved = Bundle().apply {
                putString("analysisStart", "2026-10-01"); putString("analysisEnd", "2026-10-31")
                putString("calendarMonth", "2026-10"); putBoolean("calendarCustom", false)
            }
            val binding = PanelAnalysisBinding.inflate(activity.layoutInflater)
            val panel = AnalysisPanel(activity, binding, saved) { _, _ -> }
            panel.show(Ledger(emptyList(), emptyList()))
            panel.refreshDefaults(LocalDate.of(2026, 11, 1))
            assertEquals(LocalDate.of(2026, 11, 1), panel.start)
            assertEquals(panel.start, panel.end)
            assertTrue(binding.calendarMonth.text.toString().contains("2026"))
            binding.previousMonth.performClick()
            val month = binding.calendarMonth.text.toString()
            panel.range(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            panel.refreshDefaults(LocalDate.of(2026, 12, 1))
            assertEquals(LocalDate.of(2025, 1, 1), panel.start)
            assertEquals(month, binding.calendarMonth.text.toString())
        } }
    }

    @Test fun filteredRowTapOpensTheIntendedHistoricalRecord(): Unit = runBlocking {
        record("Newest")
        record("Needle", LocalDateTime.now().minusDays(30).withNano(0))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
            onView(withId(R.id.open_search)).perform(click())
            onView(withId(R.id.record_search)).perform(replaceText("Needle"))
            await { onView(withId(R.id.record_count)).check(matches(withText("1 matching record"))) }
            await { onView(withId(R.id.meta)).check(matches(withText(org.hamcrest.Matchers.containsString("Needle")))) }
            await { scenario.onActivity { activity ->
                val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.record_list)
                assertFalse(list.isComputingLayout)
                assertFalse(list.itemAnimator?.isRunning == true)
            } }
            await { onView(org.hamcrest.Matchers.allOf(withId(R.id.description), withText("Food"), isDisplayed())).perform(click()) }
            await { onView(withId(R.id.description)).check(matches(withText("Needle"))) }
        }
    }

    @Test fun categoryPickerMarksTheUnsavedSelectionAndEditorHidesOrigin(): Unit = runBlocking {
        val id = record()
        ActivityScenario.launch<RecordEditorActivity>(Intent(app, RecordEditorActivity::class.java).putExtra("id", id)).use {
            await { onView(withId(R.id.category)).check(matches(isDisplayed())) }
            onView(withId(R.id.category)).perform(click())
            await { onView(withText("Food")).check { view, _ -> assertTrue((view.parent as View).isActivated) } }
            onView(withText("Groceries")).perform(click())
            onView(withId(R.id.category)).perform(click())
            await { onView(withText("Groceries")).check { view, _ -> assertTrue((view.parent as View).isActivated) } }
            onView(withId(R.id.back)).perform(click())
        }
        ActivityScenario.launch<CategoryEditorActivity>(Intent(app, CategoryEditorActivity::class.java).putExtra("id", "FOOD")).use {
            await { onView(withId(R.id.save)).check(matches(isEnabled())) }
            onView(withId(R.id.origin)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test fun wheelClearsEmptyDescriptionAndReleasesScrollDrag() {
        instrumentation.runOnMainSync {
            var intercept = false
            val parent = object : LinearLayout(app) {
                override fun requestDisallowInterceptTouchEvent(disallow: Boolean) { intercept = disallow; super.requestDisallowInterceptTouchEvent(disallow) }
            }
            val wheel = CategoryWheelView(app)
            parent.addView(wheel); wheel.layout(0, 0, 400, 400)
            val category = V2CategorySeeds.rows.first()
            wheel.show(listOf(category to BigInteger.valueOf(-100)))
            fun event(action: Int, x: Float, y: Float) { MotionEvent.obtain(0, 1, action, x, y, 0).let { wheel.onTouchEvent(it); it.recycle() } }
            event(MotionEvent.ACTION_DOWN, 360f, 200f); assertTrue(intercept)
            event(MotionEvent.ACTION_MOVE, 360f, 100f); assertFalse(intercept)
            wheel.show(emptyList())
            assertTrue(wheel.contentDescription.contains(app.getString(R.string.no_category_values)))
        }
    }

    @Test fun graphAccessibilityExposesDailyValuesAndActiveTotal() {
        instrumentation.runOnMainSync {
            val start = LocalDate.of(2026, 11, 1)
            val ledger = Ledger(listOf(Account(1, "Bank", 0)), listOf(WalletRecord(1, RecordType.EXPENSE, 123, 1, null, "FOOD", start.atStartOfDay(), "")), V2CategorySeeds.rows)
            val graph = DailyFlowView(app)
            graph.show(start, start.plusDays(1), ledger.analyze(start, start.plusDays(1)), BreakdownMode.NET)
            assertTrue(graph.contentDescription.contains(Money.format(-123)))
            assertTrue(graph.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
            assertTrue(graph.contentDescription.contains(start.plusDays(1).display()))
            assertTrue(graph.contentDescription.contains(Money.format(0)))
            assertFalse(graph.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null))
        }
    }

    @Test fun largeTextSearchCalendarAndAmountsRemainWithinTheirBounds(): Unit = runBlocking {
        val account = app.repository.saveAccount(Account(name = "Large account", openingBalanceMinor = Long.MAX_VALUE))
        app.repository.saveRecord(WalletRecord(type = RecordType.EXPENSE, amountMinor = Long.MAX_VALUE, accountId = account, categoryId = "FOOD", occurredAt = LocalDateTime.now().withNano(0), description = "Large expense"))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
            onView(withId(R.id.open_search)).perform(click())
            onView(withId(R.id.record_search)).perform(replaceText("test"))
            scenario.onActivity { activity ->
                val search = activity.findViewById<View>(R.id.record_search)
                assertTrue("Usable search entry width", search.width >= 100 * activity.resources.displayMetrics.density)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.title).visibility)
            }
            capture("search")
            onView(withId(R.id.record_search)).perform(pressImeActionButton())
            onView(withId(R.id.accounts_tab)).perform(click())
            capture("accounts")
            onView(withId(R.id.records_tab)).perform(click())
            onView(withId(R.id.net)).perform(scrollTo())
            capture("records")
            onView(withId(R.id.add_record_button)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val income = activity.findViewById<TextView>(R.id.income)
                val root = activity.findViewById<View>(R.id.root)
                assertTrue(income.right <= root.width)
                val net = activity.findViewById<TextView>(R.id.net)
                val visible = android.graphics.Rect()
                assertTrue("Net total is reachable", net.getGlobalVisibleRect(visible))
                val location = IntArray(2)
                net.getLocationOnScreen(location)
                val text = net.text.toString()
                for (line in 0 until net.lineCount) {
                    val glyph = android.graphics.Rect()
                    net.paint.getTextBounds(text, net.layout.getLineStart(line), net.layout.getLineEnd(line), glyph)
                    val baseline = location[1] + net.totalPaddingTop + net.layout.getLineBaseline(line)
                    assertTrue("Net glyphs are not clipped at the top", baseline + glyph.top >= visible.top)
                    assertTrue("Net glyphs are not clipped at the bottom", baseline + glyph.bottom <= visible.bottom)
                }
            }
            onView(withId(R.id.analysis_tab)).perform(click())
            instrumentation.waitForIdleSync()
            scrollAnalysis(scenario, R.id.calendar_grid)
            capture("calendar")
            scenario.onActivity { it.findViewById<android.widget.Spinner>(R.id.analysis_view).setSelection(2) }
            instrumentation.waitForIdleSync()
            scrollAnalysis(scenario, R.id.flow_graph)
            onView(withId(R.id.flow_graph)).check(matches(isCompletelyDisplayed()))
            capture("graph")
        }
        assertTrue(account > 0)
    }

    @Test fun editorTimeMatchesBothDeviceClockPreferences(): Unit = runBlocking {
        val time = LocalDateTime.of(2026, 11, 12, 15, 7)
        val id = record(date = time)
        val old = android.provider.Settings.System.getString(app.contentResolver, android.provider.Settings.System.TIME_12_24)
        fun shell(command: String) { android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() } }
        try {
            for (format in listOf("12", "24")) {
                shell("settings put system time_12_24 $format")
                ActivityScenario.launch<RecordEditorActivity>(Intent(app, RecordEditorActivity::class.java).putExtra("id", id)).use { scenario ->
                    await { onView(withId(R.id.save)).check(matches(isEnabled())) }
                    scenario.onActivity { activity ->
                        val expected = android.text.format.DateFormat.getTimeFormat(activity).format(java.util.Date.from(time.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        assertEquals(expected, activity.findViewById<TextView>(R.id.time).text.toString())
                    }
                }
            }
        } finally {
            shell(if (old == null) "settings delete system time_12_24" else "settings put system time_12_24 $old")
        }
    }

    private fun scrollAnalysis(scenario: ActivityScenario<MainActivity>, id: Int) {
        scenario.onActivity { activity ->
            val scroll = activity.findViewById<android.widget.ScrollView>(R.id.analysis_panel)
            val target = activity.findViewById<View>(id)
            val targetLocation = IntArray(2); target.getLocationOnScreen(targetLocation)
            val scrollLocation = IntArray(2); scroll.getLocationOnScreen(scrollLocation)
            scroll.scrollTo(0, scroll.scrollY + targetLocation[1] - scrollLocation[1])
        }
        instrumentation.waitForIdleSync()
    }
    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(250) // Allow the rendered frame to reach UiAutomation screenshots.
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(app.getExternalFilesDir(null), "review-$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun await(block: () -> Unit) {
        val deadline = System.currentTimeMillis() + 15000
        while (true) { try { block(); return } catch (e: Throwable) {
            if (System.currentTimeMillis() >= deadline) throw e
            Thread.sleep(100)
        } }
    }
}

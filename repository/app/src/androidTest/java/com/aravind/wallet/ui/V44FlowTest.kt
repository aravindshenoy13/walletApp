package com.aravind.wallet.ui

import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.*
import com.aravind.wallet.domain.CategoryCatalog
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V44FlowTest {
    private val app get() = ApplicationProvider.getApplicationContext<WalletApplication>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun setup() {
        WalletDatabase.create(app).let { db ->
            try { db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) } }
            finally { db.close() }
        }
        app.getSharedPreferences("appearance", 0).edit().putBoolean("dark", false).commit()
        mode(AppCompatDelegate.MODE_NIGHT_NO)
    }
    @After fun cleanup() { mode(AppCompatDelegate.MODE_NIGHT_NO) }

    @Test fun everyCategoryColorIsOpaqueAndReadableInBothThemes() {
        for (theme in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            mode(theme)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    for (choice in CategoryCatalog.colors) {
                        val color = categoryDisplayColor(activity, choice.key)
                        assertEquals(choice.key, 255, Color.alpha(color))
                        if (theme == AppCompatDelegate.MODE_NIGHT_YES)
                            assertTrue(choice.key, ColorUtils.calculateContrast(Color.WHITE, color) >= 3.0)
                        else assertEquals(Color.parseColor(choice.hex), color)
                    }
                }
            }
        }
    }

    @Test fun navigationArtworkAndTouchTargetsStayAlignedInBothThemes() {
        for (theme in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            mode(theme)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
                scenario.onActivity { activity ->
                    val density = activity.resources.displayMetrics.density
                    val buttons = listOf(R.id.records_tab, R.id.analysis_tab, R.id.accounts_tab, R.id.categories_tab)
                        .map { activity.findViewById<android.widget.Button>(it) }
                    assertEquals(1, buttons.map { it.top }.distinct().size)
                    assertEquals(1, buttons.map { it.height }.distinct().size)
                    for (button in buttons) {
                        assertTrue(button.width >= 48 * density && button.height >= 48 * density)
                        assertEquals("Unwrapped navigation label", 1, button.lineCount)
                        val icon = requireNotNull(button.compoundDrawables[1])
                        val bitmap = android.graphics.Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight,
                            android.graphics.Bitmap.Config.ARGB_8888)
                        icon.setBounds(0, 0, bitmap.width, bitmap.height)
                        icon.draw(android.graphics.Canvas(bitmap))
                        var left = bitmap.width; var right = 0; var top = bitmap.height; var bottom = 0
                        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                            if (Color.alpha(bitmap.getPixel(x, y)) > 100) {
                                left = minOf(left, x); right = maxOf(right, x)
                                top = minOf(top, y); bottom = maxOf(bottom, y)
                            }
                        }
                        assertTrue("Visible icon width", (right - left + 1) / density in 17f..21f)
                        assertTrue("Visible icon height", (bottom - top + 1) / density in 19f..21f)
                        assertEquals("Centered artwork", bitmap.width / 2f, (left + right + 1) / 2f, density)
                        assertEquals("Centered artwork", bitmap.height / 2f, (top + bottom + 1) / 2f, density)
                        bitmap.recycle()
                    }
                }
                onView(withId(R.id.categories_tab)).perform(click())
                onView(withId(R.id.categories_tab)).check(matches(isSelected()))
                onView(withId(R.id.records_tab)).check(matches(org.hamcrest.Matchers.not(isSelected())))
                onView(withId(R.id.records_tab)).perform(click())
                background(scenario, "$theme-navigation")
            }
        }
    }

    @Test fun emptySearchRestoresAppearanceAfterEveryDismissalInBothThemes() {
        for (theme in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            mode(theme)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
                val before = background(scenario, "$theme-before")
                for (dismissal in listOf("back", "done", "outside", "keyboard")) {
                    onView(withId(R.id.open_search)).perform(click())
                    onView(withId(R.id.record_search)).perform(typeText("999"))
                    onView(withId(R.id.empty_records)).check(matches(withText(R.string.no_matches)))
                    onView(withId(R.id.clear_search)).perform(click())
                    onView(withId(R.id.record_search)).check(matches(withText("")))
                    assertEquals("$theme clear background", before, background(scenario, "$theme-$dismissal-clear"))
                    onView(withId(R.id.record_search)).perform(typeText("123"))
                    when (dismissal) {
                        "back" -> androidx.test.espresso.Espresso.pressBack()
                        "done" -> onView(withId(R.id.record_search)).perform(pressImeActionButton())
                        "outside" -> onView(withId(R.id.record_count)).perform(click())
                        else -> scenario.onActivity { activity ->
                            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .hideSoftInputFromWindow(activity.findViewById<View>(R.id.record_search).windowToken, 0)
                        }
                    }
                    await { onView(withId(R.id.open_search)).check(matches(isDisplayed())) }
                    await { scenario.onActivity { activity ->
                        assertFalse(ViewCompat.getRootWindowInsets(activity.findViewById(R.id.root))
                            ?.isVisible(WindowInsetsCompat.Type.ime()) == true)
                    } }
                    onView(withId(R.id.record_count)).check(matches(withText(R.string.all_records)))
                    assertEquals("$theme $dismissal background", before, background(scenario, "$theme-$dismissal"))
                }
            }
        }
    }

    @Test fun emptyAndPopulatedWalletsOpenAndSaveRecordsAndCategoriesInBothThemes() = runBlocking {
        for (theme in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
            setup(); mode(theme)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await { onView(withId(R.id.add_record_button)).check(matches(isEnabled())) }
                onView(withId(R.id.categories_tab)).perform(click())
                await { onView(org.hamcrest.Matchers.allOf(withText("Food"), isDisplayed())).check(matches(isDisplayed())) }
                onView(org.hamcrest.Matchers.allOf(withText("Food"), isDisplayed())).perform(click())
                await { onView(withId(R.id.name)).check(matches(withText("Food"))) }
                onView(withId(R.id.save)).perform(scrollTo(), click())
                await { onView(withId(R.id.categories_tab)).check(matches(isDisplayed())) }
                onView(withId(R.id.records_tab)).perform(click())
                onView(withId(R.id.add_record_button)).perform(click())
                onView(withId(android.R.id.button1)).perform(click())
                await { onView(withId(R.id.name)).check(matches(isDisplayed())) }
                onView(withId(R.id.name)).perform(replaceText("V44 test bank"))
                onView(withId(R.id.opening_balance)).perform(replaceText("1000"))
                onView(withId(R.id.save)).perform(scrollTo(), click())
                await { onView(withId(R.id.records_tab)).check(matches(isDisplayed())) }
                onView(withId(R.id.records_tab)).perform(click())
                onView(withId(R.id.add_record_button)).perform(click())
                await { onView(withId(R.id.amount)).check(matches(isDisplayed())) }
                onView(withId(R.id.amount)).perform(replaceText("25.50"))
                onView(withId(R.id.description)).perform(replaceText("V44 disposable expense"))
                onView(withId(R.id.save)).perform(scrollTo(), click())
                await { onView(withId(R.id.expense)).check(matches(withText(com.aravind.wallet.domain.Money.format(2550)))) }
                onView(withId(R.id.categories_tab)).perform(click())
                await { onView(org.hamcrest.Matchers.allOf(withText("Food"), isDisplayed())).check(matches(isDisplayed())) }
                onView(withId(R.id.add)).perform(click())
                await { onView(withId(R.id.name)).check(matches(isDisplayed())) }
                onView(withId(R.id.name)).perform(replaceText("V44 test category"))
                onView(withId(R.id.save)).perform(scrollTo(), click())
                await { onView(withId(R.id.categories_tab)).check(matches(isDisplayed())) }
                onView(withId(R.id.records_tab)).perform(click())
                onView(withId(R.id.open_search)).perform(click())
                onView(withId(R.id.record_search)).perform(replaceText("V44 disposable"))
                onView(withId(R.id.record_count)).check(matches(withText("1 matching record")))
                onView(withId(R.id.record_search)).perform(replaceText("no-match"))
                onView(withId(R.id.empty_records)).check(matches(withText(R.string.no_matches)))
                onView(withId(R.id.record_search)).perform(pressImeActionButton())
                onView(withId(R.id.record_count)).check(matches(withText(R.string.all_records)))
                onView(org.hamcrest.Matchers.allOf(withText("Food"), isDisplayed())).perform(click())
                await { onView(withId(R.id.amount)).check(matches(withText("25.50"))) }
                onView(withId(R.id.amount)).perform(scrollTo(), replaceText("30"))
                onView(withId(R.id.save)).perform(scrollTo(), click())
                await { onView(withId(R.id.expense)).check(matches(withText(com.aravind.wallet.domain.Money.format(3000)))) }
            }
            val snapshot = app.repository.snapshot()
            assertEquals(1, snapshot.accounts.size)
            assertEquals(100000L, snapshot.accounts.single().openingBalanceMinor)
            assertEquals(3000L, snapshot.records.single().amountMinor)
            assertTrue(snapshot.categories.any { it.name == "V44 test category" })
        }
    }

    private fun background(scenario: ActivityScenario<MainActivity>, name: String): Int {
        instrumentation.waitForIdleSync()
        Thread.sleep(400)
        val bounds = Rect()
        scenario.onActivity { it.findViewById<View>(R.id.records_panel).getGlobalVisibleRect(bounds) }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(app.getExternalFilesDir(null), "v44-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        // The empty panel margin stays visible even when larger text and the IME
        // leave no height for the list itself.
        val color = bitmap.getPixel(bounds.left + 2, bounds.top + 2)
        bitmap.recycle()
        return color
    }
    private fun mode(value: Int) = instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(value) }
    private fun await(block: () -> Unit) {
        val deadline = System.currentTimeMillis() + 10000
        while (true) { try { block(); return } catch (e: Throwable) {
            if (System.currentTimeMillis() >= deadline) throw e
            Thread.sleep(100)
        } }
    }
}

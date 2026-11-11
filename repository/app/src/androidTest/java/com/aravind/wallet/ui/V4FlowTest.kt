package com.aravind.wallet.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.aravind.wallet.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V4FlowTest {
    @Test fun compactSearchAndSettings() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.open_search)).check(matches(isDisplayed())).perform(click())
            onView(withId(R.id.record_search)).perform(typeText("123"), closeSoftKeyboard())
            onView(withId(R.id.clear_search)).perform(click())
            onView(withId(R.id.record_search)).check(matches(withText("")))
            androidx.test.espresso.Espresso.pressBack()
            onView(withId(R.id.open_search)).check(matches(isDisplayed()))
            onView(withId(R.id.settings)).perform(click())
            onView(withText("Dark mode")).check(matches(isDisplayed())).perform(click())
            onView(withText("Dark mode")).check(matches(isChecked())).perform(click())
            onView(withText("Export CSV")).check(matches(isDisplayed()))
            onView(withText("Back")).perform(click())
        }
    }
    @Test fun additionalIconSelection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ActivityScenario.launch<AdditionalIconsActivity>(Intent(context, AdditionalIconsActivity::class.java).putExtra("icon", "plane").putExtra("color", "Peach")).use { scenario ->
            onView(withText("Travel")).check(matches(isChecked()))
            onView(withText("Music")).perform(click()).check(matches(isChecked()))
            onView(withText("Travel")).check(matches(isNotChecked()))
            scenario.recreate()
            onView(withText("Music")).check(matches(isChecked()))
        }
    }
}

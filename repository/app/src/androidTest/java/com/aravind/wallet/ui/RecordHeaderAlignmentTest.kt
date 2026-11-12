package com.aravind.wallet.ui

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.core.view.doOnLayout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.R
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordHeaderAlignmentTest {
    @Test fun saveBackAndTitleShareVerticalCenterInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        try {
            for (mode in listOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)) {
                instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(mode) }
                ActivityScenario.launch(RecordEditorActivity::class.java).use { scenario ->
                    val laidOut = CountDownLatch(1)
                    scenario.onActivity { activity ->
                        activity.findViewById<View>(R.id.save).doOnLayout { laidOut.countDown() }
                    }
                    assertTrue("Header must be laid out", laidOut.await(10, TimeUnit.SECONDS))
                    scenario.onActivity { activity ->
                        val save = activity.findViewById<View>(R.id.save)
                        val header = save.parent as ViewGroup
                        val center = header.height / 2f
                        val tolerance = activity.resources.displayMetrics.density
                        for (id in listOf(R.id.back, R.id.title, R.id.save)) {
                            val child = activity.findViewById<View>(id)
                            assertTrue("Header child $id must be vertically centered",
                                abs(child.y + child.height / 2f - center) <= tolerance)
                            assertTrue("Header child $id must fit inside header",
                                child.top >= 0 && child.bottom <= header.height)
                        }
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        }
    }
}

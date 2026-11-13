package com.aravind.wallet.ui
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.R
import com.aravind.wallet.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class V6FlowTest {
 @org.junit.Before fun theme() {
  val dark=InstrumentationRegistry.getArguments().getString("v6Dark")=="true"
  InstrumentationRegistry.getInstrumentation().runOnMainSync {
   androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if(dark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
  }
 }
 private fun capture(name: String) {
  val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
  java.io.File(app.getExternalFilesDir(null),name+".png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
  bitmap.recycle()
 }
 private val app get()=ApplicationProvider.getApplicationContext<WalletApplication>()
 private fun views(v: View): List<View> = listOf(v) + if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
 private fun await(condition:()->Boolean) { val end=System.currentTimeMillis()+15000; while(!condition()) { check(System.currentTimeMillis()<end) { "Timed out" }; Thread.sleep(100) } }
 @Test fun bulkValidationRecreationReviewBackAndSave(): Unit= runBlocking {
  val db=WalletDatabase.create(app); db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) }; db.close()
  app.repository.saveAccount(Account(name="Cash",openingBalanceMinor=10000))
  val scenario=ActivityScenario.launch(BulkEntryActivity::class.java)
  try {
   await { var ready=false; scenario.onActivity { ready=views(it.window.decorView).any { v -> v.id==R.id.amount } }; ready }
   scenario.onActivity { a ->
    val all=views(a.window.decorView)
    (all.first { it is Button && it.text=="Review" } as Button).performClick()
    assertTrue(views(a.window.decorView).filterIsInstance<android.widget.TextView>().any { it.text.toString().startsWith("Record 1:") })
    (all.first { it.id==R.id.amount } as EditText).setText("100+25")
   }
   scenario.recreate()
   await { var ready=false; scenario.onActivity { ready=views(it.window.decorView).filterIsInstance<EditText>().any { v -> v.id==R.id.amount && v.text.toString()=="100+25" } }; ready }
   capture("v6-bulk")
   scenario.onActivity { a -> (views(a.window.decorView).first { it is Button && it.text=="Review" } as Button).performClick() }
   scenario.onActivity { a ->
    assertTrue(views(a.window.decorView).filterIsInstance<android.widget.TextView>().any { it.text.toString().contains("Net") })
    a.onBackPressedDispatcher.onBackPressed()
    assertEquals("100+25",(views(a.window.decorView).first { it.id==R.id.amount } as EditText).text.toString())
    (views(a.window.decorView).first { it is Button && it.text=="Review" } as Button).performClick()
    (views(a.window.decorView).first { it is Button && it.text=="Confirm and save all" } as Button).apply { performClick(); performClick() }
   }
   await { runBlocking { app.repository.snapshot().records.size==1 } }
   assertEquals(12500L,app.repository.snapshot().records.single().amountMinor)
  } finally { scenario.close() }
 }
 @Test fun calendarDayFiltersAndClearKeepSearch(): Unit=runBlocking {
  val db=WalletDatabase.create(app); db.clearAllTables(); db.runInTransaction { V2CategorySeeds.insert(db.openHelper.writableDatabase) }; db.close()
  val id=app.repository.saveAccount(Account(name="Cash",openingBalanceMinor=0))
  val now=java.time.LocalDateTime.now().withSecond(0).withNano(0)
  app.repository.saveRecords(listOf(
   WalletRecord(type=com.aravind.wallet.domain.RecordType.EXPENSE,amountMinor=100,accountId=id,occurredAt=now,description="Today"),
   WalletRecord(type=com.aravind.wallet.domain.RecordType.EXPENSE,amountMinor=200,accountId=id,occurredAt=now.minusDays(1),description="Yesterday")))
  val scenario=ActivityScenario.launch(MainActivity::class.java)
  try {
   await { var ready=false; scenario.onActivity { ready=it.findViewById<View>(R.id.records_panel).visibility==View.VISIBLE }; ready }
   capture("v6-records")
   scenario.onActivity { a ->
    a.findViewById<View>(R.id.analysis_tab).performClick()
    val cell=views(a.window.decorView).first { it is android.widget.LinearLayout && it.isClickable && it.contentDescription?.toString()?.startsWith(now.toLocalDate().display())==true }
    cell.performClick()
    assertEquals(View.VISIBLE,a.findViewById<View>(R.id.clear_filters).visibility)
    assertEquals(View.GONE,a.findViewById<View>(R.id.empty_records).visibility)
    a.findViewById<EditText>(R.id.record_search).setText("Yesterday")
    assertEquals(View.VISIBLE,a.findViewById<View>(R.id.empty_records).visibility)
    a.findViewById<View>(R.id.clear_filters).performClick()
    assertEquals("Yesterday",a.findViewById<EditText>(R.id.record_search).text.toString())
   }
  } finally { scenario.close() }
 }

}

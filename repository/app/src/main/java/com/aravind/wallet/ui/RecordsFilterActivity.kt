package com.aravind.wallet.ui
import android.os.Bundle
import android.content.Intent
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.aravind.wallet.R
import java.time.LocalDate
import kotlinx.coroutines.launch

class RecordsFilterActivity : AppCompatActivity() {
 private val model: WalletViewModel by viewModels()
 private var day: String?=null
 private var category: String?=null
 private var account=0L
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  day=if(savedInstanceState!=null) savedInstanceState.getString("day") else intent.getStringExtra("day")
  category=if(savedInstanceState!=null) savedInstanceState.getString("category") else intent.getStringExtra("category")
  account=savedInstanceState?.getLong("account") ?: intent.getLongExtra("account",0)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding((16*resources.displayMetrics.density).toInt(),0,(16*resources.displayMetrics.density).toInt(),0); setBackgroundColor(getColor(R.color.paper)) }; val insetRoot=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(root) }; setContentView(insetRoot); applyInsets(insetRoot)
  fun button(label: String, action:()->Unit): Button = Button(this,null,0,R.style.OutlineButton).apply { text=label; setOnClickListener { action() }; root.addView(this,LinearLayout.LayoutParams(-1,-2)) }
  val header=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL }; root.addView(header)
  header.addView(Button(this,null,0,R.style.PlainButton).apply { text="Back"; setOnClickListener { finish() } },LinearLayout.LayoutParams(-2,-2))
  header.addView(TextView(this).apply { text="Records filters"; textSize=20f; setTextColor(getColor(R.color.ink)) },LinearLayout.LayoutParams(0,-2,1f))
  val categoryButton=button("Category") { }
  val dayButton=button(day ?: "Any day") { }
  dayButton.setOnClickListener { chooseDate(day?.let(LocalDate::parse) ?: LocalDate.now()) { day=it.toString(); dayButton.text=day } }
  button("Any day") { day=null; dayButton.text="Any day" }
  val accountButton=button("Account") { }
  val error=TextView(this); root.addView(error); error.setOnClickListener { model.retry() }
  val apply=button("Apply") { setResult(RESULT_OK,Intent().putExtra("day",day).putExtra("category",category).putExtra("account",account)); finish() }; apply.isEnabled=false; apply.setBackgroundResource(R.drawable.action_save); apply.setTextColor(getColor(R.color.action_save))
  lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { model.ledger.collect { state ->
   apply.isEnabled=state is LedgerState.Ready
   if(state==LedgerState.Failed) error.text="Could not load wallet. Tap to retry."
   if(state is LedgerState.Ready) {
    error.text=""
    val current=state.ledger
    fun labels() { categoryButton.text=if(category==null) "Any category" else current.categories.find { it.id==category }?.name ?: "Uncategorised"; accountButton.text=current.accounts.find { it.id==account }?.name ?: "All accounts" }
    labels()
    categoryButton.setOnClickListener { androidx.appcompat.app.AlertDialog.Builder(this@RecordsFilterActivity).setTitle("Category").setItems((listOf("Any category","Uncategorised")+current.categories.map { it.name }).toTypedArray()) { _,i -> category=when(i) { 0->null; 1->""; else->current.categories[i-2].id }; labels() }.show() }
    accountButton.setOnClickListener { androidx.appcompat.app.AlertDialog.Builder(this@RecordsFilterActivity).setTitle("Account").setItems((listOf("All accounts")+current.accounts.map { it.name }).toTypedArray()) { _,i -> account=current.accounts.getOrNull(i-1)?.id ?: 0; labels() }.show() }
   }
  } } }
 }
 override fun onSaveInstanceState(out: Bundle) { out.putString("day",day); out.putString("category",category); out.putLong("account",account); super.onSaveInstanceState(out) }
}

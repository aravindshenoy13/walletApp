package com.aravind.wallet.ui
import android.os.Bundle
import android.widget.*
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.aravind.wallet.R
import com.aravind.wallet.data.*
import com.aravind.wallet.domain.*
import com.aravind.wallet.databinding.ActivityRecordEditorBinding
import java.time.*
import kotlinx.coroutines.launch

class BulkEntryActivity : AppCompatActivity() {
 private val model: WalletViewModel by viewModels()
 private lateinit var root: LinearLayout
 private lateinit var rows: LinearLayout
 private lateinit var action: Button
 private lateinit var add: Button
 private lateinit var dateButton: Button
 private lateinit var error: TextView
 private lateinit var preview: TextView
 private var date=LocalDate.now()
 private var ledger: Ledger?=null
 private var touched=false
 private var reviewing=false
 private var checked=emptyList<WalletRecord>()
 private data class Draft(val b: ActivityRecordEditorBinding,var time: LocalTime,var category: String?)
 private val drafts=mutableListOf<Draft>()
 private var restored: ArrayList<Bundle>?=null
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  touched=savedInstanceState?.getBoolean("touched") ?: false
  date=savedInstanceState?.getString("date")?.let(LocalDate::parse) ?: date
  @Suppress("DEPRECATION")
  restored=savedInstanceState?.getParcelableArrayList("drafts")
  root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(getColor(R.color.paper)) }; setContentView(root); applyInsets(root)
  val header=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL; setPadding(dp(8),0,dp(16),0) }; root.addView(header)
  header.addView(Button(this,null,0,R.style.PlainButton).apply { text="Back"; setOnClickListener { back() } },LinearLayout.LayoutParams(-2,-2))
  header.addView(TextView(this).apply { text="Bulk add records"; textSize=20f; setTextColor(getColor(R.color.ink)) },LinearLayout.LayoutParams(0,-2,1f))
  dateButton=button(root,date.display()) { chooseDate(date) { date=it; dateButton.text=date.display() } }
  val scroll=ScrollView(this); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
  rows=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; scroll.addView(rows)
  preview=TextView(this).apply { isVisible=false; textSize=16f }; rows.addView(preview)
  error=TextView(this).apply { setTextColor(getColor(R.color.error)); accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE }; root.addView(error)
  error.setOnClickListener { model.retry() }
  add=button(root,"Add another record") { append(Bundle()); drafts.last().b.amount.requestFocus() }
  add.setBackgroundResource(R.drawable.action_add); add.setTextColor(getColor(R.color.action_add))
  action=button(root,"Review") { if(reviewing) model.saveRecords(checked) else review() }
  action.setBackgroundResource(R.drawable.action_save); action.setTextColor(getColor(R.color.action_save))
  listOf(dateButton,add,action).forEach { it.layoutParams=(it.layoutParams as LinearLayout.LayoutParams).apply { setMargins(dp(16),dp(4),dp(16),dp(4)) } }
  onBackPressedDispatcher.addCallback(this) { back() }
  lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
   launch { model.ledger.collect { state ->
    if(state is LedgerState.Ready) { ledger=state.ledger; if(drafts.isEmpty()) { (restored ?: arrayListOf(Bundle())).forEach { append(it) }; restored=null } }
    if(state==LedgerState.Failed) error.text="Could not load wallet. Tap to retry."
    enabled()
   } }
   launch { model.operation.collect { op ->
    if(op==Operation.Saved) { Toast.makeText(this@BulkEntryActivity,"Records saved",Toast.LENGTH_SHORT).show(); startActivity(android.content.Intent(this@BulkEntryActivity,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish() }
    if(op is Operation.Failed) error.text=op.message ?: "Nothing was saved. Try again."
    enabled()
   } }
  } }
 }
 private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
 private fun button(parent: LinearLayout,label: String,click:()->Unit): Button = Button(this,null,0,R.style.OutlineButton).apply { text=label; setOnClickListener { click() }; parent.addView(this,LinearLayout.LayoutParams(-1,-2)) }
 private fun append(s: Bundle) {
  val current=ledger ?: return
  val b=ActivityRecordEditorBinding.inflate(layoutInflater)
  (b.form.parent as android.view.ViewGroup).removeView(b.form); rows.addView(b.form); b.form.isVisible=true
  fun noSave(v: View) { v.isSaveEnabled=false; if(v is android.view.ViewGroup) for(i in 0 until v.childCount) noSave(v.getChildAt(i)) }; noSave(b.form)
  val heading=TextView(this).apply { text="Record ${drafts.size+1}"; textSize=18f }; b.form.addView(heading,0)
  val d=Draft(b,s.getString("time")?.let(LocalTime::parse) ?: LocalTime.now().withSecond(0).withNano(0),s.getString("category")); drafts.add(d)
  val remove=button(b.form,"Remove") { drafts.remove(d); rows.removeView(b.form); touched=true
   drafts.forEachIndexed { i,row -> ((row.b.form.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text="Record ${i+1}" } }
  for(sp in listOf(b.account,b.destination)) sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_item,current.accounts.map { it.name }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
  b.account.setSelection(current.accounts.indexOfFirst { it.id==s.getLong("source") }.coerceAtLeast(0))
  b.destination.setSelection(current.accounts.indexOfFirst { it.id==s.getLong("destination") }.let { if(it>=0) it else if(current.accounts.size>1) 1 else 0 })
  b.type.check(s.getInt("type",R.id.type_expense))
  fun type() { val t=b.type.checkedRadioButtonId==R.id.type_transfer; b.category.isVisible=!t; b.categoryLabel.isVisible=!t; b.destination.isVisible=t; b.destinationLabel.isVisible=t }
  b.type.setOnCheckedChangeListener { _,_->type() }; type()
  fun category() { val c=current.categories.find { it.id==d.category }; b.categoryName.text=c?.name ?: "Uncategorised"; b.categoryBadge.showCategory(c?.iconKey ?: "uncategorised",c?.backgroundColorKey ?: "Sky") }; category()
  b.category.setOnClickListener { val choices=current.categories.filter { !it.isArchived }; AlertDialog.Builder(this).setTitle("Category").setItems((listOf("Uncategorised")+choices.map { it.name }).toTypedArray()) { _,i->d.category=choices.getOrNull(i-1)?.id; category() }.show() }
  (b.date.parent as View).isVisible=false
  b.time.text=date.atTime(d.time).displayTime(this); b.time.setOnClickListener { android.app.TimePickerDialog(this,{ _,h,m->touched=true; d.time=LocalTime.of(h,m); b.time.text=date.atTime(d.time).displayTime(this) },d.time.hour,d.time.minute,android.text.format.DateFormat.is24HourFormat(this)).show() }
  b.amount.setText(s.getString("amount","")); b.description.setText(s.getString("description",""))
  val toggle=button(b.form,"Calculator") { b.calculatorKeys.isVisible=!b.calculatorKeys.isVisible }; b.form.removeView(toggle); b.form.addView(toggle,b.form.indexOfChild(b.calculatorKeys)); b.calculatorKeys.isVisible=false
  listOf(listOf("Clear","⌫","÷","×"),listOf("7","8","9","−"),listOf("4","5","6","+"),listOf("1","2","3","="),listOf("0",".")).forEach { keys ->
   val line=LinearLayout(this); b.calculatorKeys.addView(line)
   keys.forEach { key -> line.addView(Button(this,null,0,R.style.OutlineButton).apply { text=key; setOnClickListener {
    val f=b.amount; val start=f.selectionStart.coerceAtLeast(0); val end=f.selectionEnd.coerceAtLeast(start)
    try { when(key) { "Clear"->f.text.clear(); "⌫"->f.text.delete(if(start==end) (start-1).coerceAtLeast(0) else start,end); "="->f.setText(AmountCalculator.evaluate(f.text.toString())); else->f.text.replace(start,end,key) }; f.setSelection(f.length()) } catch(e: IllegalArgumentException) { f.error=e.message }
   } },LinearLayout.LayoutParams(0,-2,1f)) }
  }
  compact(b,heading,remove,toggle)
  b.form.viewTreeObserver.addOnGlobalLayoutListener { val h=maxOf(b.account.height,b.category.height,b.destination.height); if(h>0) listOf(b.account,b.category,b.destination).forEach { if(it.layoutParams.height!=h) it.layoutParams=it.layoutParams.apply { height=h } } }
 }
 private fun compact(b: ActivityRecordEditorBinding, heading: TextView, remove: Button, calculator: Button) {
  fun detach(v: View) { (v.parent as? android.view.ViewGroup)?.removeView(v) }
  fun pair(left: View,right: View): LinearLayout = LinearLayout(this).apply {
   orientation=LinearLayout.HORIZONTAL; gravity=android.view.Gravity.TOP
   listOf(left,right).forEachIndexed { i,v -> detach(v); addView(v,LinearLayout.LayoutParams(0,-2,1f).apply { if(i==0) marginEnd=dp(8) }) }
  }
  fun field(label: String, control: View): LinearLayout = LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   addView(TextView(this@BulkEntryActivity).apply { text=label; textSize=12f; setTextColor(getColor(R.color.quiet)); labelFor=control.id },LinearLayout.LayoutParams(-1,-2))
   detach(control); addView(control,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(4) })
  }
  val accounts=b.account.parent.parent as View
  detach(accounts)
  val type=Spinner(this).apply {
   contentDescription="Record type"; minimumHeight=dp(48); setBackgroundResource(R.drawable.action_outline)
   adapter=ArrayAdapter(this@BulkEntryActivity,android.R.layout.simple_spinner_item,listOf("Expense","Income","Transfer")).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
   val ids=listOf(R.id.type_expense,R.id.type_income,R.id.type_transfer)
   setSelection(ids.indexOf(b.type.checkedRadioButtonId).coerceAtLeast(0))
   onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) { b.type.check(ids[position]) }
    override fun onNothingSelected(parent: AdapterView<*>?) { }
   }
  }
  val title=pair(heading,remove)
  heading.textSize=16f; heading.setTextColor(getColor(R.color.ink))
  remove.setBackgroundResource(android.R.color.transparent); remove.setTextColor(getColor(R.color.action_delete)); remove.contentDescription="Remove "+heading.text
  val typeAmount=pair(field("Type",type),field("Amount · \u20b9",b.amount))
  val descriptionTime=pair(field("Description / payee",b.description),field("Time",b.time))
  detach(calculator); detach(b.calculatorKeys)
  b.form.removeAllViews()
  b.form.setPadding(dp(8),dp(8),dp(8),dp(12))
  b.form.layoutParams=LinearLayout.LayoutParams(-1,-2).apply { setMargins(dp(8),0,dp(8),dp(4)) }
  listOf(title,typeAmount,accounts,descriptionTime,calculator,b.calculatorKeys).forEach { b.form.addView(it,LinearLayout.LayoutParams(-1,-2).apply { topMargin=if(it==title) 0 else dp(8) }) }
  b.description.hint="Optional"
  listOf(b.accountLabel,b.categoryLabel,b.destinationLabel).forEach { it.textSize=12f }
  listOf(b.account,b.destination,b.category,b.amount,b.description,b.time,calculator,remove).forEach { v ->
   v.minimumHeight=dp(48)
   (v.layoutParams as? LinearLayout.LayoutParams)?.let { it.topMargin=dp(4); it.bottomMargin=0 }
  }
 }
 private fun review() {
  try {
   require(drafts.isNotEmpty()) { "Add at least one record." }; val current=requireNotNull(ledger)
   checked=drafts.mapIndexed { i,d -> try {
    val b=d.b; val amount=Money.parse(AmountCalculator.evaluate(b.amount.text.toString())); require(amount>0) { "Enter an amount greater than zero." }
    val type=when(b.type.checkedRadioButtonId) { R.id.type_income->RecordType.INCOME; R.id.type_transfer->RecordType.TRANSFER; else->RecordType.EXPENSE }
    val source=current.accounts.getOrNull(b.account.selectedItemPosition)?.id; require(source!=null) { "Choose an account." }
    val dest=if(type==RecordType.TRANSFER) current.accounts.getOrNull(b.destination.selectedItemPosition)?.id else null
    require(type!=RecordType.TRANSFER || (dest!=null && dest!=source)) { "Choose two different accounts." }
    WalletRecord(type=type,amountMinor=amount,accountId=source,destinationAccountId=dest,categoryId=if(type==RecordType.TRANSFER) null else d.category,occurredAt=date.atTime(d.time),description=b.description.text.toString())
   } catch(e: IllegalArgumentException) { throw IllegalArgumentException("Record ${i+1}: ${e.message}") } }
   var income=java.math.BigInteger.ZERO; var expense=java.math.BigInteger.ZERO
   preview.text=checked.mapIndexed { i,r ->
    if(r.type==RecordType.INCOME) income+=r.amountMinor.toBigInteger(); if(r.type==RecordType.EXPENSE) expense+=r.amountMinor.toBigInteger()
    val source=current.accounts.first { it.id==r.accountId }.name
    val detail=if(r.type==RecordType.TRANSFER) " → "+current.accounts.first { it.id==r.destinationAccountId }.name else " · "+(current.categories.find { it.id==r.categoryId }?.name ?: "Uncategorised")
    "${i+1}. ${r.occurredAt} · ${r.type}\n$source$detail · ${Money.format(r.amountMinor)}\n${r.description}"
   }.joinToString("\n\n")+"\n\nIncome ${Money.format(income)}\nExpenses ${Money.format(expense)}\nNet ${Money.format(income-expense)}"
   reviewing=true; error.text=""; showReview()
  } catch(e: IllegalArgumentException) { error.text=e.message }
 }
 private fun showReview() { drafts.forEach { it.b.form.isVisible=!reviewing }; preview.isVisible=reviewing; add.isVisible=!reviewing; dateButton.isEnabled=!reviewing; action.text=if(reviewing) "Confirm and save all" else "Review" }
 private fun enabled() { val busy=model.operation.value==Operation.Working || model.operation.value==Operation.Saved; rows.enableTree(!busy); add.isEnabled=!busy && ledger!=null; action.isEnabled=!busy && ledger!=null; dateButton.isEnabled=!busy && !reviewing }
 private fun back() {
  if(model.operation.value==Operation.Working || model.operation.value==Operation.Saved) return
  if(reviewing) { reviewing=false; showReview(); return }
  if(touched || drafts.size>1 || drafts.any { it.b.amount.text.isNotBlank() || it.b.description.text.isNotBlank() || it.category!=null || it.b.type.checkedRadioButtonId!=R.id.type_expense || it.b.account.selectedItemPosition>0 || it.b.destination.selectedItemPosition>1 } || date!=LocalDate.now()) AlertDialog.Builder(this).setMessage("Discard bulk drafts?").setNegativeButton("Keep editing",null).setPositiveButton("Discard") { _,_->finish() }.show() else finish()
 }
 override fun onSaveInstanceState(out: Bundle) {
  out.putBoolean("touched",touched)
  out.putString("date",date.toString()); out.putParcelableArrayList("drafts",ArrayList(drafts.map { d -> Bundle().apply { val b=d.b; putString("time",d.time.toString()); putString("category",d.category); putInt("type",b.type.checkedRadioButtonId); putString("amount",b.amount.text.toString()); putString("description",b.description.text.toString()); putLong("source",ledger?.accounts?.getOrNull(b.account.selectedItemPosition)?.id ?: 0); putLong("destination",ledger?.accounts?.getOrNull(b.destination.selectedItemPosition)?.id ?: 0) } })); super.onSaveInstanceState(out)
 }
}

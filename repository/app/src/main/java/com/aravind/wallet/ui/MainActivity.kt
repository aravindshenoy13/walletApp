package com.aravind.wallet.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.addCallback
import android.view.MotionEvent
import android.graphics.Rect
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aravind.wallet.R
import com.aravind.wallet.databinding.ActivityMainBinding
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import com.aravind.wallet.domain.searchRecords
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private enum class Tab { RECORDS, ANALYSIS, ACCOUNTS, CATEGORIES }
    private lateinit var binding: ActivityMainBinding
    private val model: WalletViewModel by viewModels()
    private var searchExpanded = false
    private var tab = Tab.RECORDS
    private var ledger: Ledger? = null
    private lateinit var analysis: AnalysisPanel
    private val rangePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val start = result.data?.getStringExtra("start")?.let(LocalDate::parse)
            val end = result.data?.getStringExtra("end")?.let(LocalDate::parse)
            if (start != null && end != null) analysis.range(start, end)
        }
    }
    private val recordsAdapter = RecordsAdapter { id ->
        startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("id", id))
    }
    private val accountsAdapter = AccountsAdapter({ id ->
        startActivity(Intent(this, AccountHistoryActivity::class.java).putExtra("id", id))
    }) { id ->
        startActivity(Intent(this, AccountEditorActivity::class.java).putExtra("id", id))
    }
    private val categoriesAdapter = CategoriesAdapter { id ->
        startActivity(Intent(this, CategoryEditorActivity::class.java).putExtra("id", id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getString("tab")?.let { runCatching { Tab.valueOf(it) }.getOrNull() }
            ?: if (savedInstanceState?.getBoolean("accounts") == true) Tab.ACCOUNTS else Tab.RECORDS
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        analysis = AnalysisPanel(this, binding.analysisPanel, savedInstanceState) { start, end ->
            rangePicker.launch(Intent(this, DateRangeActivity::class.java).putExtra("start", start.toString()).putExtra("end", end.toString()))
        }
        searchExpanded = savedInstanceState?.getBoolean("searchExpanded") ?: false
        binding.settings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.openSearch.setOnClickListener { searchExpanded = true; render(); binding.recordSearch.requestFocus(); (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(binding.recordSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) }
        binding.recordSearch.doAfterTextChanged { render() }
        binding.clearSearch.setOnClickListener { binding.recordSearch.text?.clear(); binding.recordSearch.requestFocus() }
        binding.recordSearch.dismiss = { dismissSearch() }
        binding.recordSearch.setOnEditorActionListener { _, action, _ ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { dismissSearch(); true } else false
        }
        onBackPressedDispatcher.addCallback(this) { if (searchExpanded) dismissSearch() else finish() }
        binding.recordList.layoutManager = LinearLayoutManager(this)
        binding.recordList.adapter = recordsAdapter
        binding.accountList.layoutManager = LinearLayoutManager(this)
        binding.accountList.adapter = accountsAdapter
        binding.categoryList.layoutManager = LinearLayoutManager(this)
        binding.categoryList.adapter = categoriesAdapter
        binding.recordsTab.setOnClickListener { tab = Tab.RECORDS; render() }
        binding.analysisTab.setOnClickListener { tab = Tab.ANALYSIS; analysis.open(); render() }
        binding.accountsTab.setOnClickListener { tab = Tab.ACCOUNTS; render() }
        binding.categoriesTab.setOnClickListener { tab = Tab.CATEGORIES; render() }
        binding.add.setOnClickListener {
            startActivity(Intent(this, if (tab == Tab.CATEGORIES) CategoryEditorActivity::class.java else AccountEditorActivity::class.java))
        }
        binding.addRecordButton.setOnClickListener { addRecord() }
        binding.export.setOnClickListener { startActivity(Intent(this, ExportActivity::class.java)) }
        render()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.ledger.collect { state ->
                    binding.loading.isVisible = state == LedgerState.Loading
                    binding.error.isVisible = state == LedgerState.Failed
                    binding.error.setText(R.string.load_failed)
                    if (state is LedgerState.Ready) {
                        ledger = state.ledger
                        analysis.show(state.ledger)
                        accountsAdapter.submitList(state.ledger.accounts.map { AccountRow(it, state.ledger.balances.getValue(it.id)) })
                        categoriesAdapter.submitList(state.ledger.categories)
                    }
                    val ready = state is LedgerState.Ready
                    binding.add.isEnabled = ready
                    binding.addRecordButton.isEnabled = ready
                    binding.export.isEnabled = ready
                    render()
                } }
                launch {
                    while (isActive) {
                        val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        delay((midnight - System.currentTimeMillis()).coerceAtLeast(1_000))
                        render()
                    }
                }
            }
        }
    }

    private fun dismissSearch() {
        if (!searchExpanded) return
        searchExpanded = false
        binding.recordSearch.text?.clear()
        binding.recordSearch.clearFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
            .hideSoftInputFromWindow(binding.recordSearch.windowToken, 0)
        render()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (searchExpanded && event.action == MotionEvent.ACTION_DOWN) {
            val bounds = Rect()
            binding.searchBox.getGlobalVisibleRect(bounds)
            if (!bounds.contains(event.rawX.toInt(), event.rawY.toInt())) dismissSearch()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onStop() {
        if (!isChangingConfigurations) dismissSearch()
        super.onStop()
    }

    override fun onResume() { super.onResume(); render() }

    private fun render() = with(binding) {
        val today = LocalDate.now()
        title.setText(R.string.app_name)
        add.setText(if (tab == Tab.CATEGORIES) R.string.add_category else R.string.add_account)
        add.isVisible = tab == Tab.ACCOUNTS || tab == Tab.CATEGORIES
        searchBox.isVisible = tab == Tab.RECORDS && searchExpanded
        openSearch.isVisible = tab == Tab.RECORDS && !searchExpanded
        clearSearch.isEnabled = !recordSearch.text.isNullOrEmpty()
        export.isVisible = false
        listOf(recordsTab to Tab.RECORDS, analysisTab to Tab.ANALYSIS, accountsTab to Tab.ACCOUNTS, categoriesTab to Tab.CATEGORIES).forEach { (button, target) ->
            button.setTypeface(null, if (tab == target) Typeface.BOLD else Typeface.NORMAL)
            button.isSelected = tab == target
        }
        recordsPanel.isVisible = ledger != null && tab == Tab.RECORDS
        accountsPanel.isVisible = ledger != null && tab == Tab.ACCOUNTS
        categoriesPanel.isVisible = ledger != null && tab == Tab.CATEGORIES
        analysisPanel.root.isVisible = ledger != null && tab == Tab.ANALYSIS
        todayDate.text = getString(R.string.today_date, today.display())
        val current = ledger ?: return@with
        val matching = current.searchRecords(recordSearch.text.toString())
        recordsAdapter.show(current, matching)
        recordCount.text = if (recordSearch.text.isNullOrBlank()) getString(R.string.all_records)
            else resources.getQuantityString(R.plurals.matching_records, matching.size, matching.size)
        val totals = current.totals(today)
        income.text = Money.format(totals.income)
        expense.text = Money.format(totals.expense)
        net.showNet(totals.net)
        totalBalance.showBalance(current.totalBalance)
        emptyAccounts.isVisible = current.accounts.isEmpty()
        val emptyMessage = when {
            !recordSearch.text.isNullOrBlank() && matching.isEmpty() -> R.string.no_matches
            current.accounts.isEmpty() -> R.string.no_accounts
            current.records.isEmpty() -> R.string.no_records
            current.records.none { it.occurredAt.toLocalDate() == today } -> R.string.no_records_today
            else -> null
        }
        emptyRecords.isVisible = emptyMessage != null
        emptyMessage?.let(emptyRecords::setText)
    }

    private fun addRecord() {
        val current = ledger ?: return
        if (current.accounts.isEmpty()) {
            AlertDialog.Builder(this).setTitle(R.string.account_required_title).setMessage(R.string.account_required_message)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.new_account) { _, _ ->
                    tab = Tab.ACCOUNTS
                    startActivity(Intent(this, AccountEditorActivity::class.java))
                }.show()
        } else startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("date", LocalDate.now().toString()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("searchExpanded", searchExpanded)
        outState.putString("tab", tab.name)
        analysis.save(outState)
        super.onSaveInstanceState(outState)
    }
}

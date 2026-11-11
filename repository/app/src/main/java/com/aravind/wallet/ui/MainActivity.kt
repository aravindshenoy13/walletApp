package com.aravind.wallet.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aravind.wallet.R
import com.aravind.wallet.databinding.ActivityMainBinding
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private enum class Tab { RECORDS, ACCOUNTS, CATEGORIES }
    private lateinit var binding: ActivityMainBinding
    private val model: WalletViewModel by viewModels()
    private var tab = Tab.RECORDS
    private var ledger: Ledger? = null
    private val recordsAdapter = RecordsAdapter { id ->
        startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("id", id))
    }
    private val accountsAdapter = AccountsAdapter { id ->
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
        binding.recordList.layoutManager = LinearLayoutManager(this)
        binding.recordList.adapter = recordsAdapter
        binding.accountList.layoutManager = LinearLayoutManager(this)
        binding.accountList.adapter = accountsAdapter
        binding.categoryList.layoutManager = LinearLayoutManager(this)
        binding.categoryList.adapter = categoriesAdapter
        binding.recordsTab.setOnClickListener { tab = Tab.RECORDS; render() }
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
                        recordsAdapter.show(state.ledger)
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

    override fun onResume() { super.onResume(); render() }

    private fun render() = with(binding) {
        val today = LocalDate.now()
        title.setText(when (tab) { Tab.RECORDS -> R.string.app_name; Tab.ACCOUNTS -> R.string.accounts; Tab.CATEGORIES -> R.string.categories })
        add.setText(if (tab == Tab.CATEGORIES) R.string.add_category else R.string.add_account)
        add.isVisible = tab != Tab.RECORDS
        export.isVisible = tab == Tab.RECORDS
        listOf(recordsTab to Tab.RECORDS, accountsTab to Tab.ACCOUNTS, categoriesTab to Tab.CATEGORIES).forEach { (button, target) ->
            button.setTypeface(null, if (tab == target) Typeface.BOLD else Typeface.NORMAL)
            button.isSelected = tab == target
        }
        recordsPanel.isVisible = ledger != null && tab == Tab.RECORDS
        accountsPanel.isVisible = ledger != null && tab == Tab.ACCOUNTS
        categoriesPanel.isVisible = ledger != null && tab == Tab.CATEGORIES
        todayDate.text = getString(R.string.today_date, today.display())
        val current = ledger ?: return@with
        val totals = current.totals(today)
        income.text = Money.format(totals.income)
        expense.text = Money.format(totals.expense)
        net.text = Money.format(totals.net)
        totalBalance.text = Money.format(current.totalBalance)
        emptyAccounts.isVisible = current.accounts.isEmpty()
        val emptyMessage = when {
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
        outState.putString("tab", tab.name)
        super.onSaveInstanceState(outState)
    }
}

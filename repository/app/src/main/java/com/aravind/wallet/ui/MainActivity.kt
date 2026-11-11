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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val model: WalletViewModel by viewModels()
    private var selectedDate = LocalDate.now()
    private var accountsTab = false
    private var ledger: Ledger? = null
    private val recordsAdapter = RecordsAdapter(
        edit = { id -> startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("id", id)) },
        selectDate = { date -> setDate(date, scroll = false) },
    )
    private val accountsAdapter = AccountsAdapter { id ->
        startActivity(Intent(this, AccountEditorActivity::class.java).putExtra("id", id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = savedInstanceState?.getString("date")?.let(LocalDate::parse) ?: LocalDate.now()
        accountsTab = savedInstanceState?.getBoolean("accounts") ?: false
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.recordList.layoutManager = LinearLayoutManager(this)
        binding.recordList.adapter = recordsAdapter
        binding.accountList.layoutManager = LinearLayoutManager(this)
        binding.accountList.adapter = accountsAdapter
        binding.recordsTab.setOnClickListener { accountsTab = false; render() }
        binding.accountsTab.setOnClickListener { accountsTab = true; render() }
        binding.previousDate.setOnClickListener { if (selectedDate > LocalDate.of(1, 1, 1)) setDate(selectedDate.minusDays(1)) }
        binding.nextDate.setOnClickListener { if (selectedDate < LocalDate.of(9999, 12, 31)) setDate(selectedDate.plusDays(1)) }
        binding.today.setOnClickListener { setDate(LocalDate.now()) }
        binding.selectedDate.setOnClickListener { chooseDate(selectedDate) { setDate(it) } }
        binding.add.setOnClickListener { add() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ledger.collect { state ->
                    binding.loading.isVisible = state == LedgerState.Loading
                    binding.error.isVisible = state == LedgerState.Failed
                    binding.error.setText(R.string.load_failed)
                    if (state is LedgerState.Ready) {
                        ledger = state.ledger
                        recordsAdapter.show(state.ledger)
                        accountsAdapter.submitList(state.ledger.accounts.map { AccountRow(it, state.ledger.balances.getValue(it.id)) })
                    }
                    binding.add.isEnabled = state is LedgerState.Ready
                    render()
                }
            }
        }
    }

    private fun setDate(date: LocalDate, scroll: Boolean = true) {
        selectedDate = date
        render()
        if (scroll) {
            val position = recordsAdapter.currentList.indexOfFirst { it is RecordRow.Date && it.date == date }
            if (position >= 0) binding.recordList.scrollToPosition(position)
        }
    }

    private fun render() = with(binding) {
        val showingAccounts = this@MainActivity.accountsTab
        title.setText(if (showingAccounts) R.string.accounts else R.string.app_name)
        add.setText(if (showingAccounts) R.string.add_account else R.string.add_record)
        recordsTab.setTypeface(null, if (showingAccounts) Typeface.NORMAL else Typeface.BOLD)
        accountsTab.setTypeface(null, if (showingAccounts) Typeface.BOLD else Typeface.NORMAL)
        recordsTab.isSelected = !showingAccounts
        accountsTab.isSelected = showingAccounts
        recordsPanel.isVisible = ledger != null && !showingAccounts
        accountsPanel.isVisible = ledger != null && showingAccounts
        selectedDate.text = this@MainActivity.selectedDate.display()
        val current = ledger ?: return@with
        val totals = current.totals(this@MainActivity.selectedDate)
        income.text = Money.format(totals.income)
        expense.text = Money.format(totals.expense)
        net.text = Money.format(totals.net)
        totalBalance.text = Money.format(current.totalBalance)
        emptyAccounts.isVisible = current.accounts.isEmpty()
        val emptyMessage = when {
            current.accounts.isEmpty() -> R.string.no_accounts
            current.records.isEmpty() -> R.string.no_records
            current.records.none { it.occurredAt.toLocalDate() == this@MainActivity.selectedDate } -> R.string.no_records_on_date
            else -> null
        }
        emptyRecords.isVisible = emptyMessage != null
        emptyMessage?.let(emptyRecords::setText)
    }

    private fun add() {
        val current = ledger ?: return
        if (accountsTab) {
            startActivity(Intent(this, AccountEditorActivity::class.java))
        } else if (current.accounts.isEmpty()) {
            AlertDialog.Builder(this).setTitle(R.string.account_required_title)
                .setMessage(R.string.account_required_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.new_account) { _, _ ->
                    accountsTab = true
                    startActivity(Intent(this, AccountEditorActivity::class.java))
                }.show()
        } else startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("date", selectedDate.toString()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("date", selectedDate.toString())
        outState.putBoolean("accounts", accountsTab)
        super.onSaveInstanceState(outState)
    }
}

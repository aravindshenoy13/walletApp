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
    private var accountsTab = false
    private var ledger: Ledger? = null
    private val recordsAdapter = RecordsAdapter(
        edit = { id -> startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("id", id)) },
    )
    private val accountsAdapter = AccountsAdapter { id ->
        startActivity(Intent(this, AccountEditorActivity::class.java).putExtra("id", id))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() = with(binding) {
        val today = LocalDate.now()
        val showingAccounts = this@MainActivity.accountsTab
        title.setText(if (showingAccounts) R.string.accounts else R.string.app_name)
        add.setText(if (showingAccounts) R.string.add_account else R.string.add_record)
        recordsTab.setTypeface(null, if (showingAccounts) Typeface.NORMAL else Typeface.BOLD)
        accountsTab.setTypeface(null, if (showingAccounts) Typeface.BOLD else Typeface.NORMAL)
        recordsTab.isSelected = !showingAccounts
        accountsTab.isSelected = showingAccounts
        recordsPanel.isVisible = ledger != null && !showingAccounts
        accountsPanel.isVisible = ledger != null && showingAccounts
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
        } else startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("date", LocalDate.now().toString()))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("accounts", accountsTab)
        super.onSaveInstanceState(outState)
    }
}

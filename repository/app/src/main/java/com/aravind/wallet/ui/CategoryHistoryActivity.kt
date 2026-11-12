package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aravind.wallet.R
import com.aravind.wallet.databinding.ActivityCategoryHistoryBinding
import com.aravind.wallet.domain.*
import java.time.LocalDate
import kotlinx.coroutines.launch

class CategoryHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCategoryHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root); applyInsets(binding.root)
        val categoryId = intent.getStringExtra("category") ?: return finish()
        val start = intent.getStringExtra("start")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return finish()
        val end = intent.getStringExtra("end")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return finish()
        if (end < start) return finish()
        val accountId = intent.getLongExtra("account", 0L).takeIf { it != 0L }
        val mode = intent.getStringExtra("mode")?.let { runCatching { BreakdownMode.valueOf(it) }.getOrNull() } ?: return finish()
        val model: WalletViewModel by viewModels()
        val adapter = RecordsAdapter { record -> startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("id", record)) }
        binding.historyList.layoutManager = LinearLayoutManager(this); binding.historyList.adapter = adapter
        binding.error.setOnClickListener { model.retry() }
        binding.back.setOnClickListener { finish() }
        binding.period.text = getString(R.string.date_range, start.display(), end.display())
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ledger.collect { state ->
                    binding.loading.isVisible = state == LedgerState.Loading
                    binding.error.isVisible = state == LedgerState.Failed
                    if (state is LedgerState.Ready) {
                        val ledger = state.ledger
                        val category = ledger.categoriesById[categoryId]
                        val account = accountId?.let { ledger.accountsById[it] }
                        if (category == null || (accountId != null && account == null)) { finish(); return@collect }
                        val records = ledger.categoryRecords(categoryId, start, end, accountId, mode)
                        binding.title.text = category.name
                        val modeLabel = getString(when (mode) { BreakdownMode.NET -> R.string.net; BreakdownMode.INCOME -> R.string.income; BreakdownMode.EXPENSE -> R.string.expenses })
                        binding.scope.text = getString(R.string.category_history_scope, account?.name ?: getString(R.string.all_accounts), modeLabel)
                        val totals = ledger.analyze(start, end, accountId).categories[categoryId]
                        binding.total.text = Money.format(totals?.value(mode) ?: java.math.BigInteger.ZERO)
                        binding.empty.isVisible = records.isEmpty()
                        adapter.show(ledger, records, accountId)
                    }
                }
            }
        }
    }
}

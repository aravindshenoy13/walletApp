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
import com.aravind.wallet.databinding.ActivityAccountHistoryBinding
import com.aravind.wallet.domain.Money
import com.aravind.wallet.domain.accountRecords
import kotlinx.coroutines.launch

class AccountHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAccountHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root); applyInsets(binding.root)
        val model: WalletViewModel by viewModels()
        val id = intent.getLongExtra("id", 0)
        val adapter = RecordsAdapter { record -> startActivity(Intent(this, RecordEditorActivity::class.java).putExtra("id", record)) }
        binding.historyList.layoutManager = LinearLayoutManager(this); binding.historyList.adapter = adapter
        binding.back.setOnClickListener { finish() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ledger.collect { state ->
                    binding.loading.isVisible = state == LedgerState.Loading
                    binding.error.isVisible = state == LedgerState.Failed
                    if (state is LedgerState.Ready) {
                        val account = state.ledger.accountsById[id]
                        if (account == null) finish() else {
                            binding.title.text = account.name
                            binding.title.showAccountIcon(account.iconKey)
                            binding.currentBalance.showBalance(state.ledger.balances.getValue(id))
                            adapter.show(state.ledger, state.ledger.accountRecords(id), id)
                        }
                    }
                }
            }
        }
    }
}

package com.aravind.wallet.ui

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aravind.wallet.R
import com.aravind.wallet.data.Account
import com.aravind.wallet.databinding.ActivityAccountEditorBinding
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import kotlinx.coroutines.launch

class AccountEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountEditorBinding
    private val model: WalletViewModel by viewModels()
    private var initialized = false
    private var icon = "bank"
    private var draft: Bundle? = null
    private val accountId get() = intent.getLongExtra("id", 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draft = savedInstanceState?.getBundle("draft")
        binding = ActivityAccountEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.title.setText(if (accountId == 0L) R.string.new_account else R.string.edit_account)
        onBackPressedDispatcher.addCallback(this) { if (model.operation.value != Operation.Working) finish() }
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.save.setOnClickListener { save() }
        binding.accountIcon.setOnCheckedChangeListener { _, id ->
            icon = when (id) { R.id.icon_cash -> "cash"; R.id.icon_card -> "card"; else -> "bank" }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.ledger.collect { state ->
                        when (state) {
                            is LedgerState.Ready -> if (!initialized) initialize(state.ledger)
                            LedgerState.Failed -> {
                                binding.loading.isVisible = false
                                binding.form.isVisible = true
                                showError(getString(R.string.load_failed))
                                updateEnabled()
                            }
                            LedgerState.Loading -> Unit
                        }
                    }
                }
                launch {
                    model.operation.collect { operation ->
                        if (operation == Operation.Saved) finish()
                        if (operation is Operation.Failed) showError(operation.message ?: getString(R.string.save_failed))
                        updateEnabled()
                    }
                }
            }
        }
    }

    private fun initialize(ledger: Ledger) {
        val account = ledger.accountsById[accountId]
        if (accountId != 0L && account == null) {
            finish()
            return
        }
        binding.name.setText(draft?.getString("name") ?: account?.name.orEmpty())
        binding.openingBalance.setText(draft?.getString("balance") ?: Money.input(account?.openingBalanceMinor ?: 0))
        icon = draft?.getString("icon") ?: account?.iconKey ?: "bank"
        binding.accountIcon.check(when(icon) { "cash" -> R.id.icon_cash; "card" -> R.id.icon_card; else -> R.id.icon_bank })
        initialized = true
        binding.loading.isVisible = false
        binding.form.isVisible = true
        updateEnabled()
    }

    private fun save() {
        binding.error.isVisible = false
        if (binding.name.text.isNullOrBlank()) {
            binding.name.error = "Enter an account name."
            binding.name.requestFocus()
            return
        }
        val balance = try {
            Money.parse(binding.openingBalance.text.toString(), allowNegative = true)
        } catch (error: IllegalArgumentException) {
            binding.openingBalance.error = error.message
            binding.openingBalance.requestFocus()
            return
        }
        model.saveAccount(Account(accountId, binding.name.text.toString(), balance, icon))
    }

    private fun updateEnabled() {
        val busy = model.operation.value == Operation.Working || model.operation.value == Operation.Saved
        val ready = initialized && model.ledger.value is LedgerState.Ready
        binding.form.enableTree(ready && !busy)
        binding.back.isEnabled = !busy
        binding.save.isEnabled = ready && !busy
    }

    private fun showError(message: String) {
        binding.error.text = message
        binding.error.isVisible = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("draft", if (initialized) Bundle().apply {
            putString("icon", icon)
            putString("name", binding.name.text.toString())
            putString("balance", binding.openingBalance.text.toString())
        } else draft)
        super.onSaveInstanceState(outState)
    }
}

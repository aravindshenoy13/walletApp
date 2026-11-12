package com.aravind.wallet.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aravind.wallet.R
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.databinding.ActivityCategoryPickerBinding
import kotlinx.coroutines.launch

class CategoryPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryPickerBinding
    private lateinit var adapter: CategoryChoices
    private val model: WalletViewModel by viewModels()
    private var categories = emptyList<WalletCategory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        adapter = CategoryChoices(this, mutableListOf())
        adapter.selectedId = intent.getStringExtra("selectedCategoryId")
        binding.categoryResults.adapter = adapter
        binding.message.setOnClickListener { model.retry() }
        binding.back.setOnClickListener { finish() }
        binding.search.doAfterTextChanged { showResults() }
        binding.categoryResults.setOnItemClickListener { _, _, position, _ ->
            val category = adapter.getItem(position) ?: return@setOnItemClickListener
            setResult(RESULT_OK, Intent().putExtra("categoryId", category.id))
            finish()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ledger.collect { state ->
                    binding.loading.isVisible = state == LedgerState.Loading
                    when (state) {
                        is LedgerState.Ready -> {
                            val original = state.ledger.records.find { it.id == intent.getLongExtra("recordId", 0) }?.categoryId
                            categories = state.ledger.categories.filter { !it.isArchived || it.id == original }
                            showResults()
                        }
                        LedgerState.Failed -> {
                            categories = emptyList()
                            adapter.clear()
                            binding.message.setText(R.string.load_failed)
                            binding.message.isVisible = true
                        }
                        LedgerState.Loading -> Unit
                    }
                }
            }
        }
    }

    private fun showResults() {
        if (model.ledger.value == LedgerState.Failed) {
            binding.message.setText(R.string.load_failed)
            binding.message.isVisible = true
            return
        }
        val query = binding.search.text.toString().trim()
        val results = categories.filter { it.name.contains(query, ignoreCase = true) }
        adapter.setNotifyOnChange(false)
        adapter.clear()
        adapter.addAll(results)
        adapter.notifyDataSetChanged()
        binding.message.setText(R.string.no_categories_found)
        binding.message.isVisible = results.isEmpty() && !binding.loading.isVisible
    }
}

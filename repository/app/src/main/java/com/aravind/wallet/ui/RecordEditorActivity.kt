package com.aravind.wallet.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aravind.wallet.R
import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.databinding.ActivityRecordEditorBinding
import com.aravind.wallet.domain.Ledger
import com.aravind.wallet.domain.Money
import com.aravind.wallet.domain.RecordType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

class RecordEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordEditorBinding
    private val model: WalletViewModel by viewModels()
    private var initialized = false
    private var draft: Bundle? = null
    private var accounts = emptyList<Account>()
    private var categoryChoices = emptyList<WalletCategory>()
    private var originalCategoryId: String? = null
    private var selectedCategoryId: String? = null
    private val pickCategory = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra("categoryId")?.let { selectedCategoryId = it }
            (model.ledger.value as? LedgerState.Ready)?.let { refreshCategories(it.ledger) }
        }
    }
    private var preservedNote = ""
    private var occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
    private val recordId get() = intent.getLongExtra("id", 0)
    private val type: RecordType get() = when (binding.type.checkedRadioButtonId) {
        R.id.type_income -> RecordType.INCOME
        R.id.type_transfer -> RecordType.TRANSFER
        else -> RecordType.EXPENSE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        draft = savedInstanceState?.getBundle("draft")
        binding = ActivityRecordEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        binding.title.setText(if (recordId == 0L) R.string.new_record else R.string.edit_record)
        binding.delete.isVisible = recordId != 0L
        binding.category.isSaveEnabled = false
        binding.category.setOnClickListener {
            if (categoryChoices.isEmpty()) {
                startActivity(Intent(this, CategoryEditorActivity::class.java))
                return@setOnClickListener
            }
            pickCategory.launch(Intent(this, CategoryPickerActivity::class.java)
                .putExtra("recordId", recordId).putExtra("selectedCategoryId", selectedCategoryId))
        }
        binding.account.isSaveEnabled = false
        binding.destination.isSaveEnabled = false
        onBackPressedDispatcher.addCallback(this) { if (model.operation.value != Operation.Working) finish() }
        binding.error.setOnClickListener { model.retry() }
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.type.setOnCheckedChangeListener { _, _ -> updateType() }
        binding.date.setOnClickListener {
            chooseDate(occurredAt.toLocalDate()) { occurredAt = it.atTime(occurredAt.toLocalTime()); updateDateTime() }
        }
        binding.time.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                occurredAt = occurredAt.withHour(hour).withMinute(minute)
                updateDateTime()
            }, occurredAt.hour, occurredAt.minute, DateFormat.is24HourFormat(this)).show()
        }
        binding.amount.showSoftInputOnFocus = false
        listOf(listOf("Clear", "⌫", "÷", "×"), listOf("7", "8", "9", "−"), listOf("4", "5", "6", "+"), listOf("1", "2", "3", "="), listOf("0", ".")).forEach { keys ->
            val row = android.widget.LinearLayout(this)
            keys.forEach { key ->
                val button = android.widget.Button(this, null, 0, R.style.OutlineButton).apply {
                    text = key
                    contentDescription = when (key) { "⌫" -> "Backspace"; "=" -> "Calculate amount"; else -> key }
                    setOnClickListener {
                        if (key == "=") calculateAmount() else {
                            val field = binding.amount
                            val start = field.selectionStart.coerceAtLeast(0)
                            val end = field.selectionEnd.coerceAtLeast(start)
                            when (key) {
                                "Clear" -> field.text.clear()
                                "⌫" -> field.text.delete(if (start == end) (start - 1).coerceAtLeast(0) else start, end)
                                else -> field.text.replace(start, end, key)
                            }
                            field.error = null
                        }
                    }
                }
                row.addView(button, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
            }
            binding.calculatorKeys.addView(row)
        }
        binding.save.setOnClickListener { save() }
        binding.delete.setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.delete_title).setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ -> model.deleteRecord(recordId) }.show()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.ledger.collect { state ->
                        when (state) {
                            is LedgerState.Ready -> if (!initialized) initialize(state.ledger) else {
                                if (recordId != 0L && state.ledger.records.none { it.id == recordId }) {
                                    finish()
                                } else {
                                    if (binding.error.text == getString(R.string.load_failed)) binding.error.isVisible = false
                                    binding.loading.isVisible = false
                                    refreshAccounts(state.ledger)
                                    refreshCategories(state.ledger)
                                }
                            }
                            LedgerState.Failed -> {
                                binding.loading.isVisible = false
                                binding.form.isVisible = true
                                showError(getString(R.string.load_failed))
                                updateEnabled()
                            }
                            LedgerState.Loading -> { binding.loading.isVisible = true; updateEnabled() }
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
        val record = ledger.records.find { it.id == recordId }
        if (recordId != 0L && record == null) {
            finish()
            return
        }
        accounts = ledger.accounts
        originalCategoryId = record?.categoryId
        preservedNote = record?.note.orEmpty()
        val names = accounts.map { it.name }
        binding.account.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names) .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.destination.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names) .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val sourceId = draft?.getLong("source") ?: record?.accountId
        val destinationId = draft?.getLong("destination") ?: record?.destinationAccountId
        binding.account.setSelection(accounts.indexOfFirst { it.id == sourceId }.coerceAtLeast(0))
        binding.destination.setSelection(accounts.indexOfFirst { it.id == destinationId }.let {
            if (it >= 0) it else if (accounts.size >= 2) 1 else 0
        })
        val selectedType = draft?.getString("type")?.let(RecordType::valueOf) ?: record?.type ?: RecordType.EXPENSE
        binding.type.check(when (selectedType) {
            RecordType.INCOME -> R.id.type_income
            RecordType.EXPENSE -> R.id.type_expense
            RecordType.TRANSFER -> R.id.type_transfer
        })
        refreshCategories(ledger, selectedCategoryId ?: draft?.getString("category") ?: record?.categoryId ?: "")
        binding.amount.setText(draft?.getString("amount") ?: record?.let { Money.input(it.amountMinor) }.orEmpty())
        binding.description.setText(draft?.getString("description") ?: record?.description.orEmpty())
        occurredAt = draft?.getString("occurredAt")?.let(LocalDateTime::parse) ?: record?.occurredAt
            ?: LocalDateTime.of(intent.getStringExtra("date")?.let(LocalDate::parse) ?: LocalDate.now(),
                LocalTime.now().truncatedTo(ChronoUnit.MINUTES))
        initialized = true
        binding.loading.isVisible = false
        binding.form.isVisible = true
        updateDateTime()
        updateType()
    }

    private fun refreshCategories(ledger: Ledger, wantedId: String? = selectedCategoryId) {
        categoryChoices = ledger.categories.filter { !it.isArchived || it.id == originalCategoryId }
        val category = categoryChoices.find { it.id == wantedId }
        selectedCategoryId = category?.id
        binding.categoryName.text = category?.let { if (it.isArchived) getString(R.string.archived_name, it.name) else it.name }
            ?: "Uncategorised"
        binding.categoryBadge.isVisible = category != null
        category?.let { binding.categoryBadge.showCategory(it.iconKey, it.backgroundColorKey) }
        binding.category.contentDescription = getString(R.string.category_selection, binding.categoryName.text)
        updateType()
    }

    private fun refreshAccounts(ledger: Ledger) {
        if (accounts == ledger.accounts) return
        val source = accounts.getOrNull(binding.account.selectedItemPosition)?.id
        val destination = accounts.getOrNull(binding.destination.selectedItemPosition)?.id
        accounts = ledger.accounts
        val names = accounts.map { it.name }
        binding.account.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.destination.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.account.setSelection(accounts.indexOfFirst { it.id == source }.coerceAtLeast(0))
        binding.destination.setSelection(accounts.indexOfFirst { it.id == destination }.let {
            if (it >= 0) it else if (accounts.size >= 2) 1 else 0
        })
        updateType()
    }

    private fun updateDateTime() {
        binding.date.text = occurredAt.toLocalDate().display()
        binding.time.text = occurredAt.displayTime(this)
    }

    private fun updateType() {
        val transfer = type == RecordType.TRANSFER
        binding.category.isVisible = !transfer
        binding.categoryLabel.isVisible = !transfer
        binding.destination.isVisible = transfer
        binding.destinationLabel.isVisible = transfer
        binding.categoryHelp.isVisible = false
        binding.transferHelp.isVisible = transfer
        binding.transferHelp.setText(if (accounts.size >= 2) R.string.transfer_help else R.string.transfer_needs_accounts)
        binding.accountLabel.setText(if (transfer) R.string.from_account else R.string.account)
        updateEnabled()
    }

    private fun calculateAmount(): Boolean = try {
        binding.amount.setText(com.aravind.wallet.domain.AmountCalculator.evaluate(binding.amount.text.toString()))
        binding.amount.setSelection(binding.amount.length())
        binding.amount.error = null
        true
    } catch (error: IllegalArgumentException) {
        binding.amount.error = error.message
        binding.amount.requestFocus()
        false
    }

    private fun save() {
        if (!calculateAmount()) return
        binding.error.isVisible = false
        val amount = try {
            Money.parse(binding.amount.text.toString())
        } catch (error: IllegalArgumentException) {
            binding.amount.error = error.message
            binding.amount.requestFocus()
            return
        }
        val source = accounts.getOrNull(binding.account.selectedItemPosition)?.id
        val destination = if (type == RecordType.TRANSFER) accounts.getOrNull(binding.destination.selectedItemPosition)?.id else null
        if (source == null) { showError("Choose an account."); return }
        if (type == RecordType.TRANSFER && (destination == null || source == destination)) {
            showError("Choose two different accounts for a transfer.")
            return
        }
        model.saveRecord(WalletRecord(
            id = recordId, type = type, amountMinor = amount, accountId = source, destinationAccountId = destination,
            categoryId = if (type == RecordType.TRANSFER) null else selectedCategoryId?.takeIf { it.isNotEmpty() },
            occurredAt = occurredAt, description = binding.description.text.toString(), note = preservedNote,
        ))
    }

    private fun updateEnabled() {
        val busy = model.operation.value == Operation.Working || model.operation.value == Operation.Saved
        val ready = initialized && model.ledger.value is LedgerState.Ready
        binding.form.enableTree(ready && !busy)
        binding.back.isEnabled = !busy
        binding.save.isEnabled = ready && !busy && accounts.isNotEmpty() &&
            (if (type == RecordType.TRANSFER) accounts.size >= 2 else true)
        binding.delete.isEnabled = ready && !busy
    }

    private fun showError(message: String) {
        binding.error.text = message
        binding.error.isVisible = true
        binding.error.post { binding.scroll.smoothScrollTo(0, binding.error.bottom) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("draft", if (initialized) Bundle().apply {
            putString("type", type.name)
            putString("amount", binding.amount.text.toString())
            putString("description", binding.description.text.toString())
            putString("occurredAt", occurredAt.toString())
            putString("category", selectedCategoryId)
            putLong("source", accounts.getOrNull(binding.account.selectedItemPosition)?.id ?: 0)
            putLong("destination", accounts.getOrNull(binding.destination.selectedItemPosition)?.id ?: 0)
        } else draft)
        super.onSaveInstanceState(outState)
    }
}

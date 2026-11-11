package com.aravind.wallet.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.ArrayAdapter
import androidx.activity.addCallback
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
        binding.account.isSaveEnabled = false
        binding.destination.isSaveEnabled = false
        onBackPressedDispatcher.addCallback(this) { if (model.operation.value != Operation.Working) finish() }
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
                            is LedgerState.Ready -> if (!initialized) initialize(state.ledger) else refreshCategories(state.ledger)
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
        val record = ledger.records.find { it.id == recordId }
        if (recordId != 0L && record == null) {
            finish()
            return
        }
        accounts = ledger.accounts
        originalCategoryId = record?.categoryId
        preservedNote = record?.note.orEmpty()
        val names = accounts.map { it.name }
        binding.account.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        binding.destination.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
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
        refreshCategories(ledger, draft?.getString("category") ?: record?.categoryId ?: "FOOD")
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

    private fun refreshCategories(ledger: Ledger, wantedId: String? = categoryChoices.getOrNull(binding.category.selectedItemPosition)?.id) {
        val available = ledger.categories.filter { !it.isArchived || it.id == originalCategoryId }
        if (available != categoryChoices) {
            categoryChoices = available
            binding.category.adapter = CategoryChoices(this, available)
            val chosen = available.indexOfFirst { it.id == wantedId }
            binding.category.setSelection(if (chosen >= 0) chosen else 0)
        }
        updateEnabled()
    }

    private fun updateDateTime() {
        binding.date.text = occurredAt.toLocalDate().display()
        binding.time.text = occurredAt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun updateType() {
        val transfer = type == RecordType.TRANSFER
        binding.category.isVisible = !transfer
        binding.categoryLabel.isVisible = !transfer
        binding.destination.isVisible = transfer
        binding.destinationLabel.isVisible = transfer
        binding.transferHelp.isVisible = transfer
        binding.transferHelp.setText(if (accounts.size >= 2) R.string.transfer_help else R.string.transfer_needs_accounts)
        binding.accountLabel.setText(if (transfer) R.string.from_account else R.string.account)
        updateEnabled()
    }

    private fun save() {
        binding.error.isVisible = false
        val amount = try {
            Money.parse(binding.amount.text.toString())
        } catch (error: IllegalArgumentException) {
            binding.amount.error = error.message
            binding.amount.requestFocus()
            return
        }
        if (binding.description.text.isNullOrBlank()) {
            binding.description.error = "Enter a description or payee."
            binding.description.requestFocus()
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
            categoryId = if (type == RecordType.TRANSFER) null else categoryChoices.getOrNull(binding.category.selectedItemPosition)?.id,
            occurredAt = occurredAt, description = binding.description.text.toString(), note = preservedNote,
        ))
    }

    private fun updateEnabled() {
        val busy = model.operation.value == Operation.Working || model.operation.value == Operation.Saved
        binding.form.enableTree(!busy)
        binding.back.isEnabled = !busy
        binding.save.isEnabled = initialized && !busy && accounts.isNotEmpty() &&
            (if (type == RecordType.TRANSFER) accounts.size >= 2 else categoryChoices.isNotEmpty())
        binding.delete.isEnabled = initialized && !busy
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
            putString("category", categoryChoices.getOrNull(binding.category.selectedItemPosition)?.id)
            putLong("source", accounts.getOrNull(binding.account.selectedItemPosition)?.id ?: 0)
            putLong("destination", accounts.getOrNull(binding.destination.selectedItemPosition)?.id ?: 0)
        } else draft)
        super.onSaveInstanceState(outState)
    }
}

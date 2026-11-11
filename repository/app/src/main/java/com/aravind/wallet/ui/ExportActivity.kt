package com.aravind.wallet.ui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.aravind.wallet.R
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.databinding.ActivityExportBinding
import com.aravind.wallet.domain.CsvExporter
import com.aravind.wallet.domain.RecordType
import java.io.IOException
import java.io.OutputStreamWriter
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ExportState {
    data object Idle : ExportState
    data object Choosing : ExportState
    data object Writing : ExportState
    data object Failed : ExportState
    data class Saved(val count: Int) : ExportState
}

class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WalletApplication
    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state
    var accountIds: Set<Long>? = null
    var recordType: RecordType? = null

    fun choose(type: RecordType? = recordType): Boolean {
        if (_state.value == ExportState.Choosing || _state.value == ExportState.Writing) return false
        recordType = type
        _state.value = ExportState.Choosing
        return true
    }
    fun pickerFailed() { _state.value = ExportState.Failed }
    fun selected(uri: Uri?) {
        if (_state.value == ExportState.Writing) return
        if (uri == null) { _state.value = ExportState.Idle; return }
        _state.value = ExportState.Writing
        val type = recordType
        val scope = accountIds?.toSet()
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val snapshot = app.repository.snapshot()
                    val stream = app.contentResolver.openOutputStream(uri, "w") ?: throw IOException("No output stream")
                    OutputStreamWriter(stream, Charsets.UTF_8).buffered().use { CsvExporter.write(snapshot, it, type, scope) }
                }
                _state.value = ExportState.Saved(count)
            } catch (cancelled: CancellationException) {
                removePartial(uri)
                throw cancelled
            } catch (_: Exception) {
                removePartial(uri)
                _state.value = ExportState.Failed
            }
        }
    }

    private suspend fun removePartial(uri: Uri) = withContext(NonCancellable + Dispatchers.IO) {
        runCatching { DocumentsContract.deleteDocument(app.contentResolver, uri) }
        Unit
    }
}

class ExportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExportBinding
    private val model: ExportViewModel by viewModels()
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { model.selected(it) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)
        if (savedInstanceState != null && model.state.value == ExportState.Idle) {
            model.recordType = savedInstanceState.getString("exportType")?.let(RecordType::valueOf)
        }
        binding.exportType.check(when (model.recordType) {
            RecordType.INCOME -> R.id.export_income
            RecordType.EXPENSE -> R.id.export_expenses
            RecordType.TRANSFER -> R.id.export_transfers
            null -> R.id.export_all
        })
        onBackPressedDispatcher.addCallback(this) { if (model.state.value != ExportState.Writing) finish() }
        lifecycleScope.launch {
            val accounts = (application as WalletApplication).repository.snapshot().accounts
            val restored = savedInstanceState?.getLongArray("accounts")?.toSet()
            if (model.accountIds == null) model.accountIds = restored ?: accounts.map { it.id }.toSet()
            accounts.forEach { account -> binding.accountChoices.addView(android.widget.CheckBox(this@ExportActivity).apply {
                text = account.name; isChecked = account.id in model.accountIds.orEmpty()
                setOnCheckedChangeListener { _, checked -> model.accountIds = if (checked) model.accountIds.orEmpty() + account.id else model.accountIds.orEmpty() - account.id }
            }) }
        }
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.save.setOnClickListener {
            val type = when (binding.exportType.checkedRadioButtonId) {
                R.id.export_income -> RecordType.INCOME
                R.id.export_expenses -> RecordType.EXPENSE
                R.id.export_transfers -> RecordType.TRANSFER
                else -> null
            }
            if (model.accountIds.isNullOrEmpty()) { Toast.makeText(this, "Select at least one account.", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            if (model.choose(type)) {
                try { createDocument.launch("wallet-transactions-${LocalDate.now()}.csv") }
                catch (_: RuntimeException) { model.pickerFailed() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    if (state is ExportState.Saved) {
                        Toast.makeText(this@ExportActivity, resources.getQuantityString(R.plurals.export_saved, state.count, state.count), Toast.LENGTH_LONG).show()
                        finish()
                    }
                    binding.error.isVisible = state == ExportState.Failed
                    binding.error.setText(R.string.export_failed)
                    binding.save.isEnabled = state != ExportState.Writing && state != ExportState.Choosing
                    binding.exportType.enableTree(binding.save.isEnabled)
                    binding.accountChoices.enableTree(binding.save.isEnabled)
                    binding.back.isEnabled = state != ExportState.Writing
                    binding.save.setText(when (state) {
                        ExportState.Writing -> R.string.saving
                        ExportState.Choosing -> R.string.choosing_location
                        else -> R.string.choose_save
                    })
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val type = when (binding.exportType.checkedRadioButtonId) {
            R.id.export_income -> RecordType.INCOME
            R.id.export_expenses -> RecordType.EXPENSE
            R.id.export_transfers -> RecordType.TRANSFER
            else -> null
        }
        model.accountIds?.let { outState.putLongArray("accounts", it.toLongArray()) }
        outState.putString("exportType", type?.name)
        super.onSaveInstanceState(outState)
    }
}

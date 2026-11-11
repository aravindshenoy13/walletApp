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

    fun choose(): Boolean {
        if (_state.value == ExportState.Choosing || _state.value == ExportState.Writing) return false
        _state.value = ExportState.Choosing
        return true
    }
    fun pickerFailed() { _state.value = ExportState.Failed }
    fun selected(uri: Uri?) {
        if (_state.value == ExportState.Writing) return
        if (uri == null) { _state.value = ExportState.Idle; return }
        _state.value = ExportState.Writing
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val snapshot = app.repository.snapshot()
                    val stream = app.contentResolver.openOutputStream(uri, "w") ?: throw IOException("No output stream")
                    OutputStreamWriter(stream, Charsets.UTF_8).buffered().use { CsvExporter.write(snapshot, it) }
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
        onBackPressedDispatcher.addCallback(this) { if (model.state.value != ExportState.Writing) finish() }
        binding.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.save.setOnClickListener {
            if (model.choose()) {
                try { createDocument.launch("wallet-transactions-${LocalDate.now()}.csv") }
                catch (_: RuntimeException) { model.pickerFailed() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    if (state is ExportState.Saved) {
                        Toast.makeText(this@ExportActivity, getString(R.string.export_saved, state.count), Toast.LENGTH_LONG).show()
                        finish()
                    }
                    binding.error.isVisible = state == ExportState.Failed
                    binding.error.setText(R.string.export_failed)
                    binding.save.isEnabled = state != ExportState.Writing && state != ExportState.Choosing
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
}

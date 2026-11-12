package com.aravind.wallet.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.WalletBackup
import com.aravind.wallet.domain.Ledger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface BackupState {
    data object Idle : BackupState
    data object ChoosingBackup : BackupState
    data object ChoosingRestore : BackupState
    data object Working : BackupState
    data class Confirm(val ledger: Ledger) : BackupState
    data class Result(val message: String) : BackupState
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as WalletApplication
    private val mutableState = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = mutableState

    fun choose(restore: Boolean): Boolean {
        if (state.value != BackupState.Idle) return false
        mutableState.value = if (restore) BackupState.ChoosingRestore else BackupState.ChoosingBackup
        return true
    }

    fun dismiss() {
        if (state.value is BackupState.Confirm || state.value is BackupState.Result) mutableState.value = BackupState.Idle
    }

    fun pickerFailed() { mutableState.value = BackupState.Result("Could not open the document picker.") }

    fun selected(uri: Uri?, restore: Boolean) {
        val expected = if (restore) BackupState.ChoosingRestore else BackupState.ChoosingBackup
        // Idle also accepts a picker result delivered after process recreation.
        if (state.value != expected && state.value != BackupState.Idle) return
        if (uri == null) { mutableState.value = BackupState.Idle; return }
        work {
            if (restore) {
                val ledger = withContext(Dispatchers.IO) {
                    val bytes = requireNotNull(app.contentResolver.openInputStream(uri)).use { stream ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            require(out.size() + count <= 20 * 1024 * 1024) { "Backup exceeds the 20 MB limit." }
                            out.write(buffer, 0, count)
                        }
                        out.toByteArray()
                    }
                    WalletBackup.decode(bytes.toString(Charsets.UTF_8))
                }
                BackupState.Confirm(ledger)
            } else {
                try {
                    withContext(Dispatchers.IO) {
                        val text = WalletBackup.encode(app.repository.snapshot())
                        requireNotNull(app.contentResolver.openOutputStream(uri, "w"))
                            .bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                    }
                    BackupState.Result("Full backup saved.")
                } catch (e: Exception) {
                    withContext(NonCancellable + Dispatchers.IO) {
                        runCatching { android.provider.DocumentsContract.deleteDocument(app.contentResolver, uri) }
                    }
                    throw e
                }
            }
        }
    }

    fun confirm() {
        val backup = (state.value as? BackupState.Confirm)?.ledger ?: return
        work {
            withContext(Dispatchers.IO) { app.repository.restore(backup) }
            BackupState.Result("Backup restored.")
        }
    }

    private fun work(block: suspend () -> BackupState) {
        mutableState.value = BackupState.Working
        viewModelScope.launch {
            try { mutableState.value = block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.value = BackupState.Result(e.message ?: "Could not complete this operation.") }
        }
    }
}

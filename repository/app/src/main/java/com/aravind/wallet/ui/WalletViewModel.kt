package com.aravind.wallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aravind.wallet.WalletApplication
import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletRecord
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.domain.Ledger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LedgerState {
    data object Loading : LedgerState
    data class Ready(val ledger: Ledger) : LedgerState
    data object Failed : LedgerState
}

sealed interface Operation {
    data object Idle : Operation
    data object Working : Operation
    data object Saved : Operation
    data class Failed(val message: String?) : Operation
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WalletApplication).repository
    val ledger: StateFlow<LedgerState> = repository.ledger
        .map<Ledger, LedgerState> { LedgerState.Ready(it) }
        .catch { emit(LedgerState.Failed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerState.Loading)

    private val _operation = MutableStateFlow<Operation>(Operation.Idle)
    val operation: StateFlow<Operation> = _operation

    fun saveAccount(account: Account) = perform { repository.saveAccount(account) }
    fun saveRecord(record: WalletRecord) = perform { repository.saveRecord(record) }
    fun deleteRecord(id: Long) = perform { repository.deleteRecord(id) }
    fun saveCategory(category: WalletCategory, isNew: Boolean) = perform { repository.saveCategory(category, isNew) }
    fun archiveCategory(id: String) = perform { repository.archiveCategory(id) }

    // The write outlives Activity recreation; repeated taps cannot create duplicate entries.
    private fun perform(block: suspend () -> Any) {
        if (_operation.value == Operation.Working || _operation.value == Operation.Saved) return
        _operation.value = Operation.Working
        viewModelScope.launch {
            try {
                block()
                _operation.value = Operation.Saved
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _operation.value = Operation.Failed((error as? IllegalArgumentException)?.message)
            }
        }
    }
}

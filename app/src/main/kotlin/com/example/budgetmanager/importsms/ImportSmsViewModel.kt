package com.example.budgetmanager.importsms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportSmsState(
    val isScanning: Boolean = false,
    val resultMessage: String? = null,
    val permissionMissing: Boolean = false
)

@HiltViewModel
class ImportSmsViewModel @Inject constructor(
    private val scanner: SmsInboxScanner
) : ViewModel() {

    private val _state = MutableStateFlow(ImportSmsState())
    val state: StateFlow<ImportSmsState> = _state.asStateFlow()

    fun scanSince(startMillis: Long) {
        if (_state.value.isScanning) return
        if (!scanner.hasReadSmsPermission()) {
            _state.update { it.copy(permissionMissing = true, resultMessage = null) }
            return
        }
        _state.update { it.copy(isScanning = true, resultMessage = null, permissionMissing = false) }
        viewModelScope.launch {
            val result = scanner.scanSince(startMillis)
            _state.update {
                it.copy(
                    isScanning = false,
                    resultMessage = "Scanned ${result.messagesScanned} messages · imported ${result.transactionsImported} transactions"
                )
            }
        }
    }
}

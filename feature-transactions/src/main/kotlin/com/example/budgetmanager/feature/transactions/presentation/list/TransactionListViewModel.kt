package com.example.budgetmanager.feature.transactions.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val state: StateFlow<TransactionListState> = combine(
        getTransactionsUseCase(),
        _searchQuery
    ) { transactions, query ->
        TransactionListState(
            transactions = transactions.filter {
                it.merchantName.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
            },
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionListState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}

package com.example.budgetmanager.feature.transactions.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.ExpenseSplitRepository
import com.example.budgetmanager.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

enum class DateRangeFilter(val label: String) {
    All("All time"),
    Last7Days("Last 7 days"),
    Last30Days("Last 30 days"),
    ThisMonth("This month"),
    ThisYear("This year");

    /** Inclusive lower bound in epoch millis (0 = no bound). */
    fun startMillis(): Long {
        if (this == All) return 0L
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (this) {
            Last7Days -> cal.add(Calendar.DAY_OF_YEAR, -6)
            Last30Days -> cal.add(Calendar.DAY_OF_YEAR, -29)
            ThisMonth -> cal.set(Calendar.DAY_OF_MONTH, 1)
            ThisYear -> cal.set(Calendar.DAY_OF_YEAR, 1)
            All -> {}
        }
        return cal.timeInMillis
    }
}

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val typeFilter: TransactionType? = null,
    val categoryFilter: Long? = null,
    val dateRange: DateRangeFilter = DateRangeFilter.All,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    /** transactionId -> your per-person share, for split transactions only. */
    val shareByTransaction: Map<Long, Double> = emptyMap(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    expenseSplitRepository: ExpenseSplitRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    private val _categoryFilter = MutableStateFlow<Long?>(null)
    private val _dateRange = MutableStateFlow(DateRangeFilter.All)

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pair each transaction list with a map of transactionId -> your split share.
    private val transactionsWithShare = combine(
        getTransactionsUseCase(),
        expenseSplitRepository.getAllSplits()
    ) { transactions, splits ->
        val shareByTx = splits.associate { (split, _) -> split.transactionId to split.amountPerPerson }
        transactions to shareByTx
    }

    val state: StateFlow<TransactionListState> = combine(
        transactionsWithShare,
        _searchQuery,
        _typeFilter,
        _categoryFilter,
        _dateRange
    ) { (transactions, shareByTx), query, type, category, range ->
        val startMillis = range.startMillis()
        val filtered = transactions.filter { tx ->
            (query.isBlank() ||
                tx.merchantName.contains(query, ignoreCase = true) ||
                tx.notes.contains(query, ignoreCase = true)) &&
                (type == null || tx.transactionType == type) &&
                (category == null || tx.categoryId == category) &&
                (tx.timestamp >= startMillis)
        }
        TransactionListState(
            transactions = filtered,
            searchQuery = query,
            typeFilter = type,
            categoryFilter = category,
            dateRange = range,
            totalIncome = filtered.filter { it.transactionType == TransactionType.Income && !it.excludeFromBudget }.sumOf { it.amount },
            totalExpense = filtered.filter { it.transactionType == TransactionType.Expense && !it.excludeFromBudget }.sumOf { it.amount },
            shareByTransaction = shareByTx
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionListState())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun setTypeFilter(type: TransactionType?) { _typeFilter.value = type }
    fun setCategoryFilter(categoryId: Long?) { _categoryFilter.value = categoryId }
    fun setDateRange(range: DateRangeFilter) { _dateRange.value = range }
}

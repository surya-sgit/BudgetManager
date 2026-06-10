package com.example.budgetmanager.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.usecase.GetMonthlySummaryUseCase
import com.example.budgetmanager.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        getMonthlySummaryUseCase()
            .onEach { summary ->
                _state.update { it.copy(
                    totalIncome = summary.totalIncome,
                    totalExpense = summary.totalExpense
                ) }
            }
            .launchIn(viewModelScope)

        getTransactionsUseCase()
            .onEach { transactions ->
                _state.update { it.copy(
                    recentTransactions = transactions.take(5)
                ) }
            }
            .launchIn(viewModelScope)
    }
}

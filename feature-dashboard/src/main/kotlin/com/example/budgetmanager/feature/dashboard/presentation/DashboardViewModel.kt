package com.example.budgetmanager.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.SpendingSnapshot
import com.example.budgetmanager.domain.usecase.CalculateSmartBudgetUseCase
import com.example.budgetmanager.domain.usecase.DetectRecurringTransactionsUseCase
import com.example.budgetmanager.domain.usecase.GetCategorySpendingUseCase
import com.example.budgetmanager.domain.usecase.GetMonthlySummaryUseCase
import com.example.budgetmanager.domain.usecase.GetMonthlyTrendUseCase
import com.example.budgetmanager.domain.usecase.GetSpendingInsightUseCase
import com.example.budgetmanager.domain.usecase.GetTransactionsUseCase
import com.example.budgetmanager.domain.usecase.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val calculateSmartBudgetUseCase: CalculateSmartBudgetUseCase,
    private val getCategorySpendingUseCase: GetCategorySpendingUseCase,
    private val getMonthlyTrendUseCase: GetMonthlyTrendUseCase,
    private val detectRecurringTransactionsUseCase: DetectRecurringTransactionsUseCase,
    private val getSpendingInsightUseCase: GetSpendingInsightUseCase,
    private val accountRepository: AccountRepository,
    private val updateAccountUseCase: UpdateAccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState(isLoading = true))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var insightGenerated = false

    init {
        loadDashboardData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDashboardData() {
        val summaryFlow = getMonthlySummaryUseCase()

        summaryFlow
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
                    recentTransactions = transactions.take(5),
                    isLoading = false
                ) }
            }
            .launchIn(viewModelScope)

        getCategorySpendingUseCase()
            .onEach { breakdown ->
                _state.update { it.copy(categorySpending = breakdown) }
            }
            .launchIn(viewModelScope)

        getMonthlyTrendUseCase()
            .onEach { trend ->
                _state.update { it.copy(monthlyTrend = trend) }
            }
            .launchIn(viewModelScope)

        detectRecurringTransactionsUseCase()
            .onEach { recurring ->
                _state.update { it.copy(recurring = recurring) }
            }
            .launchIn(viewModelScope)

        // Use actual monthly income as the budget so Smart Budget reflects real salary
        summaryFlow
            .flatMapLatest { summary ->
                calculateSmartBudgetUseCase(summary.totalIncome.coerceAtLeast(1.0))
            }
            .onEach { smartBudget ->
                _state.update { state ->
                    state.copy(smartBudget = if (state.totalIncome > 0) smartBudget else null)
                }
            }
            .launchIn(viewModelScope)
    }

    /** Generates an AI insight once data is available. Safe to call repeatedly; runs once unless [force]. */
    fun generateInsight(force: Boolean = false) {
        val current = _state.value
        if (current.isInsightLoading) return
        if (insightGenerated && !force) return
        if (current.totalExpense <= 0 && current.totalIncome <= 0) return

        insightGenerated = true
        _state.update { it.copy(isInsightLoading = true) }
        viewModelScope.launch {
            val insight = getSpendingInsightUseCase(
                SpendingSnapshot(
                    totalIncome = current.totalIncome,
                    totalExpense = current.totalExpense,
                    categoryBreakdown = current.categorySpending,
                    monthlyTrend = current.monthlyTrend
                )
            )
            _state.update { it.copy(aiInsight = insight, isInsightLoading = false) }
        }
    }

    fun updateSalaryDate(newDate: Int) {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
            val account = accounts.firstOrNull { it.accountType != com.example.budgetmanager.domain.model.AccountType.CreditCard }
                ?: accounts.firstOrNull()

            account?.let {
                updateAccountUseCase(it.copy(salaryDate = newDate))
            }
        }
    }
}

package com.example.budgetmanager.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.usecase.CalculateSmartBudgetUseCase
import com.example.budgetmanager.domain.usecase.GetMonthlySummaryUseCase
import com.example.budgetmanager.domain.usecase.GetTransactionsUseCase
import com.example.budgetmanager.domain.usecase.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val calculateSmartBudgetUseCase: CalculateSmartBudgetUseCase,
    private val accountRepository: AccountRepository,
    private val updateAccountUseCase: UpdateAccountUseCase
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

        // Assuming a default total budget of 30000 for smart budget calculation
        calculateSmartBudgetUseCase(30000.0)
            .onEach { smartBudget ->
                _state.update { it.copy(smartBudget = smartBudget) }
            }
            .launchIn(viewModelScope)
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

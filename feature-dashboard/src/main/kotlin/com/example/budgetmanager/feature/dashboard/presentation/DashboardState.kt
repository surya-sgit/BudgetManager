package com.example.budgetmanager.feature.dashboard.presentation

import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.usecase.CalculateSmartBudgetUseCase
import com.example.budgetmanager.domain.usecase.CategorySpend
import com.example.budgetmanager.domain.usecase.MonthlyExpense
import com.example.budgetmanager.domain.usecase.RecurringTransaction

data class DashboardState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val smartBudget: CalculateSmartBudgetUseCase.SmartBudgetState? = null,
    val categorySpending: List<CategorySpend> = emptyList(),
    val monthlyTrend: List<MonthlyExpense> = emptyList(),
    val recurring: List<RecurringTransaction> = emptyList(),
    val aiInsight: String? = null,
    val isInsightLoading: Boolean = false,
    val isLoading: Boolean = false
) {
    val netSavings: Double get() = totalIncome - totalExpense
}

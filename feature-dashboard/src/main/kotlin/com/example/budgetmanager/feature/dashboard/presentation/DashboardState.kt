package com.example.budgetmanager.feature.dashboard.presentation

import com.example.budgetmanager.domain.model.Transaction

data class DashboardState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
) {
    val netSavings: Double get() = totalIncome - totalExpense
}

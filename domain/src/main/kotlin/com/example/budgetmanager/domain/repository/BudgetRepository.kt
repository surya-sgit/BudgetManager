package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
    fun getAllBudgets(): Flow<List<Budget>>
    fun getBudgetByCategory(categoryId: Long): Flow<Budget?>
}

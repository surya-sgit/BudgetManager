package com.example.budgetmanager.data.repository

import com.example.budgetmanager.core.database.dao.BudgetDao
import com.example.budgetmanager.core.database.entities.BudgetEntity
import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {
    override suspend fun insertBudget(budget: Budget): Long {
        return budgetDao.insertBudget(BudgetEntity(id = budget.id, categoryId = budget.categoryId, monthlyLimit = budget.monthlyLimit))
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(BudgetEntity(id = budget.id, categoryId = budget.categoryId, monthlyLimit = budget.monthlyLimit))
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(BudgetEntity(id = budget.id, categoryId = budget.categoryId, monthlyLimit = budget.monthlyLimit))
    }

    override fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets().map { list ->
            list.map { Budget(it.id, it.categoryId, it.monthlyLimit) }
        }
    }

    override fun getBudgetByCategory(categoryId: Long): Flow<Budget?> {
        return budgetDao.getBudgetByCategory(categoryId).map { 
            it?.let { Budget(it.id, it.categoryId, it.monthlyLimit) }
        }
    }
}

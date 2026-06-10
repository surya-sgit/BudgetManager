package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.repository.BudgetRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.*
import javax.inject.Inject

data class BudgetProgress(
    val budget: Budget,
    val categoryName: String,
    val currentSpending: Double,
    val limit: Double
) {
    val progress: Float = if (limit > 0) (currentSpending / limit).toFloat() else 0f
    val isExceeded: Boolean = currentSpending > limit
}

class GetBudgetWithProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: com.example.budgetmanager.domain.repository.CategoryRepository
) {
    operator fun invoke(): Flow<List<BudgetProgress>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val startOfMonth = calendar.timeInMillis
        val endOfMonth = System.currentTimeMillis()

        return combine(
            budgetRepository.getAllBudgets(),
            categoryRepository.getAllCategories(),
            transactionRepository.getCategoryWiseExpenses(startOfMonth, endOfMonth)
        ) { budgets, categories, expenses ->
            budgets.map { budget ->
                val category = categories.find { it.id == budget.categoryId }
                val categoryName = category?.name ?: "Unknown"
                val currentSpending = expenses[budget.categoryId] ?: 0.0
                
                BudgetProgress(
                    budget = budget,
                    categoryName = categoryName,
                    currentSpending = currentSpending,
                    limit = budget.monthlyLimit
                ) 
            }
        }
    }
}

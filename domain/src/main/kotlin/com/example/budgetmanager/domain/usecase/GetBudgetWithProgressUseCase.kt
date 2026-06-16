package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.BudgetRepository
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.*
import javax.inject.Inject

data class BudgetProgress(
    val budget: Budget,
    val categoryName: String,
    val currentSpending: Double,
    val limit: Double,
    val dailySuggestedLimit: Double = 0.0
) {
    val progress: Float = if (limit > 0) (currentSpending / limit).toFloat() else 0f
    val isExceeded: Boolean = currentSpending > limit
}

class GetBudgetWithProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<List<BudgetProgress>> {
        return accountRepository.getAllAccounts().flatMapLatest { accounts ->
            val account = accounts.firstOrNull()
            val cycleStartDate = account?.cycleStartDate?.takeIf { it > 0L }
                ?: run {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.timeInMillis
                }
            val cycleDurationDays = account?.cycleDurationDays ?: 30
            val now = System.currentTimeMillis()
            val daysElapsed = ((now - cycleStartDate) / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            val cycleExpired = daysElapsed >= cycleDurationDays
            val remainingDays = (cycleDurationDays - daysElapsed).coerceAtLeast(1)

            combine(
                budgetRepository.getAllBudgets(),
                categoryRepository.getAllCategories(),
                transactionRepository.getCategoryWiseExpenses(cycleStartDate, now)
            ) { budgets, categories, expenses ->
                budgets.map { budget ->
                    val category = categories.find { it.id == budget.categoryId }
                    val categoryName = category?.name ?: "Unknown"
                    val currentSpending = expenses[budget.categoryId] ?: 0.0
                    val remaining = (budget.monthlyLimit - currentSpending).coerceAtLeast(0.0)
                    val dailySuggestedLimit = if (cycleExpired) 0.0 else remaining / remainingDays

                    BudgetProgress(
                        budget = budget,
                        categoryName = categoryName,
                        currentSpending = currentSpending,
                        limit = budget.monthlyLimit,
                        dailySuggestedLimit = dailySuggestedLimit
                    )
                }
            }
        }
    }
}

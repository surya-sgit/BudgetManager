package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.*
import javax.inject.Inject

/** A single category's share of this month's spending, for the breakdown chart. */
data class CategorySpend(
    val categoryName: String,
    val colorHex: String,
    val amount: Double
)

class GetCategorySpendingUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<CategorySpend>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis

        return combine(
            transactionRepository.getCategoryWiseExpenses(startTime, endTime),
            categoryRepository.getAllCategories()
        ) { spendByCategory, categories ->
            spendByCategory
                .mapNotNull { (categoryId, amount) ->
                    val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                    CategorySpend(category.name, category.color, amount)
                }
                .filter { it.amount > 0 }
                .sortedByDescending { it.amount }
        }
    }
}

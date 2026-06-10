package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(): Flow<List<Budget>> {
        return budgetRepository.getAllBudgets()
    }
}

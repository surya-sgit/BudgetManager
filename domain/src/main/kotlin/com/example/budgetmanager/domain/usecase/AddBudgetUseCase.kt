package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.repository.BudgetRepository
import javax.inject.Inject

class AddBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Long {
        return repository.insertBudget(budget)
    }
}

package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.SavingsGoal
import com.example.budgetmanager.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavingsGoalsUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    operator fun invoke(): Flow<List<SavingsGoal>> = repository.getAll()
}

class AddSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal): Long = repository.insert(goal)
}

class UpdateSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.update(goal)

    /** Adds [amount] to the goal's saved balance (negative to withdraw), clamped at zero. */
    suspend fun contribute(goal: SavingsGoal, amount: Double) {
        repository.update(goal.copy(savedAmount = (goal.savedAmount + amount).coerceAtLeast(0.0)))
    }
}

class DeleteSavingsGoalUseCase @Inject constructor(
    private val repository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal) = repository.delete(goal)
}

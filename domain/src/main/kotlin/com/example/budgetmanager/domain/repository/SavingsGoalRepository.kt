package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    suspend fun insert(goal: SavingsGoal): Long
    suspend fun update(goal: SavingsGoal)
    suspend fun delete(goal: SavingsGoal)
    fun getAll(): Flow<List<SavingsGoal>>
}

package com.example.budgetmanager.data.repository

import com.example.budgetmanager.core.database.dao.SavingsGoalDao
import com.example.budgetmanager.core.database.entities.SavingsGoalEntity
import com.example.budgetmanager.domain.model.SavingsGoal
import com.example.budgetmanager.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavingsGoalRepositoryImpl @Inject constructor(
    private val dao: SavingsGoalDao
) : SavingsGoalRepository {

    override suspend fun insert(goal: SavingsGoal): Long = dao.insert(goal.toEntity())

    override suspend fun update(goal: SavingsGoal) = dao.update(goal.toEntity())

    override suspend fun delete(goal: SavingsGoal) = dao.delete(goal.toEntity())

    override fun getAll(): Flow<List<SavingsGoal>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }
}

private fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    targetDate = targetDate
)

private fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    savedAmount = savedAmount,
    targetDate = targetDate
)

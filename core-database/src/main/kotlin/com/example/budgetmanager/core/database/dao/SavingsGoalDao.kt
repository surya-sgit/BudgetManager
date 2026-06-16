package com.example.budgetmanager.core.database.dao

import androidx.room.*
import com.example.budgetmanager.core.database.entities.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals ORDER BY id DESC")
    fun getAll(): Flow<List<SavingsGoalEntity>>
}

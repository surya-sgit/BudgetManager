package com.example.budgetmanager.core.database.dao

import androidx.room.*
import com.example.budgetmanager.core.database.entities.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplit(expenseSplit: ExpenseSplitEntity): Long

    @Update
    suspend fun updateExpenseSplit(expenseSplit: ExpenseSplitEntity)

    @Delete
    suspend fun deleteExpenseSplit(expenseSplit: ExpenseSplitEntity)

    @Query("SELECT * FROM expense_splits WHERE transactionId = :transactionId")
    fun getSplitsByTransaction(transactionId: Long): Flow<List<ExpenseSplitEntity>>
}

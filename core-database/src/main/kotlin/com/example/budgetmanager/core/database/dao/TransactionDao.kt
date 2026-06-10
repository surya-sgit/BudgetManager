package com.example.budgetmanager.core.database.dao

import androidx.room.*
import com.example.budgetmanager.core.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'Income' AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalIncome(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'Expense' AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalExpense(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT categoryId, SUM(amount) as totalAmount FROM transactions WHERE transactionType = 'Expense' AND timestamp BETWEEN :startTime AND :endTime GROUP BY categoryId")
    fun getCategoryWiseExpenses(startTime: Long, endTime: Long): Flow<List<CategorySum>>
}

data class CategorySum(
    val categoryId: Long,
    val totalAmount: Double
)

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

    @Query("SELECT COUNT(*) FROM transactions WHERE sourceSmsHash = :hash")
    suspend fun countBySourceHash(hash: String): Int

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE transactionType = 'Income' AND excludeFromBudget = 0 AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalIncome(startTime: Long, endTime: Long): Flow<Double?>

    // When a transaction is split, only the user's per-person share counts toward
    // spending; the rest is money owed back. Falls back to the full amount when unsplit.
    @Query(
        "SELECT SUM(CASE WHEN s.amountPerPerson IS NOT NULL THEN s.amountPerPerson ELSE t.amount END) " +
        "FROM transactions t LEFT JOIN expense_splits s ON s.transactionId = t.id " +
        "WHERE t.transactionType = 'Expense' AND t.excludeFromBudget = 0 AND t.timestamp BETWEEN :startTime AND :endTime"
    )
    fun getTotalExpense(startTime: Long, endTime: Long): Flow<Double?>

    @Query(
        "SELECT t.categoryId AS categoryId, " +
        "SUM(CASE WHEN s.amountPerPerson IS NOT NULL THEN s.amountPerPerson ELSE t.amount END) AS totalAmount " +
        "FROM transactions t LEFT JOIN expense_splits s ON s.transactionId = t.id " +
        "WHERE t.transactionType = 'Expense' AND t.excludeFromBudget = 0 AND t.timestamp BETWEEN :startTime AND :endTime " +
        "GROUP BY t.categoryId"
    )
    fun getCategoryWiseExpenses(startTime: Long, endTime: Long): Flow<List<CategorySum>>
}

data class CategorySum(
    val categoryId: Long,
    val totalAmount: Double
)

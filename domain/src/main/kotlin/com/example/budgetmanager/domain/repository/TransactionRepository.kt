package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(id: Long): Transaction?
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    fun getTotalIncome(startTime: Long, endTime: Long): Flow<Double>
    fun getTotalExpense(startTime: Long, endTime: Long): Flow<Double>
    fun getCategoryWiseExpenses(startTime: Long, endTime: Long): Flow<Map<Long, Double>>
}

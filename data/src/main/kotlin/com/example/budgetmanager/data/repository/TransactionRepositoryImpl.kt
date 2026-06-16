package com.example.budgetmanager.data.repository

import com.example.budgetmanager.core.database.dao.TransactionDao
import com.example.budgetmanager.data.mapper.toDomain
import com.example.budgetmanager.data.mapper.toEntity
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun existsBySourceHash(hash: String): Boolean {
        return transactionDao.countBySourceHash(hash) > 0
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId).map { list -> list.map { it.toDomain() } }
    }

    override fun getTotalIncome(startTime: Long, endTime: Long): Flow<Double> {
        return transactionDao.getTotalIncome(startTime, endTime).map { it ?: 0.0 }
    }

    override fun getTotalExpense(startTime: Long, endTime: Long): Flow<Double> {
        return transactionDao.getTotalExpense(startTime, endTime).map { it ?: 0.0 }
    }

    override fun getCategoryWiseExpenses(startTime: Long, endTime: Long): Flow<Map<Long, Double>> {
        return transactionDao.getCategoryWiseExpenses(startTime, endTime).map { list ->
            list.associate { it.categoryId to it.totalAmount }
        }
    }
}

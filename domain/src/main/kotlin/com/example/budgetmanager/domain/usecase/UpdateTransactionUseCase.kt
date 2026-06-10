package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.repository.TransactionRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        transactionRepository.updateTransaction(transaction)
    }
}

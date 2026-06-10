package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    suspend fun insertCreditCard(creditCard: CreditCard): Long
    suspend fun updateCreditCard(creditCard: CreditCard)
    suspend fun deleteCreditCard(creditCard: CreditCard)
    fun getAllCreditCards(): Flow<List<CreditCard>>
    suspend fun getCreditCardById(id: Long): CreditCard?
}

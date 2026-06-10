package com.example.budgetmanager.data.repository

import com.example.budgetmanager.core.database.dao.CreditCardDao
import com.example.budgetmanager.core.database.entities.CreditCardEntity
import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.repository.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CreditCardRepositoryImpl @Inject constructor(
    private val creditCardDao: CreditCardDao
) : CreditCardRepository {
    override suspend fun insertCreditCard(creditCard: CreditCard): Long {
        return creditCardDao.insertCreditCard(creditCard.toEntity())
    }

    override suspend fun updateCreditCard(creditCard: CreditCard) {
        creditCardDao.updateCreditCard(creditCard.toEntity())
    }

    override suspend fun deleteCreditCard(creditCard: CreditCard) {
        creditCardDao.deleteCreditCard(creditCard.toEntity())
    }

    override fun getAllCreditCards(): Flow<List<CreditCard>> {
        return creditCardDao.getAllCreditCards().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCreditCardById(id: Long): CreditCard? {
        return creditCardDao.getCreditCardById(id)?.toDomain()
    }

    private fun CreditCardEntity.toDomain() = CreditCard(
        id = id,
        cardName = cardName,
        creditLimit = creditLimit,
        availableLimit = availableLimit,
        currentSpend = currentSpend,
        statementBalance = statementBalance,
        billingDate = billingDate,
        dueDate = dueDate
    )

    private fun CreditCard.toEntity() = CreditCardEntity(
        id = id,
        cardName = cardName,
        creditLimit = creditLimit,
        availableLimit = availableLimit,
        currentSpend = currentSpend,
        statementBalance = statementBalance,
        billingDate = billingDate,
        dueDate = dueDate
    )
}

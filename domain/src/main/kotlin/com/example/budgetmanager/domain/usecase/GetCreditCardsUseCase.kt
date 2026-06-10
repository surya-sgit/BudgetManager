package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.repository.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCreditCardsUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    operator fun invoke(): Flow<List<CreditCard>> = repository.getAllCreditCards()
}

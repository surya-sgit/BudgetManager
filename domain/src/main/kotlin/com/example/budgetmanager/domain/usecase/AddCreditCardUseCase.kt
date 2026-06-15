package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.repository.CreditCardRepository
import javax.inject.Inject

class AddCreditCardUseCase @Inject constructor(
    private val repository: CreditCardRepository
) {
    suspend operator fun invoke(creditCard: CreditCard): Long {
        return repository.insertCreditCard(creditCard)
    }
}

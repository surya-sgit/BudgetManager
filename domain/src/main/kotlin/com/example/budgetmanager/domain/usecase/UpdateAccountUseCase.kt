package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.repository.AccountRepository
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(account: Account) {
        repository.updateAccount(account)
    }
}

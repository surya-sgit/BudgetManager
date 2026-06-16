package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ResetBudgetCycleUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke() {
        val account = accountRepository.getAllAccounts().first().firstOrNull() ?: return
        accountRepository.updateAccount(
            account.copy(cycleStartDate = System.currentTimeMillis())
        )
    }
}

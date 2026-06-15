package com.example.budgetmanager.domain.usecase

import app.cash.turbine.test
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.AccountType
import com.example.budgetmanager.domain.model.PaymentCycle
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class CalculateSmartBudgetUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = CalculateSmartBudgetUseCase(transactionRepository, accountRepository)

    @Test
    fun `calculate smart budget correctly`() = runTest {
        val salaryDay = 1
        val totalBudget = 30000.0
        val totalExpense = 5000.0
        
        val account = Account(
            id = 1,
            name = "Main",
            bankName = "Bank",
            accountLast4 = "1234",
            accountType = AccountType.Savings,
            salaryDate = salaryDay,
            paymentCycle = PaymentCycle.Monthly
        )

        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        // Mocking getTotalExpense for the current cycle
        every { transactionRepository.getTotalExpense(any(), any()) } returns flowOf(totalExpense)

        useCase(totalBudget).test {
            val state = awaitItem()
            
            assertEquals(25000.0, state.remainingBudget, 0.1)
            // If it's early in the month, daily limit should be around 25000 / ~30
            val calendar = Calendar.getInstance()
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val expectedDaysRemaining = daysInMonth - currentDay + 1
            
            assertEquals(expectedDaysRemaining, state.daysRemaining)
            assertEquals(25000.0 / expectedDaysRemaining, state.dailyLimit, 0.1)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}

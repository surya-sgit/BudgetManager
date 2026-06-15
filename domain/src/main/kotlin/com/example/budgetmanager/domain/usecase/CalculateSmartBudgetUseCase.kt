package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

class CalculateSmartBudgetUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    data class SmartBudgetState(
        val remainingBudget: Double,
        val dailyLimit: Double,
        val daysRemaining: Int,
        val nextSalaryDate: Long
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(totalBudget: Double): Flow<SmartBudgetState> {
        return accountRepository.getAllAccounts().flatMapLatest { accounts ->
            // Assuming we use the first account's salary date for now
            val account = accounts.firstOrNull { it.accountType != com.example.budgetmanager.domain.model.AccountType.CreditCard } 
                ?: accounts.firstOrNull()
            
            val salaryDay = account?.salaryDate ?: 1
            val (startTime, endTime) = calculateCycleRange(salaryDay)

            transactionRepository.getTotalExpense(startTime, System.currentTimeMillis()).map { totalExpense ->
                val remaining = totalBudget - totalExpense
                
                val calendar = Calendar.getInstance()
                val today = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val daysRemaining = ((endTime - today) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                
                SmartBudgetState(
                    remainingBudget = remaining.coerceAtLeast(0.0),
                    dailyLimit = (remaining / daysRemaining).coerceAtLeast(0.0),
                    daysRemaining = daysRemaining,
                    nextSalaryDate = endTime
                )
            }
        }
    }

    private fun calculateCycleRange(salaryDay: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        if (currentDay < salaryDay) {
            calendar.add(Calendar.MONTH, -1)
        }
        
        calendar.set(Calendar.DAY_OF_MONTH, salaryDay)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        val endTime = calendar.timeInMillis
        
        return Pair(startTime, endTime)
    }
}

package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.AccountType
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

    private val dayMillis = 1000L * 60 * 60 * 24

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(totalBudget: Double): Flow<SmartBudgetState> {
        return accountRepository.getAllAccounts().flatMapLatest { accounts ->
            val account = accounts.firstOrNull { it.accountType != AccountType.CreditCard }
                ?: accounts.firstOrNull()

            // Prefer the dynamic cycle anchored to the actual (detected/declared) salary
            // date. Salary rarely lands on a fixed day, so cycleStartDate — updated when a
            // salary SMS is detected — is the source of truth. Fall back to the salary
            // day-of-month only before the first salary has ever been seen.
            val (cycleStart, cycleEnd) =
                if (account != null && account.cycleStartDate > 0L) {
                    val start = account.cycleStartDate
                    start to (start + account.cycleDurationDays * dayMillis)
                } else {
                    calculateCycleRange(account?.salaryDate ?: 1)
                }

            transactionRepository.getTotalExpense(cycleStart, System.currentTimeMillis()).map { totalExpense ->
                val remaining = totalBudget - totalExpense
                val todayStart = startOfToday()
                val daysRemaining = ((cycleEnd - todayStart) / dayMillis).toInt().coerceAtLeast(1)

                SmartBudgetState(
                    remainingBudget = remaining.coerceAtLeast(0.0),
                    dailyLimit = (remaining / daysRemaining).coerceAtLeast(0.0),
                    daysRemaining = daysRemaining,
                    nextSalaryDate = cycleEnd
                )
            }
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Fallback cycle based on a fixed salary day-of-month, used until a real salary is seen. */
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

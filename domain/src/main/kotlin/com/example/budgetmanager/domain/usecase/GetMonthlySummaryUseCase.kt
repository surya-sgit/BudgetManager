package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.*
import javax.inject.Inject

class GetMonthlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    data class MonthlySummary(
        val totalIncome: Double,
        val totalExpense: Double
    )

    operator fun invoke(): Flow<MonthlySummary> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return combine(
            transactionRepository.getTotalIncome(startTime, endTime),
            transactionRepository.getTotalExpense(startTime, endTime)
        ) { income, expense ->
            MonthlySummary(income, expense)
        }
    }
}

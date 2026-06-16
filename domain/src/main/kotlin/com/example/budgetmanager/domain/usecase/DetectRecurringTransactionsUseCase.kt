package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.model.TransactionType
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import javax.inject.Inject

/** A merchant that appears to bill on a regular (roughly monthly) cadence. */
data class RecurringTransaction(
    val merchantName: String,
    val averageAmount: Double,
    val occurrences: Int,
    val averageIntervalDays: Int
)

class DetectRecurringTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    private val dayMillis = 1000L * 60 * 60 * 24

    operator fun invoke(): Flow<List<RecurringTransaction>> {
        return transactionRepository.getAllTransactions().map { transactions ->
            transactions
                .filter { it.transactionType == TransactionType.Expense }
                .groupBy { it.merchantName.trim().lowercase() }
                .mapNotNull { (_, group) ->
                    if (group.size < 2) return@mapNotNull null
                    val sorted = group.sortedBy { it.timestamp }
                    val gaps = sorted.zipWithNext { a, b -> (b.timestamp - a.timestamp) / dayMillis }
                    val avgGap = gaps.average()
                    // Treat ~weekly to ~monthly cadence as recurring (5–40 day average gap),
                    // and require the gaps to be reasonably consistent.
                    val consistent = gaps.all { abs(it - avgGap) <= 10 }
                    if (avgGap in 5.0..40.0 && consistent) {
                        RecurringTransaction(
                            merchantName = group.first().merchantName,
                            averageAmount = group.map { it.amount }.average(),
                            occurrences = group.size,
                            averageIntervalDays = avgGap.toInt()
                        )
                    } else null
                }
                .sortedByDescending { it.averageAmount }
        }
    }
}

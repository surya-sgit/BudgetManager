package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/** Total expense for a single month, for the trend bar chart. */
data class MonthlyExpense(
    val monthLabel: String,
    val total: Double
)

class GetMonthlyTrendUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(months: Int = 6): Flow<List<MonthlyExpense>> {
        val labelFormat = SimpleDateFormat("MMM", Locale.getDefault())

        // Build [start,end) ranges for the last `months` calendar months, oldest first.
        val ranges = (months - 1 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -offset)
            }
            val start = cal.timeInMillis
            val label = labelFormat.format(cal.time)
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            Triple(label, start, end)
        }

        val flows = ranges.map { (_, start, end) ->
            transactionRepository.getTotalExpense(start, end)
        }

        return combine(flows) { totals ->
            ranges.mapIndexed { index, (label, _, _) ->
                MonthlyExpense(label, totals[index])
            }
        }
    }
}

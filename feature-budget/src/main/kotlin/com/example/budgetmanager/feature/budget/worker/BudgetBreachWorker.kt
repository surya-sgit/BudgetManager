package com.example.budgetmanager.feature.budget.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budgetmanager.domain.repository.BudgetRepository
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * Daily check that warns the user when any category's spend for the current month
 * approaches (>=80%) or exceeds (>=100%) its budget limit.
 */
@HiltWorker
class BudgetBreachWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val budgets = budgetRepository.getAllBudgets().first().filter { it.monthlyLimit > 0 }
        if (budgets.isEmpty()) return Result.success()

        val (start, end) = currentMonthRange()
        val spendByCategory = transactionRepository.getCategoryWiseExpenses(start, end).first()
        val categories = categoryRepository.getAllCategories().first()

        budgets.forEach { budget ->
            val spent = spendByCategory[budget.categoryId] ?: 0.0
            val ratio = spent / budget.monthlyLimit
            val categoryName = categories.firstOrNull { it.id == budget.categoryId }?.name ?: "A category"

            when {
                ratio >= 1.0 -> showNotification(
                    id = budget.categoryId.toInt(),
                    title = "Budget exceeded: $categoryName",
                    text = "You've spent ₹${"%.0f".format(spent)} of your ₹${"%.0f".format(budget.monthlyLimit)} limit."
                )
                ratio >= 0.8 -> showNotification(
                    id = budget.categoryId.toInt(),
                    title = "Approaching budget: $categoryName",
                    text = "You've used ${(ratio * 100).toInt()}% of your ₹${"%.0f".format(budget.monthlyLimit)} limit."
                )
            }
        }
        return Result.success()
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return start to calendar.timeInMillis
    }

    private fun showNotification(id: Int, title: String, text: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "budget_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Budget Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}

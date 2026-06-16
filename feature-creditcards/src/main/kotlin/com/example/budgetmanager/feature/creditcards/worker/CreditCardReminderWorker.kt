package com.example.budgetmanager.feature.creditcards.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.budgetmanager.domain.repository.CreditCardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*

@HiltWorker
class CreditCardReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CreditCardRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val cards = repository.getAllCreditCards().first()
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        cards.forEach { card ->
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val daysUntilDue = if (card.dueDate >= currentDay) {
                card.dueDate - currentDay
            } else {
                // Due date is in the next month
                (daysInMonth - currentDay) + card.dueDate
            }
            if (daysUntilDue in listOf(7, 3, 1)) {
                showNotification(card.cardName, daysUntilDue)
            }
        }

        return Result.success()
    }

    private fun showNotification(cardName: String, days: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "credit_card_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Credit Card Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Credit Card Bill Due")
            .setContentText("$cardName bill is due in $days days. Please pay to avoid charges.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(cardName.hashCode(), notification)
    }
}

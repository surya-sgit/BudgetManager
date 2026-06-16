package com.example.budgetmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.budgetmanager.feature.budget.worker.BudgetBreachWorker
import com.example.budgetmanager.feature.creditcards.worker.CreditCardReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BudgetManagerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundWork()
    }

    private fun scheduleBackgroundWork() {
        val workManager = WorkManager.getInstance(this)

        val budgetWork = PeriodicWorkRequestBuilder<BudgetBreachWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            "budget_breach_check",
            ExistingPeriodicWorkPolicy.KEEP,
            budgetWork
        )

        val creditCardWork = PeriodicWorkRequestBuilder<CreditCardReminderWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            "credit_card_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            creditCardWork
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

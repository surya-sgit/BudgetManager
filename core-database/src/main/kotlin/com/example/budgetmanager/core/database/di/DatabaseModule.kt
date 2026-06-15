package com.example.budgetmanager.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgetmanager.core.database.BudgetDatabase
import com.example.budgetmanager.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBudgetDatabase(
        @ApplicationContext context: Context
    ): BudgetDatabase {
        // val passphrase = "secure_passphrase".toByteArray()
        // val factory = net.sqlcipher.database.SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget_manager.db"
        )
        // .openHelperFactory(factory)
        .addCallback(object : RoomDatabase.Callback() {
            // Seeding could be done here if we had the DAO, 
            // but usually it's better to do it via a Worker or on first launch in ViewModel/Repository
        })
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTransactionDao(database: BudgetDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: BudgetDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideAccountDao(database: BudgetDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCreditCardDao(database: BudgetDatabase): CreditCardDao = database.creditCardDao()

    @Provides
    fun provideBudgetDao(database: BudgetDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideExpenseSplitDao(database: BudgetDatabase): ExpenseSplitDao = database.expenseSplitDao()
}

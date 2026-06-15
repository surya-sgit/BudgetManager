package com.example.budgetmanager.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.budgetmanager.core.database.dao.*
import com.example.budgetmanager.core.database.entities.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        CreditCardEntity::class,
        BudgetEntity::class,
        ExpenseSplitEntity::class,
        SplitParticipantEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun budgetDao(): BudgetDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
}

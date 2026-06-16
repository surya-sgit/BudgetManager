package com.example.budgetmanager.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetmanager.core.database.BudgetDatabase
import com.example.budgetmanager.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add budget cycle tracking columns to accounts
        database.execSQL("ALTER TABLE accounts ADD COLUMN cycleStartDate INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE accounts ADD COLUMN cycleDurationDays INTEGER NOT NULL DEFAULT 30")

        // Fix transactions FK constraints: recreate table with RESTRICT instead of SET_DEFAULT/CASCADE
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transactions_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount REAL NOT NULL,
                transactionType TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                merchantName TEXT NOT NULL,
                accountId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                smsBody TEXT NOT NULL,
                sourceSmsHash TEXT NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                userModified INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        database.execSQL("INSERT OR IGNORE INTO transactions_new SELECT * FROM transactions")
        database.execSQL("DROP TABLE transactions")
        database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // New in v5: savings goals.
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                targetAmount REAL NOT NULL,
                savedAmount REAL NOT NULL,
                targetDate INTEGER
            )
        """.trimIndent())

        // Normalize the transactions foreign keys to RESTRICT. Some older v4 databases
        // still carry the original CASCADE / SET_DEFAULT keys, which fail schema
        // validation. Rebuilding here is idempotent for databases already on RESTRICT.
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transactions_v5 (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount REAL NOT NULL,
                transactionType TEXT NOT NULL,
                categoryId INTEGER NOT NULL,
                merchantName TEXT NOT NULL,
                accountId INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                smsBody TEXT NOT NULL,
                sourceSmsHash TEXT NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                userModified INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        database.execSQL("""
            INSERT OR IGNORE INTO transactions_v5
                (id, amount, transactionType, categoryId, merchantName, accountId,
                 timestamp, smsBody, sourceSmsHash, notes, userModified)
            SELECT id, amount, transactionType, categoryId, merchantName, accountId,
                   timestamp, smsBody, sourceSmsHash, notes, userModified
            FROM transactions
        """.trimIndent())
        database.execSQL("DROP TABLE transactions")
        database.execSQL("ALTER TABLE transactions_v5 RENAME TO transactions")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Unknown'")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS merchant_rules (" +
                "merchantKey TEXT PRIMARY KEY NOT NULL, " +
                "categoryId INTEGER NOT NULL)"
        )
    }
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE credit_cards ADD COLUMN lastFourDigits TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE credit_cards ADD COLUMN needsSetup INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN excludeFromBudget INTEGER NOT NULL DEFAULT 0")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBudgetDatabase(
        @ApplicationContext context: Context
    ): BudgetDatabase {
        return Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget_manager.db"
        )
        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {})
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

    @Provides
    fun provideSavingsGoalDao(database: BudgetDatabase): SavingsGoalDao = database.savingsGoalDao()

    @Provides
    fun provideMerchantRuleDao(database: BudgetDatabase): MerchantRuleDao = database.merchantRuleDao()
}

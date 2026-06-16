package com.example.budgetmanager.data.di

import com.example.budgetmanager.data.insights.GeminiInsightsGenerator
import com.example.budgetmanager.data.repository.*
import com.example.budgetmanager.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCreditCardRepository(
        creditCardRepositoryImpl: CreditCardRepositoryImpl
    ): CreditCardRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindExpenseSplitRepository(
        expenseSplitRepositoryImpl: ExpenseSplitRepositoryImpl
    ): ExpenseSplitRepository

    @Binds
    @Singleton
    abstract fun bindInsightsRepository(
        geminiInsightsGenerator: GeminiInsightsGenerator
    ): InsightsRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(
        savingsGoalRepositoryImpl: SavingsGoalRepositoryImpl
    ): SavingsGoalRepository

    @Binds
    @Singleton
    abstract fun bindMerchantCategoryRepository(
        merchantCategoryRepositoryImpl: MerchantCategoryRepositoryImpl
    ): MerchantCategoryRepository
}

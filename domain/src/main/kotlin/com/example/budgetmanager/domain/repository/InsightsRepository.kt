package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.usecase.CategorySpend
import com.example.budgetmanager.domain.usecase.MonthlyExpense

/** A snapshot of the user's finances used to generate a natural-language insight. */
data class SpendingSnapshot(
    val totalIncome: Double,
    val totalExpense: Double,
    val categoryBreakdown: List<CategorySpend>,
    val monthlyTrend: List<MonthlyExpense>
)

interface InsightsRepository {
    /** Returns a short, friendly insight about the user's spending. Never throws. */
    suspend fun generateInsight(snapshot: SpendingSnapshot): String
}

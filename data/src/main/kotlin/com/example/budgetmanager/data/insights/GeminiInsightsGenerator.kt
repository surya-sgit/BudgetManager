package com.example.budgetmanager.data.insights

import android.util.Log
import com.example.budgetmanager.domain.repository.InsightsRepository
import com.example.budgetmanager.domain.repository.SpendingSnapshot
import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject
import javax.inject.Named

class GeminiInsightsGenerator @Inject constructor(
    @Named("gemini_api_key") private val apiKey: String
) : InsightsRepository {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = apiKey
    )

    override suspend fun generateInsight(snapshot: SpendingSnapshot): String {
        // Nothing to analyse yet.
        if (snapshot.totalExpense <= 0 && snapshot.totalIncome <= 0) {
            return "Add a few transactions and I'll start spotting patterns in your spending."
        }

        val topCategories = snapshot.categoryBreakdown.take(5).joinToString(", ") {
            "${it.categoryName}: ₹${"%.0f".format(it.amount)}"
        }
        val trend = snapshot.monthlyTrend.joinToString(", ") {
            "${it.monthLabel}: ₹${"%.0f".format(it.total)}"
        }

        val prompt = """
            You are a friendly personal-finance assistant. Based on the data below, write ONE short,
            specific, actionable insight (max 2 sentences, no preamble, use ₹ for amounts).
            Income this month: ₹${"%.0f".format(snapshot.totalIncome)}.
            Expense this month: ₹${"%.0f".format(snapshot.totalExpense)}.
            Top categories: $topCategories.
            Monthly expense trend: $trend.
        """.trimIndent()

        return try {
            model.generateContent(prompt).text?.trim()?.takeIf { it.isNotBlank() }
                ?: ruleBasedInsight(snapshot)
        } catch (e: Exception) {
            Log.e("BudgetDebug", "Insight generation failed, using fallback", e)
            ruleBasedInsight(snapshot)
        }
    }

    /** Deterministic fallback used when the AI call is unavailable. */
    private fun ruleBasedInsight(snapshot: SpendingSnapshot): String {
        val top = snapshot.categoryBreakdown.firstOrNull()
        val savingsRate = if (snapshot.totalIncome > 0) {
            ((snapshot.totalIncome - snapshot.totalExpense) / snapshot.totalIncome * 100).toInt()
        } else null

        return when {
            top != null && snapshot.totalExpense > 0 -> {
                val pct = (top.amount / snapshot.totalExpense * 100).toInt()
                val savingsNote = savingsRate?.let { " You're saving about $it% of your income." } ?: ""
                "${top.categoryName} is your biggest expense this month at ₹${"%.0f".format(top.amount)} ($pct% of spending).$savingsNote"
            }
            savingsRate != null -> "You're saving about $savingsRate% of your income this month."
            else -> "Keep logging transactions to unlock personalised insights."
        }
    }
}

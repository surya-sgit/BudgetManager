package com.example.budgetmanager.data.sms

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import javax.inject.Inject
import javax.inject.Named

/**
 * Last-resort category guesser: used only when the rule-based classifier returns "Other".
 * The caller persists the result as a learned merchant rule, so each unknown merchant hits the
 * cloud model at most once — afterwards it's classified instantly offline.
 */
class GeminiCategoryClassifier @Inject constructor(
    @Named("gemini_api_key") private val apiKey: String
) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = apiKey
    )

    /** Returns one of [candidateCategories] (exact name) or null if undecided/unavailable. */
    suspend fun classify(
        merchant: String,
        smsBody: String,
        candidateCategories: List<String>
    ): String? {
        if (candidateCategories.isEmpty()) return null
        val prompt = """
            Categorize this bank transaction into exactly ONE of these categories:
            ${candidateCategories.joinToString(", ")}.
            Merchant: "$merchant".
            SMS: "$smsBody".
            Reply with ONLY the category name, exactly as written in the list above.
            If you cannot tell, reply "Other".
        """.trimIndent()

        return try {
            val text = model.generateContent(prompt).text?.trim()?.trim('"', '.', ' ') ?: return null
            // Only accept an exact (case-insensitive) match to a known category.
            candidateCategories.firstOrNull { it.equals(text, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e("BudgetDebug", "AI category classification failed", e)
            null
        }
    }
}

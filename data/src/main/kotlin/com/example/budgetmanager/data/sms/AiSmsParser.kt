package com.example.budgetmanager.data.sms

import android.util.Log
import com.example.budgetmanager.domain.model.ParsedSmsTransaction
import com.example.budgetmanager.domain.model.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named

@Serializable
data class GeminiTransactionResponse(
    val transactionType: String,
    val amount: Double,
    val merchant: String
)

class GeminiSmsParser @Inject constructor(
    @Named("gemini_api_key") private val apiKey: String
) {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = apiKey
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun parseWithAi(smsBody: String, timestamp: Long): ParsedSmsTransaction? {
        Log.d("BudgetDebug", "API Key starts with: ${apiKey.take(4)}")
        val prompt = """
            Extract transaction details from this SMS: "$smsBody".
            Respond with a strict JSON object containing:
            "transactionType" (either "DEBIT" or "CREDIT"),
            "amount" (numeric),
            "merchant" (string).
            If it's not a bank transaction, return null.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: return null
            Log.d("BudgetDebug", "Gemini raw response: $jsonText")
            val geminiResponse = json.decodeFromString<GeminiTransactionResponse>(jsonText)
            
            ParsedSmsTransaction(
                amount = geminiResponse.amount,
                transactionType = if (geminiResponse.transactionType.uppercase() == "CREDIT") 
                    TransactionType.Income else TransactionType.Expense,
                merchantName = geminiResponse.merchant,
                accountLast4 = "AI", // Gemini might not always reliably extract this without specific instructions
                timestamp = timestamp,
                smsBody = smsBody
            )
        } catch (e: Exception) {
            Log.e("BudgetDebug", "Gemini API Call Failed", e)
            null
        }
    }
}

package com.example.budgetmanager.data.sms

import android.util.Log
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.AccountType
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsProcessor @Inject constructor(
    private val smsParser: SmsParser,
    private val geminiSmsParser: GeminiSmsParser,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun processSms(sender: String, smsBody: String, timestamp: Long) {
        Log.d("BudgetDebug", "processSms called. Sender: $sender, Body: $smsBody")
        var parsedTransaction = smsParser.parse(sender, smsBody, timestamp)

        if (parsedTransaction == null) {
            Log.d("BudgetDebug", "Regex failed to parse. Falling back to Gemini AI.")
            // Fallback to AI
            parsedTransaction = geminiSmsParser.parseWithAi(smsBody, timestamp)
        }

        if (parsedTransaction == null) {
            Log.d("BudgetDebug", "Both Regex and AI failed to parse transaction. Aborting.")
            return
        }

        Log.d("BudgetDebug", "Transaction parsed: $parsedTransaction")

        // 1. Find or create account
        var accounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        if (accounts.isEmpty()) {
            Log.d("BudgetDebug", "No accounts exist. Creating a 'Primary Account' for you.")
            val defaultAccount = Account(
                id = 0,
                name = "Primary Account",
                bankName = "Default Bank",
                accountLast4 = parsedTransaction.accountLast4,
                accountType = AccountType.Savings
            )
            accountRepository.insertAccount(defaultAccount)
            accounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        }

        val account = accounts.find { it.accountLast4 == parsedTransaction.accountLast4 }
            ?: accounts.firstOrNull()

        if (account == null) {
            Log.d("BudgetDebug", "Account matching failed even after creation effort. Aborting.")
            return
        }

        Log.d("BudgetDebug", "Linking transaction to account: ${account.name} (${account.accountLast4})")

        // 2. Find category (Classification logic)
        var categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
        if (categories.isEmpty()) {
            Log.d("BudgetDebug", "No categories exist. Creating 'Other' category.")
            val defaultCategory = Category(
                id = 0,
                name = "Other",
                icon = "category",
                color = "#9E9E9E"
            )
            categoryRepository.insertCategory(defaultCategory)
            categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
        }

        val categoryId = classifyCategory(parsedTransaction.merchantName, categories)
        Log.d("BudgetDebug", "Classified category ID: $categoryId")

        // 3. Create and save transaction
        val transaction = Transaction(
            id = 0,
            amount = parsedTransaction.amount,
            transactionType = parsedTransaction.transactionType,
            categoryId = categoryId,
            merchantName = parsedTransaction.merchantName,
            accountId = account.id,
            timestamp = timestamp,
            smsBody = smsBody,
            sourceSmsHash = smsBody.hashCode().toString(),
            notes = "",
            userModified = false
        )

        Log.d("BudgetDebug", "Saving transaction: $transaction")
        transactionRepository.insertTransaction(transaction)
        Log.d("BudgetDebug", "Transaction saved successfully.")
    }

    private fun classifyCategory(merchantName: String, categories: List<com.example.budgetmanager.domain.model.Category>): Long {
        val merchant = merchantName.lowercase()
        return when {
            merchant.contains("swiggy") || merchant.contains("zomato") -> 
                categories.find { it.name == "Food" }?.id ?: 0
            merchant.contains("uber") || merchant.contains("ola") -> 
                categories.find { it.name == "Travel" }?.id ?: 0
            merchant.contains("amazon") || merchant.contains("flipkart") -> 
                categories.find { it.name == "Shopping" }?.id ?: 0
            else -> categories.find { it.name == "Other" }?.id ?: 0
        }
    }
}

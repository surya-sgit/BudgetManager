package com.example.budgetmanager.data.sms

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
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun processSms(smsBody: String, timestamp: Long) {
        val parsedTransaction = smsParser.parse(smsBody, timestamp) ?: return

        // 1. Find or create account
        val accounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        val account = accounts.find { it.accountLast4 == parsedTransaction.accountLast4 }
            ?: accounts.firstOrNull() // Default to first account if not found
            ?: return // Should ideally create a "Default/Unknown" account here

        // 2. Find category (Classification logic)
        val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
        val categoryId = classifyCategory(parsedTransaction.merchantName, categories)

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

        transactionRepository.insertTransaction(transaction)
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

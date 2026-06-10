package com.example.budgetmanager.domain.model

data class Transaction(
    val id: Long,
    val amount: Double,
    val transactionType: TransactionType,
    val categoryId: Long,
    val merchantName: String,
    val accountId: Long,
    val timestamp: Long,
    val smsBody: String,
    val sourceSmsHash: String,
    val notes: String,
    val userModified: Boolean
)

enum class TransactionType {
    Income, Expense
}

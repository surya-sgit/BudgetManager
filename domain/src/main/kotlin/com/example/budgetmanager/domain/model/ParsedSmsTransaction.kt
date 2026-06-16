package com.example.budgetmanager.domain.model

data class ParsedSmsTransaction(
    val amount: Double,
    val transactionType: TransactionType,
    val merchantName: String,
    val accountLast4: String,
    val timestamp: Long,
    val smsBody: String,
    val referenceNumber: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.Unknown
)

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
    val userModified: Boolean,
    val paymentMethod: PaymentMethod = PaymentMethod.Unknown,
    /** Card-bill payments / transfers between own accounts — kept for the record but not counted as spend. */
    val excludeFromBudget: Boolean = false
)

enum class TransactionType {
    Income, Expense
}

enum class PaymentMethod {
    Card, Upi, NetBanking, Wallet, Cash, Unknown;

    /** Short, user-facing label. */
    val label: String
        get() = when (this) {
            Card -> "Card"
            Upi -> "UPI"
            NetBanking -> "Net Banking"
            Wallet -> "Wallet"
            Cash -> "Cash"
            Unknown -> ""
        }
}

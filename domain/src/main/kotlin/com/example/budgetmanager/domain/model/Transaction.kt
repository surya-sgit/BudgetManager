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
    Card, DebitCard, Upi, NetBanking, Wallet, Cash, Unknown;

    /** Short, user-facing label. */
    val label: String
        get() = when (this) {
            Card -> "Credit Card"
            DebitCard -> "Debit Card"
            Upi -> "UPI"
            NetBanking -> "Net Banking"
            Wallet -> "Wallet"
            Cash -> "Cash"
            Unknown -> ""
        }

    /** Credit-card spend that should count toward a credit card's statement. */
    val isCreditCard: Boolean get() = this == Card
}

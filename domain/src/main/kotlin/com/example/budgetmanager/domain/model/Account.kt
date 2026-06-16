package com.example.budgetmanager.domain.model

data class Account(
    val id: Long,
    val name: String,
    val bankName: String,
    val accountLast4: String,
    val accountType: AccountType,
    val salaryDate: Int = 1,
    val paymentCycle: PaymentCycle = PaymentCycle.Monthly,
    val cycleStartDate: Long = 0L,
    val cycleDurationDays: Int = 30
)

enum class AccountType {
    Savings, Current, CreditCard, Wallet
}

enum class PaymentCycle {
    Monthly, Weekly
}

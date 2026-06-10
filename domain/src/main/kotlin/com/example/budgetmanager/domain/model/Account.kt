package com.example.budgetmanager.domain.model

data class Account(
    val id: Long,
    val name: String,
    val bankName: String,
    val accountLast4: String,
    val accountType: AccountType
)

enum class AccountType {
    Savings, Current, CreditCard, Wallet
}

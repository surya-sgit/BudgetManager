package com.example.budgetmanager.domain.model

data class CreditCard(
    val id: Long,
    val cardName: String,
    val creditLimit: Double,
    val availableLimit: Double,
    val currentSpend: Double,
    val statementBalance: Double,
    val billingDate: Int,
    val dueDate: Int,
    val lastFourDigits: String = "",
    /** True for cards auto-created from SMS that still need the user to fill in details. */
    val needsSetup: Boolean = false
)

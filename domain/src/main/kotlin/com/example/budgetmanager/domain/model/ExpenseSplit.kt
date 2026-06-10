package com.example.budgetmanager.domain.model

data class ExpenseSplit(
    val id: Long,
    val transactionId: Long,
    val totalParticipants: Int,
    val amountPerPerson: Double,
    val description: String
)

package com.example.budgetmanager.domain.model

data class Budget(
    val id: Long,
    val categoryId: Long,
    val monthlyLimit: Double
)

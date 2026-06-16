package com.example.budgetmanager.domain.model

data class SavingsGoal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: Long? = null
) {
    val progress: Float
        get() = if (targetAmount > 0) (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isComplete: Boolean get() = savedAmount >= targetAmount && targetAmount > 0
}

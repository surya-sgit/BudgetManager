package com.example.budgetmanager.domain.model

data class SplitParticipant(
    val id: Long,
    val splitId: Long,
    val name: String,
    val shareAmount: Double,
    val status: SplitStatus
)

enum class SplitStatus {
    Pending, Paid, Settled
}

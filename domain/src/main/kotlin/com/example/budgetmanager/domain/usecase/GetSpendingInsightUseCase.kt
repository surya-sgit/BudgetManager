package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.InsightsRepository
import com.example.budgetmanager.domain.repository.SpendingSnapshot
import javax.inject.Inject

class GetSpendingInsightUseCase @Inject constructor(
    private val insightsRepository: InsightsRepository
) {
    suspend operator fun invoke(snapshot: SpendingSnapshot): String {
        return insightsRepository.generateInsight(snapshot)
    }
}

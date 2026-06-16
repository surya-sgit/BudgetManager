package com.example.budgetmanager.domain.usecase

import com.example.budgetmanager.domain.repository.MerchantCategoryRepository
import javax.inject.Inject

class LearnMerchantCategoryUseCase @Inject constructor(
    private val merchantCategoryRepository: MerchantCategoryRepository
) {
    suspend operator fun invoke(merchantName: String, categoryId: Long) {
        val key = normalizeMerchant(merchantName)
        if (key.isNotBlank()) merchantCategoryRepository.saveRule(key, categoryId)
    }

    companion object {
        /** Canonical key for a merchant so "AMAZON", "Amazon " etc. map together. */
        fun normalizeMerchant(merchantName: String): String =
            merchantName.lowercase().trim().replace(Regex("\\s+"), " ")
    }
}

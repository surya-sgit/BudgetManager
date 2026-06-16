package com.example.budgetmanager.domain.repository

interface MerchantCategoryRepository {
    /** Returns the learned category for this merchant, or null if none. */
    suspend fun getCategoryIdForMerchant(merchantKey: String): Long?

    /** Remembers that [merchantKey] should map to [categoryId] (overwrites any prior rule). */
    suspend fun saveRule(merchantKey: String, categoryId: Long)
}

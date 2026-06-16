package com.example.budgetmanager.domain.model

/** A learned mapping: whenever this merchant key appears, classify it as [categoryId]. */
data class MerchantCategoryRule(
    val merchantKey: String,
    val categoryId: Long
)

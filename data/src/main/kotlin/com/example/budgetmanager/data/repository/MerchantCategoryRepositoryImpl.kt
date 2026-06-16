package com.example.budgetmanager.data.repository

import com.example.budgetmanager.core.database.dao.MerchantRuleDao
import com.example.budgetmanager.core.database.entities.MerchantRuleEntity
import com.example.budgetmanager.domain.repository.MerchantCategoryRepository
import javax.inject.Inject

class MerchantCategoryRepositoryImpl @Inject constructor(
    private val merchantRuleDao: MerchantRuleDao
) : MerchantCategoryRepository {

    override suspend fun getCategoryIdForMerchant(merchantKey: String): Long? =
        merchantRuleDao.getCategoryId(merchantKey)

    override suspend fun saveRule(merchantKey: String, categoryId: Long) {
        merchantRuleDao.upsert(MerchantRuleEntity(merchantKey = merchantKey, categoryId = categoryId))
    }
}

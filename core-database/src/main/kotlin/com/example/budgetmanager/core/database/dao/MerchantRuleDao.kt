package com.example.budgetmanager.core.database.dao

import androidx.room.*
import com.example.budgetmanager.core.database.entities.MerchantRuleEntity

@Dao
interface MerchantRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRuleEntity)

    @Query("SELECT categoryId FROM merchant_rules WHERE merchantKey = :key LIMIT 1")
    suspend fun getCategoryId(key: String): Long?
}

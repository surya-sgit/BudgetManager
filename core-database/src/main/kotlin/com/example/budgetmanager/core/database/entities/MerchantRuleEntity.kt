package com.example.budgetmanager.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_rules")
data class MerchantRuleEntity(
    @PrimaryKey
    val merchantKey: String,
    val categoryId: Long
)

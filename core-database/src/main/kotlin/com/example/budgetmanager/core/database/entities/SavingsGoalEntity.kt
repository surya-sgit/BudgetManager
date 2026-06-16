package com.example.budgetmanager.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: Long? = null
)

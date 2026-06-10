package com.example.budgetmanager.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardName: String,
    val creditLimit: Double,
    val availableLimit: Double,
    val currentSpend: Double,
    val statementBalance: Double,
    val billingDate: Int,
    val dueDate: Int
)

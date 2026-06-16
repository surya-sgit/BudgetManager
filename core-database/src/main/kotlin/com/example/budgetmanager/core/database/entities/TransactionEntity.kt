package com.example.budgetmanager.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId"), Index("accountId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val transactionType: String, // Income, Expense
    val categoryId: Long,
    val merchantName: String,
    val accountId: Long,
    val timestamp: Long,
    val smsBody: String,
    val sourceSmsHash: String,
    val notes: String = "",
    val userModified: Boolean = false,
    val paymentMethod: String = "Unknown",
    val excludeFromBudget: Boolean = false
)

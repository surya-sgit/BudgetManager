package com.example.budgetmanager.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bankName: String,
    val accountLast4: String,
    val accountType: String, // Savings, Current, CreditCard, Wallet
    val salaryDate: Int = 1,
    val paymentCycle: String = "Monthly",
    val cycleStartDate: Long = 0L,
    val cycleDurationDays: Int = 30
)

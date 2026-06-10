package com.example.budgetmanager.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "split_participants",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseSplitEntity::class,
            parentColumns = ["id"],
            childColumns = ["splitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("splitId")]
)
data class SplitParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val splitId: Long,
    val name: String,
    val shareAmount: Double,
    val status: String // Pending, Paid, Settled
)

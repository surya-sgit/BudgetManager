package com.example.budgetmanager.core.database.dao

import androidx.room.*
import com.example.budgetmanager.core.database.entities.ExpenseSplitEntity
import com.example.budgetmanager.core.database.entities.SplitParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplit(expenseSplit: ExpenseSplitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<SplitParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: SplitParticipantEntity)

    @Transaction
    @Query("SELECT * FROM expense_splits WHERE transactionId = :transactionId")
    fun getSplitWithParticipants(transactionId: Long): Flow<SplitWithParticipants?>

    @Transaction
    @Query("SELECT * FROM expense_splits")
    fun getAllSplitsWithParticipants(): Flow<List<SplitWithParticipants>>
}

data class SplitWithParticipants(
    @Embedded val split: ExpenseSplitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "splitId"
    )
    val participants: List<SplitParticipantEntity>
)

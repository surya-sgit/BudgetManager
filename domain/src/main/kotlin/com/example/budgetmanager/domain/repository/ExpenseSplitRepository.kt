package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.model.ExpenseSplit
import com.example.budgetmanager.domain.model.SplitParticipant
import kotlinx.coroutines.flow.Flow

interface ExpenseSplitRepository {
    suspend fun createSplit(split: ExpenseSplit, participants: List<SplitParticipant>): Long
    suspend fun updateParticipantStatus(participantId: Long, status: com.example.budgetmanager.domain.model.SplitStatus)
    fun getSplitForTransaction(transactionId: Long): Flow<Pair<ExpenseSplit, List<SplitParticipant>>?>
    fun getAllSplits(): Flow<List<Pair<ExpenseSplit, List<SplitParticipant>>>>
}

package com.example.budgetmanager.data.repository

import com.example.budgetmanager.core.database.dao.ExpenseSplitDao
import com.example.budgetmanager.core.database.dao.SplitWithParticipants
import com.example.budgetmanager.core.database.entities.ExpenseSplitEntity
import com.example.budgetmanager.core.database.entities.SplitParticipantEntity
import com.example.budgetmanager.domain.model.ExpenseSplit
import com.example.budgetmanager.domain.model.SplitParticipant
import com.example.budgetmanager.domain.model.SplitStatus
import com.example.budgetmanager.domain.repository.ExpenseSplitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExpenseSplitRepositoryImpl @Inject constructor(
    private val expenseSplitDao: ExpenseSplitDao
) : ExpenseSplitRepository {

    override suspend fun createSplit(split: ExpenseSplit, participants: List<SplitParticipant>): Long {
        val splitId = expenseSplitDao.insertExpenseSplit(split.toEntity())
        expenseSplitDao.insertParticipants(participants.map { it.toEntity(splitId) })
        return splitId
    }

    override suspend fun updateParticipantStatus(participantId: Long, status: SplitStatus) {
        expenseSplitDao.updateParticipantStatus(participantId, status.name)
    }

    override fun getSplitForTransaction(transactionId: Long): Flow<Pair<ExpenseSplit, List<SplitParticipant>>?> {
        return expenseSplitDao.getSplitWithParticipants(transactionId).map { it?.toDomain() }
    }

    override fun getAllSplits(): Flow<List<Pair<ExpenseSplit, List<SplitParticipant>>>> {
        return expenseSplitDao.getAllSplitsWithParticipants().map { list -> list.map { it.toDomain() } }
    }
}

private fun ExpenseSplit.toEntity() = ExpenseSplitEntity(
    id = id,
    transactionId = transactionId,
    totalParticipants = totalParticipants,
    amountPerPerson = amountPerPerson,
    description = description
)

private fun SplitParticipant.toEntity(splitId: Long) = SplitParticipantEntity(
    id = id,
    splitId = splitId,
    name = name,
    shareAmount = shareAmount,
    status = status.name
)

private fun SplitWithParticipants.toDomain(): Pair<ExpenseSplit, List<SplitParticipant>> {
    val domainSplit = ExpenseSplit(
        id = split.id,
        transactionId = split.transactionId,
        totalParticipants = split.totalParticipants,
        amountPerPerson = split.amountPerPerson,
        description = split.description
    )
    val domainParticipants = participants.map { entity ->
        SplitParticipant(
            id = entity.id,
            splitId = entity.splitId,
            name = entity.name,
            shareAmount = entity.shareAmount,
            status = SplitStatus.valueOf(entity.status)
        )
    }
    return Pair(domainSplit, domainParticipants)
}

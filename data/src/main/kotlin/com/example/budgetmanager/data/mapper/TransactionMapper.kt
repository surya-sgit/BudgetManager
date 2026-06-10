package com.example.budgetmanager.data.mapper

import com.example.budgetmanager.core.database.entities.TransactionEntity
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        transactionType = TransactionType.valueOf(transactionType),
        categoryId = categoryId,
        merchantName = merchantName,
        accountId = accountId,
        timestamp = timestamp,
        smsBody = smsBody,
        sourceSmsHash = sourceSmsHash,
        notes = notes,
        userModified = userModified
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        transactionType = transactionType.name,
        categoryId = categoryId,
        merchantName = merchantName,
        accountId = accountId,
        timestamp = timestamp,
        smsBody = smsBody,
        sourceSmsHash = sourceSmsHash,
        notes = notes,
        userModified = userModified
    )
}

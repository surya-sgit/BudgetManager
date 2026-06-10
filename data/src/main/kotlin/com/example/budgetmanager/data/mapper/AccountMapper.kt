package com.example.budgetmanager.data.mapper

import com.example.budgetmanager.core.database.entities.AccountEntity
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.AccountType

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        bankName = bankName,
        accountLast4 = accountLast4,
        accountType = AccountType.valueOf(accountType)
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        bankName = bankName,
        accountLast4 = accountLast4,
        accountType = accountType.name
    )
}

package com.example.budgetmanager.data.mapper

import com.example.budgetmanager.core.database.entities.AccountEntity
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.AccountType
import com.example.budgetmanager.domain.model.PaymentCycle

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        bankName = bankName,
        accountLast4 = accountLast4,
        accountType = AccountType.valueOf(accountType),
        salaryDate = salaryDate,
        paymentCycle = PaymentCycle.valueOf(paymentCycle),
        cycleStartDate = cycleStartDate,
        cycleDurationDays = cycleDurationDays
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        bankName = bankName,
        accountLast4 = accountLast4,
        accountType = accountType.name,
        salaryDate = salaryDate,
        paymentCycle = paymentCycle.name,
        cycleStartDate = cycleStartDate,
        cycleDurationDays = cycleDurationDays
    )
}

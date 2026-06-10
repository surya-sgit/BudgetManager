package com.example.budgetmanager.domain.repository

import com.example.budgetmanager.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
}

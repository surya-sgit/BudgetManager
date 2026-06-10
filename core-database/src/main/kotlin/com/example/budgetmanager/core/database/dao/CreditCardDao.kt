package com.example.budgetmanager.core.database.dao

import androidx.room.*
import com.example.budgetmanager.core.database.entities.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(creditCard: CreditCardEntity): Long

    @Update
    suspend fun updateCreditCard(creditCard: CreditCardEntity)

    @Delete
    suspend fun deleteCreditCard(creditCard: CreditCardEntity)

    @Query("SELECT * FROM credit_cards")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCreditCardById(id: Long): CreditCardEntity?
}

package com.example.budgetmanager.data.di

import com.example.budgetmanager.data.sms.BankSmsParser
import com.example.budgetmanager.data.sms.SmsParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmsModule {
    @Provides
    @Singleton
    fun provideSmsParser(): SmsParser = BankSmsParser()
}

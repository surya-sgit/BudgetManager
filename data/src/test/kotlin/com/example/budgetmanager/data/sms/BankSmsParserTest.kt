package com.example.budgetmanager.data.sms

import com.example.budgetmanager.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BankSmsParserTest {

    private val parser = BankSmsParser()

    @Test
    fun `parse debited sms correctly`() {
        val sms = "Alert: Your Acct XX1234 has been debited for Rs. 500.00 at AMAZON on 10-10-2023."
        val result = parser.parse("BANK-SMS", sms, System.currentTimeMillis())

        assertNotNull(result)
        assertEquals(500.0, result?.amount)
        assertEquals(TransactionType.Expense, result?.transactionType)
        assertEquals("AMAZON", result?.merchantName)
        assertEquals("1234", result?.accountLast4)
    }

    @Test
    fun `parse credited sms correctly`() {
        val sms = "Dear Customer, your Acct XX5678 is credited with INR 10,000.00 on 11-10-23. Info: SALARY."
        val result = parser.parse("BANK-SMS", sms, System.currentTimeMillis())

        assertNotNull(result)
        assertEquals(10000.0, result?.amount)
        assertEquals(TransactionType.Income, result?.transactionType)
        assertEquals("SALARY", result?.merchantName)
        assertEquals("5678", result?.accountLast4)
    }

    @Test
    fun `parse BOB style Dr sms correctly`() {
        val sms = "Rs.429.00 Dr. from A/C XXXXXX6777 and Cr. to DrinkprimeEasebuzz@yesbank. Ref:084810543651. AvlBal:Rs914.53(2026:06:15 08:48:11)."
        val result = parser.parse("6505551212", sms, System.currentTimeMillis())

        assertNotNull(result)
        assertEquals(429.0, result?.amount)
        assertEquals(TransactionType.Expense, result?.transactionType)
        assertEquals("DrinkprimeEasebuzz@yesbank", result?.merchantName)
        assertEquals("6777", result?.accountLast4)
    }

    @Test
    fun `return null for non-transactional sms`() {
        val sms = "Your OTP for login is 123456. Do not share it with anyone."
        val result = parser.parse("VERIFY", sms, System.currentTimeMillis())

        assertEquals(null, result)
    }
}

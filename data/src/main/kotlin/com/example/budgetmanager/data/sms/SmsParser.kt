package com.example.budgetmanager.data.sms

import com.example.budgetmanager.domain.model.ParsedSmsTransaction
import com.example.budgetmanager.domain.model.TransactionType
import java.util.regex.Pattern

interface SmsParser {
    fun parse(smsBody: String, timestamp: Long): ParsedSmsTransaction?
}

class BankSmsParser : SmsParser {
    private val patterns = listOf(
        // HDFC Bank: "Alert: You've spent Rs. 500.00 on your HDFC Bank Card xx1234 at AMAZON on 2023-10-01."
        // SBI: "Transaction of Rs.100.00 made on SBI Debit Card ending 1234 at SWIGGY."
        // ICICI: "Dear Customer, your Acct XX123 is debited for Rs 1000.00 on 01-Oct-23. Info: BIL*Zomato."
        
        // Generic pattern for testing/initial implementation
        Pattern.compile("(?i)(?:rs|inr)\\.?\\s*([\\d,]+\\.?\\d{0,2})\\s*(?:debited|spent|paid|withdrawn|transfer)\\s*.*?(?:at|to|on)\\s*(.*?)\\s*(?:on|using|via|at|\\.)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(?:debited|spent).*?(?:rs|inr)\\.?\\s*([\\d,]+\\.?\\d{0,2}).*?at\\s*(.*?)(?:\\s|\\.)", Pattern.CASE_INSENSITIVE)
    )

    override fun parse(smsBody: String, timestamp: Long): ParsedSmsTransaction? {
        // Simplified parsing logic for demonstration
        // In a real app, this would iterate through bank-specific regex patterns
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                val amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: continue
                val merchant = matcher.group(2) ?: "Unknown Merchant"
                
                // Determine transaction type
                val type = if (smsBody.contains("debited", ignoreCase = true) || 
                               smsBody.contains("spent", ignoreCase = true) ||
                               smsBody.contains("paid", ignoreCase = true)) {
                    TransactionType.Expense
                } else if (smsBody.contains("credited", ignoreCase = true) || 
                           smsBody.contains("received", ignoreCase = true)) {
                    TransactionType.Income
                } else {
                    TransactionType.Expense
                }

                // Extract account last 4 (generic regex)
                val accountMatcher = Pattern.compile("(?i)(?:acct|card|a/c|ending|xx)(\\d{3,4})", Pattern.CASE_INSENSITIVE).matcher(smsBody)
                val accountLast4 = if (accountMatcher.find()) accountMatcher.group(1) ?: "0000" else "0000"

                return ParsedSmsTransaction(
                    amount = amount,
                    transactionType = type,
                    merchantName = merchant.trim(),
                    accountLast4 = accountLast4,
                    timestamp = timestamp,
                    smsBody = smsBody
                )
            }
        }
        return null
    }
}

package com.example.budgetmanager.data.sms

import com.example.budgetmanager.domain.model.ParsedSmsTransaction
import com.example.budgetmanager.domain.model.TransactionType
import java.util.regex.Pattern

interface SmsParser {
    fun parse(sender: String, smsBody: String, timestamp: Long): ParsedSmsTransaction?
}

class BankSmsParser : SmsParser {
    /**
     * Regex Breakdown:
     * 1. Amount: Matches 'Rs.' or 'INR' followed by digits and commas.
     * 2. Type: Matches 'debited', 'spent', 'credited', etc.
     * 3. Merchant: Matches text after 'at', 'to', or 'info:'.
     * 
     * Mock Test Case 1 (Debited): "Alert: Your Acct XX1234 has been debited for Rs. 500.00 at AMAZON on 10-10-2023."
     * Mock Test Case 2 (Credited): "Dear Customer, your Acct XX5678 is credited with INR 10,000.00 on 11-10-23. Info: SALARY."
     */
    private val patterns = listOf(
        // Pattern for "Rs. 500.00 Dr. from A/C ... to Merchant"
        Pattern.compile("(?i)(?:rs|inr)\\.?\\s*([\\d,]+\\.?\\d{0,2})\\s*(Dr\\.|Cr\\.).*?(?:to|at)\\s*(.*?)(?:\\s+\\b(?:on|using|via|and|at|ref)\\b|\\s+\\d|\\.\$|\\. |$)", Pattern.CASE_INSENSITIVE),
        // Pattern for "debited for Rs 500 at Merchant"
        Pattern.compile("(?i)(debited|spent|paid|credited|received).*?(?:rs|inr)\\.?\\s*([\\d,]+\\.?\\d{0,2}).*?(?:at|to|info:)\\s*(.*?)(?:\\s+\\b(?:on|using|via|at|ref)\\b|\\s+\\d|\\.\$|\\. |$)", Pattern.CASE_INSENSITIVE),
        // Pattern for "spent Rs. 500 on your card at Merchant"
        Pattern.compile("(?i)(?:rs|inr)\\.?\\s*([\\d,]+\\.?\\d{0,2})\\s*(debited|spent|paid|credited|received).*?(?:at|to|info:)\\s*(.*?)(?:\\s+\\b(?:on|using|via|at|ref)\\b|\\s+\\d|\\.\$|\\. |$)", Pattern.CASE_INSENSITIVE)
    )

    override fun parse(sender: String, smsBody: String, timestamp: Long): ParsedSmsTransaction? {
        for (pattern in patterns) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                val group1 = matcher.group(1) ?: ""
                val group2 = matcher.group(2) ?: ""
                val group3 = matcher.group(3) ?: "Unknown Merchant"

                val isGroup1Amount = group1.firstOrNull()?.isDigit() == true || group1.contains(",")
                
                val amountStr = if (isGroup1Amount) group1 else group2
                val typeStr = if (isGroup1Amount) group2 else group1
                val merchant = group3

                val amount = amountStr.replace(",", "").toDoubleOrNull() ?: continue
                
                val type = if (typeStr.contains("debited", ignoreCase = true) || 
                               typeStr.contains("spent", ignoreCase = true) ||
                               typeStr.contains("paid", ignoreCase = true) ||
                               typeStr.contains("Dr.", ignoreCase = true)) {
                    TransactionType.Expense
                } else if (typeStr.contains("credited", ignoreCase = true) || 
                           typeStr.contains("received", ignoreCase = true) ||
                           typeStr.contains("Cr.", ignoreCase = true)) {
                    TransactionType.Income
                } else {
                    TransactionType.Expense
                }

                // Extract last 4 digits of account/card — require keyword prefix to avoid matching amounts
                val accountMatcher = Pattern.compile(
                    "(?i)(?:acct|card|a/c|account|ending|xx)\\s*(?:no\\.?\\s*)?[xX*]{0,4}(\\d{4})\\b"
                ).matcher(smsBody)
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

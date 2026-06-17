package com.example.budgetmanager.data.sms

import com.example.budgetmanager.domain.model.ParsedSmsTransaction
import com.example.budgetmanager.domain.model.TransactionType
import java.util.regex.Pattern

interface SmsParser {
    fun parse(sender: String, smsBody: String, timestamp: Long): ParsedSmsTransaction?
}

/**
 * Parses bank transaction SMS. Key correctness rules:
 *  - Bank SMS often carry TWO amounts: the transaction amount and the available balance/limit.
 *    The balance clause ("Avl Limit: INR 4,86,618.18", "AvlBal:Rs914.53") is stripped first so it
 *    can't be mistaken for the amount, and the FIRST currency amount is taken as the transaction.
 *  - Type is inferred from debit/credit keywords (a message with both Dr & Cr is a debit/transfer).
 *  - Merchant is the text after at/to/on/info:, skipping dates and pure numbers.
 *
 * Examples handled:
 *  "INR 1,820.00 spent using ICICI Bank Card XX6001 on 05-May-26 on AMAZON PAY IN U. Avl Limit: INR 4,86,618.18."
 *  "Alert: Your Acct XX1234 has been debited for Rs. 500.00 at AMAZON on 10-10-2023."
 *  "Dear Customer, your Acct XX5678 is credited with INR 10,000.00 on 11-10-23. Info: SALARY."
 */
class BankSmsParser : SmsParser {

    private val debitWords = listOf(
        "debited", "spent", "paid", "withdrawn", "purchase", "dr.", " dr ",
        "sent", "transferred", "deducted", "charged"
    )
    private val creditWords = listOf("credited", "received", "deposited", "refund", "cr.", " cr ")

    // "Avl Limit: INR X", "AvlBal:Rs X", "Available Balance Rs X", "Bal: Rs X" …
    private val balanceClause = Pattern.compile(
        "(?i)(?:avl|aval|avbl|avlbl|available|clr|clear)?\\.?\\s*(?:bal|balance|limit|lmt)\\b[^\\d]{0,12}(?:rs|inr)\\.?\\s*[\\d,]+(?:\\.\\d{1,2})?"
    )
    private val amount = Pattern.compile("(?i)(?:rs|inr)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    private val account = Pattern.compile(
        "(?i)(?:acct|card|a/c|account|ending|xx)\\s*(?:ending\\s*)?(?:with\\s*)?(?:no\\.?\\s*)?[xX*]{0,4}(\\d{4})\\b"
    )
    private val vpa = Pattern.compile("[A-Za-z0-9.\\-_]{2,}@[A-Za-z]{2,}")
    private val merchantPrefix = Pattern.compile(
        "(?i)\\b(?:at|to|on|info)\\b\\s*:?\\s*([A-Za-z0-9@.&'_\\- ]{2,40})"
    )
    private val connector = Regex("(?i)\\s+(?:on|using|via|ref|info|avl|avbl|bal|txn|dt|date|rrn|uti)\\b")
    private val dateLike = Pattern.compile(
        "(?i)\\b\\d{1,2}[-/](?:\\d{1,2}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)(?:[-/]\\d{2,4})?\\b"
    )

    override fun parse(sender: String, smsBody: String, timestamp: Long): ParsedSmsTransaction? {
        // Remove the balance/limit clause so it isn't mistaken for the transaction amount.
        val body = balanceClause.matcher(smsBody).replaceAll(" ")
        val lower = body.lowercase()

        val isDebit = debitWords.any { lower.contains(it) }
        val isCredit = creditWords.any { lower.contains(it) }
        if (!isDebit && !isCredit) return null // not a transaction (OTP, promo, etc.)

        // First currency amount = the transaction amount (balance already stripped).
        val amountMatcher = amount.matcher(body)
        if (!amountMatcher.find()) return null
        val parsedAmount = amountMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        if (parsedAmount <= 0.0) return null

        // A message with both Dr & Cr (e.g. transfer "Dr from A/C … Cr to merchant") is a debit.
        val type = if (isCredit && !isDebit) TransactionType.Income else TransactionType.Expense

        val accountMatcher = account.matcher(body)
        val accountLast4 = if (accountMatcher.find()) accountMatcher.group(1) ?: "0000" else "0000"

        return ParsedSmsTransaction(
            amount = parsedAmount,
            transactionType = type,
            merchantName = extractMerchant(body),
            accountLast4 = accountLast4,
            timestamp = timestamp,
            smsBody = smsBody
        )
    }

    private fun extractMerchant(body: String): String {
        // A UPI VPA handle is the strongest merchant signal.
        vpa.matcher(body).let { if (it.find()) return it.group() }

        // Otherwise take the best text after at/to/on/info:, skipping dates and numbers.
        val matcher = merchantPrefix.matcher(body)
        var best = ""
        while (matcher.find()) {
            var candidate = matcher.group(1)?.trim() ?: continue
            candidate = candidate.split(connector).first().trim() // cut "AMAZON on 10-10" -> "AMAZON"
            candidate = candidate.substringBefore(".").trim()
            if (candidate.length < 2) continue
            if (looksLikeDateOrNumber(candidate)) continue
            best = candidate // keep the last good one (merchant usually follows the date)
        }
        return best.ifBlank { "Unknown Merchant" }
    }

    private fun looksLikeDateOrNumber(s: String): Boolean {
        if (s.none { it.isLetter() }) return true
        return dateLike.matcher(s).find()
    }
}

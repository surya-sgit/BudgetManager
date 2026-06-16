package com.example.budgetmanager.data.sms

import android.util.Log
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.AccountType
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.model.PaymentMethod
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.CreditCardRepository
import com.example.budgetmanager.domain.repository.MerchantCategoryRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import com.example.budgetmanager.domain.usecase.LearnMerchantCategoryUseCase
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsProcessor @Inject constructor(
    private val smsParser: SmsParser,
    private val geminiSmsParser: GeminiSmsParser,
    private val geminiCategoryClassifier: GeminiCategoryClassifier,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantCategoryRepository: MerchantCategoryRepository,
    private val creditCardRepository: CreditCardRepository
) {
    /**
     * Returns true if a new transaction was recorded from this SMS.
     * [useAi] gates the cloud-model fallbacks (parsing + category guess); pass false for bulk
     * imports so a large inbox scan doesn't fire a burst of API calls.
     */
    suspend fun processSms(
        sender: String,
        smsBody: String,
        timestamp: Long,
        useAi: Boolean = true
    ): Boolean {
        Log.d("BudgetDebug", "processSms called. Sender: $sender, Body: $smsBody")

        // Drop marketing/spam ("Rs 100 wallet credits! 25% OFF, code: …, http://…") — these
        // mention amounts and "credited" but aren't real money movements.
        if (isPromotional(smsBody)) {
            Log.d("BudgetDebug", "Promotional/spam SMS detected. Skipping.")
            return false
        }

        // Drop bill reminders / payment-due notices ("bill of Rs X is due on …", "total amount
        // due", "please pay"). They mention an amount but no money has moved — only skip when the
        // SMS doesn't also describe a completed (debited/spent/credited) transaction.
        if (isPaymentReminder(smsBody) && !looksSettled(smsBody)) {
            Log.d("BudgetDebug", "Bill reminder / payment-due notice detected. Skipping.")
            return false
        }

        var parsedTransaction = smsParser.parse(sender, smsBody, timestamp)

        if (parsedTransaction == null && useAi) {
            Log.d("BudgetDebug", "Regex failed to parse. Falling back to Gemini AI.")
            parsedTransaction = geminiSmsParser.parseWithAi(smsBody, timestamp)
        }

        if (parsedTransaction == null) {
            Log.d("BudgetDebug", "Both Regex and AI failed to parse transaction. Aborting.")
            return false
        }

        Log.d("BudgetDebug", "Transaction parsed: $parsedTransaction")

        // Skip if we've already recorded this SMS (prevents duplicates on re-scan and on
        // live-receive-then-import). Keyed on the body alone: the live timestamp (SMSC time)
        // and the inbox timestamp (device-receipt time) can differ for the same message, so
        // including the timestamp would let duplicates slip through. Real bank SMS bodies are
        // unique (they carry a ref number / balance), so body-only keying is safe.
        val sourceHash = smsBody.trim().hashCode().toString()
        if (transactionRepository.existsBySourceHash(sourceHash)) {
            Log.d("BudgetDebug", "Duplicate SMS (hash already stored). Skipping.")
            return false
        }

        // Credit-card bill payments are recorded but flagged so they don't double-count as
        // spend (the individual card purchases were already counted). The card-side SMS says
        // "credited to your card", so force such payments to Expense — paying a bill is never income.
        val billPayment = isCardBillPayment(smsBody)
        val method = detectPaymentMethod(smsBody)
        val effectiveType =
            if (billPayment) TransactionType.Expense else parsedTransaction.transactionType

        // 1. Resolve account: match by last-4; if it's a real card/account number we haven't
        // seen, create a dedicated account so transactions group correctly instead of being
        // mis-assigned to whatever happens to be first.
        val account = resolveAccount(parsedTransaction.accountLast4)
        if (account == null) {
            Log.d("BudgetDebug", "Account resolution failed. Aborting.")
            return false
        }

        Log.d("BudgetDebug", "Linking transaction to account: ${account.name} (${account.accountLast4})")

        // 2. Find category (Classification logic)
        var categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
        if (categories.isEmpty()) {
            Log.d("BudgetDebug", "No categories exist. Seeding default category set.")
            defaultCategories().forEach { categoryRepository.insertCategory(it) }
            categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
        }

        // Income shouldn't be filed under a spending category — route it to "Income".
        val categoryId = if (effectiveType == TransactionType.Income) {
            incomeCategoryId(categories)
        } else {
            resolveExpenseCategory(parsedTransaction.merchantName, smsBody, categories, useAi)
        }
        Log.d("BudgetDebug", "Classified category ID: $categoryId")

        // 3. Create and save transaction
        val transaction = Transaction(
            id = 0,
            amount = parsedTransaction.amount,
            transactionType = effectiveType,
            categoryId = categoryId,
            merchantName = parsedTransaction.merchantName,
            accountId = account.id,
            timestamp = timestamp,
            smsBody = smsBody,
            sourceSmsHash = sourceHash,
            notes = "",
            userModified = false,
            paymentMethod = method,
            excludeFromBudget = billPayment
        )

        Log.d("BudgetDebug", "Saving transaction: $transaction")
        transactionRepository.insertTransaction(transaction)
        Log.d("BudgetDebug", "Transaction saved successfully.")

        // Auto-register the credit card (by last-4) so it appears under Cards for the user to
        // finish setting up. A bare "card" is treated as credit; debit-card spends are excluded.
        if (!billPayment && method == PaymentMethod.Card) {
            maybeRegisterCreditCard(parsedTransaction.accountLast4)
        }

        // Smart touch: when a salary credit lands, restart the budget cycle so the
        // daily limit recalculates from the new pay date.
        if (effectiveType == TransactionType.Income &&
            isLikelySalary(smsBody, parsedTransaction.merchantName)
        ) {
            Log.d("BudgetDebug", "Salary detected — resetting budget cycle start to now.")
            accountRepository.updateAccount(account.copy(cycleStartDate = timestamp))
        }
        return true
    }

    /**
     * Resolves the account for a parsed transaction. Matches an existing account by its last-4;
     * for an unseen real last-4 it creates a dedicated account; only falls back to the first
     * account when the SMS gave no usable account number.
     */
    private suspend fun resolveAccount(last4: String): Account? {
        val accounts = accountRepository.getAllAccounts().firstOrNull() ?: emptyList()
        accounts.find { it.accountLast4 == last4 }?.let { return it }

        val isRealLast4 = last4.length == 4 && last4.all { it.isDigit() }
        if (isRealLast4) {
            val newId = accountRepository.insertAccount(
                Account(
                    id = 0,
                    name = "Account ••$last4",
                    bankName = "Bank",
                    accountLast4 = last4,
                    accountType = AccountType.Savings
                )
            )
            return accountRepository.getAccountById(newId)
        }

        // Unknown last-4 (e.g. "AI"/"0000"): use an existing account, or seed a primary one.
        accounts.firstOrNull()?.let { return it }
        val newId = accountRepository.insertAccount(
            Account(
                id = 0,
                name = "Primary Account",
                bankName = "Bank",
                accountLast4 = last4,
                accountType = AccountType.Savings
            )
        )
        return accountRepository.getAccountById(newId)
    }

    /**
     * Creates a stub credit card for an unseen last-4 so it shows under Cards for the user to
     * complete. Called only for credit-card spends (the caller checks the payment method).
     */
    private suspend fun maybeRegisterCreditCard(last4: String) {
        val isRealLast4 = last4.length == 4 && last4.all { it.isDigit() }
        if (!isRealLast4) return

        val existing = creditCardRepository.getAllCreditCards().firstOrNull() ?: emptyList()
        if (existing.any { it.lastFourDigits == last4 }) return

        Log.d("BudgetDebug", "Auto-registering credit card ending $last4.")
        creditCardRepository.insertCreditCard(
            CreditCard(
                id = 0,
                cardName = "Card ••$last4",
                creditLimit = 0.0,
                availableLimit = 0.0,
                currentSpend = 0.0,
                statementBalance = 0.0,
                billingDate = 1,
                dueDate = 1,
                lastFourDigits = last4,
                needsSetup = true
            )
        )
    }

    /** Finds the shared "Income" category, creating it on first use. */
    private suspend fun incomeCategoryId(categories: List<Category>): Long {
        categories.find { it.name.equals("Income", ignoreCase = true) }?.let { return it.id }
        return categoryRepository.insertCategory(
            Category(id = 0, name = "Income", icon = "payments", color = "#1B873E")
        )
    }

    private fun isLikelySalary(smsBody: String, merchant: String): Boolean {
        val haystack = "$smsBody $merchant".lowercase()
        return haystack.containsAny("salary", "payroll", "wages", "stipend", "sal cr", "sal credit")
    }

    /** Detects how the money moved so the UI can show "UPI", "Card", etc. */
    private fun detectPaymentMethod(smsBody: String): PaymentMethod {
        val s = smsBody.lowercase()
        // A VPA handle like name@okhdfc / name@ybl is a strong UPI signal.
        val hasVpa = Regex("[a-z0-9.\\-_]{2,}@[a-z]{2,}").containsMatchIn(s)
        return when {
            s.containsAny("upi", "vpa") || hasVpa -> PaymentMethod.Upi
            s.contains("debit card") -> PaymentMethod.DebitCard
            // A bare "card" is treated as a credit card (true almost always); the user can
            // reclassify to Debit Card on the transaction if needed.
            s.containsAny("credit card", "bank card", "card ending", "card no", "pos ", "atm", "card xx") -> PaymentMethod.Card
            s.containsAny("neft", "imps", "rtgs", "net banking", "netbanking") -> PaymentMethod.NetBanking
            s.containsAny("wallet", "paytm balance") -> PaymentMethod.Wallet
            else -> PaymentMethod.Unknown
        }
    }

    /**
     * True when the SMS is a payment *towards* a credit card bill (a transfer between your own
     * accounts, not a new expense). Matches only unambiguous card-bill phrases so a normal card
     * spend — e.g. "credit card payment of Rs 500 at AMAZON" — is never mistaken for one.
     */
    /**
     * Detects marketing/spam SMS that merely mention amounts and words like "credited". A
     * clickable link combined with any promo word, or two-plus promo words, is treated as spam.
     * Genuine debit/credit alerts have neither, so they pass through.
     */
    private fun isPromotional(smsBody: String): Boolean {
        val s = smsBody.lowercase()
        val hasLink = s.contains("http://") || s.contains("https://") || s.contains("www.") ||
            Regex("[a-z0-9-]+\\.(in|com|co|ly|me|org|net|app)/").containsMatchIn(s)
        val promoWords = listOf(
            "% off", "offer", "discount", "coupon", "code:", "use code", "promo code",
            "cashback", "sale", "buy now", "order now", "shop now", "deal", "voucher",
            "reward points", "wallet credits", "congratulations", "you have won", "you won",
            "lowest price", "emi offer", "pre-approved", "apply now", "click here", "flat ",
            "limited time", "hurry", "unsubscribe", "t&c apply"
        )
        val promoHits = promoWords.count { s.contains(it) }
        return (hasLink && promoHits >= 1) || promoHits >= 2
    }

    /** True when the SMS is a bill reminder / amount-due notice rather than a completed payment. */
    private fun isPaymentReminder(smsBody: String): Boolean {
        val s = smsBody.lowercase()
        return s.containsAny(
            "is due", "due on", "due date", "amount due", "total due", "total amount due",
            "min due", "minimum amount due", "minimum due", "min amt due", "amt due",
            "payment due", "outstanding", "pay by", "please pay", "kindly pay", "pay now",
            "to avoid late", "late fee", "late payment", "bill generated", "statement generated",
            "bill is ready", "e-statement", "will be auto-debited", "will be debited on",
            "due immediately", "overdue", "make payment"
        )
    }

    /** True when the SMS describes a completed (already-happened) debit/credit transaction. */
    private fun looksSettled(smsBody: String): Boolean {
        // Drop future-tense phrases so "will be debited" isn't mistaken for a completed debit.
        val s = smsBody.lowercase()
            .replace("will be debited", " ")
            .replace("will be auto-debited", " ")
            .replace("will be deducted", " ")
            .replace("to be debited", " ")
        return s.containsAny(
            "debited", "spent", "credited", "received", "withdrawn", "deposited",
            "purchase", "dr.", " dr ", "cr.", " cr "
        )
    }

    private fun isCardBillPayment(smsBody: String): Boolean {
        val s = smsBody.lowercase()
        return s.containsAny(
            "towards your credit card",
            "towards your card",
            "towards card ending",
            "received towards your card",
            "payment received towards",
            "received for your credit card",
            "payment for your credit card",
            "credited to your card",
            "paid to your credit card",
            "credit card bill",
            "card bill payment",
            "payment towards your card",
            "cc bill payment"
        )
    }

    /** Expense category resolution: a learned merchant rule wins; otherwise keyword matching. */
    private suspend fun resolveExpenseCategory(
        merchant: String,
        smsBody: String,
        categories: List<Category>,
        useAi: Boolean
    ): Long {
        val key = LearnMerchantCategoryUseCase.normalizeMerchant(merchant)
        val learned = merchantCategoryRepository.getCategoryIdForMerchant(key)
        if (learned != null && categories.any { it.id == learned }) {
            Log.d("BudgetDebug", "Using learned category $learned for '$merchant'.")
            return learned
        }

        // Rule-based classification first (instant, offline).
        val ruleName = classifyCategoryName(merchant, smsBody)
        if (!ruleName.equals("Other", ignoreCase = true)) {
            return ensureCategoryByName(ruleName, categories)
        }

        // During bulk import we don't call the cloud model — leave it as Other for now (a later
        // live SMS, or a manual reclassify, will categorize and remember the merchant).
        if (!useAi) return ensureCategoryByName("Other", categories)

        // Rules couldn't place it → ask the cloud model ONCE, then remember the answer so future
        // SMS from this merchant are classified instantly without another API call.
        val candidates = (categories.map { it.name } + defaultCategories().map { it.name })
            .distinct()
            .filter { !it.equals("Income", ignoreCase = true) }
        val aiName = geminiCategoryClassifier.classify(merchant, smsBody, candidates)
        if (aiName != null && !aiName.equals("Other", ignoreCase = true)) {
            val categoryId = ensureCategoryByName(aiName, categories)
            if (key.isNotBlank() && categoryId > 0) {
                merchantCategoryRepository.saveRule(key, categoryId)
                Log.d("BudgetDebug", "Learned '$merchant' -> $aiName via AI; future SMS won't call AI.")
            }
            return categoryId
        }

        return ensureCategoryByName("Other", categories)
    }

    /** Finds the category by name, creating it (e.g. "Groceries" on older DBs) if absent. */
    private suspend fun ensureCategoryByName(name: String, categories: List<Category>): Long {
        categories.find { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        val (icon, color) = defaultCategories().firstOrNull { it.name == name }
            ?.let { it.icon to it.color } ?: ("category" to "#9E9E9E")
        return categoryRepository.insertCategory(Category(0, name, icon, color))
            .takeIf { it > 0 }
            ?: categories.find { it.name == "Other" }?.id
            ?: 0L
    }

    // Classify using the merchant name AND the full SMS — the brand keyword (SWIGGY, AMAZON…)
    // is reliably present in the body even when merchant extraction grabs something messy.
    // Uses whole-word matching so e.g. "lab" doesn't match "available" and "lic" not "click".
    private fun classifyCategoryName(merchantName: String, smsBody: String): String {
        val m = "$merchantName $smsBody".lowercase()
        return when {
            // Quick-commerce / groceries — checked first because names overlap with Food/Shopping
            m.hasWord("instamart", "blinkit", "zepto", "bigbasket", "grofers", "dunzo",
                "jiomart", "dmart", "d-mart", "reliance fresh", "more retail", "spencer", "kirana",
                "grocery", "groceries", "supermart", "supermarket", "country delight") -> "Groceries"
            m.hasWord("swiggy", "zomato", "restaurant", "cafe", "coffee", "food", "pizza",
                "burger", "biryani", "kitchen", "dhaba", "mcdonald", "kfc", "dominos", "domino's",
                "starbucks", "eatery", "bakery", "hotel") -> "Food"
            m.hasWord("uber", "ola", "rapido", "irctc", "train", "flight", "indigo",
                "spicejet", "airindia", "makemytrip", "goibibo", "redbus", "airways", "airline",
                "fuel", "petrol", "fastag") -> "Travel"
            m.hasWord("amazon", "flipkart", "myntra", "meesho", "nykaa", "ajio", "snapdeal",
                "tatacliq", "lifestyle", "pantaloons", "westside", "decathlon", "reliance trends") -> "Shopping"
            m.hasWord("netflix", "spotify", "hotstar", "prime video", "zee5", "sonyliv",
                "youtube", "subscription", "bookmyshow", "pvr", "inox") -> "Entertainment"
            m.hasWord("hospital", "clinic", "pharmacy", "doctor", "apollo", "medplus",
                "health", "medical", "diagnostic", "netmeds", "1mg", "pharmeasy") -> "Healthcare"
            m.hasWord("electricity", "water bill", "gas bill", "recharge", "broadband", "wifi",
                "airtel", "jio", "bsnl", "vodafone", "utility", "insurance", "lic",
                "postpaid", "prepaid") -> "Utilities"
            else -> "Other"
        }
    }

    /** Whole-word keyword match — avoids substring false positives (lab/available, lic/click). */
    private fun String.hasWord(vararg words: String): Boolean {
        return words.any { w ->
            Regex("\\b" + Regex.escape(w.trim()) + "\\b").containsMatchIn(this)
        }
    }

    private fun defaultCategories(): List<Category> = listOf(
        Category(0, "Food", "restaurant", "#FF5722"),
        Category(0, "Groceries", "shopping_basket", "#4CAF50"),
        Category(0, "Travel", "directions_bus", "#2196F3"),
        Category(0, "Shopping", "shopping_cart", "#E91E63"),
        Category(0, "Entertainment", "movie", "#9C27B0"),
        Category(0, "Healthcare", "health_and_safety", "#F44336"),
        Category(0, "Utilities", "bolt", "#FF9800"),
        Category(0, "Income", "payments", "#1B873E"),
        Category(0, "Other", "category", "#9E9E9E")
    )

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }
}

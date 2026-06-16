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
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantCategoryRepository: MerchantCategoryRepository,
    private val creditCardRepository: CreditCardRepository
) {
    /** Returns true if a new transaction was recorded from this SMS. */
    suspend fun processSms(sender: String, smsBody: String, timestamp: Long): Boolean {
        Log.d("BudgetDebug", "processSms called. Sender: $sender, Body: $smsBody")

        // Drop marketing/spam ("Rs 100 wallet credits! 25% OFF, code: …, http://…") — these
        // mention amounts and "credited" but aren't real money movements.
        if (isPromotional(smsBody)) {
            Log.d("BudgetDebug", "Promotional/spam SMS detected. Skipping.")
            return false
        }

        var parsedTransaction = smsParser.parse(sender, smsBody, timestamp)

        if (parsedTransaction == null) {
            Log.d("BudgetDebug", "Regex failed to parse. Falling back to Gemini AI.")
            // Fallback to AI
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
        // spend (the individual card purchases were already counted).
        val billPayment = isCardBillPayment(smsBody)

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
        val categoryId = if (parsedTransaction.transactionType == TransactionType.Income) {
            incomeCategoryId(categories)
        } else {
            resolveExpenseCategory(parsedTransaction.merchantName, categories)
        }
        Log.d("BudgetDebug", "Classified category ID: $categoryId")

        // 3. Create and save transaction
        val transaction = Transaction(
            id = 0,
            amount = parsedTransaction.amount,
            transactionType = parsedTransaction.transactionType,
            categoryId = categoryId,
            merchantName = parsedTransaction.merchantName,
            accountId = account.id,
            timestamp = timestamp,
            smsBody = smsBody,
            sourceSmsHash = sourceHash,
            notes = "",
            userModified = false,
            paymentMethod = detectPaymentMethod(smsBody),
            excludeFromBudget = billPayment
        )

        Log.d("BudgetDebug", "Saving transaction: $transaction")
        transactionRepository.insertTransaction(transaction)
        Log.d("BudgetDebug", "Transaction saved successfully.")

        // Auto-register the credit card (by last-4) so it appears under Cards for the user
        // to finish setting up — but not for a bill-payment SMS (that's not a card spend).
        if (!billPayment) {
            maybeRegisterCreditCard(parsedTransaction.accountLast4, smsBody)
        }

        // Smart touch: when a salary credit lands, restart the budget cycle so the
        // daily limit recalculates from the new pay date.
        if (parsedTransaction.transactionType == TransactionType.Income &&
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
     * Creates a stub credit card for an unseen last-4 when the SMS clearly references a credit
     * card, so it shows under Cards for the user to complete. Debit-card spends are ignored.
     */
    private suspend fun maybeRegisterCreditCard(last4: String, smsBody: String) {
        val isRealLast4 = last4.length == 4 && last4.all { it.isDigit() }
        if (!isRealLast4) return
        val s = smsBody.lowercase()
        val isCreditCard = (s.contains("credit card") || s.contains("creditcard")) && !s.contains("debit card")
        if (!isCreditCard) return

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
            s.containsAny("credit card", "debit card", "card ending", "card no", "pos ", "atm", "card xx") -> PaymentMethod.Card
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
    private suspend fun resolveExpenseCategory(merchant: String, categories: List<Category>): Long {
        val key = LearnMerchantCategoryUseCase.normalizeMerchant(merchant)
        val learned = merchantCategoryRepository.getCategoryIdForMerchant(key)
        if (learned != null && categories.any { it.id == learned }) {
            Log.d("BudgetDebug", "Using learned category $learned for '$merchant'.")
            return learned
        }
        return ensureCategoryByName(classifyCategoryName(merchant), categories)
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

    private fun classifyCategoryName(merchantName: String): String {
        val m = merchantName.lowercase()
        return when {
            // Quick-commerce / groceries — checked first because names overlap with Food/Shopping
            m.containsAny("instamart", "blinkit", "zepto", "bigbasket", "grofers", "dunzo",
                "jiomart", "dmart", "d-mart", "reliance fresh", "more retail", "spencer", "kirana",
                "grocery", "groceries", "supermart", "supermarket", "country delight", "minutes") -> "Groceries"
            m.containsAny("swiggy", "zomato", "restaurant", "cafe", "coffee", "food", "pizza",
                "burger", "biryani", "kitchen", "dhaba", "mcdonald", "kfc", "dominos", "starbucks",
                "eatery", "bakery") -> "Food"
            m.containsAny("uber", "ola", "rapido", "irctc", "train", "flight", "indigo",
                "spicejet", "airindia", "makemytrip", "goibibo", "redbus", "airways", "airline",
                "metro", "fuel", "petrol", "fastag", "toll") -> "Travel"
            m.containsAny("amazon", "flipkart", "myntra", "meesho", "nykaa", "ajio", "snapdeal",
                "tatacliq", "mall", "retail", "lifestyle", "pantaloons", "westside", "decathlon") -> "Shopping"
            m.containsAny("netflix", "spotify", "hotstar", "prime video", "zee5", "sonyliv",
                "youtube", "subscription", "streaming", "bookmyshow", "pvr", "inox") -> "Entertainment"
            m.containsAny("hospital", "clinic", "pharmacy", "doctor", "apollo", "medplus",
                "health", "medical", "diagnostic", "lab", "netmeds", "1mg", "pharmeasy") -> "Healthcare"
            m.containsAny("electricity", "water bill", "gas", "recharge", "broadband", "wifi",
                "airtel", "jio", "bsnl", "vodafone", "idea", "utility", "insurance", "lic",
                "postpaid", "prepaid") -> "Utilities"
            else -> "Other"
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

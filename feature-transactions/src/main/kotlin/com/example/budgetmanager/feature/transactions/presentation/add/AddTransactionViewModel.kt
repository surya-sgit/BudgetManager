package com.example.budgetmanager.feature.transactions.presentation.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.AccountType
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.model.PaymentMethod
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        seedCategoriesIfEmpty()
    }

    fun save(
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        merchantName: String,
        notes: String,
        timestamp: Long,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            val accountId = ensureDefaultAccount()
            val resolvedCategoryId = categoryId.takeIf { it > 0 }
                ?: categoryRepository.getAllCategories().first().firstOrNull()?.id
                ?: return@launch
            addTransactionUseCase(
                Transaction(
                    id = 0L,
                    amount = amount,
                    transactionType = type,
                    categoryId = resolvedCategoryId,
                    merchantName = merchantName.ifBlank { "Manual entry" },
                    accountId = accountId,
                    timestamp = timestamp,
                    smsBody = "",
                    sourceSmsHash = "manual_${timestamp}_${System.nanoTime()}",
                    notes = notes,
                    userModified = true,
                    paymentMethod = paymentMethod
                )
            )
            _saved.value = true
        }
    }

    private suspend fun ensureDefaultAccount(): Long {
        val accounts = accountRepository.getAllAccounts().first()
        val nonCredit = accounts.firstOrNull { it.accountType != AccountType.CreditCard }
        if (nonCredit != null) return nonCredit.id
        // No usable account yet — create a Cash wallet so manual entries have a valid FK target.
        return accountRepository.insertAccount(
            Account(
                id = 0L,
                name = "Cash",
                bankName = "Cash",
                accountLast4 = "0000",
                accountType = AccountType.Wallet
            )
        )
    }

    private fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            val current = categoryRepository.getAllCategories().first()
            if (current.isEmpty()) {
                listOf(
                    Category(0, "Food", "restaurant", "#FF5722"),
                    Category(0, "Groceries", "shopping_basket", "#4CAF50"),
                    Category(0, "Travel", "directions_bus", "#2196F3"),
                    Category(0, "Shopping", "shopping_cart", "#E91E63"),
                    Category(0, "Entertainment", "movie", "#9C27B0"),
                    Category(0, "Healthcare", "health_and_safety", "#F44336"),
                    Category(0, "Utilities", "bolt", "#FF9800"),
                    Category(0, "Other", "category", "#9E9E9E")
                ).forEach { categoryRepository.insertCategory(it) }
            }
            // Ensure the Income category exists regardless of who seeded the rest.
            val afterSeed = categoryRepository.getAllCategories().first()
            if (afterSeed.none { it.name.equals("Income", ignoreCase = true) }) {
                categoryRepository.insertCategory(Category(0, "Income", "payments", "#1B873E"))
            }
        }
    }
}

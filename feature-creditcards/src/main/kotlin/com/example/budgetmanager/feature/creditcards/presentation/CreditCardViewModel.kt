package com.example.budgetmanager.feature.creditcards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Account
import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import com.example.budgetmanager.domain.repository.AccountRepository
import com.example.budgetmanager.domain.repository.CreditCardRepository
import com.example.budgetmanager.domain.repository.TransactionRepository
import com.example.budgetmanager.domain.usecase.AddCreditCardUseCase
import com.example.budgetmanager.domain.usecase.GetCreditCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val getCreditCardsUseCase: GetCreditCardsUseCase,
    private val addCreditCardUseCase: AddCreditCardUseCase,
    private val creditCardRepository: CreditCardRepository,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    val creditCards: StateFlow<List<CreditCard>> = getCreditCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Live spend for the current statement cycle, keyed by card id. */
    val cycleSpend: StateFlow<Map<Long, Double>> = combine(
        getCreditCardsUseCase(),
        transactionRepository.getAllTransactions(),
        accountRepository.getAllAccounts()
    ) { cards, transactions, accounts ->
        cards.associate { card -> card.id to computeCycleSpend(card, transactions, accounts) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun computeCycleSpend(
        card: CreditCard,
        transactions: List<Transaction>,
        accounts: List<Account>
    ): Double {
        // Manual cards with no linked last-4 keep their entered spend.
        if (card.lastFourDigits.isBlank()) return card.currentSpend
        val accountIds = accounts
            .filter { it.accountLast4 == card.lastFourDigits }
            .map { it.id }
            .toSet()
        if (accountIds.isEmpty()) return card.currentSpend

        val cycleStart = cycleStartMillis(card.billingDate)
        return transactions
            .filter {
                it.transactionType == TransactionType.Expense &&
                    !it.excludeFromBudget &&
                    it.paymentMethod.isCreditCard &&
                    it.accountId in accountIds &&
                    it.timestamp >= cycleStart
            }
            .sumOf { it.amount }
    }

    /** Start of the current statement cycle = most recent occurrence of the billing day. */
    private fun cycleStartMillis(billingDate: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.get(Calendar.DAY_OF_MONTH) < billingDate) {
            cal.add(Calendar.MONTH, -1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, billingDate.coerceIn(1, maxDay))
        return cal.timeInMillis
    }

    fun addCreditCard(creditCard: CreditCard) {
        viewModelScope.launch { addCreditCardUseCase(creditCard) }
    }

    fun updateCreditCard(creditCard: CreditCard) {
        viewModelScope.launch { creditCardRepository.updateCreditCard(creditCard) }
    }

    fun deleteCreditCard(creditCard: CreditCard) {
        viewModelScope.launch { creditCardRepository.deleteCreditCard(creditCard) }
    }

    /** Merges an auto-created stub into an existing card: moves the last-4 over, deletes the stub. */
    fun mergeInto(source: CreditCard, target: CreditCard) {
        viewModelScope.launch {
            creditCardRepository.updateCreditCard(
                target.copy(lastFourDigits = source.lastFourDigits.ifBlank { target.lastFourDigits })
            )
            creditCardRepository.deleteCreditCard(source)
        }
    }
}

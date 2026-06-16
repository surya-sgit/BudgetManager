package com.example.budgetmanager.feature.transactions.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.model.PaymentMethod
import com.example.budgetmanager.domain.model.ExpenseSplit
import com.example.budgetmanager.domain.model.SplitParticipant
import com.example.budgetmanager.domain.model.SplitStatus
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.repository.ExpenseSplitRepository
import com.example.budgetmanager.domain.usecase.DeleteTransactionUseCase
import com.example.budgetmanager.domain.usecase.GetTransactionByIdUseCase
import com.example.budgetmanager.domain.usecase.LearnMerchantCategoryUseCase
import com.example.budgetmanager.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val learnMerchantCategoryUseCase: LearnMerchantCategoryUseCase,
    private val expenseSplitRepository: ExpenseSplitRepository,
    categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = checkNotNull(savedStateHandle["transactionId"])

    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction = _transaction.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val split: StateFlow<Pair<ExpenseSplit, List<SplitParticipant>>?> =
        expenseSplitRepository.getSplitForTransaction(transactionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadTransaction()
    }

    private fun loadTransaction() {
        viewModelScope.launch {
            _transaction.value = getTransactionByIdUseCase(transactionId)
        }
    }

    /** Splits this transaction equally among [participantNames]. The first name is "you" (already paid). */
    fun createEqualSplit(participantNames: List<String>) {
        val tx = _transaction.value ?: return
        if (participantNames.size < 2) return
        viewModelScope.launch {
            val share = tx.amount / participantNames.size
            val newSplit = ExpenseSplit(
                id = 0L,
                transactionId = transactionId,
                totalParticipants = participantNames.size,
                amountPerPerson = share,
                description = tx.merchantName
            )
            val participants = participantNames.mapIndexed { index, rawName ->
                SplitParticipant(
                    id = 0L,
                    splitId = 0L,
                    name = rawName.ifBlank { "Person ${index + 1}" },
                    shareAmount = share,
                    status = if (index == 0) SplitStatus.Paid else SplitStatus.Pending
                )
            }
            expenseSplitRepository.createSplit(newSplit, participants)

            // Record the user's share on the transaction note so it's visible everywhere.
            val shareNote = "Your share: ₹${"%.2f".format(share)} of ₹${"%.2f".format(tx.amount)}"
            val updated = tx.copy(
                notes = if (tx.notes.isBlank()) shareNote else "${tx.notes} · $shareNote",
                userModified = true
            )
            updateTransactionUseCase(updated)
            _transaction.value = updated
        }
    }

    /** Changes this transaction's category and remembers the choice for this merchant. */
    fun reclassify(categoryId: Long) {
        val tx = _transaction.value ?: return
        if (tx.categoryId == categoryId) return
        viewModelScope.launch {
            val updated = tx.copy(categoryId = categoryId, userModified = true)
            updateTransactionUseCase(updated)
            _transaction.value = updated
            // Train future SMS from the same merchant to use this category.
            learnMerchantCategoryUseCase(tx.merchantName, categoryId)
        }
    }

    /** Changes how the transaction was paid (Card, UPI, Cash…). */
    fun setPaymentMethod(method: PaymentMethod) {
        val tx = _transaction.value ?: return
        if (tx.paymentMethod == method) return
        viewModelScope.launch {
            val updated = tx.copy(paymentMethod = method, userModified = true)
            updateTransactionUseCase(updated)
            _transaction.value = updated
        }
    }

    /** Toggles whether this transaction counts toward spending (e.g. mark a card-bill payment). */
    fun setExcludeFromBudget(exclude: Boolean) {
        val tx = _transaction.value ?: return
        if (tx.excludeFromBudget == exclude) return
        viewModelScope.launch {
            val updated = tx.copy(excludeFromBudget = exclude, userModified = true)
            updateTransactionUseCase(updated)
            _transaction.value = updated
        }
    }

    fun delete() {
        val tx = _transaction.value ?: return
        viewModelScope.launch {
            deleteTransactionUseCase(tx)
            _deleted.value = true
        }
    }

    fun toggleParticipantPaid(participant: SplitParticipant) {
        val next = if (participant.status == SplitStatus.Paid) SplitStatus.Pending else SplitStatus.Paid
        viewModelScope.launch {
            expenseSplitRepository.updateParticipantStatus(participant.id, next)
        }
    }
}

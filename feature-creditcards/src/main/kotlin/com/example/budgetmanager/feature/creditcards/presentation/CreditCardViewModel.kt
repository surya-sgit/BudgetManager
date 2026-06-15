package com.example.budgetmanager.feature.creditcards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.usecase.AddCreditCardUseCase
import com.example.budgetmanager.domain.usecase.GetCreditCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val getCreditCardsUseCase: GetCreditCardsUseCase,
    private val addCreditCardUseCase: AddCreditCardUseCase
) : ViewModel() {

    val creditCards: StateFlow<List<CreditCard>> = getCreditCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCreditCard(creditCard: CreditCard) {
        viewModelScope.launch {
            addCreditCardUseCase(creditCard)
        }
    }
}

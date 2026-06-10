package com.example.budgetmanager.feature.creditcards.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.CreditCard
import com.example.budgetmanager.domain.usecase.GetCreditCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val getCreditCardsUseCase: GetCreditCardsUseCase
) : ViewModel() {

    val creditCards: StateFlow<List<CreditCard>> = getCreditCardsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

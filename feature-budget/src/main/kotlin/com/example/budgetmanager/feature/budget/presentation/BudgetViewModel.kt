package com.example.budgetmanager.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.usecase.BudgetProgress
import com.example.budgetmanager.domain.usecase.GetBudgetWithProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getBudgetWithProgressUseCase: GetBudgetWithProgressUseCase
) : ViewModel() {

    val budgetProgress: StateFlow<List<BudgetProgress>> = getBudgetWithProgressUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

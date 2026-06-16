package com.example.budgetmanager.feature.dashboard.presentation.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.SavingsGoal
import com.example.budgetmanager.domain.usecase.AddSavingsGoalUseCase
import com.example.budgetmanager.domain.usecase.DeleteSavingsGoalUseCase
import com.example.budgetmanager.domain.usecase.GetSavingsGoalsUseCase
import com.example.budgetmanager.domain.usecase.UpdateSavingsGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    getSavingsGoalsUseCase: GetSavingsGoalsUseCase,
    private val addSavingsGoalUseCase: AddSavingsGoalUseCase,
    private val updateSavingsGoalUseCase: UpdateSavingsGoalUseCase,
    private val deleteSavingsGoalUseCase: DeleteSavingsGoalUseCase
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = getSavingsGoalsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(name: String, targetAmount: Double, initialSaved: Double, targetDate: Long?) {
        viewModelScope.launch {
            addSavingsGoalUseCase(
                SavingsGoal(
                    id = 0L,
                    name = name,
                    targetAmount = targetAmount,
                    savedAmount = initialSaved,
                    targetDate = targetDate
                )
            )
        }
    }

    fun contribute(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch { updateSavingsGoalUseCase.contribute(goal, amount) }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch { deleteSavingsGoalUseCase(goal) }
    }
}

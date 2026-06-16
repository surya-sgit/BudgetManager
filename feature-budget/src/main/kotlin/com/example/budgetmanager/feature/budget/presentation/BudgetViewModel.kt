package com.example.budgetmanager.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.usecase.AddBudgetUseCase
import com.example.budgetmanager.domain.usecase.BudgetProgress
import com.example.budgetmanager.domain.usecase.GetBudgetWithProgressUseCase
import com.example.budgetmanager.domain.usecase.ResetBudgetCycleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getBudgetWithProgressUseCase: GetBudgetWithProgressUseCase,
    private val addBudgetUseCase: AddBudgetUseCase,
    private val resetBudgetCycleUseCase: ResetBudgetCycleUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val budgetProgress: StateFlow<List<BudgetProgress>> = getBudgetWithProgressUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedCategoriesIfEmpty()
    }

    private fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            val current = categoryRepository.getAllCategories().first()
            if (current.isEmpty()) {
                listOf(
                    Category(id = 0, name = "Food", icon = "restaurant", color = "#FF5722"),
                    Category(id = 0, name = "Groceries", icon = "shopping_basket", color = "#4CAF50"),
                    Category(id = 0, name = "Travel", icon = "directions_bus", color = "#2196F3"),
                    Category(id = 0, name = "Shopping", icon = "shopping_cart", color = "#E91E63"),
                    Category(id = 0, name = "Entertainment", icon = "movie", color = "#9C27B0"),
                    Category(id = 0, name = "Healthcare", icon = "health_and_safety", color = "#F44336"),
                    Category(id = 0, name = "Utilities", icon = "bolt", color = "#FF9800"),
                    Category(id = 0, name = "Other", icon = "category", color = "#9E9E9E")
                ).forEach { categoryRepository.insertCategory(it) }
            }
        }
    }

    fun addBudget(categoryId: Long, limit: Double) {
        viewModelScope.launch {
            addBudgetUseCase(Budget(id = 0, categoryId = categoryId, monthlyLimit = limit))
        }
    }

    fun logSalaryAndResetCycle() {
        viewModelScope.launch {
            resetBudgetCycleUseCase()
        }
    }
}

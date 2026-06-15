package com.example.budgetmanager.feature.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetmanager.domain.model.Budget
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.repository.CategoryRepository
import com.example.budgetmanager.domain.usecase.AddBudgetUseCase
import com.example.budgetmanager.domain.usecase.BudgetProgress
import com.example.budgetmanager.domain.usecase.GetBudgetWithProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val getBudgetWithProgressUseCase: GetBudgetWithProgressUseCase,
    private val addBudgetUseCase: AddBudgetUseCase,
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
            categories.collect {
                if (it.isEmpty()) {
                    // This is a simple way to trigger seeding. 
                    // In a production app, this should be handled more robustly in the data layer.
                    val defaultCategories = listOf(
                        Category(id = 0, name = "Food", icon = "restaurant", color = "#FF5722"),
                        Category(id = 0, name = "Travel", icon = "directions_bus", color = "#2196F3"),
                        Category(id = 0, name = "Shopping", icon = "shopping_cart", color = "#E91E63"),
                        Category(id = 0, name = "Other", icon = "category", color = "#9E9E9E")
                    )
                    defaultCategories.forEach { category ->
                        categoryRepository.insertCategory(category)
                    }
                }
            }
        }
    }

    fun addBudget(categoryId: Long, limit: Double) {
        viewModelScope.launch {
            addBudgetUseCase(Budget(id = 0, categoryId = categoryId, monthlyLimit = limit))
        }
    }
}

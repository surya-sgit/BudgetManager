package com.example.budgetmanager.core.database.di

import com.example.budgetmanager.core.database.dao.CategoryDao
import com.example.budgetmanager.core.database.entities.CategoryEntity
import javax.inject.Inject

class DatabaseInitializer @Inject constructor(
    private val categoryDao: CategoryDao
) {
    suspend fun seedDefaultCategories() {
        val defaultCategories = listOf(
            CategoryEntity(name = "Food", icon = "restaurant", color = "#FF5722"),
            CategoryEntity(name = "Travel", icon = "directions_bus", color = "#2196F3"),
            CategoryEntity(name = "Shopping", icon = "shopping_cart", color = "#E91E63"),
            CategoryEntity(name = "Bills", icon = "receipt", color = "#FFC107"),
            CategoryEntity(name = "Fuel", icon = "local_gas_station", color = "#795548"),
            CategoryEntity(name = "Health", icon = "medical_services", color = "#4CAF50"),
            CategoryEntity(name = "Salary", icon = "payments", color = "#00BCD4"),
            CategoryEntity(name = "Entertainment", icon = "movie", color = "#9C27B0"),
            CategoryEntity(name = "Investment", icon = "trending_up", color = "#3F51B5"),
            CategoryEntity(name = "Transfer", icon = "compare_arrows", color = "#607D8B"),
            CategoryEntity(name = "Other", icon = "category", color = "#9E9E9E")
        )
        
        // In a real app, check if already seeded
        defaultCategories.forEach {
            categoryDao.insertCategory(it)
        }
    }
}

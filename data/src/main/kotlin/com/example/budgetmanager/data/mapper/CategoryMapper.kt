package com.example.budgetmanager.data.mapper

import com.example.budgetmanager.core.database.entities.CategoryEntity
import com.example.budgetmanager.domain.model.Category

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        icon = icon,
        color = color
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        color = color
    )
}

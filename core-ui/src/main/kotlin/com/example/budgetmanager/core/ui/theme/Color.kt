package com.example.budgetmanager.core.ui.theme

import androidx.compose.ui.graphics.Color

// Blue palette — primary (trust, stability)
val Blue10 = Color(0xFF001D36)
val Blue20 = Color(0xFF003258)
val Blue40 = Color(0xFF0061A4)
val Blue80 = Color(0xFF9ECAFF)
val Blue90 = Color(0xFFD1E4FF)

// Teal palette — secondary (calm, growth)
val Teal30 = Color(0xFF00687A)
val Teal40 = Color(0xFF006780)
val Teal80 = Color(0xFF4FD8EB)
val Teal90 = Color(0xFFB2EBEF)

// Neutral palette
val Neutral10 = Color(0xFF1A1C1E)
val Neutral20 = Color(0xFF2F3033)
val Neutral90 = Color(0xFFE2E2E9)
val Neutral95 = Color(0xFFF0F0F7)
val Neutral99 = Color(0xFFFCFCFF)

// Semantic tokens — import these in any module that needs income/expense colors
val IncomeGreen = Color(0xFF1B873E)
val IncomeGreenContainer = Color(0xFFBDF5D0)
val IncomeGreenOnContainer = Color(0xFF1B6E3D)
val ExpenseRed = Color(0xFFC62828)
val ExpenseRedContainer = Color(0xFFFFDAD6)

// Chart palette — used for category breakdown / trends. Cycle through by index.
val ChartPalette = listOf(
    Color(0xFF0061A4), // blue
    Color(0xFF00897B), // teal
    Color(0xFFEF6C00), // orange
    Color(0xFF8E24AA), // purple
    Color(0xFFC62828), // red
    Color(0xFF2E7D32), // green
    Color(0xFF5C6BC0), // indigo
    Color(0xFFF9A825), // amber
    Color(0xFF00838F), // cyan
    Color(0xFF6D4C41)  // brown
)

package com.example.budgetmanager.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal30,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    background = Neutral95,
    onBackground = Neutral10,
    error = ExpenseRed,
    errorContainer = ExpenseRedContainer,
    onError = Color.White,
    onErrorContainer = Color(0xFF93000A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue20,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = Teal30,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
    background = Neutral10,
    onBackground = Neutral90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun BudgetManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

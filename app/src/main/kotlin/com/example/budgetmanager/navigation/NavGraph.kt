package com.example.budgetmanager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.budgetmanager.feature.budget.presentation.BudgetScreen
import com.example.budgetmanager.feature.creditcards.presentation.CreditCardScreen
import com.example.budgetmanager.feature.dashboard.presentation.DashboardScreen
import com.example.budgetmanager.feature.expensesplit.presentation.ExpenseSplitScreen
import com.example.budgetmanager.feature.transactions.presentation.detail.TransactionDetailScreen
import com.example.budgetmanager.feature.transactions.presentation.list.TransactionListScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(id: Long) = "transaction_detail/$id"
    }
    object Budget : Screen("budget")
    object CreditCards : Screen("credit_cards")
    object ExpenseSplit : Screen("expense_split")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onViewAllTransactions = {
                    navController.navigate(Screen.Transactions.route)
                },
                onTransactionClick = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                }
            )
        }
        composable(Screen.Transactions.route) {
            TransactionListScreen(
                onTransactionClick = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                }
            )
        }
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) {
            TransactionDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Budget.route) {
            BudgetScreen()
        }
        composable(Screen.CreditCards.route) {
            CreditCardScreen()
        }
        composable(Screen.ExpenseSplit.route) {
            ExpenseSplitScreen()
        }
    }
}

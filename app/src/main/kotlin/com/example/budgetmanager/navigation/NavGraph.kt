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
import com.example.budgetmanager.feature.dashboard.presentation.savings.SavingsGoalScreen
import com.example.budgetmanager.importsms.ImportSmsScreen
import com.example.budgetmanager.feature.expensesplit.presentation.ExpenseSplitScreen
import com.example.budgetmanager.feature.transactions.presentation.add.AddTransactionScreen
import com.example.budgetmanager.feature.transactions.presentation.detail.TransactionDetailScreen
import com.example.budgetmanager.feature.transactions.presentation.list.TransactionListScreen

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Home")
    object Transactions : Screen("transactions", "History")
    object TransactionDetail : Screen("transaction_detail/{transactionId}", "Detail") {
        fun createRoute(id: Long) = "transaction_detail/$id"
    }
    object Budget : Screen("budget", "Budget")
    object CreditCards : Screen("credit_cards", "Cards")
    object ExpenseSplit : Screen("expense_split", "Splits")
    object AddTransaction : Screen("add_transaction", "Add")
    object SavingsGoals : Screen("savings_goals", "Goals")
    object ImportSms : Screen("import_sms", "Import")
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
                },
                onOpenSavingsGoals = {
                    navController.navigate(Screen.SavingsGoals.route)
                }
            )
        }
        composable(Screen.SavingsGoals.route) {
            SavingsGoalScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Transactions.route) {
            TransactionListScreen(
                onTransactionClick = { id ->
                    navController.navigate(Screen.TransactionDetail.createRoute(id))
                },
                onAddClick = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onImportClick = {
                    navController.navigate(Screen.ImportSms.route)
                }
            )
        }
        composable(Screen.AddTransaction.route) {
            AddTransactionScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ImportSms.route) {
            ImportSmsScreen(
                onBack = { navController.popBackStack() }
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

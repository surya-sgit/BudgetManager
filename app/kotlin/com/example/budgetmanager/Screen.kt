sealed class Screen(val route: String, val label: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Transactions : Screen("transactions", "Trans")
    object TransactionDetail : Screen("transaction_detail/{transactionId}", "Detail") {
        fun createRoute(id: Long) = "transaction_detail/$id"
    }

    object Budget : Screen("budget", "Budget")
    object CreditCards : Screen("credit_cards", "Cards")
    object ExpenseSplit : Screen("expense_split", "Split")
}
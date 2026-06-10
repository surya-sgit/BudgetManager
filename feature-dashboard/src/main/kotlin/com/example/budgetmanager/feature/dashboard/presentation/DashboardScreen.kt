package com.example.budgetmanager.feature.dashboard.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.domain.model.Transaction

@Composable
fun DashboardScreen(
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SummaryCard(state)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onViewAllTransactions) {
                    Text("View All")
                }
            }
        }

        items(state.recentTransactions) { transaction ->
            TransactionItem(transaction, onClick = { onTransactionClick(transaction.id) })
        }
    }
}

@Composable
fun SummaryCard(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Income", "₹${state.totalIncome}", Color(0xFF4CAF50))
                SummaryItem("Expense", "₹${state.totalExpense}", Color(0xFFF44336))
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Net Savings: ₹${state.netSavings}",
                style = MaterialTheme.typography.titleMedium,
                color = if (state.netSavings >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = amount, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = transaction.merchantName, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Account: ${transaction.accountId}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "${if (transaction.transactionType.name == "Expense") "-" else "+"} ₹${transaction.amount}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (transaction.transactionType.name == "Expense") Color.Red else Color(0xFF4CAF50)
            )
        }
    }
}

package com.example.budgetmanager.feature.dashboard.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.domain.model.Transaction

@Composable
fun DashboardScreen(
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSalaryDateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showSalaryDateDialog = true }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        item {
            SummaryCard(state)
        }

        state.smartBudget?.let { smartBudget ->
            item {
                SmartBudgetCard(smartBudget)
            }
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

    if (showSalaryDateDialog) {
        SalaryDateDialog(
            onDismiss = { showSalaryDateDialog = false },
            onConfirm = { date ->
                viewModel.updateSalaryDate(date)
                showSalaryDateDialog = false
            }
        )
    }
}

@Composable
fun SmartBudgetCard(smartBudget: com.example.budgetmanager.domain.usecase.CalculateSmartBudgetUseCase.SmartBudgetState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Smart Budget Cycle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Daily Limit", style = MaterialTheme.typography.labelMedium)
                    Text(text = "₹${"%.2f".format(smartBudget.dailyLimit)}", style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Days Left", style = MaterialTheme.typography.labelMedium)
                    Text(text = "${smartBudget.daysRemaining}", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Remaining Cycle Budget: ₹${"%.2f".format(smartBudget.remainingBudget)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SalaryDateDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dateText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Salary Date") },
        text = {
            Column {
                Text("Enter the day of month you receive your salary (1-31):")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 2) dateText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = dateText.toIntOrNull()
                if (date != null && date in 1..31) {
                    onConfirm(date)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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

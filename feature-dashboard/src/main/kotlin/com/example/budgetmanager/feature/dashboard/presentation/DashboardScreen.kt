package com.example.budgetmanager.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.core.ui.theme.ExpenseRed
import com.example.budgetmanager.core.ui.theme.ExpenseRedContainer
import com.example.budgetmanager.core.ui.theme.IncomeGreen
import com.example.budgetmanager.core.ui.theme.IncomeGreenContainer
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onOpenSavingsGoals: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSalaryDateDialog by remember { mutableStateOf(false) }

    // Kick off AI insight generation once the month's data has loaded.
    LaunchedEffect(state.isLoading, state.totalExpense, state.totalIncome) {
        if (!state.isLoading) viewModel.generateInsight()
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val dateStr = SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date())

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Greeting header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showSalaryDateDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Hero balance card
        item {
            BalanceHeroCard(
                state = state,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp)
            )
        }

        // Smart budget card
        state.smartBudget?.let { sb ->
            item {
                SmartBudgetCard(
                    smartBudget = sb,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                )
            }
        }

        // AI spending insight
        if (state.aiInsight != null || state.isInsightLoading) {
            item {
                AiInsightCard(
                    insight = state.aiInsight,
                    isLoading = state.isInsightLoading,
                    onRefresh = { viewModel.generateInsight(force = true) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                )
            }
        }

        // Savings goals entry
        item {
            SavingsGoalsEntryCard(
                onClick = onOpenSavingsGoals,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
            )
        }

        // Category breakdown chart
        if (state.categorySpending.isNotEmpty()) {
            item {
                CategoryBreakdownCard(
                    breakdown = state.categorySpending,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                )
            }
        }

        // Monthly trend chart
        if (state.monthlyTrend.any { it.total > 0 }) {
            item {
                MonthlyTrendCard(
                    trend = state.monthlyTrend,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                )
            }
        }

        // Recurring / subscriptions
        if (state.recurring.isNotEmpty()) {
            item {
                RecurringCard(
                    recurring = state.recurring,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                )
            }
        }

        // Recent transactions header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onViewAllTransactions) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (state.recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.recentTransactions) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
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
fun BalanceHeroCard(state: DashboardState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Net Savings this month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "₹${"%.2f".format(state.netSavings)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStat(
                    label = "Income",
                    amount = "₹${"%.0f".format(state.totalIncome)}",
                    dotColor = Color(0xFF81C784)
                )
                HeroStat(
                    label = "Expenses",
                    amount = "₹${"%.0f".format(state.totalExpense)}",
                    dotColor = Color(0xFFEF9A9A)
                )
            }
        }
    }
}

@Composable
fun HeroStat(label: String, amount: String, dotColor: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun SmartBudgetCard(
    smartBudget: com.example.budgetmanager.domain.usecase.CalculateSmartBudgetUseCase.SmartBudgetState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Smart Budget",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmartBudgetStat(
                    label = "Daily Limit",
                    value = "₹${"%.0f".format(smartBudget.dailyLimit)}"
                )
                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                SmartBudgetStat(
                    label = "Days Left",
                    value = "${smartBudget.daysRemaining}"
                )
                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                SmartBudgetStat(
                    label = "Remaining",
                    value = "₹${"%.0f".format(smartBudget.remainingBudget)}"
                )
            }
        }
    }
}

@Composable
fun SmartBudgetStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
        )
    }
}

@Composable
fun SalaryDateDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var dateText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Salary Date") },
        text = {
            Column {
                Text("Day of month you receive your salary (1–31):")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { if (it.length <= 2) dateText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = dateText.toIntOrNull()
                if (date != null && date in 1..31) onConfirm(date)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.transactionType == TransactionType.Expense
    val avatarBg = if (isExpense) ExpenseRedContainer else IncomeGreenContainer
    val avatarFg = if (isExpense) ExpenseRed else IncomeGreen
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val amountPrefix = if (isExpense) "−" else "+"
    val initial = transaction.merchantName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val timeStr = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
        .format(Date(transaction.timestamp))
    val methodLabel = transaction.paymentMethod.label
    val subtitle = listOfNotNull(
        timeStr,
        methodLabel.takeIf { it.isNotEmpty() },
        "Not counted".takeIf { transaction.excludeFromBudget }
    ).joinToString(" · ")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = avatarFg
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$amountPrefix ₹${"%.0f".format(transaction.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}

package com.example.budgetmanager.feature.budget.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.core.ui.theme.ExpenseRed
import com.example.budgetmanager.core.ui.theme.IncomeGreen
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.usecase.BudgetProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val progressList by viewModel.budgetProgress.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Start New Salary Cycle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Budget") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (progressList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No budgets set yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to create your first budget",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(progressList) { progress ->
                    BudgetItem(progress)
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { categoryId, limit ->
                viewModel.addBudget(categoryId, limit)
                showAddDialog = false
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Start New Salary Cycle?") },
            text = { Text("This will reset the cycle start date to today. Budget progress will be measured from now.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.logSalaryAndResetCycle()
                    showResetConfirm = false
                }) { Text("Start Cycle") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var limit by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Monthly Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val limitValue = limit.toDoubleOrNull()
            val isValid = selectedCategoryId != null && limitValue != null && limitValue > 0
            Button(
                onClick = {
                    selectedCategoryId?.let { id ->
                        onConfirm(id, limitValue!!)
                    }
                },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BudgetItem(progress: BudgetProgress) {
    val progressClamped = progress.progress.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = when {
        progress.isExceeded -> ExpenseRed
        progressClamped > 0.75f -> Color(0xFFF57C00)
        else -> IncomeGreen
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = progress.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Limit: ₹${"%.0f".format(progress.limit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${"%.0f".format(progress.currentSpending)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = progressClamped,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = indicatorColor,
                trackColor = trackColor
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progressClamped * 100).toInt()}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progress.dailySuggestedLimit > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "₹${"%.0f".format(progress.dailySuggestedLimit)}/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

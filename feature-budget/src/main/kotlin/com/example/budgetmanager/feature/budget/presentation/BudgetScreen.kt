package com.example.budgetmanager.feature.budget.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Budgets") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        if (progressList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = "No budgets set yet. Click + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(progressList) { progress ->
                    BudgetItem(progress)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text("Monthly Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCategoryId?.let { id ->
                        val limitValue = limit.toDoubleOrNull() ?: 0.0
                        onConfirm(id, limitValue)
                    }
                },
                enabled = selectedCategoryId != null && limit.isNotBlank()
            ) {
                Text("Add")
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
fun BudgetItem(progress: BudgetProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = progress.categoryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "₹${progress.limit}", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress.progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress.isExceeded) Color.Red else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ₹${progress.currentSpending}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (progress.isExceeded) Color.Red else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

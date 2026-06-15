package com.example.budgetmanager.feature.creditcards.presentation

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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.domain.model.CreditCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen(
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val cards by viewModel.creditCards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Credit Cards") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cards) { card ->
                CreditCardItem(card)
            }
        }

        if (showAddDialog) {
            AddCreditCardDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { card ->
                    viewModel.addCreditCard(card)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (CreditCard) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var spend by remember { mutableStateOf("") }
    var billDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Credit Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Card Name") })
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Credit Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = spend,
                    onValueChange = { spend = it },
                    label = { Text("Current Spend") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = billDate,
                    onValueChange = { billDate = it },
                    label = { Text("Billing Date (Day of Month)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (Day of Month)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cardLimit = limit.toDoubleOrNull() ?: 0.0
                    val cardSpend = spend.toDoubleOrNull() ?: 0.0
                    onConfirm(
                        CreditCard(
                            id = 0L,
                            cardName = name,
                            creditLimit = cardLimit,
                            availableLimit = cardLimit - cardSpend,
                            currentSpend = cardSpend,
                            statementBalance = 0.0,
                            billingDate = billDate.toIntOrNull() ?: 1,
                            dueDate = dueDate.toIntOrNull() ?: 1
                        )
                    )
                }
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
fun CreditCardItem(card: CreditCard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = card.cardName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Current Spend", style = MaterialTheme.typography.labelMedium)
                    Text(text = "₹${card.currentSpend}", style = MaterialTheme.typography.titleLarge)
                }
                Column {
                    Text(text = "Available Limit", style = MaterialTheme.typography.labelMedium)
                    Text(text = "₹${card.availableLimit}", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val utilization = (card.currentSpend / card.creditLimit).toFloat()
            LinearProgressIndicator(
                progress = utilization,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (utilization > 0.8f) Color.Red else MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Bill Date: ${card.billingDate}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Due Date: ${card.dueDate}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

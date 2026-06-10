package com.example.budgetmanager.feature.expensesplit.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetmanager.domain.model.SplitParticipant
import com.example.budgetmanager.domain.model.SplitStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSplitScreen() {
    // Placeholder UI for Expense Split
    Scaffold(
        topBar = { TopAppBar(title = { Text("Expense Splits") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Split")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "Track who owes you what",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Example list of split summaries
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    SplitSummaryCard("Dinner with Friends", 1200.0, 3)
                }
                item {
                    SplitSummaryCard("Grocery Split", 450.0, 2)
                }
            }
        }
    }
}

@Composable
fun SplitSummaryCard(title: String, amount: Double, participants: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = "₹$amount", style = MaterialTheme.typography.titleMedium)
            }
            Text(text = "$participants participants", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = 0.6f, // Example: 60% settled
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "60% Settled", style = MaterialTheme.typography.labelSmall)
        }
    }
}

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.domain.model.CreditCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen(
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val cards by viewModel.creditCards.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Credit Cards") })
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
    }
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

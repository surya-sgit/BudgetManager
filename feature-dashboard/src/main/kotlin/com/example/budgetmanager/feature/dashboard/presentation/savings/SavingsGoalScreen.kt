package com.example.budgetmanager.feature.dashboard.presentation.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.domain.model.SavingsGoal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalScreen(
    onBack: () -> Unit,
    viewModel: SavingsGoalViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var contributeTarget by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                text = { Text("New Goal") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No savings goals yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to set your first target",
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
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        onContribute = { contributeTarget = goal },
                        onDelete = { viewModel.deleteGoal(goal) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target, saved ->
                viewModel.addGoal(name, target, saved, null)
                showAddDialog = false
            }
        )
    }

    contributeTarget?.let { goal ->
        ContributeDialog(
            goalName = goal.name,
            onDismiss = { contributeTarget = null },
            onConfirm = { amount ->
                viewModel.contribute(goal, amount)
                contributeTarget = null
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: SavingsGoal,
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "₹${"%.0f".format(goal.savedAmount)} / ₹${"%.0f".format(goal.targetAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = goal.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goal.isComplete) "🎉 Goal reached!" else "${(goal.progress * 100).toInt()}% saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onContribute) { Text("Add money") }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, saved: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }

    val targetValue = target.toDoubleOrNull()
    val savedValue = saved.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && targetValue != null && targetValue > 0 && savedValue >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = saved,
                    onValueChange = { saved = it },
                    label = { Text("Already saved (₹, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, targetValue!!, savedValue) },
                enabled = isValid
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ContributeDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val amountValue = amount.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $goalName") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amountValue ?: return@Button) },
                enabled = amountValue != null && amountValue > 0
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

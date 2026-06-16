package com.example.budgetmanager.feature.creditcards.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.core.ui.theme.ExpenseRed
import com.example.budgetmanager.domain.model.CreditCard

private val CardGradients = listOf(
    listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
    listOf(Color(0xFF283593), Color(0xFF1A237E)),
    listOf(Color(0xFF00695C), Color(0xFF004D40)),
    listOf(Color(0xFF6A1B9A), Color(0xFF4A148C))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen(
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val cards by viewModel.creditCards.collectAsState()
    val cycleSpend by viewModel.cycleSpend.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cards", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Card") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (cards.isEmpty()) {
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
                        text = "No cards added yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to add your credit card",
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cards.withIndex().toList()) { (index, card) ->
                    CreditCardItem(
                        card = card,
                        gradientIndex = index % CardGradients.size,
                        spentThisCycle = cycleSpend[card.id] ?: card.currentSpend,
                        onClick = { editingCard = card }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CreditCardDialog(
            existing = null,
            otherCards = emptyList(),
            onDismiss = { showAddDialog = false },
            onConfirm = { card ->
                viewModel.addCreditCard(card)
                showAddDialog = false
            },
            onDelete = null,
            onMerge = null
        )
    }

    editingCard?.let { card ->
        CreditCardDialog(
            existing = card,
            otherCards = cards.filter { it.id != card.id },
            onDismiss = { editingCard = null },
            onConfirm = { updated ->
                viewModel.updateCreditCard(updated)
                editingCard = null
            },
            onDelete = {
                viewModel.deleteCreditCard(card)
                editingCard = null
            },
            onMerge = { target ->
                viewModel.mergeInto(source = card, target = target)
                editingCard = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardDialog(
    existing: CreditCard?,
    otherCards: List<CreditCard>,
    onDismiss: () -> Unit,
    onConfirm: (CreditCard) -> Unit,
    onDelete: (() -> Unit)?,
    onMerge: ((CreditCard) -> Unit)?
) {
    val isEdit = existing != null
    var mergeMenuOpen by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(existing?.cardName ?: "") }
    var last4 by remember { mutableStateOf(existing?.lastFourDigits ?: "") }
    var limit by remember { mutableStateOf(existing?.creditLimit?.takeIf { it > 0 }?.let { "%.0f".format(it) } ?: "") }
    var spend by remember { mutableStateOf(existing?.currentSpend?.takeIf { it > 0 }?.let { "%.0f".format(it) } ?: "") }
    var billDate by remember { mutableStateOf(existing?.billingDate?.takeIf { isEdit }?.toString() ?: "") }
    var dueDate by remember { mutableStateOf(existing?.dueDate?.takeIf { isEdit }?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Card" else "Add Credit Card") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = last4,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4 = it },
                    label = { Text("Last 4 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Credit Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = spend,
                    onValueChange = { spend = it },
                    label = { Text("Current Spend (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billDate,
                        onValueChange = { if (it.length <= 2) billDate = it },
                        label = { Text("Bill Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { if (it.length <= 2) dueDate = it },
                        label = { Text("Due Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Merge this (e.g. auto-created) card into another card you already have
                if (onMerge != null && otherCards.isNotEmpty()) {
                    Box {
                        TextButton(
                            onClick = { mergeMenuOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Merge into another card…") }
                        DropdownMenu(
                            expanded = mergeMenuOpen,
                            onDismissRequest = { mergeMenuOpen = false }
                        ) {
                            otherCards.forEach { target ->
                                DropdownMenuItem(
                                    text = {
                                        val suffix = target.lastFourDigits.takeIf { it.isNotBlank() }
                                            ?.let { " ••$it" } ?: ""
                                        Text("${target.cardName}$suffix")
                                    },
                                    onClick = {
                                        mergeMenuOpen = false
                                        onMerge(target)
                                    }
                                )
                            }
                        }
                    }
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Delete card", color = ExpenseRed) }
                }
            }
        },
        confirmButton = {
            val cardLimit = limit.toDoubleOrNull()
            val cardSpend = spend.toDoubleOrNull() ?: 0.0
            val billDay = billDate.toIntOrNull()
            val dueDay = dueDate.toIntOrNull()
            val isValid = name.isNotBlank() &&
                cardLimit != null && cardLimit > 0 &&
                cardSpend >= 0 && cardSpend <= cardLimit &&
                (billDate.isBlank() || billDay != null && billDay in 1..31) &&
                (dueDate.isBlank() || dueDay != null && dueDay in 1..31)
            Button(
                onClick = {
                    onConfirm(
                        CreditCard(
                            id = existing?.id ?: 0L,
                            cardName = name,
                            creditLimit = cardLimit!!,
                            availableLimit = cardLimit - cardSpend,
                            currentSpend = cardSpend,
                            statementBalance = existing?.statementBalance ?: 0.0,
                            billingDate = billDay ?: 1,
                            dueDate = dueDay ?: 1,
                            lastFourDigits = last4,
                            needsSetup = false
                        )
                    )
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
fun CreditCardItem(
    card: CreditCard,
    gradientIndex: Int,
    spentThisCycle: Double,
    onClick: () -> Unit = {}
) {
    val gradient = CardGradients[gradientIndex]
    val available = (card.creditLimit - spentThisCycle).coerceAtLeast(0.0)
    val utilization = if (card.creditLimit > 0) (spentThisCycle / card.creditLimit).toFloat() else 0f
    val utilizationColor = when {
        utilization > 0.8f -> ExpenseRed
        utilization > 0.5f -> Color(0xFFF57C00)
        else -> Color(0xFF81C784)
    }

    // Visual credit card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(
                brush = Brush.linearGradient(gradient),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Card name and chip row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = card.cardName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (card.lastFourDigits.isNotBlank()) {
                        Text(
                            text = "•••• ${card.lastFourDigits}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    if (card.needsSetup) {
                        Text(
                            text = "Tap to set up",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFE082)
                        )
                    }
                }
                // Chip decoration
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 28.dp)
                        .background(Color(0xFFFFD54F), RoundedCornerShape(4.dp))
                )
            }

            // Balance row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Spent this cycle",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "₹${"%.0f".format(spentThisCycle)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (card.creditLimit > 0) "₹${"%.0f".format(available)}" else "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Details panel below card
    Spacer(Modifier.height(8.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Utilization",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(utilization * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = utilizationColor
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = utilization.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = utilizationColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bill: ${card.billingDate}th",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Due: ${card.dueDate}th",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Limit: ₹${"%.0f".format(card.creditLimit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

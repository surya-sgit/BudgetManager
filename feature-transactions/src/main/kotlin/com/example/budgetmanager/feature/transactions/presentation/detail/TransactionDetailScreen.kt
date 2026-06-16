package com.example.budgetmanager.feature.transactions.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import com.example.budgetmanager.domain.model.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.core.ui.theme.ExpenseRed
import com.example.budgetmanager.core.ui.theme.ExpenseRedContainer
import com.example.budgetmanager.core.ui.theme.IncomeGreen
import com.example.budgetmanager.core.ui.theme.IncomeGreenContainer
import com.example.budgetmanager.domain.model.ExpenseSplit
import com.example.budgetmanager.domain.model.PaymentMethod
import com.example.budgetmanager.domain.model.SplitParticipant
import com.example.budgetmanager.domain.model.SplitStatus
import com.example.budgetmanager.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsState()
    val split by viewModel.split.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val deleted by viewModel.deleted.collectAsState()
    var showSplitDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("d MMMM yyyy, h:mm:ss a", Locale.getDefault())

    LaunchedEffect(deleted) {
        if (deleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        transaction?.let { tx ->
            val isExpense = tx.transactionType == TransactionType.Expense
            val amountColor = if (isExpense) ExpenseRed else IncomeGreen
            val amountBg = if (isExpense) ExpenseRedContainer else IncomeGreenContainer
            val amountPrefix = if (isExpense) "−" else "+"
            val typeLabel = if (isExpense) "Expense" else "Income"

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // Amount hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(amountBg, RoundedCornerShape(24.dp))
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = amountColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$amountPrefix ₹${"%.2f".format(tx.amount)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Details card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        DetailRow(label = "Merchant", value = tx.merchantName)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        DetailRow(label = "Date & Time", value = dateFormat.format(Date(tx.timestamp)))
                        if (tx.notes.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                            DetailRow(label = "Notes", value = tx.notes)
                        }
                    }
                }

                // Category — tap to reclassify; also teaches future SMS from this merchant
                Spacer(Modifier.height(16.dp))
                CategorySelectorCard(
                    currentCategoryId = tx.categoryId,
                    categories = categories,
                    onSelect = viewModel::reclassify
                )

                // Payment method (Card / UPI / Cash …)
                Spacer(Modifier.height(16.dp))
                PaymentMethodSelectorCard(
                    current = tx.paymentMethod,
                    onSelect = viewModel::setPaymentMethod
                )

                // Exclude from spending (card bill payment / transfer)
                if (isExpense) {
                    Spacer(Modifier.height(16.dp))
                    ExcludeFromBudgetCard(
                        excluded = tx.excludeFromBudget,
                        onToggle = viewModel::setExcludeFromBudget
                    )
                }

                // Split section — only meaningful for expenses
                if (isExpense) {
                    Spacer(Modifier.height(16.dp))
                    SplitSection(
                        totalAmount = tx.amount,
                        split = split,
                        onStartSplit = { showSplitDialog = true },
                        onTogglePaid = viewModel::toggleParticipantPaid
                    )
                }

                Spacer(Modifier.height(16.dp))

                // SMS body
                Text(
                    text = "Original SMS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Text(
                        text = tx.smsBody,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            if (showSplitDialog) {
                SplitDialog(
                    totalAmount = tx.amount,
                    onDismiss = { showSplitDialog = false },
                    onConfirm = { names ->
                        viewModel.createEqualSplit(names)
                        showSplitDialog = false
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete transaction?") },
                    text = { Text("This permanently removes \"${tx.merchantName}\" and any split attached to it.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.delete()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun SplitSection(
    totalAmount: Double,
    split: Pair<ExpenseSplit, List<SplitParticipant>>?,
    onStartSplit: () -> Unit,
    onTogglePaid: (SplitParticipant) -> Unit
) {
    Text(
        text = "Split",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (split == null) {
                Text(
                    text = "Shared this with friends? Split it equally and track who's paid you back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStartSplit, modifier = Modifier.fillMaxWidth()) {
                    Text("Split this expense")
                }
            } else {
                val (info, participants) = split
                val settledCount = participants.count { it.status == SplitStatus.Paid }
                val owed = participants.filter { it.status != SplitStatus.Paid }.sumOf { it.shareAmount }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "₹${"%.2f".format(info.amountPerPerson)} each",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$settledCount/${participants.size} paid",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (owed > 0)
                        "Only your ₹${"%.0f".format(info.amountPerPerson)} share counts toward your budget · ₹${"%.0f".format(owed)} owed to you"
                    else "Fully settled — everyone has paid you back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                participants.forEach { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "₹${"%.0f".format(participant.shareAmount)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterChip(
                            selected = participant.status == SplitStatus.Paid,
                            onClick = { onTogglePaid(participant) },
                            label = {
                                Text(if (participant.status == SplitStatus.Paid) "Paid" else "Pending")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitDialog(
    totalAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var names by remember { mutableStateOf(listOf("You", "Person 2")) }
    val perPerson = if (names.isNotEmpty()) totalAmount / names.size else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split Expense") },
        text = {
            Column {
                Text(
                    text = "₹${"%.2f".format(perPerson)} per person (${names.size} people)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                names.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { updated ->
                                names = names.toMutableList().also { it[index] = updated }
                            },
                            label = { Text("Person ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (names.size > 2) {
                            IconButton(onClick = {
                                names = names.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove person")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { names = names + "Person ${names.size + 1}" },
                    enabled = names.size < 12
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add person")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(names) },
                enabled = names.size >= 2
            ) { Text("Split") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PaymentMethodSelectorCard(
    current: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
) {
    val options = listOf(
        PaymentMethod.Card, PaymentMethod.Upi, PaymentMethod.NetBanking,
        PaymentMethod.Wallet, PaymentMethod.Cash
    )
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = current.label.ifEmpty { "Not set" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paid via",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Change payment method",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.label) },
                        onClick = {
                            onSelect(method)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExcludeFromBudgetCard(
    excluded: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Card bill payment / transfer",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Exclude from spending totals so it isn't double-counted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = excluded, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun CategorySelectorCard(
    currentCategoryId: Long,
    categories: List<Category>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentName = categories.firstOrNull { it.id == currentCategoryId }?.name ?: "Uncategorized"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = categories.isNotEmpty()) { expanded = true }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = currentName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Change category",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onSelect(category.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

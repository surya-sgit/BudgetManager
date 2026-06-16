package com.example.budgetmanager.feature.transactions.presentation.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.domain.model.PaymentMethod
import com.example.budgetmanager.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.Expense) }
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var method by remember { mutableStateOf(PaymentMethod.Cash) }
    var methodMenuOpen by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val methodOptions = listOf(
        PaymentMethod.Cash, PaymentMethod.Upi, PaymentMethod.Card,
        PaymentMethod.NetBanking, PaymentMethod.Wallet
    )
    val dateLabel = remember(dateMillis) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    // Default the category to match the chosen type: "Income" for income, the first
    // spending category otherwise.
    LaunchedEffect(type, categories) {
        if (categories.isEmpty()) return@LaunchedEffect
        val incomeCat = categories.firstOrNull { it.name.equals("Income", ignoreCase = true) }
        selectedCategoryId = if (type == TransactionType.Income) {
            incomeCat?.id ?: categories.first().id
        } else {
            categories.firstOrNull { !it.name.equals("Income", ignoreCase = true) }?.id
                ?: categories.first().id
        }
    }

    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    val amountValue = amount.toDoubleOrNull()
    val isValid = amountValue != null && amountValue > 0 && selectedCategoryId > 0L
    val selectedCategoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Select category"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Income / Expense toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TransactionType.Expense,
                    onClick = { type = TransactionType.Expense },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Expense") }
                SegmentedButton(
                    selected = type == TransactionType.Income,
                    onClick = { type = TransactionType.Income },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Income") }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amount.isNotBlank() && (amountValue == null || amountValue <= 0),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryMenuOpen,
                onExpandedChange = { categoryMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuOpen,
                    onDismissRequest = { categoryMenuOpen = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategoryId = category.id
                                categoryMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Payment method dropdown
            ExposedDropdownMenuBox(
                expanded = methodMenuOpen,
                onExpandedChange = { methodMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = method.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment method") },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = methodMenuOpen,
                    onDismissRequest = { methodMenuOpen = false }
                ) {
                    methodOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                method = option
                                methodMenuOpen = false
                            }
                        )
                    }
                }
            }

            // Date (back-dating supported)
            OutlinedTextField(
                value = dateLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.save(
                        amount = amountValue ?: return@Button,
                        type = type,
                        categoryId = selectedCategoryId,
                        merchantName = merchant,
                        notes = notes,
                        timestamp = dateMillis,
                        paymentMethod = method
                    )
                },
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save Transaction", fontWeight = FontWeight.SemiBold)
            }
        }

        if (showDatePicker) {
            val dpState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = dpState)
            }
        }
    }
}

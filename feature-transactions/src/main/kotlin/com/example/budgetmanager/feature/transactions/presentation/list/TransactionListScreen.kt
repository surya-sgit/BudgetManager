package com.example.budgetmanager.feature.transactions.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetmanager.core.ui.theme.ExpenseRed
import com.example.budgetmanager.core.ui.theme.ExpenseRedContainer
import com.example.budgetmanager.core.ui.theme.IncomeGreen
import com.example.budgetmanager.core.ui.theme.IncomeGreenContainer
import com.example.budgetmanager.domain.model.Category
import com.example.budgetmanager.domain.model.Transaction
import com.example.budgetmanager.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("History", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Import from SMS"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = {
                            Text(
                                "Search transactions…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Filters: type + category + date range
            FilterBar(
                typeFilter = state.typeFilter,
                categoryFilter = state.categoryFilter,
                dateRange = state.dateRange,
                categories = categories,
                onTypeChange = viewModel::setTypeFilter,
                onCategoryChange = viewModel::setCategoryFilter,
                onDateRangeChange = viewModel::setDateRange
            )

            // Summary of the currently filtered set
            FilterSummary(
                count = state.transactions.size,
                totalIncome = state.totalIncome,
                totalExpense = state.totalExpense
            )

            if (state.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isBlank()) "No transactions match these filters" else "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.transactions) { transaction ->
                        TransactionListItem(
                            transaction = transaction,
                            myShare = state.shareByTransaction[transaction.id],
                            onClick = { onTransactionClick(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBar(
    typeFilter: TransactionType?,
    categoryFilter: Long?,
    dateRange: DateRangeFilter,
    categories: List<Category>,
    onTypeChange: (TransactionType?) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onDateRangeChange: (DateRangeFilter) -> Unit
) {
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var dateMenuOpen by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.firstOrNull { it.id == categoryFilter }?.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = typeFilter == null,
            onClick = { onTypeChange(null) },
            label = { Text("All") }
        )
        FilterChip(
            selected = typeFilter == TransactionType.Expense,
            onClick = { onTypeChange(TransactionType.Expense) },
            label = { Text("Expense") }
        )
        FilterChip(
            selected = typeFilter == TransactionType.Income,
            onClick = { onTypeChange(TransactionType.Income) },
            label = { Text("Income") }
        )

        Box {
            FilterChip(
                selected = categoryFilter != null,
                onClick = { categoryMenuOpen = true },
                label = { Text(selectedCategoryName ?: "Category") }
            )
            DropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { categoryMenuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All categories") },
                    onClick = {
                        onCategoryChange(null)
                        categoryMenuOpen = false
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onCategoryChange(category.id)
                            categoryMenuOpen = false
                        }
                    )
                }
            }
        }

        Box {
            FilterChip(
                selected = dateRange != DateRangeFilter.All,
                onClick = { dateMenuOpen = true },
                label = { Text(if (dateRange == DateRangeFilter.All) "Date" else dateRange.label) }
            )
            DropdownMenu(
                expanded = dateMenuOpen,
                onDismissRequest = { dateMenuOpen = false }
            ) {
                DateRangeFilter.values().forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.label) },
                        onClick = {
                            onDateRangeChange(range)
                            dateMenuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSummary(count: Int, totalIncome: Double, totalExpense: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count txns",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (totalExpense > 0) {
            Text(
                text = "Spent ₹${"%.0f".format(totalExpense)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = ExpenseRed
            )
        }
        if (totalIncome > 0) {
            Text(
                text = "Received ₹${"%.0f".format(totalIncome)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = IncomeGreen
            )
        }
    }
}

@Composable
fun TransactionListItem(transaction: Transaction, myShare: Double? = null, onClick: () -> Unit) {
    val isExpense = transaction.transactionType == TransactionType.Expense
    val avatarBg = if (isExpense) ExpenseRedContainer else IncomeGreenContainer
    val avatarFg = if (isExpense) ExpenseRed else IncomeGreen
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val amountPrefix = if (isExpense) "−" else "+"
    val initial = transaction.merchantName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val dateStr = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
        .format(Date(transaction.timestamp))
    val methodLabel = transaction.paymentMethod.label
    val subtitle = listOfNotNull(
        dateStr,
        methodLabel.takeIf { it.isNotEmpty() },
        "Not counted".takeIf { transaction.excludeFromBudget }
    ).joinToString(" · ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix ₹${"%.0f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
                if (myShare != null && myShare < transaction.amount) {
                    Text(
                        text = "your share ₹${"%.0f".format(myShare)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

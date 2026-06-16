package com.example.budgetmanager.importsms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSmsScreen(
    onBack: () -> Unit,
    viewModel: ImportSmsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val datePickerState = rememberDatePickerState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from SMS", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Pick a start date — every bank SMS from that date until today will be scanned and imported. Already-imported messages are skipped automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            DatePicker(
                state = datePickerState,
                title = null,
                showModeToggle = false
            )

            if (state.permissionMissing) {
                Text(
                    text = "SMS read permission is required. Enable it in App Settings, then try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.resultMessage?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.scanSince(it) }
                },
                enabled = datePickerState.selectedDateMillis != null && !state.isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Scanning…")
                } else {
                    Text("Scan & Import", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

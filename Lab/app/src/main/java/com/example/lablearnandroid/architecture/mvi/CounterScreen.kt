package com.example.lablearnandroid.architecture.mvi

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * MVI Screen: The Compose UI component.
 * In MVI, the Screen observes the State and dispatches Intents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    counterViewModel: CounterMviViewModel = viewModel()
) {
    val uiState by counterViewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MVI Counter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "MVI Architecture Counter",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Count",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = uiState.count.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { counterViewModel.handleIntent(CounterIntent.Decrement) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-")
                }
                
                Button(
                    onClick = { counterViewModel.handleIntent(CounterIntent.Increment) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+")
                }
            }
            
            Button(
                onClick = { counterViewModel.handleIntent(CounterIntent.Reset) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Reset")
            }
            
            // MVI Architecture Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "MVI Pattern",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Model: CounterState (immutable state)\n• View: CounterScreen (UI + dispatch)\n• Intent: CounterIntent (user actions)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

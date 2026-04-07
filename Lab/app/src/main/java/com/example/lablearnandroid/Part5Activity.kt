package com.example.lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel demonstrating side effects with SharedFlow for one-time events
 * Uses SharedFlow instead of StateFlow to ensure events are consumed only once
 */
class SideEffectViewModel : ViewModel() {
    
    // SharedFlow for one-time error events
    // Unlike StateFlow, SharedFlow doesn't hold the last value - perfect for transient events
    private val _errorEvents = Channel<String>(Channel.BUFFERED).apply {
        // Convert Channel to SharedFlow for multiple observers
        receiveAsFlow().shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep alive for 5 seconds after last subscriber
            replay = 0 // No replay - events are one-time only
        )
    }
    
    // Public flow that UI can collect
    val errorEvents: Flow<String> = _errorEvents.receiveAsFlow()
    
    // Mock error messages for demonstration
    private val errorMessages = listOf(
        "Connection Timeout!",
        "Network Error: Please check your internet",
        "Server Unavailable - Try again later",
        "Authentication Failed - Invalid credentials",
        "Data Sync Error - Changes not saved"
    )
    
    private var errorIndex = 0
    
    /**
     * Triggers a one-time error event
     * Emits to SharedFlow - consumed only once by collectors
     */
    fun triggerError() {
        val errorMessage = errorMessages[errorIndex % errorMessages.size]
        errorIndex++
        
        viewModelScope.launch {
            // Send error event to the channel
            _errorEvents.send(errorMessage)
        }
    }
    
    /**
     * Triggers a specific error message
     */
    fun triggerSpecificError(message: String) {
        viewModelScope.launch {
            _errorEvents.send(message)
        }
    }
    
    /**
     * Demonstrates the difference between one-time events and persistent state
     */
    private val _persistentState = MutableStateFlow("This is persistent state")
    val persistentState: StateFlow<String> = _persistentState.asStateFlow()
    
    fun updatePersistentState() {
        _persistentState.value = "Updated at ${System.currentTimeMillis()}"
    }
}

/**
 * Part5Activity demonstrating Compose Side Effects for One-time UI Events
 */
class Part5Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideEffectDemoScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SideEffectDemoScreen(
    sideEffectViewModel: SideEffectViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Snackbar host state for showing transient messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect persistent state (demonstrates difference from one-time events)
    val persistentState by sideEffectViewModel.persistentState.collectAsState()
    
    // SIDE EFFECT: LaunchedEffect to handle one-time error events
    // This is the key concept - LaunchedEffect handles side effects that shouldn't be in UI state
    LaunchedEffect(Unit) {
        // Collect error events from ViewModel
        sideEffectViewModel.errorEvents.collect { errorMessage ->
            // Show Snackbar for one-time error event
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short,
                actionLabel = "Dismiss"
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Side Effects Demo",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            // Bottom bar showing persistent state (for comparison)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Persistent State: $persistentState",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Side Effects vs UI State",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "This demo shows the difference between one-time events (side effects) and persistent UI state:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "1. Errors appear once in Snackbar (side effect)\n2. Persistent state shows in bottom bar (UI state)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Main trigger buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Primary error trigger button
                Button(
                    onClick = { sideEffectViewModel.triggerError() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Error")
                }
                
                // Additional test buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            sideEffectViewModel.triggerSpecificError("Custom Error Message!")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Custom Error")
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            sideEffectViewModel.updatePersistentState()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update State")
                    }
                }
            }
            
            // Technical explanation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Why LaunchedEffect?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "LaunchedEffect handles side effects that shouldn't persist in UI state:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        text = "1. Error messages appear once, then disappear\n2. Navigation events\n3. Analytics tracking\n4. One-time notifications",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Regular UI State (StateFlow): Persistent data that should survive configuration changes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // SharedFlow vs StateFlow comparison
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SharedFlow: One-time events (no replay)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = "StateFlow: Persistent state (holds last value)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

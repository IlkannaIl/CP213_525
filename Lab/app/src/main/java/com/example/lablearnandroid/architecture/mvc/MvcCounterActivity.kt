package com.example.lablearnandroid.architecture.mvc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lablearnandroid.ui.theme.LabLearnAndroidTheme

/**
 * MVC Activity: Acts as the Controller, handling both UI (Jetpack Compose) and logic updates.
 * In MVC, the Controller handles user input and updates both Model and View.
 */
class MvcCounterActivity : ComponentActivity() {
    
    // Model instance - holds the data
    private val counterModel = MvcCounterModel()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabLearnAndroidTheme {
                MvcCounterScreen()
            }
        }
    }
    
    // Controller methods - handle user actions
    private fun incrementCounter(): Int {
        return counterModel.increment()
    }
    
    private fun decrementCounter(): Int {
        return counterModel.decrement()
    }
    
    private fun resetCounter(): Int {
        return counterModel.reset()
    }
    
    private fun getCurrentCount(): Int {
        return counterModel.getCount()
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MvcCounterScreen() {
        // State to trigger recomposition when counter changes
        var count by remember { mutableStateOf(getCurrentCount()) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MVC Counter") },
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
                    text = "MVC Architecture Counter",
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
                            text = count.toString(),
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
                        onClick = { 
                            count = decrementCounter()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-")
                    }
                    
                    Button(
                        onClick = { 
                            count = incrementCounter()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+")
                    }
                }
                
                Button(
                    onClick = { 
                        count = resetCounter()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Reset")
                }
                
                // MVC Architecture Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "MVC Pattern",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Model: MvcCounterModel (data)\n• View: Compose UI (display)\n• Controller: Activity (logic)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

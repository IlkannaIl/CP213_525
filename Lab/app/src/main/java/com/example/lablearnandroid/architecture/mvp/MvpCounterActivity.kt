package com.example.lablearnandroid.architecture.mvp

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
 * MVP Activity: Implements the View interface and initializes the Presenter.
 * In MVP, the Activity implements the View interface and delegates UI updates to the Presenter.
 */
class MvpCounterActivity : ComponentActivity(), MvpCounterView {
    
    // MVP components
    private lateinit var model: MvpCounterModel
    private lateinit var presenter: MvpCounterPresenter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize MVP components
        model = MvpCounterModel()
        presenter = MvpCounterPresenter(model, this)
        
        setContent {
            LabLearnAndroidTheme {
                MvpCounterScreen()
            }
        }
    }
    
    // MVP View interface implementation
    override fun displayCount(count: Int) {
        // This will be handled by the Compose state
    }
    
    override fun showIncrementResult(count: Int) {
        // This will be handled by the Compose state
    }
    
    override fun showDecrementResult(count: Int) {
        // This will be handled by the Compose state
    }
    
    override fun showResetResult(count: Int) {
        // This will be handled by the Compose state
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MvpCounterScreen() {
        // State to trigger recomposition when counter changes
        var count by remember { mutableStateOf(presenter.getCurrentCount()) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("MVP Counter") },
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
                    text = "MVP Architecture Counter",
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
                            presenter.onDecrementClicked()
                            count = presenter.getCurrentCount()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-")
                    }
                    
                    Button(
                        onClick = { 
                            presenter.onIncrementClicked()
                            count = presenter.getCurrentCount()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+")
                    }
                }
                
                Button(
                    onClick = { 
                        presenter.onResetClicked()
                        count = presenter.getCurrentCount()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Reset")
                }
                
                // MVP Architecture Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "MVP Pattern",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Model: MvpCounterModel (data)\n• View: Activity + Interface (display)\n• Presenter: Logic + coordination",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

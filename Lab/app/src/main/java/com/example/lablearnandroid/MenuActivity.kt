package com.example.lablearnandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lablearnandroid.architecture.mvc.MvcCounterActivity
import com.example.lablearnandroid.architecture.mvp.MvpCounterActivity
import com.example.lablearnandroid.architecture.mvvm.MvvmCounterActivity
import com.example.lablearnandroid.architecture.mvi.MviCounterActivity
import com.example.lablearnandroid.ui.theme.LabLearnAndroidTheme

class MenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabLearnAndroidTheme {
                MenuScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen() {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Lab Learn Android",
                        fontWeight = FontWeight.Bold
                    ) 
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select an Activity",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Core Activities
            MenuButton(
                text = "RPG Card Activity",
                description = "RPG card display with status controls",
                onClick = { 
                    context.startActivity(Intent(context, RPGCardActivity::class.java))
                }
            )
            
            MenuButton(
                text = "Life Cycle Compose",
                description = "Compose lifecycle demonstration",
                onClick = { 
                    context.startActivity(Intent(context, LifeCycleComposeActivity::class.java))
                }
            )
            
            MenuButton(
                text = "Main Activity 3",
                description = "Third main activity variant",
                onClick = { 
                    context.startActivity(Intent(context, MainActivity3::class.java))
                }
            )
            
            // Feature Activities
            MenuButton(
                text = "Image Picker",
                description = "Gallery image picker with permissions",
                onClick = { 
                    context.startActivity(Intent(context, ImagePickerActivity::class.java))
                }
            )
            
            MenuButton(
                text = "Sensors + MVVM",
                description = "Real-time accelerometer and location with MVVM",
                onClick = { 
                    context.startActivity(Intent(context, SensorActivity::class.java))
                }
            )
            
                        
            MenuButton(
                text = "Pokedex",
                description = "Pokemon API demonstration",
                onClick = { 
                    context.startActivity(Intent(context, PokedexActivity::class.java))
                }
            )
            
            // Architecture Pattern Activities
            MenuButton(
                text = "MVC Counter",
                description = "Model-View-Controller pattern counter",
                onClick = { 
                    context.startActivity(Intent(context, MvcCounterActivity::class.java))
                }
            )
            
            MenuButton(
                text = "MVP Counter",
                description = "Model-View-Presenter pattern counter",
                onClick = { 
                    context.startActivity(Intent(context, MvpCounterActivity::class.java))
                }
            )
            
            MenuButton(
                text = "MVVM Counter",
                description = "Model-View-ViewModel pattern counter",
                onClick = { 
                    context.startActivity(Intent(context, MvvmCounterActivity::class.java))
                }
            )
            
            MenuButton(
                text = "MVI Counter",
                description = "Model-View-Intent pattern counter",
                onClick = { 
                    context.startActivity(Intent(context, MviCounterActivity::class.java))
                }
            )
        }
    }
}

@Composable
fun MenuButton(
    text: String,
    description: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
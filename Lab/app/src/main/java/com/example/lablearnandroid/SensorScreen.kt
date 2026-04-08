package com.example.lablearnandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
    sensorViewModel: SensorViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Collect StateFlow values
    val accelerometerData by sensorViewModel.accelerometerData.collectAsState()
    val locationData by sensorViewModel.locationData.collectAsState()
    val hasAccelerometerPermission by sensorViewModel.hasAccelerometerPermission.collectAsState()
    val hasLocationPermission by sensorViewModel.hasLocationPermission.collectAsState()
    val errorMessage by sensorViewModel.errorMessage.collectAsState()
    
    // Permission launchers
    val accelerometerPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        sensorViewModel.updateAccelerometerPermission(isGranted)
        if (isGranted) {
            sensorViewModel.startAccelerometer()
        } else {
            Toast.makeText(context, "Accelerometer permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        val hasPermission = fineLocationGranted || coarseLocationGranted
        sensorViewModel.updateLocationPermission(hasPermission)
        
        if (hasPermission) {
            sensorViewModel.startLocationUpdates()
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Check permissions on composition
    LaunchedEffect(Unit) {
        val accelPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val locationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        sensorViewModel.updateAccelerometerPermission(accelPermission)
        sensorViewModel.updateLocationPermission(locationPermission)
        
        if (accelPermission) {
            sensorViewModel.startAccelerometer()
        }
        if (locationPermission) {
            sensorViewModel.startLocationUpdates()
        }
    }
    
    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            sensorViewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensors + MVVM") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Accelerometer Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📱 Accelerometer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (!hasAccelerometerPermission) {
                            Button(
                                onClick = {
                                    accelerometerPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            ) {
                                Text("Request Permission")
                            }
                        }
                    }
                    
                    Divider()
                    
                    Text(
                        text = "Real-time Acceleration Data:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Display accelerometer values
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SensorValueRow("X-axis:", "${accelerometerData.x.format(3)} m/s²", Color.Red)
                        SensorValueRow("Y-axis:", "${accelerometerData.y.format(3)} m/s²", Color.Green)
                        SensorValueRow("Z-axis:", "${accelerometerData.z.format(3)} m/s²", Color.Blue)
                    }
                    
                    Text(
                        text = "Last updated: ${accelerometerData.timestamp.formatTimestamp()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Location Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 Location",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (!hasLocationPermission) {
                            Button(
                                onClick = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            ) {
                                Text("Request Permission")
                            }
                        }
                    }
                    
                    Divider()
                    
                    Text(
                        text = "GPS Coordinates:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Display location values
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SensorValueRow("Latitude:", "${locationData.latitude.format(6)}°", Color.Blue)
                        SensorValueRow("Longitude:", "${locationData.longitude.format(6)}°", Color.Blue)
                        SensorValueRow("Accuracy:", "${locationData.accuracy.format(1)}m", Color.Red)
                    }
                    
                    Text(
                        text = "Last updated: ${locationData.timestamp.formatTimestamp()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (hasAccelerometerPermission) {
                            sensorViewModel.startAccelerometer()
                        } else {
                            accelerometerPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Sensors")
                }
                
                Button(
                    onClick = {
                        sensorViewModel.stopAccelerometer()
                        sensorViewModel.stopLocationUpdates()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Stop Sensors")
                }
            }
            
            // Back button
            Button(
                onClick = {
                    val intent = Intent(context, MenuActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Back to Menu")
            }
        }
    }
}

@Composable
fun SensorValueRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// Extension functions for formatting
fun Float.format(digits: Int): String = "%.${digits}f".format(this)
fun Double.format(digits: Int): String = "%.${digits}f".format(this)
fun Long.formatTimestamp(): String {
    val date = java.util.Date(this)
    val format = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
    return format.format(date)
}

package com.example.lablearnandroid

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccelerometerData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestamp: Long = 0L
)

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = 0L
)

class SensorViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    
    private val context = getApplication<Application>()
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // StateFlow for accelerometer data
    private val _accelerometerData = MutableStateFlow(AccelerometerData())
    val accelerometerData: StateFlow<AccelerometerData> = _accelerometerData.asStateFlow()
    
    // StateFlow for location data
    private val _locationData = MutableStateFlow(LocationData())
    val locationData: StateFlow<LocationData> = _locationData.asStateFlow()
    
    // Permission states
    private val _hasAccelerometerPermission = MutableStateFlow(false)
    val hasAccelerometerPermission: StateFlow<Boolean> = _hasAccelerometerPermission.asStateFlow()
    
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()
    
    // Error states
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            viewModelScope.launch {
                _locationData.value = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            viewModelScope.launch {
                _errorMessage.value = "Location provider disabled: $provider"
            }
        }
    }
    
    init {
        // Check if accelerometer is available
        if (accelerometer == null) {
            _errorMessage.value = "Accelerometer not available on this device"
        }
    }
    
    fun startAccelerometer() {
        if (accelerometer != null) {
            val success = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            if (!success) {
                _errorMessage.value = "Failed to register accelerometer listener"
            }
        }
    }
    
    fun stopAccelerometer() {
        sensorManager.unregisterListener(this)
    }
    
    fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000L, // 1 second
                            1f,    // 1 meter
                            locationListener
                        )
                    }
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            1000L,
                            1f,
                            locationListener
                        )
                    }
                    else -> {
                        _errorMessage.value = "No location provider available"
                    }
                }
            } catch (e: SecurityException) {
                _errorMessage.value = "Location permission not granted"
            } catch (e: Exception) {
                _errorMessage.value = "Error starting location updates: ${e.message}"
            }
        }
    }
    
    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }
    
    fun updateAccelerometerPermission(hasPermission: Boolean) {
        _hasAccelerometerPermission.value = hasPermission
    }
    
    fun updateLocationPermission(hasPermission: Boolean) {
        _hasLocationPermission.value = hasPermission
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                _accelerometerData.value = AccelerometerData(
                    x = it.values[0],
                    y = it.values[1],
                    z = it.values[2],
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAccelerometer()
        stopLocationUpdates()
    }
}

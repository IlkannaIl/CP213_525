package com.example.lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing WebView URL state
 * Uses MutableStateFlow for reactive URL updates
 */
class WebViewModel : ViewModel() {
    
    // MutableStateFlow for current URL with default Google URL
    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()
    
    // Loading state for WebView
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Updates the current URL with proper formatting
     * Handles URL validation and adds https:// if missing
     */
    fun updateUrl(newUrl: String) {
        viewModelScope.launch {
            val formattedUrl = formatUrl(newUrl)
            _currentUrl.value = formattedUrl
        }
    }
    
    /**
     * Sets loading state for WebView operations
     */
    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
    
    /**
     * Formats URL to ensure proper protocol
     * Adds https:// if missing, validates basic URL structure
     */
    private fun formatUrl(url: String): String {
        val trimmedUrl = url.trim()
        
        // If URL is empty, return default
        if (trimmedUrl.isEmpty()) {
            return "https://www.google.com"
        }
        
        // If URL already has protocol, return as-is
        if (trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://")) {
            return trimmedUrl
        }
        
        // If URL looks like a domain (contains dots), add https://
        if (trimmedUrl.contains(".") && !trimmedUrl.contains(" ")) {
            return "https://$trimmedUrl"
        }
        
        // Otherwise, treat as search query
        return "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmedUrl, "UTF-8")}"
    }
    
    /**
     * Gets the current URL value synchronously
     */
    fun getCurrentUrl(): String = _currentUrl.value
}

/**
 * Part6Activity demonstrating Android View Interoperability with WebView in Compose
 */
class Part6Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebViewScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    webViewModel: WebViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Collect reactive state from ViewModel
    val currentUrl by webViewModel.currentUrl.collectAsStateWithLifecycle()
    val isLoading by webViewModel.isLoading.collectAsStateWithLifecycle()
    
    // Local state for URL input field
    var urlInput by remember { mutableStateOf(currentUrl) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Scaffold(
        topBar = {
            // URL input bar with TextField and Go Button
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "WebView Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // URL TextField
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Enter URL or search") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Web,
                                    contentDescription = "Web"
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    webViewModel.updateUrl(urlInput)
                                    keyboardController?.hide()
                                }
                            ),
                            singleLine = true
                        )
                        
                        // Go Button
                        Button(
                            onClick = {
                                webViewModel.updateUrl(urlInput)
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Go",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Go")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // Main content with WebView
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AndroidView composable for WebView integration
            AndroidView(
                factory = { context ->
                    // FACTORY BLOCK: Called once to create the WebView
                    android.webkit.WebView(context).apply {
                        // Configure WebView settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false // Hide zoom controls
                        
                        // Set WebViewClient to handle navigation within app
                        webViewClient = android.webkit.WebViewClient(
                            shouldOverrideUrlLoading = { view, request ->
                                // Load URL within the WebView instead of external browser
                                view.loadUrl(request.url.toString())
                                true
                            },
                            onPageStarted = { _, _, _ ->
                                webViewModel.setLoading(true)
                            },
                            onPageFinished = { _, _ ->
                                webViewModel.setLoading(false)
                            }
                        )
                        
                        // Load initial URL
                        loadUrl(webViewModel.getCurrentUrl())
                    }
                },
                update = { webView ->
                    // UPDATE BLOCK: Called on recomposition when state changes
                    // This is where we sync WebView with ViewModel state
                    val newUrl = webViewModel.getCurrentUrl()
                    if (webView.url != newUrl) {
                        webView.loadUrl(newUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading indicator overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Information card explaining AndroidView integration
 */
@Composable
fun AndroidViewInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AndroidView Interoperability",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "This demo shows how to integrate legacy Android Views (WebView) into Jetpack Compose:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "1. Factory Block: Initialize WebView once\n2. Update Block: Sync with Compose state\n3. StateFlow: Reactive data synchronization",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.example.lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing a contact
 */
data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String
)

/**
 * ViewModel managing contact list with pagination
 * Uses MutableStateFlow to manage list state and loading status
 */
class ContactViewModel : ViewModel() {
    
    // Private mutable state flows
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    
    // Public immutable state flows
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Current page for pagination
    private var currentPage = 0
    
    init {
        // Load initial contacts
        loadMoreContacts()
    }
    
    /**
     * Simulates network delay and loads more contacts
     * Uses delay(2000) to simulate network request
     */
    fun loadMoreContacts() {
        if (_isLoading.value) return // Prevent multiple simultaneous loads
        
        viewModelScope.launch {
            _isLoading.value = true
            
            // Simulate network delay
            delay(2000)
            
            // Generate mock contacts for current page
            val newContacts = generateMockContacts(currentPage)
            
            // Append new contacts to existing list
            val currentContacts = _contacts.value.toMutableList()
            currentContacts.addAll(newContacts)
            _contacts.value = currentContacts
            
            currentPage++
            _isLoading.value = false
        }
    }
    
    /**
     * Generates mock contact data grouped alphabetically
     */
    private fun generateMockContacts(page: Int): List<Contact> {
        val allNames = listOf(
            "Alice Johnson", "Alice Smith", "Bob Anderson", "Bob Wilson", "Bob Taylor",
            "Charlie Brown", "Charlie Davis", "Charlie Miller", "Diana Evans", "Diana Garcia",
            "David Harris", "David Martinez", "Emma Clark", "Emma Rodriguez", "Emma Lewis",
            "Frank Walker", "Frank Hall", "Grace King", "Grace Wright", "Grace Lopez",
            "Henry Hill", "Henry Scott", "Ivy Green", "Ivy Adams", "Ivy Baker",
            "Jack Nelson", "Jack Carter", "Julia Mitchell", "Julia Perez", "Julia Roberts",
            "Kevin Turner", "Kevin Phillips", "Linda Campbell", "Linda Parker", "Linda Evans",
            "Michael Edwards", "Michael Collins", "Nancy Stewart", "Nancy Sanchez", "Nancy Morris",
            "Oliver Rogers", "Oliver Reed", "Patricia Cook", "Patricia Morgan", "Patricia Bell",
            "Quinn Bailey", "Quinn Cooper", "Rachel Rivera", "Rachel Peterson", "Rachel Gray",
            "Samuel Powell", "Samuel Jenkins", "Tina Howard", "Tina Ward", "Tina Cox",
            "Uma Foster", "Uma Barnes", "Victor Griffin", "Victor Ross", "Victor Henderson",
            "Wendy Perry", "Wendy Coleman", "Xavier Price", "Xavier Peterson", "Xavier Long",
            "Yvonne Bennett", "Yvonne Wood", "Zachary Murphy", "Zachary Barnes", "Zachary Ross"
        )
        
        // Calculate slice based on page
        val pageSize = 10
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, allNames.size)
        
        return allNames
            .subList(startIndex, endIndex)
            .sorted() // Sort alphabetically
            .mapIndexed { index, name ->
                Contact(
                    id = startIndex + index,
                    name = name,
                    phone = "+1-${(1000000 + startIndex + index)}",
                    email = "${name.lowercase().replace(" ", ".")}@example.com"
                )
            }
    }
    
    /**
     * Groups contacts by first letter for sticky headers
     */
    fun getContactsGroupedByFirstLetter(): Map<String, List<Contact>> {
        return _contacts.value
            .sortedBy { it.name }
            .groupBy { it.name.first().uppercase() }
    }
}

/**
 * Part2Activity demonstrating advanced LazyColumn with sticky headers and pagination
 */
class Part2Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactListScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    contactViewModel: ContactViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Collect state flows
    val contacts by contactViewModel.contacts.collectAsStateWithLifecycle()
    val isLoading by contactViewModel.isLoading.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    
    // Group contacts by first letter for sticky headers
    val groupedContacts = remember(contacts) {
        contactViewModel.getContactsGroupedByFirstLetter()
    }
    
    // Detect when user reaches near end of list for pagination
    LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.layoutInfo.visibleItemsInfo.size) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo.size
        val totalItems = contacts.size
        
        // Trigger pagination when user is near the end (last 3 items)
        if (visibleItems > 0 && totalItems > 0) {
            val lastVisibleIndex = lazyListState.firstVisibleItemIndex + visibleItems - 1
            if (lastVisibleIndex >= totalItems - 3 && !isLoading) {
                contactViewModel.loadMoreContacts()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Contact List",
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
                .padding(horizontal = 16.dp)
        ) {
            // Contact count and loading status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${contacts.size} Contacts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            // LazyColumn with sticky headers
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groupedContacts.forEach { (firstLetter, contactsInGroup) ->
                    // Sticky header for each letter group
                    item(
                        key = "header_$firstLetter",
                        contentType = "header"
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = firstLetter,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Contacts in this group
                    items(
                        items = contactsInGroup,
                        key = { it.id },
                        contentType = { "contact" }
                    ) { contact ->
                        ContactItem(contact = contact)
                    }
                }
                
                // Loading indicator at bottom when paginating
                if (isLoading && contacts.isNotEmpty()) {
                    item(
                        key = "loading",
                        contentType = "loading"
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual contact item component
 */
@Composable
fun ContactItem(contact: Contact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateItemPlacement(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = contact.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = contact.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.example.lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Part8Activity demonstrating Adaptive Profile Screen using BoxWithConstraints
 * Shows responsive design that adapts to different screen sizes and orientations
 */
class Part8Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveProfileScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveProfileScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Adaptive Profile",
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Detect screen width and choose layout accordingly
            val isCompact = maxWidth < 600.dp
            
            // Responsive layout based on screen size
            if (isCompact) {
                // Compact Layout: Mobile/Portrait - Vertical stacking
                CompactProfileLayout()
            } else {
                // Expanded Layout: Tablet/Landscape - Side-by-side
                ExpandedProfileLayout()
            }
        }
    }
}

/**
 * Compact layout for mobile/portrait screens (< 600dp)
 * Profile picture and info stacked vertically
 */
@Composable
fun CompactProfileLayout() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile Picture Section
        ProfilePictureSection(
            modifier = Modifier.size(120.dp),
            isCompact = true
        )
        
        // Personal Info Section
        PersonalInfoSection(
            modifier = Modifier.fillMaxWidth(),
            isCompact = true
        )
        
        // Stats Section
        StatsSection(
            modifier = Modifier.fillMaxWidth(),
            isCompact = true
        )
        
        // Screen size indicator
        ScreenSizeIndicator(isCompact = true)
    }
}

/**
 * Expanded layout for tablet/landscape screens (>= 600dp)
 * Profile picture and info side-by-side with weight distribution
 */
@Composable
fun ExpandedProfileLayout() {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile Picture Section - Takes 1/3 of space
        ProfilePictureSection(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            isCompact = false
        )
        
        // Personal Info Section - Takes 2/3 of space
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PersonalInfoSection(
                modifier = Modifier.fillMaxWidth(),
                isCompact = false
            )
            
            StatsSection(
                modifier = Modifier.fillMaxWidth(),
                isCompact = false
            )
            
            ScreenSizeIndicator(isCompact = false)
        }
    }
}

/**
 * Profile picture component with gray background and icon
 */
@Composable
fun ProfilePictureSection(
    modifier: Modifier,
    isCompact: Boolean
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = if (isCompact) CircleShape else RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(if (isCompact) 48.dp else 64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Profile Photo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Personal information section with name, bio, and details
 */
@Composable
fun PersonalInfoSection(
    modifier: Modifier,
    isCompact: Boolean
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            Text(
                text = "Alexandra Chen",
                style = if (isCompact) 
                    MaterialTheme.typography.headlineSmall 
                else 
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Title
            Text(
                text = "Senior Android Developer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Bio
            Text(
                text = "Passionate about creating beautiful and functional mobile applications. Specializing in Jetpack Compose, Material Design, and modern Android architecture.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Contact Info
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = "alexandra.chen@example.com"
                )
                
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = "+1 (555) 123-4567"
                )
                
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = "San Francisco, CA"
                )
            }
        }
    }
}

/**
 * Statistics section showing followers, following, and posts
 */
@Composable
fun StatsSection(
    modifier: Modifier,
    isCompact: Boolean
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isCompact) 
                    Arrangement.SpaceEvenly 
                else 
                    Arrangement.SpaceBetween
            ) {
                StatItem(
                    number = "1,234",
                    label = "Followers",
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    number = "567",
                    label = "Following",
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    number = "89",
                    label = "Posts",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
fun StatItem(
    number: String,
    label: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Info row with icon, label, and value
 */
@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Screen size indicator for testing purposes
 */
@Composable
fun ScreenSizeIndicator(isCompact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompact) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Layout Mode:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = if (isCompact) "Compact (Mobile)" else "Expanded (Tablet)",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCompact) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

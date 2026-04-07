package com.example.lablearnandroid

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * State-Driven "Like Button" with Jetpack Compose Animations
 * 
 * This composable demonstrates three different animation APIs triggered by state changes:
 * 1. Scale Animation using animateFloatAsState with Spring spec
 * 2. Color Animation using animateColorAsState 
 * 3. Content Animation using AnimatedVisibility
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedLikeButton(
    modifier: Modifier = Modifier
) {
    // Local state management - no ViewModel needed as requested
    var isLiked by remember { mutableStateOf(false) }
    
    // SCALE ANIMATION: animateFloatAsState with Spring spec
    // This creates a bouncy "pop" effect when the button is clicked
    val scale: Float by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1.0f, // Scale up when liked, normal when not liked
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Controls the bounciness
            stiffness = Spring.StiffnessLow // Controls the speed of the animation
        ),
        label = "scale_animation" // Label for debugging
    )
    
    // COLOR ANIMATION: animateColorAsState
    // Smoothly transitions between gray (not liked) and pink (liked) states
    val containerColor: Color by animateColorAsState(
        targetValue = if (isLiked) 
            Color(0xFFFF69B4) // Vibrant pink for liked state
        else 
            MaterialTheme.colorScheme.surfaceVariant, // Gray for not liked state
        animationSpec = tween(
            durationMillis = 300, // Duration of color transition
            easing = EaseInOutCubic // Smooth easing function
        ),
        label = "color_animation"
    )
    
    // Content color animation for better contrast
    val contentColor: Color by animateColorAsState(
        targetValue = if (isLiked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "content_color_animation"
    )
    
    // Main button click handler
    Button(
        onClick = { 
            isLiked = !isLiked // Toggle the liked state
        },
        modifier = modifier
            .scale(scale) // Apply the scale animation
            .defaultMinSize(minWidth = 120.dp, minHeight = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, // Apply animated container color
            contentColor = contentColor // Apply animated content color
        ),
        shape = RoundedCornerShape(24.dp) // Rounded pill shape
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // CONTENT ANIMATION: AnimatedVisibility
            // The heart icon appears/disappears with fade and expand animations
            AnimatedVisibility(
                visible = isLiked, // Only show when liked
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = LinearEasing
                    )
                ) + expandVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = EaseOutBounce
                    ),
                    expandFrom = Alignment.CenterVertically
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = LinearEasing
                    )
                ) + shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = EaseInCubic
                    ),
                    shrinkTowards = Alignment.CenterVertically
                )
            ) {
                // Heart icon that appears when liked
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Liked",
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Spacer between icon and text (only when icon is visible)
            if (isLiked) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // Text that's always visible
            Text(
                text = if (isLiked) "Liked" else "Like",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Demo screen to showcase the AnimatedLikeButton
 * This can be used to test the implementation immediately
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikeButtonDemoScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Like Button Animation Demo") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = "Click the button to see the animations!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // The main animated like button
            AnimatedLikeButton()
            
            // Additional information
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Animation Features:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1. Scale: Bouncy pop effect with Spring spec",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Color: Smooth transition between gray and pink",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Content: Heart icon fades in/out with expand/shrink",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

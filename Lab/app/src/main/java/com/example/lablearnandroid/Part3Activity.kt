package com.example.lablearnandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Part3Activity demonstrating custom Animated Donut Chart using Compose Canvas
 */
class Part3Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DonutChartDemoScreen()
        }
    }
}

/**
 * Custom Animated Donut Chart Composable
 * 
 * @param proportions List of proportions (percentages) for each segment
 * @param colors List of colors corresponding to each proportion
 * @param modifier Modifier for the chart
 * @param strokeWidth Width of the donut stroke (thickness)
 */
@Composable
fun AnimatedDonutChart(
    proportions: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 50f,
    surfaceColor: Color = MaterialTheme.colorScheme.surface
) {
    // Validate input
    require(proportions.size == colors.size) {
        "Proportions and colors must have the same size"
    }
    require(proportions.isNotEmpty()) {
        "Proportions list cannot be empty"
    }
    
    // Normalize proportions to sum to 100
    val total = proportions.sum()
    val normalizedProportions = proportions.map { it / total * 100f }
    
    // Sweep animation: animates from 0 to 1 (0% to 100% completion)
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 2000, // 2 seconds animation
            easing = EaseOutCubic // Smooth easing
        ),
        label = "sweep_animation"
    )
    
    // Canvas for drawing the donut chart
    Canvas(
        modifier = modifier
            .size(250.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(125.dp)
            )
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(canvasWidth / 2, canvasHeight / 2)
        val radius = min(canvasWidth, canvasHeight) / 2 - strokeWidth / 2
        
        // Calculate cumulative angles for each segment
        var currentAngle = -90f // Start from top (12 o'clock position)
        
        normalizedProportions.forEachIndexed { index, proportion ->
            val sweepAngle = proportion / 100f * 360f * sweepProgress
            
            // Draw arc segment
            drawArc(
                color = colors[index],
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                useCenter = false, // Don't fill to center (creates donut effect)
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                style = Stroke(
                    width = strokeWidth,
                    miter = Stroke.DefaultMiter
                )
            )
            
            currentAngle += sweepAngle
        }
        
        // Draw center circle to enhance donut effect
        drawCircle(
            color = surfaceColor,
            radius = radius - strokeWidth / 2,
            center = center
        )
    }
}

/**
 * Demo screen showcasing the Animated Donut Chart
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonutChartDemoScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Animated Donut Chart",
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = "Custom Canvas Donut Chart",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Demo data 1: Sales distribution
            DonutChartSection(
                title = "Sales Distribution",
                proportions = listOf(35f, 25f, 20f, 20f),
                colors = listOf(
                    Color(0xFF4CAF50), // Green
                    Color(0xFF2196F3), // Blue
                    Color(0xFFFF9800), // Orange
                    Color(0xFFE91E63)  // Pink
                ),
                labels = listOf("Product A", "Product B", "Product C", "Product D")
            )
            
            // Demo data 2: Market share
            DonutChartSection(
                title = "Market Share",
                proportions = listOf(40f, 30f, 20f, 10f),
                colors = listOf(
                    Color(0xFF9C27B0), // Purple
                    Color(0xFF673AB7), // Deep Purple
                    Color(0xFF3F51B5), // Indigo
                    Color(0xFF009688)  // Teal
                ),
                labels = listOf("Company X", "Company Y", "Company Z", "Others")
            )
            
            // Demo data 3: Budget allocation
            DonutChartSection(
                title = "Budget Allocation",
                proportions = listOf(45f, 30f, 15f, 10f),
                colors = listOf(
                    Color(0xFFFF5722), // Deep Orange
                    Color(0xFF795548), // Brown
                    Color(0xFF607D8B), // Blue Grey
                    Color(0xFF9E9E9E)  // Grey
                ),
                labels = listOf("Development", "Marketing", "Operations", "Other")
            )
        }
    }
}

/**
 * Section containing a donut chart with legend
 */
@Composable
fun DonutChartSection(
    title: String,
    proportions: List<Float>,
    colors: List<Color>,
    labels: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut chart
                AnimatedDonutChart(
                    proportions = proportions,
                    colors = colors,
                    strokeWidth = 40f
                )
                
                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    proportions.forEachIndexed { index, proportion ->
                        LegendItem(
                            color = colors[index],
                            label = labels[index],
                            percentage = proportion
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual legend item
 */
@Composable
fun LegendItem(
    color: Color,
    label: String,
    percentage: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color indicator
        Canvas(
            modifier = Modifier.size(16.dp)
        ) {
            drawCircle(color = color)
        }
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

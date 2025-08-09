package com.example.myapplication.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun Sensor3DBlock(
    modifier: Modifier = Modifier,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    showAxes: Boolean = true,
    showValues: Boolean = true,
    blockSize: Float = 100f,
    isAnimated: Boolean = false
) {
    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = if (isAnimated) tween(500) else snap(),
        label = "rotationX"
    )
    
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = if (isAnimated) tween(500) else snap(),
        label = "rotationY"
    )
    
    val animatedRotationZ by animateFloatAsState(
        targetValue = rotationZ,
        animationSpec = if (isAnimated) tween(500) else snap(),
        label = "rotationZ"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3D Block Canvas
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .rotate(animatedRotationZ)
            ) {
                // Apply 3D transformations
                rotate(animatedRotationX, Offset(size.width / 2f, size.height / 2f)) {
                    rotate(animatedRotationY, Offset(size.width / 2f, size.height / 2f)) {
                        // 3D Cube drawing
                        draw3DCube(blockSize)
                        
                        if (showAxes) {
                            drawAxes(blockSize)
                        }
                    }
                }
            }
        }

        if (showValues) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sensor Values Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorValueCard(
                    label = "X軸",
                    value = rotationX,
                    unit = "°",
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                SensorValueCard(
                    label = "Y軸",
                    value = rotationY,
                    unit = "°",
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                SensorValueCard(
                    label = "Z軸",
                    value = rotationZ,
                    unit = "°",
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SensorValueCard(
    label: String,
    value: Float,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${String.format("%.1f", value)}$unit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

private fun DrawScope.draw3DCube(size: Float) {
    val halfSize = size / 2f
    
    // 3D Cube vertices (simplified to 2D projection)
    val vertices = listOf(
        Offset(-halfSize, -halfSize),
        Offset(halfSize, -halfSize),
        Offset(halfSize, halfSize),
        Offset(-halfSize, halfSize)
    )
    
    // Draw cube faces
    val faces = listOf(
        listOf(0, 1, 2, 3) // Front face
    )
    
    val faceColors = listOf(
        Color(0xFF2196F3) // Blue
    )
    
    faces.forEachIndexed { index, face ->
        val path = Path().apply {
            moveTo(vertices[face[0]].x, vertices[face[0]].y)
            face.forEach { vertexIndex ->
                lineTo(vertices[vertexIndex].x, vertices[vertexIndex].y)
            }
            close()
        }
        
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    faceColors[index].copy(alpha = 0.8f),
                    faceColors[index].copy(alpha = 0.6f)
                )
            )
        )
        
        // Draw edges
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.8f),
                    Color.Transparent
                )
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}

private fun DrawScope.drawAxes(size: Float) {
    val center = Offset(size / 2f, size / 2f)
    val axisLength = size * 0.4f
    
    // X-axis (Red)
    drawLine(
        color = Color.Red,
        start = center,
        end = Offset(center.x + axisLength, center.y),
        strokeWidth = 3f
    )
    
    // Y-axis (Green)
    drawLine(
        color = Color.Green,
        start = center,
        end = Offset(center.x, center.y - axisLength),
        strokeWidth = 3f
    )
    
    // Z-axis (Blue) - simplified as diagonal line
    drawLine(
        color = Color.Blue,
        start = center,
        end = Offset(center.x + axisLength * 0.7f, center.y - axisLength * 0.7f),
        strokeWidth = 3f
    )
    
    // Axis labels
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            "X",
            center.x + axisLength + 10f,
            center.y + 5f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.RED
                textSize = 16f
                isFakeBoldText = true
            }
        )
        
        drawText(
            "Y",
            center.x + 5f,
            center.y - axisLength - 10f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.GREEN
                textSize = 16f
                isFakeBoldText = true
            }
        )
        
        drawText(
            "Z",
            center.x + axisLength * 0.7f + 10f,
            center.y - axisLength * 0.7f - 10f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLUE
                textSize = 16f
                isFakeBoldText = true
            }
        )
    }
}

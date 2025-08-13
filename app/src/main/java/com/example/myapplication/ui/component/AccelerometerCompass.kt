package com.example.myapplication.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.service.SensorMonitor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.*

@Composable
fun AccelerometerCompass(
    modifier: Modifier = Modifier,
    accelerationX: Float = 0f,
    accelerationY: Float = 0f,
    accelerationZ: Float = 0f,
    showCompass: Boolean = true,
    showValues: Boolean = true,
    showMagnitude: Boolean = true,
    compassSize: Float = 200f,
    maxAcceleration: Float = 20f, // m/s²
    isAnimated: Boolean = true,
    useSensorData: Boolean = true
) {
    // Inject SensorMonitor if using sensor data
    val sensorMonitor = remember { 
        if (useSensorData) {
            try {
                object : KoinComponent {
                    val monitor: SensorMonitor by inject()
                }.monitor
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    // Start sensor monitoring when component is created
    LaunchedEffect(sensorMonitor) {
        sensorMonitor?.let { monitor ->
            try {
                if (!monitor.isMonitoring) {
                    monitor.startMonitoring()
                }
            } catch (e: Exception) {
                // Handle any errors during monitoring start
            }
        }
    }
    
    // Get sensor data directly with error handling
    val sensorData by sensorMonitor?.sensorData?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    
    // Use sensor data if available, otherwise use provided values
    val actualAccelX = if (useSensorData && sensorData != null) sensorData!!.accelerometer.x else accelerationX
    val actualAccelY = if (useSensorData && sensorData != null) sensorData!!.accelerometer.y else accelerationY
    val actualAccelZ = if (useSensorData && sensorData != null) sensorData!!.accelerometer.z else accelerationZ
    
    // Calculate heading from magnetometer data
    val heading = remember(sensorData) {
        if (sensorData != null) {
            calculateHeadingFromMagnetometer(
                sensorData!!.magnetometer.x,
                sensorData!!.magnetometer.y,
                sensorData!!.magnetometer.z,
                sensorData!!.accelerometer.x,
                sensorData!!.accelerometer.y,
                sensorData!!.accelerometer.z
            )
        } else 0f
    }
    
    val animatedAccelX by animateFloatAsState(
        targetValue = actualAccelX,
        animationSpec = if (isAnimated) tween(300) else snap(),
        label = "accelerationX"
    )
    
    val animatedAccelY by animateFloatAsState(
        targetValue = actualAccelY,
        animationSpec = if (isAnimated) tween(300) else snap(),
        label = "accelerationY"
    )
    
    val animatedAccelZ by animateFloatAsState(
        targetValue = actualAccelZ,
        animationSpec = if (isAnimated) tween(300) else snap(),
        label = "accelerationZ"
    )
    
    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = if (isAnimated) tween(500) else snap(),
        label = "heading"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compass Display
        if (showCompass) {
            Box(
                modifier = Modifier
                    .size(compassSize.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size((compassSize - 20).dp)
                        .clip(CircleShape)
                ) {
                    drawCompass(
                        accelerationX = animatedAccelX,
                        accelerationY = animatedAccelY,
                        accelerationZ = animatedAccelZ,
                        heading = animatedHeading,
                        maxAcceleration = maxAcceleration,
                        size = size
                    )
                }
            }
        }

        if (showValues) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Acceleration Values Display
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // X, Y, Z values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AccelerationValueCard(
                        label = "X軸",
                        value = actualAccelX,
                        unit = "m/s²",
                        color = Color.Red,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    AccelerationValueCard(
                        label = "Y軸",
                        value = actualAccelY,
                        unit = "m/s²",
                        color = Color.Green,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    AccelerationValueCard(
                        label = "Z軸",
                        value = actualAccelZ,
                        unit = "m/s²",
                        color = Color.Blue,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Heading display
                Spacer(modifier = Modifier.height(12.dp))
                HeadingCard(
                    heading = heading,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showMagnitude) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Magnitude display
                    val magnitude = sqrt(actualAccelX * actualAccelX + actualAccelY * actualAccelY + actualAccelZ * actualAccelZ)
                    MagnitudeCard(
                        magnitude = magnitude,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Tilt information
                sensorData?.let { data ->
                    Spacer(modifier = Modifier.height(12.dp))
                    TiltInfoCard(
                        accelerometer = data.accelerometer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// 地磁気センサーと加速度センサーから方位を計算する関数
private fun calculateHeadingFromMagnetometer(
    magX: Float, magY: Float, magZ: Float,
    accelX: Float, accelY: Float, accelZ: Float
): Float {
    // 重力ベクトルの大きさを計算
    val gravityMagnitude = sqrt((accelX * accelX + accelY * accelY + accelZ * accelZ).toDouble()).toFloat()
    
    if (gravityMagnitude < 0.1f) {
        return 0f
    }
    
    // 正規化された重力ベクトル
    val normalizedAccelX = accelX / gravityMagnitude
    val normalizedAccelY = accelY / gravityMagnitude
    val normalizedAccelZ = accelZ / gravityMagnitude
    
    // ピッチ角とロール角を計算
    val pitch = asin(-normalizedAccelX.toDouble()).toFloat()
    val roll = atan2(normalizedAccelY.toDouble(), normalizedAccelZ.toDouble()).toFloat()
    
    // 地磁気ベクトルを回転行列で補正
    val cosPitch = cos(pitch.toDouble()).toFloat()
    val sinPitch = sin(pitch.toDouble()).toFloat()
    val cosRoll = cos(roll.toDouble()).toFloat()
    val sinRoll = sin(roll.toDouble()).toFloat()
    
    // 地磁気ベクトルを水平面に投影
    val magXCorrected = magX * cosPitch + magZ * sinPitch
    val magYCorrected = magX * sinRoll * sinPitch + magY * cosRoll - magZ * sinRoll * cosPitch
    
    // 方位角を計算（北を0度として時計回り）
    val heading = atan2(magYCorrected.toDouble(), magXCorrected.toDouble()).toFloat()
    
    // 度単位に変換して0-360度の範囲に正規化
    val headingDegrees = (heading * 180f / PI.toFloat() + 360f) % 360f
    
    return headingDegrees
}

@Composable
private fun AccelerationValueCard(
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
                text = "${String.format("%.2f", value)}$unit",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun HeadingCard(
    heading: Float,
    modifier: Modifier = Modifier
) {
    val direction = when {
        heading in 337.5f..360f || heading in 0f..22.5f -> "北"
        heading in 22.5f..67.5f -> "北東"
        heading in 67.5f..112.5f -> "東"
        heading in 112.5f..157.5f -> "南東"
        heading in 157.5f..202.5f -> "南"
        heading in 202.5f..247.5f -> "南西"
        heading in 247.5f..292.5f -> "西"
        heading in 292.5f..337.5f -> "北西"
        else -> "不明"
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "方位",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = direction,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${String.format("%.1f", heading)}°",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MagnitudeCard(
    magnitude: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "合成加速度",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "${String.format("%.2f", magnitude)} m/s²",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TiltInfoCard(
    accelerometer: com.example.myapplication.domain.model.Vector3D,
    modifier: Modifier = Modifier
) {
    // 重力ベクトルから傾き角度を計算
    val gravityMagnitude = sqrt(
        (accelerometer.x * accelerometer.x +
        accelerometer.y * accelerometer.y +
        accelerometer.z * accelerometer.z).toDouble()
    ).toFloat()
    
    val normalizedX = accelerometer.x / gravityMagnitude
    val normalizedY = accelerometer.y / gravityMagnitude
    val normalizedZ = accelerometer.z / gravityMagnitude
    
    val pitch = asin(-normalizedX.toDouble()).toFloat() * 180f / PI.toFloat()
    val roll = atan2(normalizedY.toDouble(), normalizedZ.toDouble()).toFloat() * 180f / PI.toFloat()
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "デバイス傾き",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "前後傾き: ${String.format("%.1f", pitch)}°",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "左右傾き: ${String.format("%.1f", roll)}°",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

private fun DrawScope.drawCompass(
    accelerationX: Float,
    accelerationY: Float,
    accelerationZ: Float,
    heading: Float,
    maxAcceleration: Float,
    size: androidx.compose.ui.geometry.Size
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) / 2f - 20f
    
    // Draw compass background
    drawCircle(
        color = Color(0xFFF5F5F5),
        radius = radius,
        center = center
    )
    
    // Draw compass circles
    for (i in 1..3) {
        val circleRadius = radius * i / 3f
        drawCircle(
            color = Color.Gray.copy(alpha = 0.3f),
            radius = circleRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }
    
    // Draw direction indicators
    drawDirectionIndicators(center, radius)
    
    // Draw heading arrow (North indicator)
    drawHeadingArrow(center, radius, heading)
    
    // Calculate acceleration magnitude and direction
    val magnitude = sqrt((accelerationX * accelerationX + accelerationY * accelerationY).toDouble()).toFloat()
    val normalizedMagnitude = (magnitude / maxAcceleration).coerceIn(0f, 1f)
    
    // Calculate direction angle (atan2 returns angle in radians)
    val angle = atan2(accelerationY.toDouble(), accelerationX.toDouble()).toFloat()
    
    // Draw acceleration vector
    if (magnitude > 0.1f) { // Only draw if there's significant acceleration
        val vectorLength = radius * 0.6f * normalizedMagnitude
        val endPoint = Offset(
            x = center.x + vectorLength * cos(angle.toDouble()).toFloat(),
            y = center.y + vectorLength * sin(angle.toDouble()).toFloat()
        )
        
        // Draw acceleration vector
        drawLine(
            color = Color.Red,
            start = center,
            end = endPoint,
            strokeWidth = 3f
        )
        
        // Draw arrow head
        drawArrowHead(endPoint, angle, Color.Red)
        
        // Draw magnitude indicator
        drawMagnitudeIndicator(center, radius, normalizedMagnitude)
    }
    
    // Draw individual axis components
    drawAxisComponents(center, radius, accelerationX, accelerationY, accelerationZ, maxAcceleration)
}

private fun DrawScope.drawDirectionIndicators(center: Offset, radius: Float) {
    val directions = listOf("N", "E", "S", "W")
    val angles = listOf(-PI/2, 0f, PI/2, PI) // North, East, South, West
    
    directions.forEachIndexed { index, direction ->
        val angle = angles[index]
        val point = Offset(
            x = center.x + radius * cos(angle.toDouble()).toFloat(),
            y = center.y + radius * sin(angle.toDouble()).toFloat()
        )
        
        // Draw direction line
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = center,
            end = point,
            strokeWidth = 1f
        )
        
        // Draw direction label
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                direction,
                point.x - 8f,
                point.y + 4f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 14f
                    isFakeBoldText = true
                }
            )
        }
    }
}

private fun DrawScope.drawHeadingArrow(center: Offset, radius: Float, heading: Float) {
    // Convert heading to radians (North is 0 degrees, clockwise)
    val headingRad = (heading - 90f) * PI.toFloat() / 180f // Adjust for canvas coordinate system
    
    val arrowLength = radius * 0.8f
    val endPoint = Offset(
        x = center.x + arrowLength * cos(headingRad.toDouble()).toFloat(),
        y = center.y + arrowLength * sin(headingRad.toDouble()).toFloat()
    )
    
    // Draw heading arrow
    drawLine(
        color = Color.Blue,
        start = center,
        end = endPoint,
        strokeWidth = 4f
    )
    
    // Draw arrow head
    drawArrowHead(endPoint, headingRad, Color.Blue)
    
    // Draw "N" label at the arrow tip
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            "N",
            endPoint.x - 8f,
            endPoint.y + 4f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLUE
                textSize = 16f
                isFakeBoldText = true
            }
        )
    }
}

private fun DrawScope.drawArrowHead(point: Offset, angle: Float, color: Color) {
    val arrowLength = 12f
    val arrowAngle = PI / 6f // 30 degrees
    
    val arrowPoint1 = Offset(
        x = point.x - arrowLength * cos((angle - arrowAngle).toDouble()).toFloat(),
        y = point.y - arrowLength * sin((angle - arrowAngle).toDouble()).toFloat()
    )
    
    val arrowPoint2 = Offset(
        x = point.x - arrowLength * cos((angle + arrowAngle).toDouble()).toFloat(),
        y = point.y - arrowLength * sin((angle + arrowAngle).toDouble()).toFloat()
    )
    
    val path = Path().apply {
        moveTo(point.x, point.y)
        lineTo(arrowPoint1.x, arrowPoint1.y)
        lineTo(arrowPoint2.x, arrowPoint2.y)
        close()
    }
    
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(color, color.copy(alpha = 0.8f))
        )
    )
}

private fun DrawScope.drawMagnitudeIndicator(center: Offset, radius: Float, normalizedMagnitude: Float) {
    val indicatorRadius = radius * 0.1f
    val indicatorColor = when {
        normalizedMagnitude < 0.3f -> Color.Green
        normalizedMagnitude < 0.7f -> Color.Yellow
        else -> Color.Red
    }
    
    drawCircle(
        color = indicatorColor,
        radius = indicatorRadius,
        center = Offset(
            x = center.x + radius * 0.8f,
            y = center.y - radius * 0.8f
        )
    )
    
    // Draw magnitude text
    drawContext.canvas.nativeCanvas.apply {
        drawText(
            "${(normalizedMagnitude * 100).toInt()}%",
            center.x + radius * 0.8f - 20f,
            center.y - radius * 0.8f + 5f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isFakeBoldText = true
            }
        )
    }
}

private fun DrawScope.drawAxisComponents(
    center: Offset,
    radius: Float,
    accelerationX: Float,
    accelerationY: Float,
    accelerationZ: Float,
    maxAcceleration: Float
) {
    val componentRadius = radius * 0.6f
    
    // X-axis component (horizontal)
    if (abs(accelerationX) > 0.1f) {
        val xLength = (accelerationX / maxAcceleration * componentRadius).coerceIn(-componentRadius, componentRadius)
        val xColor = if (accelerationX > 0) Color.Red else Color.Red.copy(alpha = 0.5f)
        
        drawLine(
            color = xColor,
            start = Offset(center.x - componentRadius, center.y),
            end = Offset(center.x + componentRadius, center.y),
            strokeWidth = 2f
        )
        
        drawLine(
            color = xColor,
            start = center,
            end = Offset(center.x + xLength, center.y),
            strokeWidth = 4f
        )
    }
    
    // Y-axis component (vertical)
    if (abs(accelerationY) > 0.1f) {
        val yLength = (accelerationY / maxAcceleration * componentRadius).coerceIn(-componentRadius, componentRadius)
        val yColor = if (accelerationY > 0) Color.Green else Color.Green.copy(alpha = 0.5f)
        
        drawLine(
            color = yColor,
            start = Offset(center.x, center.y - componentRadius),
            end = Offset(center.x, center.y + componentRadius),
            strokeWidth = 2f
        )
        
        drawLine(
            color = yColor,
            start = center,
            end = Offset(center.x, center.y + yLength),
            strokeWidth = 4f
        )
    }
    
    // Z-axis indicator (depth - shown as circle size)
    if (abs(accelerationZ) > 0.1f) {
        val zIndicatorRadius = (abs(accelerationZ) / maxAcceleration * componentRadius * 0.3f).coerceIn(5f, 20f)
        val zColor = if (accelerationZ > 0) Color.Blue else Color.Blue.copy(alpha = 0.5f)
        
        drawCircle(
            color = zColor,
            radius = zIndicatorRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }
}

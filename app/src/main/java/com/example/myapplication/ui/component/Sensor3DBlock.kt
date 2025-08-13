package com.example.myapplication.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.Vector3D
import com.example.myapplication.service.SensorMonitor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.*

/**
 * 3D立方体の頂点データ
 */
data class Vertex3D(val x: Float, val y: Float, val z: Float) {
    fun rotateX(angle: Float): Vertex3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        return Vertex3D(
            x = x,
            y = y * cosA - z * sinA,
            z = y * sinA + z * cosA
        )
    }
    
    fun rotateY(angle: Float): Vertex3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        return Vertex3D(
            x = x * cosA + z * sinA,
            y = y,
            z = -x * sinA + z * cosA
        )
    }
    
    fun rotateZ(angle: Float): Vertex3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        return Vertex3D(
            x = x * cosA - y * sinA,
            y = x * sinA + y * cosA,
            z = z
        )
    }
    
    fun project(screenWidth: Float, screenHeight: Float, distance: Float = 200f): Offset {
        val factor = distance / (distance + z)
        return Offset(
            x = x * factor + screenWidth / 2,
            y = y * factor + screenHeight / 2
        )
    }
}

/**
 * 3D立方体の面データ
 */
data class Face3D(val vertices: List<Int>, val color: Color)

// 角度の差分を計算して連続性を保つ関数
private fun calculateAngleDifference(prevAngle: Float, newAngle: Float): Float {
    var diff = newAngle - prevAngle
    
    // 角度の差分を-πからπの範囲に正規化
    while (diff > PI.toFloat()) {
        diff -= 2 * PI.toFloat()
    }
    while (diff < -PI.toFloat()) {
        diff += 2 * PI.toFloat()
    }
    
    return diff
}

// 角度を度単位で正規化する関数（0-360度の範囲）
private fun normalizeAngleToDegrees(angleRad: Float): Float {
    val angleDeg = angleRad * 180f / PI.toFloat()
    return (angleDeg + 360f) % 360f
}

// 加速度から傾き角度を計算する関数
private fun calculateTiltFromAcceleration(accelX: Float, accelY: Float, accelZ: Float): Triple<Float, Float, Float> {
    // 重力加速度の大きさ（通常9.81 m/s²）
    val gravityMagnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
    
    if (gravityMagnitude < 0.1f) {
        return Triple(0f, 0f, 0f)
    }
    
    // 正規化された重力ベクトル
    val normalizedX = accelX / gravityMagnitude
    val normalizedY = accelY / gravityMagnitude
    val normalizedZ = accelZ / gravityMagnitude
    
    // ピッチ角（X軸周りの回転）- 前後の傾き
    val pitch = asin(-normalizedX)
    
    // ロール角（Y軸周りの回転）- 左右の傾き
    val roll = atan2(normalizedY, normalizedZ)
    
    // ヨー角は加速度センサーだけでは正確に計算できない（地磁気センサーが必要）
    val yaw = 0f
    
    return Triple(pitch, roll, yaw)
}

@Composable
fun Sensor3DBlock(
    modifier: Modifier = Modifier,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    showAxes: Boolean = true,
    showValues: Boolean = true,
    blockSize: Float = 100f,
    isAnimated: Boolean = true,
    useSensorData: Boolean = true
) {
    // 立方体の頂点（中心を原点とする）
    val halfSize = 50f // 固定サイズでテスト
    val vertices = remember {
        listOf(
            Vertex3D(-halfSize, -halfSize, -halfSize), // 0: 後左下
            Vertex3D(halfSize, -halfSize, -halfSize),  // 1: 後右下
            Vertex3D(halfSize, halfSize, -halfSize),   // 2: 後右上
            Vertex3D(-halfSize, halfSize, -halfSize),  // 3: 後左上
            Vertex3D(-halfSize, -halfSize, halfSize),  // 4: 前左下
            Vertex3D(halfSize, -halfSize, halfSize),   // 5: 前右下
            Vertex3D(halfSize, halfSize, halfSize),    // 6: 前右上
            Vertex3D(-halfSize, halfSize, halfSize)    // 7: 前左上
        )
    }
    
    // 立方体の面（頂点インデックス）
    val faces = remember {
        listOf(
            Face3D(listOf(0, 1, 2, 3), Color.Red),     // 後ろ面（赤）
            Face3D(listOf(4, 5, 6, 7), Color.Green),   // 前面（緑）
            Face3D(listOf(0, 1, 5, 4), Color.Blue),    // 下面（青）
            Face3D(listOf(2, 3, 7, 6), Color.Yellow),  // 上面（黄）
            Face3D(listOf(0, 3, 7, 4), Color.Magenta), // 左面（マゼンタ）
            Face3D(listOf(1, 2, 6, 5), Color.Cyan)     // 右面（シアン）
        )
    }
    
    // 軸の頂点
    val axisLength = halfSize * 1.5f
    val axes = remember {
        listOf(
            // X軸（赤）
            Pair(Vertex3D(0f, 0f, 0f), Vertex3D(axisLength, 0f, 0f)),
            // Y軸（緑）
            Pair(Vertex3D(0f, 0f, 0f), Vertex3D(0f, axisLength, 0f)),
            // Z軸（青）
            Pair(Vertex3D(0f, 0f, 0f), Vertex3D(0f, 0f, axisLength))
        )
    }
    
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
    
    // Calculate rotation angles from sensor data with accumulation
    var accumulatedRotationX by remember { mutableStateOf(0f) }
    var accumulatedRotationY by remember { mutableStateOf(0f) }
    var accumulatedRotationZ by remember { mutableStateOf(0f) }
    
    // Manual rotation for testing
    var manualRotationX by remember { mutableStateOf(0f) }
    var manualRotationY by remember { mutableStateOf(0f) }
    var manualRotationZ by remember { mutableStateOf(0f) }
    
    // Toggle sensor data usage for testing
    var useSensorDataForTest by remember { mutableStateOf(useSensorData) }
    
    // Update accumulated rotation based on sensor data
    LaunchedEffect(sensorData) {
        sensorData?.let { data ->
            try {
                // 加速度センサーから傾き角度を計算
                val (pitchFromAccel, rollFromAccel, _) = calculateTiltFromAcceleration(
                    data.accelerometer.x,
                    data.accelerometer.y,
                    data.accelerometer.z
                )
                
                // オイラー角を使用してスマホの実際の向きを取得
                val orientation = data.orientation
                
                // オイラー角をラジアンに変換（センサーデータは度単位の可能性があるため）
                val rollRad = orientation.x * PI.toFloat() / 180f
                val pitchRad = orientation.y * PI.toFloat() / 180f
                val yawRad = orientation.z * PI.toFloat() / 180f
                
                // 加速度センサーとオイラー角を組み合わせてより正確な角度を計算
                // 加速度センサーは重力方向の検出に優れている
                val finalPitch = if (abs(pitchFromAccel) > 0.1f) pitchFromAccel else pitchRad
                val finalRoll = if (abs(rollFromAccel) > 0.1f) rollFromAccel else rollRad
                val finalYaw = yawRad // ヨー角は地磁気センサーが必要
                
                // 前回の角度を保存
                val prevX = accumulatedRotationX
                val prevY = accumulatedRotationY
                val prevZ = accumulatedRotationZ
                
                // 角度の差分を計算して連続性を保つ
                val deltaX = calculateAngleDifference(prevX, finalPitch)
                val deltaY = calculateAngleDifference(prevY, finalRoll)
                val deltaZ = calculateAngleDifference(prevZ, finalYaw)
                
                // 累積角度を更新
                accumulatedRotationX += deltaX
                accumulatedRotationY += deltaY
                accumulatedRotationZ += deltaZ
                
                // 正規化は行わない：アニメーションの連続性を保つため、角度は累積のまま保持する
            } catch (e: Exception) {
                // フォールバック: ジャイロスコープデータを使用
                val gyro = data.gyroscope
                accumulatedRotationX += gyro.x * 2f
                accumulatedRotationY += gyro.y * 2f
                accumulatedRotationZ += gyro.z * 2f
                
                // 正規化は行わない：アニメーションの連続性を保つため、角度は累積のまま保持する
            }
        }
    }
    
    // Use accumulated sensor data if available, otherwise use provided values
    val actualRotationX = if (useSensorDataForTest && sensorMonitor != null && sensorData != null) {
        accumulatedRotationX
    } else {
        manualRotationX + rotationX
    }
    val actualRotationY = if (useSensorDataForTest && sensorMonitor != null && sensorData != null) {
        accumulatedRotationY
    } else {
        manualRotationY + rotationY
    }
    val actualRotationZ = if (useSensorDataForTest && sensorMonitor != null && sensorData != null) {
        accumulatedRotationZ
    } else {
        manualRotationZ + rotationZ
    }
    
    val animatedRotationX by animateFloatAsState(
        targetValue = actualRotationX,
        animationSpec = if (isAnimated) tween(200) else androidx.compose.animation.core.snap(),
        label = "rotationX"
    )
    
    val animatedRotationY by animateFloatAsState(
        targetValue = actualRotationY,
        animationSpec = if (isAnimated) tween(200) else androidx.compose.animation.core.snap(),
        label = "rotationY"
    )
    
    val animatedRotationZ by animateFloatAsState(
        targetValue = actualRotationZ,
        animationSpec = if (isAnimated) tween(200) else androidx.compose.animation.core.snap(),
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
                .size(300.dp)
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
                modifier = Modifier.size(280.dp)
            ) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                
                // デバッグ: 背景を描画
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height)
                )
                
                // 回転した頂点を計算
                val rotatedVertices = vertices.map { vertex ->
                    vertex
                        .rotateX(animatedRotationX)
                        .rotateY(animatedRotationY)
                        .rotateZ(animatedRotationZ)
                }
                
                // 面を奥行き順にソート（Z-buffer風の処理）
                val sortedFaces = faces.mapIndexed { index, face ->
                    val centerZ = face.vertices.map { rotatedVertices[it].z }.average().toFloat()
                    Triple(index, face, centerZ)
                }.sortedByDescending { it.third }
                
                // 面を描画
                sortedFaces.forEach { (_, face, _) ->
                    val projectedVertices = face.vertices.map { rotatedVertices[it].project(size.width, size.height) }
                    
                    // 面を描画
                    val path = Path().apply {
                        moveTo(projectedVertices[0].x, projectedVertices[0].y)
                        projectedVertices.forEach { vertex ->
                            lineTo(vertex.x, vertex.y)
                        }
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        color = face.color
                    )
                    
                    // エッジを描画
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
                
                // 軸を描画
                if (showAxes) {
                    val axisColors = listOf(Color.Red, Color.Green, Color.Blue)
                    axes.forEachIndexed { index, (start, end) ->
                        val rotatedStart = start
                            .rotateX(animatedRotationX)
                            .rotateY(animatedRotationY)
                            .rotateZ(animatedRotationZ)
                        val rotatedEnd = end
                            .rotateX(animatedRotationX)
                            .rotateY(animatedRotationY)
                            .rotateZ(animatedRotationZ)
                        
                        val projectedStart = rotatedStart.project(size.width, size.height)
                        val projectedEnd = rotatedEnd.project(size.width, size.height)
                        
                        drawLine(
                            color = axisColors[index],
                            start = projectedStart,
                            end = projectedEnd,
                            strokeWidth = 3f
                        )
                    }
                }
                
                // デバッグ: 中心点を描画
                drawCircle(
                    color = Color.Yellow,
                    radius = 5f,
                    center = Offset(centerX, centerY)
                )
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
                    label = "Pitch",
                    value = normalizeAngleToDegrees(actualRotationX),
                    unit = "°",
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                SensorValueCard(
                    label = "Roll",
                    value = normalizeAngleToDegrees(actualRotationY),
                    unit = "°",
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                SensorValueCard(
                    label = "Yaw",
                    value = normalizeAngleToDegrees(actualRotationZ),
                    unit = "°",
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 加速度センサーの値を表示
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorValueCard(
                    label = "Accel X",
                    value = sensorData?.accelerometer?.x ?: 0f,
                    unit = "m/s²",
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                SensorValueCard(
                    label = "Accel Y",
                    value = sensorData?.accelerometer?.y ?: 0f,
                    unit = "m/s²",
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                SensorValueCard(
                    label = "Accel Z",
                    value = sensorData?.accelerometer?.z ?: 0f,
                    unit = "m/s²",
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 重力ベクトルの大きさを表示
            sensorData?.let { data ->
                val gravityMagnitude = sqrt(
                    data.accelerometer.x * data.accelerometer.x +
                    data.accelerometer.y * data.accelerometer.y +
                    data.accelerometer.z * data.accelerometer.z
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "重力ベクトル: ${String.format("%.2f", gravityMagnitude)} m/s²",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Cyan
                )
                
                // 加速度から計算した傾き角度を表示
                val (pitchFromAccel, rollFromAccel, _) = calculateTiltFromAcceleration(
                    data.accelerometer.x,
                    data.accelerometer.y,
                    data.accelerometer.z
                )
                
                Text(
                    text = "加速度傾き: Pitch=${String.format("%.1f", pitchFromAccel * 180f / PI.toFloat())}°, Roll=${String.format("%.1f", rollFromAccel * 180f / PI.toFloat())}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Magenta
                )
            }
            
            // Debug info
            if (useSensorData) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (sensorData != null) "センサーデータ: 有効" else "センサーデータ: 無効",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sensorData != null) Color.Green else Color.Red
                )
                
                Text(
                    text = if (sensorMonitor?.isMonitoring == true) "監視中: 有効" else "監視中: 無効",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sensorMonitor?.isMonitoring == true) Color.Green else Color.Red
                )
                
                sensorData?.let { data ->
                    // オイラー角の値を表示
                    Text(
                        text = "オイラー角: Roll=${data.orientation.x.toInt()}°, Pitch=${data.orientation.y.toInt()}°, Yaw=${data.orientation.z.toInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Cyan
                    )
                }
            }
            
            // Show current rotation values
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "現在の回転: X=${normalizeAngleToDegrees(actualRotationX).toInt()}°, Y=${normalizeAngleToDegrees(actualRotationY).toInt()}°, Z=${normalizeAngleToDegrees(actualRotationZ).toInt()}°",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Blue
            )
            
            if (!useSensorDataForTest) {
                Text(
                    text = "手動回転: X=${normalizeAngleToDegrees(manualRotationX).toInt()}°, Y=${normalizeAngleToDegrees(manualRotationY).toInt()}°, Z=${normalizeAngleToDegrees(manualRotationZ).toInt()}°",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800)
                )
            }
            
            // Manual rotation test buttons
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "手動回転テスト",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Toggle sensor data usage
            Button(
                onClick = { useSensorDataForTest = !useSensorDataForTest },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (useSensorDataForTest) Color.Green else Color.Red
                )
            ) {
                Text(if (useSensorDataForTest) "センサーデータ: 有効" else "センサーデータ: 無効")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { manualRotationX += PI.toFloat() / 12f }, // 15度
                    modifier = Modifier.weight(1f)
                ) {
                    Text("X +15°")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { manualRotationY += PI.toFloat() / 12f }, // 15度
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Y +15°")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { manualRotationZ += PI.toFloat() / 12f }, // 15度
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Z +15°")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { 
                    manualRotationX = 0f
                    manualRotationY = 0f
                    manualRotationZ = 0f
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("リセット")
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${String.format("%.1f", value)}$unit",
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
        }
    }
}

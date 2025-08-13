package com.example.myapplication.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.UserPosition3D
import kotlin.math.max
import kotlin.math.min

/**
 * 座標履歴グラフ
 */
@Composable
fun CoordinateHistoryGraph(
    positions: List<UserPosition3D>,
    modifier: Modifier = Modifier,
    maxDataPoints: Int = 100,
    showGrid: Boolean = true,
    showLegend: Boolean = true
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // タイトル
            Text(
                text = "座標履歴",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // グラフ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (positions.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCoordinateHistory(
                            positions = positions.takeLast(maxDataPoints),
                            size = size,
                            showGrid = showGrid
                        )
                    }
                } else {
                    // データがない場合の表示
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "座標データがありません",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // 凡例
            if (showLegend && positions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CoordinateLegend()
            }
            
            // 統計情報
            if (positions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CoordinateStatistics(positions = positions)
            }
        }
    }
}

/**
 * 座標履歴を描画
 */
private fun DrawScope.drawCoordinateHistory(
    positions: List<UserPosition3D>,
    size: androidx.compose.ui.geometry.Size,
    showGrid: Boolean
) {
    if (positions.size < 2) return
    
    val padding = 40f
    val graphWidth = size.width - 2 * padding
    val graphHeight = size.height - 2 * padding
    
    // データ範囲を計算
    val xRange = positions.map { it.x }
    val yRange = positions.map { it.y }
    val zRange = positions.map { it.z }
    
    val minX = xRange.minOrNull() ?: 0f
    val maxX = xRange.maxOrNull() ?: 0f
    val minY = yRange.minOrNull() ?: 0f
    val maxY = yRange.maxOrNull() ?: 0f
    val minZ = zRange.minOrNull() ?: 0f
    val maxZ = zRange.maxOrNull() ?: 0f
    
    val xScale = if (maxX > minX) graphWidth / (maxX - minX) else 1f
    val yScale = if (maxY > minY) graphHeight / (maxY - minY) else 1f
    
    // グリッドを描画
    if (showGrid) {
        drawGrid(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            padding = padding,
            graphWidth = graphWidth,
            graphHeight = graphHeight
        )
    }
    
    // 座標変換関数
    fun toScreenX(x: Float): Float = padding + (x - minX) * xScale
    fun toScreenY(y: Float): Float = size.height - padding - (y - minY) * yScale
    
    // パスを描画
    val path = Path()
    val points = positions.map { pos ->
        Offset(toScreenX(pos.x), toScreenY(pos.y))
    }
    
    // X-Y軌跡（赤線）
    path.moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { point ->
        path.lineTo(point.x, point.y)
    }
    drawPath(
        path = path,
        color = Color.Red,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
    )
    
    // データポイントを描画
    points.forEachIndexed { index, point ->
        val position = positions[index]
        val color = when {
            position.confidence >= 0.8f -> Color.Green
            position.confidence >= 0.6f -> Color.Yellow
            else -> Color.Red
        }
        
        drawCircle(
            color = color,
            radius = 4f,
            center = point
        )
    }
    
    // 最新位置を強調表示
    val latestPoint = points.last()
    drawCircle(
        color = Color.Blue,
        radius = 8f,
        center = latestPoint
    )
    
    // 軸ラベルを描画
    drawAxisLabels(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
        padding = padding,
        size = size
    )
}

/**
 * グリッドを描画
 */
private fun DrawScope.drawGrid(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    padding: Float,
    graphWidth: Float,
    graphHeight: Float
) {
    val gridColor = Color.Gray.copy(alpha = 0.3f)
    val gridStrokeWidth = 1f
    
    // 縦線（X軸）
    val xSteps = 5
    for (i in 0..xSteps) {
        val x = minX + (maxX - minX) * i / xSteps
        val screenX = padding + (x - minX) * graphWidth / (maxX - minX)
        drawLine(
            color = gridColor,
            start = Offset(screenX, padding),
            end = Offset(screenX, padding + graphHeight),
            strokeWidth = gridStrokeWidth
        )
    }
    
    // 横線（Y軸）
    val ySteps = 5
    for (i in 0..ySteps) {
        val y = minY + (maxY - minY) * i / ySteps
        val screenY = padding + graphHeight - (y - minY) * graphHeight / (maxY - minY)
        drawLine(
            color = gridColor,
            start = Offset(padding, screenY),
            end = Offset(padding + graphWidth, screenY),
            strokeWidth = gridStrokeWidth
        )
    }
}

/**
 * 軸ラベルを描画
 */
private fun DrawScope.drawAxisLabels(
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    padding: Float,
    size: androidx.compose.ui.geometry.Size
) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        
        // X軸ラベル
        drawText(
            "X軸 (m)",
            size.width / 2,
            size.height - 5f,
            paint
        )
        
        // Y軸ラベル
        save()
        rotate(-90f, size.width / 2, size.height / 2)
        drawText(
            "Y軸 (m)",
            size.width / 2,
            15f,
            paint
        )
        restore()
    }
}

/**
 * 座標凡例
 */
@Composable
private fun CoordinateLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(
            color = Color.Red,
            label = "軌跡",
            modifier = Modifier.weight(1f)
        )
        LegendItem(
            color = Color.Blue,
            label = "現在位置",
            modifier = Modifier.weight(1f)
        )
        LegendItem(
            color = Color.Green,
            label = "高信頼度",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 凡例アイテム
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * 座標統計情報
 */
@Composable
private fun CoordinateStatistics(positions: List<UserPosition3D>) {
    val validPositions = positions.filter { it.isValid() }
    if (validPositions.isEmpty()) return
    
    val totalDistance = validPositions.zipWithNext().sumOf { (prev, next) ->
        prev.distance2DTo(next).toDouble()
    }
    
    val avgAccuracy = validPositions.map { it.accuracy }.average()
    val avgConfidence = validPositions.map { it.confidence }.average()
    
    Column {
        Text(
            text = "統計情報",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                label = "総移動距離",
                value = "${totalDistance.format(2)} m"
            )
            StatItem(
                label = "平均精度",
                value = "${avgAccuracy.format(2)} m"
            )
            StatItem(
                label = "平均信頼度",
                value = "${(avgConfidence * 100).format(0)}%"
            )
        }
    }
}

/**
 * 統計アイテム
 */
@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Double値を指定された小数点以下桁数でフォーマット
 */
private fun Double.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

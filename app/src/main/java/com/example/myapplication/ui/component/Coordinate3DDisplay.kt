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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.UserPosition3D
import kotlin.math.roundToInt

/**
 * 3D座標表示コンポーネント
 */
@Composable
fun Coordinate3DDisplay(
    position: UserPosition3D,
    modifier: Modifier = Modifier,
    showAccuracy: Boolean = true,
    showFloor: Boolean = true,
    showHeading: Boolean = true,
    showVelocity: Boolean = true
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
                text = "3D座標情報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // 座標値表示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CoordinateValue(
                    label = "X",
                    value = position.x,
                    unit = "m",
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CoordinateValue(
                    label = "Y",
                    value = position.y,
                    unit = "m",
                    color = Color.Green,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CoordinateValue(
                    label = "Z",
                    value = position.z,
                    unit = "m",
                    color = Color.Blue,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 追加情報
            if (showFloor) {
                InfoRow(
                    label = "フロア",
                    value = "${position.floor}階",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (showAccuracy) {
                InfoRow(
                    label = "精度",
                    value = "${position.accuracy.format(2)} m",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (showHeading && position.heading != null) {
                InfoRow(
                    label = "方位",
                    value = "${position.heading.roundToInt()}°",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (showVelocity && position.velocity != null) {
                InfoRow(
                    label = "速度",
                    value = "${position.velocity.format(2)} m/s",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 信頼度表示
            ConfidenceIndicator(
                confidence = position.confidence,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

/**
 * 座標値表示コンポーネント
 */
@Composable
private fun CoordinateValue(
    label: String,
    value: Float,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${value.format(2)} $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 情報行コンポーネント
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 信頼度インジケーター
 */
@Composable
private fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "信頼度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "${(confidence * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = confidence,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                confidence >= 0.8f -> Color.Green
                confidence >= 0.6f -> Color.Yellow
                else -> Color.Red
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * 3D座標軸表示
 */
@Composable
fun Coordinate3DAxes(
    modifier: Modifier = Modifier,
    size: Float = 200f
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "座標軸",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Canvas(
                modifier = Modifier
                    .size(size.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                drawAxes(size = size)
            }
        }
    }
}

/**
 * 座標軸を描画
 */
private fun DrawScope.drawAxes(size: Float) {
    val center = Offset(size / 2f, size / 2f)
    val axisLength = size * 0.3f
    
    // X軸（赤）
    drawLine(
        color = Color.Red,
        start = center,
        end = Offset(center.x + axisLength, center.y),
        strokeWidth = 4f
    )
    
    // Y軸（緑）
    drawLine(
        color = Color.Green,
        start = center,
        end = Offset(center.x, center.y - axisLength),
        strokeWidth = 4f
    )
    
    // Z軸（青）- 斜め線で表現
    drawLine(
        color = Color.Blue,
        start = center,
        end = Offset(center.x + axisLength * 0.7f, center.y - axisLength * 0.7f),
        strokeWidth = 4f
    )
    
    // 軸ラベル（簡易版）
    // 実際の実装では、drawIntoCanvasを使用するか、別の方法でテキストを描画
}

/**
 * Float値を指定された小数点以下桁数でフォーマット
 */
private fun Float.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

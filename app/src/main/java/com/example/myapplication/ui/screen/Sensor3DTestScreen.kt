package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.component.Sensor3DBlock
import kotlinx.coroutines.delay
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sensor3DTestScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {}
) {
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var rotationZ by remember { mutableStateOf(0f) }
    
    var isAutoRotation by remember { mutableStateOf(false) }
    var isAnimated by remember { mutableStateOf(true) }
    var showAxes by remember { mutableStateOf(true) }
    var showValues by remember { mutableStateOf(true) }
    
    // Auto rotation effect
    LaunchedEffect(isAutoRotation) {
        while (isAutoRotation) {
            rotationX = (rotationX + 2f) % 360f
            rotationY = (rotationY + 1.5f) % 360f
            rotationZ = (rotationZ + 1f) % 360f
            delay(50) // 20 FPS
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("3Dセンサーテスト") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { isAutoRotation = !isAutoRotation }) {
                        Icon(
                            if (isAutoRotation) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoRotation) "停止" else "自動回転"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 3D Sensor Block Display
            Sensor3DBlock(
                rotationX = rotationX,
                rotationY = rotationY,
                rotationZ = rotationZ,
                showAxes = showAxes,
                showValues = showValues,
                isAnimated = isAnimated
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Control Panel
            ControlPanel(
                rotationX = rotationX,
                rotationY = rotationY,
                rotationZ = rotationZ,
                onRotationXChange = { rotationX = it },
                onRotationYChange = { rotationY = it },
                onRotationZChange = { rotationZ = it },
                isAutoRotation = isAutoRotation,
                onAutoRotationChange = { isAutoRotation = it },
                isAnimated = isAnimated,
                onAnimatedChange = { isAnimated = it },
                showAxes = showAxes,
                onShowAxesChange = { showAxes = it },
                showValues = showValues,
                onShowValuesChange = { showValues = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Preset Buttons
            PresetButtons(
                onPresetSelected = { preset ->
                    when (preset) {
                        "reset" -> {
                            rotationX = 0f
                            rotationY = 0f
                            rotationZ = 0f
                        }
                        "x_axis" -> {
                            rotationX = 45f
                            rotationY = 0f
                            rotationZ = 0f
                        }
                        "y_axis" -> {
                            rotationX = 0f
                            rotationY = 45f
                            rotationZ = 0f
                        }
                        "z_axis" -> {
                            rotationX = 0f
                            rotationY = 0f
                            rotationZ = 45f
                        }
                        "diagonal" -> {
                            rotationX = 30f
                            rotationY = 30f
                            rotationZ = 30f
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Information Panel
            InformationPanel()
        }
    }
}

@Composable
private fun ControlPanel(
    rotationX: Float,
    rotationY: Float,
    rotationZ: Float,
    onRotationXChange: (Float) -> Unit,
    onRotationYChange: (Float) -> Unit,
    onRotationZChange: (Float) -> Unit,
    isAutoRotation: Boolean,
    onAutoRotationChange: (Boolean) -> Unit,
    isAnimated: Boolean,
    onAnimatedChange: (Boolean) -> Unit,
    showAxes: Boolean,
    onShowAxesChange: (Boolean) -> Unit,
    showValues: Boolean,
    onShowValuesChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "コントロールパネル",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Rotation Sliders
            RotationSlider(
                label = "X軸回転",
                value = rotationX,
                onValueChange = onRotationXChange,
                valueRange = 0f..360f,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            RotationSlider(
                label = "Y軸回転",
                value = rotationY,
                onValueChange = onRotationYChange,
                valueRange = 0f..360f,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            RotationSlider(
                label = "Z軸回転",
                value = rotationZ,
                onValueChange = onRotationZChange,
                valueRange = 0f..360f,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Toggle Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isAutoRotation,
                            onCheckedChange = onAutoRotationChange
                        )
                        Text(
                            text = "自動回転",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isAnimated,
                            onCheckedChange = onAnimatedChange
                        )
                        Text(
                            text = "アニメーション",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = showAxes,
                            onCheckedChange = onShowAxesChange
                        )
                        Text(
                            text = "軸表示",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = showValues,
                            onCheckedChange = onShowValuesChange
                        )
                        Text(
                            text = "値表示",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RotationSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    color: androidx.compose.ui.graphics.Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${String.format("%.1f", value)}°",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PresetButtons(
    onPresetSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "プリセット",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onPresetSelected("reset") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("リセット")
                }
                
                Button(
                    onClick = { onPresetSelected("x_axis") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("X軸45°")
                }
                
                Button(
                    onClick = { onPresetSelected("y_axis") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Y軸45°")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { onPresetSelected("z_axis") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Z軸45°")
                }
                
                Button(
                    onClick = { onPresetSelected("diagonal") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("対角線")
                }
            }
        }
    }
}

@Composable
private fun InformationPanel() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "使用方法",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "• スライダーで各軸の回転角度を調整できます\n" +
                       "• 自動回転ボタンで連続的な回転をテストできます\n" +
                       "• プリセットボタンでよく使う角度に素早く設定できます\n" +
                       "• 軸表示と値表示の切り替えが可能です\n" +
                       "• アニメーション効果のオン/オフができます",
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "センサー情報",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "• X軸（赤）: ピッチ（前後傾き）\n" +
                       "• Y軸（緑）: ロール（左右傾き）\n" +
                       "• Z軸（青）: ヨー（左右回転）\n" +
                       "• 角度範囲: 0°〜360°",
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
    }
}

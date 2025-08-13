package com.example.myapplication.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.example.myapplication.presentation.viewmodel.MapViewModel
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.getKoin
import com.example.myapplication.service.SensorMonitor

/**
 * Hyper-style map screen with modern, clean UI design.
 * Features:
 * - Floating action buttons for quick access
 * - Real-time position indicator with accuracy ring
 * - Smooth animations and transitions
 * - Minimal, distraction-free interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = koinViewModel(),
    isDebugMode: Boolean = false,
    windowSizeClass: WindowSizeClass? = null,
    onNavigateToSettings: () -> Unit = {}
) {
    val isCompactWidth = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact || windowSizeClass == null
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    // 位置の可視化（精度リング）は削除
    var showDebugInfo by remember { mutableStateOf(false) }

    // アニメーションを最適化（より短い時間でスムーズに）
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(200), // 300ms → 200ms
        label = "scale"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(200), // 300ms → 200ms
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(200), // 300ms → 200ms
        label = "offsetY"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main map canvas
            HyperMapCanvas(
                uiState = uiState,
                scale = animatedScale,
                offsetX = animatedOffsetX,
                offsetY = animatedOffsetY,
                onTransform = { newScale, newOffsetX, newOffsetY ->
                    scale = newScale
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                }
            )

            // Top status bar
            HyperStatusBar(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )

            // センサー登録状態のミニインジケータは削除

            // 位置インジケータは削除

            // Floating action buttons
            HyperFloatingActions(
                onSettingsClick = onNavigateToSettings,
                onDebugToggle = { showDebugInfo = !showDebugInfo },
                showDebugInfo = showDebugInfo,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )

            // Debug overlay (when enabled)
            if (showDebugInfo) {
                HyperDebugOverlay(
                    uiState = uiState,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
        }
    }
}
// MiniDot は未使用のため削除

@Composable
private fun HyperMapCanvas(
    uiState: MapViewModel.MapUiState?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onTransform: (Float, Float, Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 3f)
                    val newOffsetX = (offsetX + pan.x).coerceIn(-1000f, 1000f)
                    val newOffsetY = (offsetY + pan.y).coerceIn(-1000f, 1000f)
                    onTransform(newScale, newOffsetX, newOffsetY)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val newScale = (scale * 1.5f).coerceIn(0.5f, 3f)
                        onTransform(newScale, offsetX, offsetY)
                    }
                )
            }
    ) {
        // Map background (placeholder for now)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // Grid pattern for visual reference
            HyperGridPattern()
        }
    }
}

@Composable
private fun HyperStatusBar(
    uiState: MapViewModel.MapUiState?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 座標数値の表示は削除

            // 既存のステータス情報
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 測位状態は廃止

                // 更新頻度
                Column {
                    Text(
                        text = "更新頻度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "10 Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // バッテリー使用量
                Column {
                    Text(
                        text = "バッテリー",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "5%/h",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HyperFloatingActions(
    onSettingsClick: () -> Unit,
    onDebugToggle: () -> Unit,
    showDebugInfo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Settings button
        FloatingActionButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }

        // Debug toggle button
        FloatingActionButton(
            onClick = onDebugToggle,
            modifier = Modifier.size(56.dp),
            containerColor = if (showDebugInfo)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface,
            contentColor = if (showDebugInfo)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Debug"
            )
        }

        // 精度リングのトグルは削除
    }
}

@Composable
private fun HyperDebugOverlay(
    uiState: MapViewModel.MapUiState?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Debug Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // 座標数値の表示は削除

            // センサーデータ
            uiState?.debugData?.let { debugData ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "センサーデータ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // 加速度計
                    Text(
                        text = "加速度計: [%.2f, %.2f, %.2f]".format(
                            debugData.accelerometerValues.getOrNull(0) ?: 0f,
                            debugData.accelerometerValues.getOrNull(1) ?: 0f,
                            debugData.accelerometerValues.getOrNull(2) ?: 0f
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    // ジャイロスコープ
                    Text(
                        text = "ジャイロ: [%.2f, %.2f, %.2f]".format(
                            debugData.gyroscopeValues.getOrNull(0) ?: 0f,
                            debugData.gyroscopeValues.getOrNull(1) ?: 0f,
                            debugData.gyroscopeValues.getOrNull(2) ?: 0f
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )

                    // 磁力計
                    Text(
                        text = "磁力計: [%.2f, %.2f, %.2f]".format(
                            debugData.magnetometerValues.getOrNull(0) ?: 0f,
                            debugData.magnetometerValues.getOrNull(1) ?: 0f,
                            debugData.magnetometerValues.getOrNull(2) ?: 0f
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // PDR情報
            uiState?.debugData?.let { debugData ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "PDR情報",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "歩数: ${debugData.pdrStepCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "方位: %.1f°".format(debugData.pdrHeading),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "歩幅: %.2f m".format(debugData.pdrStepLength),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // Wi-Fi情報
            uiState?.debugData?.let { debugData ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Wi-Fi情報",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "信号強度: %.1f dBm".format(debugData.wifiSignalStrength),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "アクセスポイント: ${debugData.wifiAccessPointCount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = "精度: %.1f m".format(debugData.wifiPositionAccuracy),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun HyperGridPattern() {
    // Simple grid pattern for visual reference
    // This would be replaced with actual map rendering
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Grid lines would be drawn here
        // For now, just a placeholder
    }
}

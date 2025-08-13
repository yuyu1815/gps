package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.domain.model.UserPosition3D
import com.example.myapplication.ui.component.*
import com.example.myapplication.presentation.viewmodel.CoordinateDisplayViewModel

/**
 * 座標表示画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinateDisplayScreen(
    viewModel: CoordinateDisplayViewModel,
    onNavigateBack: () -> Unit
) {
    val position by viewModel.currentPosition.collectAsState()
    val positionHistory by viewModel.positionHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "3D座標表示",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPosition() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "更新"
                        )
                    }
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "履歴クリア"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // ローディング表示
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "座標を取得中...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                error != null -> {
                    // エラー表示
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "エラー",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error ?: "不明なエラー",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    // メインコンテンツ
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 3D Sensor Block for device orientation
                        Sensor3DBlock(
                            modifier = Modifier.padding(16.dp),
                            showAxes = true,
                            showValues = true,
                            isAnimated = true,
                            useSensorData = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 3D Coordinate Display
                        position?.let { currentPosition ->
                            Coordinate3DDisplay(
                                position = currentPosition,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } ?: run {
                            // 座標データがない場合の表示
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "座標データがありません",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 統計情報カード
 */
@Composable
private fun StatisticsCard(
    positionHistory: List<UserPosition3D>,
    modifier: Modifier = Modifier
) {
    val validPositions = positionHistory.filter { it.isValid() }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "統計情報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (validPositions.isNotEmpty()) {
                val totalDistance = validPositions.zipWithNext().sumOf { (prev, next) ->
                    prev.distance2DTo(next).toDouble()
                }
                
                val avgAccuracy = validPositions.map { it.accuracy }.average()
                val avgConfidence = validPositions.map { it.confidence }.average()
                val maxElevation = validPositions.map { it.z }.maxOrNull() ?: 0.0f
                val minElevation = validPositions.map { it.z }.minOrNull() ?: 0.0f
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem(
                        label = "総移動距離",
                        value = "${totalDistance.format(2)} m",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        label = "平均精度",
                        value = "${avgAccuracy.format(2)} m",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem(
                        label = "平均信頼度",
                        value = "${(avgConfidence * 100).format(0)}%",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        label = "高さ範囲",
                        value = "${(maxElevation - minElevation).toDouble().format(2)} m",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatisticItem(
                        label = "データポイント",
                        value = "${validPositions.size}",
                        modifier = Modifier.weight(1f)
                    )
                    StatisticItem(
                        label = "現在の高さ",
                        value = "${validPositions.lastOrNull()?.z?.format(2) ?: "0.00"} m",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "統計データがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 統計アイテム
 */
@Composable
private fun StatisticItem(
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
            style = MaterialTheme.typography.bodyMedium,
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

/**
 * Float値を指定された小数点以下桁数でフォーマット
 */
private fun Float.format(digits: Int): String {
    return "%.${digits}f".format(this)
}

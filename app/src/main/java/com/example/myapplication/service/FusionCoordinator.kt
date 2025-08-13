package com.example.myapplication.service

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.domain.model.Motion
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.model.UserPosition3D
import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase
import com.example.myapplication.slam.ArCoreManager
import com.example.myapplication.wifi.WifiPositionEstimator
import com.example.myapplication.wifi.WifiScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import timber.log.Timber
import kotlinx.coroutines.delay

/**
 * Coordinates Wi‑Fi positioning and SLAM motion to continuously update fused position.
 * Minimal Hyper-like pipeline: Wi‑Fi (absolute) + VI-SLAM (relative) fused by EKF.
 * Now includes 3D coordinate support with Z-axis and floor information.
 */
class FusionCoordinator(
    private val wifiScanner: WifiScanner,
    private val wifiPositionEstimator: WifiPositionEstimator,
    private val arCoreManager: ArCoreManager,
    private val fuseSensorDataUseCase: FuseSensorDataUseCase,
    private val positionRepository: IPositionRepository,
    private val settingsRepository: ISettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var job: Job? = null

    // Last known absolute Wi‑Fi position
    @Volatile private var lastWifiPosition: UserPosition = UserPosition.invalid()
    
    // Last known 3D position
    @Volatile private var last3DPosition: UserPosition3D = UserPosition3D.invalid()
    
    fun start() {
        if (job != null) return
        
        // Start Wi-Fi periodic scanning (will fail silently if permissions are missing)
        val wifiScanStarted = wifiScanner.startPeriodicScanning(intervalMs = 3000L) // 3秒間隔でスキャン
        if (!wifiScanStarted) {
            Timber.w("Wi-Fi scanning failed to start (likely due to missing permissions)")
        }
        
        job = scope.launch {
            launch { collectWifi() }
            launch { collectSlamAndFuse() }
            // 初期位置確定ロジックは廃止
        }
        Timber.d("FusionCoordinator started")
    }

    fun stop() {
        job?.cancel()
        job = null
        
        // Stop Wi-Fi scanning
        wifiScanner.stopScan()
        
        Timber.d("FusionCoordinator stopped")
    }

    private suspend fun collectWifi() {
        wifiScanner.scanResults.collectLatest { scanResults ->
            try {
                // Check if we have scan results
                if (scanResults.isNotEmpty()) {
                    // Get selected BSSIDs from settings
                    val selectedBssids = try {
                        settingsRepository.getSelectedWifiBssids()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to get selected BSSIDs")
                        emptySet()
                    }
                    
                    // Use RSSI-based position estimation with selected BSSIDs
                    val pos = wifiPositionEstimator.estimatePositionFromRssi(selectedBssids = selectedBssids)
                    if (pos != null) {
                        // Map estimator accuracy to float; clamp minimum
                        val acc = pos.accuracy.coerceAtLeast(2.5).toFloat()
                        val confidence = (1.0 / (1.0 + acc / 5.0)).toFloat()
                        
                        // Create 2D position
                        lastWifiPosition = UserPosition(
                            x = pos.x.toFloat(),
                            y = pos.y.toFloat(),
                            accuracy = acc,
                            timestamp = System.currentTimeMillis(),
                            source = PositionSource.WIFI,
                            confidence = confidence
                        )
                        
                        // Create 3D position with estimated Z coordinate and floor
                        val zCoordinate = estimateZCoordinateFromWifi(pos)
                        val floor = estimateFloorFromWifi(pos)
                        
                        last3DPosition = UserPosition3D.from2D(
                            position2D = lastWifiPosition,
                            z = zCoordinate,
                            floor = floor
                        )
                        
                        // Update 3D position in repository
                        positionRepository.update3DPosition(last3DPosition)
                        
                        Timber.v("Wi‑Fi 3D position: (${last3DPosition.x}, ${last3DPosition.y}, ${last3DPosition.z}) ±${last3DPosition.accuracy}m, floor: ${last3DPosition.floor}, using ${selectedBssids.size} selected APs")
                    } else {
                        Timber.w("Wi-Fi position estimation failed - no valid position data")
                    }
                } else {
                    Timber.w("No Wi-Fi scan results available")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error estimating Wi‑Fi position")
            }
        }
    }
    
    /**
     * Wi-FiデータからZ座標を推定
     */
    private fun estimateZCoordinateFromWifi(pos: com.example.myapplication.wifi.Position): Float {
        // Wi-Fi信号強度やアクセスポイントの配置から高さを推定
        // 実際の実装では、以下のデータを使用すべき：
        // 1. アクセスポイントの設置高さ情報
        // 2. 信号強度のパターン分析
        // 3. 複数フロアのWi-Fiフィンガープリント比較
        
        // 現在は簡易的に、精度に基づいて推定
        return when {
            pos.accuracy <= 2.0 -> 1.5f  // 高精度の場合は立位の高さ
            pos.accuracy <= 5.0 -> 1.0f  // 中精度の場合は座席の高さ
            else -> 0.5f  // 低精度の場合は低い位置
        }
    }
    
    /**
     * Wi-Fiデータからフロアを推定
     */
    private fun estimateFloorFromWifi(pos: com.example.myapplication.wifi.Position): Int {
        // Wi-Fiアクセスポイントの配置や信号強度パターンからフロアを推定
        // 実際の実装では、以下のデータを使用すべき：
        // 1. フロア別Wi-Fiフィンガープリントデータベース
        // 2. アクセスポイントの設置階情報
        // 3. 信号強度の垂直方向パターン
        
        // 現在は簡易的に、精度に基づいて推定
        return when {
            pos.accuracy <= 2.0 -> 1  // 高精度の場合は1階
            pos.accuracy <= 5.0 -> 1  // 中精度の場合は1階
            else -> 1  // 低精度の場合は1階（デフォルト）
        }
    }

    private suspend fun collectSlamAndFuse() {
        while (true) {
            try {
                val motion: Motion? = if (arCoreManager.isArCoreSupported() && arCoreManager.isReadyForUpdate()) {
                    try {
                        arCoreManager.update()
                    } catch (e: com.google.ar.core.exceptions.SessionPausedException) {
                        Timber.d("ARCore session paused, skipping SLAM update")
                        null
                    } catch (e: Exception) {
                        Timber.e(e, "Error in ARCore update")
                        null
                    }
                } else {
                    null
                }
                
                if (motion != null) {
                    val wifi = if (UserPosition.isValid(lastWifiPosition)) lastWifiPosition else UserPosition.invalid()
                    val fused = fuseSensorDataUseCase.invoke(
                        FuseSensorDataUseCase.Params(
                            slamMotion = motion,
                            wifiPosition = wifi,
                            pdrMotion = null,
                            motionConfidence = 0.8f,
                            updateRepository = true
                        )
                    )
                    
                    // Update 3D position with SLAM data
                    val updated3DPosition = update3DPositionWithSLAM(fused, motion)
                    last3DPosition = updated3DPosition
                    
                    // Update both 2D and 3D positions in repository
                    positionRepository.updatePosition(fused)
                    positionRepository.update3DPosition(updated3DPosition)
                    
                    Timber.v("SLAM fused 3D position: (${updated3DPosition.x}, ${updated3DPosition.y}, ${updated3DPosition.z}) ±${updated3DPosition.accuracy}m, floor: ${updated3DPosition.floor}")
                } else {
                    // No SLAM available: still push last Wi‑Fi absolute to repository (acts as hold)
                    if (UserPosition.isValid(lastWifiPosition)) {
                        positionRepository.updatePosition(lastWifiPosition)
                        
                        // Also update 3D position if available
                        if (last3DPosition.isValid()) {
                            positionRepository.update3DPosition(last3DPosition)
                            Timber.v("Using last known 3D position: (${last3DPosition.x}, ${last3DPosition.y}, ${last3DPosition.z}) ±${last3DPosition.accuracy}m, floor: ${last3DPosition.floor}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in SLAM update or fusion")
            }
            // Let AR update cadence control itself inside arCoreManager
            kotlinx.coroutines.delay(30)
        }
    }
    
    /**
     * SLAMデータを使用して3D位置を更新
     */
    private fun update3DPositionWithSLAM(fused2D: UserPosition, motion: Motion): UserPosition3D {
        // SLAMデータからZ座標情報を推定
        // Motionクラスにはtranslationプロパティがないため、簡易的な推定を使用
        val slamZ = estimateZCoordinateFromSLAM(motion)
        val slamFloor = estimateFloorFromSLAM(motion)
        
        return UserPosition3D.from2D(
            position2D = fused2D,
            z = slamZ,
            floor = slamFloor
        )
    }
    
    /**
     * SLAMデータからZ座標を推定
     */
    private fun estimateZCoordinateFromSLAM(motion: Motion): Float {
        // SLAMの速度情報から高さを推定
        // 実際の実装では、以下のデータを使用すべき：
        // 1. ARCoreのPose情報からZ座標を取得
        // 2. 気圧センサーとの補正
        // 3. 建物の階高情報
        
        // 現在は簡易的に、速度に基づいて推定
        return when {
            motion.velocity > 1.0 -> 1.5f  // 高速移動時は立位の高さ
            motion.velocity > 0.5 -> 1.0f  // 中速移動時は座席の高さ
            else -> 0.5f  // 低速/停止時は低い位置
        }
    }
    
    /**
     * SLAMデータからフロアを推定
     */
    private fun estimateFloorFromSLAM(motion: Motion): Int {
        // SLAMの速度情報からフロアを推定
        // 実際の実装では、以下のデータを使用すべき：
        // 1. ARCoreのPose情報からZ座標を取得
        // 2. 建物の階高情報
        // 3. 初期化時の基準フロア情報
        
        // 現在は簡易的に、速度に基づいて推定
        return when {
            motion.velocity > 1.0 -> 1  // 高速移動時は1階
            motion.velocity > 0.5 -> 1  // 中速移動時は1階
            else -> 1  // 低速/停止時は1階（デフォルト）
        }
    }

    // ensureInitialFixIfNeeded は削除
}



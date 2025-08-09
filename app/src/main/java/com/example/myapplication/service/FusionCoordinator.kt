package com.example.myapplication.service

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.domain.model.Motion
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
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

    fun start() {
        if (job != null) return
        job = scope.launch {
            launch { collectWifi() }
            launch { collectSlamAndFuse() }
            launch { ensureInitialFixIfNeeded() }
        }
        Timber.d("FusionCoordinator started")
    }

    fun stop() {
        job?.cancel()
        job = null
        Timber.d("FusionCoordinator stopped")
    }

    private suspend fun collectWifi() {
        wifiScanner.scanResults.collectLatest { _ ->
            try {
                // Prefer KNN estimator if fingerprints available; fallback to simple
                val pos = wifiPositionEstimator.estimatePosition() ?: wifiPositionEstimator.estimatePositionSimple()
                if (pos != null) {
                    // Map estimator accuracy to float; clamp minimum
                    val acc = pos.accuracy.coerceAtLeast(2.5).toFloat()
                    lastWifiPosition = UserPosition(
                        x = pos.x.toFloat(),
                        y = pos.y.toFloat(),
                        accuracy = acc,
                        timestamp = System.currentTimeMillis(),
                        source = PositionSource.WIFI,
                        confidence = (1.0 / (1.0 + acc / 5.0)).toFloat()
                    )
                    Timber.v("Wi‑Fi position: (${lastWifiPosition.x}, ${lastWifiPosition.y}) ±${lastWifiPosition.accuracy}m")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error estimating Wi‑Fi position")
            }
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
                    // Redundant but ensures repository is updated in case updateRepository is false later
                    positionRepository.updatePosition(fused)
                } else {
                    // No SLAM available: still push last Wi‑Fi absolute to repository (acts as hold)
                    if (UserPosition.isValid(lastWifiPosition)) {
                        positionRepository.updatePosition(lastWifiPosition)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in SLAM update or fusion")
            }
            // Let AR update cadence control itself inside arCoreManager
            kotlinx.coroutines.delay(30)
        }
    }

    private suspend fun ensureInitialFixIfNeeded() {
        try {
            // Read mode once at start; can be extended to observe changes if needed
            val mode = settingsRepository.getInitialFixMode()
            when (mode) {
                com.example.myapplication.domain.model.InitialFixMode.AUTO -> {
                    // No-op: Wi‑Fi初回Fixで自動的に反映（collectWifi/collectSlamAndFuseが行う）
                }
                com.example.myapplication.domain.model.InitialFixMode.MANUAL -> {
                    // ユーザー操作待ち（UI側でPositionRepository.updatePositionを呼ぶ想定）
                }
                com.example.myapplication.domain.model.InitialFixMode.TEMPORARY -> {
                    // 一定時間で(0,0)を暫定設定し、後から上書き
                    delay(10_000)
                    val current = positionRepository.getCurrentPosition()
                    val haveWifi = UserPosition.isValid(lastWifiPosition)
                    if (!UserPosition.isValid(current) && !haveWifi) {
                        val provisional = UserPosition(
                            x = 0f,
                            y = 0f,
                            accuracy = 50f,
                            source = PositionSource.UNKNOWN,
                            confidence = 0.1f
                        )
                        positionRepository.updatePosition(provisional)
                        Timber.w("Temporary initial fix applied at (0,0). Will be overwritten when absolute fix available.")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error ensuring initial fix")
        }
    }
}



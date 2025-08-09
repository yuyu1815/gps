package com.example.myapplication.slam

import android.app.Activity
import android.content.Context
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Motion
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.StaticDetector
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.math.atan2
import kotlin.math.sqrt
import com.example.myapplication.service.StringProvider

/**
 * Manages ARCore sessions and SLAM functionality with power optimization.
 * Implements dynamic frame processing based on device movement to reduce power consumption.
 */
class ArCoreManager(private val context: Context) : KoinComponent {

    private var session: Session? = null
    private val featureTracker = FeatureTracker()
    private val motionEstimator = MotionEstimator()
    private val mapManager = MapManager()
    private var previousPose: Pose? = null
    
    // Power optimization parameters
    private val staticDetector = StaticDetector()
    private val sensorMonitor: SensorMonitor by inject()
    private var sensorData: StateFlow<SensorData.Combined>? = null
    
    // Frame processing rate control
    private var isDeviceStatic = false
    private var isLongStatic = false
    private var frameSkipCount = 0
    private var maxFrameSkip = 0
    private var lastProcessedTime = 0L
    
    // Retry/backoff and logging flags
    private var createSessionFailedOnce = false
    private var notSupportedLoggedOnce = false
    private var nextCreateSessionAttemptTime = 0L
    
    // Processing thresholds based on movement state
    private val staticProcessingIntervalMs = 1000L  // Process every 1 second when static
    private val movingProcessingIntervalMs = 100L   // Process every 100ms when moving
    private val longStaticProcessingIntervalMs = 3000L // Process every 3 seconds when long static
    
    // Feature point filtering
    private val staticMaxFeaturePoints = 10  // Limit feature points when static
    private val movingMaxFeaturePoints = 50  // More feature points when moving
    
    // Session lifecycle state to guard updates when paused
    private var isResumed: Boolean = false

    init {
        // Register for sensor data updates
        sensorData = sensorMonitor.sensorData
    }

    fun isArCoreSupported(): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            if (availability.isTransient) {
                // Avoid attempting session creation during transient checking state
                false
            } else {
                availability == ArCoreApk.Availability.SUPPORTED_INSTALLED
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun requestArCoreInstall(activity: Activity) {
        try {
            ArCoreApk.getInstance().requestInstall(activity, true)
        } catch (e: Exception) {
            Timber.e(e, StringProvider.getString(com.example.myapplication.R.string.log_arcore_install_failed))
        }
    }

    fun createSession(): Session? {
        if (session != null) return session
        val now = System.currentTimeMillis()
        if (now < nextCreateSessionAttemptTime) {
            // Backing off from retries
            return null
        }
        return try {
            session = Session(context)
            configureSession()
            // Reset failure/backoff flags on success
            createSessionFailedOnce = false
            nextCreateSessionAttemptTime = 0L
            session
        } catch (e: Exception) {
            if (!createSessionFailedOnce) {
                Timber.e(e, StringProvider.getString(com.example.myapplication.R.string.log_arcore_create_session_failed))
                createSessionFailedOnce = true
            } else {
                Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_arcore_create_session_still_failing))
            }
            nextCreateSessionAttemptTime = now + 60_000L // 60s backoff
            null
        }
    }

    private fun configureSession() {
        val config = Config(session)
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        session?.configure(config)
    }

    fun resumeSession() {
        try {
            session?.resume()
            staticDetector.reset()
            isResumed = true
        } catch (e: CameraNotAvailableException) {
            Timber.e(e, StringProvider.getString(com.example.myapplication.R.string.log_camera_not_available))
            isResumed = false
        }
    }

    fun pauseSession() {
        try {
            session?.pause()
        } catch (_: Exception) {
            // ignore
        }
        isResumed = false
    }

    fun closeSession() {
        session?.close()
        session = null
        isResumed = false
    }

    fun isReadyForUpdate(): Boolean {
        return session != null && isResumed
    }

    /**
     * Updates ARCore session and returns motion data if available.
     * 
     * @return Motion data if available, null otherwise
     */
    fun update(): Motion? {
        // Do not call into ARCore when session is paused; avoids native errors
        if (!isReadyForUpdate()) {
            return null
        }
        val currentTime = System.currentTimeMillis()
        val timeSinceLastProcess = currentTime - lastProcessedTime
        
        val processingInterval = when {
            isLongStatic -> longStaticProcessingIntervalMs
            isDeviceStatic -> staticProcessingIntervalMs
            else -> movingProcessingIntervalMs
        }
        
        // Skip frame if not enough time has passed since last processing
        if (timeSinceLastProcess < processingInterval) {
            return null
        }
        
        // Lazy init session if needed (shouldn't happen if isReadyForUpdate passed, but keep for safety)
        if (session == null) {
            if (!isArCoreSupported()) {
                if (!notSupportedLoggedOnce) {
                    Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_arcore_not_supported))
                    notSupportedLoggedOnce = true
                }
                return null
            }
            val created = createSession()
            if (created == null) {
                // createSession() already throttles and logs once on first failure
                return null
            }
        }
        
        // Check if session is available and not paused
        if (session == null) {
            // Avoid repeated warnings; just skip until next attempt window
            return null
        }
        
        // Process frame with error handling
        lastProcessedTime = currentTime
        val frame = try {
            session?.update()
        } catch (e: com.google.ar.core.exceptions.SessionPausedException) {
            // Mark paused to avoid further updates until resume
            isResumed = false
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_arcore_session_paused))
            return null
        } catch (e: Exception) {
            Timber.e(e, StringProvider.getString(com.example.myapplication.R.string.log_error_updating_arcore))
            return null
        }
        
        if (frame != null) {
            updateMap(frame)
            val poseDelta = updateMotion(frame)
            val dtSeconds = (timeSinceLastProcess.coerceAtLeast(1L)).toDouble() / 1000.0
            if (poseDelta != null) {
                val t = poseDelta.translation
                val distance2D = sqrt((t[0] * t[0] + t[1] * t[1]).toDouble())
                val velocity = distance2D / dtSeconds

                val q = poseDelta.rotationQuaternion
                // Yaw from quaternion
                val siny_cosp = 2.0 * (q[3] * q[2] + q[0] * q[1])
                val cosy_cosp = 1.0 - 2.0 * (q[1] * q[1] + q[2] * q[2])
                val yaw = atan2(siny_cosp, cosy_cosp)
                val angularVelocity = yaw / dtSeconds

                return Motion(velocity = velocity, angularVelocity = angularVelocity)
            }
        }
        return null
    }

    /**
     * Gets feature points with filtering based on movement state to reduce processing load.
     * 
     * @param frame The current ARCore frame
     * @return Filtered list of feature points
     */
    fun getFeaturePoints(frame: Frame): List<FeaturePoint> {
        // Set maximum feature points based on movement state
        val maxPoints = if (isDeviceStatic) staticMaxFeaturePoints else movingMaxFeaturePoints
        
        // Get and filter feature points
        val allPoints = featureTracker.trackFeatures(session, frame)
        
        // Return all points if under the limit, otherwise sample
        return if (allPoints.size <= maxPoints) {
            allPoints
        } else {
            // Sample points evenly
            val samplingInterval = allPoints.size / maxPoints
            allPoints.filterIndexed { index, _ -> index % samplingInterval == 0 }
                .take(maxPoints)
        }
    }

    private fun updateMotion(frame: Frame): Pose? {
        val currentPose = frame.camera?.pose
        var motion: Pose? = null
        if (currentPose != null && previousPose != null) {
            motion = motionEstimator.estimateMotion(currentPose, previousPose!!)
        }
        previousPose = currentPose
        return motion
    }

    private fun updateMap(frame: Frame) {
        if (session != null) {
            mapManager.updateMap(session!!, frame)
        }
    }

    fun getAnchors(): List<Anchor> {
        return mapManager.getAnchors()
    }
    
    /**
     * Gets the current feature points detected by ARCore.
     * This is a mock implementation that generates synthetic feature points
     * until the ARCore integration issues are resolved.
     *
     * @return List of synthetic feature points for testing
     */
    fun getFeaturePoints(): List<FeaturePoint> {
        // Check if we should generate points based on movement state
        if (isLongStatic) {
            // Return fewer points when device is static for a long time
            return generateSyntheticFeaturePoints(5)
        } else if (isDeviceStatic) {
            // Return moderate number of points when device is static
            return generateSyntheticFeaturePoints(10)
        } else {
            // Return more points when device is moving
            return generateSyntheticFeaturePoints(20)
        }
    }
    
    /**
     * Generates synthetic feature points for testing purposes.
     * This is used as a temporary solution until ARCore integration is fixed.
     *
     * @param count Number of points to generate
     * @return List of synthetic feature points
     */
    private fun generateSyntheticFeaturePoints(count: Int): List<FeaturePoint> {
        val points = mutableListOf<FeaturePoint>()
        val currentTime = System.currentTimeMillis()
        
        for (i in 0 until count) {
            // Generate points in a grid pattern with some randomness
            val x = (i % 5) * 0.2f + (Math.random() * 0.05).toFloat() - 0.025f
            val y = (i / 5) * 0.2f + (Math.random() * 0.05).toFloat() - 0.025f
            val z = (Math.random() * 0.5).toFloat() - 0.25f
            val confidence = (0.7 + Math.random() * 0.3).toFloat()
            
            points.add(FeaturePoint(
                x = x,
                y = y,
                z = z,
                confidence = confidence,
                id = i.toLong(),
                timestamp = currentTime
            ))
        }
        
        return points
    }
    
    /**
     * Updates the movement state based on sensor data.
     * Uses StaticDetector for accurate static state detection.
     * 
     * @param sensorData The current sensor data
     */
    private fun updateMovementState(sensorData: SensorData.Combined?) {
        if (sensorData == null) return
        
        // Detect static state using StaticDetector
        isDeviceStatic = staticDetector.detectStaticState(sensorData)
        isLongStatic = staticDetector.isLongStaticState()
        
        // Log state changes for debugging
        if (isLongStatic) {
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_device_long_static))
        } else if (isDeviceStatic) {
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_device_static))
        } else {
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_device_moving))
        }
    }
}

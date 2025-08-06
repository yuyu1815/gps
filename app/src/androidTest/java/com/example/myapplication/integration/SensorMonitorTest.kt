package com.example.myapplication.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.service.SensorMonitor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Integration test for SensorMonitor.
 * 
 * This test verifies that the SensorMonitor correctly collects sensor data
 * and emits it through its flows.
 */
@RunWith(AndroidJUnit4::class)
class SensorMonitorTest {

    private lateinit var context: Context
    private lateinit var sensorMonitor: SensorMonitor
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sensorMonitor = SensorMonitor(context)
    }
    
    @After
    fun tearDown() {
        sensorMonitor.stopMonitoring()
    }
    
    @Test
    fun testSensorMonitorCollectsAccelerometerData() = runBlocking {
        // Start sensor monitoring
        sensorMonitor.startMonitoring()
        
        // Wait for sensor data to be collected (with timeout)
        val accelerometerData = withTimeout(TimeUnit.SECONDS.toMillis(5)) {
            // Keep checking until we get non-null data
            var data: SensorData.Accelerometer? = null
            while (data == null) {
                data = sensorMonitor.accelerometerFlow.first()
                if (data == null) {
                    // Small delay before trying again
                    kotlinx.coroutines.delay(100)
                }
            }
            data
        }
        
        // Verify we got some accelerometer data
        assertNotNull(accelerometerData)
        
        // Stop monitoring
        sensorMonitor.stopMonitoring()
    }
    
    @Test
    fun testSensorMonitorCollectsGyroscopeData() = runBlocking {
        // Start sensor monitoring
        sensorMonitor.startMonitoring()
        
        // Wait for sensor data to be collected (with timeout)
        val gyroscopeData = withTimeout(TimeUnit.SECONDS.toMillis(5)) {
            // Keep checking until we get non-null data
            var data: SensorData.Gyroscope? = null
            while (data == null) {
                data = sensorMonitor.gyroscopeFlow.first()
                if (data == null) {
                    // Small delay before trying again
                    kotlinx.coroutines.delay(100)
                }
            }
            data
        }
        
        // Verify we got some gyroscope data
        assertNotNull(gyroscopeData)
        
        // Stop monitoring
        sensorMonitor.stopMonitoring()
    }
    
    @Test
    fun testSensorMonitorCollectsMagnetometerData() = runBlocking {
        // Start sensor monitoring
        sensorMonitor.startMonitoring()
        
        // Wait for sensor data to be collected (with timeout)
        val magnetometerData = withTimeout(TimeUnit.SECONDS.toMillis(5)) {
            // Keep checking until we get non-null data
            var data: SensorData.Magnetometer? = null
            while (data == null) {
                data = sensorMonitor.magnetometerFlow.first()
                if (data == null) {
                    // Small delay before trying again
                    kotlinx.coroutines.delay(100)
                }
            }
            data
        }
        
        // Verify we got some magnetometer data
        assertNotNull(magnetometerData)
        
        // Stop monitoring
        sensorMonitor.stopMonitoring()
    }
}
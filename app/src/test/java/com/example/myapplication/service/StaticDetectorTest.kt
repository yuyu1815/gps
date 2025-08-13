package com.example.myapplication.service

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StaticDetectorTest {

    private lateinit var staticDetector: StaticDetector

    @Before
    fun setup() {
        staticDetector = StaticDetector()
    }

    @Test
    fun `test initial state is not static`() {
        assertFalse(staticDetector.isLongStaticState())
        assertEquals(0.0f, staticDetector.getStaticConfidence(), 0.01f)
        assertEquals(0L, staticDetector.getStaticDuration())
    }

    @Test
    fun `test static detection with static data`() {
        // Create static sensor data (very little movement)
        val staticData = createSensorData(
            linearAcceleration = Vector3D(0.05f, 0.03f, 0.04f),
            gyroscope = Vector3D(0.02f, 0.01f, 0.03f)
        )

        // First few detections should not trigger static state due to buffer size and consecutive detection requirements
        for (i in 1..4) {
            assertFalse(staticDetector.detectStaticState(staticData))
        }

        // After enough consistent static data, it should detect static state
        for (i in 1..10) {
            val result = staticDetector.detectStaticState(staticData)
            if (result) {
                // Once we detect static state, verify it stays static
                assertTrue(staticDetector.detectStaticState(staticData))
                assertTrue(staticDetector.getStaticConfidence() > 0.3f)
                assertTrue(staticDetector.getStaticDuration() > 0L)
                return
            }
        }

        // If we reach here, the test failed to detect static state
        assertTrue("Failed to detect static state after sufficient static data", false)
    }

    @Test
    fun `test static detection with movement data`() {
        // Create movement sensor data
        val movementData = createSensorData(
            linearAcceleration = Vector3D(1.5f, 2.0f, 1.8f),
            gyroscope = Vector3D(0.5f, 0.6f, 0.4f)
        )

        // Should not detect static state with movement data
        for (i in 1..10) {
            assertFalse(staticDetector.detectStaticState(movementData))
        }

        assertEquals(0.0f, staticDetector.getStaticConfidence(), 0.01f)
    }

    @Test
    fun `test transition from static to movement`() {
        // First establish static state
        val staticData = createSensorData(
            linearAcceleration = Vector3D(0.05f, 0.03f, 0.04f),
            gyroscope = Vector3D(0.02f, 0.01f, 0.03f)
        )

        // Feed static data until we detect static state
        var isStatic = false
        for (i in 1..15) {
            isStatic = staticDetector.detectStaticState(staticData)
            if (isStatic) break
        }

        // Verify we reached static state
        assertTrue("Failed to establish static state", isStatic)
        
        // Now introduce movement data
        val movementData = createSensorData(
            linearAcceleration = Vector3D(1.5f, 2.0f, 1.8f),
            gyroscope = Vector3D(0.5f, 0.6f, 0.4f)
        )

        // Should exit static state with movement data
        assertFalse(staticDetector.detectStaticState(movementData))
        assertEquals(0.0f, staticDetector.getStaticConfidence(), 0.01f)
    }

    @Test
    fun `test reset functionality`() {
        // First establish static state
        val staticData = createSensorData(
            linearAcceleration = Vector3D(0.05f, 0.03f, 0.04f),
            gyroscope = Vector3D(0.02f, 0.01f, 0.03f)
        )

        // Feed static data until we detect static state
        for (i in 1..15) {
            val isStatic = staticDetector.detectStaticState(staticData)
            if (isStatic) break
        }

        // Reset the detector
        staticDetector.reset()

        // Verify state is reset
        assertFalse(staticDetector.isLongStaticState())
        assertEquals(0.0f, staticDetector.getStaticConfidence(), 0.01f)
        assertEquals(0L, staticDetector.getStaticDuration())
    }

    private fun createSensorData(
        linearAcceleration: Vector3D,
        gyroscope: Vector3D
    ): SensorData.Combined {
        return SensorData.Combined(
            timestamp = System.currentTimeMillis(),
            accelerometer = Vector3D(9.8f, 0f, 0f), // Default gravity
            gyroscope = gyroscope,
            magnetometer = Vector3D(0f, 0f, 0f),
            linearAcceleration = linearAcceleration,
            gravity = Vector3D(0f, 0f, 9.8f), // Default gravity
            orientation = Vector3D(0f, 0f, 0f),
            heading = 0f
        )
    }
}
package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before

/**
 * Instrumented test, which will execute on an Android device.
 *
 * These tests verify basic component interactions and device capabilities
 * required for the indoor positioning application.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    
    private lateinit var appContext: Context
    
    @Before
    fun setUp() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @Test
    fun useAppContext() {
        // Verify the application package name
        assertEquals("com.example.myapplication", appContext.packageName)
    }
    
    @Test
    fun verifyRequiredPermissions() {
        // Check that the app has declared the required permissions
        val packageInfo = appContext.packageManager.getPackageInfo(
            appContext.packageName,
            PackageManager.GET_PERMISSIONS
        )
        
        val declaredPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        
        // Verify Bluetooth permissions
        assertTrue("BLUETOOTH permission missing", 
            declaredPermissions.contains("android.permission.BLUETOOTH"))
        assertTrue("BLUETOOTH_ADMIN permission missing", 
            declaredPermissions.contains("android.permission.BLUETOOTH_ADMIN"))
        
        // Verify location permissions (required for BLE scanning)
        assertTrue("ACCESS_FINE_LOCATION permission missing", 
            declaredPermissions.contains("android.permission.ACCESS_FINE_LOCATION"))
    }
    
    @Test
    fun verifySensorAvailability() {
        // Check that the device has the required sensors
        val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        
        // Check for accelerometer
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        assertNotNull("Accelerometer not available", accelerometer)
        
        // Check for gyroscope
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        assertNotNull("Gyroscope not available", gyroscope)
        
        // Check for magnetometer
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        assertNotNull("Magnetometer not available", magnetometer)
    }
    
    @Test
    fun testDistanceCalculationIntegration() {
        // Create a test beacon
        val beacon = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 10f,
            y = 20f,
            txPower = -70,
            name = "Test Beacon"
        )
        
        // Set RSSI value
        beacon.updateRssi(-80, System.currentTimeMillis())
        
        // Calculate expected distance using the formula
        val environmentalFactor = 2.0f
        val expectedDistance = Math.pow(10.0, ((beacon.txPower - beacon.lastRssi) / (10.0 * environmentalFactor)).toDouble()).toFloat()
        
        // Verify the beacon's distance calculation matches the expected formula
        val calculatedDistance = beacon.calculateDistance(environmentalFactor)
        assertEquals(expectedDistance, calculatedDistance, 0.01f)
    }
}
package com.example.myapplication.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(MockitoJUnitRunner::class)
class BatteryOptimizerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPowerManager: PowerManager

    @Mock
    private lateinit var mockBatteryIntent: Intent

    private lateinit var batteryOptimizer: BatteryOptimizer

    @Before
    fun setup() {
        // Setup PowerManager mock
        `when`(mockContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mockPowerManager)
        `when`(mockPowerManager.isPowerSaveMode).thenReturn(false)

        // Setup BatteryManager intent mock
        `when`(mockContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
            .thenReturn(mockBatteryIntent)

        // Default battery values
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            .thenReturn(BatteryManager.BATTERY_STATUS_DISCHARGING)
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1))
            .thenReturn(0) // Not plugged

        // Create battery optimizer with mocked context
        batteryOptimizer = BatteryOptimizer(mockContext)
    }

    @Test
    fun `test battery level detection`() {
        // Update battery state
        batteryOptimizer.updateBatteryState()

        // Verify battery level
        assertEquals(80, batteryOptimizer.getBatteryLevel())
        assertFalse(batteryOptimizer.isCharging())
        assertEquals(BatteryOptimizer.ChargingType.NONE, batteryOptimizer.getChargingType())
    }

    @Test
    fun `test charging state detection`() {
        // Set charging state
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            .thenReturn(BatteryManager.BATTERY_STATUS_CHARGING)
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1))
            .thenReturn(BatteryManager.BATTERY_PLUGGED_AC)

        // Update battery state
        batteryOptimizer.updateBatteryState()

        // Verify charging state
        assertTrue(batteryOptimizer.isCharging())
        assertEquals(BatteryOptimizer.ChargingType.AC, batteryOptimizer.getChargingType())
    }

    @Test
    fun `test optimization level with normal battery`() {
        // Set normal battery level
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.BALANCED, batteryOptimizer.getOptimizationLevel())
    }

    @Test
    fun `test optimization level with low battery`() {
        // Set low battery level
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.BATTERY_SAVING, batteryOptimizer.getOptimizationLevel())
    }

    @Test
    fun `test optimization level with critical battery`() {
        // Set critical battery level
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.CRITICAL, batteryOptimizer.getOptimizationLevel())
    }

    @Test
    fun `test optimization level with charging`() {
        // Set charging state with AC
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            .thenReturn(BatteryManager.BATTERY_STATUS_CHARGING)
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1))
            .thenReturn(BatteryManager.BATTERY_PLUGGED_AC)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.MAXIMUM_PERFORMANCE, batteryOptimizer.getOptimizationLevel())
    }

    @Test
    fun `test optimization level with power save mode`() {
        // Set power save mode
        `when`(mockPowerManager.isPowerSaveMode).thenReturn(true)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.BATTERY_SAVING, batteryOptimizer.getOptimizationLevel())
    }

    @Test
    fun `test scan config for static device with normal battery`() {
        // Set normal battery level
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        batteryOptimizer.updateBatteryState()

        // Get scan config for static device
        val scanConfig = batteryOptimizer.getScanConfig(
            isStatic = true,
            isLongStatic = false,
            activityLevel = ActivityLevel.NORMAL,
            defaultScanMode = 1, // SCAN_MODE_BALANCED
            lowPowerScanMode = 0, // SCAN_MODE_LOW_POWER
            highPrecisionScanMode = 2 // SCAN_MODE_LOW_LATENCY
        )

        // Verify scan config
        assertEquals(4000, scanConfig.scanPeriodMs)
        assertEquals(6000, scanConfig.scanIntervalMs)
        assertEquals(1, scanConfig.scanMode) // SCAN_MODE_BALANCED
        assertTrue(scanConfig.description.contains("Balanced"))
        assertTrue(scanConfig.description.contains("static"))
    }

    @Test
    fun `test scan config for long static device with low battery`() {
        // Set low battery level
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        batteryOptimizer.updateBatteryState()

        // Get scan config for long static device
        val scanConfig = batteryOptimizer.getScanConfig(
            isStatic = true,
            isLongStatic = true,
            activityLevel = ActivityLevel.LOW,
            defaultScanMode = 1, // SCAN_MODE_BALANCED
            lowPowerScanMode = 0, // SCAN_MODE_LOW_POWER
            highPrecisionScanMode = 2 // SCAN_MODE_LOW_LATENCY
        )

        // Verify scan config
        assertEquals(3000, scanConfig.scanPeriodMs)
        assertTrue(scanConfig.scanIntervalMs >= 15000) // Should be a long interval
        assertEquals(0, scanConfig.scanMode) // SCAN_MODE_LOW_POWER
        assertTrue(scanConfig.description.contains("Battery saving"))
        assertTrue(scanConfig.description.contains("long static"))
    }

    @Test
    fun `test scan config for moving device with critical battery`() {
        // Set critical battery level
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        batteryOptimizer.updateBatteryState()

        // Get scan config for moving device
        val scanConfig = batteryOptimizer.getScanConfig(
            isStatic = false,
            isLongStatic = false,
            activityLevel = ActivityLevel.HIGH,
            defaultScanMode = 1, // SCAN_MODE_BALANCED
            lowPowerScanMode = 0, // SCAN_MODE_LOW_POWER
            highPrecisionScanMode = 2 // SCAN_MODE_LOW_LATENCY
        )

        // Verify scan config
        assertTrue(scanConfig.scanIntervalMs >= 20000) // Should be a very long interval
        assertEquals(0, scanConfig.scanMode) // SCAN_MODE_LOW_POWER
        assertTrue(scanConfig.description.contains("Critical"))
    }

    @Test
    fun `test scan config for high activity with charging`() {
        // Set charging state with AC
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            .thenReturn(BatteryManager.BATTERY_STATUS_CHARGING)
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1))
            .thenReturn(BatteryManager.BATTERY_PLUGGED_AC)
        batteryOptimizer.updateBatteryState()

        // Get scan config for high activity
        val scanConfig = batteryOptimizer.getScanConfig(
            isStatic = false,
            isLongStatic = false,
            activityLevel = ActivityLevel.HIGH,
            defaultScanMode = 1, // SCAN_MODE_BALANCED
            lowPowerScanMode = 0, // SCAN_MODE_LOW_POWER
            highPrecisionScanMode = 2 // SCAN_MODE_LOW_LATENCY
        )

        // Verify scan config
        assertEquals(6000, scanConfig.scanPeriodMs)
        assertEquals(2000, scanConfig.scanIntervalMs) // Should be a short interval
        assertEquals(2, scanConfig.scanMode) // SCAN_MODE_LOW_LATENCY
        assertTrue(scanConfig.description.contains("Maximum performance"))
    }

    @Test
    fun `test battery thresholds update`() {
        // Set custom thresholds
        batteryOptimizer.setBatteryThresholds(25, 15)

        // Set battery level between new thresholds
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(20)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.BATTERY_SAVING, batteryOptimizer.getOptimizationLevel())

        // Set battery level below new critical threshold
        `when`(mockBatteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(10)
        batteryOptimizer.updateBatteryState()

        // Verify optimization level
        assertEquals(BatteryOptimizer.OptimizationLevel.CRITICAL, batteryOptimizer.getOptimizationLevel())
    }
}
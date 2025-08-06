package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase.FusionMethod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        `when`(mockContext.getSharedPreferences("indoor_positioning_settings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putLong(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putFloat(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(any(), any())).thenReturn(mockEditor)

        settingsRepository = SettingsRepository(mockContext)
    }

    @Test
    fun `getBleScanInterval returns default value when not set`() = runBlocking<Unit> {
        // Given
        val defaultValue = 1000L
        `when`(mockSharedPreferences.getLong("ble_scan_interval_ms", defaultValue))
            .thenReturn(defaultValue)

        // When
        val result = settingsRepository.getBleScanInterval()

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `getBleScanInterval returns stored value`() = runBlocking<Unit> {
        // Given
        val storedValue = 2000L
        `when`(mockSharedPreferences.getLong("ble_scan_interval_ms", 1000L))
            .thenReturn(storedValue)

        // When
        val result = settingsRepository.getBleScanInterval()

        // Then
        assertEquals(storedValue, result)
    }

    @Test
    fun `setBleScanInterval stores value in SharedPreferences`() = runBlocking<Unit> {
        // Given
        val valueToStore = 3000L

        // When
        settingsRepository.setBleScanInterval(valueToStore)

        // Then
        verify(mockEditor).putLong("ble_scan_interval_ms", valueToStore)
        verify(mockEditor).apply()
    }

    @Test
    fun `getEnvironmentalFactor returns default value when not set`() = runBlocking<Unit> {
        // Given
        val defaultValue = 2.0f
        `when`(mockSharedPreferences.getFloat("environmental_factor", defaultValue))
            .thenReturn(defaultValue)

        // When
        val result = settingsRepository.getEnvironmentalFactor()

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `setEnvironmentalFactor stores value in SharedPreferences`() = runBlocking<Unit> {
        // Given
        val valueToStore = 3.5f

        // When
        settingsRepository.setEnvironmentalFactor(valueToStore)

        // Then
        verify(mockEditor).putFloat("environmental_factor", valueToStore)
        verify(mockEditor).apply()
    }

    @Test
    fun `isDebugModeEnabled returns default value when not set`() = runBlocking<Unit> {
        // Given
        val defaultValue = false
        `when`(mockSharedPreferences.getBoolean("debug_mode_enabled", defaultValue))
            .thenReturn(defaultValue)

        // When
        val result = settingsRepository.isDebugModeEnabled()

        // Then
        assertEquals(defaultValue, result)
    }

    @Test
    fun `setDebugModeEnabled stores value in SharedPreferences`() = runBlocking<Unit> {
        // Given
        val valueToStore = true

        // When
        settingsRepository.setDebugModeEnabled(valueToStore)

        // Then
        verify(mockEditor).putBoolean("debug_mode_enabled", valueToStore)
        verify(mockEditor).apply()
    }

    @Test
    fun `getFusionMethod returns default value when not set`() = runBlocking<Unit> {
        // Given
        val defaultOrdinal = 0
        `when`(mockSharedPreferences.getInt("fusion_method", defaultOrdinal))
            .thenReturn(defaultOrdinal)

        // When
        val result = settingsRepository.getFusionMethod()

        // Then
        assertEquals(FusionMethod.WEIGHTED_AVERAGE, result)
    }

    @Test
    fun `setFusionMethod stores value in SharedPreferences`() = runBlocking<Unit> {
        // Given
        val valueToStore = FusionMethod.KALMAN_FILTER

        // When
        settingsRepository.setFusionMethod(valueToStore)

        // Then
        verify(mockEditor).putInt("fusion_method", valueToStore.ordinal)
        verify(mockEditor).apply()
    }

    @Test
    fun `resetToDefaults resets all settings to default values`() = runBlocking<Unit> {
        // When
        settingsRepository.resetToDefaults()

        // Then
        verify(mockEditor).putLong("ble_scan_interval_ms", 1000L)
        verify(mockEditor).putFloat("environmental_factor", 2.0f)
        verify(mockEditor).putFloat("step_length_cm", 75.0f)
        verify(mockEditor).putLong("beacon_staleness_timeout_ms", 5000L)
        verify(mockEditor).putBoolean("debug_mode_enabled", false)
        verify(mockEditor).putBoolean("data_logging_enabled", false)
        verify(mockEditor).putFloat("sensor_fusion_weight", 0.5f)
        verify(mockEditor).putInt("fusion_method", 0)
        verify(mockEditor).apply()
    }

    // Helper function to handle Mockito's any() for different types
    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()
}
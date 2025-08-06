package com.example.myapplication.service

import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Unit tests for the CrashReporter class.
 * These tests verify that the CrashReporter correctly interacts with Firebase Crashlytics.
 */
class CrashReporterTest {

    private lateinit var mockCrashlytics: FirebaseCrashlytics

    @Before
    fun setUp() {
        // Create a mock of FirebaseCrashlytics
        mockCrashlytics = mock(FirebaseCrashlytics::class.java)
        
        // Use reflection to replace the static instance of FirebaseCrashlytics in CrashReporter
        val crashlyticsField = CrashReporter.Companion::class.java.getDeclaredField("crashlytics")
        crashlyticsField.isAccessible = true
        
        // Remove final modifier
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(crashlyticsField, crashlyticsField.modifiers and Modifier.FINAL.inv())
        
        // Set our mock instance
        crashlyticsField.set(null, mockCrashlytics)
    }

    @Test
    fun `logException should record exception in Crashlytics`() {
        // Arrange
        val testException = RuntimeException("Test exception")
        val testMessage = "Test message"
        
        // Act
        CrashReporter.logException(testException, testMessage)
        
        // Assert
        verify(mockCrashlytics).log("Non-fatal exception: $testMessage")
        verify(mockCrashlytics).recordException(testException)
    }

    @Test
    fun `logException without message should record exception in Crashlytics`() {
        // Arrange
        val testException = RuntimeException("Test exception")
        
        // Act
        CrashReporter.logException(testException)
        
        // Assert
        verify(mockCrashlytics, Mockito.never()).log(Mockito.anyString())
        verify(mockCrashlytics).recordException(testException)
    }

    @Test
    fun `logError should log message in Crashlytics`() {
        // Arrange
        val testMessage = "Test error message"
        
        // Act
        CrashReporter.logError(testMessage)
        
        // Assert
        verify(mockCrashlytics).log("Error: $testMessage")
    }

    @Test
    fun `logError with custom keys should set custom keys in Crashlytics`() {
        // Arrange
        val testMessage = "Test error message"
        val customKeys = mapOf(
            "stringKey" to "stringValue",
            "intKey" to 123,
            "booleanKey" to true
        )
        
        // Act
        CrashReporter.logError(testMessage, customKeys)
        
        // Assert
        verify(mockCrashlytics).log("Error: $testMessage")
        verify(mockCrashlytics).setCustomKey("stringKey", "stringValue")
        verify(mockCrashlytics).setCustomKey("intKey", 123)
        verify(mockCrashlytics).setCustomKey("booleanKey", true)
    }

    @Test
    fun `setUserId should set user ID in Crashlytics`() {
        // Arrange
        val testUserId = "test-user-123"
        
        // Act
        CrashReporter.setUserId(testUserId)
        
        // Assert
        verify(mockCrashlytics).setUserId(testUserId)
    }

    @Test
    fun `setCustomKey with string should set custom key in Crashlytics`() {
        // Arrange
        val testKey = "testKey"
        val testValue = "testValue"
        
        // Act
        CrashReporter.setCustomKey(testKey, testValue)
        
        // Assert
        verify(mockCrashlytics).setCustomKey(testKey, testValue)
    }

    @Test
    fun `setCustomKey with int should set custom key in Crashlytics`() {
        // Arrange
        val testKey = "testKey"
        val testValue = 123
        
        // Act
        CrashReporter.setCustomKey(testKey, testValue)
        
        // Assert
        verify(mockCrashlytics).setCustomKey(testKey, testValue)
    }

    @Test
    fun `setCustomKey with boolean should set custom key in Crashlytics`() {
        // Arrange
        val testKey = "testKey"
        val testValue = true
        
        // Act
        CrashReporter.setCustomKey(testKey, testValue)
        
        // Assert
        verify(mockCrashlytics).setCustomKey(testKey, testValue)
    }

    @Test
    fun `setCrashlyticsCollectionEnabled should set collection enabled in Crashlytics`() {
        // Arrange
        val enabled = true
        
        // Act
        CrashReporter.setCrashlyticsCollectionEnabled(enabled)
        
        // Assert
        verify(mockCrashlytics).setCrashlyticsCollectionEnabled(enabled)
    }
}
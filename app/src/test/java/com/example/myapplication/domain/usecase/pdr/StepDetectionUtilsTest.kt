package com.example.myapplication.domain.usecase.pdr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for [StepDetectionUtils].
 */
class StepDetectionUtilsTest {

    @Test
    fun testApplyLowPassFilter() {
        // Given
        val currentValue = 10.0f
        val lastFilteredValue = 5.0f
        val alpha = 0.3f
        
        // When
        val result = StepDetectionUtils.applyLowPassFilter(currentValue, lastFilteredValue, alpha)
        
        // Then
        val expected = alpha * currentValue + (1 - alpha) * lastFilteredValue
        assertEquals(expected, result, 0.001f)
    }
    
    @Test
    fun testCalculateMedianForOddSizedList() {
        // Given
        val values = listOf(5f, 2f, 9f, 1f, 7f)
        
        // When
        val result = StepDetectionUtils.calculateMedian(values)
        
        // Then
        if (result != null) {
            assertEquals(5f, result, 0.001f)
        }
    }
    
    @Test
    fun testCalculateMedianForEvenSizedList() {
        // Given
        val values = listOf(5f, 2f, 9f, 1f, 7f, 3f)
        
        // When
        val result = StepDetectionUtils.calculateMedian(values)
        
        // Then
        if (result != null) {
            assertEquals(4f, result, 0.001f) // (3 + 5) / 2 = 4
        }
    }
    
    @Test
    fun testCalculateMedianForEmptyList() {
        // Given
        val values = emptyList<Float>()
        
        // When
        val result = StepDetectionUtils.calculateMedian(values)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testCalculateStandardDeviation() {
        // Given
        val values = listOf(2f, 4f, 4f, 4f, 5f, 5f, 7f, 9f)
        
        // When
        val result = StepDetectionUtils.calculateStandardDeviation(values)
        
        // Then
        // Mean = 5, Variance = 4, StdDev = 2
        if (result != null) {
            assertEquals(2f, result, 0.1f)
        }
    }
    
    @Test
    fun testCalculateStandardDeviationForEmptyList() {
        // Given
        val values = emptyList<Float>()
        
        // When
        val result = StepDetectionUtils.calculateStandardDeviation(values)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testCalculateQuartiles() {
        // Given
        val values = listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
        
        // When
        val result = StepDetectionUtils.calculateQuartiles(values)
        
        // Then
        if (result != null) {
            assertEquals(2f, result.first, 0.001f)  // Q1
            assertEquals(6f, result.second, 0.001f)   // Q3
        }
    }
    
    @Test
    fun testCalculateQuartilesForEmptyList() {
        // Given
        val values = emptyList<Float>()
        
        // When
        val result = StepDetectionUtils.calculateQuartiles(values)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testIsInRangeWithValueInRange() {
        // Given
        val value = 5f
        val min = 1f
        val max = 10f
        
        // When
        val result = StepDetectionUtils.isInRange(value, min, max)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun testIsInRangeWithValueOutsideRange() {
        // Given
        val value = 15f
        val min = 1f
        val max = 10f
        
        // When
        val result = StepDetectionUtils.isInRange(value, min, max)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun testIsInRangeWithLongValues() {
        // Given
        val value = 500L
        val min = 100L
        val max = 1000L
        
        // When
        val result = StepDetectionUtils.isInRange(value, min, max)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun testConstrainWithDifferentValues() {
        // Given
        val belowMin = 0f
        val aboveMax = 20f
        val withinRange = 5f
        val min = 1f
        val max = 10f
        
        // When
        val resultBelowMin = StepDetectionUtils.constrain(belowMin, min, max)
        val resultAboveMax = StepDetectionUtils.constrain(aboveMax, min, max)
        val resultWithinRange = StepDetectionUtils.constrain(withinRange, min, max)
        
        // Then
        assertEquals(min, resultBelowMin, 0.001f)
        assertEquals(max, resultAboveMax, 0.001f)
        assertEquals(withinRange, resultWithinRange, 0.001f)
    }
}
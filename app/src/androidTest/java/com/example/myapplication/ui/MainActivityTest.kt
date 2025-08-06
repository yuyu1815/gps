package com.example.myapplication.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the MainActivity.
 * 
 * These tests verify that the MainActivity launches correctly and
 * displays the expected UI elements.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun testMainActivityLaunches() {
        // Launch the MainActivity
        ActivityScenario.launch(MainActivity::class.java)
        
        // Verify that the root view is displayed
        onView(withId(android.R.id.content)).check(matches(isDisplayed()))
    }
}
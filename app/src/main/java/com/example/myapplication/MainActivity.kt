package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.service.CrashReporter
import com.example.myapplication.service.VersionChecker
import timber.log.Timber
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.viewmodel.MainViewModel
import com.example.myapplication.service.AnalyticsManager
import com.example.myapplication.ui.screen.CalibrationScreen
import com.example.myapplication.ui.screen.DebugScreen
import com.example.myapplication.ui.screen.LogManagementScreen
import com.example.myapplication.ui.screen.MapScreen
import com.example.myapplication.ui.screen.SettingsScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    
    // State for update notification
    private val updateInfo = mutableStateOf<VersionChecker.VersionCheckResult.UpdateAvailable?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Register for update notifications
        VersionChecker.setUpdateCallback { updateAvailable ->
            updateInfo.value = updateAvailable
        }
        
        // Example of using CrashReporter to log non-fatal exceptions
        try {
            // This is just a demonstration - in a real app, this would be actual functionality
            val testMap = mapOf("key1" to "value1")
            // Avoid forcing a crash in the demonstration
            val nonExistentValue = testMap["nonExistentKey"] ?: "default"
            Timber.d("Retrieved value: $nonExistentValue")
        } catch (e: Exception) {
            // Log the exception with CrashReporter
            CrashReporter.logException(e, "Demonstration of non-fatal exception logging")
            
            // You can also add custom keys for better debugging
            CrashReporter.setCustomKey("screen", "MainActivity")
            CrashReporter.setCustomKey("function", "onCreate")
        }
        
        // Example of logging an error without an exception
        CrashReporter.logError("Demonstration of error logging", 
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "isFirstLaunch" to false
            )
        )
        
        setContent {
            MaterialTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                
                // Show update dialog if update is available
                updateInfo.value?.let { versionInfo ->
                    com.example.myapplication.ui.component.UpdateNotificationDialog(
                        versionInfo = versionInfo,
                        onDismiss = { updateInfo.value = null },
                        onUpdateLater = { updateInfo.value = null }
                    )
                }
                
                MainScreen(windowSizeClass = windowSizeClass)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass? = null
) {
    val navController = rememberNavController()
    val navigationState by viewModel.navigationState.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "map"
    
    // Determine if we should use a different layout based on screen width
    val useCompactLayout = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact || windowSizeClass == null
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom navigation on compact screens (phones)
            if (useCompactLayout) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Map") },
                        label = { Text("Map") },
                        selected = currentRoute == "map",
                        onClick = { 
                            viewModel.navigateTo(MainViewModel.NavigationState.Map)
                            // Track navigation event
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = currentRoute,
                                toScreen = "map"
                            )
                            navController.navigate("map") {
                                popUpTo("map") { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == "settings",
                        onClick = { 
                            viewModel.navigateTo(MainViewModel.NavigationState.Settings)
                            // Track navigation event
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = currentRoute,
                                toScreen = "settings"
                            )
                            navController.navigate("settings") {
                                popUpTo("map")
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Build, contentDescription = "Debug") },
                        label = { Text("Debug") },
                        selected = currentRoute == "debug",
                        onClick = { 
                            viewModel.navigateTo(MainViewModel.NavigationState.Debug)
                            // Track navigation event
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = currentRoute,
                                toScreen = "debug"
                            )
                            navController.navigate("debug") {
                                popUpTo("map")
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("map") {
                // Track screen view
                AnalyticsManager.getInstance().logScreenView(
                    screenName = "Map Screen",
                    screenClass = "MapScreen"
                )
                MapScreen(
                    isDebugMode = false,
                    windowSizeClass = windowSizeClass
                )
            }
            composable("settings") {
                // Track screen view
                AnalyticsManager.getInstance().logScreenView(
                    screenName = "Settings Screen",
                    screenClass = "SettingsScreen"
                )
                SettingsScreen(
                    onNavigateBack = { 
                        viewModel.navigateTo(MainViewModel.NavigationState.Map)
                        // Track navigation event
                        AnalyticsManager.getInstance().logNavigation(
                            fromScreen = "settings",
                            toScreen = "map"
                        )
                        navController.popBackStack() 
                    },
                    onNavigateToLogs = { 
                        // Track navigation event
                        AnalyticsManager.getInstance().logNavigation(
                            fromScreen = "settings",
                            toScreen = "logs"
                        )
                        navController.navigate("logs") 
                    }
                )
            }
            composable("logs") {
                // Track screen view
                AnalyticsManager.getInstance().logScreenView(
                    screenName = "Log Management Screen",
                    screenClass = "LogManagementScreen"
                )
                LogManagementScreen(
                    onNavigateBack = { 
                        // Track navigation event
                        AnalyticsManager.getInstance().logNavigation(
                            fromScreen = "logs",
                            toScreen = "settings"
                        )
                        navController.popBackStack() 
                    }
                )
            }
            composable("debug") {
                // Track screen view
                AnalyticsManager.getInstance().logScreenView(
                    screenName = "Debug Screen",
                    screenClass = "DebugScreen"
                )
                DebugScreen(
                    onNavigateBack = { 
                        // Track navigation event
                        AnalyticsManager.getInstance().logNavigation(
                            fromScreen = "debug",
                            toScreen = "map"
                        )
                        navController.popBackStack() 
                    }
                )
            }
            composable("calibration") {
                // Track screen view
                AnalyticsManager.getInstance().logScreenView(
                    screenName = "Calibration Screen",
                    screenClass = "CalibrationScreen"
                )
                CalibrationScreen(
                    onNavigateBack = { 
                        // Track navigation event
                        AnalyticsManager.getInstance().logNavigation(
                            fromScreen = "calibration",
                            toScreen = "debug"
                        )
                        navController.popBackStack() 
                    }
                )
            }
        }
    }
}

@Composable
fun MapPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "Map Screen - To be implemented",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun DebugPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "Debug Screen - To be implemented",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
fun CalibrationPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "Calibration Screen - To be implemented",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen()
    }
}
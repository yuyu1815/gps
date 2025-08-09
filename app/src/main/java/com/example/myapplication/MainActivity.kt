package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.myapplication.service.VersionChecker
import com.example.myapplication.service.LanguageManager
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.slam.ArCoreManager
import com.example.myapplication.wifi.FingerprintGenerator
import com.example.myapplication.wifi.PositionEstimator
import com.example.myapplication.wifi.WifiScanner
import timber.log.Timber
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.presentation.viewmodel.MainViewModel
import com.example.myapplication.service.AnalyticsManager
import com.example.myapplication.ui.screen.CalibrationScreen
import com.example.myapplication.ui.screen.DebugScreen
import com.example.myapplication.ui.screen.MapScreen
import com.example.myapplication.ui.screen.SettingsScreen
import com.example.myapplication.ui.screen.WifiScreen
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.android.ext.android.inject
import com.example.myapplication.service.PdrTracker
import com.example.myapplication.service.FusionCoordinator
import com.example.myapplication.service.EventBus
import com.example.myapplication.ui.screen.SensorDiagnosticScreen
import com.example.myapplication.ui.screen.Sensor3DTestScreen
import com.example.myapplication.ui.component.AccelerometerCompass
import kotlinx.coroutines.withTimeout
import androidx.compose.material.icons.filled.ArrowBack
import com.example.myapplication.ui.screen.InitLogsScreen
import org.koin.androidx.compose.getKoin

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : AppCompatActivity() {

    private val wifiScanner: WifiScanner by inject()
    private val sensorMonitor: SensorMonitor by inject()
    private lateinit var fingerprintGenerator: FingerprintGenerator
    private lateinit var positionEstimator: PositionEstimator
    private val arCoreManager: ArCoreManager by inject()
    private val pdrTracker: PdrTracker by inject()
    private val fusionCoordinator: FusionCoordinator by inject()
    private lateinit var languageManager: LanguageManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if (isGranted) {
                // 非同期でARCoreセッションを作成
                lifecycleScope.launch(Dispatchers.IO) {
                    arCoreManager.createSession()
                    withContext(Dispatchers.Main) {
                        Timber.d("Camera permission granted, ARCore session created")
                    }
                }
            } else {
                Timber.w("Camera permission denied - falling back to WiFi+PDR mode")
                // Show user notification about fallback mode
                showArCoreFallbackNotification()
            }
        }

    // 位置情報/近接Wi‑Fiなど複数権限リクエスト用
    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val nearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                results[Manifest.permission.NEARBY_WIFI_DEVICES] ?: true
            } else true
            val postNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                results[Manifest.permission.POST_NOTIFICATIONS] ?: true
            } else true
            Timber.d("[INIT][Main] Permission results: fine=$fine, nearbyWifi=$nearbyWifi, postNotif=$postNotif")
            lifecycleScope.launch(Dispatchers.Main) {
                wifiScanner.updatePermissionState()
                if (wifiScanner.hasRequiredPermissions.value) {
                    val started = wifiScanner.startPeriodicScanning()
                    if (started) Timber.d("[INIT][Main] Wi‑Fi periodic scanning started after permission grant")
                }
            }
        }

    // State for update notification
    private val updateInfo = mutableStateOf<VersionChecker.VersionCheckResult.UpdateAvailable?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 言語管理を初期化
        languageManager = LanguageManager(this)
        
        // 言語設定を適用
        applyLanguageSettings()

        // SensorMonitorをライフサイクルに登録
        lifecycle.addObserver(sensorMonitor)

        // UIを即座に表示
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

                MainScreen(windowSizeClass = windowSizeClass, startAtSettings = !hasStartupPermissions())
            }
        }

        // 重い初期化処理を非同期で実行
        lifecycleScope.launch(Dispatchers.IO) {
            initializeServicesAsync()
        }

        // イベントバスの監視を開始
        lifecycleScope.launch(Dispatchers.Main) {
            EventBus.events.collect { event ->
                when (event) {
                    is EventBus.AppEvent.LanguageChanged -> {
                        changeLanguage(event.languageCode)
                    }
                    is EventBus.AppEvent.SettingsUpdated -> {
                        Timber.d("Settings updated: ${event.settingKey} = ${event.value}")
                    }
                    is EventBus.AppEvent.NavigationRequested -> {
                        Timber.d("Navigation requested: ${event.destination}")
                    }
                }
            }
        }
    }

    /**
     * 言語設定を適用
     */
    private fun applyLanguageSettings() {
        try {
            val currentLanguage = languageManager.getCurrentLanguage()
            val updatedContext = languageManager.updateResources(currentLanguage)
            
            // リソースを更新
            resources.updateConfiguration(
                updatedContext.resources.configuration,
                resources.displayMetrics
            )
            
            Timber.d("Language settings applied: $currentLanguage")
        } catch (e: Exception) {
            Timber.e(e, "Error applying language settings")
        }
    }

    /**
     * 言語設定を変更
     */
    fun changeLanguage(languageCode: String) {
        try {
            languageManager.setLanguage(languageCode)
            applyLanguageSettings()
            
            // アクティビティを再起動して言語変更を反映
            recreate()
            
            Timber.d("Language changed to: $languageCode")
        } catch (e: Exception) {
            Timber.e(e, "Error changing language")
        }
    }

    private suspend fun initializeServicesAsync() {
        try {
            // 初期化タイムアウトを設定（30秒）
            withTimeout(30000L) {
                val initStart = System.currentTimeMillis()
                Timber.d("[INIT][Main] initializeServicesAsync start")
                // 軽量な初期化を先に実行
                fingerprintGenerator = FingerprintGenerator(wifiScanner)
                positionEstimator = PositionEstimator()
                
                // SensorMonitorの監視を開始
                withContext(Dispatchers.Main) {
                    // この環境での失敗回避として、まずハンドラ無し登録を有効化
                    sensorMonitor.setForceNoHandlerRegistration(true)
                    sensorMonitor.startMonitoring()
                    Timber.d("[INIT][Main] SensorMonitor started")
                }
                
                // 権限チェックとWi-Fiスキャン開始
                withContext(Dispatchers.Main) {
                    wifiScanner.updatePermissionState()
                    if (wifiScanner.hasRequiredPermissions.value) {
                        val scanStarted = wifiScanner.startPeriodicScanning()
                        if (scanStarted) {
                            Timber.d("[INIT][Main] Wi‑Fi periodic scanning started successfully")
                        } else {
                            Timber.w("[INIT][Main] Failed to start Wi‑Fi periodic scanning")
                        }
                    } else {
                        Timber.w("[INIT][Main] Cannot start Wi‑Fi scanning: missing required Wi‑Fi permissions")
                        // 不足権限をまとめてリクエスト
                        val toRequest = mutableListOf<String>()
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            toRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                                toRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                            }
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (toRequest.isNotEmpty()) {
                            requestMultiplePermissionsLauncher.launch(toRequest.toTypedArray())
                        }
                    }
                }

                // Wi-Fiスキャン結果の監視を非同期で開始
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        wifiScanner.scanResults
                            .onEach { scanResults ->
                                try {
                                    if (scanResults.isNotEmpty()) {
                                        val fingerprint = fingerprintGenerator.generateFingerprintMap(scanResults)
                                        Timber.d("Wi-Fi Fingerprint generated: ${fingerprint.size} APs")
                                        val position = positionEstimator.estimatePosition(fingerprint)
                                        if (position != null) {
                                            Timber.d("Estimated Position: $position")
                                        } else {
                                            Timber.d("No position estimated from Wi-Fi fingerprint")
                                        }
                                    } else {
                                        Timber.d("No Wi-Fi scan results available")
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Error processing Wi-Fi scan results")
                                }
                            }
                            .launchIn(lifecycleScope)
                    } catch (e: Exception) {
                        Timber.e(e, "Error setting up Wi-Fi scan monitoring")
                    }
                }

                // ARCoreの初期化を非同期で実行
                if (arCoreManager.isArCoreSupported()) {
                    withContext(Dispatchers.Main) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            arCoreManager.createSession()
                            Timber.d("[INIT][Main] ARCore supported and camera permission granted")
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Timber.w("[INIT][Main] ARCore not supported - running in Wi‑Fi+PDR mode")
                        showArCoreFallbackNotification()
                    }
                }

                // ARCore機能ポイントの監視を非同期で開始
                lifecycleScope.launch(Dispatchers.IO) {
                    while (true) {
                        val featurePoints = arCoreManager.getFeaturePoints()
                        if (featurePoints.isNotEmpty()) {
                            Timber.d("Feature points: ${featurePoints.size}")
                        }
                        delay(100)
                    }
                }

                // バージョンチェックを非同期で実行
                withContext(Dispatchers.IO) {
                    VersionChecker.setUpdateCallback { updateAvailable ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            updateInfo.value = updateAvailable
                        }
                    }
                }

                // PDRトラッキングとFusionCoordinatorを非同期で開始
                withContext(Dispatchers.IO) {
                    pdrTracker.startTracking()
                    fusionCoordinator.start()
                }

                // センサーが全滅しているケースの自動フォールバック（合成モード）
                withContext(Dispatchers.Main) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(6000)
                        val reg = sensorMonitor.registrationStates.value
                        val allError = listOf(
                            reg.accelerometer,
                            reg.gyroscope,
                            reg.magnetometer,
                            reg.linearAcceleration,
                            reg.gravity
                        ).all { it == com.example.myapplication.service.RegistrationState.ERROR }
                        if (allError) {
                            Timber.w("[INIT][Main] All sensors failed to register after 6s. Enabling synthetic mode.")
                            sensorMonitor.enableSyntheticMode("All sensors ERROR after timeout")
                        }
                    }
                }
                val took = System.currentTimeMillis() - initStart
                Timber.d("[INIT][Main] initializeServicesAsync finished in ${took}ms")
            }
        } catch (e: Exception) {
            Timber.e(e, "[INIT][Main] Error during async initialization")
        }
    }

    private fun hasStartupPermissions(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val postNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val nearbyWifiGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else true

        // 最低限: 位置情報 +（API条件に応じて）通知/Wi‑Fi近接 + カメラ
        return fineLocationGranted && postNotificationsGranted && nearbyWifiGranted && cameraGranted
    }

    override fun onResume() {
        super.onResume()
        // ARCoreセッションの再開を非同期で実行
        lifecycleScope.launch(Dispatchers.IO) {
            arCoreManager.resumeSession()
            // Ensure tracking is running when activity returns to foreground
            pdrTracker.startTracking()
            fusionCoordinator.start()
        }
    }

    override fun onPause() {
        super.onPause()
        // ARCoreセッションの一時停止を非同期で実行
        lifecycleScope.launch(Dispatchers.IO) {
            arCoreManager.pauseSession()
            pdrTracker.stopTracking()
            fusionCoordinator.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // クリーンアップを非同期で実行
        lifecycleScope.launch(Dispatchers.IO) {
            wifiScanner.stopScan()
            arCoreManager.closeSession()
        }
        
        // SensorMonitorをライフサイクルから削除
        lifecycle.removeObserver(sensorMonitor)
    }

    private fun showArCoreFallbackNotification() {
        // Show a user-friendly notification about fallback mode
        lifecycleScope.launch(Dispatchers.Main) {
            // Use a simple Toast for now since Snackbar requires Compose context
            android.widget.Toast.makeText(
                this@MainActivity,
                "ARCore not available - using WiFi + PDR mode for positioning",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        Timber.i("ARCore fallback notification shown - WiFi+PDR mode active")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass? = null,
    startAtSettings: Boolean = false
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
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(id = R.string.nav_map)) },
                        label = { Text(stringResource(id = R.string.nav_map)) },
                        selected = currentRoute == "map",
                        onClick = {
                            // 即座にUIを更新
                            viewModel.navigateTo(MainViewModel.NavigationState.Map)
                            // 非同期でナビゲーション処理を実行
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                // Track navigation event (stub)
                                AnalyticsManager.getInstance().logNavigation(
                                    fromScreen = currentRoute,
                                    toScreen = "map"
                                )
                                navController.navigate("map") {
                                    popUpTo("map") { inclusive = true }
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.nav_settings)) },
                        label = { Text(stringResource(id = R.string.nav_settings)) },
                        selected = currentRoute == "settings",
                        onClick = {
                            // 即座にUIを更新
                            viewModel.navigateTo(MainViewModel.NavigationState.Settings)
                            // 非同期でナビゲーション処理を実行
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                AnalyticsManager.getInstance().logNavigation(
                                    fromScreen = currentRoute,
                                    toScreen = "settings"
                                )
                                navController.navigate("settings") {
                                    popUpTo("map")
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Build, contentDescription = stringResource(id = R.string.nav_debug)) },
                        label = { Text(stringResource(id = R.string.nav_debug)) },
                        selected = currentRoute == "debug",
                        onClick = {
                            // 即座にUIを更新
                            viewModel.navigateTo(MainViewModel.NavigationState.Debug)
                            // 非同期でナビゲーション処理を実行
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                AnalyticsManager.getInstance().logNavigation(
                                    fromScreen = currentRoute,
                                    toScreen = "debug"
                                )
                                navController.navigate("debug") {
                                    popUpTo("map")
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Wifi, contentDescription = "Wi‑Fi") },
                        label = { Text("Wi‑Fi") },
                        selected = currentRoute == "wifi",
                        onClick = {
                            // 即座にUIを更新
                            viewModel.navigateTo(MainViewModel.NavigationState.Wifi)
                            // 非同期でナビゲーション処理を実行
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                AnalyticsManager.getInstance().logNavigation(
                                    fromScreen = currentRoute,
                                    toScreen = "wifi"
                                )
                                navController.navigate("wifi") {
                                    popUpTo("map")
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startAtSettings) "settings" else "map",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("map") {
                // Track screen view
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Map Screen",
                        screenClass = "MapScreen"
                    )
                }
                MapScreen(
                    isDebugMode = false,
                    windowSizeClass = windowSizeClass,
                    onNavigateToSettings = {
                        viewModel.navigateTo(MainViewModel.NavigationState.Settings)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "map",
                                toScreen = "init_logs"
                            )
                            navController.navigate("init_logs")
                        }
                    }
                )
            }
            composable("settings") {
                // Track screen view
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Settings Screen",
                        screenClass = "SettingsScreen"
                    )
                }
                SettingsScreen(
                    onNavigateBack = {
                        viewModel.navigateTo(MainViewModel.NavigationState.Map)
                        // Track navigation event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "settings",
                                toScreen = "map"
                            )
                        }
                        navController.popBackStack()
                    },
                    onNavigateToLogs = {
                        // Track navigation event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "settings",
                                toScreen = "logs"
                            )
                        }
                        navController.navigate("logs")
                    }
                )
            }
            composable("logs") {
                // Track screen view
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Log Management Screen",
                        screenClass = "LogManagementScreen"
                    )
                }
                // LogManagementScreen removed - placeholder
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(
                        text = "Log Management Screen - Removed",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            composable("debug") {
                // Track screen view
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Debug Screen",
                        screenClass = "DebugScreen"
                    )
                }
                DebugScreen(
                    onNavigateBack = {
                        // Track navigation event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "debug",
                                toScreen = "map"
                            )
                        }
                        navController.popBackStack()
                    },
                    onNavigateToSensorDiagnostic = {
                        // Track navigation event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "debug",
                                toScreen = "sensor_diagnostic"
                            )
                        }
                        navController.navigate("sensor_diagnostic")
                    },
                    onNavigateToSensor3DTest = {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "debug",
                                toScreen = "sensor_3d_test"
                            )
                        }
                        navController.navigate("sensor_3d_test")
                    },
                    onNavigateToAccelerometerCompass = {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "debug",
                                toScreen = "accelerometer_compass"
                            )
                        }
                        navController.navigate("accelerometer_compass")
                    }
                )
            }
            composable("wifi") {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Wi‑Fi Screen",
                        screenClass = "WifiScreen"
                    )
                }
                WifiScreen(
                    onNavigateBack = {
                        viewModel.navigateTo(MainViewModel.NavigationState.Map)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "wifi",
                                toScreen = "map"
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable("calibration") {
                // Track screen view
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Calibration Screen",
                        screenClass = "CalibrationScreen"
                    )
                }
                CalibrationScreen(
                    onNavigateBack = {
                        // Track navigation event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "calibration",
                                toScreen = "debug"
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable("sensor_diagnostic") {
                // Track screen view
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    AnalyticsManager.getInstance().logScreenView(
                        screenName = "Sensor Diagnostic Screen",
                        screenClass = "SensorDiagnosticScreen"
                    )
                }
                SensorDiagnosticScreen(
                    onNavigateBack = {
                        // Track navigation event
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            AnalyticsManager.getInstance().logNavigation(
                                fromScreen = "sensor_diagnostic",
                                toScreen = "debug"
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable("sensor_3d_test") {
                Sensor3DTestScreen(onBackPressed = { navController.popBackStack() })
            }
            composable("accelerometer_compass") {
                // Lightweight host for AccelerometerCompass demo (live data)
                androidx.compose.material3.Scaffold(
                    topBar = {
                        androidx.compose.material3.TopAppBar(
                            title = { androidx.compose.material3.Text("加速度コンパス") },
                            navigationIcon = {
                                androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = "戻る"
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    val koin = getKoin()
                    val sensorMonitor = koin.get<com.example.myapplication.service.SensorMonitor>()
                    val data by sensorMonitor.sensorData.collectAsState(initial = null)
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        AccelerometerCompass(
                            accelerationX = data?.accelerometer?.x ?: 0f,
                            accelerationY = data?.accelerometer?.y ?: 0f,
                            accelerationZ = data?.accelerometer?.z ?: 0f,
                            isAnimated = true
                        )
                    }
                }
            }
            composable("init_logs") {
                InitLogsScreen(onBack = { navController.popBackStack() })
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
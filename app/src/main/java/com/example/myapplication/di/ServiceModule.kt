package com.example.myapplication.di

import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase
import com.example.myapplication.service.BatteryMonitor
import com.example.myapplication.service.BatteryOptimizer
import com.example.myapplication.service.LogFileManager
import com.example.myapplication.service.LowPowerMode
import com.example.myapplication.service.PdrTracker
import com.example.myapplication.service.PerformanceMetricsCollector
import com.example.myapplication.service.FusionCoordinator
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.StaticDetector
import com.example.myapplication.wifi.WifiScanner
import com.example.myapplication.wifi.WifiFingerprintManager
import com.example.myapplication.wifi.WifiPositionEstimator
import com.example.myapplication.slam.ArCoreManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module that provides service-related dependencies.
 * Heavy initialization is deferred to prevent blocking the main thread.
 */
val serviceModule = module {
    // Provide LogFileManager as a singleton - 軽量な初期化
    single {
        LogFileManager(androidContext())
    }

    // Provide LowPowerMode as a singleton - 軽量な初期化
    single {
        LowPowerMode(androidContext())
    }
    
    // Provide BatteryOptimizer as a singleton - 軽量な初期化
    single {
        BatteryOptimizer(
            context = androidContext(),
            lowPowerMode = get()
        )
    }
    
    // BLE components removed for Hyper-like minimal build
    
    // Provide SensorMonitor as a singleton - 遅延初期化
    single {
        SensorMonitor(androidContext()).apply {
            // センサー初期化は遅延実行
        }
    }

    // Provide WifiScanner as a singleton - 遅延初期化
    single {
        WifiScanner(androidContext()).apply {
            // Wi-Fi初期化は遅延実行
        }
    }

    // Provide Wi‑Fi fingerprinting components - 軽量な初期化
    single { WifiFingerprintManager(get()) }
    single { WifiPositionEstimator(get(), get(), get()) }

    // Provide SLAM manager (real ARCore manager) - 遅延初期化
    single { 
        ArCoreManager(androidContext()).apply {
            // ARCore初期化は遅延実行
        }
    }
    
    // Provide FuseSensorDataUseCase as a factory - 軽量な初期化
    factory {
        FuseSensorDataUseCase(
            positionRepository = get()
        )
    }
    
    // Provide PdrTracker as a singleton - 遅延初期化
    single {
        PdrTracker(
            sensorMonitor = get(),
            positionRepository = get(),
            detectStepUseCase = get(),
            estimateHeadingUseCase = get(),
            estimateStepLengthUseCase = get(),
            updatePdrPositionUseCase = get(),
            fuseSensorDataUseCase = get()
        ).apply {
            // PDR初期化は遅延実行
        }
    }

    // Provide FusionCoordinator - 遅延初期化
    single {
        FusionCoordinator(
            wifiScanner = get(),
            wifiPositionEstimator = get(),
            arCoreManager = get(),
            fuseSensorDataUseCase = get(),
            positionRepository = get(),
            settingsRepository = get()
        ).apply {
            // Fusion初期化は遅延実行
        }
    }
    
    // Metrics collector retained (optional); comment out if not needed
    single { PerformanceMetricsCollector(androidContext()) }
    
    // Beacon-related services removed for minimal build
    
    // Provide BatteryMonitor as a singleton - 軽量な初期化
    single {
        BatteryMonitor(androidContext())
    }
}
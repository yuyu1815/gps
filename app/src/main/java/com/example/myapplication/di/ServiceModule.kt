package com.example.myapplication.di

import com.example.myapplication.service.BackgroundProcessingManager
import com.example.myapplication.service.BatteryOptimizer
import com.example.myapplication.service.BeaconDiscovery
import com.example.myapplication.service.BeaconMaintenanceTool
import com.example.myapplication.service.BleScanner
import com.example.myapplication.service.BleScanService
import com.example.myapplication.service.LowPowerMode
import com.example.myapplication.service.PdrTracker
import com.example.myapplication.service.PerformanceMetricsCollector
import com.example.myapplication.service.RemoteConfigurationService
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.SystemHealthMonitor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module that provides service-related dependencies.
 */
val serviceModule = module {
    // Provide LowPowerMode as a singleton
    single {
        LowPowerMode(androidContext())
    }
    
    // Provide BatteryOptimizer as a singleton
    single {
        BatteryOptimizer(
            context = androidContext(),
            lowPowerMode = get()
        )
    }
    
    // Provide BackgroundProcessingManager as a singleton
    single {
        BackgroundProcessingManager(androidContext())
    }
    
    // Provide BeaconDiscovery as a singleton
    single { 
        BeaconDiscovery(get()) 
    }
    
    // Provide BleScanner factory (new instance each time)
    factory { 
        BleScanner(
            context = androidContext(),
            beaconRepository = get(),
            updateBeaconRssiUseCase = get(),
            updateBeaconStalenessUseCase = get(),
            beaconDiscovery = get()
        ) 
    }
    
    // Provide SensorMonitor as a singleton
    single {
        SensorMonitor(androidContext())
    }
    
    // Provide PdrTracker as a singleton
    single {
        PdrTracker(
            sensorMonitor = get(),
            positionRepository = get(),
            detectStepUseCase = get(),
            estimateHeadingUseCase = get(),
            estimateStepLengthUseCase = get(),
            updatePdrPositionUseCase = get()
        )
    }
    
    // Provide PerformanceMetricsCollector as a singleton
    single {
        PerformanceMetricsCollector(androidContext())
    }
    
    // Provide BeaconMaintenanceTool as a singleton
    single {
        BeaconMaintenanceTool(
            beaconRepository = get()
        )
    }
    
    // Provide RemoteConfigurationService as a singleton
    single {
        RemoteConfigurationService(
            beaconRepository = get(),
            settingsRepository = get()
        )
    }
    
    // Provide SystemHealthMonitor as a singleton
    single {
        SystemHealthMonitor(
            context = androidContext(),
            beaconRepository = get()
        )
    }
}
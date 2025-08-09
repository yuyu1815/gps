package com.example.myapplication.di

import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.presentation.viewmodel.MainViewModel
import com.example.myapplication.presentation.viewmodel.MapViewModel
import com.example.myapplication.presentation.viewmodel.SettingsViewModel
import com.example.myapplication.presentation.viewmodel.SensorDiagnosticViewModel
import com.example.myapplication.presentation.viewmodel.WifiViewModel
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.PdrTracker
import com.example.myapplication.wifi.WifiScanner
import com.example.myapplication.wifi.WifiFingerprintManager
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module that provides ViewModel-related dependencies.
 */
val viewModelModule = module {
    // Provide MainViewModel
    viewModel {
        MainViewModel()
    }

    // Provide MapViewModel
    viewModel {
        MapViewModel(
            mapRepository = get<IMapRepository>(),
            positionRepository = get<IPositionRepository>(),
            sensorMonitor = get<SensorMonitor>(),
            pdrTracker = get<PdrTracker>(),
            wifiScanner = get<WifiScanner>()
        )
    }

    // Provide SettingsViewModel
    viewModel {
        SettingsViewModel(
            settingsRepository = get<ISettingsRepository>(),
            wifiFingerprintManager = get<WifiFingerprintManager>()
        )
    }

    // Provide WifiViewModel
    viewModel {
        WifiViewModel(
            wifiScanner = get(),
            settingsRepository = get()
        )
    }

    // Provide SensorDiagnosticViewModel
    viewModel {
        SensorDiagnosticViewModel()
    }
}
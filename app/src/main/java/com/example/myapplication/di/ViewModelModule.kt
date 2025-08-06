package com.example.myapplication.di

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.presentation.viewmodel.BeaconMaintenanceViewModel
import com.example.myapplication.presentation.viewmodel.MainViewModel
import com.example.myapplication.presentation.viewmodel.MapViewModel
import com.example.myapplication.presentation.viewmodel.PerformanceMetricsViewModel
import com.example.myapplication.presentation.viewmodel.SettingsViewModel
import com.example.myapplication.service.BeaconMaintenanceTool
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module that provides ViewModel dependencies.
 */
val viewModelModule = module {
    // Provide MainViewModel (no repository dependencies)
    viewModel { 
        MainViewModel() 
    }
    
    // Provide MapViewModel with repository dependencies
    viewModel { 
        MapViewModel(
            mapRepository = get<IMapRepository>(),
            positionRepository = get<IPositionRepository>(),
            beaconRepository = get<IBeaconRepository>()
        ) 
    }
    
    // Provide SettingsViewModel with repository dependencies
    viewModel { 
        SettingsViewModel(
            beaconRepository = get<IBeaconRepository>(),
            settingsRepository = get<ISettingsRepository>()
        ) 
    }
    
    // Provide PerformanceMetricsViewModel
    viewModel {
        PerformanceMetricsViewModel()
    }
    
    // Provide BeaconMaintenanceViewModel
    viewModel {
        BeaconMaintenanceViewModel(
            beaconRepository = get<IBeaconRepository>(),
            maintenanceTool = get<BeaconMaintenanceTool>()
        )
    }
}
package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.repository.BeaconRepository
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.data.repository.MapRepository
import com.example.myapplication.data.repository.PositionRepository
import com.example.myapplication.data.repository.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module that provides repository dependencies.
 */
val repositoryModule = module {
    // Provide BeaconRepository as a singleton
    single<IBeaconRepository> { 
        BeaconRepository(androidContext()) 
    }
    
    // Provide MapRepository as a singleton
    single<IMapRepository> { 
        MapRepository() 
    }
    
    // Provide PositionRepository as a singleton
    single<IPositionRepository> { 
        PositionRepository(androidContext()) 
    }
    
    // Provide SettingsRepository as a singleton
    single<ISettingsRepository> { 
        SettingsRepository(androidContext()) 
    }
}
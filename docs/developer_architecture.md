# Android Indoor Positioning Application - Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [Architecture Principles](#architecture-principles)
3. [Project Structure](#project-structure)
4. [Core Components](#core-components)
5. [Data Flow](#data-flow)
6. [Dependency Injection](#dependency-injection)
7. [Threading Model](#threading-model)
8. [Testing Strategy](#testing-strategy)
9. [Extension Points](#extension-points)

## Overview

The Android Indoor Positioning Application follows the MVVM (Model-View-ViewModel) architecture pattern with a clean architecture approach. This document provides a comprehensive overview of the application's architecture, explaining the key components, their interactions, and the design decisions behind them.

## Architecture Principles

The application architecture is guided by the following principles:

1. **Separation of Concerns**: Each component has a specific responsibility and should not handle tasks outside its domain.
2. **Single Responsibility**: Classes should have only one reason to change.
3. **Dependency Inversion**: High-level modules should not depend on low-level modules. Both should depend on abstractions.
4. **Testability**: The architecture facilitates comprehensive testing at all levels.
5. **Modularity**: Components are designed to be modular and reusable.
6. **Lifecycle Awareness**: Components respect and properly handle Android lifecycle events.

## Project Structure

The project follows a feature-based package structure with clear separation of concerns:

```
app/src/main/java/com/example/myapplication/
├── domain/                 # Business logic and models
│   ├── model/              # Data models
│   └── usecase/            # Business logic implementation
├── data/                   # Data handling
│   ├── repository/         # Data access layer
│   └── parser/             # Data parsing utilities
├── service/                # Background services
├── ui/                     # User interface
│   ├── component/          # Reusable UI components
│   ├── screen/             # Application screens
│   └── theme/              # UI theme definitions
├── presentation/           # ViewModels and UI state management
└── di/                     # Dependency injection
```

## Core Components

### Domain Layer

The domain layer contains the core business logic and is independent of other layers.

#### Models

Domain models represent the core data structures:

- `Beacon`: Represents a BLE beacon with its properties and state
- `IndoorMap`: Represents an indoor map with its properties and metadata
- `UserPosition`: Represents the user's position with uncertainty information
- `SensorData`: Contains sensor readings from various device sensors
- `KalmanFilter`: Implements the Kalman filter algorithm for position estimation

#### Use Cases

Use cases implement specific business logic operations:

- BLE-related use cases: `StartBleScanningUseCase`, `GetBeaconsUseCase`, etc.
- Position calculation use cases: `CalculatePositionUseCase`, `TriangulatePositionUseCase`, etc.
- PDR-related use cases: `DetectStepUseCase`, `EstimateHeadingUseCase`, etc.
- Sensor fusion use cases: `FuseSensorDataUseCase`
- Map-related use cases: `LoadMapsFromDirectoryUseCase`, `TransformCoordinatesUseCase`, etc.

### Data Layer

The data layer handles data access and persistence.

#### Repositories

Repositories provide a clean API for data access:

- `BeaconRepository`: Manages beacon data
- `MapRepository`: Manages indoor map data
- `PositionRepository`: Manages position data
- `SettingsRepository`: Manages application settings

### Service Layer

The service layer handles background operations and device interactions:

- `BleScanService`: Manages BLE scanning in the background
- `SensorService`: Manages sensor monitoring
- `LogFileManager`: Handles log file operations
- `StaticDetector`: Detects when the device is stationary
- `BatteryOptimizer`: Optimizes operations based on battery level

### Presentation Layer

The presentation layer handles UI state and user interactions.

#### ViewModels

ViewModels manage UI state and handle user interactions:

- `MainViewModel`: Manages the main application state
- `MapViewModel`: Manages the map view state
- `SettingsViewModel`: Manages settings state
- `LogManagementViewModel`: Manages log operations

### UI Layer

The UI layer handles the visual representation and user interactions.

#### Screens

Screens represent the main application views:

- `MapScreen`: Displays the indoor map and user position
- `SettingsScreen`: Allows users to configure the application
- `CalibrationScreen`: Provides tools for calibration
- `LogManagementScreen`: Manages log files
- `DebugScreen`: Provides debugging information

#### Components

Components are reusable UI elements:

- `MapRenderer`: Renders the indoor map
- `BeaconDebugVisualizer`: Visualizes beacon placements
- `DistanceUncertaintyVisualizer`: Visualizes position uncertainty
- `DebugOverlay`: Displays debug information

## Data Flow

The data flow in the application follows a unidirectional pattern:

1. **User Interaction**: User interacts with the UI
2. **ViewModel Processing**: ViewModel processes the interaction and calls appropriate use cases
3. **Use Case Execution**: Use cases execute business logic, potentially accessing repositories
4. **Repository Access**: Repositories access data sources (local storage, sensors, BLE)
5. **State Update**: Results flow back through the same path, updating the UI state
6. **UI Rendering**: UI renders based on the updated state

### Example: Position Update Flow

1. BLE scanner detects beacons and updates the BeaconRepository
2. SensorMonitor collects sensor data
3. PositionViewModel requests position update from CalculatePositionUseCase
4. CalculatePositionUseCase combines beacon data and sensor data to calculate position
5. Position is stored in PositionRepository
6. PositionViewModel observes PositionRepository and updates UI state
7. MapScreen renders the updated position

## Dependency Injection

The application uses a simple dependency injection framework to manage component dependencies:

- `RepositoryModule`: Provides repository instances
- `ServiceModule`: Provides service instances
- `ViewModelModule`: Provides ViewModel instances

Dependencies are injected through constructor injection, making components more testable and modular.

## Threading Model

The application uses Kotlin coroutines for asynchronous operations:

- UI operations run on the Main dispatcher
- I/O operations (file access, database) run on the IO dispatcher
- CPU-intensive operations run on the Default dispatcher
- Background services use WorkManager for long-running tasks

ViewModels use viewModelScope to launch coroutines that are automatically cancelled when the ViewModel is cleared.

## Testing Strategy

The application follows a comprehensive testing strategy:

### Unit Tests

Unit tests focus on testing individual components in isolation:

- Use case tests: Verify business logic with mocked dependencies
- Repository tests: Verify data access logic with mocked data sources
- ViewModel tests: Verify UI state management with mocked use cases

### Integration Tests

Integration tests verify the interaction between components:

- Repository integration tests: Verify repository interaction with actual data sources
- Service integration tests: Verify service interaction with actual system components
- ViewModel integration tests: Verify ViewModel interaction with actual use cases

### UI Tests

UI tests verify the user interface behavior:

- Screen tests: Verify screen rendering and user interactions
- End-to-end tests: Verify complete user flows

### Test Utilities

The application includes several test utilities:

- `MockBleScanner`: Simulates BLE scanning for testing
- `SensorDataSimulator`: Generates simulated sensor data for testing

## Extension Points

The application is designed to be extensible in several areas:

### Adding New Positioning Methods

To add a new positioning method:

1. Create new domain models if needed
2. Implement new use cases for the positioning method
3. Update the sensor fusion logic to incorporate the new method
4. Update the UI to display the new positioning information

### Adding New Map Types

To support a new map format:

1. Update the `IndoorMap` model to support the new format
2. Implement a new parser in the data layer
3. Update the `MapRepository` to handle the new format
4. Update the `MapRenderer` to render the new format

### Adding New Sensors

To incorporate data from new sensors:

1. Update the `SensorData` model to include the new sensor data
2. Update the `SensorMonitor` to collect data from the new sensor
3. Implement new use cases to process the sensor data
4. Update the sensor fusion logic to incorporate the new sensor data

### Custom Algorithms

To implement custom algorithms:

1. Create new use cases implementing the custom algorithms
2. Update the appropriate repositories to use the new use cases
3. Update the ViewModels to expose the new functionality
4. Update the UI to allow users to select and configure the new algorithms
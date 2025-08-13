# Android Indoor Positioning Application

An Android application for high-precision indoor location tracking using BLE beacons and smartphone sensors in environments where GPS is unavailable.

## Features

- **BLE Beacon Scanning**: Detects and processes signals from BLE beacons
- **Pedestrian Dead Reckoning (PDR)**: Uses device sensors for continuous position tracking
- **Sensor Fusion**: Combines BLE positioning with PDR for improved accuracy
- **Indoor Mapping**: Displays user position on indoor maps with uncertainty visualization
- **Calibration Tools**: Provides tools for TxPower and environmental factor calibration
- **Debug Overlay**: Visualizes real-time sensor and beacon data
- **Low Power Mode**: Optimizes battery usage with dynamic scanning intervals

## Prerequisites

- Android Studio Flamingo (2023.2.1) or newer
- JDK 11 or newer
- Gradle 8.0 or newer
- Android SDK with API level 35 (compileSdk)
- Minimum supported Android version: API level 30 (minSdk)
- Device with BLE (Bluetooth Low Energy) support
- Device with accelerometer, gyroscope, and magnetometer sensors for full functionality

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/android-indoor-positioning.git
cd android-indoor-positioning
```

### 2. Open in Android Studio

- Launch Android Studio
- Select "Open an existing Android Studio project"
- Navigate to the cloned repository and click "Open"

### 3. Sync Gradle Files

- Wait for the automatic Gradle sync to complete
- If it doesn't start automatically, select "Sync Project with Gradle Files" from the toolbar

### 4. Check SDK Versions

- Open the SDK Manager (Tools > SDK Manager)
- Ensure you have Android SDK Platform 35 installed
- Install any missing SDK components

### 5. Configure Device

- Enable Developer Options on your Android device
- Enable USB Debugging
- Connect your device to your computer

### 6. Run the Application

- Select "Run" from the toolbar
- Choose your connected device
- Wait for the application to build and install

Alternatively, you can build and install using Gradle:

```bash
./gradlew installDebug
```

## Project Structure

- `app/src/main/java/com/example/myapplication/`
  - `domain/`: Core business logic and models
    - `model/`: Data models for beacons, maps, etc.
    - `usecase/`: Business logic implementation
  - `data/`: Data handling
    - `repository/`: Data access layer
    - `parser/`: Data parsing utilities
  - `service/`: Background services
    - BLE scanning, sensor monitoring, etc.
  - `ui/`: User interface
    - `component/`: Reusable UI components
    - `screen/`: Application screens
    - `theme/`: UI theme definitions
  - `presentation/`: ViewModels and UI state management
  - `di/`: Dependency injection

## Testing

### Running Unit Tests

```bash
./gradlew test
```

### Running Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Running Specific Tests

```bash
./gradlew test --tests "com.example.myapplication.TestClassName"
```

## Permissions

The application requires the following permissions:

- `BLUETOOTH`: For BLE scanning
- `BLUETOOTH_ADMIN`: For controlling BLE functionality
- `ACCESS_FINE_LOCATION`: Required for BLE scanning on Android 6.0+
- `ACCESS_COARSE_LOCATION`: Alternative to fine location
- `FOREGROUND_SERVICE`: For background scanning

## Troubleshooting

### BLE Scanning Issues

- Ensure Bluetooth is enabled on your device
- Check that location services are enabled
- Verify that the app has been granted location permissions
- For Android 12+, ensure nearby devices permission is granted

### Sensor Issues

- Verify that your device has the required sensors
- Check sensor calibration (compass may need calibration)
- Ensure the device is held in a stable position during calibration

## License

[Insert your license information here]

## Contact

[Insert your contact information here]
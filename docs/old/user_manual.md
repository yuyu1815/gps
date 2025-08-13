# Android Indoor Positioning Application - User Manual

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Getting Started](#getting-started)
4. [Main Interface](#main-interface)
5. [Indoor Maps](#indoor-maps)
6. [Positioning](#positioning)
7. [Calibration](#calibration)
8. [Settings](#settings)
9. [Debug Mode](#debug-mode)
10. [Logging and Replay](#logging-and-replay)
11. [Troubleshooting](#troubleshooting)
12. [FAQ](#faq)

## Introduction

The Android Indoor Positioning Application provides high-precision location tracking in indoor environments where GPS is unavailable. It uses BLE beacons and smartphone sensors to determine your position with accuracy typically within 1-3 meters.

This user manual will guide you through the installation, setup, and usage of the application, including advanced features like calibration and debugging.

## Installation

### System Requirements

- Android device running Android 11 (API level 30) or higher
- Bluetooth Low Energy (BLE) support
- Accelerometer, gyroscope, and magnetometer sensors
- At least 100MB of free storage space

### Installation Steps

1. Download the APK from the provided source
2. Enable installation from unknown sources in your device settings if needed
3. Open the APK file and follow the installation prompts
4. Grant all requested permissions when prompted

## Getting Started

### First Launch

When you first launch the application, you'll be guided through a setup process:

1. **Permissions**: Grant the required permissions (Bluetooth, Location, Sensors)
2. **Introduction**: A brief tutorial explaining the app's features
3. **Map Selection**: Choose an indoor map or import a new one

### Required Permissions

- **Bluetooth**: Required for BLE beacon scanning
- **Location**: Required for BLE scanning on Android 6.0+
- **Sensors**: Required for motion detection and orientation

## Main Interface

The main interface consists of several key elements:

- **Map View**: Displays the indoor map with your current position
- **Position Indicator**: Shows your current position with an uncertainty circle
- **Navigation Bar**: Access to different app sections
- **Status Bar**: Shows connection status, battery optimization info, and more

### Navigation

The bottom navigation bar provides access to the main sections of the app:

- **Map**: The main map view showing your position
- **Calibration**: Tools for calibrating beacons and sensors
- **Settings**: Application settings and configuration
- **Logs**: Access to data logging and replay features

## Indoor Maps

### Loading Maps

The application supports loading indoor maps from:

1. **Bundled Maps**: Pre-installed maps included with the app
2. **Local Files**: Maps stored on your device
3. **Import**: Import maps from external sources

To load a map:
1. Go to Settings > Maps
2. Select "Load Map"
3. Choose the map source
4. Select the desired map file

### Map Interaction

- **Zoom**: Pinch to zoom in and out
- **Pan**: Drag to move the map
- **Rotate**: Two-finger rotation gesture
- **Reset View**: Double-tap to reset to default view

## Positioning

### Positioning Methods

The application uses multiple methods to determine your position:

- **BLE Positioning**: Uses signal strength from BLE beacons
- **Pedestrian Dead Reckoning (PDR)**: Uses device sensors to track movement
- **Sensor Fusion**: Combines BLE and PDR for improved accuracy

### Position Accuracy

The position indicator shows:

- **Center Dot**: Your estimated position
- **Circle**: Uncertainty radius (larger circle = less certainty)
- **Direction Arrow**: Your estimated heading direction

## Calibration

### Beacon Calibration

For optimal positioning accuracy, beacons should be calibrated:

1. Go to the Calibration section
2. Select "Beacon Calibration"
3. Stand exactly 1 meter from a beacon
4. Follow the on-screen instructions to measure TxPower

### Environmental Calibration

To adjust for specific indoor environments:

1. Go to the Calibration section
2. Select "Environmental Calibration"
3. Walk a predefined path in the environment
4. The app will automatically adjust the environmental factor

### Sensor Calibration

To improve sensor accuracy:

1. Go to the Calibration section
2. Select "Sensor Calibration"
3. Follow the on-screen instructions to calibrate each sensor
4. Keep the device steady during calibration

## Settings

### General Settings

- **Theme**: Light, dark, or system default
- **Units**: Metric or imperial
- **Language**: Select application language
- **Notifications**: Enable/disable notifications

### Positioning Settings

- **Update Frequency**: How often position is updated
- **Accuracy Mode**: Balance between accuracy and battery life
- **Beacon Timeout**: Time before considering a beacon stale
- **PDR Settings**: Step detection sensitivity and parameters

### Power Settings

- **Low Power Mode**: Reduces scanning frequency to save battery
- **Background Operation**: Controls app behavior when in background
- **Battery Optimization**: Adjusts performance based on battery level

## Debug Mode

Debug mode provides additional information for troubleshooting:

1. Go to Settings > Advanced
2. Enable "Debug Mode"
3. Return to the Map view to see the debug overlay

### Debug Overlay

The debug overlay shows:

- **Beacon Information**: MAC addresses, RSSI values, estimated distances
- **Sensor Data**: Raw and filtered accelerometer, gyroscope, and magnetometer data
- **Position Calculations**: Triangulation data and PDR tracking
- **Performance Metrics**: CPU usage, battery impact, and update frequency

## Logging and Replay

### Data Logging

To record positioning data for later analysis:

1. Go to the Logs section
2. Select "Start Logging"
3. Choose what data to log (beacons, sensors, positions)
4. Begin walking around the environment
5. Select "Stop Logging" when finished

### Log Replay

To replay recorded data:

1. Go to the Logs section
2. Select "Replay"
3. Choose a log file
4. Use the playback controls to analyze the recorded session

### Log Export

To export logs for external analysis:

1. Go to the Logs section
2. Select the log file
3. Choose "Export"
4. Select the export format and destination

## Troubleshooting

### Common Issues

#### Bluetooth Issues

- **No Beacons Detected**: Ensure Bluetooth is enabled and beacons are powered on
- **Intermittent Detection**: Check beacon battery levels and placement
- **Poor Signal Quality**: Reduce interference from other devices

#### Positioning Issues

- **Inaccurate Position**: Perform beacon and environmental calibration
- **Position Jumps**: Adjust the filtering settings in Advanced Settings
- **No Position Updates**: Check beacon connectivity and sensor permissions

#### Sensor Issues

- **Erratic Movement**: Calibrate sensors and hold device steadily
- **Incorrect Orientation**: Perform compass calibration
- **Step Detection Problems**: Adjust step detection sensitivity

### Resetting the Application

If you encounter persistent issues:

1. Go to Settings > Advanced
2. Select "Reset Application"
3. Choose what to reset (settings, calibration, or all data)
4. Confirm the reset

## FAQ

### General Questions

**Q: How accurate is the positioning?**  
A: Typically 1-3 meters, depending on beacon density and environmental factors.

**Q: How many beacons do I need?**  
A: At least 3 beacons are recommended for reliable positioning, with more beacons providing better accuracy.

**Q: Does the app work without beacons?**  
A: Limited functionality is available using only PDR, but accuracy will degrade over time without beacon fixes.

### Battery Usage

**Q: How does the app affect battery life?**  
A: Battery impact depends on scanning frequency and positioning mode. Low Power Mode can significantly extend battery life.

**Q: How can I reduce battery consumption?**  
A: Enable Low Power Mode, reduce update frequency, or use the Static Detection feature.

### Privacy and Data

**Q: Does the app collect my location data?**  
A: The app only stores location data locally on your device unless you explicitly enable data sharing.

**Q: Can I delete my recorded data?**  
A: Yes, go to Logs, select the log files, and choose "Delete" to remove them from your device.
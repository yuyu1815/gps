# Android Indoor Positioning Application - Troubleshooting Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Installation Issues](#installation-issues)
3. [Bluetooth and BLE Issues](#bluetooth-and-ble-issues)
4. [Sensor Issues](#sensor-issues)
5. [Positioning Accuracy Issues](#positioning-accuracy-issues)
6. [Map Display Issues](#map-display-issues)
7. [Performance Issues](#performance-issues)
8. [Battery Consumption Issues](#battery-consumption-issues)
9. [Logging and Debugging](#logging-and-debugging)
10. [Common Error Messages](#common-error-messages)
11. [Contact Support](#contact-support)

## Introduction

This troubleshooting guide provides solutions for common issues encountered when using the Android Indoor Positioning Application. It covers installation problems, hardware issues, positioning accuracy, performance concerns, and more.

If you encounter an issue not covered in this guide, please refer to the [Contact Support](#contact-support) section for additional assistance.

## Installation Issues

### Application Won't Install

**Symptoms:**
- Installation fails with an error message
- Installation appears to complete but the app doesn't appear

**Possible Causes and Solutions:**

1. **Insufficient Storage Space**
   - Check available storage: Settings > Storage
   - Free up space by removing unused apps or files
   - Try installing again

2. **Incompatible Android Version**
   - Verify your device runs Android 11 (API level 30) or higher
   - Check: Settings > About phone > Android version
   - If your Android version is too old, consider updating your device if possible

3. **Installation from Unknown Sources Blocked**
   - Enable installation from unknown sources:
     - Settings > Security > Unknown sources (Android 8 or lower)
     - Settings > Apps > Special access > Install unknown apps (Android 9+)
   - Try installing again

4. **Corrupted APK File**
   - Re-download the APK file
   - Verify the file integrity (check the MD5/SHA hash if provided)
   - Try installing again

### Application Crashes on Launch

**Symptoms:**
- App opens briefly then closes immediately
- Error message appears: "App has stopped"

**Possible Causes and Solutions:**

1. **Insufficient Permissions**
   - Open device Settings > Apps > Indoor Positioning > Permissions
   - Ensure all required permissions are granted
   - Restart the app

2. **Corrupted App Data**
   - Go to Settings > Apps > Indoor Positioning
   - Select "Clear data" and "Clear cache"
   - Restart the app

3. **Device Compatibility Issue**
   - Check if your device has all required sensors (accelerometer, gyroscope, magnetometer)
   - Verify Bluetooth LE support: Settings > About phone > Hardware information
   - Contact support if your device meets requirements but still crashes

## Bluetooth and BLE Issues

### No Beacons Detected

**Symptoms:**
- "No beacons found" message
- Empty beacon list
- Position cannot be determined

**Possible Causes and Solutions:**

1. **Bluetooth is Disabled**
   - Check if Bluetooth is enabled in the quick settings panel
   - Go to Settings > Connected devices > Connection preferences > Bluetooth
   - Enable Bluetooth and restart the app

2. **Location Services Disabled**
   - BLE scanning requires location services to be enabled
   - Go to Settings > Location
   - Enable location services and set to high accuracy mode
   - Restart the app

3. **Insufficient Permissions**
   - The app requires location permission for BLE scanning
   - Go to Settings > Apps > Indoor Positioning > Permissions
   - Ensure location permission is granted and set to "Allow all the time"
   - For Android 12+, ensure "Nearby devices" permission is granted

4. **Beacons Not in Range**
   - Verify beacons are powered on and within range (typically 10-50 meters)
   - Check beacon battery levels if applicable
   - Try moving closer to known beacon locations

5. **Beacon Compatibility Issues**
   - Verify beacons are using a supported protocol (iBeacon, Eddystone, etc.)
   - Check beacon configuration (transmission power, advertising interval)
   - Try using a generic BLE scanner app to verify beacons are advertising

### Intermittent Beacon Detection

**Symptoms:**
- Beacons appear and disappear from the list
- Unstable positioning
- Frequent "Beacon lost" notifications

**Possible Causes and Solutions:**

1. **Signal Interference**
   - Check for sources of 2.4 GHz interference (Wi-Fi routers, microwave ovens, etc.)
   - Move away from potential interference sources
   - Try adjusting beacon placement to improve line-of-sight

2. **Battery Optimization Affecting Scanning**
   - Disable battery optimization for the app:
     - Settings > Apps > Indoor Positioning > Battery
     - Select "Don't optimize" or "Unrestricted"
   - Restart the app

3. **Beacon Battery Issues**
   - Check beacon battery levels if available
   - Replace batteries in beacons with weak signals
   - Consider using powered beacons for critical locations

4. **Scan Settings Too Aggressive**
   - Go to Settings > Advanced > BLE Scan Settings
   - Increase scan duration or decrease scan interval
   - Disable "Low Power Mode" temporarily

## Sensor Issues

### Inaccurate Step Detection

**Symptoms:**
- Steps not counted correctly
- Position drifts when using PDR
- Erratic movement detection

**Possible Causes and Solutions:**

1. **Uncalibrated Sensors**
   - Go to Settings > Calibration > Sensor Calibration
   - Follow the instructions to calibrate accelerometer and gyroscope
   - Restart the app after calibration

2. **Irregular Walking Pattern**
   - The step detection algorithm works best with consistent walking
   - Try walking at a steady pace
   - Adjust step detection sensitivity in Settings > Advanced > PDR Settings

3. **Device Position**
   - Hold the device in a consistent position (preferably in hand or stable pocket)
   - Avoid frequent changes in device orientation
   - If using in pocket, specify pocket position in Settings > Advanced > Device Position

4. **Sensor Quality Issues**
   - Some devices have lower quality sensors
   - Enable "Enhanced Sensor Processing" in Settings > Advanced
   - Consider using a device with better sensors for critical applications

### Incorrect Heading Direction

**Symptoms:**
- Direction arrow points in wrong direction
- Position moves in wrong direction when walking
- Heading jumps or spins randomly

**Possible Causes and Solutions:**

1. **Uncalibrated Magnetometer**
   - Go to Settings > Calibration > Sensor Calibration > Magnetometer
   - Follow the figure-eight pattern calibration procedure
   - Repeat calibration away from magnetic interference

2. **Magnetic Interference**
   - Move away from sources of magnetic interference (large metal objects, electronics)
   - Hold the device away from metal objects (keys, magnetic cases)
   - Enable "Gyroscope-based Heading" in Settings > Advanced > PDR Settings

3. **Device Orientation Issues**
   - Hold the device in the orientation specified in the app (typically flat or upright)
   - Specify your carrying position in Settings > Advanced > Device Position
   - Restart heading estimation by pressing the "Reset Heading" button

## Positioning Accuracy Issues

### Large Position Errors

**Symptoms:**
- Position shown far from actual location
- Position jumps between distant points
- Uncertainty circle very large

**Possible Causes and Solutions:**

1. **Insufficient Beacon Coverage**
   - Ensure at least 3-4 beacons are visible from any position
   - Add more beacons in areas with poor coverage
   - Check beacon placement and adjust if necessary

2. **Uncalibrated Beacons**
   - Go to Settings > Calibration > TxPower Calibration
   - Calibrate each beacon following the procedure
   - Verify calibration by checking distance estimates at known distances

3. **Incorrect Environmental Factor**
   - Go to Settings > Calibration > Environmental Calibration
   - Adjust the environmental factor based on the space
   - Typical values: 2.0 (open space) to 4.0 (complex indoor environment)

4. **Multipath Effects**
   - Identify areas with signal reflections (large metal surfaces, glass walls)
   - Add more beacons to improve triangulation
   - Enable "Multipath Mitigation" in Settings > Advanced > Positioning Settings

5. **Algorithm Settings**
   - Try different positioning algorithms in Settings > Advanced > Positioning Algorithm
   - Adjust the Kalman filter parameters for smoother tracking
   - Enable "GDOP Consideration" to account for beacon geometry

### Position Drift Over Time

**Symptoms:**
- Position slowly moves away from actual location
- Increasing error over time
- PDR tracking becomes inaccurate after walking

**Possible Causes and Solutions:**

1. **Sensor Drift**
   - Recalibrate sensors regularly
   - Enable "Drift Compensation" in Settings > Advanced
   - Use beacon fixes to correct drift when available

2. **Step Length Calibration**
   - Go to Settings > Calibration > Sensor Calibration > Step Length
   - Calibrate your step length following the procedure
   - Adjust for different walking speeds if necessary

3. **Heading Drift**
   - Enable "Gyro-Magnetometer Fusion" for better heading stability
   - Perform regular magnetometer calibration
   - Use map constraints to correct heading when possible

## Map Display Issues

### Map Not Loading

**Symptoms:**
- Blank screen where map should appear
- "Map not found" error message
- Map loads partially or incorrectly

**Possible Causes and Solutions:**

1. **Missing or Corrupted Map File**
   - Go to Settings > Maps > Manage Maps
   - Verify the map file exists and is not corrupted
   - Re-import the map if necessary

2. **Incorrect Map Format**
   - Verify the map file is in a supported format
   - Check the map file specifications in the documentation
   - Convert the map to a supported format if necessary

3. **Memory Issues**
   - Close other apps to free up memory
   - Restart the app
   - If using a large map, try using a lower resolution version

### Incorrect Map Scaling

**Symptoms:**
- Objects appear too large or too small on the map
- Distances on the map don't match real-world distances
- Position indicator moves too fast or too slow

**Possible Causes and Solutions:**

1. **Incorrect Pixels Per Meter Setting**
   - Go to Settings > Maps > Edit Map
   - Adjust the "Pixels Per Meter" value
   - Use a known distance to calibrate the scale

2. **Map Metadata Issues**
   - Verify the map metadata contains correct dimensions
   - Update the map configuration file with correct values
   - Re-import the map with correct parameters

3. **Coordinate Transformation Issues**
   - Check the coordinate system used in the map
   - Verify the transformation parameters
   - Adjust the coordinate transformer settings if available

## Performance Issues

### Slow Application Response

**Symptoms:**
- UI feels sluggish
- Delayed position updates
- Long loading times

**Possible Causes and Solutions:**

1. **Device Resource Limitations**
   - Close background apps to free up resources
   - Restart the device
   - Consider using a more powerful device for critical applications

2. **Excessive Logging**
   - Go to Settings > Advanced > Logging
   - Reduce logging level or disable logging
   - Clear old logs to free up storage

3. **Map Rendering Issues**
   - Use a simpler or lower resolution map
   - Disable advanced rendering features
   - Reduce the map update frequency in Settings > Advanced

4. **Algorithm Complexity**
   - Reduce the complexity of positioning algorithms
   - Disable features not needed for your use case
   - Adjust update frequencies to balance accuracy and performance

### High CPU Usage

**Symptoms:**
- Device becomes hot
- Other apps slow down
- Battery drains quickly

**Possible Causes and Solutions:**

1. **Excessive Sensor Sampling**
   - Go to Settings > Advanced > Sensor Settings
   - Reduce sensor sampling rates
   - Enable "Static Detection" to reduce processing when not moving

2. **Debug Features Enabled**
   - Disable debug overlay and visualizations
   - Turn off detailed logging
   - Disable real-time analytics

3. **Background Processing**
   - Limit background processing in Settings > Advanced > Background Mode
   - Use "Essential Only" mode when not actively using the app
   - Enable "Suspend When Static" feature

## Battery Consumption Issues

### Rapid Battery Drain

**Symptoms:**
- Battery depletes much faster than normal
- Device becomes warm
- Battery usage statistics show app using significant power

**Possible Causes and Solutions:**

1. **Continuous BLE Scanning**
   - Enable "Low Power Mode" in Settings > Power
   - Increase scan interval in Settings > Advanced > BLE Scan Settings
   - Enable "Static Detection" to reduce scanning when not moving

2. **High Sensor Sampling Rates**
   - Reduce sensor sampling rates in Settings > Advanced > Sensor Settings
   - Disable sensors not needed for your use case
   - Use "Essential Sensors Only" mode

3. **Background Operation**
   - Limit background operation in Settings > Power > Background Mode
   - Close the app when not in use
   - Enable "Battery Optimization" in Settings > Power

4. **Display Always On**
   - Disable "Keep Screen On" in Settings > Display
   - Reduce screen brightness
   - Enable "Auto-Lock Screen" when static

### Battery Optimization Affecting Functionality

**Symptoms:**
- App stops updating in background
- Missed beacon detections
- Positioning becomes inaccurate after screen off

**Possible Causes and Solutions:**

1. **System Battery Optimization**
   - Disable battery optimization for the app:
     - Settings > Apps > Indoor Positioning > Battery
     - Select "Don't optimize" or "Unrestricted"
   - Restart the app

2. **Aggressive Power Saving Mode**
   - Disable device power saving mode
   - Add the app to power saving exceptions
   - Use foreground service mode in Settings > Advanced > Service Mode

3. **Background Restrictions**
   - Check for background restrictions in device settings
   - Ensure background processing permission is granted
   - Use "High Priority" background mode in Settings > Advanced

## Logging and Debugging

### Enabling Debug Mode

To troubleshoot complex issues, enable debug mode:

1. Go to Settings > Advanced > Developer Options
2. Enable "Debug Mode"
3. Select the debug information to display
4. Use the debug overlay to view real-time data

### Collecting Logs for Support

If you need to share logs with support:

1. Go to Settings > Advanced > Logging
2. Enable "Detailed Logging"
3. Reproduce the issue
4. Go to Settings > Advanced > Logging > Export Logs
5. Share the exported log file with support

### Using the Field Test Tool

For on-site troubleshooting:

1. Go to Settings > Advanced > Field Test Tool
2. Select the test type (Beacon Detection, Positioning Accuracy, etc.)
3. Follow the on-screen instructions
4. Review the test results and recommendations

## Common Error Messages

### "Bluetooth Not Available"

**Possible Causes and Solutions:**
- Bluetooth hardware issue: Restart device
- Bluetooth service crashed: Toggle Bluetooth off and on
- Permission issue: Check Bluetooth permissions in app settings

### "Location Permission Required"

**Possible Causes and Solutions:**
- Permission not granted: Go to Settings > Apps > Indoor Positioning > Permissions
- Location services disabled: Enable location services in device settings
- For Android 12+: Also grant "Nearby devices" permission

### "Insufficient Beacon Data"

**Possible Causes and Solutions:**
- Not enough beacons in range: Move to an area with better coverage
- Beacon signal issues: Check beacon batteries and placement
- Filtering too aggressive: Adjust filtering settings in Advanced Settings

### "Sensor Not Available"

**Possible Causes and Solutions:**
- Device lacks required sensor: Check device specifications
- Sensor service issue: Restart device
- Permission issue: Check sensor permissions in app settings

### "Map Loading Failed"

**Possible Causes and Solutions:**
- File not found: Verify map file exists in the correct location
- Format issue: Check if the map is in a supported format
- Parsing error: Check map file for corruption or format issues

## Contact Support

If you've tried the solutions in this guide and still experience issues, please contact support:

- Email: support@indoorpositioning.example.com
- In-app: Settings > Help > Contact Support
- Website: https://indoorpositioning.example.com/support

When contacting support, please include:
- Device model and Android version
- App version
- Detailed description of the issue
- Steps to reproduce the problem
- Exported logs if available
- Screenshots or videos demonstrating the issue
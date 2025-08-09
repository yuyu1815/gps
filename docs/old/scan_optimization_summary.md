# BLE Scan Interval Optimization Implementation

## Overview

This document summarizes the implementation of scan interval optimization for battery saving in the Android Indoor Positioning Application. The optimization dynamically adjusts BLE scanning parameters based on user activity, device movement, and battery level to balance positioning accuracy with battery consumption.

## Key Components

### 1. Activity Level Detection

- Added `ActivityLevel` enum with three levels: LOW, NORMAL, and HIGH
- Implemented movement sample collection to determine activity patterns
- Created `updateActivityLevel()` method to analyze movement patterns and classify activity level

### 2. Movement and Stationary State Tracking

- Enhanced accelerometer data processing to detect movement more accurately
- Added stationary duration tracking to identify long periods without movement
- Implemented long stationary state detection for aggressive power saving

### 3. Dynamic Scan Parameter Adjustment

- Enhanced `adjustScanParameters()` method with more sophisticated decision logic
- Implemented different scanning strategies based on:
  - Battery level (normal, low, critical)
  - Movement state (moving vs. stationary)
  - Activity level (high, normal, low)
  - Stationary duration (short vs. long)

### 4. Battery Level Monitoring

- Added critical battery threshold for extreme power saving
- Implemented different scanning strategies for different battery levels
- Prioritized battery saving over positioning accuracy when battery is low

### 5. Service Configuration Interface

- Added methods to BleScanService to configure optimization parameters:
  - `setLongStationaryThreshold()` - Configure time threshold for long stationary state
  - `setMovementThreshold()` - Adjust sensitivity of movement detection
  - `setBatteryThresholds()` - Set low and critical battery thresholds

## Optimization Strategies

### Battery Level Based Optimization

| Battery Level | Scan Mode | Scan Period | Scan Interval | Description |
|---------------|-----------|-------------|---------------|-------------|
| Critical (<10%) | SCAN_MODE_LOW_POWER | 3000ms | 20000ms | Ultra low power scanning with very infrequent updates |
| Low (<20%) | SCAN_MODE_LOW_POWER | 4000ms | 12000ms | Low power scanning with reduced frequency |
| Normal | Based on activity | Varies | Varies | Balanced based on activity level |

### Activity Level Based Optimization

| Activity Level | Scan Mode | Scan Period | Scan Interval | Description |
|----------------|-----------|-------------|---------------|-------------|
| HIGH | SCAN_MODE_LOW_LATENCY | 6000ms | 2000ms | High precision scanning with frequent updates |
| NORMAL | SCAN_MODE_BALANCED | 5000ms | 5000ms | Balanced scanning with moderate frequency |
| LOW | SCAN_MODE_LOW_POWER | 4000ms | 10000ms | Low power scanning with reduced frequency |

### Stationary State Optimization

| State | Scan Mode | Scan Period | Scan Interval | Description |
|-------|-----------|-------------|---------------|-------------|
| Moving | Based on activity | Varies | Varies | Determined by activity level |
| Short stationary | Based on activity | Varies | Varies | Determined by activity level |
| Long stationary (>60s) | SCAN_MODE_LOW_POWER | 3000ms | 15000ms | Very low power scanning with infrequent updates |

## Benefits

1. **Extended Battery Life**: By reducing scan frequency during periods of inactivity or when battery is low, the application can significantly extend battery life.

2. **Adaptive Accuracy**: Provides higher accuracy when the user is actively moving and requires precise positioning, while conserving battery when high accuracy is less critical.

3. **Context Awareness**: The system adapts to the user's behavior patterns, providing a better balance between functionality and power consumption.

4. **Configurable Parameters**: All thresholds and timing parameters can be adjusted to fine-tune the optimization for different devices and use cases.

## Future Improvements

1. **Machine Learning**: Implement machine learning to predict user movement patterns and optimize scanning proactively.

2. **Location-based Optimization**: Adjust scanning parameters based on known locations (e.g., more frequent scanning in complex areas).

3. **Time-based Patterns**: Learn user's daily patterns to optimize scanning based on time of day.

4. **Power Source Detection**: Implement different strategies when the device is charging vs. on battery.
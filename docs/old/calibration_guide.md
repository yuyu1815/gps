# Android Indoor Positioning Application - Calibration Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Preparation](#preparation)
3. [Beacon Placement](#beacon-placement)
4. [TxPower Calibration](#txpower-calibration)
5. [Environmental Factor Calibration](#environmental-factor-calibration)
6. [Sensor Calibration](#sensor-calibration)
7. [Validation and Testing](#validation-and-testing)
8. [Troubleshooting](#troubleshooting)
9. [Maintenance](#maintenance)

## Introduction

This guide provides detailed instructions for calibrating the Android Indoor Positioning Application for optimal performance in a specific indoor environment. Proper calibration is essential for achieving high positioning accuracy and reliability.

The calibration process involves several steps:
1. Preparing the environment and equipment
2. Placing beacons optimally
3. Calibrating beacon TxPower values
4. Adjusting environmental factors
5. Calibrating device sensors
6. Validating the calibration

## Preparation

### Required Equipment

- Android device with the Indoor Positioning Application installed
- BLE beacons (minimum 3, recommended 6-10 for a typical floor)
- Measuring tape or laser distance meter
- Tripod or beacon mounting equipment
- Floor plan of the indoor environment
- Notebook for recording calibration values

### Environment Assessment

Before placing beacons, assess the environment:

1. **Identify Obstacles**: Note walls, large metal objects, and electronic equipment that may interfere with BLE signals
2. **Map Coverage Areas**: Determine the areas where positioning is most critical
3. **Identify Power Sources**: If using powered beacons, locate available power outlets
4. **Note RF Interference Sources**: Identify Wi-Fi access points, microwave ovens, and other sources of 2.4 GHz interference

## Beacon Placement

Proper beacon placement is critical for accurate positioning.

### Placement Guidelines

- **Height**: Place beacons at a consistent height, ideally 2-3 meters above the floor
- **Obstacles**: Avoid placing beacons behind large metal objects, concrete walls, or electronic equipment
- **Spacing**: Maintain a distance of 5-15 meters between beacons, depending on the environment
- **Coverage**: Ensure that most locations have line-of-sight to at least 3 beacons
- **Geometry**: Avoid placing beacons in a straight line; triangular or rectangular arrangements provide better positioning accuracy

### Placement Procedure

1. **Create a Placement Plan**:
   - Mark beacon locations on the floor plan
   - Ensure good geometric distribution
   - Verify coverage of critical areas

2. **Install Beacons**:
   - Mount beacons securely at the planned locations
   - Record the exact coordinates of each beacon on the floor plan
   - Label each beacon with a unique identifier

3. **Record Beacon Information**:
   - For each beacon, record:
     - Beacon ID
     - MAC address
     - Physical coordinates (x, y, z)
     - Mounting height
     - Battery status (if applicable)

## TxPower Calibration

TxPower calibration ensures accurate distance estimation from RSSI values.

### TxPower Calibration Procedure

1. **Launch the Application**:
   - Open the Indoor Positioning Application
   - Navigate to Settings > Calibration > TxPower Calibration

2. **Calibrate Each Beacon**:
   - Stand exactly 1 meter away from the beacon
   - Ensure there are no obstacles between the device and the beacon
   - Hold the device at a consistent height (approximately chest level)
   - Point the device directly at the beacon
   - Press "Start Calibration" and follow the on-screen instructions
   - The app will collect multiple RSSI readings and calculate the average
   - Record the calibrated TxPower value for each beacon

3. **Verify Calibration**:
   - After calibrating all beacons, verify the distance estimation
   - Stand at known distances (1m, 3m, 5m) from each beacon
   - Check if the estimated distance matches the actual distance
   - If there are significant discrepancies, recalibrate the affected beacons

## Environmental Factor Calibration

The environmental factor (N) in the path loss model affects how RSSI values are converted to distances.

### Environmental Factor Calibration Procedure

1. **Launch the Application**:
   - Open the Indoor Positioning Application
   - Navigate to Settings > Calibration > Environmental Calibration

2. **Collect Calibration Data**:
   - Walk along a predefined path in the environment
   - The app will collect RSSI values at various distances from beacons
   - Ensure you cover different areas of the environment
   - The app will automatically calculate the optimal environmental factor

3. **Fine-tune the Environmental Factor**:
   - The app will suggest an environmental factor value
   - You can manually adjust this value if needed
   - Typical values range from 2.0 (open space) to 4.0 (complex indoor environment)
   - Test the positioning accuracy with different values
   - Select the value that provides the best overall accuracy

## Sensor Calibration

Sensor calibration improves the accuracy of Pedestrian Dead Reckoning (PDR).

### Accelerometer Calibration

1. **Launch the Application**:
   - Open the Indoor Positioning Application
   - Navigate to Settings > Calibration > Sensor Calibration > Accelerometer

2. **Calibration Procedure**:
   - Place the device on a flat, level surface
   - Press "Start Calibration"
   - The app will collect accelerometer data and calculate offset values
   - Keep the device completely still during this process

### Gyroscope Calibration

1. **Launch the Application**:
   - Open the Indoor Positioning Application
   - Navigate to Settings > Calibration > Sensor Calibration > Gyroscope

2. **Calibration Procedure**:
   - Place the device on a flat, level surface
   - Press "Start Calibration"
   - The app will collect gyroscope data and calculate bias values
   - Keep the device completely still during this process

### Magnetometer (Compass) Calibration

1. **Launch the Application**:
   - Open the Indoor Positioning Application
   - Navigate to Settings > Calibration > Sensor Calibration > Magnetometer

2. **Calibration Procedure**:
   - Hold the device away from metal objects and electronic devices
   - Press "Start Calibration"
   - Rotate the device in a figure-eight pattern
   - Continue until the progress bar reaches 100%
   - The app will calculate the magnetometer calibration parameters

### Step Length Calibration

1. **Launch the Application**:
   - Open the Indoor Positioning Application
   - Navigate to Settings > Calibration > Sensor Calibration > Step Length

2. **Calibration Procedure**:
   - Measure a straight path of exactly 10 meters
   - Stand at the starting point
   - Press "Start Calibration"
   - Walk normally along the measured path
   - Press "Stop Calibration" when you reach the end
   - The app will calculate your average step length

## Validation and Testing

After completing all calibration steps, validate the overall positioning accuracy.

### Validation Procedure

1. **Define Test Points**:
   - Mark several test points throughout the environment
   - Record the exact coordinates of each test point
   - Include points with different beacon visibility conditions

2. **Measure Positioning Accuracy**:
   - Stand at each test point
   - Record the position reported by the app
   - Calculate the error between the reported position and the actual position
   - Repeat multiple times for statistical significance

3. **Analyze Results**:
   - Calculate average error across all test points
   - Identify areas with higher error
   - Determine if additional beacons or recalibration is needed

### Performance Metrics

Track the following metrics during validation:

- **Average Position Error**: Should be less than 2-3 meters
- **95th Percentile Error**: Should be less than 5 meters
- **Update Rate**: Position updates should be smooth and consistent
- **Latency**: Position updates should have minimal delay

## Troubleshooting

### Common Calibration Issues

#### Poor TxPower Calibration

**Symptoms**:
- Inconsistent distance estimates
- Distance estimates that change dramatically with small movements

**Solutions**:
- Ensure you're exactly 1 meter from the beacon during calibration
- Remove obstacles between the device and beacon
- Check for interference from other RF sources
- Recalibrate multiple times and use the average value

#### Inaccurate Environmental Factor

**Symptoms**:
- Consistent overestimation or underestimation of distances
- Position estimates that are consistently offset in one direction

**Solutions**:
- Collect more calibration data in different areas
- Manually adjust the environmental factor
- Consider using different environmental factors for different zones

#### Sensor Calibration Issues

**Symptoms**:
- Drift in position when using PDR
- Incorrect heading direction
- Missed or false step detections

**Solutions**:
- Recalibrate sensors in an interference-free environment
- Adjust step detection sensitivity
- Verify that the device is held in a consistent position

## Maintenance

Regular maintenance ensures continued positioning accuracy.

### Routine Checks

Perform these checks monthly or after any changes to the environment:

1. **Beacon Health Check**:
   - Use the app's beacon health monitoring feature
   - Verify that all beacons are operational
   - Check battery levels of battery-powered beacons
   - Replace batteries as needed

2. **Positioning Accuracy Check**:
   - Verify positioning accuracy at several test points
   - If accuracy has degraded, recalibrate as needed

3. **Environmental Changes**:
   - If there have been significant changes to the environment (new walls, furniture rearrangement, etc.), recalibrate the environmental factor

### Beacon Replacement

When replacing a beacon:

1. Mount the new beacon in the exact same location
2. Update the beacon information in the app
3. Perform TxPower calibration for the new beacon
4. Verify positioning accuracy near the replaced beacon

### Software Updates

After updating the application:

1. Verify that all calibration settings are preserved
2. Check positioning accuracy at several test points
3. Recalibrate if necessary